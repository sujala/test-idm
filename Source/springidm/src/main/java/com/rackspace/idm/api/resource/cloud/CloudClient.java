package com.rackspace.idm.api.resource.cloud;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
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

    public Response authenticate(String url, String body) {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(url);
        // Provide custom retry handler is necessary

        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        byte[] responseBody = null;
        String exceptionMessage = null;
        int statusCode = 500;
        try {
            statusCode = client.executeMethod(method);
            responseBody = method.getResponseBody();
        } catch (HttpException e) {
            exceptionMessage = e.getMessage();
        } catch (IOException e) {
            exceptionMessage = e.getMessage();
        } finally {
            method.releaseConnection();
        }
        if (exceptionMessage != null) {
            return Response.status(500).entity(exceptionMessage).build();
        } else {
            return Response.status(statusCode).entity(responseBody).build();
        }
    }

    public Response get(String url, String username, String key) {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(url);
        // Provide custom retry handler is necessary

        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        byte[] responseBody = null;
        String exceptionMessage = null;
        int statusCode = 500;
        try {
            statusCode = client.executeMethod(method);
            responseBody = method.getResponseBody();
        } catch (HttpException e) {
            exceptionMessage = e.getMessage();
        } catch (IOException e) {
            exceptionMessage = e.getMessage();
        } finally {
            method.releaseConnection();
        }
        if (exceptionMessage != null) {
            return Response.status(500).entity(exceptionMessage).build();
        } else {
            return Response.status(statusCode).entity(responseBody).build();
        }
    }

    public Response post(String url, HttpHeaders httpHeaders, String body) throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost(url);
        BasicHttpEntity entity = new BasicHttpEntity();
        ByteArrayInputStream bs = new ByteArrayInputStream(body.getBytes());
        entity.setContent(bs);
        request.setEntity(entity);
        Set<String> keys = httpHeaders.getRequestHeaders().keySet();
        request.setHeaders(new Header[]{});
        for (String key : keys) {
            if (!key.equalsIgnoreCase("content-length")) {
                request.setHeader(key, httpHeaders.getRequestHeaders().getFirst(key));
            }
        }
        client.addRequestInterceptor(new RequestAcceptEncoding());
        client.addResponseInterceptor(new ResponseContentEncoding());
        String responseBody = null;
        int statusCode = 500;
        try {
            HttpResponse response = client.execute(request);
            statusCode = response.getStatusLine().getStatusCode();
            responseBody = convertStreamToString(response.getEntity().getContent());
        } catch (IOException e) {
            responseBody = e.getMessage();
        }
        return Response.status(statusCode).entity(responseBody).build();
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
