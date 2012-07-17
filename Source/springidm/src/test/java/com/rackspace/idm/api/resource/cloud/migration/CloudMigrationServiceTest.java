package com.rackspace.idm.api.resource.cloud.migration;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.*;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.MigrationClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.v20.CloudKsGroupBuilder;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import com.sun.jersey.api.ConflictException;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 4/25/12
 * Time: 9:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloudMigrationServiceTest {
    private CloudMigrationService cloudMigrationService;
    private CloudMigrationService spy;
    private Configuration config;
    private ApplicationService applicationService;
    private EndpointService endpointService;
    private JAXBObjectFactories jaxbObjectFactories;
    private TenantService tenantService;
    private UserConverterCloudV20 userConverterCloudV20;
    private UserService userService;
    private GroupService groupService;
    private MigrationClient client;
    private GregorianCalendar gc = new GregorianCalendar();
    private static DatatypeFactory df = null;
    private RoleConverterCloudV20 roleConverterCloudV20;
    private ScopeAccessService scopeAccessService;
    private EndpointConverterCloudV20 endpointConverterCloudV20;
    private CloudBaseUrl cloudBaseUrl;
    private User user;
    private org.openstack.docs.identity.api.v2.User cloudUser;
    private CloudKsGroupBuilder cloudKsGroupBuilder;
    AtomHopperClient atomHopperClient;

    @Before
    public void setUp() throws Exception {
        cloudMigrationService = new CloudMigrationService();
        endpointConverterCloudV20 = new EndpointConverterCloudV20();
        cloudBaseUrl = new CloudBaseUrl();
        //mocks
        config = mock(Configuration.class);
        applicationService = mock(ApplicationService.class);
        client = mock(MigrationClient.class);
        endpointService = mock(EndpointService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        tenantService = mock(TenantService.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        userService = mock(UserService.class);
        groupService = mock(GroupService.class);
        roleConverterCloudV20 = mock(RoleConverterCloudV20.class);
        scopeAccessService = mock(ScopeAccessService.class);
        cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        atomHopperClient = mock(AtomHopperClient.class);

        //setting mocks
        cloudMigrationService.setApplicationService(applicationService);
        cloudMigrationService.setClient(client);
        cloudMigrationService.setConfig(config);
        cloudMigrationService.setEndpointService(endpointService);
        cloudMigrationService.setOBJ_FACTORIES(jaxbObjectFactories);
        cloudMigrationService.setTenantService(tenantService);
        cloudMigrationService.setUserConverterCloudV20(userConverterCloudV20);
        cloudMigrationService.setUserService(userService);
        cloudMigrationService.setCloudGroupService(groupService);
        cloudMigrationService.setRoleConverterCloudV20(roleConverterCloudV20);
        cloudMigrationService.setScopeAccessService(scopeAccessService);
        cloudMigrationService.setEndpointConverterCloudV20(endpointConverterCloudV20);
        cloudMigrationService.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        cloudMigrationService.setAtomHopperClient(atomHopperClient);
        gc.setTimeInMillis(new Date().getTime());

        //setting mocks for endpointconverter
        endpointConverterCloudV20.setOBJ_FACTORIES(jaxbObjectFactories);

        //fields
        user = new User();
        user.setEnabled(true);
        user.setUsername("username");
        user.setId("1");
        user.setMossoId(123);
        user.setInMigration(false);
        user.setSecretQuestion("Question");
        user.setApiKey("APIKEY");
        user.setDisplayName("name");
        user.setEmail("email@test.rackspacec.com");

        df = DatatypeFactory.newInstance();
        cloudUser = new org.openstack.docs.identity.api.v2.User();
        cloudUser.setId("1");
        cloudUser.setDisplayName("Migrate User");
        cloudUser.setEmail("migrate@me.com");
        cloudUser.setEnabled(true);
        cloudUser.setUsername("migrateUser");
        cloudUser.setCreated(df.newXMLGregorianCalendar(gc));
        cloudUser.setUpdated(df.newXMLGregorianCalendar(gc));

        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setUniqueId("uniqueId");

        //stubbing
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        when(config.getString("rackspace.customerId")).thenReturn(null);
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");

        spy = spy(cloudMigrationService);
    }

    @Test
    public void migratedBaseURLs_callsAddOrUpdateEndpointTemplates() throws Exception {
        doNothing().when(spy).addOrUpdateEndpointTemplates(anyString());
        doReturn("").when(spy).getAdminToken();
        spy.migrateBaseURLs();
        verify(spy).addOrUpdateEndpointTemplates(anyString());
    }

    @Test (expected = NotFoundException.class)
    public void getMigratedUserList_usersIsNull_throwsNotFoundException() throws Exception {
        spy.getMigratedUserList();
    }

    @Test
    public void getMigratedUserList_responseOk_returns200() throws Exception {
        Users users = new Users();
        when(userService.getAllUsers(any(FilterParam[].class))).thenReturn(users);
        Response.ResponseBuilder responseBuilder = spy.getMigratedUserList();
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test (expected = NotFoundException.class)
    public void getInMigrationUserList_usersIsNull_throwsNotFoundException() throws Exception {
        spy.getInMigrationUserList();
    }

    @Test
    public void getInMigrationUserList_responseOk_returns200() throws Exception {
        when(userService.getAllUsers(any(FilterParam[].class))).thenReturn(new Users());
        Response.ResponseBuilder responseBuilder = spy.getInMigrationUserList();
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test (expected = NotFoundException.class)
    public void getMigratedUserRoles_userIsNull_throwsNotFoundException() throws Exception {
        spy.getMigratedUserRoles("");
    }

    @Test
    public void getMigratedUserRoles_responseOk_returns200() throws Exception {
        when(userService.getUser("")).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getMigratedUserRoles("");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test (expected = NotFoundException.class)
    public void getMigratedUserEndpoints_userIsNull_throwsNotFoundException() throws Exception {
        spy.getMigratedUserEndpoints("");
    }

    @Test
    public void getMigratedUserEndpoints_responseOk_returns200() throws Exception {
        user.setUniqueId("uniqueId");
        when(userService.getUser("")).thenReturn(user);
        doReturn(new EndpointList()).when(spy).getEndpointsForUser("uniqueId");
        Response.ResponseBuilder responseBuilder = spy.getMigratedUserEndpoints("");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getEndpointsForUser_returnsCorrectEndpointList() throws Exception {
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setTenantId("tenantId");
        openstackEndpoint.setTenantName("tenantName");
        openstackEndpoint.setBaseUrls(cloudBaseUrlList);
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        endpoints.add(openstackEndpoint);
        when(config.getString("cloudAuth.clientId")).thenReturn("bde1268ebabeeabb70a0e702a4626977c331d5c4");
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(any(ScopeAccess.class))).thenReturn(endpoints);
        EndpointList list = spy.getEndpointsForUser("userId");
        assertThat("Tenant Id", list.getEndpoint().get(0).getTenantId(), equalTo("tenantId"));
    }

    @Test
    public void migrateGroups_callsAddOrUpdateGroups() throws Exception {
        Group group = new Group();
        group.setName("group");
        doReturn("").when(spy).getAdminToken();
        when(groupService.getGroupById(anyInt())).thenReturn(group);
        spy.migrateGroups();
        verify(spy).addOrUpdateGroups(anyString());
    }

    @Test(expected = NotFoundException.class)
    public void getMigratedUser_returns_userNotFound() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getMigratedUser("migrateTestUserNotFound");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getMigratedUser_returns_User() throws Exception {
        when(userService.getUser("cmarin3")).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getMigratedUser("cmarin3");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test(expected = ConflictException.class)
    public void migrateUserByUsername_returns_ConflicException() throws Exception {
        when(userService.userExistsByUsername(anyString())).thenReturn(true);
        spy.migrateUserByUsername("conflictingUser", false, null);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void migrateUserByUsername_getAdminTokenReturnsBlankString_throwsNotAuthenticated() throws Exception {
        doReturn("").when(spy).getAdminToken();
        spy.migrateUserByUsername("", false, null);
    }

    @Test (expected = NotFoundException.class)
    public void migrateUserByUsername_client11UsernameCanNotBeFound_throwsNotFoundException() throws Exception {
        doReturn("asdf").when(spy).getAdminToken();
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenThrow(new JAXBException("EXCEPTION"));
        spy.migrateUserByUsername("userNotExist", false, null);
    }

    @Test (expected = NotFoundException.class)
    public void migrateUserByUsername_clientGetUserAndUsernameCanNotBeFound_throwsNotFoundException() throws Exception {
        doReturn("token").when(spy).getAdminToken();
        doThrow(new NotFoundException()).when(client).getUser(anyString(), anyString());
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Test (expected = ConflictException.class)
    public void migrateUserByUsername_usernameAlreadyExists_throwsConflictException() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(new com.rackspacecloud.docs.auth.api.v1.User());
        doReturn("asdf").when(spy).getAdminToken();
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        ArrayList<String> subUsers = new ArrayList<String>();
        subUsers.add("subUserName1");
        subUsers.add("subUserName2");
        doReturn(subUsers).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        when(userService.userExistsByUsername(anyString())).thenReturn(false).thenReturn(false).thenReturn(true);
        doReturn(true).when(spy).isSubUser(any(RoleList.class));
        spy.migrateUserByUsername("cmarin2", true, "1");
    }

    @Test (expected = BadRequestException.class)
    public void migrateUserByUsername_domainIdIsNullAndIsSubUser_throwsBadRequestException() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(new com.rackspacecloud.docs.auth.api.v1.User());
        doReturn("token").when(spy).getAdminToken();
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        doReturn(true).when(spy).isSubUser(any(RoleList.class));
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Test
    public void migrateUserByUsername_enableIsTrue_callsUserServiceUpdateUserById() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(SecretQA.class),any(RoleList.class), any(Groups.class), anyList());
        spy.migrateUserByUsername("cmarin2", true, "1");
        verify(userService).updateUserById(any(User.class), eq(false));
    }

    @Test
    public void migrateUserByUsername_CloudBaseUrlBaseUrlTypeIsNAST_succeedsWithNoExceptions() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        cloudBaseUrl.setBaseUrlType("NAST");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void migrateUserByUsername_CloudBaseUrlGetBaseUrlTypeIsMOSSO_succeedsWithNoExceptions() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        cloudBaseUrl.setBaseUrlType("MOSSO");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void migrateUserByUsername_userDoesNotExist_returnsWithNoExceptions() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void migrateUserByUsername_succeedsWithNoExceptions() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        when(client.getSecretQA(anyString(), anyString())).thenThrow(new NotFoundException());
        doReturn("token").when(spy).getAdminToken();
        ArrayList<String> subUsers = new ArrayList<String>();
        subUsers.add("subUserName");
        doReturn(subUsers).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        when(userService.userExistsByUsername(anyString())).thenReturn(false);
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void migrateUserByUsername_withPassword_usesGivenPassword() throws Exception {
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        when(client.getUserCredentials(anyString(), anyString())).thenReturn(new CredentialListType());
        doReturn("password").when(spy).getPassword(any(CredentialListType.class));
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(tenantService.getTenant(anyString())).thenReturn(null);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        spy.migrateUserByUsername("cmarin2", false, "1");
        verify(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), eq("password"), any(SecretQA.class), anyString());
    }

    @Test( expected = ConflictException.class)
    public void migrateUserByUsername_throwsConflictExceptions() throws Exception {
        doThrow(new ConflictException()).when(spy).migrateUserByUsername("user", false, null);
        spy.migrateUserByUsername("user", false);
    }

    @Test
    public void migrateUserByUsername_NonClinflictException_UnmigratesUser() throws Exception {
        doThrow(new Exception()).when(spy).migrateUserByUsername("user", false, null);
        try {
            spy.migrateUserByUsername("user", false);
        } catch (Exception e) {}
        verify(spy).unmigrateUserByUsername("user");
    }

    @Test
    public void migrateUserByUsername_getsUserFromUserService() throws Exception {
        doReturn("asdf").when(spy).getAdminToken();
        when(userService.getUser(anyString())).thenReturn(new User());
        MigrateUserResponseType toBeReturned = new MigrateUserResponseType();
        toBeReturned.getUsers().add(new UserType());
        doReturn(toBeReturned).when(spy).migrateUserByUsername("user", false, null);
        spy.migrateUserByUsername("user", false);
        verify(userService).getUser(anyString());
    }

    @Test
    public void migrateUserByUsername_callsAtomHopper_postAsync() throws Exception {
        doReturn("asdf").when(spy).getAdminToken();
        when(userService.getUser(anyString())).thenReturn(new User());
        MigrateUserResponseType toBeReturned = new MigrateUserResponseType();
        toBeReturned.getUsers().add(new UserType());
        doReturn(toBeReturned).when(spy).migrateUserByUsername("user", false, null);
        spy.migrateUserByUsername("user", false);
        verify(atomHopperClient).asyncPost(any(User.class), anyString(), anyString(), anyString());
    }

    @Test
    public void migrateUserByUsername_withNoUserCredentials_GeneratesRandomPassword() throws Exception {
        doReturn("asdf").when(spy).getAdminToken();
        when(userService.getUser(anyString())).thenReturn(new User());
        MigrateUserResponseType toBeReturned = new MigrateUserResponseType();
        toBeReturned.getUsers().add(new UserType());
        doReturn(toBeReturned).when(spy).migrateUserByUsername("user", false, null);
        doThrow(new RuntimeException()).when(spy).getApiKey(any(CredentialListType.class));
        spy.migrateUserByUsername("user", false);
        verify(spy, never()).getPassword(any(CredentialListType.class));
    }

    @Test
    public void isSubUser_identityNotMatchName_returnsFalse() throws Exception {
        RoleList roles = new RoleList();
        Role role = new Role();
        role.setName("notMatch");
        List<Role> roleList = roles.getRole();
        roleList.add(role);
        boolean subUser = cloudMigrationService.isSubUser(roles);
        assertThat("boolean", subUser, equalTo(false));
    }

    @Test
    public void isSubUser_identityMatches_returnsTrue() throws Exception {
        RoleList roles = new RoleList();
        Role role = new Role();
        role.setName("identity:default");
        List<Role> roleList = roles.getRole();
        roleList.add(role);
        boolean subUser = cloudMigrationService.isSubUser(roles);
        assertThat("boolean", subUser, equalTo(true));
    }


    @Test (expected = NotFoundException.class)
    public void setMigratedUserEnabledStatus_userIsNull_throwsNotFoundException() throws Exception {
        when(userService.getUser(null)).thenReturn(null);
        spy.setMigratedUserEnabledStatus(null, false);
    }

    @Test (expected = NotFoundException.class)
    public void setMigratedUserEnabledStatus_userInMigrationIsNull_throwsNotFoundException() throws Exception {
        when(userService.getUser(null)).thenReturn(new User());
        spy.setMigratedUserEnabledStatus(null, false);
    }

    @Test
    public void setMigratedUserEnabledStatus_callsUserServiceUpdateUserById() throws Exception {
        user.setInMigration(true);
        when(userService.getUser(null)).thenReturn(user);
        spy.setMigratedUserEnabledStatus(null, true);
        verify(userService).updateUserById(any(User.class), eq(false));
    }

    @Test (expected = NotFoundException.class)
    public void unmigrateUserByUsername_userIsNull_throwsNotFoundException() throws Exception {
        spy.unmigrateUserByUsername(null);
    }

    @Test (expected = NotFoundException.class)
    public void unmigrateUserByUsername_userGetInMigrationIsNull_throwsNotFoundException() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        spy.unmigrateUserByUsername("username");
    }

    @Test (expected = NotFoundException.class)
    public void unmigrateUserByUsername_NoUsersInDomain_throwsNotFoundException() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(userService.getAllUsers(any(FilterParam[].class), eq(0), eq(0))).thenReturn(new Users());
        spy.unmigrateUserByUsername("username");
    }

    @Test (expected = NotFoundException.class)
    public void unmigrateUserByUsername_usersWithNullSubUsers_throwsNotFoundException() throws Exception {
        User user1 = new User();
        user1.setInMigration(true);
        when(userService.getUser("username")).thenReturn(user1);
        when(userService.getAllUsers(any(FilterParam[].class), eq(0), eq(0))).thenReturn(new Users());
        spy.unmigrateUserByUsername("username");
    }

    @Test (expected = ConflictException.class)
    public void unmigrateUserByUsername_usersWithSubUsers_withNullInMigration_throwsConflictException() throws Exception {
        User user1 = new User();
        user1.setInMigration(true);
        when(userService.getUser("username")).thenReturn(user1);
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        users.getUsers().add(new User());
        when(userService.getAllUsers(any(FilterParam[].class), eq(0), eq(0))).thenReturn(users);
        spy.unmigrateUserByUsername("username");
    }

    @Test
    public void unmigrateUserByUsername_usersNoUsersInDomain_neverCallsUserService_deleteUser() throws Exception {
        User user1 = new User();
        user1.setInMigration(true);
        when(userService.getUser("username")).thenReturn(user1);
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        when(userService.getAllUsers(any(FilterParam[].class), eq(0), eq(0))).thenReturn(users);
        spy.unmigrateUserByUsername("username");
        verify(userService, never()).deleteUser(anyString());
    }

    @Test
    public void unmigrateUserByUsername_usersWithSubUsers_callsUserService_deleteUser() throws Exception {
        User user1 = new User();
        user1.setInMigration(true);
        when(userService.getUser("username")).thenReturn(user1);
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        User user2 = new User();
        user2.setInMigration(false);
        users.getUsers().add(user2);
        when(userService.getAllUsers(any(FilterParam[].class), eq(0), eq(0))).thenReturn(users);
        spy.unmigrateUserByUsername("username");
        verify(userService).deleteUser(anyString());
    }

    @Test
    public void getGroups_callsCloudKsGroupBuilder_reponseOk() throws Exception {
        Group group = new Group();
        List<Group> groups = new ArrayList<Group>();
        groups.add(group);
        when(groupService.getGroups("", 0)).thenReturn(groups);
        when(cloudKsGroupBuilder.build(group)).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group());
        Response.ResponseBuilder result = spy.getGroups();
        assertThat("response code", result.build().getStatus(), equalTo(200));
    }

    @Test
    public void getGroups_callsGroupService_responseOk() throws Exception {
        List<Group> groups = new ArrayList<Group>();
        when(groupService.getGroups("", 0)).thenReturn(groups);
        Response.ResponseBuilder result = spy.getGroups();
        assertThat("response code", result.build().getStatus(), equalTo(200));
    }

    @Test(expected = ConflictException.class)
    public void getSubUsers_withDisabledUserAdmin_throwsConflictException() throws Exception {
        RoleList roles = new RoleList();
        Role role = new Role();
        role.setName("identity:user-admin");
        roles.getRole().add(role);
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setEnabled(false);
        spy.getSubUsers(user, "", "", roles);
    }

    @Test
    public void getSubUsers_withNonUserAdmin_returnsZeroSubUsers() throws Exception {
        RoleList roles = new RoleList();
        Role role = new Role();
        role.setName("identity:default-user");
        roles.getRole().add(role);
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setEnabled(false);
        List<String> subUsers = spy.getSubUsers(user, "", "", roles);
        assertThat("returned sub users", subUsers.size(), equalTo(0));
    }

    @Test(expected = ConflictException.class)
    public void getSubUsers_clientException_throwsConflictException() throws Exception {
        doReturn(true).when(spy).isUserAdmin(any(RoleList.class));
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setEnabled(true);
        AuthenticateResponse authResponse = new AuthenticateResponse();
        authResponse.setToken(new Token());
        doReturn(authResponse).when(spy).authenticate(anyString(), anyString(), anyString());
        when(client.getUsers(anyString())).thenThrow(new JAXBException("EXCEPTION"));
        spy.getSubUsers(user, "", "", new RoleList());
    }

    @Test
    public void getSubUsers_withNullUsers_returnsEmptyArray() throws Exception {
        doReturn(true).when(spy).isUserAdmin(any(RoleList.class));
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setEnabled(true);
        AuthenticateResponse authResponse = new AuthenticateResponse();
        authResponse.setToken(new Token());
        doReturn(authResponse).when(spy).authenticate(anyString(), anyString(), anyString());
        when(client.getUsers(anyString())).thenReturn(null);
        List<String> subUsers = spy.getSubUsers(user, "", "", new RoleList());
        assertThat("sub users", subUsers.size(), equalTo(0));
    }

    @Test
    public void getSubUsers_withUsers_returnsArrayOfSubUsers() throws Exception {
        doReturn(true).when(spy).isUserAdmin(any(RoleList.class));
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setEnabled(true);
        AuthenticateResponse authResponse = new AuthenticateResponse();
        authResponse.setToken(new Token());
        doReturn(authResponse).when(spy).authenticate(anyString(), anyString(), anyString());
        UserList userList = new UserList();
        when(client.getUsers(anyString())).thenReturn(userList);
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("someUserName");
        userList.getUser().add(user1);
        List<String> subUsers = spy.getSubUsers(user, "", "", new RoleList());
        assertThat("sub users", subUsers.size(), equalTo(1));
    }

    @Test
    public void getSubUsers_withUsers_returnsArrayOfSubUsers_withoutAdminUser() throws Exception {
        doReturn(true).when(spy).isUserAdmin(any(RoleList.class));
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setEnabled(true);
        user.setUsername("adminUser");
        AuthenticateResponse authResponse = new AuthenticateResponse();
        authResponse.setToken(new Token());
        doReturn(authResponse).when(spy).authenticate(anyString(), anyString(), anyString());
        UserList userList = new UserList();
        when(client.getUsers(anyString())).thenReturn(userList);
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("someUserName");
        org.openstack.docs.identity.api.v2.User user2 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("adminUser");
        userList.getUser().add(user1);
        userList.getUser().add(user2);
        List<String> subUsers = spy.getSubUsers(user, "", "", new RoleList());
        assertThat("sub users", subUsers.size(), equalTo(1));
    }

    @Test
    public void getSubUsers_withNonAdminUser_returnsEmptyList() throws Exception {
        List<String> subUsers = spy.getSubUsers(null, "", "", new RoleList());
        assertThat("sub users", subUsers.size(), equalTo(0));
    }

    @Test
    public void validateBaseUrlRefs_withBaseUrls_withCorrospondingEndpoint_setsValidEndpoint() throws Exception {
        ArrayList<BaseURLRef> baseUrlRefs = new ArrayList<BaseURLRef>();
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(12345);
        baseUrlRefs.add(baseURLRef);
        EndpointList newEndpoints = new EndpointList();
        Endpoint endpoint = new Endpoint();
        endpoint.setId(12345);
        newEndpoints.getEndpoint().add(endpoint);
        UserType result = new UserType();
        spy.validateBaseUrlRefs(baseUrlRefs, newEndpoints, result);
        assertThat("result endpoints", result.getEndpoints().get(0).isValid(), equalTo(true));
    }

    @Test
    public void validateBaseUrlRefs_withBaseUrls_withOutCorrospondingEndpoint_setsInvalidEndpoint() throws Exception {
        ArrayList<BaseURLRef> baseUrlRefs = new ArrayList<BaseURLRef>();
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(123456);
        baseUrlRefs.add(baseURLRef);
        EndpointList newEndpoints = new EndpointList();
        Endpoint endpoint = new Endpoint();
        endpoint.setId(12345);
        newEndpoints.getEndpoint().add(endpoint);
        UserType result = new UserType();
        spy.validateBaseUrlRefs(baseUrlRefs, newEndpoints, result);
        assertThat("result endpoints", result.getEndpoints().get(0).isValid(), equalTo(false));
        assertThat("result endpoints", result.getEndpoints().get(0).getName(), nullValue());
    }

    @Test
    public void validateBaseUrlRefs_withBaseUrls_withEmptyEndpointList_returnsInvalidEndpointForEachBaseUrl() throws Exception {
        ArrayList<BaseURLRef> baseUrlRefs = new ArrayList<BaseURLRef>();
        BaseURLRef baseURLRef = new BaseURLRef();
        BaseURLRef baseURLRef2 = new BaseURLRef();
        baseURLRef.setId(123456);
        baseURLRef2.setId(123457);
        baseUrlRefs.add(baseURLRef);
        baseUrlRefs.add(baseURLRef2);
        EndpointList newEndpoints = new EndpointList();
        UserType result = new UserType();
        spy.validateBaseUrlRefs(baseUrlRefs, newEndpoints, result);
        assertThat("result endpoints", result.getEndpoints().size(), equalTo(2));
        assertThat("result endpoints", result.getEndpoints().get(1).isValid(), equalTo(false));
    }

    @Test
    public void validateBaseUrlRefs_withNoBaseUrls_returnsEmptyEndpointList() throws Exception {
        ArrayList<BaseURLRef> baseUrlRefs = new ArrayList<BaseURLRef>();
        EndpointList newEndpoints = new EndpointList();
        UserType result = new UserType();
        spy.validateBaseUrlRefs(baseUrlRefs, newEndpoints, result);
        assertThat("result endpoints", result.getEndpoints().size(), equalTo(0));
    }

    @Test
    public void validateUser_withValidUser_setsIsValidToTrue() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setId("userId");
        user1.setUsername("userName");
        user1.setEmail("userEmail");
        User user2 = new User();
        user2.setId("userId");
        user2.setUsername("userName");
        user2.setEmail("userEmail");
        user2.setApiKey("apiKey");
        user2.setPassword("password");
        user2.setSecretAnswer("secretAnswer");
        user2.setSecretQuestion("secretQuestion");
        when(userService.getUser(anyString())).thenReturn(user2);
        SecretQA secretQA = new SecretQA();
        secretQA.setAnswer("secretAnswer");
        secretQA.setQuestion("secretQuestion");
        doNothing().when(spy).validateRoles(anyList(), any(RoleList.class), any(UserType.class));
        doNothing().when(spy).validateGroups(any(Groups.class), anyList(), any(UserType.class));
        doReturn(null).when(spy).getEndpointsForUser(anyString());
        doNothing().when(spy).validateBaseUrlRefs(anyList(), any(EndpointList.class), any(UserType.class));
        UserType userType = spy.validateUser(user1, "apiKey", "password", secretQA, null, null, null);
        assertThat("userType result", userType.isValid(), equalTo(true));
    }

    @Test
    public void validateUser_withValidUser_withNoSecretQA_setsIsValidToTrue() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setId("userId");
        user1.setUsername("userName");
        user1.setEmail("userEmail");
        User user2 = new User();
        user2.setId("userId");
        user2.setUsername("userName");
        user2.setEmail("userEmail");
        user2.setApiKey("apiKey");
        user2.setPassword("password");
        user2.setSecretAnswer("secretAnswer");
        user2.setSecretQuestion("secretQuestion");
        when(userService.getUser(anyString())).thenReturn(user2);
        doNothing().when(spy).validateRoles(anyList(), any(RoleList.class), any(UserType.class));
        doNothing().when(spy).validateGroups(any(Groups.class), anyList(), any(UserType.class));
        doReturn(null).when(spy).getEndpointsForUser(anyString());
        doNothing().when(spy).validateBaseUrlRefs(anyList(), any(EndpointList.class), any(UserType.class));
        UserType userType = spy.validateUser(user1, "apiKey", "password", null, null, null, null);
        assertThat("userType result", userType.isValid(), equalTo(true));
    }

    @Test
    public void validateUser_withDifferingUsers_setsValidToFalse() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setId("asfwefawefa");
        user1.setUsername("userName");
        user1.setEmail("userEmail");
        User user2 = new User();
        user2.setId("userId");
        user2.setUsername("userName");
        user2.setEmail("userEmail");
        user2.setApiKey("apiKey");
        user2.setPassword("password");
        user2.setSecretAnswer("secretAnswer");
        user2.setSecretQuestion("secretQuestion");
        when(userService.getUser(anyString())).thenReturn(user2);
        SecretQA secretQA = new SecretQA();
        secretQA.setAnswer("secretAnswer");
        secretQA.setQuestion("secretQuestion");
        doNothing().when(spy).validateRoles(anyList(), any(RoleList.class), any(UserType.class));
        doNothing().when(spy).validateGroups(any(Groups.class), anyList(), any(UserType.class));
        doReturn(null).when(spy).getEndpointsForUser(anyString());
        doNothing().when(spy).validateBaseUrlRefs(anyList(), any(EndpointList.class), any(UserType.class));
        UserType userType = spy.validateUser(user1, "apiKey", "password", secretQA, null, null, null);
        assertThat("userType result", userType.isValid(), equalTo(false));
    }

    @Test
    public void validateUser_callsOtherValidates() throws Exception {
        when(userService.getUser(anyString())).thenReturn(new User());
        SecretQA secretQA = new SecretQA();
        doNothing().when(spy).validateRoles(anyList(), any(RoleList.class), any(UserType.class));
        doNothing().when(spy).validateGroups(any(Groups.class), anyList(), any(UserType.class));
        doReturn(null).when(spy).getEndpointsForUser(anyString());
        doNothing().when(spy).validateBaseUrlRefs(anyList(), any(EndpointList.class), any(UserType.class));

        UserType userType = spy.validateUser(new org.openstack.docs.identity.api.v2.User(), "apiKey", "password", secretQA, null, null, null);
        verify(spy).validateRoles(anyList(), any(RoleList.class), eq(userType));
        verify(spy).validateGroups(any(Groups.class), anyList(), eq(userType));
        verify(spy).validateBaseUrlRefs(anyList(), any(EndpointList.class), eq(userType));
        assertThat("userType result", userType.isValid(), equalTo(false));
    }

    @Test
    public void validateGroups_withNewGroupSameAsOld_returnsValidGroupResponse() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groups.getGroup().add(group);
        ArrayList<Group> newGroups = new ArrayList<Group>();
        Group newGroup = new Group();
        newGroups.add(newGroup);
        group.setName("groupName");
        group.setId("123456");
        newGroup.setName("groupName");
        newGroup.setGroupId(123456);

        UserType result = new UserType();
        spy.validateGroups(groups, newGroups, result);
        assertThat("userType result", result.getGroups().get(0).isValid(), equalTo(true));
    }

    @Test
    public void validateGroups_withNewGroupDifferentIdFromOld_returnsNotValidGroupResponse() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groups.getGroup().add(group);
        ArrayList<Group> newGroups = new ArrayList<Group>();
        Group newGroup = new Group();
        newGroups.add(newGroup);
        group.setName("groupName");
        group.setId("12345");
        newGroup.setName("groupName");
        newGroup.setGroupId(123456);

        UserType result = new UserType();
        spy.validateGroups(groups, newGroups, result);
        assertThat("userType result", result.getGroups().get(0).isValid(), equalTo(false));
    }


    @Test
    public void validateGroups_withNewGroupDifferentNameFromOld_returnsNotValidGroupResponse() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groups.getGroup().add(group);
        ArrayList<Group> newGroups = new ArrayList<Group>();
        Group newGroup = new Group();
        newGroups.add(newGroup);
        group.setName("groupName2");
        group.setId("123456");
        newGroup.setName("groupName");
        newGroup.setGroupId(123456);

        UserType result = new UserType();
        spy.validateGroups(groups, newGroups, result);
        assertThat("userType result", result.getGroups().get(0).isValid(), equalTo(false));
    }

    @Test
    public void validateGroups_withNoNewGroups_returnsNotValidGroupResponseForEachOldGroup() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group2 = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groups.getGroup().add(group);
        groups.getGroup().add(group2);
        group.setName("groupName");
        group.setId("12345");
        group.setName("groupName2");
        group.setId("123456");

        ArrayList<Group> newGroups = new ArrayList<Group>();


        UserType result = new UserType();
        spy.validateGroups(groups, newGroups, result);
        assertThat("userType result", result.getGroups().get(0).isValid(), equalTo(false));
        assertThat("userType result", result.getGroups().size(), equalTo(2));
    }



    @Test
    public void validateGroups_withNoOldGroups_returnsEmptyList() throws Exception {
        Groups groups = new Groups();
        ArrayList<Group> newGroups = new ArrayList<Group>();


        UserType result = new UserType();
        spy.validateGroups(groups, newGroups, result);
        assertThat("userType result", result.getGroups().size(), equalTo(0));
    }

    @Test
    public void validateRoles_withValidRole_setsIsValidTrue() throws Exception {
        RoleList newRoles = new RoleList();
        Role role = new Role();
        role.setName("roleName");
        role.setId("roleId");
        newRoles.getRole().add(role);
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("roleName");
        tenantRole.setRoleRsId("roleId");
        roles.add(tenantRole);


        UserType userType = new UserType();
        spy.validateRoles(roles, newRoles, userType);
        assertThat("usertype result", userType.getRoles().get(0).isValid(), equalTo(true));
    }

    @Test
    public void validateRoles_withDifferentRoleName_returnsNotValidRole() throws Exception {
        RoleList newRoles = new RoleList();
        Role role = new Role();
        role.setName("roleName");
        role.setId("roleId");
        newRoles.getRole().add(role);
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("roleName2");
        tenantRole.setRoleRsId("roleId");
        roles.add(tenantRole);


        UserType userType = new UserType();
        spy.validateRoles(roles, newRoles, userType);
        assertThat("usertype result", userType.getRoles().get(0).isValid(), equalTo(false));
    }

    @Test
    public void validateRoles_withDifferentRoleId_returnsNotValidRole() throws Exception {
        RoleList newRoles = new RoleList();
        Role role = new Role();
        role.setName("roleName");
        role.setId("roleId");
        newRoles.getRole().add(role);
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("roleName");
        tenantRole.setRoleRsId("roleId2");
        roles.add(tenantRole);


        UserType userType = new UserType();
        spy.validateRoles(roles, newRoles, userType);
        assertThat("usertype result", userType.getRoles().get(0).isValid(), equalTo(false));
    }

    @Ignore
    @Test
    public void validateRoles_withNoNewRoles_returnsNotValidForEachOldRole() throws Exception {
        RoleList newRoles = new RoleList();
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("roleName");
        tenantRole.setRoleRsId("roleId2");
        roles.add(tenantRole);
        roles.add(tenantRole);


        UserType userType = new UserType();
        spy.validateRoles(roles, newRoles, userType);
        assertThat("usertype result size", userType.getRoles().size(), equalTo(2));
    }

    @Test
    public void validateRoles_withNoOldRoles_returnsNoRoles() throws Exception {
        RoleList newRoles = new RoleList();
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();


        UserType userType = new UserType();
        spy.validateRoles(roles, newRoles, userType);
        assertThat("usertype result size", userType.getRoles().size(), equalTo(0));
    }

    @Test
    public void getApiKey_withEmptyCredsList_returnsEmptyString() throws Exception {
        CredentialListType credentialListType = mock(CredentialListType.class);
        when(credentialListType.getCredential()).thenReturn(new ArrayList<JAXBElement<? extends CredentialType>>());
        String apiKey = cloudMigrationService.getApiKey(credentialListType);
        assertThat("apikey", apiKey, equalTo(""));
    }

    @Test
    public void getApiKey_withNoApiCredsList_returnsEmptyString() throws Exception {
        CredentialListType credentialListType = mock(CredentialListType.class);
        ArrayList<JAXBElement<? extends CredentialType>> jaxbElements = new ArrayList<JAXBElement<? extends CredentialType>>();
        JAXBObjectFactories jaxbObjectFactories1 = new JAXBObjectFactories();
        jaxbElements.add(jaxbObjectFactories1.getOpenStackIdentityV2Factory().createCredential(new PasswordCredentialsRequiredUsername()));
        when(credentialListType.getCredential()).thenReturn(jaxbElements);
        String apiKey = cloudMigrationService.getApiKey(credentialListType);
        assertThat("apikey", apiKey, equalTo(""));
    }

    @Test
    public void getApiKey_withApiCredsList_returnsApiKey() throws Exception {
        CredentialListType credentialListType = mock(CredentialListType.class);
        ArrayList<JAXBElement<? extends CredentialType>> jaxbElements = new ArrayList<JAXBElement<? extends CredentialType>>();
        JAXBObjectFactories jaxbObjectFactories1 = new JAXBObjectFactories();
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setApiKey("apiKey");

        jaxbElements.add(jaxbObjectFactories1.getOpenStackIdentityV2Factory().createCredential(new PasswordCredentialsRequiredUsername()));
        jaxbElements.add(jaxbObjectFactories1.getOpenStackIdentityV2Factory().createCredential(apiKeyCredentials));
        when(credentialListType.getCredential()).thenReturn(jaxbElements);
        String apiKey = cloudMigrationService.getApiKey(credentialListType);
        assertThat("apikey", apiKey, equalTo("apiKey"));
    }

    @Test
    public void getApiKey_withNull_returnsEmptyApiKey() throws Exception {
        CredentialListType credentialListType = null;
        String apiKey = cloudMigrationService.getApiKey(credentialListType);
        assertThat("apikey", apiKey, equalTo(""));
    }

    @Test
    public void getPassword_withEmptyCredsList_returnsEmptyString() throws Exception {
        CredentialListType credentialListType = mock(CredentialListType.class);
        when(credentialListType.getCredential()).thenReturn(new ArrayList<JAXBElement<? extends CredentialType>>());
        String apiKey = cloudMigrationService.getPassword(credentialListType);
        assertThat("password", apiKey, equalTo(""));
    }

    @Test
    public void getPassword_withNoPasswordCreds_returnsEmptyString() throws Exception {
        CredentialListType credentialListType = mock(CredentialListType.class);
        ArrayList<JAXBElement<? extends CredentialType>> jaxbElements = new ArrayList<JAXBElement<? extends CredentialType>>();
        JAXBObjectFactories jaxbObjectFactories1 = new JAXBObjectFactories();
        jaxbElements.add(jaxbObjectFactories1.getOpenStackIdentityV2Factory().createCredential(new ApiKeyCredentials()));
        when(credentialListType.getCredential()).thenReturn(jaxbElements);
        String apiKey = cloudMigrationService.getPassword(credentialListType);
        assertThat("password", apiKey, equalTo(""));
    }

    @Test
    public void getPassword_withPasswordCreds_returnsApiKey() throws Exception {
        CredentialListType credentialListType = mock(CredentialListType.class);
        ArrayList<JAXBElement<? extends CredentialType>> jaxbElements = new ArrayList<JAXBElement<? extends CredentialType>>();
        JAXBObjectFactories jaxbObjectFactories1 = new JAXBObjectFactories();
        PasswordCredentialsRequiredUsername passwordCredentials = new PasswordCredentialsRequiredUsername();
        passwordCredentials.setPassword("password");

        jaxbElements.add(jaxbObjectFactories1.getOpenStackIdentityV2Factory().createCredential(new ApiKeyCredentials()));
        jaxbElements.add(jaxbObjectFactories1.getOpenStackIdentityV2Factory().createCredential(passwordCredentials));
        when(credentialListType.getCredential()).thenReturn(jaxbElements);
        String apiKey = cloudMigrationService.getPassword(credentialListType);
        assertThat("password", apiKey, equalTo("password"));
    }

    @Test
    public void getPassword_withNull_returnsEmptyPassword() throws Exception {
        CredentialListType credentialListType = null;
        String apiKey = cloudMigrationService.getPassword(credentialListType);
        assertThat("password", apiKey, equalTo(""));
    }

    @Test
    public void authenticate_withApiKey_callsClientAuthWithApi() throws Exception {
        cloudMigrationService.authenticate("username", "apiKey", "password");
        verify(client).authenticateWithApiKey("username", "apiKey");
    }

    @Test
    public void authenticate_withPasswordWithoutApiKey_callsClientAuthWithPassword() throws Exception {
        cloudMigrationService.authenticate("username", "", "password");
        verify(client).authenticateWithPassword("username", "password");
    }

    @Test(expected = BadRequestException.class)
    public void authenticate_withNeitherPasswordNorApiKey_throwsBadRequestException() throws Exception {
        cloudMigrationService.authenticate("username", "", "");
    }

    @Test(expected = BadRequestException.class)
    public void
    authenticate_withClientException_throwsBadRequestException() throws Exception {
        when(client.authenticateWithApiKey("username", "apiKey")).thenThrow(new IOException());
        cloudMigrationService.authenticate("username", "apiKey", "");
    }

    @Test
    public void getAdminToken_callsConfig_getString() throws Exception {
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setToken(new Token());
        when(client.authenticateWithApiKey(anyString(), anyString())).thenReturn(authenticateResponse);
        cloudMigrationService.getAdminToken();
        verify(config).getString("migration.username");
        verify(config).getString("migration.apikey");
    }

    @Test
    public void getAdminToken_callsClient_authenticateWithApiKey() throws Exception {
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setToken(new Token());
        when(client.authenticateWithApiKey(anyString(), anyString())).thenReturn(authenticateResponse);
        cloudMigrationService.getAdminToken();
        verify(client).authenticateWithApiKey(anyString(), anyString());
    }

    @Test
    public void getAdminToken_returnsTokenId() throws Exception {
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        Token token = new Token();
        token.setId("tokenId");
        authenticateResponse.setToken(token);
        when(client.authenticateWithApiKey(anyString(), anyString())).thenReturn(authenticateResponse);
        String adminToken = cloudMigrationService.getAdminToken();
        assertThat("admin token", adminToken, equalTo("tokenId"));
    }

    @Test(expected = NotAuthenticatedException.class)
    public void getAdminToken_throwsNotAuthenticatedExceptionOnException() throws Exception {
        when(client.authenticateWithApiKey(anyString(), anyString())).thenThrow(new IOException());
        cloudMigrationService.getAdminToken();
    }

    @Test
    public void addMigrationUser_returnsNewUserSetCorrectly() throws Exception {
        SecretQA secretQA = new SecretQA();
        secretQA.setAnswer("answer");
        secretQA.setQuestion("question");
        org.openstack.docs.identity.api.v2.User user2 = new org.openstack.docs.identity.api.v2.User();
        user2.setCreated(new XMLGregorianCalendarImpl());
        user2.setUpdated(new XMLGregorianCalendarImpl());
        user2.setEmail("email");
        User user1 = cloudMigrationService.addMigrationUser(user2, 12345, "nastId", "apiKey", "password", secretQA, "domainId");
        assertThat("user mosso id", user1.getMossoId(), equalTo(12345));
        assertThat("user nast id", user1.getNastId(), equalTo("nastId"));
        assertThat("user apiKey", user1.getApiKey(), equalTo("apiKey"));
        assertThat("user password", user1.getPassword(), equalTo("password"));
        assertThat("user secret answer", user1.getSecretAnswer(), equalTo("answer"));
        assertThat("user secret question", user1.getSecretQuestion(), equalTo("question"));
        assertThat("user domain id", user1.getDomainId(), equalTo("domainId"));
        assertThat("user email", user1.getEmail(), equalTo("email"));
        assertThat("user created", user1.getCreated(), not(nullValue()));
        assertThat("user updated", user1.getUpdated(), not(nullValue()));
    }

    @Test
    public void addMigrationUser_WithNulls_callsAddUser() throws Exception {
        cloudMigrationService.addMigrationUser(new org.openstack.docs.identity.api.v2.User(), 12345, "nastId", "apiKey","password", null, null);
        verify(userService).addUser(any(User.class));
    }

    @Test
    public void addMigrationUser_callsUserService_addUser() throws Exception {
        cloudMigrationService.addMigrationUser(new org.openstack.docs.identity.api.v2.User(), 12345, "nastId", "apiKey","password", new SecretQA(), "domainId");
        verify(userService).addUser(any(User.class));
    }

    @Test
    public void addUserGroups_callsCloudGroupService_addGroupToUser_forEachGroup() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setId("123456");
        groups.getGroup().add(group);
        groups.getGroup().add(group);
        groups.getGroup().add(group);
        cloudMigrationService.addUserGroups("userId", groups);
        verify(groupService, times(3)).addGroupToUser(anyInt(), eq("userId"));
    }

    @Test
    public void addUserGroups_withZeroGroups_callsGroupServiceZeroTimes() throws Exception {
        Groups groups = new Groups();
        cloudMigrationService.addUserGroups("userId", groups);
        verify(groupService, never()).addGroupToUser(anyInt(), eq("userId"));
    }

    @Test
    public void addUserGroups_withException_consumesException() throws Exception {
        Groups groups = new Groups();
        groups.getGroup().add(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group());
        cloudMigrationService.addUserGroups("userId", groups);
        verify(groupService, never()).addGroupToUser(anyInt(), eq("userId"));
    }

    @Test
    public void addUserGlobalRoles_withMatchingGlobalAndClientRoles_addsRole() throws Exception {
        ArrayList<ClientRole> clientRoles = new ArrayList<ClientRole>();
        ClientRole clientRole = new ClientRole();
        clientRole.setName("Role");
        clientRoles.add(clientRole);
        RoleList roleList = new RoleList();
        Role role = new Role();
        role.setName("Role");
        roleList.getRole().add(role);
        when(applicationService.getAllClientRoles(null)).thenReturn(clientRoles);
        cloudMigrationService.addUserGlobalRoles(new User(), roleList);
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addUserGlobalRoles_withNotMatchingGlobalAndClientRoles_addsNoRoles() throws Exception {
        ArrayList<ClientRole> clientRoles = new ArrayList<ClientRole>();
        ClientRole clientRole = new ClientRole();
        clientRole.setName("Role");
        clientRoles.add(clientRole);
        RoleList roleList = new RoleList();
        Role role = new Role();
        role.setName("Role2");
        roleList.getRole().add(role);
        when(applicationService.getAllClientRoles(null)).thenReturn(clientRoles);
        cloudMigrationService.addUserGlobalRoles(new User(), roleList);
        verify(tenantService, never()).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addUserGlobalRoles_withNoClientRoles_addsNoRoles() throws Exception {
        RoleList roleList = new RoleList();
        Role role = new Role();
        role.setName("Role2");
        roleList.getRole().add(role);
        when(applicationService.getAllClientRoles(null)).thenReturn(new ArrayList<ClientRole>());
        cloudMigrationService.addUserGlobalRoles(new User(), roleList);
        verify(tenantService, never()).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addUserGlobalRoles_withNoRoleList_addsNoRoles() throws Exception {
        when(applicationService.getAllClientRoles(null)).thenReturn(new ArrayList<ClientRole>());
        cloudMigrationService.addUserGlobalRoles(new User(), new RoleList());
        verify(tenantService, never()).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addOrUpdateGroups_withPreExistingGroup_withDifferences_callsCloudGroupService_updateGroup() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setId("123456");
        group.setName("groupNameNew");
        group.setDescription("groupDescriptionNew");
        groups.getGroup().add(group);
        when(client.getGroups("adminToken")).thenReturn(groups);
        Group group1 = new Group();
        group1.setName("groupName");
        group1.setDescription("groupDescription");
        when(groupService.getGroupById(123456)).thenReturn(group1);
        cloudMigrationService.addOrUpdateGroups("adminToken");
        verify(groupService).updateGroup(any(Group.class));
    }

    @Test
    public void addOrUpdateGroups_withPreExistingGroup_withNameDifferences_callsCloudGroupService_updateGroup() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setId("123456");
        group.setName("groupNameNew");
        group.setDescription("groupDescription");
        groups.getGroup().add(group);
        when(client.getGroups("adminToken")).thenReturn(groups);
        Group group1 = new Group();
        group1.setName("groupName");
        group1.setDescription("groupDescription");
        when(groupService.getGroupById(123456)).thenReturn(group1);
        cloudMigrationService.addOrUpdateGroups("adminToken");
        verify(groupService).updateGroup(any(Group.class));
    }

    @Test
    public void addOrUpdateGroups_withPreExistingGroup_withDescriptionDifferences_callsCloudGroupService_updateGroup() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setId("123456");
        group.setName("groupName");
        group.setDescription("groupDescriptionNew");
        groups.getGroup().add(group);
        when(client.getGroups("adminToken")).thenReturn(groups);
        Group group1 = new Group();
        group1.setName("groupName");
        group1.setDescription("groupDescription");
        when(groupService.getGroupById(123456)).thenReturn(group1);
        cloudMigrationService.addOrUpdateGroups("adminToken");
        verify(groupService).updateGroup(any(Group.class));
    }

    @Test
    public void addOrUpdateGroups_withPreExistingGroup_withNoChanges_doesNotUpdateGroup() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setId("123456");
        group.setName("groupName");
        group.setDescription("groupDescription");
        groups.getGroup().add(group);
        when(client.getGroups("adminToken")).thenReturn(groups);
        Group group1 = new Group();
        group1.setName("groupName");
        group1.setDescription("groupDescription");
        when(groupService.getGroupById(123456)).thenReturn(group1);
        cloudMigrationService.addOrUpdateGroups("adminToken");
        verify(groupService, never()).updateGroup(any(Group.class));
    }


    @Test
    public void addOrUpdateGroups_withNoExistingGroup_addsNewGroup() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setId("123456");
        group.setName("groupName");
        group.setDescription("groupDescription");
        groups.getGroup().add(group);
        when(client.getGroups("adminToken")).thenReturn(groups);
        when(groupService.getGroupById(123456)).thenThrow(new NotFoundException());
        cloudMigrationService.addOrUpdateGroups("adminToken");
        verify(groupService).insertGroup(any(Group.class));
    }

    @Test
    public void addOrUpdateGroups_withNoDescription_setsDescriptionToName() throws Exception {
        Groups groups = new Groups();
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setId("123456");
        group.setName("groupName");
        groups.getGroup().add(group);
        when(client.getGroups("adminToken")).thenReturn(groups);
        when(groupService.getGroupById(123456)).thenThrow(new NotFoundException());
        cloudMigrationService.addOrUpdateGroups("adminToken");
        ArgumentCaptor<Group> groupArgumentCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupService).insertGroup(groupArgumentCaptor.capture());
        assertThat("group description", groupArgumentCaptor.getValue().getDescription(), equalTo("groupName"));
    }

    @Test
    public void addOrUpdateEndpointTemplates_withNullCloudBaseUrl_withNullBaseUrl() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setVersion(new VersionForService());
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate);
        when(client.getEndpointTemplates(anyString())).thenReturn(endpointTemplateList);
        when(client.getBaseUrls(anyString(), anyString())).thenThrow(new NotFoundException());
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(null);

        cloudMigrationService.addOrUpdateEndpointTemplates(null);
        verify(endpointService).addBaseUrl(any(CloudBaseUrl.class));
    }

    @Test
    public void addOrUpdateEndpointTemplates_withNullCloudBaseUrl_withBaseUrl() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setVersion(new VersionForService());
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate);
        when(client.getEndpointTemplates(anyString())).thenReturn(endpointTemplateList);
        BaseURLList baseURLList = new BaseURLList();
        BaseURL baseURL = new BaseURL();
        baseURL.setId(0);
        baseURL.setUserType(com.rackspacecloud.docs.auth.api.v1.UserType.CLOUD);
        baseURL.setDefault(true);
        baseURLList.getBaseURL().add(baseURL);
        when(client.getBaseUrls(anyString(), anyString())).thenReturn(baseURLList);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(null);

        cloudMigrationService.addOrUpdateEndpointTemplates(null);
        ArgumentCaptor<CloudBaseUrl> cloudBaseUrlArgumentCaptor = ArgumentCaptor.forClass(CloudBaseUrl.class);
        verify(endpointService).addBaseUrl(cloudBaseUrlArgumentCaptor.capture());
        assertThat("cloud base Url type", cloudBaseUrlArgumentCaptor.getValue().getBaseUrlType(), equalTo("CLOUD"));
    }

    @Test
    public void addOrUpdateEndpointTemplates_withCloudBaseUrl_withBaseUrl_preservesUid() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setVersion(new VersionForService());
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate);
        when(client.getEndpointTemplates(anyString())).thenReturn(endpointTemplateList);
        BaseURLList baseURLList = new BaseURLList();
        BaseURL baseURL = new BaseURL();
        baseURL.setId(0);
        baseURL.setUserType(com.rackspacecloud.docs.auth.api.v1.UserType.CLOUD);
        baseURL.setDefault(true);
        baseURLList.getBaseURL().add(baseURL);
        when(client.getBaseUrls(anyString(), anyString())).thenReturn(baseURLList);
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setUniqueId("someUniqueId");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl1);

        cloudMigrationService.addOrUpdateEndpointTemplates(null);
        ArgumentCaptor<CloudBaseUrl> cloudBaseUrlArgumentCaptor = ArgumentCaptor.forClass(CloudBaseUrl.class);
        verify(endpointService).updateBaseUrl(cloudBaseUrlArgumentCaptor.capture());
        assertThat("cloud base Url type", cloudBaseUrlArgumentCaptor.getValue().getBaseUrlType(), equalTo("CLOUD"));
        assertThat("cloud base UID", cloudBaseUrlArgumentCaptor.getValue().getUniqueId(), equalTo("someUniqueId"));
    }

    @Test
    public void addOrUpdateEndpointTemplates_withCloudBaseUrl_withNullBaseUrl_preservesUid() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setVersion(new VersionForService());
        endpointTemplate.setPublicURL("http://www.public.com");
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate);
        when(client.getEndpointTemplates(anyString())).thenReturn(endpointTemplateList);
        BaseURLList baseURLList = new BaseURLList();
        when(client.getBaseUrls(anyString(), anyString())).thenReturn(baseURLList);
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setUniqueId("someUniqueId");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl1);

        cloudMigrationService.addOrUpdateEndpointTemplates(null);
        ArgumentCaptor<CloudBaseUrl> cloudBaseUrlArgumentCaptor = ArgumentCaptor.forClass(CloudBaseUrl.class);
        verify(endpointService).updateBaseUrl(cloudBaseUrlArgumentCaptor.capture());
        assertThat("cloud base Url type", cloudBaseUrlArgumentCaptor.getValue().getBaseUrlType(), equalTo("MOSSO"));
        assertThat("cloud base UID", cloudBaseUrlArgumentCaptor.getValue().getUniqueId(), equalTo("someUniqueId"));
    }

    @Test
    public void addTenantRole_withApplication_withClientRoles_addsTenantRoleToUser() throws Exception {
        when(endpointService.getBaseUrlById(123456)).thenReturn(new CloudBaseUrl());
        when(applicationService.getByName(anyString())).thenReturn(new Application());
        ArrayList<ClientRole> clientRoles = new ArrayList<ClientRole>();
        clientRoles.add(new ClientRole());
        when(applicationService.getClientRolesByClientId(anyString())).thenReturn(clientRoles);
        cloudMigrationService.addTenantRole(new User(), "tenantId", 123456);
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addTenantRole_withApplication_withNoClientRoles_NeverAddsTenantRoleToUser() throws Exception {
        when(endpointService.getBaseUrlById(123456)).thenReturn(new CloudBaseUrl());
        when(applicationService.getByName(anyString())).thenReturn(new Application());
        when(applicationService.getClientRolesByClientId(anyString())).thenReturn(new ArrayList<ClientRole>());
        cloudMigrationService.addTenantRole(new User(), "tenantId", 123456);
        verify(tenantService, never()).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void migrateRoles_callsMigrationClient_getRoles() throws Exception {
        RoleList roleList = new RoleList();
        doReturn("adminToken").when(spy).getAdminToken();
        when(client.getRoles("adminToken")).thenReturn(roleList);
        spy.migrateRoles();
        verify(client).getRoles("adminToken");
    }

    @Test
    public void migrateRoles_callsAdminToken_throwsException_doesNothing() throws Exception {
        spy.migrateRoles();
        verify(spy).getAdminToken();
    }

    @Test
    public void migrateRoles_callsApplicationService_getClientRoleById() throws Exception {
        RoleList roleList = new RoleList();
        Role role = new Role();
        role.setId("id");
        List<Role> roles = roleList.getRole();
        roles.add(role);
        doReturn("adminToken").when(spy).getAdminToken();
        when(client.getRoles("adminToken")).thenReturn(roleList);
        spy.migrateRoles();
        verify(applicationService).getClientRoleById("id");
    }

    @Test
    public void migrateRoles_foundClientRoleAndDescriptionMatchesAndNameMatches_doesNotUpdateClientRole() throws Exception {
        RoleList roleList = new RoleList();
        Role role = new Role();
        role.setId("id");
        role.setName("name");
        role.setDescription("description");
        List<Role> roles = roleList.getRole();
        roles.add(role);
        ClientRole clientRole = new ClientRole();
        clientRole.setDescription("description");
        clientRole.setName("name");
        doReturn("adminToken").when(spy).getAdminToken();
        when(client.getRoles("adminToken")).thenReturn(roleList);
        when(applicationService.getClientRoleById("id")).thenReturn(clientRole);
        spy.migrateRoles();
        verify(applicationService, times(0)).updateClientRole(any(ClientRole.class));
    }

    @Test
    public void migrateRoles_foundClientRoleAndDescriptionNotMatch_callsApplicationServiceUpdateClientRole() throws Exception {
        RoleList roleList = new RoleList();
        Role role = new Role();
        role.setId("id");
        role.setName("name");
        role.setDescription("notDescription");
        List<Role> roles = roleList.getRole();
        roles.add(role);
        ClientRole clientRole = new ClientRole();
        clientRole.setDescription("description");
        clientRole.setName("name");
        doReturn("adminToken").when(spy).getAdminToken();
        when(client.getRoles("adminToken")).thenReturn(roleList);
        when(applicationService.getClientRoleById("id")).thenReturn(clientRole);
        spy.migrateRoles();
        verify(applicationService).updateClientRole(clientRole);
    }

    @Test
    public void migrateRoles_foundClientRoleAndNameNotMatch_callsApplicationServiceUpdateClientRole() throws Exception {
        RoleList roleList = new RoleList();
        Role role = new Role();
        role.setId("id");
        role.setName("notName");
        role.setDescription("description");
        List<Role> roles = roleList.getRole();
        roles.add(role);
        ClientRole clientRole = new ClientRole();
        clientRole.setDescription("description");
        clientRole.setName("name");
        doReturn("adminToken").when(spy).getAdminToken();
        when(client.getRoles("adminToken")).thenReturn(roleList);
        when(applicationService.getClientRoleById("id")).thenReturn(clientRole);
        spy.migrateRoles();
        verify(applicationService).updateClientRole(clientRole);
    }
}
