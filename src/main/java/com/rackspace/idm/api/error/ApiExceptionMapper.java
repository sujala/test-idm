package com.rackspace.idm.api.error;

import com.rackspace.api.common.fault.v1.*;
import com.rackspace.api.idm.v1.*;
import com.rackspace.idm.exception.*;
import com.rackspacecloud.docs.auth.api.v1.AuthFault;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
    private final Logger logger = LoggerFactory.getLogger(ApiExceptionMapper.class);
    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory cloudOf = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();
    private final com.rackspace.api.common.fault.v1.ObjectFactory raxOf = new com.rackspace.api.common.fault.v1.ObjectFactory();
    private final com.rackspace.api.idm.v1.ObjectFactory gaOf = new com.rackspace.api.idm.v1.ObjectFactory();

    public ApiExceptionMapper() {}

    @Override
    public Response toResponse(Throwable thrown) {
        Throwable e = thrown;

        if (thrown instanceof ApplicationException) {
            e = thrown.getCause();
        }

        if (e instanceof NotProvisionedException) {
            NotProvisionedFault fault = new NotProvisionedFault();
            fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createNotProvisioned(fault)).status(Response.Status.FORBIDDEN).build();
        }

        if (e instanceof NumberFormatException || e instanceof BadRequestException || e instanceof ClassCastException) {
            BadRequestFault fault = new BadRequestFault();
            fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(raxOf.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
        }

        if (e instanceof PermissionConflictException) {
            PermisionIdConflictFault fault = new PermisionIdConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createPermissionIdConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof BaseUrlConflictException) {
            BaseUrlIdConflictFault fault = new BaseUrlIdConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createBaseUrlIdConflict(fault)).status(Response.Status.CONFLICT).build();
        }
        if (e instanceof DuplicateClientGroupException) {
            ClientGroupConflictFault fault = new ClientGroupConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createClientGroupConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof DuplicateException) {
            ServiceFault fault = new ServiceFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(raxOf.createServiceFault(fault)).build();
        }

        if (e instanceof CustomerConflictException) {
            CustomerIdConflictFault fault = new CustomerIdConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createCustomerIdConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof UserDisabledException) {
            UserDisabledFault fault = new UserDisabledFault();
            fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createUserDisabled(fault)).status(Response.Status.FORBIDDEN).build();
        }

        if (e instanceof PasswordValidationException) {
            PasswordValidationFault fault = new PasswordValidationFault();
            fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createPasswordValidationFault(fault)).status(Response.Status.BAD_REQUEST).build();
        }

        if (e instanceof PasswordSelfUpdateTooSoonException) {
            PasswordSelfUpdateTooSoonFault fault = new PasswordSelfUpdateTooSoonFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createPasswordSelfUpdateTooSoonFault(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof StalePasswordException) {
            StalePasswordFault fault = new StalePasswordFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createStalePasswordFault(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof NotAuthenticatedException || e instanceof NotAuthorizedException) {
            UnauthorizedFault fault = new UnauthorizedFault();
            fault.setCode(Response.Status.UNAUTHORIZED.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(raxOf.createUnauthorized(fault)).status(Response.Status.UNAUTHORIZED).build();
        }
        
        if (e instanceof CloudAdminAuthorizationException) {
            AuthFault afault = new AuthFault();
            afault.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            afault.setMessage(e.getMessage());
            return Response.ok(cloudOf.createAuthFault(afault)).status(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build();
        }
        
        if (e instanceof ForbiddenException) {
            ForbiddenFault fault = new ForbiddenFault();
            fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(raxOf.createForbidden(fault)).status(Response.Status.FORBIDDEN).build();
        }

        if (e instanceof NotFoundException) {
            ItemNotFoundFault fault = new ItemNotFoundFault();
            fault.setCode(Response.Status.NOT_FOUND.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(raxOf.createItemNotFound(fault)).status(Response.Status.NOT_FOUND).build();
        }
        if (e instanceof com.sun.jersey.api.NotFoundException) {
            ItemNotFoundFault fault = new ItemNotFoundFault();
            fault.setCode(Response.Status.NOT_FOUND.getStatusCode());
            fault.setMessage("Resource Not Found");
            return Response.ok(raxOf.createItemNotFound(fault)).status(Response.Status.NOT_FOUND).build();
        }

        if (e instanceof DuplicateUsernameException) {
            UsernameConflictFault fault = new UsernameConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createUsernameConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof DuplicateClientException || e instanceof ClientConflictException) {
            ApplicationNameConflictFault fault = new ApplicationNameConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            return Response.ok(gaOf.createApplicationNameConflict(fault)).status(Response.Status.CONFLICT).build();
        }
        if (e instanceof IdmException) {
            ServiceFault fault = new ServiceFault();
            fault.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            logger.info(e.getMessage());
            return Response.ok(raxOf.createServiceFault(fault)).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (e instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) e;
            Throwable cause = wae.getCause();
            if (cause instanceof ClassCastException) {
                    BadRequestFault fault = new BadRequestFault();
                    fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
                    fault.setMessage(e.getMessage());
                    return Response.ok(raxOf.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
            }

            switch (wae.getResponse().getStatus()) {
                case HttpServletResponse.SC_BAD_REQUEST:
                    BadRequestFault fault = new BadRequestFault();
                    fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
                    fault.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
                case HttpServletResponse.SC_UNAUTHORIZED:
                    UnauthorizedFault ufault = new UnauthorizedFault();
                    ufault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
                    ufault.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createUnauthorized(ufault)).status(HttpServletResponse.SC_UNAUTHORIZED).build();
                case HttpServletResponse.SC_FORBIDDEN:
                    ForbiddenFault ffault = new ForbiddenFault();
                    ffault.setCode(HttpServletResponse.SC_FORBIDDEN);
                    ffault.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createForbidden(ffault)).status(HttpServletResponse.SC_FORBIDDEN).build();
                case HttpServletResponse.SC_NOT_FOUND:
                    ItemNotFoundFault ifault = new ItemNotFoundFault();
                    ifault.setCode(HttpServletResponse.SC_NOT_FOUND);
                    ifault.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createItemNotFound(ifault)).status(HttpServletResponse.SC_NOT_FOUND).build();
                case HttpServletResponse.SC_METHOD_NOT_ALLOWED:
                    MethodNotAllowedFault mfault = new MethodNotAllowedFault();
                    mfault.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    mfault.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createMethodNotAllowed(mfault)).status(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build();
                case HttpServletResponse.SC_NOT_ACCEPTABLE:
                    List<Variant> variants = new ArrayList<Variant>();
                    variants.add(new Variant(MediaType.APPLICATION_XML_TYPE, Locale.getDefault(), "UTF-8"));
                    variants.add(new Variant(MediaType.APPLICATION_JSON_TYPE, Locale.getDefault(), "UTF-8"));
                    return Response.notAcceptable(variants).build();
                case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE:
                    UnsupportedMediaTypeFault unsupportedMediaTypeFault = new UnsupportedMediaTypeFault();
                    unsupportedMediaTypeFault.setCode(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    unsupportedMediaTypeFault.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createUnsupportedMediaType(unsupportedMediaTypeFault)).status(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE).build();
                case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
                    ServiceFault sfault = new ServiceFault();
                    sfault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    sfault.setMessage(wae.getMessage());
                    logger.error(e.getMessage());
                    return Response.ok(raxOf.createServiceFault(sfault)).status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
                case HttpServletResponse.SC_SERVICE_UNAVAILABLE:
                    ServiceUnavailableFault sufault = new ServiceUnavailableFault();
                    sufault.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    sufault.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createServiceUnavailable(sufault)).status(HttpServletResponse.SC_SERVICE_UNAVAILABLE).build();
                default:
                    ServiceUnavailableFault sufault2 = new ServiceUnavailableFault();
                    sufault2.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    sufault2.setMessage(wae.getMessage());
                    return Response.ok(raxOf.createServiceUnavailable(sufault2)).status(HttpServletResponse.SC_SERVICE_UNAVAILABLE).build();
            }
        }
        logger.error(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        logger.error(e.getMessage());
        logger.error("Exception is :::",e);
        ServiceFault sfault = new ServiceFault();
        sfault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        sfault.setMessage("Server Error");
        return Response.ok(raxOf.createServiceFault(sfault)).status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
    }
}
