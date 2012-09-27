package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import org.apache.commons.configuration.Configuration;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/12/11
 * Time: 1:53 PM
 */

public class CloudClient {

    private final Configuration config;

    @Autowired
    public CloudClient(Configuration config) {
        this.config = config;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Response.ResponseBuilder get(String url, HttpHeaders httpHeaders) {
        HttpGet request = new HttpGet(url);
        setHttpHeaders(httpHeaders, request);
        return executeRequest(request);
    }

    public Response.ResponseBuilder get(String url, Map<String, String> headers) {
        HttpGet request = new HttpGet(url);
        setHeaders(headers, request);
        return executeRequest(request);
    }

    public Response.ResponseBuilder delete(String url, HttpHeaders httpHeaders) {
        HttpDelete request = new HttpDelete(url);
        setHttpHeaders(httpHeaders, request);
        return executeRequest(request);
    }

    public Response.ResponseBuilder put(String url, HttpHeaders httpHeaders, String body){
        HttpPut request = new HttpPut(url);
        request.setEntity(getHttpEntity(body));
        setHttpHeaders(httpHeaders, request);
        return executeRequest(request);
    }

    public Response.ResponseBuilder post(String url, HttpHeaders httpHeaders, String body) {
        HttpPost request = new HttpPost(url);
        request.setEntity(getHttpEntity(body));
        setHttpHeaders(httpHeaders, request);
        return executeRequest(request);
    }

    public Response.ResponseBuilder post(String url, Map<String, String> headers, String body) {
        HttpPost request = new HttpPost(url);
        request.setEntity(getHttpEntity(body));
        setHeaders(headers, request);
        return executeRequest(request);
    }


    Response.ResponseBuilder executeRequest(HttpRequestBase request) {
        HttpClient client = getHttpClient();
        // ToDo: Fix when returning 301 - errors in build of ResponseBuilder due to entity
        client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);

        String responseBody = null;
        int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        HttpResponse response = new BasicHttpResponse(request.getProtocolVersion(), statusCode, "Unable To connect to Auth Service");
        try {
            response = client.execute(request);
            statusCode = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                responseBody = convertStreamToString(response.getEntity().getContent());
                if (responseBody.equals("")) { responseBody = null; }
            }
        } catch (IOException e) {
            responseBody = e.getMessage();
        }

        // Catch 301 - MOVED_PERMANENTLY
        if (statusCode == HttpServletResponse.SC_MOVED_PERMANENTLY) {
            //Quick Fix: not best way to pass the body
            return handleRedirect(response, responseBody);
        }

        Response.ResponseBuilder responseBuilder = Response.status(statusCode).entity(responseBody);
        for (Header header : response.getAllHeaders()) {
            String key = header.getName();
            if (!key.equalsIgnoreCase("content-encoding") && !key.equalsIgnoreCase("content-length")
                    && !key.equalsIgnoreCase("transfer-encoding") && !key.equalsIgnoreCase("vary")) {
                responseBuilder = responseBuilder.header(key, header.getValue());
            }
            if (key.equalsIgnoreCase("location")) {
                String globalLocation = config.getString("ga.endpoint") + "v1.1";
                String cloudLocation = header.getValue();
                try {
                    URL u = new URL(cloudLocation);
                    globalLocation += u.getPath();
                } catch (MalformedURLException e) {
                    globalLocation = cloudLocation;
                }

                responseBuilder.header(key, globalLocation);
            }
        }
        if (statusCode == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            logger.info("Cloud Auth returned a 500 status code.");
        }
        responseBuilder.header("response-source", "cloud-auth");

        return responseBuilder;
    }

    Response.ResponseBuilder handleRedirect(HttpResponse response, String responseBody) {
        try {
            Response.ResponseBuilder builder = Response.status(Response.Status.MOVED_PERMANENTLY); //.header("Location", uri);
            for (Header header : response.getAllHeaders()) {
                String key = header.getName();
                if (!key.equalsIgnoreCase("content-encoding") && !key.equalsIgnoreCase("content-length") && !key.equalsIgnoreCase("transfer-encoding") && !key.equalsIgnoreCase("vary")) {
                    builder.header(key, header.getValue());
                }
            }
            //builder.entity(response.getEntity());
            if (responseBody != null) {
                builder.entity(responseBody);
            }
            builder.header("response-source", "cloud-auth");
            return builder;
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private BasicHttpEntity getHttpEntity(String body) {
        if (body == null) { return null; }
        BasicHttpEntity entity = new BasicHttpEntity();
        ByteArrayInputStream bs = new ByteArrayInputStream(body.getBytes());
        entity.setContent(bs);
        return entity;
    }

    HttpClient getHttpClient() {
        HttpClient client = new DecompressingHttpClient(WebClientDevWrapper.wrapClient(new DefaultHttpClient()));

        return client;
    }

    private void setHeaders(Map<String, String> headers, HttpRequestBase request) {
        if (!headers.isEmpty()) {
            for (String header : headers.keySet()) {
                request.addHeader(header, headers.get(header).toString());
            }
        }
    }

    private void setHttpHeaders(HttpHeaders httpHeaders, HttpRequestBase request) {
        Set<String> keys = httpHeaders.getRequestHeaders().keySet();
        request.setHeaders(new Header[]{});
        for (String key : keys) {
            if (!key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH) && !key.equalsIgnoreCase(HttpHeaders.HOST)) {
                if (key.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                    request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
                } else {
                    request.setHeader(key, httpHeaders.getRequestHeaders().getFirst(key));
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