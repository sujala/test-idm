package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.*;
import com.rackspace.api.idm.v1.Racker;
import com.rackspace.api.idm.v1.User;
import com.rackspace.idm.domain.entity.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import java.util.ArrayList;
import java.util.Locale;

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

    @Test
    public void toUserDO_withJaxbUser_withNullPasswordInCurrentPassword_DoesNotSetPassword() throws Exception {
        UserPasswordCredentials passwordCredentials = new UserPasswordCredentials();
        passwordCredentials.setCurrentPassword(new UserPassword());
        jaxbUser.setPasswordCredentials(passwordCredentials);
        com.rackspace.idm.domain.entity.User user = userConverter.toUserDO(jaxbUser);
        assertThat("user password", user.getPassword(), nullValue());
    }

    @Test
    public void toUserDO_withJaxbUser_withBlankPassword_DoesNotSetPassword() throws Exception {
        UserPasswordCredentials passwordCredentials = new UserPasswordCredentials();
        passwordCredentials.setCurrentPassword(new UserPassword());
        jaxbUser.setPasswordCredentials(passwordCredentials);
        com.rackspace.idm.domain.entity.User user = userConverter.toUserDO(jaxbUser);
        assertThat("user password", user.getPassword(), nullValue());
    }

    @Test
    public void toUserDO_withJaxbUser_withNullSecret_DoesNotSetSecretAnswerOrQuestion() throws Exception {
        com.rackspace.idm.domain.entity.User user = userConverter.toUserDO(jaxbUser);
        assertThat("user secret question", user.getSecretQuestion(), nullValue());
        assertThat("user secret answer", user.getSecretAnswer(), nullValue());
    }

    @Test
    public void toUserDO_withJaxbUser_withBlankSecretAnswer_DoesNotSetSecretAnswerOrQuestion() throws Exception {
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretQuestion("Is this a question?");
        jaxbUser.setSecret(userSecret);
        com.rackspace.idm.domain.entity.User user = userConverter.toUserDO(jaxbUser);
        assertThat("user secret answer", user.getSecretAnswer(), nullValue());
        assertThat("user secret question", user.getSecretQuestion(), nullValue());
    }

    @Test
    public void toUserDO_withJaxbUser_withBlankSecretQuestion_DoesNotSetSecretAnswerOrQuestion() throws Exception {
        UserSecret userSecret = new UserSecret();
        userSecret.setSecretAnswer("Is this a question?");
        jaxbUser.setSecret(userSecret);
        com.rackspace.idm.domain.entity.User user = userConverter.toUserDO(jaxbUser);
        assertThat("user secret answer", user.getSecretAnswer(), nullValue());
        assertThat("user secret question", user.getSecretQuestion(), nullValue());
    }

    @Test
    public void toUserListJaxb_withNullUsers_returnsNull() throws Exception {
        JAXBElement<UserList> userListJAXBElement = userConverter.toUserListJaxb(null);
        assertThat("user list jaxb", userListJAXBElement, nullValue());
    }

    @Test
    public void toUserListJaxb_withNullUserList_returnsNull() throws Exception {
        JAXBElement<UserList> userListJAXBElement = userConverter.toUserListJaxb(new Users());
        assertThat("user list jaxb", userListJAXBElement, nullValue());
    }

    @Test
    public void toUserListJaxb_withUsers_returnsJaxbListOfCorrectSize() throws Exception {
        Users users = new Users();
        users.setUsers(new ArrayList<com.rackspace.idm.domain.entity.User>());
        users.getUsers().add(new com.rackspace.idm.domain.entity.User());
        users.getUsers().add(new com.rackspace.idm.domain.entity.User());
        JAXBElement<UserList> userListJAXBElement = userConverter.toUserListJaxb(users);
        assertThat("user list jaxb", userListJAXBElement.getValue().getUser().size(), equalTo(2));
    }

    @Test
    public void toUserListJaxb_withUsers_setsFields() throws Exception {
        Users users = new Users();
        users.setUsers(new ArrayList<com.rackspace.idm.domain.entity.User>());
        users.setLimit(2);
        users.setOffset(3);
        users.setTotalRecords(4);
        JAXBElement<UserList> userListJAXBElement = userConverter.toUserListJaxb(users);
        assertThat("user list jaxb limit", userListJAXBElement.getValue().getLimit(), equalTo(2));
        assertThat("user list jaxb offset", userListJAXBElement.getValue().getOffset(), equalTo(3));
        assertThat("user list jaxb total records", userListJAXBElement.getValue().getTotalRecords(), equalTo(4));
    }

    @Test
    public void toRackerJaxb_withRackerId_setsUsername() throws Exception {
        Racker racker = userConverter.toRackerJaxb("rackerId");
        assertThat("racker id", racker.getUsername(), equalTo("rackerId"));
    }

    @Test
    public void toRackerJaxb_withRacker_setsRackerId() throws Exception {
        com.rackspace.idm.domain.entity.Racker rackerDO = new com.rackspace.idm.domain.entity.Racker();
        rackerDO.setRackerId("rackerId");
        JAXBElement<Racker> rackerJaxb = userConverter.toRackerJaxb(rackerDO);
        assertThat("racker id", rackerJaxb.getValue().getId(), equalTo("rackerId"));
    }

    @Test
    public void toRackerJaxb_withRacker_withNoRoles_doesNotSetRoles() throws Exception {
        com.rackspace.idm.domain.entity.Racker rackerDO = new com.rackspace.idm.domain.entity.Racker();
        JAXBElement<Racker> rackerJaxb = userConverter.toRackerJaxb(rackerDO);
        assertThat("racker Roles", rackerJaxb.getValue().getRoles(), nullValue());
    }

    @Test
    public void toRackerJaxb_withRacker_withRoles_setsRoles() throws Exception {
        com.rackspace.idm.domain.entity.Racker rackerDO = new com.rackspace.idm.domain.entity.Racker();
        rackerDO.setRackerRoles(new ArrayList<String>());
        rackerDO.getRackerRoles().add("role one");
        rackerDO.getRackerRoles().add("role two");
        JAXBElement<Racker> rackerJaxb = userConverter.toRackerJaxb(rackerDO);
        assertThat("racker Roles", rackerJaxb.getValue().getRoles().getRole().size(), equalTo(2));
    }

    @Test
    public void toRackerRolesJaxb_withEmptyList_returnsEmptyList() throws Exception {
        JAXBElement<RoleList> roleListJAXBElement = userConverter.toRackerRolesJaxb(new ArrayList<String>());
        assertThat("role list", roleListJAXBElement.getValue().getRole().size(), equalTo(0));
    }

    @Test
    public void toRackerRolesJaxb_withListSizeTwo_returnsListSizeTwo() throws Exception {
        ArrayList<String> roles = new ArrayList<String>();
        roles.add("role one");
        roles.add("role two");
        JAXBElement<RoleList> roleListJAXBElement = userConverter.toRackerRolesJaxb(roles);
        assertThat("role list", roleListJAXBElement.getValue().getRole().size(), equalTo(2));
    }

    @Test
    public void toRackerRolesJaxb_withList_withEmptyElements_doesNotAddEmptyElements() throws Exception {
        ArrayList<String> roles = new ArrayList<String>();
        roles.add("role one");
        roles.add("");
        JAXBElement<RoleList> roleListJAXBElement = userConverter.toRackerRolesJaxb(roles);
        assertThat("role list", roleListJAXBElement.getValue().getRole().size(), equalTo(1));
    }

    @Test
    public void toUserJaxb_withDomainUser_setsFields() throws Exception {
        userDo.setCountry("country");
        userDo.setCreated(new DateTime(1));
        userDo.setUpdated(new DateTime(2));
        userDo.setCustomerId("customerId");
        userDo.setDisplayName("displayName");
        userDo.setEmail("email");
        userDo.setEnabled(true);
        userDo.setFirstname("firstName");
        userDo.setMiddlename("middleName");
        userDo.setLastname("lastName");
        userDo.setPersonId("personId");
        userDo.setLocale(new Locale("en_us"));
        userDo.setRegion("region");
        userDo.setUsername("username");
        userDo.setMaxLoginFailuresExceded(false);
        userDo.setPassword("password");
        userDo.setSecretAnswer("secretAnswer");
        userDo.setSecretQuestion("secretQuestion");

        JAXBElement<User> userJAXBElement = userConverter.toUserJaxb(userDo, true, true);

        assertThat("user country", userJAXBElement.getValue().getCountry(), equalTo("country"));
        assertThat("user country", userJAXBElement.getValue().getCreated().toGregorianCalendar().getTimeInMillis(), equalTo(1L));
        assertThat("user country", userJAXBElement.getValue().getUpdated().toGregorianCalendar().getTimeInMillis(), equalTo(2L));
        assertThat("user country", userJAXBElement.getValue().getCustomerId(), equalTo("customerId"));
        assertThat("user country", userJAXBElement.getValue().getDisplayName(), equalTo("displayName"));
        assertThat("user country", userJAXBElement.getValue().getEmail(), equalTo("email"));
        assertThat("user country", userJAXBElement.getValue().isEnabled(), equalTo(true));
        assertThat("user country", userJAXBElement.getValue().getFirstName(), equalTo("firstName"));
        assertThat("user country", userJAXBElement.getValue().getMiddleName(), equalTo("middleName"));
        assertThat("user country", userJAXBElement.getValue().getLastName(), equalTo("lastName"));
        assertThat("user country", userJAXBElement.getValue().getPersonId(), equalTo("personId"));
        assertThat("user country", userJAXBElement.getValue().getPrefLanguage(), equalTo("en_us")); //All lowercase while Domain user is lower_UPPER
        assertThat("user country", userJAXBElement.getValue().getRegion(), equalTo("region"));
        assertThat("user country", userJAXBElement.getValue().getUsername(), equalTo("username"));
        assertThat("user country", userJAXBElement.getValue().isMaxLoginFailuresExceded(), equalTo(false));
        assertThat("user country", userJAXBElement.getValue().getPasswordCredentials().getCurrentPassword().getPassword(), equalTo("password"));
        assertThat("user country", userJAXBElement.getValue().getSecret().getSecretAnswer(), equalTo("secretAnswer"));
        assertThat("user country", userJAXBElement.getValue().getSecret().getSecretQuestion(), equalTo("secretQuestion"));
    }

    @Test
    public void toUserJaxb_withNoCreatedDate_doesNotSetCreated() throws Exception {
        JAXBElement<User> userJAXBElement = userConverter.toUserJaxb(userDo);
        assertThat("user created", userJAXBElement.getValue().getCreated(), nullValue());
    }

    @Test
    public void toUserJaxb_withNoUpdatedDate_doesNotSetUpdated() throws Exception {
        JAXBElement<User> userJAXBElement = userConverter.toUserJaxb(userDo);
        assertThat("user updated", userJAXBElement.getValue().getUpdated(), nullValue());
    }

    @Test
    public void toUserJaxb_withNoPassword_doesNotSetPassword() throws Exception {
        JAXBElement<User> userJAXBElement = userConverter.toUserJaxb(userDo);
        assertThat("user password", userJAXBElement.getValue().getPasswordCredentials(), nullValue());
    }

    @Test
    public void toUserJaxb_withBlankPassword_doesNotSetPassword() throws Exception {
        userDo.getPasswordObj().setValue("");
        JAXBElement<User> userJAXBElement = userConverter.toUserJaxb(userDo);
        assertThat("user password", userJAXBElement.getValue().getPasswordCredentials(), nullValue());
    }

    @Test
    public void toUserJaxb_withPassword_withFalseIncludePassword_doesNotSetPassword() throws Exception {
        userDo.setPassword("password");
        JAXBElement<User> userJAXBElement = userConverter.toUserJaxb(userDo, false, false);
        assertThat("user password", userJAXBElement.getValue().getPasswordCredentials(), nullValue());
    }

    @Test
    public void toUserJaxb_withNullSecret_DoesNotSetUserSecret() throws Exception {
        JAXBElement<User> user = userConverter.toUserJaxb(userDo);
        assertThat("user secret", user.getValue().getSecret(), nullValue());
    }

    @Test
    public void toUserJaxb_withBlankSecretAnswer_DoesNotSetSecretAnswerOrQuestion() throws Exception {
        userDo.setSecretAnswer("this is an answer");
        JAXBElement<User> user = userConverter.toUserJaxb(userDo);
        assertThat("user secret", user.getValue().getSecret(), nullValue());
    }

    @Test
    public void toUserJaxb_withBlankSecretQuestion_DoesNotSetSecretAnswerOrQuestion() throws Exception {
        userDo.setSecretQuestion("is this a question?");
        JAXBElement<User> user = userConverter.toUserJaxb(userDo);
        assertThat("user secret", user.getValue().getSecret(), nullValue());
    }

    @Test
    public void toUserJaxb_withUserSecret_withIncludeSecretFalse_DoesNotSetSecretAnswerOrQuestion() throws Exception {
        userDo.setSecretQuestion("is this a question?");
        userDo.setSecretAnswer("this is an answer");
        JAXBElement<User> user = userConverter.toUserJaxb(userDo, false, false);
        assertThat("user secret", user.getValue().getSecret(), nullValue());
    }

    @Test
    public void toUserJaxbFromUser_setsFields() throws Exception {
        JAXBElement<User> userJAXBElement = userConverter.toUserJaxbFromUser("username", "customerId");
        assertThat("jaxb user username", userJAXBElement.getValue().getUsername(), equalTo("username"));
        assertThat("jaxb user customer id", userJAXBElement.getValue().getCustomerId(), equalTo("customerId"));
    }
}
