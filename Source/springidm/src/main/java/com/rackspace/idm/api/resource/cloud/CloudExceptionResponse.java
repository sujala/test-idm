package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.exception.*;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 12/29/11
 * Time: 1:55 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class CloudExceptionResponse extends WebApplicationException {

    private static final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public CloudExceptionResponse(Response.ResponseBuilder response) {
        super(response.build());
     }

    public CloudExceptionResponse() {
    }

    public Response.ResponseBuilder exceptionResponse(Exception ex) {
        if (ex instanceof NotFoundException) {
            return notFoundExceptionResponse(ex.getMessage());
        }
        if (ex instanceof UserDisabledException) {
            return userDisabledExceptionResponse(ex.getMessage());
        }
        if (ex instanceof DuplicateUsernameException) {
            return usernameConflictExceptionResponse(ex.getMessage());
        }
        if (ex instanceof DuplicateException) {
            return usernameConflictExceptionResponse(ex.getMessage());
        }
        if (ex instanceof NotAuthenticatedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        }
        if (ex instanceof BadRequestException) {
            return badRequestExceptionResponse(ex.getMessage());
        }
        if (ex instanceof CloudAdminAuthorizationException) {
            return methodNotAllowedExceptionResponse(ex.getMessage());
        }
        if (ex instanceof NotAuthorizedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        }
        if (ex instanceof NumberFormatException) {
            return badRequestExceptionResponse("baseURLId not an integer");
        }
        if (ex instanceof BaseUrlConflictException) {
            return badRequestExceptionResponse(ex.getMessage());
        }

        return serviceExceptionResponse();
    }
    
    public Response.ResponseBuilder notAuthenticatedExceptionResponse(String errMsg) {
        UnauthorizedFault fault = OBJ_FACTORY.createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(errMsg);
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).entity(OBJ_FACTORY.createUnauthorized(fault).getValue());
    }

    public Response.ResponseBuilder notFoundExceptionResponse(String message) {
        ItemNotFoundFault fault = OBJ_FACTORY.createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(OBJ_FACTORY.createItemNotFound(fault).getValue());
    }

    public Response.ResponseBuilder usernameConflictExceptionResponse(String message) {
        UsernameConflictFault fault = OBJ_FACTORY.createUsernameConflictFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_CONFLICT).entity(OBJ_FACTORY.createUsernameConflict(fault).getValue());
    }

    public Response.ResponseBuilder serviceExceptionResponse() {
        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(OBJ_FACTORY.createAuthFault(fault).getValue());
    }

    public Response.ResponseBuilder userDisabledExceptionResponse(String message) {
        UserDisabledFault fault = OBJ_FACTORY.createUserDisabledFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_FORBIDDEN).entity(OBJ_FACTORY.createUserDisabled(fault).getValue());
    }

    public Response.ResponseBuilder methodNotAllowedExceptionResponse(String message) {
        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_METHOD_NOT_ALLOWED).entity(OBJ_FACTORY.createAuthFault(fault).getValue());
    }

    public Response.ResponseBuilder badRequestExceptionResponse(String message) {
        BadRequestFault fault = OBJ_FACTORY.createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_BAD_REQUEST);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(OBJ_FACTORY.createBadRequest(fault).getValue());
    }

}
