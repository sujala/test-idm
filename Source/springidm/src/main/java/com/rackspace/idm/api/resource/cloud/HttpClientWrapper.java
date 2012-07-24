package com.rackspace.idm.api.resource.cloud;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;

import java.util.zip.GZIPInputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;

import org.apache.http.conn.scheme.Scheme;

import org.apache.http.conn.ssl.SSLSocketFactory;

import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;

import org.apache.http.HeaderElement;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;

import org.apache.http.impl.client.ContentEncodingHttpClient;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.protocol.HttpContext;

import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

@Component
public class HttpClientWrapper {

    private Map<String, String> headers;
    private String url = "";
    private HttpClient client;

    public HttpClientWrapper() {
        headers = new HashMap<String, String>();
        client = new DecompressingHttpClient(WebClientDevWrapper.wrapClient(new DefaultHttpClient()));
    }

    public HttpClientWrapper header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public String get() throws URISyntaxException, HttpException, IOException {
        HttpGet get = new HttpGet(url);
        return executeRequest(get);
    }

    public String put(String body) throws URISyntaxException, HttpException, IOException {
        HttpPut put = new HttpPut(url);
        setEntity(put, body);

        return executeRequest(put);
    }

    public String post(String body) throws URISyntaxException, HttpException, IOException {
        HttpPost post = new HttpPost(url);
        setEntity(post, body);

        return executeRequest(post);
    }

    public String delete() throws URISyntaxException, HttpException, IOException {
        HttpDelete delete = new HttpDelete(url);
        return executeRequest(delete);
    }

    void setEntity(HttpEntityEnclosingRequestBase requestBase, String body) throws UnsupportedEncodingException {
        StringEntity request = new StringEntity(body);
        request.setChunked(false);

        requestBase.setEntity(request);
    }

    String executeRequest(HttpRequestBase requestBase) throws URISyntaxException, HttpException, IOException {

        for (String key : headers.keySet()) {
            requestBase.addHeader(key, headers.get(key));
        }

        HttpResponse response = client.execute(requestBase);
        HttpEntity entity = response.getEntity();

        String result = null;
        if (entity != null) {
            result = EntityUtils.toString(entity);
        }
        return result;
    }

    public HttpClientWrapper url(String url)
    {
        this.headers = new HashMap<String, String>();
        this.url = url;
        return this;
    }
}
