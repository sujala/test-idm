package com.rackspace.idm.api.resource.cloud.migration;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.MigrationClient;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.sun.jersey.api.ConflictException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.RoleList;

import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

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
    private String adminToken;
    private CloudBaseUrl cloudBaseUrl;

    private User user;
    private org.openstack.docs.identity.api.v2.User cloudUser;

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
        adminToken = "a7c3507e-48aa-4bb2-af6c-af74ea834423";

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

        spy = spy(cloudMigrationService);
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
        doReturn(client).doReturn(client11).when(spy).getMigrationClientInstance();
        doReturn(adminToken).when(spy).getAdminToken();
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        doThrow(new NotFoundException()).when(client).getUser(anyString(), anyString());
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Test
    public void migrateUserByUsername_passwordIsBlankString_checksPasswordIsNotEmptyString() throws Exception {
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        doReturn(adminToken).when(spy).getAdminToken();
        doReturn("").when(spy).getPassword(any(CredentialListType.class));
        MigrateUserResponseType responseType = spy.migrateUserByUsername("cmarin2", false, "1");
        List<UserType> userList = responseType.getUsers();
        assertThat("password", userList.get(0).getPassword().length(), not(0));
    }

    @Test (expected = BadRequestException.class)
    public void migrateUserByUsername_domainIdIsNullAndIsSubUser() throws Exception {
        doReturn(adminToken).when(spy).getAdminToken();
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        doReturn(true).when(spy).isSubUser(any(RoleList.class));
        spy.migrateUserByUsername("cmarin2", false, null);
    }

    @Test
    public void migrateUserByUsername_succeedsWithNoExceptions() throws Exception {
        doReturn(adminToken).when(spy).getAdminToken();
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(cloudBaseUrl);
        when(userService.getUser(anyString())).thenReturn(user);
        spy.migrateUserByUsername("cmarin2", false, "1");
    }

    @Test
    public void getSubUsers_returns() throws Exception {
        MigrationClient clientTest = new MigrationClient();
        spy.setClient(clientTest);
        clientTest.setCloud20Host("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        RoleList roles = clientTest.getRolesForUser("a7c3507e-48aa-4bb2-af6c-af74ea834423", "104472");
        spy.getSubUsers("cmarin2", "0f97f489c848438090250d50c7e1ea88", "Password1", roles);
    }

}
