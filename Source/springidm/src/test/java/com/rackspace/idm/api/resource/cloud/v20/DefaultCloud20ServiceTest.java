package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TenantConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
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
    private Configuration config;
    private UserService userService;
    private UserGroupService userGroupService;
    private JAXBObjectFactories jaxbObjectFactories;
    private ScopeAccessService scopeAccessService;
    private AuthorizationService authorizationService;
    private TenantService tenantService;
    private EndpointService endpointService;
    private ApplicationService clientService;
    private UserConverterCloudV20 userConverterCloudV20;
    private TenantConverterCloudV20 tenantConverterCloudV20;
    private EndpointConverterCloudV20 endpointConverterCloudV20;
    private String authToken = "token";
    private EndpointTemplate endpointTemplate;
    private String userId = "id";
    private User user;
    private Tenant tenant;
    private String tenantId = "tenantId";
    private CloudBaseUrl baseUrl;
    private Role role;
    private TenantRole tenantRole;
    private ClientRole clientRole;
    private Service service;
    private org.openstack.docs.identity.api.v2.Tenant tenantOS;
    private org.openstack.docs.identity.api.v2.User userOS;
    private CloudBaseUrl cloudBaseUrl;
    private Application application;
    private String roleId = "roleId";

    @Before
    public void setUp() throws Exception {
        defaultCloud20Service = new DefaultCloud20Service();

        //mocks
        userService = mock(UserService.class);
        userGroupService = mock(UserGroupService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        tenantConverterCloudV20 = mock(TenantConverterCloudV20.class);
        endpointConverterCloudV20 = mock(EndpointConverterCloudV20.class);
        tenantService = mock(TenantService.class);
        endpointService = mock(EndpointService.class);
        clientService = mock(ApplicationService.class);
        config = mock(Configuration.class);

        //setting mocks
        defaultCloud20Service.setUserService(userService);
        defaultCloud20Service.setUserGroupService(userGroupService);
        defaultCloud20Service.setOBJ_FACTORIES(jaxbObjectFactories);
        defaultCloud20Service.setScopeAccessService(scopeAccessService);
        defaultCloud20Service.setAuthorizationService(authorizationService);
        defaultCloud20Service.setUserConverterCloudV20(userConverterCloudV20);
        defaultCloud20Service.setTenantConverterCloudV20(tenantConverterCloudV20);
        defaultCloud20Service.setEndpointConverterCloudV20(endpointConverterCloudV20);
        defaultCloud20Service.setTenantService(tenantService);
        defaultCloud20Service.setEndpointService(endpointService);
        defaultCloud20Service.setClientService(clientService);
        defaultCloud20Service.setConfig(config);

        //fields
        user = new User();
        user.setUsername(userId);
        user.setId(userId);
        user.setMossoId(123);
        role = new Role();
        role.setId("roleId");
        role.setName("roleName");
        role.setServiceId("role-ServiceId");
        tenantRole = new TenantRole();
        tenantRole.setClientId("clientId");
        tenantRole.setUserId(userId);
        tenantRole.setRoleRsId("tenantRoleId");
        ScopeAccess scopeAccess = new ScopeAccess();
        tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setBaseUrlIds(new String[]{});
        clientRole = new ClientRole();
        clientRole.setClientId("clientId");
        clientRole.setId("clientRoleId");
        endpointTemplate = new EndpointTemplate();
        endpointTemplate.setId(101);
        service=new Service();
        service.setName("serviceName");
        service.setId("serviceId");
        service.setType("serviceType");
        tenantOS = new org.openstack.docs.identity.api.v2.Tenant();
        tenantOS.setId("tenantName");
        tenantOS.setName("tenantName");
        userOS = new org.openstack.docs.identity.api.v2.User();
        userOS.setId("userName");
        userOS.setUsername("username");
        cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(101);
        cloudBaseUrl.setGlobal(false);
        application = new Application();
        application.setClientId("clientId");

        //stubbing
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(true);
        when(endpointService.getBaseUrlById(101)).thenReturn(cloudBaseUrl);
        when(clientService.getById(role.getServiceId())).thenReturn(application);
        when(clientService.getById("clientId")).thenReturn(application);
        when(clientService.getClientRoleById(role.getId())).thenReturn(clientRole);
        when(clientService.getClientRoleById(tenantRole.getRoleRsId())).thenReturn(clientRole);
        when(tenantService.getTenant(tenantId)).thenReturn(tenant);
        when(userService.getUserById(userId)).thenReturn(user);
        when(config.getString("rackspace.customerId")).thenReturn(null);
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(user);

        spy = spy(defaultCloud20Service);
        doNothing().when(spy).checkXAUTHTOKEN(eq(authToken), anyBoolean(), any(String.class));
        doNothing().when(spy).checkXAUTHTOKEN(eq(authToken), anyBoolean(), eq(tenantId));
    }

    @Test
    public void updateUserPasswordCredentials_withNullPassword_returns400() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername("username");
        Response.ResponseBuilder responseBuilder = spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserPasswordCredentials_withNullUsername_returns400() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername(null);
        creds.setPassword("foo");
        Response.ResponseBuilder responseBuilder = spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserPasswordCredentials_withValidCredentials_callsUserService_updateUserMethod() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername(userId);
        creds.setPassword("ABCdef123");
        spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        verify(userService).updateUser(user, false);
    }
    
    @Test
    public void addUser_callsCheckXAUTHTOKEN_withFlagSetToFalse() throws Exception {
        spy.addUser(null,null,authToken,userOS);
        verify(spy).checkXAUTHTOKEN(authToken, false, null);
    }

    @Test
    public void addUser_callsAddUserRole_whenCallerHasIdentityAdminRole() throws Exception {
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(clientRole);
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn(clientRole.getId());
        spy.addUser(null,null,authToken,userOS);
        verify(spy).addUserRole(null,authToken,user.getId(), clientRole.getId());
    }

    @Test
    public void addUser_callsClientServiceGetClientRoleByClientIdAndName_whenCallerHasIdentityAdminRole() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("roleId");
        spy.addUser(null,null,authToken,userOS);
        verify(clientService).getClientRoleByClientIdAndRoleName(anyString(),anyString());
    }

    @Test
    public void deleteUser_callsUserService_softDeleteUserMethod() throws Exception {
        spy.deleteUser(null,authToken,userId);
        verify(userService).softDeleteUser(any(User.class));
    }

    @Test
    public void addEndpopintTemplate_endPointServiceThrowsBaseUrlConflictException_returns409() throws Exception {
        doThrow(new BaseUrlConflictException()).when(endpointService).addBaseUrl(any(CloudBaseUrl.class));
        Response.ResponseBuilder responseBuilder = spy.addEndpointTemplate(null, null, authToken, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void userDisabledExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userDisabledExceptionResponse("userName");
        JAXBElement<UserDisabledFault> jaxbElement =((JAXBElement<UserDisabledFault>)(responseBuilder.build().getEntity()));
        assertThat("message", jaxbElement.getValue().getMessage(), equalTo("userName"));
    }

    @Test
    public void userDisabledExceptionResponse_returns403() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userDisabledExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void tenantConflictExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.tenantConflictExceptionResponse("responseMessage");
        JAXBElement<TenantConflictFault> jaxbElement =((JAXBElement<TenantConflictFault>)(responseBuilder.build().getEntity()));
        assertThat("message", jaxbElement.getValue().getMessage(), equalTo("responseMessage"));
    }

    @Test
    public void tenantConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.tenantConflictExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void userConflictExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userConflictExceptionResponse("responseMessage");
        JAXBElement<BadRequestFault> jaxbElement =((JAXBElement<BadRequestFault>)(responseBuilder.build().getEntity()));
        assertThat("message", jaxbElement.getValue().getMessage(), equalTo("responseMessage"));
    }

    @Test
    public void userConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userConflictExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void exceptionResponse_withForbiddenException_403() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.exceptionResponse(new ForbiddenException());
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void exceptionResponse_withNotAuthenticatedException_401() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.exceptionResponse(new NotAuthenticatedException());
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void deleteService_callsClientService_deleteMethod() throws Exception {
        spy.deleteService(null,authToken,"clientId");
        verify(clientService).delete("clientId");
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsTenantService_deleteTenantRoleMethod() throws Exception {
        spy.deleteRoleFromUserOnTenant(null,authToken,tenantId,userId,role.getId());
        verify(tenantService).deleteTenantRole(anyString(),any(TenantRole.class));
    }

    @Test
    public void deleteRole_callsClientService_deleteClientRoleMethod() throws Exception {
        spy.deleteRole(null,authToken,role.getId());
        verify(clientService).deleteClientRole(any(ClientRole.class));
    }

    @Test
    public void deleteEndpointTemplate_callsEndpointService_deleteBaseUrlMethod() throws Exception {
        spy.deleteEndpointTemplate(null,authToken,"101");
        verify(endpointService).deleteBaseUrl(101);
    }

    @Test
    public void deleteEndpoint_callsTenantService_updateTenantMethod() throws Exception {
        spy.deleteEndpoint(null,authToken,tenantId,"101");
        verify(tenantService).updateTenant(any(Tenant.class));
    }

    @Test
    public void addUserRole_callsTenantService_addTenantRoleToUserMethod() throws Exception {
        spy.addUserRole(null,authToken,userId,tenantRole.getRoleRsId());
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addUser_withUserMissingUsername_returns400() throws Exception {
        userOS.setUsername(null);
        Response.ResponseBuilder responseBuilder = spy.addUser(null, null, authToken, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addUser_callsUserConverter_toUserDOMethod() throws Exception {
        spy.addUser(null,null,authToken, userOS);
        verify(userConverterCloudV20).toUserDO(userOS);
    }

    @Test
    public void addUser_callsUserService_addUserMethod() throws Exception {
        spy.addUser(null,null,authToken, userOS);
        verify(userService).addUser(user);
    }

    @Test
    public void addTenant_callsTenantService_addTenant() throws Exception {
        spy.addTenant(null,null,authToken,tenantOS);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addTenant_callsTenantConverterCloudV20_toTenantDO() throws Exception {
        spy.addTenant(null,null,authToken,tenantOS);
        verify(tenantConverterCloudV20).toTenantDO(tenantOS);
    }

    @Test
    public void addTenant_withNullTenantName_returns400() throws Exception {
        tenantOS.setName(null);
        Response.ResponseBuilder responseBuilder = spy.addTenant(null, null, authToken, tenantOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addService_callsClientService_add() throws Exception {
        spy.addService(null,null,authToken, service);
        verify(clientService).add(any(Application.class));
    }

    @Test
    public void addService_withNullService_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.addService(null, null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addService_withNullServiceType_returns400() throws Exception {
        service.setType(null);
        Response.ResponseBuilder responseBuilder = spy.addService(null, null, authToken, service);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addService_withNullName_returns400() throws Exception {
        service.setName(null);
        Response.ResponseBuilder responseBuilder = spy.addService(null, null, authToken, service);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addRole_roleWithNullName_returns400() throws Exception {
        Role role1 = new Role();
        role1.setName(null);
        Response.ResponseBuilder responseBuilder = spy.addRole(null, null, authToken, role1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addRole_nullRole_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.addRole(null, null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addRole_roleWithNullServiceId_returns400() throws Exception {
        Role role1 = new Role();
        role1.setName("roleName");
        role1.setServiceId(null);
        Response.ResponseBuilder responseBuilder = spy.addRole(null, null, authToken, role1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addRolesToUserOnTenant_callsTenantService_addTenantRoleToUser() throws Exception {
        spy.addRolesToUserOnTenant(null,authToken,tenantId,userId,role.getId());
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant() throws Exception {
        spy.addEndpoint(null,authToken,tenantId, endpointTemplate);
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant_throw400() throws Exception {
        cloudBaseUrl.setGlobal(true);
        Response.ResponseBuilder responseBuilder = spy.addEndpoint(null,authToken,tenantId, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addEndpointTemplate_callsEndpointService_addBaseUrl() throws Exception {
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(baseUrl);
        spy.addEndpointTemplate(null,null,authToken,endpointTemplate);
        verify(endpointService).addBaseUrl(baseUrl);
    }

    @Test
    public void getGroupList_callsUserService() throws Exception {
        spy.listUserGroups(null,authToken, userId);
        verify(userService).getUserById(userId);
    }

    @Test
    public void addRole_callsClientService_addClientRole() throws Exception {
        spy.addRole(null,null,authToken, role);
        verify(clientService).addClientRole(any(ClientRole.class));
    }

    @Test
    public void listUserGroups_withUserNotFound_returns404() throws Exception {
        when(userService.getUserById(userId)).thenReturn(null);
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
    @Ignore
    public void listTenants_invalidToken_returnsEmptyList() throws Exception {
        when(scopeAccessService.getAccessTokenByAuthHeader("bad")).thenReturn(null);
        when(tenantConverterCloudV20.toTenantList(org.mockito.Matchers.<List<Tenant>>any())).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listTenants(null, "bad", null, 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addEndpoint_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addEndpoint(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addEndpointTemplate_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addEndpointTemplate(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addRole(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addRoleToUserOnTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addRolesToUserOnTenant(null,authToken,null,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addService(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addTenant(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addUser_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addUser(null,null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, false, null);
    }

    @Test
    public void addUserCredential_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addUserCredential(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addUserRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addUserRole(null,authToken,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, false, null);
    }

    @Test
    public void checkToken_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.checkToken(null,authToken,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteEndpoint_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteEndpoint(null,authToken,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteEndpointTemplate_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteEndpointTemplate(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteRole(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteRoleFromUserOnTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteRoleFromUserOnTenant(null,authToken,tenantId,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, tenantId);
    }

    @Test
    public void deleteService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteService(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteTenant(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteUser_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteUser(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteUserCredential_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteUserCredential(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteUserRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.deleteUserRole(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getEndpoint_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getEndpoint(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getEndpointTemplate_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getEndpointTemplate(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getRole(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getSecretQA_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getSecretQA(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getService(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getTenantById_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getTenantById(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getTenantByName_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getTenantByName(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getUserById_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserById(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getUserByName_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserByName(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getUserCredential_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserCredential(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void getUserRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.getUserRole(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listCredentials_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listCredentials(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listEndpoints_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listEndpoints(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, false, null);
    }

    @Test
    public void listEndpointsForToken_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listEndpointsForToken(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listEndpointTemplates_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listEndpointTemplates(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listRoles_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listRoles(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listRolesForTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listRolesForTenant(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listRolesForUserOnTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listRolesForUserOnTenant(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listServices_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listServices(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listUserGlobalRoles_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserGlobalRoles(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listUserGlobalROlesByServiceId_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserGlobalRolesByServiceId(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listUserGroups_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserGroups(null,authToken,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listUsers_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUsers(null,authToken,null,0);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listUsersForTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUsersForTenant(null,authToken,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listUsersWithRoleForTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUsersWithRoleForTenant(null,authToken,null,null,null,0);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void setUserEnabled_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.setUserEnabled(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void updateSecretQA_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateSecretQA(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void updateTenant_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateTenant(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void updateUser_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateUser(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void updateUserApiKeyCredentials_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateUserApiKeyCredentials(null,authToken,null,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void updateUserPasswordCredentials_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.updateUserPasswordCredentials(null,authToken,null,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void validateToken_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.validateToken(null,authToken,null,null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void validatePassword_ValidPassword_succeeds() throws Exception {
        defaultCloud20Service.validatePassword("Ab345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_LessThan8CharactersLong_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("123");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainUpperCaseLetter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("ab345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainLowerCaseLetter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("AB345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainNumericCharacter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("Abcdefghik");
    }
}
