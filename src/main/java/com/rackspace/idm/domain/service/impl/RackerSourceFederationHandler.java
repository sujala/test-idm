package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedRackerRequest;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.FederatedRackerDao;
import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.SamlLogoutResponseUtil;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.LogoutResponse;
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

    @Autowired(required = false)
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

        IdentityProviderFederationTypeEnum federationTypeEnum = provider.getFederationTypeAsEnum();
        if (federationTypeEnum != IdentityProviderFederationTypeEnum.RACKER) {
            throw new IllegalStateException(String.format("Invalid target user source '%s' for racker user federation", federationTypeEnum));
        }

        FederatedRackerRequest request = parseSaml(samlResponseDecorator, provider);

        /*
         Get the roles from edir. This has the side effect of validating the user exists in eDir. If not, want to throw
         an error because the request is invalid.
         */
        List<TenantRole> tenantRoles = generateRolesForRacker(request.getUser().getUsername());

        if (identityConfig.getReloadableConfig().shouldPersistRacker()) {
            persistRackerForRequest(request);
        }

        //get the auth by values
        List<AuthenticatedByMethodEnum> authByList = new ArrayList<AuthenticatedByMethodEnum>(2);
        authByList.add(AuthenticatedByMethodEnum.FEDERATION);
        authByList.add(samlResponseDecorator.checkAndGetAuthContextClassRef().getIdmAuthBy());

        RackerScopeAccess token = createToken(request.getUser(), request.getRequestedTokenExpirationDate(), authByList);

        return new SamlAuthResponse(request.getUser(), tenantRoles, Collections.EMPTY_LIST, token);
    }

    @Override
    public SamlLogoutResponse processLogoutRequestForProvider(LogoutRequestDecorator logoutRequestDecorator, IdentityProvider provider) {
        BadRequestException ex = new BadRequestException("Logging out federated rackers is not supported", ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST);
        return SamlLogoutResponseUtil.createErrorLogoutResponse(logoutRequestDecorator.getLogoutRequest().getID(), StatusCode.REQUESTER_URI, ex.getMessage(), ex);
    }

    private void persistRackerForRequest(FederatedRackerRequest request) {
        Racker userToCreate = request.getUser();
        Racker resultUser = federatedRackerDao.getUserById(userToCreate.getId());
        if (resultUser == null) {
            federatedRackerDao.addUser(request.getIdentityProvider(), userToCreate);
            tenantService.addTenantRoleToUser(userToCreate, tenantService.getEphemeralRackerTenantRole());
        } else {
            //Clean up tokens underneath existing racker
            scopeAccessService.deleteExpiredTokensQuietly(resultUser);
        }
    }

    private FederatedRackerRequest parseSaml(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider) {
        //populate a federated user object based on saml data
        FederatedRackerRequest request = new FederatedRackerRequest();

        //validate assertion
        //set racker
        String rackerUserName = samlResponseDecorator.checkAndGetUsername();
        String rackerId = Racker.asFederatedRackerId(rackerUserName, provider.getUri());
        Racker federatedRacker = userService.getRackerByRackerId(rackerId);

        request.setIdentityProvider(provider);
        request.setUser(federatedRacker);
        request.setRequestedTokenExpirationDate(samlResponseDecorator.checkAndGetSubjectConfirmationNotOnOrAfterDate());

        return request;
    }

    /**
     * Determine the roles that the new federated user should receive. Roles are pulled from eDir (plus additional
     * "racker" role automatically added).
     *
     * @return
     */
    private List<TenantRole> generateRolesForRacker(String rackerUsername) {
        List<TenantRole> roles;
        try {
            roles = tenantService.getEphemeralRackerTenantRoles(rackerUsername);
        } catch (NotFoundException e) {
            //the racker does not exist. log and translate
            log.debug("Requested racker does not exist", e);
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(
                    ErrorCodes.ERROR_CODE_FEDERATION_RACKER_NON_EXISTANT_RACKER
                    , String.format(ErrorCodes.ERROR_MESSAGE_FORMAT_FEDERATION_RACKER_NON_EXISTANT_RACKER, rackerUsername)));
        }

        return roles;
    }

    /**
     * A new token is always created on every racker federated auth
     *
     * @param user
     * @param requestedExpirationDate
     * @param authBy
     *
     * @return
     */
    private RackerScopeAccess createToken(Racker user, DateTime requestedExpirationDate, List<AuthenticatedByMethodEnum> authBy) {
        RackerScopeAccess token = new RackerScopeAccess();
        token.setRackerId(user.getId());
        token.setAccessTokenString(scopeAccessService.generateToken());
        token.setAccessTokenExp(requestedExpirationDate.toDate());
        token.setClientId(identityConfig.getStaticConfig().getCloudAuthClientId());

        for (AuthenticatedByMethodEnum authMethod : authBy) {
            token.getAuthenticatedBy().add(authMethod.getValue());
        }

        scopeAccessService.addUserScopeAccess(user, token);

        return token;
    }
}
