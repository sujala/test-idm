package com.rackspace.idm.api.resource.cloud.migration;

import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.MigrationClient;
import com.rackspace.idm.api.resource.cloud.v20.CloudKsGroupBuilder;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.sun.jersey.api.ConflictException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 4/25/12
 * Time: 9:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloudMigrationServiceIntegrationTest {
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
    private String adminToken;
    private CloudKsGroupBuilder cloudKsGroupBuilder;

    @Before
    public void setUp() throws Exception {
        cloudMigrationService = new CloudMigrationService();
        endpointConverterCloudV20 = new EndpointConverterCloudV20();
        cloudBaseUrl = new CloudBaseUrl();
        //mocks
        config = mock(Configuration.class);
        applicationService = mock(ApplicationService.class);
        client = spy(new MigrationClient());
        endpointService = mock(EndpointService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        tenantService = mock(TenantService.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        userService = mock(UserService.class);
        groupService = mock(GroupService.class);
        roleConverterCloudV20 = mock(RoleConverterCloudV20.class);
        scopeAccessService = mock(ScopeAccessService.class);
        cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);

        //setting mocks
        cloudMigrationService.setApplicationService(applicationService);
        cloudMigrationService.setClient(client);
        cloudMigrationService.setConfig(config);
        cloudMigrationService.setEndpointService(endpointService);
        cloudMigrationService.setObj_factories(jaxbObjectFactories);
        cloudMigrationService.setTenantService(tenantService);
        cloudMigrationService.setUserConverterCloudV20(userConverterCloudV20);
        cloudMigrationService.setUserService(userService);
        cloudMigrationService.setCloudGroupService(groupService);
        cloudMigrationService.setRoleConverterCloudV20(roleConverterCloudV20);
        cloudMigrationService.setScopeAccessService(scopeAccessService);
        cloudMigrationService.setEndpointConverterCloudV20(endpointConverterCloudV20);
        cloudMigrationService.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        gc.setTimeInMillis(new Date().getTime());

        //setting mocks for endpointconverter
        endpointConverterCloudV20.setObjFactories(jaxbObjectFactories);

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
        adminToken = spy.getAdminToken();
    }

    @Test
    public void migratedBaseURLs_callsAddOrUpdateEndpointTemplates() throws Exception {
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
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
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
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
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        spy.migrateUserByUsername("userNotExist", false, null);
    }

    @Test (expected = NotFoundException.class)
    public void migrateUserByUsername_clientGetUserAndUsernameCanNotBeFound_throwsNotFoundException() throws Exception {
        doReturn("token").when(spy).getAdminToken();
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        doThrow(new NotFoundException()).when(client).getUser(anyString(), anyString());
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Ignore
    @Test (expected = ConflictException.class)
    public void migrateUserByUsername_usernameALreadyExists_throwsConflictException() throws Exception {
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsByUsername(anyString())).thenReturn(false).thenReturn(true);
        doReturn(true).when(spy).isSubUser(any(RoleList.class));
        when(client.getUser(anyString(), anyString())).thenReturn(new org.openstack.docs.identity.api.v2.User());
        spy.migrateUserByUsername("cmarin2", true, "1");
    }

    @Ignore
    @Test (expected = BadRequestException.class)
    public void migrateUserByUsername_domainIdIsNullAndIsSubUser_throwsBadRequestException() throws Exception {
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        doReturn(true).when(spy).isSubUser(any(RoleList.class));
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Ignore
    @Test
    public void migrateUserByUsername_enableIsTrue_callsUserServiceUpdateUserById() throws Exception {
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        spy.migrateUserByUsername("cmarin2", true, "1");
        verify(userService, times(2)).updateUserById(any(User.class), eq(false));
    }

    @Ignore
    @Test
    public void migrateUserByUsername_CloudBaseUrlBaseUrlTypeIsNAST_succeedsWithNoExceptions() throws Exception {
        cloudBaseUrl.setBaseUrlType("NAST");
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Ignore
    @Test
    public void migrateUserByUsername_CloudBaseUrlGetBaseUrlTypeIsMOSSO_succeedsWithNoExceptions() throws Exception {
        cloudBaseUrl.setBaseUrlType("MOSSO");
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Ignore
    @Test
    public void migrateUserByUsername_userDoesNotExist_returnsWithNoExceptions() throws Exception {
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        when(userService.userExistsById(anyString())).thenReturn(true);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Ignore
    @Test
    public void migrateUserByUsername_succeedsWithNoExceptions() throws Exception {
        when(config.getString("migration.username")).thenReturn("migration_user");
        when(config.getString("migration.apikey")).thenReturn("0f97f489c848438090250d50c7e1ea88");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
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

    @Ignore // Ignored due to code being removed...
    @Test (expected = BadRequestException.class)
    public void unmigrateUserByUsername_isRootUserAndIsSubUser_throwsBadRequest() throws Exception {
        RoleList roleList = new RoleList();
        List<Role> roles = roleList.getRole();
        Role role = new Role();
        role.setName("identity:default");
        roles.add(role);
        spy.setClient(client);
        doReturn(adminToken).when(spy).getAdminToken();
        when(client.getUserCredentials(anyString(), anyString())).thenReturn(new CredentialListType());
        doReturn("").when(spy).getPassword(any(CredentialListType.class));
        doReturn("").when(spy).getApiKey(any(CredentialListType.class));
        when(client.getRolesForUser(anyString(), anyString())).thenReturn(roleList);
        when(userService.getUser("username")).thenReturn(user);
        spy.unmigrateUserByUsername("username");
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
}
