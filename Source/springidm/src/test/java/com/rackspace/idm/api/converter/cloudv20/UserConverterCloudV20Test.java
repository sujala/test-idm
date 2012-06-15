package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.UserList;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

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
    public void toUserDO_withUser_setsFields() throws Exception {
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setUsername("username");
        user.setEmail("email");
        user.setDisplayName("displayName");
        user.setEnabled(true);

        User userDO = userConverterCloudV20.toUserDO(user);
        assertThat("username", userDO.getUsername(), equalTo("username"));
        assertThat("email", userDO.getEmail(), equalTo("email"));
        assertThat("display name", userDO.getDisplayName(), equalTo("displayName"));
        assertThat("enabled", userDO.isEnabled(), equalTo(true));
    }

    @Test
    public void toUserDO_withUserForCreate_setsPassword() throws Exception {
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setPassword("password");

        User user = userConverterCloudV20.toUserDO(userForCreate);
        assertThat("password", user.getPassword(), equalTo("password"));
    }

    @Test
    public void toUserDO_setsRegion() throws Exception {
        final UserForCreate user = new UserForCreate();
        user.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","defaultRegion"),"foo");
        final User userDO = userConverterCloudV20.toUserDO(user);
        assertThat("user region", userDO.getRegion(), equalTo("foo"));
    }

    @Test
    public void toUser_domainUserHasRegion_setsJaxbObjectsRegion() throws Exception {
        final User user = new User();
        user.setRegion("myRegion");
        when(objectFactory.createUser()).thenReturn(new org.openstack.docs.identity.api.v2.User());
        final org.openstack.docs.identity.api.v2.User jaxbObject = userConverterCloudV20.toUser(user);
        assertThat("region", jaxbObject.getOtherAttributes().get(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","defaultRegion")),equalTo("myRegion"));
    }

    @Test
    public void toUser_domainUserHasNullRegion_doesNotSetJaxbObjectsRegion() throws Exception {
        final User user = new User();
        when(objectFactory.createUser()).thenReturn(new org.openstack.docs.identity.api.v2.User());
        final org.openstack.docs.identity.api.v2.User jaxbObject = userConverterCloudV20.toUser(user);
        assertThat("does not contain region", jaxbObject.getOtherAttributes().containsKey("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\",\"defaultRegion"), equalTo(false));
    }

    @Test
    public void toUser_withNonNullCreatedDate_setsJaxbCreatedDate() throws Exception {
        User user = new User();
        user.setCreated(new DateTime(1));
        when(objectFactory.createUser()).thenReturn(new org.openstack.docs.identity.api.v2.User());
        org.openstack.docs.identity.api.v2.User user1 = userConverterCloudV20.toUser(user);
        assertThat("date created", user1.getCreated().toGregorianCalendar().getTimeInMillis(), equalTo(1L));
    }

    @Test
    public void toUser_withNonNullUpdatedDate_setsJaxbUpdatedDate() throws Exception {
        User user = new User();
        user.setUpdated(new DateTime(1));
        when(objectFactory.createUser()).thenReturn(new org.openstack.docs.identity.api.v2.User());
        org.openstack.docs.identity.api.v2.User user1 = userConverterCloudV20.toUser(user);
        assertThat("date updated", user1.getUpdated().toGregorianCalendar().getTimeInMillis(), equalTo(1L));
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

    @Test
    public void toUserForAuthenticateResponse_withUser_calls_createUserForAuthenticateResponse() throws Exception {
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        userConverterCloudV20.toUserForAuthenticateResponse(new User(), null);
        verify(objectFactory).createUserForAuthenticateResponse();
    }

    @Test
    public void toUserForAuthenticateResponse_withUser_setsUsername() throws Exception {
        User user = new User();
        user.setUsername("username");
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        UserForAuthenticateResponse userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse(user, null);
        assertThat("name", userForAuthenticateResponse.getName(), equalTo("username"));
    }

    @Test
    public void toUserForAuthenticateResponse_withUser_setsId() throws Exception {
        User user = new User();
        user.setId("userId");
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        UserForAuthenticateResponse userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse(user, null);
        assertThat("user id", userForAuthenticateResponse.getId(), equalTo("userId"));
    }

    @Test
    public void toUserForAuthenticateResponse_withUser_whenRolesNotNull_callsRoleConverter() throws Exception {
        User user = new User();
        user.setId("joe.User");
        when(objectFactory.createUserForAuthenticateResponse()).thenReturn(new UserForAuthenticateResponse());
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        userConverterCloudV20.toUserForAuthenticateResponse(user, roles);
        verify(roleConverterCloudV20).toRoleListJaxb(roles);
    }

    @Test
    public void toUserList_withUsers_returnsListofCorrectSize() throws Exception {
        List<User> users = new ArrayList<User>();
        users.add(new User());
        users.add(new User());

        when(objectFactory.createUser()).thenReturn(new org.openstack.docs.identity.api.v2.User());
        when(objectFactory.createUserList()).thenReturn(new UserList());
        UserList userList = userConverterCloudV20.toUserList(users);
        assertThat("user list size", userList.getUser().size(), equalTo(2));
    }

    @Test
    public void toUserList_withZeroUsers_returnsEmptyUserList() throws Exception {
        List<User> users = new ArrayList<User>();

        when(objectFactory.createUser()).thenReturn(new org.openstack.docs.identity.api.v2.User());
        when(objectFactory.createUserList()).thenReturn(new UserList());
        UserList userList = userConverterCloudV20.toUserList(users);
        assertThat("user list size", userList.getUser().size(), equalTo(0));
    }
}
