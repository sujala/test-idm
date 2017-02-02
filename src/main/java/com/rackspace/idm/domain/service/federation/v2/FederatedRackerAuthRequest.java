package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.lang.StringUtils;

/**
 * A simple bean for extracting the necessary information from a raw saml request - refines a federatedAuthRequest to be
 * a Federated Racker Auth Request
 */
public class FederatedRackerAuthRequest {

    @Delegate
    private FederatedAuthRequest federatedAuthRequest;

    @Getter
    private String username;

    public FederatedRackerAuthRequest(FederatedAuthRequest federatedAuthRequest) {
        this.federatedAuthRequest = federatedAuthRequest;

        username = federatedAuthRequest.getWrappedSamlResponse().getUsername();

        validateStructure();
    }

    private void validateStructure() {
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException("Invalid username. One, and only one, domain must be provided.");
        }
    }
}
