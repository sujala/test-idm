package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.User;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 8/3/12
 * Time: 4:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class Validator20Test {

    Validator20 validator20;
    Validator20 spy;

    @Before
    public void setUp() throws Exception {
        validator20 = new Validator20();
        spy = spy(validator20);
    }

    @Test
    public void validateUsername_withEmptyString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting username"));
        }
    }

    @Test
    public void validateUsername_withNullString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername(null);
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting username"));
        }
    }

    @Test
    public void validateUsername_withWhiteSpaceContainingString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("first last");
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Username should not contain white spaces"));
        }
    }

    @Test
    public void validateUsername_withWhiteSpaceContainingString2_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername(" firstlast");
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Username should not contain white spaces"));
        }
    }

    @Test
    public void validateUsername_withWhiteSpaceContainingString3_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("firstlast ");
            assertTrue("should throw exception",false);
        }catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Username should not contain white spaces"));
        }
    }

    @Test
    public void validateUsername_withTabContainingString_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsername("first   last");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Username should not contain white spaces"));
        }
    }

    @Test
    public void validateUsernameForUpdateOrCreate_withNonAlphChara_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsernameForUpdateOrCreate("12nogood");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Username must begin with an alphabetic character."));
        }
    }

    @Test
    public void validateUsernameForUpdateOrCreate_withSpecialChara_throwBadRequestException() throws Exception {
        try{
            validator20.validateUsernameForUpdateOrCreate("jorgenogood!");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Username has invalid characters; only alphanumeric characters are allowed."));
        }
    }

    @Test
    public void validateUsernameForUpdateOrCreate_validUserName() throws Exception {
        validator20.validateUsernameForUpdateOrCreate("jorgegood");
    }

    @Test
    public void validateEmail_missingEmail_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail(null);
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_blankEmail_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail2_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo@");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail3_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo.com");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateEmail_withInvalidEmail4_throwsBadRequestException() throws Exception {
        try{
            validator20.validateEmail("foo@.com");
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Expecting valid email address"));
        }
    }

    @Test
    public void validateUser_withValidEmail_succeeds() throws Exception {
        validator20.validateEmail("foo@bar.com");
    }

    @Test
    public void validateUser_withValidEmail2_succeeds() throws Exception {
        validator20.validateEmail("racker@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail3_succeeds() throws Exception {
        validator20.validateEmail("john.smith@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail4_succeeds() throws Exception {
        validator20.validateEmail("john.\"elGuapo\".smith@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail5_succeeds() throws Exception {
        validator20.validateEmail("1@rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail6_succeeds() throws Exception {
        validator20.validateEmail("1@1.net");
    }

    @Test
    public void validateEmail_withValidEmail7_succeeds() throws Exception {
        validator20.validateEmail("1@1.rackspace.com");
    }

    @Test
    public void validateEmail_withValidEmail8_succeeds() throws Exception {
        validator20.validateEmail("R_a_c_K_e_r_4000@rackspace.com");
    }

    @Test
    public void validateUserForCreate_callsValidateUsername() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setEmail("email");
        doNothing().when(spy).validateUsername("username");
        doNothing().when(spy).validateUsernameForUpdateOrCreate("username");
        doNothing().when(spy).validateEmail("email");
        spy.validateUserForCreate(user);
        verify(spy).validateUsername("username");
    }

    @Test
    public void validateUserForCreate_callsValidateUsernameForUpdateOrCreate() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setEmail("email");
        doNothing().when(spy).validateUsername("username");
        doNothing().when(spy).validateUsernameForUpdateOrCreate("username");
        doNothing().when(spy).validateEmail("email");
        spy.validateUserForCreate(user);
        verify(spy).validateUsernameForUpdateOrCreate("username");
    }

    @Test
    public void validateUserForCreate_callsValidateEmail() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setEmail("email");
        doNothing().when(spy).validateUsername("username");
        doNothing().when(spy).validateUsernameForUpdateOrCreate("username");
        doNothing().when(spy).validateEmail("email");
        spy.validateUserForCreate(user);
        verify(spy).validateEmail("email");
    }

    @Test
    public void validatePasswordCredentials_passwordIsBlank_throwsBadRequest() throws Exception {
        try{
            PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
            passwordCredentialsRequiredUsername.setUsername("username");
            passwordCredentialsRequiredUsername.setPassword(" ");
            doNothing().when(spy).validateUsername("username");
            validator20.validatePasswordCredentials(passwordCredentialsRequiredUsername);
            assertTrue("should throw exception",false);
        } catch (BadRequestException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Expecting password"));
        }
    }

    @Test
    public void validatePasswordCredentials_passwordIsNotBlank_throwsBadRequest() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("username");
        passwordCredentialsRequiredUsername.setPassword("password");
        doNothing().when(spy).validateUsername("username");
        validator20.validatePasswordCredentials(passwordCredentialsRequiredUsername);
    }
}
