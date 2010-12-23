package services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class HttpClientService {

    private final int REQUEST_NUM_RETRIES = 3;
    private HttpClient httpClient;

    public HttpClientService() {
        this.httpClient = new DefaultHttpClient();
    }

    public HttpResponse sendGet(String url, Map headers)
            throws IOException, HttpException, ResourceNotFoundException
    {
        // Prepare a request object
        HttpGet httpget = new HttpGet(url);
        setupRequest(httpget, headers);

        // Execute the request
        HttpResponse response = httpClient.execute(httpget);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                throw new ResourceNotFoundException();
            }
            throw new HttpException("Http error: " + statusLine.getStatusCode());
        }
        
        return response;
    }

    public HttpResponse sendPost(String url, String contentType, Map headers, List<NameValuePair> data)
            throws IOException, HttpException
    {
        HttpPost httppost = new HttpPost(url);
        setupRequest(httppost, headers);

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(data, "UTF-8");
        httppost.setEntity(entity);
        entity.setContentType(contentType);

        // Execute the request
        HttpResponse response = httpClient.execute(httppost);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode != HttpStatus.SC_OK &&
                statusCode != HttpStatus.SC_CREATED) {
            throw new HttpException("Http error: " + statusLine.getStatusCode());
        }

        return response;
    }

    public HttpResponse sendPost(String url, String contentType, Map headers, String data)
            throws IOException, HttpException
    {
        HttpPost httppost = new HttpPost(url);
        setupRequest(httppost, headers);

        InputStreamEntity reqEntity = new InputStreamEntity(
                new ByteArrayInputStream(data.getBytes("utf-8")), -1);
        reqEntity.setContentType(contentType);
        httppost.setEntity(reqEntity);

        // Execute the request
        HttpResponse response = httpClient.execute(httppost);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode != HttpStatus.SC_OK &&
                statusCode != HttpStatus.SC_CREATED) {
            throw new HttpException("Http error: " + statusLine.getStatusCode());
        }

        return response;
    }

    public HttpResponse sendPut(String url, String contentType, Map headers, String data)
            throws IOException, HttpException
    {
        HttpPut httpput = new HttpPut(url);
        setupRequest(httpput, headers);

        InputStreamEntity reqEntity = new InputStreamEntity(
                new ByteArrayInputStream(data.getBytes("utf-8")), -1);
        reqEntity.setContentType(contentType);
        httpput.setEntity(reqEntity);

        // Execute the request
        HttpResponse response = httpClient.execute(httpput);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode != HttpStatus.SC_OK &&
                statusCode != HttpStatus.SC_CREATED) {
            throw new HttpException("Http error: " + statusLine.getStatusCode());
        }

        return response;
    }

    public HttpResponse sendDelete(String url, Map headers)
            throws IOException, HttpException
    {
        HttpDelete httpdelete = new HttpDelete(url);
        setupRequest(httpdelete, headers);

        HttpResponse response = httpClient.execute(httpdelete);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode != HttpStatus.SC_OK &&
                statusCode != HttpStatus.SC_NOT_FOUND) {
            throw new HttpException("Http error: " + statusLine.getStatusCode());
        }

        return response;
    }

    public String getResponseBody(HttpResponse response)
            throws IOException
    {
        HttpEntity entity = response.getEntity();
        String responseBody = "";
        if (entity != null) {
            long len = entity.getContentLength();
            if (len != -1 && len < 2048) {
                responseBody = EntityUtils.toString(entity);
            } else {
                // Stream content out
            }
        }
        return responseBody;
    }

    private void setupRequest(HttpRequest request, Map headers) {
        
        request.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
    		new DefaultHttpMethodRetryHandler(REQUEST_NUM_RETRIES, false));

        // set headers
        Set headerSet = headers.entrySet();
        Iterator i = headerSet.iterator();
        while(i.hasNext()){
            Map.Entry me = (Map.Entry)i.next();
            request.setHeader(me.getKey().toString(), me.getValue().toString());
        }
    }
}
