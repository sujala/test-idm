package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.api.idm.v1.ApplicationSecretCredentials;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 1:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForApplicationTest {

    private String applicationJSON = "{" +
            "   \"application\" : {" +
            "       \"clientId\" : \"clientId\"," +
            "       \"customerId\" : \"customerId\"," +
            "       \"name\" : \"applicationName\"," +
            "       \"enabled\" : false," +
            "       \"title\" : \"applicationTitle\"," +
            "       \"description\" : \"applicationDescription\"," +
            "       \"callBackUrl\" : \"applicationCallBackUrl\"," +
            "       \"scope\" : \"applicationScope\"," +
            "       \"applicationSecretCredentials\" : {" +
            "           \"clientSecret\" : \"clientSecret\"" +
            "       }" +
            "   }" +
            "}";


    private String applicationJSONWithClientSecret = "{" +
            "   \"applicationSecretCredentials\" : {" +
            "       \"clientSecret\" : \"clientSecret\"" +
            "   }" +
            "}";

    private String applicationJSONWithOutClientSecret = "{" +
            "   \"applicationSecretCredentials\" : {" +
            "   }" +
            "}";

    private String emptyApplicationJSON = "{" +
            "   \"application\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForApplication jsonReaderForApplication = new JSONReaderForApplication();
        boolean readable = jsonReaderForApplication.isReadable(Application.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForApplication jsonReaderForApplication = new JSONReaderForApplication();
        boolean readable = jsonReaderForApplication.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnsApplicationObject() throws Exception {
        JSONReaderForApplication jsonReaderForApplication = new JSONReaderForApplication();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(applicationJSON.getBytes()));
        Application application = jsonReaderForApplication.readFrom(Application.class, null, null, null, null, inputStream);
        assertThat("application", application, is(Application.class));
        assertThat("application name", application.getName(), equalTo("applicationName"));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationClientId() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application client id", applicationFromJSONString.getClientId(), equalTo("clientId"));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationCustomerId() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application customer id", applicationFromJSONString.getCustomerId(), equalTo("customerId"));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationName() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application name", applicationFromJSONString.getName(), equalTo("applicationName"));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationEnabled() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application enabled", applicationFromJSONString.isEnabled(), equalTo(false));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationTitle() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application title", applicationFromJSONString.getTitle(), equalTo("applicationTitle"));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationDescription() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application description", applicationFromJSONString.getDescription(), equalTo("applicationDescription"));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationCallBackUrl() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application callback url", applicationFromJSONString.getCallBackUrl(), equalTo("applicationCallBackUrl"));
    }

    @Test
    public void getApplicationFromJSONString_withValidJSON_setsApplicationScope() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON);
        assertThat("application scope", applicationFromJSONString.getScope(), equalTo("applicationScope"));
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationClientId() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application client id", applicationFromJSONString.getClientId(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationCustomerId() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application customer id", applicationFromJSONString.getCustomerId(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationName() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application name", applicationFromJSONString.getName(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationEnabled() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application enabled", applicationFromJSONString.isEnabled(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationTitle() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application title", applicationFromJSONString.getTitle(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationDescription() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application description", applicationFromJSONString.getDescription(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationCallBackUrl() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application callback url", applicationFromJSONString.getCallBackUrl(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationScope() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application scope", applicationFromJSONString.getScope(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyValidJSON_setsNullApplicationSecretCredentials() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString(emptyApplicationJSON);
        assertThat("application secret credentials", applicationFromJSONString.getSecretCredentials(), nullValue());
    }

    @Test
    public void getApplicationFromJSONString_withEmptyJSON_returnsNewApplicationObject() throws Exception {
        Application applicationFromJSONString = JSONReaderForApplication.getApplicationFromJSONString("{ }");
        assertThat("application", applicationFromJSONString, is(Application.class));
        assertThat("application name", applicationFromJSONString.getName(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getApplicationFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForApplication.getApplicationFromJSONString("Invalid JSON");
    }

    @Test
    public void getSecretCredentialsFromJSONString_withValidJsonAndSecretIsNull_doesNotSetClientSecret() throws Exception {
        ApplicationSecretCredentials creds = JSONReaderForApplication.getSecretCredentialsFromJSONString(applicationJSONWithOutClientSecret);
        assertThat("Secret", creds.getClientSecret(), equalTo(null));
    }

    @Test
    public void getSecretCredentialsFromJSONString_withValidJsonAndSecretIsNotNull_setClientSecret() throws Exception {
        ApplicationSecretCredentials creds = JSONReaderForApplication.getSecretCredentialsFromJSONString(applicationJSONWithClientSecret);
        assertThat("Secret", creds.getClientSecret(), equalTo("clientSecret"));
    }

    @Test(expected = BadRequestException.class)
    public void getSecretCredentialsFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForApplication.getSecretCredentialsFromJSONString("Invalid JSON");
    }

    @Test
    public void getSecretCredentialsFromJSONString_withInvalidJsonKey_returnsEmptyCredentials() throws Exception {
        ApplicationSecretCredentials creds = JSONReaderForApplication.getSecretCredentialsFromJSONString(applicationJSON);
        assertThat("Secret", creds.getClientSecret(), equalTo(null));
    }

    @Ignore
    @Test
    public void getApplicationFromJSONString_withValidSecretCredentials_returnsSecretCredentialsCredentials() throws Exception {
        ApplicationSecretCredentials clientSecret = JSONReaderForApplication.getApplicationFromJSONString(applicationJSON).getSecretCredentials();
        assertThat("Secret", clientSecret, notNullValue());
    }
}
