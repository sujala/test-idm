package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
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
    private User user;
    private HttpResponse httpResponse;
    private Configuration config;
    private StatusLine statusLine;
    private HttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        atomHopperClient = new AtomHopperClient();

        user = new User();
        user.setId("1");
        user.setDisplayName("test");
        user.setEmail("test@rackspace.com");
        user.setUsername("test");

        //Mocks
        httpResponse = mock(HttpResponse.class);
        config = mock(Configuration.class);
        statusLine = mock(StatusLine.class);
        httpClient = mock(HttpClient.class);


        //Setters
        atomHopperClient.setConfig(config);
        atomHopperClient.setHttpClient(httpClient);
    }

    @Test
    public void createRequestEntity() throws Exception {
        InputStreamEntity reqEntity = atomHopperClient.createRequestEntity("<user name=\"Jorge\" />");
        assertThat("Input Stream is created", reqEntity != null, equalTo(true));
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

}
