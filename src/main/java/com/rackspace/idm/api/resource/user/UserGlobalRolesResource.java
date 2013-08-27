package com.rackspace.idm.api.resource.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * User Application Roles Resource.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserGlobalRolesResource {

    private final UserGlobalRoleResource roleResource;
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public UserGlobalRolesResource(UserGlobalRoleResource roleResource) {
        this.roleResource = roleResource;
    }

    @Path("{roleId}")
    public UserGlobalRoleResource getRoleResource() {
        return roleResource;
    }
}
