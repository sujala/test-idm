package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.cloud.migration.CloudMigrationService;
import com.rackspace.idm.api.resource.cloud.migration.MigrateUserResponseType;
import com.rackspace.idm.api.resource.tenant.TenantsResource;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/15/12
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class MigrationResourceTest {
    private MigrationResource migrationResource;
    private TenantsResource tenantsResource;
    private CloudMigrationService cloudMigrationService;

    @Before
    public void setUp() throws Exception {
        tenantsResource = mock(TenantsResource.class);
        cloudMigrationService = mock(CloudMigrationService.class);
        migrationResource = new MigrationResource(tenantsResource);
        migrationResource.setCloudMigrationService(cloudMigrationService);
    }

    @Test
    public void getMigratedUsers_callsCloudMigrationService_getMigratedUserList() throws Exception {
        when(cloudMigrationService.getMigratedUserList()).thenReturn(Response.ok());
        migrationResource.getMigratedUsers();
        verify(cloudMigrationService).getMigratedUserList();
    }

    @Test
    public void getInMigrationUsers_callsCloudMigrationService_getInMigrationUserList() throws Exception {
        when(cloudMigrationService.getInMigrationUserList()).thenReturn(Response.ok());
        migrationResource.getInMigrationUsers();
        verify(cloudMigrationService).getInMigrationUserList();
    }

    @Test
    public void migrateCloudUserByUsername_createsMigrateUserResponseType_returns200() throws Exception {
        when(cloudMigrationService.migrateUserByUsername("username", true)).thenReturn(new MigrateUserResponseType());
        Response response = migrationResource.migrateCloudUserByUsername("username", false);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void enableMigratedUserByUsername_callsCloudMigrationService_setMigratedUserEnabledStatus() throws Exception {
        migrationResource.enableMigratedUserByUsername("username");
        verify(cloudMigrationService).setMigratedUserEnabledStatus("username", false);
    }

    @Test
    public void enableMigratedUserByUsername_statusAccepted_returns202() throws Exception {
        Response response = migrationResource.enableMigratedUserByUsername("username");
        assertThat("response code", response.getStatus(), equalTo(202));
    }

    @Test
    public void disableMigratedUserByUsername_callsCloudMigrationService_setMigratedUserEnabledStatus() throws Exception {
        migrationResource.disableMigratedUserByUsername("username");
        verify(cloudMigrationService).setMigratedUserEnabledStatus("username", true);
    }

    @Test
    public void disableMigratedUserByUsername_statusAccepted_returns202() throws Exception {
        Response response = migrationResource.disableMigratedUserByUsername("username");
        assertThat("response code", response.getStatus(), equalTo(202));
    }

    @Test
    public void unmigrateCloudUserByUsername_callsCloudMigrationService_unmigrateUserByUsername() throws Exception {
        migrationResource.unmigrateCloudUserByUsername("username");
        verify(cloudMigrationService).unmigrateUserByUsername("username");
    }

    @Test
    public void unmigrateCloudUserByUsername_statusAccepted_returns202() throws Exception {
        Response response = migrationResource.unmigrateCloudUserByUsername("username");
        assertThat("response code", response.getStatus(), equalTo(202));
    }

    @Test
    public void getMigratedCloudUserByUsername_callsCloudMigrationService_getMigratedUser() throws Exception {
        when(cloudMigrationService.getMigratedUser("username")).thenReturn(Response.ok());
        migrationResource.getMigratedCloudUserByUsername("username");
        verify(cloudMigrationService).getMigratedUser("username");
    }

    @Test
    public void getMigratedCloudUserRolesByUsername_callsCloudMigrationService_getMigratedUserRoles() throws Exception {
        when(cloudMigrationService.getMigratedUserRoles("username")).thenReturn(Response.ok());
        migrationResource.getMigratedCloudUserRolesByUsername("username");
        verify(cloudMigrationService).getMigratedUserRoles("username");
    }

    @Test
    public void getMigratedCloudUserEndpointsByUsername_callsCloudMigrationService_getMigratedUserEndpoints() throws Exception {
        when(cloudMigrationService.getMigratedUserEndpoints("username")).thenReturn(Response.ok());
        migrationResource.getMigratedCloudUserEndpointsByUsername("username");
        verify(cloudMigrationService).getMigratedUserEndpoints("username");
    }

    @Test
    public void migrateBaseURLs_callsCloudMigrationService_migrateBaseURLs() throws Exception {
        migrationResource.migrateBaseURLs();
        verify(cloudMigrationService).migrateBaseURLs();
    }

    @Test
    public void migrateBaseURLs_statusAccepted_returns202() throws Exception {
        Response response = migrationResource.migrateBaseURLs();
        assertThat("response code", response.getStatus(), equalTo(202));
    }

    @Test
    public void migrateGroups_callsCloudMigrationService_migrateGroups() throws Exception {
        migrationResource.migrateGroups();
        verify(cloudMigrationService).migrateGroups();
    }

    @Test
    public void migrateGroups_statusAccepted_returns202() throws Exception {
        Response response = migrationResource.migrateGroups();
        assertThat("response code", response.getStatus(), equalTo(202));
    }

    @Test
    public void getGroups_callsCloudMigrationService_getGroups() throws Exception {
        when(cloudMigrationService.getGroups()).thenReturn(Response.ok());
        migrationResource.getGroups();
        verify(cloudMigrationService).getGroups();
    }

    @Test
    public void getGroups_responseOk_returns200() throws Exception {
        when(cloudMigrationService.getGroups()).thenReturn(Response.ok());
        Response result = migrationResource.getGroups();
        assertThat("response code", result.getStatus(), equalTo(200));
    }
}
