package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedRackerRequest;
import com.rackspace.idm.domain.config.IdentityConfig;
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
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles v2 Federated authentication requests for rackers.
 */
@Component
public class FederatedRackerRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FederatedRackerRequestHandler.class);

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    public SamlAuthResponse processAuthRequestForProvider(FederatedRackerAuthRequest authRequest, IdentityProvider rackerIdentityProvider) {
        // Just a few sanity checks
        Validate.notNull(authRequest, "request must not be null");
        Validate.isTrue(rackerIdentityProvider.getFederationTypeAsEnum() == IdentityProviderFederationTypeEnum.RACKER, "Provider must be a Racker provider");

        /*
         Get the roles from edir. This has a side effect of validating the user exists in eDir and throwing a
         BadRequestException if it doesn't.
         */
        List<TenantRole> tenantRoles = generateRolesForRacker(authRequest.getUsername());

        // Create a Racker object - required for use by scope service to generate appropriate token
        String rackerUserName = authRequest.getUsername();
        String rackerId = Racker.asFederatedRackerId(rackerUserName, authRequest.getOriginIssuer());
        Racker federatedRacker = userService.getRackerByRackerId(rackerId);

        RackerScopeAccess token = createToken(federatedRacker, authRequest);

        return new SamlAuthResponse(federatedRacker, tenantRoles, Collections.EMPTY_LIST, token);
    }

    /**
     * Determine the roles that the new federated user should receive. Roles are pulled from IAM (plus additional
     * "racker" role automatically added).
     *
     * @return
     *
     * @throws BadRequestException If the specified racker does not exist in the associated IAM system
     */
    private List<TenantRole> generateRolesForRacker(String rackerUsername) {
        List<TenantRole> roles;
        try {
            roles = tenantService.getEphemeralRackerTenantRoles(rackerUsername);
        } catch (NotFoundException e) {
            //the racker does not exist. log and translate
            log.debug(String.format("Requested racker '%s' does not exist", rackerUsername), e);
            throw new BadRequestException(String.format(ErrorCodes.ERROR_MESSAGE_FORMAT_FEDERATION_RACKER_NON_EXISTANT_RACKER, rackerUsername)
                    , ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }

        return roles;
    }

    private RackerScopeAccess createToken(Racker federatedRacker, FederatedRackerAuthRequest authRequest) {
        List<String> authBy = Arrays.asList(AuthenticatedByMethodEnum.FEDERATION.getValue(), authRequest.getAuthenticatedByForRequest().getValue());
        return (RackerScopeAccess) scopeAccessService.addScopedScopeAccess(federatedRacker, identityConfig.getStaticConfig().getCloudAuthClientId(), authBy, authRequest.getRequestedTokenExpiration().toDate(), null);
    }
}
