package com.rackspace.idm.api.resource.cloud.atomHopper;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AtomHopperLogger {

    private static final Pattern pattern = Pattern.compile("<atom:id>(.*)</atom:id>");

    private static final String AUDIT_LOGGER_ID = "feeds";
    private CloseableHttpClient client;
    private Logger logger;

    public AtomHopperLogger(CloseableHttpClient client) {
        this.client = client;
        this.logger = LoggerFactory.getLogger(AUDIT_LOGGER_ID);
    }

    public CloseableHttpResponse execute(final HttpUriRequest request) throws IOException, ClientProtocolException {
        String method = request.getMethod();
        String uri = request.getURI().getPath();
        String entity = "none";

        if (request instanceof HttpPost) {
            HttpPost post = (HttpPost)request;
            entity = EntityUtils.toString(post.getEntity());
            post.setEntity(createRequestEntity(entity));
        }

        CloseableHttpResponse response;
        try {
            response = client.execute(request);
        } catch (IOException e) {
            logger.error("request[method: {} URL: {} payload: {}] response[exception: {} error: {}]", method, uri, entity, e.getClass(), e.getMessage());
            throw e;
        }

        int statusCode = response.getStatusLine().getStatusCode();
        String responseEntity = EntityUtils.toString(response.getEntity());
        response.setEntity(createRequestEntity(entity));

        if (statusCode == HttpServletResponse.SC_CREATED) {
            logger.info("request[method: {} URL: {} payload: {}] response[status: {} atom:id: {}]", method, uri, entity, statusCode, getAtomId(responseEntity));
        } else {
            logger.info("request[method: {} URL: {} payload: {}] response[status: {} error: {}]", method, uri, entity, statusCode, responseEntity);
        }

        return response;
    }

    public void close() throws IOException {
        client.close();
    }

    public ClientConnectionManager getConnManager() {
        return client.getConnectionManager();
    }

    private InputStreamEntity createRequestEntity(String s) throws UnsupportedEncodingException {
        return new InputStreamEntity(new ByteArrayInputStream(s.getBytes("UTF-8")), -1);
    }

    private String getAtomId(String entry) {
        Matcher matcher = pattern.matcher(entry);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
