package com.rackspace.idm.interceptors;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.jboss.resteasy.spi.interception.MessageBodyWriterInterceptor;
import org.springframework.stereotype.Component;

import com.rackspace.idm.GlobalConstants;

@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
@Component
/**
 * Defaults response content type to application/json, if unspecified.
 */
public class ContentTypeInterceptor implements MessageBodyWriterInterceptor {
    @Context
    private HttpRequest request;
    @Context
    private HttpServletResponse response;

    public ContentTypeInterceptor() {
        // This is used by RESTEasy
    }

    /**
     * Use for unit testing only
     * 
     * @param request
     *            Mock/stub instance of HttpRequest
     */
    ContentTypeInterceptor(HttpRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void write(MessageBodyWriterContext context) throws IOException,
        WebApplicationException {
        Object overrideType = request
            .getAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT);
        if (overrideType == null
            || StringUtils.isBlank(overrideType.toString())) {
            context.proceed();
            return;
        }

        response.setContentType(overrideType.toString());
        context.setMediaType(MediaType.valueOf(overrideType.toString()));
        context.proceed();
    }

}
