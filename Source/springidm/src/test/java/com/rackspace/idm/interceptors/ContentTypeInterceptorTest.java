package com.rackspace.idm.interceptors;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import junit.framework.Assert;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.GlobalConstants;

public class ContentTypeInterceptorTest {
    private ContentTypeInterceptor interceptor;
    private MockHttpRequest request;
    private MockHttpServletResponse response;
    private MockMessageBodyWriterContext context;

    @Before
    public void setUp() throws URISyntaxException {
        request = MockHttpRequest.get("/foo");
        response = new MockHttpServletResponse();
        interceptor = new ContentTypeInterceptor(request, response);
        context = new MockMessageBodyWriterContext();
    }

    @Test
    public void shouldDoNothingWhenContentTypeAttributeNotSet()
        throws WebApplicationException, IOException {
        interceptor.write(context);
        Assert.assertNull(context.getMediaType());
    }

    @Test
    public void shouldSetMediaTypeWhenContentTypeIsJson()
        throws WebApplicationException, IOException {
        request.setAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT,
            MediaType.APPLICATION_JSON);
        interceptor.write(context);
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, context
            .getMediaType());
    }

    @Test
    public void shouldSetMediaTypeWhenContentTypeIsXml()
        throws WebApplicationException, IOException {
        request.setAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT,
            MediaType.APPLICATION_XML);
        interceptor.write(context);
        Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, context
            .getMediaType());
    }

    static class MockMessageBodyWriterContext implements
        MessageBodyWriterContext {

        private MediaType mediaType;

        @Override
        public Annotation[] getAnnotations() {
            return null;
        }

        @Override
        public Object getEntity() {
            return null;
        }

        @Override
        public Type getGenericType() {
            return null;
        }

        @Override
        public MultivaluedMap<String, Object> getHeaders() {
            return null;
        }

        @Override
        public MediaType getMediaType() {
            return mediaType;
        }

        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        public Class getType() {
            return null;
        }

        @Override
        public void proceed() throws IOException, WebApplicationException {
        }

        @Override
        public void setAnnotations(Annotation[] annotations) {
        }

        @Override
        public void setEntity(Object entity) {
        }

        @Override
        public void setGenericType(Type genericType) {
        }

        @Override
        public void setMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
        }

        @Override
        public void setOutputStream(OutputStream os) {
        }

        @Override
        public void setType(Class type) {
        }

    }
}
