package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.SamlAuthResponse;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.Response;

public interface FederatedIdentityService {
    SamlAuthResponse processSamlResponse(Response samlResponse);

    /**
     * Logs out/terminates the associated federated user, which removes any persistent state for that user and revokes any tokens
     * that may have been created for the user.
     *
     * @param logoutRequest
     * @return
     */
    void processLogoutRequest(LogoutRequest logoutRequest);

}
