package com.rackspace.idm.api.resource.cloud;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
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
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(url);
        Set<String> keys = httpHeaders.getRequestHeaders().keySet();
        for(String key: keys){
            method.setRequestHeader(key, httpHeaders.getRequestHeaders().getFirst(key));
        }
        method.setRequestBody(body);
        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        String responseBody = null;
        int statusCode = 500;
        try {
            statusCode = client.executeMethod(method);
            responseBody = new String(method.getResponseBody());
        } finally {
            method.releaseConnection();
        }
        return Response.status(statusCode).entity(responseBody).build();
    }

}
