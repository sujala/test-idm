package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory;
import com.rackspace.idm.api.converter.cloudv20.TenantConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserGroupService;
import com.rackspace.idm.domain.service.UserService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;

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
    private TenantConverterCloudV20 tenantConverterCloudV20;
    private String authToken = "token";

    @Before
    public void setUp() throws Exception {
        defaultCloud20Service = new DefaultCloud20Service();

        userService = mock(UserService.class);
        userGroupService = mock(UserGroupService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);
        tenantConverterCloudV20 = mock(TenantConverterCloudV20.class);
        defaultCloud20Service.setUserService(userService);
        defaultCloud20Service.setUserGroupService(userGroupService);
        defaultCloud20Service.setOBJ_FACTORIES(jaxbObjectFactories);
        defaultCloud20Service.setScopeAccessService(scopeAccessService);
        defaultCloud20Service.setAuthorizationService(authorizationService);
        defaultCloud20Service.setTenantConverterCloudV20(tenantConverterCloudV20);
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
        when(scopeAccessService.getAccessTokenByAuthHeader("bad")).thenReturn(null);
        when(tenantConverterCloudV20.toTenantList(org.mockito.Matchers.<List<Tenant>>any())).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listTenants(null, "bad", null, 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void addEndpoint_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addEndpoint(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addEndpointTemplate_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addEndpointTemplate(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addRole(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addRoleToUserOnTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addRolesToUserOnTenant(null,authToken,null,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addService(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addTenant(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addUser_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addUser(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addUserCredential_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addUserCredential(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void addUserRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addUserRole(null,authToken,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void checkToken_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.checkToken(null,authToken,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteEndpoint_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteEndpoint(null,authToken,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteEndpointTemplate_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteEndpointTemplate(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteRole(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteRoleFromUserOnTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteRoleFromUserOnTenant(null,authToken,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteService(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteTenant(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteUser_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteUser(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteUserCredential_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteUserCredential(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void deleteUserRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteUserRole(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getEndpoint_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getEndpoint(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getEndpointTemplate_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getEndpointTemplate(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getRole(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getSecretQA_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getSecretQA(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getService(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getTenantById_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getTenantById(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getTenantByName_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getTenantByName(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getUserById_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserById(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getUserByName_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserByName(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getUserCredential_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserCredential(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void getUserRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserRole(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listCredentials_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listCredentials(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listEndpoints_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listEndpoints(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listEndpointsForToken_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listEndpointsForToken(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listEndpointTemplates_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listEndpointTemplates(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listRoles_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listRoles(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listRolesForTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listRolesForTenant(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listRolesForUserOnTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listRolesForUserOnTenant(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listServices_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listServices(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listUserGlobalRoles_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserGlobalRoles(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listUserGlobalROlesByServiceId_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserGlobalRolesByServiceId(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listUserGroups_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserGroups(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listUserRoles_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserRoles(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listUsers_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUsers(null,authToken,null,0);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listUsersForTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUsersForTenant(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void listUsersWithRoleForTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUsersWithRoleForTenant(null,authToken,null,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void setUserEnabled_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.setUserEnabled(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void updateSecretQA_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateSecretQA(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void updateTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateTenant(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void updateUser_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateUser(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void updateUserApiKeyCredentials_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateUserApiKeyCredentials(null,authToken,null,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void updateUserPasswordCredentials_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateUserPasswordCredentials(null,authToken,null,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

    @Test
    public void validateToken_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.validateToken(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken);
    }

}
