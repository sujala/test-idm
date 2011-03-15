package com.rackspace.idm.api.error;

import java.util.List;
import java.util.ResourceBundle;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.domain.config.LoggerFactoryWrapper;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateClientException;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.PasswordSelfUpdateTooSoonException;
import com.rackspace.idm.exception.PasswordValidationException;
import com.rackspace.idm.exception.PermissionConflictException;
import com.rackspace.idm.exception.StalePasswordException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.jaxb.BadRequest;
import com.rackspace.idm.jaxb.BaseUrlIdConflict;
import com.rackspace.idm.jaxb.ClientGroupConflict;
import com.rackspace.idm.jaxb.ClientnameConflict;
import com.rackspace.idm.jaxb.CustomerIdConflict;
import com.rackspace.idm.jaxb.Forbidden;
import com.rackspace.idm.jaxb.IdmFault;
import com.rackspace.idm.jaxb.ItemNotFound;
import com.rackspace.idm.jaxb.MethodNotAllowed;
import com.rackspace.idm.jaxb.PasswordSelfUpdateTooSoonFault;
import com.rackspace.idm.jaxb.PasswordValidationFault;
import com.rackspace.idm.jaxb.PermissionIdConflict;
import com.rackspace.idm.jaxb.ServerError;
import com.rackspace.idm.jaxb.ServiceUnavailable;
import com.rackspace.idm.jaxb.StalePasswordFault;
import com.rackspace.idm.jaxb.Unauthorized;
import com.rackspace.idm.jaxb.UserDisabled;
import com.rackspace.idm.jaxb.UsernameConflict;

@Component
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
    private ResourceBundle faultMessageConfig;
    private Logger logger = LoggerFactory.getLogger(ApiExceptionMapper.class);

    @Context
    private HttpHeaders headers;

    @Autowired
    public ApiExceptionMapper(ResourceBundle faultMessageConfig, LoggerFactoryWrapper loggerWrapper) {
        this.faultMessageConfig = faultMessageConfig;
    }

    public Response toResponse(Throwable thrown) {
        Throwable e = thrown;

        if (thrown instanceof ApplicationException) {
            e = thrown.getCause();
        }

        if (e instanceof NumberFormatException) {
            return toResponse(new BadRequest(), e, 400);
        }
        if (e instanceof PermissionConflictException) {
            return toResponse(new PermissionIdConflict(), e, 409);
        }
        if (e instanceof BaseUrlConflictException) {
            return toResponse(new BaseUrlIdConflict(), e, 409);
        }
        if (e instanceof DuplicateClientGroupException) {
            return toResponse(new ClientGroupConflict(), e, 409);
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
        if (e instanceof PasswordSelfUpdateTooSoonException) {
            return toResponse(new PasswordSelfUpdateTooSoonFault(), e, 400);
        }
        if (e instanceof StalePasswordException) {
            return toResponse(new StalePasswordFault(), e, 409);
        }
        if (e instanceof NotAuthenticatedException || e instanceof NotAuthorizedException) {
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
                    return toResponse(new ServerError(), e.getCause(), 500);
                case 503:
                    return toResponse(new ServiceUnavailable(), e.getCause(), 503);
                default:
                    return toResponse(new ServiceUnavailable(), e.getCause(), wae.getResponse().getStatus());
            }
        }

        logger.error(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        return toResponse(new ServerError(), e, 500);
    }

    private Response toResponse(IdmFault fault, Throwable t, int code) {
        fault.setCode(code);

        String msg = null;
        String dtl = null;
        String faultClassName = fault.getClass().getSimpleName();
        try {
            msg = faultMessageConfig.getString(faultClassName + ".msg");
            dtl = faultMessageConfig.getString(faultClassName + ".dtl");
        } catch (Exception e) {
            // Unable to load the message or details! Most likey no entry has
            // been made
            logger.error("Could not load fault message resource for {}:\n{}", faultClassName, e);
        }

        if (StringUtils.isBlank(msg)) {
            if (t == null) {
                msg = ErrorMsg.SERVER_ERROR;
            } else if (StringUtils.isBlank(t.getMessage())) {
                msg = faultClassName;
            } else {
                msg = t.getMessage();
            }
        }

        if (StringUtils.isBlank(dtl)) {
            if (t == null) {
                dtl = ErrorMsg.SERVER_ERROR;
            } else {
                dtl = t.getMessage();
            }
        }

        fault.setMessage(msg);
        fault.setDetails(dtl);

        ResponseBuilder builder = Response.status(code).entity(fault);
        List<String> acceptHeaderVals = headers.getRequestHeader(HttpHeaders.ACCEPT);
        if(acceptHeaderVals != null && acceptHeaderVals.size() > 0) {
	        boolean isOctetStreamResponse = acceptHeaderVals.get(0).equals(MediaType.APPLICATION_OCTET_STREAM);
	        if (isOctetStreamResponse) {
	            // Convert to a different response
	            MediaType respType = MediaType.APPLICATION_JSON_TYPE;
	            if (acceptHeaderVals.size() > 1) {
	                respType = MediaType.valueOf(acceptHeaderVals.get(1));
	            }
	            return builder.type(respType).build();
	        }
        }

        return builder.build();
    }
}
