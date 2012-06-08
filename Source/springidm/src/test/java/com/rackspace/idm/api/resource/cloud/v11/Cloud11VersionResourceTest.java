package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Matchers;
import org.hamcrest.text.StringContains;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/22/11
 * Time: 10:06 AM
 */
public class Cloud11VersionResourceTest extends AbstractAroundClassJerseyTest {

    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
    }

    @Ignore
    @Test
    public void getVersion_withValidPath_returns200() {
        WebResource resource = resource().path("cloud/v1.1");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        System.out.println(clientResponse.getEntity(String.class));
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Ignore
    @Test
    public void getVersion_acceptsXml_returnsVersion() throws Exception {
        WebResource resource = resource().path("cloud/v1.1");
        ClientResponse clientResponse = resource.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        String entity = clientResponse.getEntity(String.class);
        assertThat("version", entity, Matchers.containsString("id=\"v1.1\""));
    }

    @Ignore
    @Test
    public void getVersion_acceptsJson_returnsVersion() throws Exception {
        WebResource resource = resource().path("cloud/v1.1");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        String entity = clientResponse.getEntity(String.class);
        assertThat("version", entity, Matchers.containsString("\"id\":\"v1.1\""));
    }

    @Test
    @Ignore
    public void authenticate_withValidUser_returns200() {
        WebResource resource = resource().path("cloud/v1.1/auth");
        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_XML_TYPE).accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class, "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"authuser1\" key=\"the_key\" />");
        String entity = clientResponse.getEntity(String.class);
        assertThat("token xml", entity, StringContains.containsString("<token"));
        assertThat("cloudFilesCDN xml", entity, StringContains.containsString("<service name=\"cloudFilesCDN\"><endpoint"));
        assertThat("cloudFiles xml", entity, StringContains.containsString("<service name=\"cloudFiles\"><endpoint"));
        assertThat("cloudDNS xml", entity, StringContains.containsString("<service name=\"cloudDNS\"><endpoint"));
        assertThat("cloudServers xml", entity, StringContains.containsString("<service name=\"cloudServers"));
        assertThat("auth ending tag", entity, StringContains.containsString("</auth>"));
    }

    @Ignore
    @Test
    public void createAndDeleteUser_withValidId_returns201() {
        WebResource resource = resource().path("cloud/v1.1/users");
        ClientResponse clientResponse = resource.header("Authorization", "Basic YXV0aDphdXRoMTIz")
                .get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(201));
    }

    @Test
    public void authenticate_withInvalidUser_returns401() {
        WebResource resource = resource().path("cloud/v1.1/auth");
        ClientResponse clientResponse = resource
                .header("Authorization", "Basic badauth")
                .type(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class, "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"badUser\" key=\"70b67400a497d8148987d083b35caf9d\" />");
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Ignore
    @Test
    public void getUser_withValidUser_returns200() {
        WebResource resource = resource().path("cloud/v1.1/users/user01");
        ClientResponse clientResponse = resource.header("Authorization", "Basic YXV0aDphdXRoMTIz").get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Ignore
    @Test
    public void getUser_withInvalidUser_returns404() {
        WebResource resource = resource().path("cloud/v1.1/users/a1b2c3");
        ClientResponse clientResponse = resource.header("Authorization", "Basic YXV0aDphdXRoMTIz").get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void getUser_withoutAuth_returns401() {
        WebResource resource = resource().path("cloud/v1.1/users/a1b2c3");
        ClientResponse clientResponse = resource.header("Authorization", "Basic badauth").get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }
}
