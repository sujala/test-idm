package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

import javax.ws.rs.core.MediaType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 4:21 PM
 */
public class Cloud20VersionResourceTest extends AbstractAroundClassJerseyTest {


    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
    }

    @Test
    public void getVersion_withValidPath_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getVersion_acceptsXml_returnsVersion() throws Exception {
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        String entity = clientResponse.getEntity(String.class);
        assertThat("version", entity, Matchers.containsString("id=\"v2.0\""));
    }

    @Test
    public void getVersion_acceptsJson_returnsVersion() throws Exception {
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        String entity = clientResponse.getEntity(String.class);
        assertThat("version", entity, Matchers.containsString("\"id\":\"v2.0\""));
    }

    @Test
    public void getTenants__returns200() throws Exception {
        String token = getAuthToken("cmarin3", "Password1");
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getTenants_admin__returns200() throws Exception {
        String token = getAuthToken("cmarin4", "Password1");
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    //call gets forwarded and cloud auth returns 200 with an empty tenant list
    @Test
    public void getTenants_badToken_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", "bad").accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    private String getAuthToken(String username, String password) {
        WebResource resource = resource().path("cloud/v2.0/tokens");
        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class,
                        "<auth xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><passwordCredentials username=\""+username+"\" password=\""+password+"\"/></auth>");
        return clientResponse.getEntity(AuthenticateResponse.class).getToken().getId();
    }

    @Test
    public void authenticate_withValidPath_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");
        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class,
                        "<auth xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><passwordCredentials username=\"cmarin1\" password=\"Password1\"/></auth>");
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    //works when forwarding, user id might be different in ga and ca
    @Test
    public void listUserGroups_returns200() throws Exception {
        String token = getAuthToken("cmarin4", "Password1");
        WebResource resource = resource().path("cloud/v2.0/users/104472/RAX-KSGRP");
        ClientResponse clientResponse = resource.header("X-Auth-Token",token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_invalidAuthToken_returns401() throws Exception {
        String token = "invalid";
        WebResource resource = resource().path("cloud/v2.0/users/104472/RAX-KSGRP");
        ClientResponse clientResponse = resource.header("X-Auth-Token",token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Ignore
    @Test
    public void authenticate_json_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");

        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class,
                        "{" + "\"auth\":{" + "\"passwordCredentials\":{" + "\"username\":\"cmarin1\",\"password\":\"Password1\"},\"tenantId\":\"1234\" }}");
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_MissingCredentials_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");
        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    //This functionality is not implemented in cloud auth
    @Test
    public void listEndpointTemplates_returns200() throws Exception {
        String token = getAuthToken("cmarin4", "Password1");
        WebResource resource = resource().path("cloud/v2.0/OS-KSCATALOG/endpointTemplates");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    //call gets forwarded to cloud and it is not implemented in cloud
    @Test
    public void listEndpointTemplates_returns404() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/OS-KSCATALOG/endpointTemplates");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }
}
