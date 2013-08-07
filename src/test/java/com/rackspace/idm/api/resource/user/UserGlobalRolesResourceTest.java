package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 2:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserGlobalRolesResourceTest {
    private UserGlobalRolesResource userGlobalRolesResource;
    private UserGlobalRoleResource roleResource;

    @Before
    public void setUp() throws Exception {
        roleResource = mock(UserGlobalRoleResource.class);
        userGlobalRolesResource = new UserGlobalRolesResource(roleResource);
    }

    @Test
    public void getRoleResource_returnsRoleResource() throws Exception {
        UserGlobalRoleResource resource = userGlobalRolesResource.getRoleResource();
        assertThat("roles resource", resource, equalTo(roleResource));
    }
}
