package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TenantConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
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
    private GroupService userGroupService;
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
    private UserForCreate userOS;
    private CloudBaseUrl cloudBaseUrl;
    private Application application;
    private String roleId = "roleId";
    private com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs;
    private Group group;
    private UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        defaultCloud20Service = new DefaultCloud20Service();

        //mocks
        userService = mock(UserService.class);
        userGroupService = mock(GroupService.class);
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
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("access");
        userScopeAccess.setAccessTokenExp(new Date(3000,1,1));
        tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setBaseUrlIds(new String[]{});
        clientRole = new ClientRole();
        clientRole.setClientId("clientId");
        clientRole.setId("clientRoleId");
        endpointTemplate = new EndpointTemplate();
        endpointTemplate.setId(101);
        service = new Service();
        service.setName("serviceName");
        service.setId("serviceId");
        service.setType("serviceType");
        tenantOS = new org.openstack.docs.identity.api.v2.Tenant();
        tenantOS.setId("tenantName");
        tenantOS.setName("tenantName");
        userOS = new UserForCreate();
        userOS.setId("userName");
        userOS.setUsername("username");
        userOS.setEmail("foo@bar.com");
        cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(101);
        cloudBaseUrl.setGlobal(false);
        application = new Application();
        application.setClientId("clientId");
        group = new Group();
        group.setName("Group1");
        group.setDescription("Group Description");
        group.setGroupId(1);
        groupKs = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groupKs.setId("1");
        groupKs.setName("Group1");
        groupKs.setDescription("Group Description");
        uriInfo = mock(UriInfo.class);

        //stubbing
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
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
    public void addUser_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void addUser_callsAddUserRole_whenCallerHasIdentityAdminRole() throws Exception {
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn(clientRole.getId());
        spy.addUser(null, null, authToken, userOS);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test
    public void addUser_callsClientServiceGetClientRoleByClientIdAndName_whenCallerHasIdentityAdminRole() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("roleId");
        spy.addUser(null, null, authToken, userOS);
        verify(clientService).getClientRoleByClientIdAndRoleName(anyString(), anyString());
    }

    @Test
    public void deleteUser_callsUserService_softDeleteUserMethod() throws Exception {
        User user1 = new User();
        User caller = new User();
        user1.setDomainId("domainId");
        caller.setId("id");
        caller.setDomainId("domainId");
        when(userService.getUserById(userId)).thenReturn(user1);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        spy.deleteUser(null, authToken, userId);
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
        JAXBElement<UserDisabledFault> jaxbElement = ((JAXBElement<UserDisabledFault>) (responseBuilder.build().getEntity()));
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
        JAXBElement<TenantConflictFault> jaxbElement = ((JAXBElement<TenantConflictFault>) (responseBuilder.build().getEntity()));
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
        JAXBElement<BadRequestFault> jaxbElement = ((JAXBElement<BadRequestFault>) (responseBuilder.build().getEntity()));
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
        spy.deleteService(null, authToken, "clientId");
        verify(clientService).delete("clientId");
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsTenantService_deleteTenantRoleMethod() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken,tenantId);
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).deleteTenantRole(anyString(), any(TenantRole.class));
    }

    @Test
    public void deleteRole_callsClientService_deleteClientRoleMethod() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.deleteRole(null, authToken, role.getId());
        verify(clientService).deleteClientRole(any(ClientRole.class));
    }

    @Test
    public void deleteRole_serviceAdminCallerDoesNotHaveAccess_throwsForbiddenException() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        Response.ResponseBuilder responseBuilder = spy.deleteRole(null, authToken, "roleId");
        assertThat("code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void deleteRole_withNullRole_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.deleteRole(null, authToken, null);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void deleteEndpointTemplate_callsEndpointService_deleteBaseUrlMethod() throws Exception {
        spy.deleteEndpointTemplate(null, authToken, "101");
        verify(endpointService).deleteBaseUrl(101);
    }

    @Test
    public void deleteEndpoint_callsTenantService_updateTenantMethod() throws Exception {
        spy.deleteEndpoint(null, authToken, tenantId, "101");
        verify(tenantService).updateTenant(any(Tenant.class));
    }

    @Test
    public void addUserRole_callsTenantService_addTenantRoleToUserMethod() throws Exception {
        spy.addUserRole(null, authToken, userId, tenantRole.getRoleRsId());
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
        spy.addUser(null, null, authToken, userOS);
        verify(userConverterCloudV20).toUserDO(userOS);
    }

    @Test
    public void addUser_callsUserService_addUserMethod() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(userService).addUser(user);
    }

    @Test
    public void addUser_callsSetDomainId() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(spy).setDomainId(any(ScopeAccess.class), any(User.class));
    }

    @Test
    public void addTenant_callsTenantService_addTenant() throws Exception {
        spy.addTenant(null, null, authToken, tenantOS);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addTenant_callsTenantConverterCloudV20_toTenantDO() throws Exception {
        spy.addTenant(null, null, authToken, tenantOS);
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
        spy.addService(null, null, authToken, service);
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
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken,tenantId);
        spy.addRolesToUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant() throws Exception {
        spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant_throw400() throws Exception {
        cloudBaseUrl.setGlobal(true);
        Response.ResponseBuilder responseBuilder = spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void addEndpointTemplate_callsEndpointService_addBaseUrl() throws Exception {
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(baseUrl);
        spy.addEndpointTemplate(null, null, authToken, endpointTemplate);
        verify(endpointService).addBaseUrl(baseUrl);
    }

    @Ignore
    @Test
    public void getGroupList_callsUserService() throws Exception {
        spy.listUserGroups(null, authToken, userId);
        verify(userService).getUserById(userId);
    }

    @Test
    public void addRole_callsClientService_addClientRole() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.addRole(null, null, authToken, role);
        verify(clientService).addClientRole(any(ClientRole.class));
    }

    @Ignore
    @Test
    public void listUserGroups_withUserNotFound_returns404() throws Exception {
        when(userService.getUserById(userId)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Ignore
    @Test
    public void listUserGroups_withNullUserPassedIn_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, null);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Ignore
    @Test
    public void listUserGroups_withValidUser_returns200() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Ignore
    @Test
    public void listUserGroups_withValidUser_returnsNonNullEntity() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getEntity(), Matchers.<Object>notNullValue());
    }

    @Ignore
    @Test
    public void listUserGroups_withValidUser_returnsAJaxbElement() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getEntity(), instanceOf(javax.xml.bind.JAXBElement.class));
    }

    @Ignore
    @Test
    public void listUserGroups_withValidUser_invalidMossoId_returns404() throws Exception {
        user.setMossoId(null);
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Ignore
    @Test
    public void listUserGroups_withValidUser_callsUserGroupService() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        spy.listUserGroups(null, authToken, userId);
        verify(userGroupService).getGroups("1", 100);
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
    public void addEndpoint_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.addEndpoint(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addEndpointTemplate_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.addEndpointTemplate(null, null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        Role role1 = new Role();
        role1.setServiceId("id");
        spy.addRole(null, null, authToken, role1);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addRoleToUserOnTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addRolesToUserOnTenant(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addService(null, null, authToken, null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addTenant_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.addTenant(null, null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addUser_callerIsUserAdmin_setsMossoAndNastId() throws Exception {
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        User caller = new User();
        caller.setMossoId(123);
        caller.setNastId("nastId");
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        doNothing().when(spy).setDomainId(any(ScopeAccess.class), any(User.class));
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setUsername("userforcreate");
        userForCreate.setEmail("user@rackspace.com");
        spy.addUser(null, null, authToken, userForCreate);
        verify(userService).addUser(argument.capture());
        assertThat("nast id", argument.getValue().getNastId(), equalTo("nastId"));
        assertThat("mosso id", argument.getValue().getMossoId(), equalTo(123));
    }

    @Test
    public void addUser_callerIsNotUserAdmin_doesNotSetMossoAndNastId() throws Exception {
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        User caller = new User();
        caller.setMossoId(123);
        caller.setNastId("nastId");
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        doNothing().when(spy).setDomainId(any(ScopeAccess.class), any(User.class));
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setUsername("userforcreate");
        userForCreate.setEmail("user@rackspace.com");
        spy.addUser(null, null, authToken, userForCreate);
        verify(userService).addUser(argument.capture());
        assertThat("nast id", argument.getValue().getNastId(), not(equalTo("nastId")));
        assertThat("mosso id", argument.getValue().getMossoId(), not(equalTo(123)));
    }

    @Test
    public void addUserCredential_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addUserCredential(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addUserRole_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addUserRole(null, authToken, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void checkToken_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.checkToken(null, authToken, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteEndpoint_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.deleteEndpoint(null, authToken, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteEndpointTemplate_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.deleteEndpointTemplate(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.deleteRole(null, authToken, roleId);
        verify(spy).verifyServiceAdminLevelAccess(anyString());
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteService_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteService(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteTenant(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUser_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.deleteUser(null, authToken, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUser_userAdmin_differentDomain_throwsForbiddenException() throws Exception {
        when(userService.getUserById("dude")).thenReturn(new User());
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.deleteUser(null, authToken, "dude");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void deleteUserCredential_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.deleteUserCredential(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUserRole_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.deleteUserRole(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getEndpoint_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getEndpoint(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getEndpointTemplate_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getEndpointTemplate(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getRole_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getRole(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getSecretQA_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getSecretQA(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getService_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getService(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getTenantById_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getTenantById(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getTenantByName_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getTenantByName(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getUserById_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getUserById(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getUserByName_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getUserByName(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getUserCredential_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getUserCredential(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getUserRole_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getUserRole(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listCredentials(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listEndpoints_verifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpoints(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listEndpointTemplates_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpointTemplates(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRoles_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listRoles(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRolesForTenant_verifyServiceAdminLevelAccess() throws Exception {
        spy.listRolesForTenant(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRolesForUserOnTenant_verifyServiceAdminLevelAccess() throws Exception {
        spy.listRolesForUserOnTenant(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listServices_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.listServices(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUserGlobalRoles_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.listUserGlobalRoles(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUserGlobalROlesByServiceId_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.listUserGlobalRolesByServiceId(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Ignore
    @Test
    public void listUserGroups_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.listUserGroups(null, authToken, null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void listUsers_callerIsNotDefaultUser_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(false);
        spy.listUsers(null, authToken, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUsers_callerIsNotUserAdmin_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        spy.listUsers(null, authToken, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUsersForTenant_CallsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUsersForTenant(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUsersWithRoleForTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUsersWithRoleForTenant(null, authToken, null, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void setUserEnabled_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.setUserEnabled(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateSecretQA_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateSecretQA(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateTenant(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateUser_callsverifyUserAdminLevelAccess() throws Exception {
        spy.updateUser(null, authToken, null, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_missingUsername_throwsBadRequestException() throws Exception {
        defaultCloud20Service.validateUser(new org.openstack.docs.identity.api.v2.User());
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_missingEmail_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail2_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail3_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail4_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@bar.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail2_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("racker@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail3_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("john.smith@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail4_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("john.\"elGuapo\".smith@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail5_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail6_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@1.net");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail7_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@1.rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail8_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("R_a_c_K_e_r_4000@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void updateUserApiKeyCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateUserApiKeyCredentials(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateUserPasswordCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateUserPasswordCredentials(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
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

    @Test
    public void setDomainId_callsAuthorizationService_authorizeCloudUserAdmin() throws Exception {
        ScopeAccess scopeAccessByAccessToken = new ScopeAccess();
        User userDO = new User();
        defaultCloud20Service.setDomainId(scopeAccessByAccessToken, userDO);
        verify(authorizationService).authorizeCloudUserAdmin(scopeAccessByAccessToken);
    }

    @Test
    public void setDomainId_callerIsUserAdmin_callsUserService() throws Exception {
        User userDO = new User();
        userDO.setUsername("dude");
        Attribute[] attributes = {new Attribute(LdapRepository.ATTR_UID, "dude")};
        ScopeAccess scopeAccessByAccessToken = mock(ScopeAccess.class);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)).thenReturn(true);
        when(userService.getUser("dude")).thenReturn(userDO);
        when(scopeAccessByAccessToken.getLDAPEntry()).thenReturn(new SearchResultEntry("", attributes, new Control[]{}));
        defaultCloud20Service.setDomainId(scopeAccessByAccessToken, userDO);
        verify(userService).getUser("dude");
    }

    @Test(expected = NotAuthorizedException.class)
    public void checkXAuthToken_withNullToken_throwsNotAuthorizedException() throws Exception {
        defaultCloud20Service.verifyServiceAdminLevelAccess(null);
    }

    @Test(expected = NotAuthorizedException.class)
    public void checkXAuthToken_withBlankToken_throwsNotAuthorizedException() throws Exception {
        defaultCloud20Service.verifyServiceAdminLevelAccess("");
    }

    @Test
    public void checkXAuthToken_identityAdminCaller_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        when(scopeAccessService.getScopeAccessByAccessToken("admin")).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        defaultCloud20Service.verifyServiceAdminLevelAccess("admin");
    }

    @Test(expected = ForbiddenException.class)
    public void checkXAuthToken_userAdminCaller_throwsForbiddenException() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        when(scopeAccessService.getScopeAccessByAccessToken("admin")).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud20Service.verifyServiceAdminLevelAccess("admin");
    }

    @Test
    public void validateToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.validateToken(null, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(null);
    }

    @Test
    public void listTenants_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listTenants(null, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(null);
    }

    @Test
    public void listEndpointsForToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpointsForToken(null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(null);
    }

    @Test
    public void addGroup_callsCloudGroupBuilder() throws Exception {
        CloudGroupBuilder cloudGrpBuilder = mock(CloudGroupBuilder.class);
        defaultCloud20Service.setCloudGroupBuilder(cloudGrpBuilder);
    }

    @Test
    public void addGroup_validGroup_returns201() throws Exception{
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        CloudKsGroupBuilder cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        when(cloudGroupBuilder.build(null)).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromPath("path"));
        defaultCloud20Service.setCloudGroupBuilder(cloudGroupBuilder);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null ,uriInfo, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addGroup_duplicateGroup_returns409() throws Exception{
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        when(cloudGroupBuilder.build((com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group) any())).thenReturn(group);
        doThrow(new DuplicateException()).when(userGroupService).addGroup(org.mockito.Matchers.<Group>any());
        defaultCloud20Service.setCloudGroupBuilder(cloudGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null, null, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addGroup_emptyName_returns400() throws Exception{
        groupKs.setName("");
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null ,null, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void verifyTokenHasTenantAccess_callsTenantService() throws Exception {
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        Tenant tenant1 = new Tenant();
        tenant1.setTenantId("1");
        list.add(tenant1);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(list);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken,tenant1.getTenantId());
        verify(tenantService).getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifyTokenHasTenantAccess_NoTenants_throwsException() throws Exception {
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(new ArrayList<Tenant>());
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken, null);
    }

    @Test
    public void verifyTokenHasTenantAccess_WithMatchingTenant_succeeds() throws Exception {
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        Tenant tenant1 = new Tenant();
        tenant1.setTenantId("1");
        list.add(tenant1);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(list);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken,tenant1.getTenantId());
    }

    @Test
    public void validateKsGroup_validGroup(){
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_invalidGroup_throwsBadRequest(){
        groupKs.setName("");
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test
    public void validateKsGroup_invalidGroupLength_throwsBadRequestMessage(){
        groupKs.setName("Invalidnamellllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
        try{
        defaultCloud20Service.validateKsGroup(groupKs);
        }catch(Exception e){
            assertThat("Exception",e.getMessage(),equalTo("Group name length cannot exceed 200 characters"));

        }
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_invalidGroupLength_throwsBadRequest(){
        groupKs.setName("Invalidnamellllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test
    public void validateGroupId_validGroupId(){
        defaultCloud20Service.validateGroupId("1");
    }

    @Test
    public void validateGroupId_validGroupIdwithSpaces(){
        defaultCloud20Service.validateGroupId("  1   ");
    }

    @Test(expected = BadRequestException.class)
    public void validateGroupId_inValidGroupId(){
        defaultCloud20Service.validateGroupId("a");
    }

    @Test
    public void validateGroupId_inValidGroupId_throwBadRequest(){
        try{
        defaultCloud20Service.validateGroupId(" ");
        }catch(Exception e){
            assertThat("Exception",e.getMessage(),equalTo("Invalid group id"));
        }
    }

    @Test
    public void validateGroupId_inValidGroupIdWithSpaces_throwBadRequest(){
        try{
        defaultCloud20Service.validateGroupId(" a ");
        }catch(Exception e){
            assertThat("Exception",e.getMessage(),equalTo("Invalid group id"));
        }
    }

    @Test
    public void listUsers_callerIsUserAdmin_returns200() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.listUsers(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsers_callerIsDefaultUser_returns200() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.listUsers(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }
}
