package com.rackspace.idm.api.resource.cloud;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/12/11
 * Time: 1:53 PM
 */
@Component
public class CloudClient {

    //Todo: create a property
    boolean ignoreSSLCert = true;

    public Response get(String url, HttpHeaders httpHeaders) throws IOException {
        HttpGet request = new HttpGet(url);
        return executeRequest(request, httpHeaders);
    }

    public Response delete(String url, HttpHeaders httpHeaders) throws IOException {
        HttpDelete request = new HttpDelete(url);
        return executeRequest(request, httpHeaders);
    }

    public Response put(String url, HttpHeaders httpHeaders, String body) throws IOException {
        HttpPut request = new HttpPut(url);
        request.setEntity(getHttpEntity(body));
        return executeRequest(request, httpHeaders);
    }

    public Response post(String url, HttpHeaders httpHeaders, String body) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(getHttpEntity(body));
        return executeRequest(request, httpHeaders);
    }

    private Response executeRequest(HttpRequestBase request, HttpHeaders httpHeaders) {
        DefaultHttpClient client = getHttpClient();
        setHttpHeaders(httpHeaders, request);

        String responseBody = null;
        int statusCode = 500;
        HttpResponse response = null;
        try {
            response = client.execute(request);
            statusCode = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                responseBody = convertStreamToString(response.getEntity().getContent());
            }
        } catch (IOException e) {
            responseBody = e.getMessage();
        }
        Response.ResponseBuilder responseBuilder = Response.status(statusCode);
        for (Header header : response.getAllHeaders()) {
            String key = header.getName();
            if (!key.equalsIgnoreCase("content-encoding")) {
                responseBuilder = responseBuilder.header(key, header.getValue());
            }
        }
        return responseBuilder.entity(responseBody).build();
    }

    private BasicHttpEntity getHttpEntity(String body) {
        BasicHttpEntity entity = new BasicHttpEntity();
        ByteArrayInputStream bs = new ByteArrayInputStream(body.getBytes());
        entity.setContent(bs);
        return entity;
    }

    private DefaultHttpClient getHttpClient() {
        DefaultHttpClient client = new DefaultHttpClient();
        client.addRequestInterceptor(new RequestAcceptEncoding());
        client.addResponseInterceptor(new ResponseContentEncoding());

        if (ignoreSSLCert) {
            client = WebClientDevWrapper.wrapClient(client);
        }

        return client;
    }

    private void setHttpHeaders(HttpHeaders httpHeaders, HttpRequestBase request) {
        Set<String> keys = httpHeaders.getRequestHeaders().keySet();
        request.setHeaders(new Header[]{});
        for (String key : keys) {
            if (!key.equalsIgnoreCase("content-length")) {
                request.setHeader(key, httpHeaders.getRequestHeaders().getFirst(key));
            }
        }
    }

    private String convertStreamToString(InputStream is)
            throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

}
