package com.rackspace.idm.api.error;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.rackspace.api.common.fault.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    private final Logger logger = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);
    @Override
    public Response toResponse(WebApplicationException exception) {
        WebApplicationException wae = (WebApplicationException) exception;
        Throwable cause = wae.getCause();
        if (cause instanceof ClassCastException) {
                BadRequestFault fault = new BadRequestFault();
                fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
                fault.setMessage(exception.getMessage());
                return Response.ok(objectFactory.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
        }

        switch (wae.getResponse().getStatus()) {
            case HttpServletResponse.SC_BAD_REQUEST:
                BadRequestFault fault = new BadRequestFault();
                fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
                fault.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
            case HttpServletResponse.SC_UNAUTHORIZED:
                UnauthorizedFault ufault = new UnauthorizedFault();
                ufault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
                ufault.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createUnauthorized(ufault)).status(HttpServletResponse.SC_UNAUTHORIZED).build();
            case HttpServletResponse.SC_FORBIDDEN:
                ForbiddenFault ffault = new ForbiddenFault();
                ffault.setCode(HttpServletResponse.SC_FORBIDDEN);
                ffault.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createForbidden(ffault)).status(HttpServletResponse.SC_FORBIDDEN).build();
            case HttpServletResponse.SC_NOT_FOUND:
                ItemNotFoundFault ifault = new ItemNotFoundFault();
                ifault.setCode(HttpServletResponse.SC_NOT_FOUND);
                ifault.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createItemNotFound(ifault)).status(HttpServletResponse.SC_NOT_FOUND).build();
            case HttpServletResponse.SC_METHOD_NOT_ALLOWED:
                MethodNotAllowedFault mfault = new MethodNotAllowedFault();
                mfault.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                mfault.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createMethodNotAllowed(mfault)).status(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build();
            case HttpServletResponse.SC_NOT_ACCEPTABLE:
                List<Variant> variants = new ArrayList<Variant>();
                variants.add(new Variant(MediaType.APPLICATION_XML_TYPE, Locale.getDefault(), "UTF-8"));
                variants.add(new Variant(MediaType.APPLICATION_JSON_TYPE, Locale.getDefault(), "UTF-8"));
                return Response.notAcceptable(variants).build();
            case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE:
                UnsupportedMediaTypeFault unsupportedMediaTypeFault = new UnsupportedMediaTypeFault();
                unsupportedMediaTypeFault.setCode(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                unsupportedMediaTypeFault.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createUnsupportedMediaType(unsupportedMediaTypeFault)).status(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE).build();
            case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
                ServiceFault sfault = new ServiceFault();
                sfault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                sfault.setMessage(wae.getMessage());
                logger.error(exception.getMessage());
                return Response.ok(objectFactory.createServiceFault(sfault)).status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
            case HttpServletResponse.SC_SERVICE_UNAVAILABLE:
                ServiceUnavailableFault sufault = new ServiceUnavailableFault();
                sufault.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                sufault.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createServiceUnavailable(sufault)).status(HttpServletResponse.SC_SERVICE_UNAVAILABLE).build();
            default:
                ServiceUnavailableFault sufault2 = new ServiceUnavailableFault();
                sufault2.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                sufault2.setMessage(wae.getMessage());
                return Response.ok(objectFactory.createServiceUnavailable(sufault2)).status(HttpServletResponse.SC_SERVICE_UNAVAILABLE).build();
        }
    }
}
