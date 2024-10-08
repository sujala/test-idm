package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.junit.Before;
import org.junit.Ignore;
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
    public void validate_UserWithEmptyId_throwsBadRequestException() throws Exception {
        User user = new User();
        user.setId("");
        userValidator.validate(user);
    }

    @Test
    public void validate_UserWithNastIdAndKey() throws Exception {
        User user = new User();
        user.setNastId("nastId");
        user.setKey("key");
        user.setId("id");
        userValidator.validate(user);
    }

    @Test
    public void validate_UserWithOnlyId() throws Exception {
        User user = new User();
        user.setId("Id");
        userValidator.validate(user);
    }

    @Test
    public void validate_validUser() throws Exception {
        User user = new User();
        user.setId("id");
        user.setNastId("nastId");
        user.setMossoId(1);
        user.setKey("key");
        userValidator.validate(user);
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

    @Ignore // for migration users with number starting username is allows 12/03/2012
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
