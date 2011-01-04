package com.rackspace.idm.errors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.omg.CORBA.portable.ApplicationException;
import org.springframework.stereotype.Component;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ClientConflictException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateClientException;
import com.rackspace.idm.exceptions.DuplicateUsernameException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.IdmException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.PasswordValidationException;
import com.rackspace.idm.exceptions.PermissionConflictException;
import com.rackspace.idm.exceptions.UserDisabledException;
import com.rackspace.idm.jaxb.BadRequest;
import com.rackspace.idm.jaxb.ClientnameConflict;
import com.rackspace.idm.jaxb.CustomerIdConflict;
import com.rackspace.idm.jaxb.Forbidden;
import com.rackspace.idm.jaxb.IdmFault;
import com.rackspace.idm.jaxb.ItemNotFound;
import com.rackspace.idm.jaxb.MethodNotAllowed;
import com.rackspace.idm.jaxb.PasswordValidationFault;
import com.rackspace.idm.jaxb.PermissionIdConflict;
import com.rackspace.idm.jaxb.ServiceUnavailable;
import com.rackspace.idm.jaxb.ServerError;
import com.rackspace.idm.jaxb.Unauthorized;
import com.rackspace.idm.jaxb.UserDisabled;
import com.rackspace.idm.jaxb.UsernameConflict;

@Component
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    public Response toResponse(Throwable thrown) {
        Throwable e = thrown;

        if (thrown instanceof ApplicationException) {
            e = thrown.getCause();
        }

        if (e instanceof PermissionConflictException) {
            return toResponse(new PermissionIdConflict(), e, 409);
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
        if (e instanceof PasswordValidationException) {
            return toResponse(new PasswordValidationFault(), e, 400);
        }
        if (e instanceof NotAuthenticatedException
            || e instanceof NotAuthorizedException) {
            return toResponse(new Unauthorized(), e, 401);
        }
        if (e instanceof ForbiddenException) {
            return toResponse(new Forbidden(), e, 403);
        }
        if (e instanceof NotFoundException) {
            return toResponse(new ItemNotFound(), e, 404);
        }
        if (e instanceof com.sun.jersey.api.NotFoundException) {
            NotFoundException exp = new NotFoundException("Resource Not Found"); 
            return toResponse(new ItemNotFound(), exp, 404);
        }
        if (e instanceof DuplicateUsernameException) {
            return toResponse(new UsernameConflict(), e, 409);
        }
        if (e instanceof DuplicateClientException || e instanceof ClientConflictException) {
            return toResponse(new ClientnameConflict(), e, 409);
        }
        if (e instanceof ClassCastException) {
            return toResponse(new BadRequest(), e, 400);
        }
        if (e instanceof IdmException) {
            return toResponse(new ServerError(), e, 500);
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
                case 405:
                    Exception exp = new Exception("Method Not Allowed");
                    return toResponse(new MethodNotAllowed(), exp, 405);
                case 500:
                    return toResponse(new ServerError() , e.getCause(), 500);
                case 503:
                    return toResponse(new ServiceUnavailable(), e.getCause(),
                        503);
                default:
                    return toResponse(new ServiceUnavailable(), e.getCause(), wae
                        .getResponse().getStatus());
            }
        }

        return toResponse(new ServiceUnavailable(), e, 500);
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
        return Response.status(code).entity(fault).build();
    }
}
