package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ServiceCatalogInfo;
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
     * Builds the AuthenticateResponse object for a successful authenticate request while applying rcn role logic.
     * This method takes the AuthResponseTuple
     * data for the given request and the AuthenticationRequest object. Note, this method assumes that the user in the
     * authResponseTuple has successfully authenticated.
     *
     * @param authResponseTuple
     * @param authenticationRequest
     * @return
     */
    Response.ResponseBuilder buildAuthResponseForAuthenticateApplyRcn(AuthResponseTuple authResponseTuple, AuthenticationRequest authenticationRequest);

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
     * Builds the AuthenticateResponse object for a given UserScopeAccess object and the given tenant while applying
     * the rcn role logic.
     *
     * @param sa
     * @param tenantId
     * @return
     */
    AuthenticateResponse buildAuthResponseForValidateTokenApplyRcnRoles(UserScopeAccess sa, String tenantId);

    /**
     * Builds the AuthenticateResponse object for a given ImpersonatedScopeAccess object and the given tenant
     *
     * @param sa
     * @return
     */
    AuthenticateResponse buildAuthResponseForValidateToken(ImpersonatedScopeAccess sa, String tenantId);

    /**
     * Builds the AuthenticateResponse object for a given ImpersonatedScopeAccess object and the given tenant including
     * the application of RCN role logic.
     *
     * @param isa
     * @param tenantId
     * @return
     */
    AuthenticateResponse buildAuthResponseForValidateTokenApplyRcnRoles(ImpersonatedScopeAccess isa, String tenantId);

    /**
     * Return the tenant to reflect in the X-Tenant-Id header on authenticate calls. This tenant is
     * used for UAEs. This call uses the following logic:
     *
     * 1. If user has a single tenant id then use that value
     * 2. If user has multiple tenant ids then use the mosso tenant id
     * 3. If user has multiple tenant ids and none are mosso pick a unique tenant
     * 4. If user does not have any tenants then return null
     *
     */
    Tenant getTenantForAuthResponseTenantHeader(ServiceCatalogInfo serviceCatalogInfo);
    
}
