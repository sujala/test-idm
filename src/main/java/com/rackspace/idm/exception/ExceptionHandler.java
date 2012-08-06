package com.rackspace.idm.exception;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.openstack.docs.identity.api.v2.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/24/12
 * Time: 9:21 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class ExceptionHandler {

    @Autowired
    private JAXBObjectFactories objFactories;

    public Response.ResponseBuilder badRequestExceptionResponse(String message) {
        BadRequestFault fault = objFactories.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_BAD_REQUEST);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(
                objFactories.getOpenStackIdentityV2Factory().createBadRequest(fault).getValue());
    }

    public Response.ResponseBuilder notAuthenticatedExceptionResponse(String message) {
        UnauthorizedFault fault = objFactories.getOpenStackIdentityV2Factory().createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).entity(
                objFactories.getOpenStackIdentityV2Factory().createUnauthorized(fault).getValue());
    }

    public Response.ResponseBuilder forbiddenExceptionResponse(String errMsg) {
        ForbiddenFault fault = objFactories.getOpenStackIdentityV2Factory().createForbiddenFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(errMsg);
        return Response.status(HttpServletResponse.SC_FORBIDDEN).entity(
                objFactories.getOpenStackIdentityV2Factory().createForbidden(fault).getValue());
    }

    public Response.ResponseBuilder notFoundExceptionResponse(String message) {
        ItemNotFoundFault fault = objFactories.getOpenStackIdentityV2Factory().createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
                objFactories.getOpenStackIdentityV2Factory().createItemNotFound(fault).getValue());
    }

    public Response.ResponseBuilder tenantConflictExceptionResponse(String message) {
        TenantConflictFault fault = objFactories.getOpenStackIdentityV2Factory().createTenantConflictFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_CONFLICT).entity(
                objFactories.getOpenStackIdentityV2Factory().createTenantConflict(fault).getValue());
    }

    public Response.ResponseBuilder userDisabledExceptionResponse(String message) {
        UserDisabledFault fault = objFactories.getOpenStackIdentityV2Factory().createUserDisabledFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_FORBIDDEN).entity(
                objFactories.getOpenStackIdentityV2Factory().createUserDisabled(fault).getValue());
    }

    public Response.ResponseBuilder conflictExceptionResponse(String message) {
        BadRequestFault fault = objFactories.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        return Response.status(HttpServletResponse.SC_CONFLICT).entity(
                objFactories.getOpenStackIdentityV2Factory().createBadRequest(fault).getValue());
    }


    public Response.ResponseBuilder serviceExceptionResponse() {
        IdentityFault fault = objFactories.getOpenStackIdentityV2Factory().createIdentityFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(
                objFactories.getOpenStackIdentityV2Factory().createIdentityFault(fault).getValue());
    }

    public Response.ResponseBuilder exceptionResponse(Exception ex) {
        if (ex instanceof BadRequestException || ex instanceof StalePasswordException) {
            return badRequestExceptionResponse(ex.getMessage());

        } else if (ex instanceof NotAuthorizedException || ex instanceof NotAuthenticatedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());

        } else if (ex instanceof ForbiddenException) {
            return forbiddenExceptionResponse(ex.getMessage());

        } else if (ex instanceof NotFoundException) {
            return notFoundExceptionResponse(ex.getMessage());

        } else if (ex instanceof ClientConflictException) {
            return tenantConflictExceptionResponse(ex.getMessage());

        } else if (ex instanceof UserDisabledException) {
            return userDisabledExceptionResponse(ex.getMessage());

        } else if (ex instanceof DuplicateUsernameException || ex instanceof BaseUrlConflictException) {
            return conflictExceptionResponse(ex.getMessage());

        } else {
            return serviceExceptionResponse();
        }
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }
}
