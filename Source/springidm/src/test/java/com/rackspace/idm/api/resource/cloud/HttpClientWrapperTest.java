package com.rackspace.idm.api.resource.cloud;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/20/12
 * Time: 4:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpClientWrapperTest {

    HttpClientWrapper httpClientWrapper;
    HttpClientWrapper spy;

    @Before
    public void setUp() throws Exception {
        httpClientWrapper = new HttpClientWrapper();
        spy = spy(httpClientWrapper);
    }


    @Test
    public void put_setsEntity() throws Exception {
        doNothing().when(spy).setEntity(any(HttpPut.class),eq("body"));
        doReturn("success").when(spy).executeRequest(any(HttpPut.class));
        spy.put("body");
        verify(spy).setEntity(any(HttpPut.class),eq("body"));
    }

    @Test
    public void put_returnsExecuteRequest() throws Exception {
        doNothing().when(spy).setEntity(any(HttpPut.class),eq("body"));
        doReturn("success").when(spy).executeRequest(any(HttpPut.class));
        assertThat("returns string",spy.put("body"),equalTo("success"));
    }

    @Test
    public void delete_returnsExecuteRequest() throws Exception {
        doReturn("success").when(spy).executeRequest(any(HttpDelete.class));
        assertThat("returns string",spy.put("body"),equalTo("success"));
    }

    @Test
    public void getContentLength_returnsNegativeOne() throws Exception {
        HttpEntity httpEntity = mock(HttpEntity.class);
        HttpClientWrapper.GzipDecompressingEntity gzipDecompressingEntity = new HttpClientWrapper.GzipDecompressingEntity(httpEntity);
        assertThat("return -1", gzipDecompressingEntity.getContentLength(), equalTo((long) -1));
    }
}
