package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 12/28/11
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class Cloud10VersionResourceIntegrationTest extends AbstractAroundClassJerseyTest {

    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
    }

    @Test
    public void getVersion_withValidLocalOnly_returns204() throws Exception {
        WebResource resource = resource().path("cloud/v1.0");
        ClientResponse clientResponse = resource.header("X-Auth-User", "mkovacs")
                .header("X-Auth-Key", "1234567890")
                .get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(204));
    }

    @Test
    @Ignore
    public void getVersion_withValidCloudOnly_returns204() throws Exception {
        WebResource resource = resource().path("cloud/v1.0");
        ClientResponse clientResponse = resource.header("X-Auth-User", "cmarin1")
                .header("X-Auth-Key", "70b67400a497d8148987d083b35caf9d")
                .get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(204));
    }

    @Test
    public void getVersion_withValidPath_returns401() throws Exception {
        WebResource resource = resource().path("cloud/v1.0");
        ClientResponse clientResponse = resource.header("X-Auth-User", "invalid")
                .header("X-Auth-Key", "no_key")
                .get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

}
