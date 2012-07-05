package com.rackspace.idm.api.resource.cloud.migration;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory;
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
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import com.sun.jersey.api.ConflictException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
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
    private MigrationClient client11;
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
        client11 = mock(MigrationClient.class);
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

        doReturn(client).when(spy).getMigrationClientInstance();
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
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        doReturn("asdf").when(spy).getAdminToken();
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenThrow(new JAXBException("EXCEPTION"));
        spy.migrateUserByUsername("userNotExist", false, null);
    }

    @Test (expected = NotFoundException.class)
    public void migrateUserByUsername_clientGetUserAndUsernameCanNotBeFound_throwsNotFoundException() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        doReturn("token").when(spy).getAdminToken();
        doThrow(new NotFoundException()).when(client).getUser(anyString(), anyString());
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Test (expected = ConflictException.class)
    public void migrateUserByUsername_usernameAlreadyExists_throwsConflictException() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(new com.rackspacecloud.docs.auth.api.v1.User());
        doReturn("asdf").when(spy).getAdminToken();
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        ArrayList<String> roles = new ArrayList<String>();
        roles.add("role1");
        doReturn(roles).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        when(userService.userExistsByUsername(anyString())).thenReturn(false).thenReturn(true);
        doReturn(true).when(spy).isSubUser(any(RoleList.class));
        spy.migrateUserByUsername("cmarin2", true, "1");
    }

    @Test (expected = BadRequestException.class)
    public void migrateUserByUsername_domainIdIsNullAndIsSubUser_throwsBadRequestException() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(new com.rackspacecloud.docs.auth.api.v1.User());
        doReturn("token").when(spy).getAdminToken();
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        doReturn(true).when(spy).isSubUser(any(RoleList.class));
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Test
    public void migrateUserByUsername_enableIsTrue_callsUserServiceUpdateUserById() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), any(CredentialListType.class), anyString(), anyString(), any(SecretQA.class),any(RoleList.class), any(Groups.class), anyList());
        spy.migrateUserByUsername("cmarin2", true, "1");
        verify(userService).updateUserById(any(User.class), eq(false));
    }

    @Test
    public void migrateUserByUsername_CloudBaseUrlBaseUrlTypeIsNAST_succeedsWithNoExceptions() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), any(CredentialListType.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        cloudBaseUrl.setBaseUrlType("NAST");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void migrateUserByUsername_CloudBaseUrlGetBaseUrlTypeIsMOSSO_succeedsWithNoExceptions() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), any(CredentialListType.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        cloudBaseUrl.setBaseUrlType("MOSSO");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void migrateUserByUsername_userDoesNotExist_returnsWithNoExceptions() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), any(CredentialListType.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void migrateUserByUsername_succeedsWithNoExceptions() throws Exception {
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        com.rackspacecloud.docs.auth.api.v1.User user11 = new com.rackspacecloud.docs.auth.api.v1.User();
        user11.setMossoId(1);
        user11.setBaseURLRefs(new BaseURLRefList());
        user11.getBaseURLRefs().getBaseURLRef().add(new BaseURLRef());
        when(client11.getUserTenantsBaseUrls(anyString(), anyString(), anyString())).thenReturn(user11);
        doReturn("token").when(spy).getAdminToken();
        doReturn(new ArrayList<String>()).when(spy).getSubUsers(any(org.openstack.docs.identity.api.v2.User.class), anyString(), anyString(), any(RoleList.class));
        doReturn(new User()).when(spy).addMigrationUser(any(org.openstack.docs.identity.api.v2.User.class), anyInt(), anyString(), anyString(), anyString(), any(SecretQA.class), anyString());
        doNothing().when(spy).addUserGlobalRoles(any(User.class), any(RoleList.class));
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        doReturn(new UserType()).when(spy).validateUser(any(org.openstack.docs.identity.api.v2.User.class), any(CredentialListType.class), anyString(), anyString(), any(SecretQA.class), any(RoleList.class), any(Groups.class), anyList());
        spy.migrateUserByUsername("cmarin2", false, "1");
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
        spy.unmigrateUserByUsername(null, true);
    }

    @Test (expected = NotFoundException.class)
    public void unmigrateUserByUsername_userGetInMigrationIsNull_throwsNotFoundException() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        spy.unmigrateUserByUsername("username", true);
    }

    @Ignore // Ignored due to code being removed...
    @Test (expected = BadRequestException.class)
    public void unmigrateUserByUsername_isRootUserAndIsSubUser_throwsBadRequest() throws Exception {
        RoleList roleList = new RoleList();
        List<Role> roles = roleList.getRole();
        Role role = new Role();
        role.setName("identity:default");
        roles.add(role);
        spy.setClient(client);
        when(client.getUserCredentials(anyString(), anyString())).thenReturn(new CredentialListType());
        doReturn("").when(spy).getPassword(any(CredentialListType.class));
        doReturn("").when(spy).getApiKey(any(CredentialListType.class));
        when(client.getRolesForUser(anyString(), anyString())).thenReturn(roleList);
        when(userService.getUser("username")).thenReturn(user);
        spy.unmigrateUserByUsername("username", true);
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
        doReturn(true).when(spy).isUserAdmin(any(RoleList.class));
        org.openstack.docs.identity.api.v2.User user = mock(org.openstack.docs.identity.api.v2.User.class);
        when(user.isEnabled()).thenReturn(false);
        spy.getSubUsers(user, "", "", new RoleList());
    }

    @Test(expected = ConflictException.class)
    public void getSubUsers_clientException_throwsConflictException() throws Exception {
        doReturn(true).when(spy).isUserAdmin(any(RoleList.class));
        org.openstack.docs.identity.api.v2.User user = mock(org.openstack.docs.identity.api.v2.User.class);
        when(user.isEnabled()).thenReturn(true);
        AuthenticateResponse authResponse = new AuthenticateResponse();
        authResponse.setToken(new Token());
        doReturn(authResponse).when(spy).authenticate(anyString(), anyString(), anyString());
        when(client.getUsers(anyString())).thenThrow(new JAXBException("EXCEPTION"));
        spy.getSubUsers(user, "", "", new RoleList());
    }
}
