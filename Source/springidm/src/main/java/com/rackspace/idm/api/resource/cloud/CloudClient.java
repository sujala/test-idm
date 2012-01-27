package com.rackspace.idm.api.resource.cloud;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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

    // Todo: create a property
    private boolean ignoreSSLCert = true;

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

    private Response.ResponseBuilder executeRequest(HttpRequestBase request, HttpHeaders httpHeaders) {
        DefaultHttpClient client = getHttpClient();
        setHttpHeaders(httpHeaders, request);

        // ToDo: Fix when returning 301 - errors in build of ResponseBuilder due to entity
        client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);

        String responseBody = null;
        int statusCode = 500;
        HttpResponse response = null;
        try {
            response = client.execute(request);
            statusCode = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                responseBody = convertStreamToString(response.getEntity().getContent());
                if (responseBody.equals(""))
                    responseBody = null;
            }
        } catch (IOException e) {
            responseBody = e.getMessage();
        }

        // Catch 301 - MOVED_PERMANENTLY
        if(statusCode == 301){
            //Quick Fix: not best way to pass the body
            return handleRedirect(response, responseBody);
        }

        Response.ResponseBuilder responseBuilder = Response.status(statusCode).entity(responseBody);
        for (Header header : response.getAllHeaders()) {
            String key = header.getName();
            if (!key.equalsIgnoreCase("content-encoding") && !key.equalsIgnoreCase("content-length")) {
                responseBuilder = responseBuilder.header(key, header.getValue());
            }
        }
//      if (statusCode == 500) {
            responseBuilder.header("response-source","cloud-auth");
//      }
        return responseBuilder;
    }

    private Response.ResponseBuilder handleRedirect(HttpResponse response, String responseBody){
        try{
            Response.ResponseBuilder builder = Response.status(Response.Status.MOVED_PERMANENTLY); //.header("Location", uri);
            for (Header header : response.getAllHeaders()) {
                String key = header.getName();
                if (!key.equalsIgnoreCase("content-encoding") && !key.equalsIgnoreCase("content-length")) {
                    builder.header(key, header.getValue());
                }
            }
            //builder.entity(response.getEntity());
            if (responseBody != null){
                builder.entity(responseBody);
            }
            builder.header("response-source","cloud-auth");
            return builder;
        }catch(Exception ex){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private BasicHttpEntity getHttpEntity(String body) {
        if (body == null)
            return null;
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