package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

public interface UserAuthenticationFactor {
    /**
     * Authenticates a request and returns an AuthResponseTuple that contains the associated User and valid ScopeAccess
     * objects associated with the authentication.
     *
     * @param authenticationRequest
     * @return
     */
    AuthResponseTuple authenticateForAuthResponse(AuthenticationRequest authenticationRequest);

    /**
     * Returns whether authentication succeeded or not for the specified request.
     *
     * @param authenticationRequest
     * @return
     */
    UserAuthenticationResult authenticate(AuthenticationRequest authenticationRequest);

    AuthResponseTuple createScopeAccessForUserAuthenticationResult(UserAuthenticationResult userAuthenticationResult);
}
