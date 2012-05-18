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
import com.sun.jersey.api.ConflictException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeFactory;
import java.util.Date;
import java.util.GregorianCalendar;

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
    private MigrationClient client = new MigrationClient();
    private GregorianCalendar gc = new GregorianCalendar();
    private static DatatypeFactory df = null;

    private User user;
    private org.openstack.docs.identity.api.v2.User cloudUser;

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
        gc.setTimeInMillis(new Date().getTime());

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

        //stubbing
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        when(config.getString("rackspace.customerId")).thenReturn(null);

        spy = spy(cloudMigrationService);
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
    public void migrateUser_returns_ConflicException() throws Exception {
        when(userService.userExistsByUsername(anyString())).thenReturn(true);
        MigrateUserResponseType response = spy.migrateUserByUsername("conflictingUser", false, null);
    }

    @Ignore
    @Test
    public void migrateUser_returns() throws Exception {
        when(userService.userExistsByUsername(anyString())).thenReturn(false);
        when(client.getUser(anyString(), anyString())).thenReturn(cloudUser);
        MigrateUserResponseType response = spy.migrateUserByUsername("migrateUser", false, null);
    }

}
