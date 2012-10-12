package com.rackspace.idm.api.resource.cloud;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.configuration.Configuration;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.BasicHttpParams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/21/12
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudClientTest {

    @Mock
    Configuration config;
    @InjectMocks
    CloudClient cloudClient = new CloudClient();
    CloudClient spy;

    @Before
    public void setUp() throws Exception {
        spy = spy(cloudClient);
    }

    @Test
    public void get_withHttpHeaders_callsExecuteRequest_withHttpGet() throws Exception {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("someHeader", "someValue");
        when(headers.getRequestHeaders()).thenReturn(multivaluedMap);
        doReturn(null).when(spy).executeRequest(any(HttpGet.class));

        spy.get("url", headers);
        verify(spy).executeRequest(any(HttpGet.class));
    }

    @Test
    public void get_withMapHeaders_callsExecuteRequest_withHttpGet() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", "someValue");
        doReturn(null).when(spy).executeRequest(any(HttpGet.class));

        spy.get("url", headers);
        verify(spy).executeRequest(any(HttpGet.class));
    }

    @Test
    public void post_withHttpHeaders_callsExecuteRequest_withHttpPost() throws Exception {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("someHeader", "someValue");
        when(headers.getRequestHeaders()).thenReturn(multivaluedMap);
        doReturn(null).when(spy).executeRequest(any(HttpPost.class));

        spy.post("url", headers, "body");
        verify(spy).executeRequest(any(HttpPost.class));
    }

    @Test
    public void post_withMapHeaders_callsExecuteRequest_withHttpPost() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("someHeader", "someValue");
        doReturn(null).when(spy).executeRequest(any(HttpPost.class));

        spy.post("url", headers, "body");
        verify(spy).executeRequest(any(HttpPost.class));
    }

    @Test
    public void delete_callsExecuteRequest_withHttpDelete() throws Exception {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("someHeader", "someValue");
        when(headers.getRequestHeaders()).thenReturn(multivaluedMap);
        doReturn(null).when(spy).executeRequest(any(HttpDelete.class));

        spy.delete("url", headers);
        verify(spy).executeRequest(any(HttpDelete.class));
    }

    @Test
    public void put_callsExecuteRequest_withHttpPut() throws Exception {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("someHeader", "someValue");
        when(headers.getRequestHeaders()).thenReturn(multivaluedMap);
        doReturn(null).when(spy).executeRequest(any(HttpPut.class));

        spy.put("url", headers, "body");
        verify(spy).executeRequest(any(HttpPut.class));
    }

    @Test
    public void executeRequest_callsClient_execute() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        verify(client).execute(any(HttpUriRequest.class));
    }

    @Test
    public void executeRequest_withEntityInResponse_returnsResponseWithEntity() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("body entity\n".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response entity", responseBuilder.build().getEntity().toString(), equalTo("body entity\n"));
    }

    @Test
    public void executeRequest_withEmptyEntityInResponse_returnsResponseWithNullEntity() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response entity", responseBuilder.build().getEntity(), nullValue());
    }

    @Test
    public void executeRequest_withIoException_returns500WithErrorMessageAsEntity() throws Exception {
        HttpClient client = mock(HttpClient.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenThrow(new IOException("IoException"));

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        Response retResponse = responseBuilder.build();
        assertThat("response entity", retResponse.getEntity().toString(), equalTo("IoException"));
        assertThat("response status", retResponse.getStatus(), equalTo(500));
    }

    @Test
    public void executeRequest_withStatus301_callsHandleRedirect() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("Response Body".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 301, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        verify(spy).handleRedirect(response, "Response Body\n");
    }

    @Test
    public void executeRequest_returnsResponseWithResponseSourceHeader() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("Response Body".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response source header", responseBuilder.build().getMetadata().containsKey("Response-Source"), equalTo(true));
    }

    @Test
    public void executeRequest_returnsResponseWithNonContentEncodingHeadersFromCloudResponse() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("Response Body".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{new Header() {
            @Override
            public String getName() {
                return "content-encoding";
            }

            @Override
            public String getValue() {
                return "someValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];
            }
        }});

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response header", responseBuilder.build().getMetadata().containsKey("content-encoding"), equalTo(false));
    }

    @Test
    public void executeRequest_returnsResponseWithNonContentLengthHeadersFromCloudResponse() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("Response Body".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{new Header() {
            @Override
            public String getName() {
                return "content-length";
            }

            @Override
            public String getValue() {
                return "someValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];
            }
        }});

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response header", responseBuilder.build().getMetadata().containsKey("content-length"), equalTo(false));
    }

    @Test
    public void executeRequest_returnsResponseWithNoTransferEncodingHeadersFromCloudResponse() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("Response Body".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{new Header() {
            @Override
            public String getName() {
                return "transfer-encoding";
            }

            @Override
            public String getValue() {
                return "someValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];
            }
        }});

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response header", responseBuilder.build().getMetadata().containsKey("transfer-encoding"), equalTo(false));
    }

    @Test
    public void executeRequest_returnsResponseWithNoVaryHeadersFromCloudResponse() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("Response Body".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{new Header() {
            @Override
            public String getName() {
                return "vary";
            }

            @Override
            public String getValue() {
                return "someValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];
            }
        }});

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response header", responseBuilder.build().getMetadata().containsKey("vary"), equalTo(false));
    }

    @Test
    public void executeRequest_returnsResponseWithNonContentRelatedHeadersFromCloudResponse() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        doReturn(client).when(spy).getHttpClient();
        when(client.getParams()).thenReturn(new BasicHttpParams());
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(new ByteArrayEntity("Response Body".getBytes()));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{new Header() {
            @Override
            public String getName() {
                return "someHeader";
            }

            @Override
            public String getValue() {
                return "someValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];
            }
        }});

        Response.ResponseBuilder responseBuilder = spy.executeRequest(new HttpGet("http://owijfaw.vmjioew.imkfwo"));
        assertThat("response header", responseBuilder.build().getMetadata().containsKey("someHeader"), equalTo(true));
    }

    @Test
    public void handleRedirect_returnsResponseBuilderWith301Status_andCorrectEntity() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        Response.ResponseBuilder responseBuilder = spy.handleRedirect(response, "response body");
        Response builtResponse = responseBuilder.build();
        assertThat("response entity", builtResponse.getEntity().toString(), equalTo("response body"));
        assertThat("response status", builtResponse.getStatus(), equalTo(301));
    }

    @Test
    public void handleRedirect_returnsResponseBuilderResponseSourceHeader() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        Response.ResponseBuilder responseBuilder = spy.handleRedirect(response, "response body");
        Response builtResponse = responseBuilder.build();
        assertThat("response-source header", builtResponse.getMetadata().containsKey("response-source"), equalTo(true));
    }

    @Test
    public void handleRedirect_withNullResponseBody_returnsResponseBuilderWithNullEntity() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        Response.ResponseBuilder responseBuilder = spy.handleRedirect(response, null);
        Response builtResponse = responseBuilder.build();
        assertThat("response entity", builtResponse.getEntity(), nullValue());
    }

    //TODO: should be 400 bad request?
    @Test
    public void handleRedirect_withNullResponse_returns500Status() throws Exception {
        HttpResponse response = null;

        Response.ResponseBuilder responseBuilder = spy.handleRedirect(response, null);
        assertThat("response entity", responseBuilder.build().getStatus(), equalTo(500));
    }

    @Test
    public void handleRedirect_returnsResponseBuilderWithHeaders() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getAllHeaders()).thenReturn(new Header[]{new Header() {
            @Override
            public String getName() {
                return "someHeader";
            }

            @Override
            public String getValue() {
                return "someHeaderValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
        }});

        Response.ResponseBuilder responseBuilder = spy.handleRedirect(response, "response body");
        Response builtResponse = responseBuilder.build();
        assertThat("Other header", builtResponse.getMetadata().containsKey("someHeader"), equalTo(true));
    }

    @Test
    public void handleRedirect_withContentTypeHeader_setsContentTypeToXML() throws Exception {
        HttpHeaders headers = mock(HttpHeaders.class);
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("content-type", "application/notxml");
        when(headers.getRequestHeaders()).thenReturn(multivaluedMap);
        doReturn(null).when(spy).executeRequest(any(HttpRequestBase.class));

        spy.post("someUrl", headers, "someBody");
        ArgumentCaptor<HttpRequestBase> argumentCaptor = ArgumentCaptor.forClass(HttpRequestBase.class);
        verify(spy).executeRequest(argumentCaptor.capture());
        assertThat("content-type is application/xml", argumentCaptor.getValue().getFirstHeader("content-type").getValue(), equalTo("application/xml"));
    }

    @Test
    public void handleRedirect_returnsResponseBuilderWithOutEncodingOrContentRelatedHeaders() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getAllHeaders()).thenReturn(new Header[]{new Header() {
            @Override
            public String getName() {
                return "content-encoding";
            }

            @Override
            public String getValue() {
                return "someHeaderValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
        },new Header() {
            @Override
            public String getName() {
                return "content-length";
            }

            @Override
            public String getValue() {
                return "someHeaderValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
        },new Header() {
            @Override
            public String getName() {
                return "host";
            }

            @Override
            public String getValue() {
                return "someHeaderValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
        },new Header() {
            @Override
            public String getName() {
                return "transfer-encoding";
            }

            @Override
            public String getValue() {
                return "someHeaderValue";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
        }});

        Response.ResponseBuilder responseBuilder = spy.handleRedirect(response, "response body");
        Response builtResponse = responseBuilder.build();
        assertThat("content-encoding header", builtResponse.getMetadata().containsKey("content-encoding"), equalTo(false));
        assertThat("content-length header", builtResponse.getMetadata().containsKey("content-length"), equalTo(false));
        assertThat("transfer-encoding header", builtResponse.getMetadata().containsKey("transfer-encoding"), equalTo(false));
    }


    @Test
    public void getHttpClient_returnsHttpClient() throws Exception {
        assertThat("HttpClient", cloudClient.getHttpClient(), instanceOf(HttpClient.class));
    }

    @Test
    public void put_withNullBody_setsNullEntityInRequest() throws Exception {
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getRequestHeaders()).thenReturn(new MultivaluedMapImpl());
        doReturn(new ResponseBuilderImpl()).when(spy).executeRequest(any(HttpRequestBase.class));
        spy.put("someUrl", headers, null);
        ArgumentCaptor<HttpPut> httpRequestArgumentCaptor = ArgumentCaptor.forClass(HttpPut.class);
        verify(spy).executeRequest(httpRequestArgumentCaptor.capture());
        assertThat("put entity", httpRequestArgumentCaptor.getValue().getEntity(), nullValue());
    }

    @Test
    public void setHeaders_withEmptyHeaders_setsNoHeaders() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();
        doReturn(new ResponseBuilderImpl()).when(spy).executeRequest(any(HttpRequestBase.class));
        spy.post("url", headers, null);
        ArgumentCaptor<HttpPost> httpRequestArgumentCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(spy).executeRequest(httpRequestArgumentCaptor.capture());
        assertThat("request headers", httpRequestArgumentCaptor.getValue().getAllHeaders().length, equalTo(0));
    }
}
