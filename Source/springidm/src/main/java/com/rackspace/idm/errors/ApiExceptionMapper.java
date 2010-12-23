package com.rackspace.idm.errors;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.jboss.resteasy.spi.ApplicationException;
import org.springframework.stereotype.Component;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateClientException;
import com.rackspace.idm.exceptions.DuplicateUsernameException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.IdmException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.PermissionConflictException;
import com.rackspace.idm.exceptions.UserDisabledException;
import com.rackspace.idm.faults.BadRequest;
import com.rackspace.idm.faults.CustomerIdConflict;
import com.rackspace.idm.faults.Forbidden;
import com.rackspace.idm.faults.IdmFault;
import com.rackspace.idm.faults.ItemNotFound;
import com.rackspace.idm.faults.ServiceUnavailable;
import com.rackspace.idm.faults.Unauthorized;
import com.rackspace.idm.faults.UserDisabled;
import com.rackspace.idm.faults.UsernameConflict;

@Component
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
    @Context
    private HttpHeaders headers;
    @Context
    private HttpServletRequest request;

    public Response toResponse(Throwable thrown) throws Failure {
        Throwable e = thrown;

        if (thrown instanceof ApplicationException) {
            e = thrown.getCause();
        }

        if (e instanceof PermissionConflictException) {
            return toResponse(new IdmFault(), e, 409);
        }
        if (e instanceof CustomerConflictException) {
            return toResponse(new CustomerIdConflict(), e, 409);
        }
        if (e instanceof UserDisabledException) {
            return toResponse(new UserDisabled(), e, 403);
        }
        if (e instanceof BadRequestException) {
            return toResponse(new BadRequest(), e, 400);
        }
        if (e instanceof NotAuthenticatedException
            || e instanceof UnauthorizedException
            || e instanceof NotAuthorizedException) {
            return toResponse(new Unauthorized(), e, 401);
        }
        if (e instanceof ForbiddenException ||
            e instanceof org.jboss.resteasy.spi.MethodNotAllowedException) {
            return toResponse(new Forbidden(), e, 403);
        }
        if (e instanceof NotFoundException ||
            e instanceof org.jboss.resteasy.spi.NotFoundException) {
            return toResponse(new ItemNotFound(), e, 404);
        }
        if (e instanceof DuplicateUsernameException) {
            return toResponse(new UsernameConflict(), e, 409);
        }
        if (e instanceof DuplicateClientException) {
            return toResponse(new IdmFault(), e, 409);
        }
        if (e instanceof ClassCastException) {
            return toResponse(new BadRequest(), e, 400);
        }
        if (e instanceof IdmException) {
            return toResponse(new IdmFault(), e, 500);
        }
        if (e instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) e;

            Throwable cause = wae.getCause();
            if (cause != null) {
                //
                // Common user errors
                //
                if (cause instanceof ClassCastException) {
                    return toResponse(new BadRequest(), cause, 400);
                }
            }

            switch (wae.getResponse().getStatus()) {
                case 400:
                    return toResponse(new BadRequest(), e.getCause(), 400);
                case 401:
                    return toResponse(new Unauthorized(), e.getCause(), 401);
                case 403:
                    return toResponse(new Forbidden(), e.getCause(), 403);
                case 404:
                    return toResponse(new ItemNotFound(), e.getCause(), 404);
                case 500:
                    return toResponse(new IdmFault(), e.getCause(), 500);
                case 503:
                    return toResponse(new ServiceUnavailable(), e.getCause(),
                        503);
                default:
                    return toResponse(new IdmFault(), e.getCause(), wae
                        .getResponse().getStatus());
            }
        }

        return toResponse(new IdmFault(), e, 500);
    }

    private Response toResponse(IdmFault fault, Throwable t, int code) {
        if ((t != null) && (t.getMessage() != null)) {
            fault.setMessage(t.getMessage());
        } else {
            fault.setMessage(fault.getClass().getSimpleName());
        }
        fault.setCode(code);

        if (t != null) {
            fault.setMessage(t.getMessage());
            fault.setDetails(t.getMessage());
        } else {
            fault.setMessage(ErrorMsg.SERVER_ERROR);
            fault.setDetails(ErrorMsg.SERVER_ERROR);
        }

        MediaType acceptType = MediaType.APPLICATION_JSON_TYPE;

        try {
            // URI extension overrides Accept header.
            Object uriExtType = request
                .getAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT);
            if (uriExtType == null) {
                List<MediaType> acceptTypes = headers.getAcceptableMediaTypes();
                if (acceptTypes != null && !acceptTypes.isEmpty()) {
                    MediaType headerAcceptType = acceptTypes.get(0);
                    if (headerAcceptType.equals(MediaType.APPLICATION_XML_TYPE)
                        || headerAcceptType.equals(MediaType.APPLICATION_JSON_TYPE)) {
                        acceptType = headerAcceptType;
                    }
                }
            } else {
                acceptType = MediaType.valueOf(uriExtType.toString());
            }
        } catch (Exception e) {
            acceptType = MediaType.APPLICATION_JSON_TYPE;
        }

        return Response.status(code).entity(fault).type(acceptType).build();
    }
}
