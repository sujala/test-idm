package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ServiceCatalogInfo;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories jaxbObjectFactories;

    @Autowired
    private TokenConverterCloudV20 tokenConverterCloudV20;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;

    public AuthenticateResponse toAuthenticationResponse(SamlAuthResponse samlAuthResponse) {
        AuthenticateResponse auth = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

        auth.setToken(this.tokenConverterCloudV20.toToken(samlAuthResponse.getToken(), samlAuthResponse.getUserRoles()));
        if (samlAuthResponse.getUser() instanceof Racker) {
            auth.setUser(this.userConverterCloudV20.toRackerForAuthenticateResponse((Racker) samlAuthResponse.getUser(), samlAuthResponse.getUserRoles()));
        } else {
            auth.setUser(this.userConverterCloudV20.toUserForAuthenticateResponse((EndUser) samlAuthResponse.getUser(), samlAuthResponse.getUserRoles()));
        }
        auth.setServiceCatalog(this.endpointConverterCloudV20.toServiceCatalog(samlAuthResponse.getEndpoints()));

        return getAuthResponseWithoutServiceId(auth);
    }

    public AuthenticateResponse toAuthenticationResponse(AuthResponseTuple authResponseTuple, ServiceCatalogInfo scInfo) {
        AuthenticateResponse auth = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

        auth.setToken(this.tokenConverterCloudV20.toToken(authResponseTuple.getUserScopeAccess(), scInfo.getUserTenantRoles()));
        auth.setUser(this.userConverterCloudV20.toUserForAuthenticateResponse(authResponseTuple.getUser(), scInfo.getUserTenantRoles()));
        auth.setServiceCatalog(this.endpointConverterCloudV20.toServiceCatalog(scInfo.getUserEndpoints()));

        return getAuthResponseWithoutServiceId(auth);
    }

    public AuthenticateResponse toRackerAuthenticationResponse(Racker user, ScopeAccess scopeAccess, List<TenantRole> roles, List<OpenstackEndpoint> endpoints) {
        AuthenticateResponse auth = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

        auth.setToken(this.tokenConverterCloudV20.toToken(scopeAccess, roles));
        auth.setUser(this.userConverterCloudV20.toRackerForAuthenticateResponse(user, roles));
        auth.setServiceCatalog(this.endpointConverterCloudV20.toServiceCatalog(endpoints));

        return getAuthResponseWithoutServiceId(auth);
    }

    public ImpersonationResponse toImpersonationResponse(ScopeAccess scopeAccess) {
        ImpersonationResponse auth = jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createImpersonationResponse();
        auth.setToken(this.tokenConverterCloudV20.toToken(scopeAccess, null));
        return auth;
    }

    public void setJaxbObjectFactories(JAXBObjectFactories jaxbObjectFactories) {
        this.jaxbObjectFactories = jaxbObjectFactories;
    }

    public void setTokenConverterCloudV20(TokenConverterCloudV20 tokenConverterCloudV20) {
        this.tokenConverterCloudV20 = tokenConverterCloudV20;
    }

    public void setUserConverterCloudV20(UserConverterCloudV20 userConverterCloudV20) {
        this.userConverterCloudV20 = userConverterCloudV20;
    }

    public void setEndpointConverterCloudV20(EndpointConverterCloudV20 endpointConverterCloudV20) {
        this.endpointConverterCloudV20 = endpointConverterCloudV20;
    }

    private AuthenticateResponse getAuthResponseWithoutServiceId(AuthenticateResponse auth) {
        if (auth.getUser() != null && auth.getUser().getRoles() != null) {
            for (Role r : auth.getUser().getRoles().getRole()) {
                r.setServiceId(null);
            }
        }
        return auth;
    }
}
