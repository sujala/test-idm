package com.rackspace.idm.api.resource.cloud.migration;

import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.MigrationClient;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;

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
public class CloudMigrationServiceTest {
    private CloudMigrationService cloudMigrationService;
    private CloudMigrationService spy;
    private Configuration config;
    private ApplicationService applicationService;
    private MigrationClient migrationClient;
    private EndpointService endpointService;
    private JAXBObjectFactories jaxbObjectFactories;
    private TenantService tenantService;
    private UserConverterCloudV20 userConverterCloudV20;
    private UserService userService;

    private User user;

    @Before
    public void setUp() throws Exception {
        cloudMigrationService = new CloudMigrationService();

        //mocks
        config = mock(Configuration.class);
        applicationService = mock(ApplicationService.class);
        migrationClient = mock(MigrationClient.class);
        endpointService = mock(EndpointService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        tenantService = mock(TenantService.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        userService = mock(UserService.class);

        //setting mocks
        cloudMigrationService.setApplicationService(applicationService);
        cloudMigrationService.setClient(migrationClient);
        cloudMigrationService.setConfig(config);
        cloudMigrationService.setEndpointService(endpointService);
        cloudMigrationService.setOBJ_FACTORIES(jaxbObjectFactories);
        cloudMigrationService.setTenantService(tenantService);
        cloudMigrationService.setUserConverterCloudV20(userConverterCloudV20);
        cloudMigrationService.setUserService(userService);

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

        //stubbing

        spy = spy(cloudMigrationService);
    }

    @Test(expected = NotFoundException.class)
    public void getMigratedUser_returns_userNotFound() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getMigratedUser("migrateTestUserNotFound");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Ignore
    @Test
    public void getMigratedUser_returns_User() throws Exception {
        when(userService.getUser("cmarin3")).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getMigratedUser("cmarin3");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    

}
