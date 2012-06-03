package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import junit.framework.TestCase;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.mockito.Matchers;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.Logger;

import java.io.Writer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import org.openstack.docs.identity.api.v2.ObjectFactory;

import org.slf4j.Logger;

import javax.xml.bind.JAXBElement;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Jun 1, 2012
 * Time: 10:08:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtomHopperClientTest extends TestCase {

    private AtomHopperClient atomHopperClient;
    private AtomHopperClient spy;
    private User user;
    private HttpResponse httpResponse;
    private com.rackspace.idm.domain.entity.User user2;
    private UserConverterCloudV20 userConverterCloudV20;
    private Configuration config;
    private ObjectFactory objectFactory;
    private StatusLine statusLine;

    @Before
    public void setUp() throws Exception {
        atomHopperClient = new AtomHopperClient();

        user = new User();
        user.setId("1");
        user.setDisplayName("test");
        user.setEmail("test@rackspace.com");
        user.setUsername("test");

        user2 = new com.rackspace.idm.domain.entity.User();
        user2.setId("2");
        user2.setUsername("test2");
        user2.setDisplayName("test2");
        objectFactory = new ObjectFactory();

        //Mocks
        httpResponse = mock(HttpResponse.class);
        config = mock(Configuration.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        statusLine = mock(StatusLine.class);

        //Setters
        atomHopperClient.setUserConverterCloudV20(userConverterCloudV20);
        atomHopperClient.setConfig(config);
        atomHopperClient.setObjectFactory(objectFactory);

        //spy
        spy = spy(atomHopperClient);
        doReturn(httpResponse).when(spy).executePostRequest(Matchers.<String>any(), Matchers.<Writer>any(), Matchers.<String>any());
    }

    public void testPostUserDeleted() throws Exception {
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        when(config.getString(Matchers.<String>any())).thenReturn("http://localhost:8080/test");
        when(userConverterCloudV20.toUser(Matchers.<com.rackspace.idm.domain.entity.User>any())).thenReturn(user);
        spy.postUser(user2,"token","deleted");
        assertThat("Post to Atom Hopper",httpResponse.getStatusLine().getStatusCode(),equalTo(201));
    }

    public void testMarshal20User() throws Exception {
        Writer writer = atomHopperClient.marshal20User(user);
        assertThat("Marshal User",writer.toString(),equalTo("<user display-name=\"test\" email=\"test@rackspace.com\" username=\"test\" id=\"1\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns1=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\"/>"));
    }

    public void testCreateRequestEntity() throws Exception {
        InputStreamEntity reqEntity = atomHopperClient.createRequestEntity("<user name=\"Jorge\" />");
        assertThat("Input Stream is created",reqEntity!=null,equalTo(true));
    }

    public void testCreateEntryPayload() throws Exception {
         assertThat("Entry String",atomHopperClient.createEntryPayload("<user></user>") , equalTo("<entry xmlns=\"http://www.w3.org/2005/Atom\"><user></user></entry>"));
    }
}
