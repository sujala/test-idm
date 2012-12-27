package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomFeed;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedUser;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import com.rackspace.idm.domain.entity.User;

import java.io.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
    private HttpClient httpClient;

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
        httpClient = mock(HttpClient.class);


        //Setters
        atomHopperClient.setConfig(config);
        atomHopperClient.setHttpClient(httpClient);

        //spy
        spy = spy(atomHopperClient);
        doReturn(httpResponse).when(spy).executePostRequest(anyString(), any(Writer.class), anyString());
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
    public void postUser_withNon201Response_logsUsernameAndId() throws Exception {
        User mockUser = mock(User.class);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(401);
        when(config.getString(Matchers.<String>any())).thenReturn("http://localhost:8080/test");
        doReturn(null).when(spy).createAtomFeed(mockUser, AtomHopperConstants.CONTENT_TYPE, null);
        doReturn(null).when(spy).marshalFeed(null);
        spy.postUser(mockUser, "token", "deleted");
        verify(mockUser).getUsername();
        verify(mockUser).getId();
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
        AtomFeed testAtomFeed = atomHopperClient.createAtomFeed(user, AtomHopperConstants.CONTENT_TYPE, null);
        assertThat("Test Atom Create", testAtomFeed.getUser().getId(), equalTo("1"));
    }

    @Test
    public void executePostRequest_callsHttpClient_execute() throws Exception {
        Writer writer = mock(Writer.class);
        when(writer.toString()).thenReturn("writerString");
        atomHopperClient.executePostRequest("token",writer, "url");
        verify(httpClient).execute(any(HttpUriRequest.class));
    }

    @Test
    public void executePostRequest_sets() throws Exception {
        Writer writer = mock(Writer.class);
        when(writer.toString()).thenReturn("writerString");

        atomHopperClient.executePostRequest("token",writer, "url");
        ArgumentCaptor<HttpPost> httpUriRequestArgumentCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(httpUriRequestArgumentCaptor.capture());
        HttpPost request = httpUriRequestArgumentCaptor.getValue();
        assertThat("httpRequest url", request.getURI().getPath(), equalTo("url"));
        assertThat("httpRequest token header", request.getHeaders("x-auth-token")[0].getValue(), equalTo("token"));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        request.getEntity().writeTo(byteArrayOutputStream);
        assertThat("httpRequest url", byteArrayOutputStream.toString(), equalTo("writerString"));
    }

    @Test
    public void asyncPost_callsPostUserThrowsException_getsCaught() throws Exception {
        doThrow(new IOException()).when(spy).postUser(user, "token", "notMigrated");
        spy.asyncPost(user, "notMigrated");
        assertTrue(true);
    }

    @Test
    public void asyncPost_callsPostUser() throws Exception {
        AtomFeed atomFeed = new AtomFeed();
        doReturn(atomFeed).when(spy).createAtomFeed(any(User.class), eq(AtomHopperConstants.CONTENT_TYPE), eq("migrationStatus"));
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1,1), 404, "not found"));
        spy.asyncPost(user, "notMigrated");
        verify(spy, timeout(100)).postUser(user, "token", "notMigrated");
    }

    @Test
    public void createAtomFeed_returnsAtomFeed() throws Exception {
        AtomFeed atomFeed = atomHopperClient.createAtomFeed(user, AtomHopperConstants.CONTENT_TYPE, "migrated");
        FeedUser result = atomFeed.getUser();
        assertThat("username", result.getUsername(), equalTo(user.getUsername()));
        assertThat("id", result.getId(), equalTo(user.getId()));
        assertThat("display name", result.getDisplayName(), equalTo(user.getDisplayName()));
    }
}
