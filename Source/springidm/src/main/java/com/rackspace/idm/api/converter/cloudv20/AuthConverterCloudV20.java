package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationResponse;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;
    
    @Autowired
    private TokenConverterCloudV20 tokenConverterCloudV20;
    
    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;
    
    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;
    
    public AuthenticateResponse toAuthenticationResponse(User user, ScopeAccess scopeAccess, List<TenantRole> roles, List<OpenstackEndpoint> endpoints) {
        AuthenticateResponse auth = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createAuthenticateResponse();
        
        auth.setToken(this.tokenConverterCloudV20.toToken(scopeAccess, roles));
        auth.setUser(this.userConverterCloudV20.toUserForAuthenticateResponse(user, roles));
        auth.setServiceCatalog(this.endpointConverterCloudV20.toServiceCatalog(endpoints));
        
        return auth;
    }

    public ImpersonationResponse toImpersonationResponse(ScopeAccess scopeAccess) {
        ImpersonationResponse auth = OBJ_FACTORIES.getRackspaceIdentityExtRaxgaV1Factory().createImpersonationResponse();
        auth.setToken(this.tokenConverterCloudV20.toToken(scopeAccess));
        return auth;
    }
}
