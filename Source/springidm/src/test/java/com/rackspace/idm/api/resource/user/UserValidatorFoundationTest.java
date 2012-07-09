package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.resource.cloud.v11.UserValidator;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 7/9/12
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserValidatorFoundationTest{
    UserValidatorFoundation userValidator;
    @Before
    public void setUp() throws Exception {
        userValidator = new UserValidatorFoundation();
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
