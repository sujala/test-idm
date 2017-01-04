package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.Response;

public interface AuthenticateResponseService {

    /**
     * Builds the AuthenticateResponse object for a successful authenticate request. This method takes the AuthResponseTuple
     * data for the given request and the AuthenticationRequest object. Note, this method assumes that the user in the
     * authResponseTuple has successfully authenticated.
     *
     * @param authResponseTuple
     * @param authenticationRequest
     * @return
     */
    Response.ResponseBuilder buildAuthResponseForAuthenticate(AuthResponseTuple authResponseTuple, AuthenticationRequest authenticationRequest);

    /**
     * Builds the AuthenticateResponse object for a given RackerScopeAccess
     *
     * @param sa
     * @return
     */
    AuthenticateResponse buildAuthResponseForValidateToken(RackerScopeAccess sa);

    /**
     * Builds the AuthenticateResponse object for a given UserScopeAccess object and the given tenant
     *
     * @param sa
     * @return
     */
    AuthenticateResponse buildAuthResponseForValidateToken(UserScopeAccess sa, String tenantId);

    /**
     * Builds the AuthenticateResponse object for a given ImpersonatedScopeAccess object and the given tenant
     *
     * @param sa
     * @return
     */
    AuthenticateResponse buildAuthResponseForValidateToken(ImpersonatedScopeAccess sa, String tenantId);

}
