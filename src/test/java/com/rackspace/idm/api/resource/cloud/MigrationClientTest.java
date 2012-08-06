package com.rackspace.idm.api.resource.cloud;

import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.Tenants;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 8/6/12
 * Time: 12:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class MigrationClientTest {
    MigrationClient migrationClient;
    HttpClientWrapper httpClientWrapper;
    ObjectFactory objectFactory;
    Logger logger;
    String cloud20Host;
    String cloud11Host;

    @Before
    public void setUp() throws Exception {
        migrationClient = new MigrationClient();
        objectFactory = new ObjectFactory();

        // Mocks
        httpClientWrapper = mock(HttpClientWrapper.class);
        logger = mock(Logger.class);

        // setting up fields
        cloud11Host = "cloud11host/";
        cloud20Host = "cloud20host/";
        migrationClient.setClient(httpClientWrapper);
        migrationClient.setLogger(logger);
        migrationClient.setObjectFactory(objectFactory);
        migrationClient.setCloud11Host(cloud11Host);
        migrationClient.setCloud20Host(cloud20Host);
    }

    @Test
    public void authenticateWithPassword_returnsAuthenticateResponse() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<access xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "    <token id=\"ab48a9efdfedb23ty3494\" expires=\"2010-11-01T03:32:15-05:00\">\n" +
                "        <tenant id=\"345\" name=\"My Project\" />\n" +
                "    </token>\n" +
                "    <user id=\"123\" username=\"jqsmith\">\n" +
                "        <roles xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "            <role id=\"123\" name=\"compute:admin\" />\n" +
                "            <role id=\"234\" name=\"object-store:admin\" />\n" +
                "        </roles>\n" +
                "    </user>\n" +
                "</access>";
        when(httpClientWrapper.url(cloud20Host + "tokens")).thenReturn(httpClientWrapper);
        when(httpClientWrapper.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)).thenReturn(httpClientWrapper);
        when(httpClientWrapper.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)).thenReturn(httpClientWrapper);
        when(httpClientWrapper.post(anyString())).thenReturn(body);
        AuthenticateResponse result = migrationClient.authenticateWithPassword("username", "password");
        assertThat("user id", result.getUser().getId(), equalTo("123"));
        assertThat("user id", result.getToken().getId(), equalTo("ab48a9efdfedb23ty3494"));
    }

    @Test
    public void authenticateWithApiKey_returnsAuthenticateResponse() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<access xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "    <token id=\"ab48a9efdfedb23ty3494\" expires=\"2010-11-01T03:32:15-05:00\">\n" +
                "        <tenant id=\"345\" name=\"My Project\" />\n" +
                "    </token>\n" +
                "    <user id=\"123\" username=\"jqsmith\">\n" +
                "        <roles xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "            <role id=\"123\" name=\"compute:admin\" />\n" +
                "            <role id=\"234\" name=\"object-store:admin\" />\n" +
                "        </roles>\n" +
                "    </user>\n" +
                "</access>";
        when(httpClientWrapper.url(cloud20Host + "tokens")).thenReturn(httpClientWrapper);
        when(httpClientWrapper.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)).thenReturn(httpClientWrapper);
        when(httpClientWrapper.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)).thenReturn(httpClientWrapper);
        when(httpClientWrapper.post(anyString())).thenReturn(body);
        AuthenticateResponse result = migrationClient.authenticateWithApiKey("username", "apikey");
        assertThat("user id", result.getUser().getId(), equalTo("123"));
        assertThat("user id", result.getToken().getId(), equalTo("ab48a9efdfedb23ty3494"));
    }
    /*
    @Test
    public void getTenants_returnsTenants() throws Exception {
        when(httpClientWrapper.url(cloud20Host + "tenants")).thenReturn(httpClientWrapper);
        when(httpClientWrapper.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)).thenReturn(httpClientWrapper);
        when(httpClientWrapper.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)).thenReturn(httpClientWrapper);
        when(httpClientWrapper.header("X-AUTH-TOKEN", "token")).thenReturn(httpClientWrapper);
        Tenants result = migrationClient.getTenants("token");
        assertThat("tenant id", result.getTenant().get(0).getId(), equalTo("123"));
    }   */
}
