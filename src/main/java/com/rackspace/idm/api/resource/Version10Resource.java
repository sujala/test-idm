package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.tenant.TenantsResource;
import com.rackspace.idm.api.resource.token.TokensResource;
import com.rackspace.idm.api.resource.user.UsersResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * API Version
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Version10Resource {

    private final UsersResource usersResource;
    private final TokensResource tokenResource;
    private final TenantsResource tenantsResource;

    @Autowired
    public Version10Resource(UsersResource usersResource,
        TokensResource tokenResource,
        TenantsResource tenantsResource) {
        this.usersResource = usersResource;
        this.tokenResource = tokenResource;
        this.tenantsResource = tenantsResource;
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return usersResource;
    }
   
    @Path("tokens")
    public TokensResource getTokenResource() {
        return tokenResource;
    }

    @Path("tenants")
    public TenantsResource getTenantResource() {
        return tenantsResource;
    }
}
