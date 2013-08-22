package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
