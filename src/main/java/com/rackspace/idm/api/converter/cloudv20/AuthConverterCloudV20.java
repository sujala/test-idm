package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.*;
import com.rsa.cryptoj.c.B;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    /**
     * converts authData object to authenticate response
     * @param authData
     * @return
     */
    public AuthenticateResponse toAuthenticationResponse(AuthData authData) {
        AuthenticateResponse auth = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

        auth.setToken(this.tokenConverterCloudV20.toToken(authData.getToken()));
        auth.setUser(this.userConverterCloudV20.toUserForAuthenticateResponse(authData.getUser()));
        auth.setServiceCatalog(this.endpointConverterCloudV20.toServiceCatalog(authData.getEndpoints()));

        return getAuthResponseWithoutServiceId(auth);
    }

    @Deprecated
    public AuthenticateResponse toAuthenticationResponse(BaseUser user, ScopeAccess scopeAccess, List<TenantRole> roles, List<OpenstackEndpoint> endpoints) {
        AuthenticateResponse auth = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

        auth.setToken(this.tokenConverterCloudV20.toToken(scopeAccess, roles));
        if(user instanceof Racker){
            auth.setUser(this.userConverterCloudV20.toRackerForAuthenticateResponse((Racker) user, roles));
        }else {
            auth.setUser(this.userConverterCloudV20.toUserForAuthenticateResponse((User) user, roles));
        }
        auth.setServiceCatalog(this.endpointConverterCloudV20.toServiceCatalog(endpoints));
        
        return auth;
    }

    public ImpersonationResponse toImpersonationResponse(ScopeAccess scopeAccess) {
        ImpersonationResponse auth = jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createImpersonationResponse();
        auth.setToken(this.tokenConverterCloudV20.toToken(scopeAccess));
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
