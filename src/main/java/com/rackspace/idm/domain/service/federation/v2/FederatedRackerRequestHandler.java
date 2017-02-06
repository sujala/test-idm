package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.domain.entity.IdentityProvider;
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

    public SamlAuthResponse processAuthRequestForProvider(FederatedRackerAuthRequest authRequest, IdentityProvider rackerIdentityProvider) {
        // Just a few sanity checks
        Validate.notNull(authRequest, "request must not be null");
        Validate.isTrue(rackerIdentityProvider.getFederationTypeAsEnum() == IdentityProviderFederationTypeEnum.RACKER, "Provider must be a Racker provider");

        //TODO: Processing for RACKER Auth
        throw new UnsupportedOperationException("Federated Racker Auth v2 not yet available");
    }

}
