package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserGroupService;
import com.rackspace.idm.domain.service.UserService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 3:58 PM
 */
public class DefaultCloud20ServiceTest {

    private DefaultCloud20Service defaultCloud20Service;
    private DefaultCloud20Service spy;
    private UserService userService;
    private UserGroupService userGroupService;
    private String userId = "id";
    private User user;
    private JAXBObjectFactories jaxbObjectFactories;
    private ScopeAccessService scopeAccessService;
    private AuthorizationService authorizationService;
    private String authToken = "token";

    @Before
    public void setUp() throws Exception {
        defaultCloud20Service = new DefaultCloud20Service();

        userService = mock(UserService.class);
        userGroupService = mock(UserGroupService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);
        defaultCloud20Service.setUserService(userService);
        defaultCloud20Service.setUserGroupService(userGroupService);
        defaultCloud20Service.setOBJ_FACTORIES(jaxbObjectFactories);
        defaultCloud20Service.setScopeAccessService(scopeAccessService);
        defaultCloud20Service.setAuthorizationService(authorizationService);
        user = new User();
        user.setMossoId(123);
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudAdmin(scopeAccess)).thenReturn(true);
        spy = spy(defaultCloud20Service);
        doNothing().when(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getGroupList_callsUserService() throws Exception {
        spy.listUserGroups(null,authToken, userId);
        verify(userService).getUserById(userId);
    }

    @Test
    public void listUserGroups_withUserNotFound_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null,authToken, userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void listUserGroups_withNullUserPassedIn_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null,authToken, null);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void listUserGroups_withValidUser_returns200() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null,authToken, userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_withValidUser_returnsNonNullEntity() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getEntity(), Matchers.<Object>notNullValue());
    }

    @Test
    public void listUserGroups_withValidUser_returnsAJaxbElement() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken,userId);
        assertThat("code", responseBuilder.build().getEntity(), instanceOf(javax.xml.bind.JAXBElement.class));
    }

    @Test
    public void listUserGroups_withValidUser_invalidMossoId_returns404() throws Exception {
        user.setMossoId(null);
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken,userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void listUserGroups_withValidUser_callsUserGroupService () throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        spy.listUserGroups(null, authToken, userId);
        verify(userGroupService).getGroups(user.getMossoId());
    }

    @Test
    public void listTenants_invalidToken_returns401() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listTenants(null, "bad", null, 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }
}
