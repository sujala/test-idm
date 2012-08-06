package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 7/9/12
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserValidatorFoundationTest{
    UserValidatorFoundation userValidator;
    Configuration config;
    CloudClient cloudClient;
    @Before
    public void setUp() throws Exception {
        userValidator = new UserValidatorFoundation();

        config = mock(Configuration.class);
        cloudClient = mock(CloudClient.class);

        userValidator.setConfig(config);
        userValidator.setCloudClient(cloudClient);
    }

    @Test (expected = BadRequestException.class)
    public void isUsernameEmpty_usernameIsEmpty_throwsBadRequestException() throws Exception {
        userValidator.isUsernameEmpty("");
    }

    @Test
    public void isUsernameEmpty_usernameNotEmpty_doesNotThrowException() throws Exception {
        userValidator.isUsernameEmpty("username");
        assertTrue("no exception", true);
    }

    @Test
    public void validateUsername_validName() throws Exception {
        User user = new User();
        user.setId("test12");
        userValidator.validateUsername(user.getId());
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_invalidName() throws Exception {
        User user = new User();
        user.setId("test12?");
        userValidator.validateUsername(user.getId());
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_validNameOther() throws Exception {
        User user = new User();
        user.setId("123nogood");
        userValidator.validateUsername(user.getId());
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_validNameLast() throws Exception {
        User user = new User();
        user.setId("test/");
        userValidator.validateUsername(user.getId());
    }

    @Test
    public void checkCloudAuthForUsername_usernameIsBlank_doesNothing() throws Exception {
        userValidator.checkCloudAuthForUsername("");
        verify(config, times(0)).getString("ga.username");
    }

    @Test (expected = DuplicateUsernameException.class)
    public void checkCloudAuthForUsername_foundUserInUSCloudStatusIs200_throwsDuplicateUsernameException() throws Exception {
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(cloudClient.get(eq("https://auth.staging.us.ccp.rackspace.net/v1.1/users/username"), any(HashMap.class))).thenReturn(Response.ok());
        userValidator.checkCloudAuthForUsername("username");
    }

    @Test (expected = DuplicateUsernameException.class)
    public void checkCloudAuthForUsername_foundUserInUKCloudStatusIs200_throwsDuplicateUsernameException() throws Exception {
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(config.getString("cloudAuthUK11url")).thenReturn("https://auth.staging.ord1.uk.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(cloudClient.get(eq("https://auth.staging.us.ccp.rackspace.net/v1.1/users/username"), any(HashMap.class))).thenReturn(Response.status(405));
        when(cloudClient.get(eq("https://auth.staging.ord1.uk.ccp.rackspace.net/v1.1/users/username"), any(HashMap.class))).thenReturn(Response.status(200));
        userValidator.checkCloudAuthForUsername("username");
    }

    @Test
    public void checkCloudAuthForUsername_userNotFoundInUSOrUKCloud_doesNothing() throws Exception {
        when(config.getString("ga.username")).thenReturn("auth");
        when(config.getString("ga.password")).thenReturn("auth123");
        when(config.getString("cloudAuthUK11url")).thenReturn("https://auth.staging.ord1.uk.ccp.rackspace.net/v1.1/");
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(cloudClient.get(eq("https://auth.staging.us.ccp.rackspace.net/v1.1/users/username"), any(HashMap.class))).thenReturn(Response.status(405));
        when(cloudClient.get(eq("https://auth.staging.ord1.uk.ccp.rackspace.net/v1.1/users/username"), any(HashMap.class))).thenReturn(Response.status(405));
        userValidator.checkCloudAuthForUsername("username");
        verify(cloudClient, times(1)).get(eq("https://auth.staging.us.ccp.rackspace.net/v1.1/users/username"), any(HashMap.class));
        verify(cloudClient, times(1)).get(eq("https://auth.staging.ord1.uk.ccp.rackspace.net/v1.1/users/username"), any(HashMap.class));
    }
}
