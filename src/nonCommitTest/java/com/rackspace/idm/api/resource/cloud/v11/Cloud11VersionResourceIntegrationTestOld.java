package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
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
public class Cloud11VersionResourceIntegrationTestOld extends AbstractAroundClassJerseyTest {

    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
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

    @Test
    public void authenticate_invalidUser_returns401(){
         WebResource resource = resource().path("cloud/v1.1/auth");
        ClientResponse clientResponse = resource
                .header("Authorization", "Basic badauth")
                .type(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class, "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"bad-User\" key=\"70b67400a497d8148987d083b35caf9d\" />");
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void getUser_withoutAuth_returns401() {
        WebResource resource = resource().path("cloud/v1.1/users/a1b2c3");
        ClientResponse clientResponse = resource.header("Authorization", "Basic badauth").get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }
}
