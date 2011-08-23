package com.rackspace.idm.api.resource.cloud;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/22/11
 * Time: 10:06 AM
 */
public class Cloud11VersionResourceTest extends AbstractAroundClassJerseyTest{

    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
    }

    @Test
    public void getVersion_withValidPath_returns200(){
        WebResource resource = resource().path("cloud/v1.1");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

}
