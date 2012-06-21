package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomFeed;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedUser;
import junit.framework.TestCase;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.InputStreamEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import com.rackspace.idm.domain.entity.User;

import java.io.Writer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Jun 1, 2012
 * Time: 10:08:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtomHopperClientTest {

    private AtomHopperClient atomHopperClient;
    private AtomHopperClient spy;
    private AtomFeed atomFeed;
    private User user;
    private HttpResponse httpResponse;
    private Configuration config;
    private StatusLine statusLine;

    @Before
    public void setUp() throws Exception {
        atomHopperClient = new AtomHopperClient();

        atomFeed = new AtomFeed();
        FeedUser feedUser = new FeedUser();

        user = new User();
        user.setId("1");
        user.setDisplayName("test");
        user.setEmail("test@rackspace.com");
        user.setUsername("test");

        feedUser.setDisplayName("testDisplayName");
        feedUser.setId("2");
        feedUser.setUsername("test2");

        atomFeed.setUser(feedUser);

        //Mocks
        httpResponse = mock(HttpResponse.class);
        config = mock(Configuration.class);
        statusLine = mock(StatusLine.class);

        //Setters
        atomHopperClient.setConfig(config);

        //spy
        spy = spy(atomHopperClient);
        doReturn(httpResponse).when(spy).executePostRequest(Matchers.<String>any(), Matchers.<Writer>any(), Matchers.<String>any());
    }

    @Test
    public void postUserDeleted() throws Exception {
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        when(config.getString(Matchers.<String>any())).thenReturn("http://localhost:8080/test");
        spy.postUser(user, "token", "deleted");
        assertThat("Post to Atom Hopper", httpResponse.getStatusLine().getStatusCode(), equalTo(201));
    }

    @Test
    public void postUser_userStatusDisabled_setsCorrectResponse() throws Exception {
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        when(config.getString(Matchers.<String>any())).thenReturn("http://localhost:8080/test");
        spy.postUser(user, "token", "disabled");
        assertThat("Post to Atom Hopper", httpResponse.getStatusLine().getStatusCode(), equalTo(201));
    }

    @Test
    public void postUser_userStatusNotDisabledOrDeleted_setsNullResponse() throws Exception {
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        when(config.getString(Matchers.<String>any())).thenReturn("http://localhost:8080/test");
        spy.postUser(user, "token", "");
        assertThat("Post to Atom Hopper", httpResponse.getStatusLine().getStatusCode(), equalTo(201));
    }

    @Test
    public void marshalFeed() throws Exception {
        Writer writer = atomHopperClient.marshalFeed(atomFeed);
        assertThat("Marshal User", writer.toString(), equalTo("<entry xmlns=\"http://www.w3.org/2005/Atom\"><user displayName=\"testDisplayName\" id=\"2\" username=\"test2\"/></entry>"));
    }

    @Test
    public void createRequestEntity() throws Exception {
        InputStreamEntity reqEntity = atomHopperClient.createRequestEntity("<user name=\"Jorge\" />");
        assertThat("Input Stream is created", reqEntity != null, equalTo(true));
    }

    @Test
    public void createAtomFeed() throws Exception {
        AtomFeed testAtomFeed = atomHopperClient.createAtomFeed(user,null);
        assertThat("Test Atom Create", testAtomFeed.getUser().getId(), equalTo("1"));
    }
}
