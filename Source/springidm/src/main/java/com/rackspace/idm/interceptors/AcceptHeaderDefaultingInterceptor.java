package com.rackspace.idm.interceptors;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.springframework.stereotype.Component;

import com.rackspace.idm.GlobalConstants;

@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
@Component
public class AcceptHeaderDefaultingInterceptor implements PreProcessInterceptor {

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method)
        throws Failure, WebApplicationException {
        List<MediaType> acceptHeaders = request.getHttpHeaders()
            .getAcceptableMediaTypes();
        if (acceptHeaders.contains(MediaType.APPLICATION_JSON_TYPE)
            || acceptHeaders.contains(MediaType.APPLICATION_XML_TYPE)) {
            return null;
        }
        request.setAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT,
            MediaType.APPLICATION_JSON);
        return null;
    }

}
