package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.UserSecret;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.api.idm.v1.User;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Tenant;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/7/12
 * Time: 1:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserTest {
    JSONReaderForUser jsonReaderForUser;

    @Before
    public void setUp() throws Exception {
        jsonReaderForUser = new JSONReaderForUser();
    }

    @Test
    public void isReadable_typeIsUser_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForUser.isReadable(User.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotUser_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForUser.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsUser() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("user",jsonReaderForUser.readFrom(User.class, null, null, null, null, inputStream),instanceOf(User.class));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectId() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getId(), equalTo("123"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectUserName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getUsername(), equalTo("jqsmith"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectCustomerID() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getCustomerId(), equalTo("456"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectEmail() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getEmail(), equalTo("john.smith@example.org"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectPersonID() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPersonId(), equalTo("789"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectFirstName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getFirstName(), equalTo("John"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectMiddleName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getMiddleName(), equalTo("Quentin"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectLastName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getLastName(), equalTo("Smith"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectDisplayName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getDisplayName(), equalTo("john"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectPrefLanguage() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPrefLanguage(), equalTo("english"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectCountry() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getCountry(), equalTo("USA"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectTimeZone() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getTimeZone(), equalTo("CMT"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectPassWordCredentials() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPasswordCredentials(), instanceOf(UserPasswordCredentials.class));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectSecret() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getSecret(), instanceOf(UserSecret.class));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserNotEnabled() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).isEnabled(), equalTo(true));
    }

    @Test
    public void getUserFromJSONString_passwordCredentialInput_returnsEmptyUser() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getId(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getUsername(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getCustomerId(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getEmail(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPersonId(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getFirstName(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getMiddleName(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getLastName(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getDisplayName(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPrefLanguage(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getCountry(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getTimeZone(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPasswordCredentials(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getSecret(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).isEnabled(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getUserFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForUser.getUserFromJSONString(body);
    }

    @Test
    public void getUserFromJSONString_nullId_returnsUserNullId() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullUserName_returnsUserNullUserName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getUsername(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullCustomerID_returnsUserNullCustomerID() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getCustomerId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullEmail_returnsUserNullEmail() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getEmail(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullPersonID_returnsUserNullPersonID() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPersonId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullFirstName_returnsUserNullFirstName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getFirstName(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullMiddleName_returnsUserNullMiddleName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getMiddleName(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullLastName_returnsUserNullLastName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getLastName(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullDisplayName_returnsUserNullDisplayName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getDisplayName(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullPrefLanguage_returnsUserNullPrefLanguage() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPrefLanguage(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullCountry_returnsUserNullCountry() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getCountry(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullTimeZone_returnsUserNullTimeZone() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getTimeZone(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullPasswordCredentials_returnsUserNullPasswordCredentials() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"secret\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getPasswordCredentials(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullSecret_returnsUserNullSecret() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getSecret(), nullValue());
    }

    @Test
    public void getUserFromJSONString_EnableNull_returnsUserNullEnable() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"customerId\": \"456\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"personId\": \"789\",\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"middleName\": \"Quentin\",\n" +
                "    \"lastName\": \"Smith\",\n" +
                "    \"displayName\": \"john\",\n" +
                "    \"prefLanguage\": \"english\",\n" +
                "    \"country\": \"USA\",\n" +
                "    \"timeZone\": \"CMT\",\n" +
                "    \"passwordCredentials\": {" +
                "       }\n" +
                "    \"secret\": {" +
                "       }\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).isEnabled(), nullValue());
    }
}