package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;

import javax.xml.namespace.QName;
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
    public void toUserDO_setsRegion() throws Exception {
        final UserForCreate user = new UserForCreate();
        user.getOtherAttributes().put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0","defaultRegion"),"foo");
        final User userDO = userConverterCloudV20.toUserDO(user);
        assertThat("user region", userDO.getRegion(), equalTo("foo"));
    }

    @Test
    public void toUser_domainUserHasRegion_setsJaxbObjectsRegion() throws Exception {
        final User user = new User();
        user.setRegion("myRegion");
        when(objectFactory.createUser()).thenReturn(new org.openstack.docs.identity.api.v2.User());
        final org.openstack.docs.identity.api.v2.User jaxbObject = userConverterCloudV20.toUser(user);
        assertThat("region", jaxbObject.getOtherAttributes().get(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0","defaultRegion")),equalTo("myRegion"));
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
