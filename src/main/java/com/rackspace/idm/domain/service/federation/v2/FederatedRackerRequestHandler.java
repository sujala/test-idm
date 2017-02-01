package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles SAML authentication requests for rackers.
 */
@Component
public class FederatedRackerRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FederatedRackerRequestHandler.class);

    public SamlAuthResponse processAuthRequest(FederatedRackerAuthRequest authRequest) {
        // Just a few sanity checks
        Validate.notNull(authRequest, "request must not be null");
        Validate.notNull(authRequest.getBrokerIdp(), "Broker IDP must not be null");
        Validate.notNull(authRequest.getOriginIdp(), "Origin IDP must not be null");
        Validate.isTrue(authRequest.getBrokerIdp().getFederationTypeAsEnum() == IdentityProviderFederationTypeEnum.BROKER, "Broker IDP must be a BROKER type");
        Validate.isTrue(authRequest.getOriginIdp().getFederationTypeAsEnum() == IdentityProviderFederationTypeEnum.RACKER, "Origin IDP must be a DOMAIN type");

        //TODO: Processing for RACKER Auth
        throw new UnsupportedOperationException("Federated Racker Auth v2 not yet available");
    }

}
