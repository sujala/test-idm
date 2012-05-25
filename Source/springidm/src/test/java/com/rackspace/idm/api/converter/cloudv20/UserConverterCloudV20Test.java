package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.TenantRole;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 5/25/12
 * Time: 11:39 AM
 */
public class UserConverterCloudV20Test {

    UserConverterCloudV20 userConverterCloudV20;
    ObjectFactory objectFactory = mock(ObjectFactory.class);
    RoleConverterCloudV20 roleConverterCloudV20 = mock(RoleConverterCloudV20.class);

    @Before
    public void setUp(){
        userConverterCloudV20 = new UserConverterCloudV20();
        userConverterCloudV20.setObjectFactory(objectFactory);
        userConverterCloudV20.setRoleConverterCloudV20(roleConverterCloudV20);
    }

    @Test
    public void toUserForAuthenticateResponse_calls_createUserForAuthenticateResponse() throws Exception {
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        userConverterCloudV20.toUserForAuthenticateResponse(new Racker(), null);
        verify(objectFactory).createUserForAuthenticateResponse();
    }

    @Test
    public void toUserForAuthenticateResponse_setsUsername() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("joe.Racker");
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        UserForAuthenticateResponse userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse(racker, null);
        assertThat("name", userForAuthenticateResponse.getId(), equalTo("joe.Racker"));
    }

    @Test
    public void toUserForAuthenticateResponse_setsId() throws Exception {
        Racker racker = new Racker();
        racker.setUsername("joe.Racker");
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        UserForAuthenticateResponse userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse(racker, null);
        assertThat("name", userForAuthenticateResponse.getName(), equalTo("joe.Racker"));
    }

    @Test
    public void toUserForAuthenticateResponse_whenRolesNotNull_callsRoleConverter() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("joe.Racker");
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        userConverterCloudV20.toUserForAuthenticateResponse(racker, roles);
        verify(roleConverterCloudV20).toRoleListJaxb(roles);
    }
}
