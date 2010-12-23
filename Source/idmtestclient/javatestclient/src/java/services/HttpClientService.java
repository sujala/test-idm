package services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            throws IOException, HttpException
    {
        // Prepare a request object
        HttpGet httpget = new HttpGet(url);
        if (headers != null) {
            setHeaders(httpget, headers);
        }

        // Execute the request
        HttpResponse response = httpClient.execute(httpget);

        return response;
    }

    public HttpResponse sendPost(String url, String contentType, Map headers, List<NameValuePair> data)
            throws IOException, HttpException
    {
        HttpPost httppost = new HttpPost(url);
        if (headers != null) {
            setHeaders(httppost, headers);
        }

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(data, "UTF-8");
        httppost.setEntity(entity);
        entity.setContentType(contentType);

        // Execute the request
        HttpResponse response = httpClient.execute(httppost);

        return response;
    }

    public HttpResponse sendPost(String url, String contentType, Map headers, String data)
            throws IOException, HttpException
    {
        HttpPost httppost = new HttpPost(url);
        if (headers != null) {
            setHeaders(httppost, headers);
        }

        InputStreamEntity reqEntity = new InputStreamEntity(
                new ByteArrayInputStream(data.getBytes("utf-8")), -1);
        reqEntity.setContentType(contentType);
        httppost.setEntity(reqEntity);

        // Execute the request
        HttpResponse response = httpClient.execute(httppost);

        return response;
    }

    public HttpResponse sendPut(String url, String contentType, Map headers, String data)
            throws IOException, HttpException
    {
        HttpPut httpput = new HttpPut(url);
        if (headers != null) {
            setHeaders(httpput, headers);
        }

        InputStreamEntity reqEntity = new InputStreamEntity(
                new ByteArrayInputStream(data.getBytes("utf-8")), -1);
        reqEntity.setContentType(contentType);
        httpput.setEntity(reqEntity);

        // Execute the request
        HttpResponse response = httpClient.execute(httpput);

        return response;
    }

    public HttpResponse sendDelete(String url, Map headers)
            throws IOException, HttpException
    {
        HttpDelete httpdelete = new HttpDelete(url);
        if (headers != null) {
            setHeaders(httpdelete, headers);
        }

        HttpResponse response = httpClient.execute(httpdelete);

        return response;
    }

    public String getResponseBody(HttpResponse response)
            throws IOException
    {
        HttpEntity entity = response.getEntity();
        String responseBody = "";
        if (entity != null) {
            responseBody = EntityUtils.toString(entity);
        }
        return responseBody;
    }

    private void setHeaders(HttpRequest request, Map headers) {
        
        // set headers
        Set headerSet = headers.entrySet();
        Iterator i = headerSet.iterator();
        while(i.hasNext()){
            Map.Entry me = (Map.Entry)i.next();
            request.setHeader(me.getKey().toString(), me.getValue().toString());
        }
    }
}
