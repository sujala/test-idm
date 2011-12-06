package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/6/11
 * Time: 10:00 AM
 */
public class UserValidatorTest {

    UserValidator userValidator;

    @Before
    public void setUp() throws Exception {
        userValidator = new UserValidator();
    }

    @Test(expected = BadRequestException.class)
    public void validate_withNullUser_throwsBadRequestException() throws Exception {
        userValidator.validate(null);
    }

    @Test(expected = BadRequestException.class)
    public void validate_UserWithNullId_throwsBadRequestException() throws Exception {
        User user = new User();
        user.setId(null);
        userValidator.validate(user);
    }

    @Test(expected = BadRequestException.class)
    public void validate_UserWithNEmptyId_throwsBadRequestException() throws Exception {
        User user = new User();
        user.setId("");
        userValidator.validate(user);
    }

    @Test
    public void validate_validUser() throws Exception {
        User user = new User();
        user.setNastId("nastId");
        user.setMossoId(1);
        user.setKey("key");
        userValidator.validate(user);
    }
}
