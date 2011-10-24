package com.rackspace.idm.api.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.api.common.fault.v1.BadRequestFault;
import com.rackspace.api.common.fault.v1.Detail;
import com.rackspace.api.common.fault.v1.Fault;
import com.rackspace.api.common.fault.v1.ForbiddenFault;
import com.rackspace.api.common.fault.v1.ItemNotFoundFault;
import com.rackspace.api.common.fault.v1.MethodNotAllowedFault;
import com.rackspace.api.common.fault.v1.ServiceFault;
import com.rackspace.api.common.fault.v1.ServiceUnavailableFault;
import com.rackspace.api.common.fault.v1.UnauthorizedFault;
import com.rackspace.api.idm.v1.ApplicationNameConflictFault;
import com.rackspace.api.idm.v1.BaseUrlIdConflictFault;
import com.rackspace.api.idm.v1.ClientGroupConflictFault;
import com.rackspace.api.idm.v1.CustomerIdConflictFault;
import com.rackspace.api.idm.v1.NotProvisionedFault;
import com.rackspace.api.idm.v1.PasswordSelfUpdateTooSoonFault;
import com.rackspace.api.idm.v1.PasswordValidationFault;
import com.rackspace.api.idm.v1.PermisionIdConflictFault;
import com.rackspace.api.idm.v1.StalePasswordFault;
import com.rackspace.api.idm.v1.UserDisabledFault;
import com.rackspace.api.idm.v1.UsernameConflictFault;
import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.CloudAdminAuthorizationException;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateClientException;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.NotProvisionedException;
import com.rackspace.idm.exception.PasswordSelfUpdateTooSoonException;
import com.rackspace.idm.exception.PasswordValidationException;
import com.rackspace.idm.exception.PermissionConflictException;
import com.rackspace.idm.exception.StalePasswordException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspacecloud.docs.auth.api.v1.AuthFault;

@Component
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
    private final ResourceBundle faultMessageConfig;
    private final Logger logger = LoggerFactory.getLogger(ApiExceptionMapper.class);
    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory objectFactory = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    @Context
    private HttpHeaders headers;

    @Autowired
    public ApiExceptionMapper(ResourceBundle faultMessageConfig) {
        this.faultMessageConfig = faultMessageConfig;
    }

    @Override
    public Response toResponse(Throwable thrown) {
        Throwable e = thrown;

        if (thrown instanceof ApplicationException) {
            e = thrown.getCause();
        }

        if (e instanceof NotProvisionedException) {
            return toResponse(new NotProvisionedFault(), e, 403);
        }
        if (e instanceof NumberFormatException) {
            return toResponse(new BadRequestFault(), e, 400);
        }
        if (e instanceof PermissionConflictException) {
            return toResponse(new PermisionIdConflictFault(), e, 409);
        }
        if (e instanceof BaseUrlConflictException) {
            return toResponse(new BaseUrlIdConflictFault(), e, 409);
        }
        if (e instanceof DuplicateClientGroupException) {
            return toResponse(new ClientGroupConflictFault(), e, 409);
        }
        if (e instanceof CustomerConflictException) {
            return toResponse(new CustomerIdConflictFault(), e, 409);
        }
        if (e instanceof UserDisabledException) {
            return toResponse(new UserDisabledFault(), e, 403);
        }
        if (e instanceof BadRequestException) {
            return toResponse(new BadRequestFault(), e, 400);
        }
        if (e instanceof PasswordValidationException) {
            return toResponse(new PasswordValidationFault(), e, 400);
        }
        if (e instanceof PasswordSelfUpdateTooSoonException) {
            return toResponse(new PasswordSelfUpdateTooSoonFault(), e, 409);
        }
        if (e instanceof StalePasswordException) {
            return toResponse(new StalePasswordFault(), e, 409);
        }
        if (e instanceof NotAuthenticatedException || e instanceof NotAuthorizedException) {
            return toResponse(new UnauthorizedFault(), e, 401);
        }
        if (e instanceof CloudAdminAuthorizationException) {
            return toResponse(new AuthFault(), e, 405);
        }
        if (e instanceof ForbiddenException) {
            return toResponse(new ForbiddenFault(), e, 403);
        }
        if (e instanceof NotFoundException) {
            return toResponse(new ItemNotFoundFault(), e, 404);
        }
        if (e instanceof com.sun.jersey.api.NotFoundException) {
            NotFoundException exp = new NotFoundException("Resource Not Found");
            return toResponse(new ItemNotFoundFault(), exp, 404);
        }
        if (e instanceof DuplicateUsernameException) {
            return toResponse(new UsernameConflictFault(), e, 409);
        }
        if (e instanceof DuplicateClientException || e instanceof ClientConflictException) {
            return toResponse(new ApplicationNameConflictFault(), e, 409);
        }
        if (e instanceof ClassCastException) {
            return toResponse(new BadRequestFault(), e, 400);
        }
        if (e instanceof IdmException) {
            return toResponse(new ServiceFault(), e, 500);
        }
        if (e instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) e;

            Throwable cause = wae.getCause();
            if (cause != null) {
                //
                // Common user errors
                //
                if (cause instanceof ClassCastException) {
                    return toResponse(new BadRequestFault(), cause, 400);
                }
            }

            switch (wae.getResponse().getStatus()) {
                case 400:
                    return toResponse(new BadRequestFault(), e.getCause(), 400);
                case 401:
                    return toResponse(new UnauthorizedFault(), e.getCause(), 401);
                case 403:
                    return toResponse(new ForbiddenFault(), e.getCause(), 403);
                case 404:
                    return toResponse(new ItemNotFoundFault(), e.getCause(), 404);
                case 405:
                    Exception exp = new Exception("Method Not Allowed");
                    return toResponse(new MethodNotAllowedFault(), exp, 405);
                case 406:
                    List<Variant> variants = new ArrayList<Variant>();
                    variants.add(new Variant(MediaType.APPLICATION_XML_TYPE, Locale.getDefault(), "UTF-8"));
                    variants.add(new Variant(MediaType.APPLICATION_JSON_TYPE, Locale.getDefault(), "UTF-8"));
                    return Response.notAcceptable(variants).build();
                case 500:
                    return toResponse(new ServiceFault(), e.getCause(), 500);
                case 503:
                    return toResponse(new ServiceUnavailableFault(), e.getCause(), 503);
                default:
                    return toResponse(new ServiceUnavailableFault(), e.getCause(), wae.getResponse().getStatus());
            }
        }

        logger.error(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        return toResponse(new ServiceFault(), new Exception("Server Error"), 500);
    }

    private Response toResponse(Fault fault, Throwable t, int code) {
        fault.setCode(code);

        String msg = null;
        String dtl = null;
        String faultClassName = fault.getClass().getSimpleName();
        try {
            msg = faultMessageConfig.getString(faultClassName + ".msg");
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

        dtl = MDC.get(Audit.GUUID);

        fault.setMessage(msg);
        
        Detail detail = new Detail();
        detail.setDescription(dtl);
        fault.setDetail(detail);

        ResponseBuilder builder = Response.status(code).entity(fault);
        return builder.build();
    }

    private Response toResponse(AuthFault fault, Throwable t, int code) {
        fault.setCode(code);

        String msg = null;
        String dtl = null;
        String faultClassName = fault.getClass().getSimpleName();
        try {
            msg = faultMessageConfig.getString(faultClassName + ".msg");
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

        dtl = MDC.get(Audit.GUUID);

        fault.setMessage(msg);
        fault.setDetails(dtl);

        ResponseBuilder builder = Response.status(code).entity(objectFactory.createAuthFault(fault));
        return builder.build();
    }
}
