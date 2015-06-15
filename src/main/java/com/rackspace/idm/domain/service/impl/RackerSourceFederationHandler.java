package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedRackerRequest;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.FederatedRackerDao;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ErrorCodeIdmException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles SAML authentication requests against Racker based identity providers.
 */
@Component
public class RackerSourceFederationHandler implements FederationHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);

    public static final String FEDERATED_RACKER_ID_PATTERN = "%s@%s";

    @Autowired
    FederatedRackerDao federatedRackerDao;

    @Autowired
    ScopeAccessService scopeAccessService;

    @Autowired
    UserService userService;

    @Autowired
    TenantService tenantService;

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public SamlAuthResponse processRequestForProvider(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider) {
        Validate.notNull(samlResponseDecorator, "saml response must not be null");
        Validate.notNull(provider, "provider must not be null");

        TargetUserSourceEnum targetUserSourceEnum = provider.getTargetUserSourceAsEnum();
        if (targetUserSourceEnum != TargetUserSourceEnum.RACKER) {
            throw new IllegalStateException(String.format("Invalid target user source for racker user federation", targetUserSourceEnum));
        }

        FederatedRackerRequest request = parseSaml(samlResponseDecorator, provider);

        /*
         Get the roles from edir. This has the side effect of validating the user exists in eDir. If not, want to throw
         an error because the request is invalid.
         */
        List<TenantRole> tenantRoles = generateRolesForRacker(request.getUser().getUsername());

        Racker user = processUserForRequest(request);
        RackerScopeAccess token = createToken(user, request.getRequestedTokenExpirationDate());

        return new SamlAuthResponse(user, tenantRoles, Collections.EMPTY_LIST, token);
    }

    private FederatedRackerRequest parseSaml(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider) {
        //populate a federated user object based on saml data
        FederatedRackerRequest request = new FederatedRackerRequest();

        //validate assertion
        //set racker
        Racker federatedRacker = new Racker();
        String rackerUserName = samlResponseDecorator.checkAndGetUsername();
        String federatedRackerId = String.format(FEDERATED_RACKER_ID_PATTERN, rackerUserName, provider.getUri());
        federatedRacker.setId(federatedRackerId);
        federatedRacker.setEnabled(true);

        request.setIdentityProvider(provider);
        request.setUser(federatedRacker);
        request.setRequestedTokenExpirationDate(samlResponseDecorator.checkAndGetSubjectConfirmationNotOnOrAfterDate());

        return request;
    }

    /**
     * If the user already exists, will return a _new_ user object representing the stored user. If the user does not exist, a new user is created in the backend, but the provided
     * user object in request is altered (updated with ID).
     * <p/>
     * Will also delete expired tokens if the user already existed
     *
     * @param request
     * @return
     */
    private Racker processUserForRequest(FederatedRackerRequest request) {
        Racker resultUser = federatedRackerDao.getUserById(request.getUser().getId());
        if (resultUser == null) {
            resultUser = createUserForRequest(request);
        } else {
            //Clean up tokens underneath existing racker
            scopeAccessService.deleteExpiredTokensQuietly(resultUser);
        }
        return resultUser;
    }

    private Racker createUserForRequest(FederatedRackerRequest request) {
        Racker userToCreate = request.getUser();

        federatedRackerDao.addUser(request.getIdentityProvider(), userToCreate);
        tenantService.addTenantRoleToUser(userToCreate, createRackerTenantRole());

        return userToCreate;
    }

    private TenantRole createRackerTenantRole() {
        TenantRole rackerTenantRole = new TenantRole();
        rackerTenantRole.setRoleRsId(identityConfig.getStaticConfig().getRackerRoleId());
        rackerTenantRole.setClientId(identityConfig.getStaticConfig().getFoundationClientId());
        rackerTenantRole.setName(GlobalConstants.ROLE_NAME_RACKER);

        return rackerTenantRole;
    }

    /**
     * Determine the roles that the new federated user should receive. Roles are pulled from eDir (plus additional
     * "racker" role automatically added).
     *
     * @return
     */
    private List<TenantRole> generateRolesForRacker(String rackerUsername) {
        List<TenantRole> roles = new ArrayList<TenantRole>();

        //Add Racker eDir Roles
        List<String> rackerRoles = null;
        try {
            rackerRoles = userService.getRackerRoles(rackerUsername);
        } catch (NotFoundException e) {
            //the racker does not exist. log and translate
            log.debug("Requested racker does not exist", e);
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(
                    ErrorCodes.ERROR_CODE_FEDERATION_RACKER_NON_EXISTANT_RACKER
                    , String.format(ErrorCodes.ERROR_MESSAGE_FORMAT_FEDERATION_RACKER_NON_EXISTANT_RACKER, rackerUsername)));
        }

        if (rackerRoles != null) {
            for (String r : rackerRoles) {
                TenantRole t = new TenantRole();
                t.setName(r);
                roles.add(t);
            }
        }

        roles.add(createRackerTenantRole());

        return roles;
    }

    /**
     * A new token is always created on every racker federated auth
     *
     * @param user
     * @param requestedExpirationDate
     * @return
     */
    private RackerScopeAccess createToken(Racker user, DateTime requestedExpirationDate) {
        RackerScopeAccess token = new RackerScopeAccess();
        token.setRackerId(user.getId());
        token.setAccessTokenString(scopeAccessService.generateToken());
        token.setAccessTokenExp(requestedExpirationDate.toDate());
        token.setClientId(identityConfig.getStaticConfig().getCloudAuthClientId());
        token.getAuthenticatedBy().add(AuthenticatedByMethodEnum.FEDERATION.getValue());

        scopeAccessService.addUserScopeAccess(user, token);

        return token;
    }
}
