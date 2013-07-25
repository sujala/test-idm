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
}
