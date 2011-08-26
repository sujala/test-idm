package com.rackspace.idm.api.resource.cloud;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/12/11
 * Time: 1:53 PM
 */
@Component
public class CloudClient {

    // Todo: create a property
    boolean ignoreSSLCert = true;

    public Response.ResponseBuilder get(String url, HttpHeaders httpHeaders)
        throws IOException {
        HttpGet request = new HttpGet(url);
        return executeRequest(request, httpHeaders);
    }

    public Response.ResponseBuilder delete(String url, HttpHeaders httpHeaders)
        throws IOException {
        HttpDelete request = new HttpDelete(url);
        return executeRequest(request, httpHeaders);
    }

    public Response.ResponseBuilder put(String url, HttpHeaders httpHeaders,
        String body) throws IOException {
        HttpPut request = new HttpPut(url);
        request.setEntity(getHttpEntity(body));
        return executeRequest(request, httpHeaders);
    }

    public Response.ResponseBuilder post(String url, HttpHeaders httpHeaders,
        String body) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(getHttpEntity(body));
        return executeRequest(request, httpHeaders);
    }

    private Response.ResponseBuilder executeRequest(HttpRequestBase request,
        HttpHeaders httpHeaders) {
        DefaultHttpClient client = getHttpClient();
        setHttpHeaders(httpHeaders, request);

        String responseBody = null;
        int statusCode = 500;
        HttpResponse response = null;
        try {
            response = client.execute(request);
            statusCode = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                responseBody = convertStreamToString(response.getEntity()
                    .getContent());
            }
        } catch (IOException e) {
            responseBody = e.getMessage();
        }
        Response.ResponseBuilder responseBuilder = Response.status(statusCode)
            .entity(responseBody);
        for (Header header : response.getAllHeaders()) {
            String key = header.getName();
            if (!key.equalsIgnoreCase("content-encoding")
                && !key.equalsIgnoreCase("content-length")) {
                responseBuilder = responseBuilder
                    .header(key, header.getValue());
            }
        }
        return responseBuilder;
    }

    private BasicHttpEntity getHttpEntity(String body) {
        BasicHttpEntity entity = new BasicHttpEntity();
        ByteArrayInputStream bs = new ByteArrayInputStream(body.getBytes());
        entity.setContent(bs);
        return entity;
    }

    private DefaultHttpClient getHttpClient() {
        DefaultHttpClient client = new DefaultHttpClient();
        // client.addRequestInterceptor(new RequestAcceptEncoding());
        // client.addResponseInterceptor(new ResponseContentEncoding());

        if (ignoreSSLCert) {
            client = WebClientDevWrapper.wrapClient(client);
        }

        return client;
    }

    private void setHttpHeaders(HttpHeaders httpHeaders, HttpRequestBase request) {
        Set<String> keys = httpHeaders.getRequestHeaders().keySet();
        request.setHeaders(new Header[]{});
        for (String key : keys) {
            if (!key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                if (key.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                    request.setHeader(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_XML);
                } else {
                    request.setHeader(key, httpHeaders.getRequestHeaders()
                        .getFirst(key));
                }
            }
        }
    }

    public static String convertStreamToString(InputStream is)
        throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        is.close();
        return sb.toString();
    }

}

// private String convertStreamToString(InputStream is)
// throws IOException {
// if (is != null) {
// Writer writer = new StringWriter();
// char[] buffer = new char[6144];
// try {
// Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
// int n;
// while ((n = reader.read(buffer)) != -1) {
// writer.write(buffer, 0, n);
// }
// } finally {
// is.close();
// }
// return writer.toString();
// } else {
// return "";
// }
// }

// }
