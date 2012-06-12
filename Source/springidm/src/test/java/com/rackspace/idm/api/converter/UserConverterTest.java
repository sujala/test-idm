package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.User;
import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.api.idm.v1.UserSecret;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/12/12
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserConverterTest {

    UserConverter userConverter;
    RolesConverter rolesConverter;
    User jaxbUser;
    com.rackspace.idm.domain.entity.User userDo;

    @Before
    public void setUp() throws Exception {
        jaxbUser = new User();
        userDo = new com.rackspace.idm.domain.entity.User();
        rolesConverter = new RolesConverter();
        userConverter = new UserConverter(rolesConverter);
    }

    @Test
    public void toUserDO_withJaxbUser_setsFields() throws Exception {
        jaxbUser.setId("userId");
        jaxbUser.setCountry("country");
        jaxbUser.setTimeZone("CET");
        jaxbUser.setCustomerId("customerId");
        jaxbUser.setDisplayName("displayName");
        jaxbUser.setEmail("email");
        jaxbUser.setFirstName("firstName");
        jaxbUser.setEnabled(true);
        jaxbUser.setLastName("lastName");
        jaxbUser.setMiddleName("middleName");
        jaxbUser.setPersonId("personId");
        jaxbUser.setPrefLanguage("en_US");
        jaxbUser.setRegion("region");
        jaxbUser.setMaxLoginFailuresExceded(false);
        jaxbUser.setUsername("username");
        UserPasswordCredentials userPasswordCredentials = new UserPasswordCredentials();
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("password");
        userPasswordCredentials.setCurrentPassword(userPassword);
        jaxbUser.setPasswordCredentials(userPasswordCredentials);
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretQuestion("Is this a question?");
        userSecret.setSecretAnswer("This is an answer.");
        jaxbUser.setSecret(userSecret);

        com.rackspace.idm.domain.entity.User user = userConverter.toUserDO(jaxbUser);

        assertThat("user id", user.getId(), equalTo("userId"));
        assertThat("user country", user.getCountry(), equalTo("country"));
        assertThat("user time zome", user.getTimeZone(), equalTo("CET"));
        assertThat("user customer id", user.getCustomerId(), equalTo("customerId"));
        assertThat("user display name", user.getDisplayName(), equalTo("displayName"));
        assertThat("user email", user.getEmail(), equalTo("email"));
        assertThat("user first name", user.getFirstname(), equalTo("firstName"));
        assertThat("user enabled", user.isEnabled(), equalTo(true));
        assertThat("user last name", user.getLastname(), equalTo("lastName"));
        assertThat("user middle name", user.getMiddlename(), equalTo("middleName"));
        assertThat("user person id", user.getPersonId(), equalTo("personId"));
        assertThat("user preferred language", user.getPreferredLang(), equalTo("en_US"));
        assertThat("user region", user.getRegion(), equalTo("region"));
        assertThat("user max login failures exceded", user.isMaxLoginFailuresExceded(), equalTo(false));
        assertThat("user username", user.getUsername(), equalTo("username"));
        assertThat("user password credentials", user.getPassword(), equalTo("password"));
        assertThat("user secret question", user.getSecretQuestion(), equalTo("Is this a question?"));
        assertThat("user secret answer", user.getSecretAnswer(), equalTo("This is an answer."));
    }

    @Test
    public void toUserDO_withJaxbUser_withNullCurrentPassword_DoesNotSetPassword() throws Exception {
        jaxbUser.setPasswordCredentials(new UserPasswordCredentials());
        com.rackspace.idm.domain.entity.User user = userConverter.toUserDO(jaxbUser);
        assertThat("user password", user.getPassword(), nullValue());
    }



}
