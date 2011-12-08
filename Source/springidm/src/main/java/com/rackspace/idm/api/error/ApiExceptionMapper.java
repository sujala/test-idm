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
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.api.common.fault.v1.BadRequestFault;
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
    private final Logger logger = LoggerFactory
        .getLogger(ApiExceptionMapper.class);
    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory cloud_of = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();
    private final com.rackspace.api.common.fault.v1.ObjectFactory rax_of = new com.rackspace.api.common.fault.v1.ObjectFactory();
    private final com.rackspace.api.idm.v1.ObjectFactory ga_of = new com.rackspace.api.idm.v1.ObjectFactory();

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

        String detail = MDC.get(Audit.GUUID);

        if (e instanceof NotProvisionedException) {
            NotProvisionedFault fault = new NotProvisionedFault();
            fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createNotProvisioned(fault)).status(Response.Status.FORBIDDEN).build();
        }

        if (e instanceof NumberFormatException || e instanceof BadRequestException || e instanceof ClassCastException) {
            BadRequestFault fault = new BadRequestFault();
            fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(rax_of.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
        }

        if (e instanceof PermissionConflictException) {
            PermisionIdConflictFault fault = new PermisionIdConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createPermissionIdConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof BaseUrlConflictException) {
            BaseUrlIdConflictFault fault = new BaseUrlIdConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createBaseUrlIdConflict(fault)).status(Response.Status.CONFLICT).build();
        }
        if (e instanceof DuplicateClientGroupException) {
            ClientGroupConflictFault fault = new ClientGroupConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createClientGroupConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof CustomerConflictException) {
            CustomerIdConflictFault fault = new CustomerIdConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createCustomerIdConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof UserDisabledException) {
            UserDisabledFault fault = new UserDisabledFault();
            fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createUserDisabled(fault)).status(Response.Status.FORBIDDEN).build();
        }

        if (e instanceof PasswordValidationException) {
            PasswordValidationFault fault = new PasswordValidationFault();
            fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createPasswordValidationFault(fault)).status(Response.Status.BAD_REQUEST).build();
        }

        if (e instanceof PasswordSelfUpdateTooSoonException) {
            PasswordSelfUpdateTooSoonFault fault = new PasswordSelfUpdateTooSoonFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createPasswordSelfUpdateTooSoonFault(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof StalePasswordException) {
            StalePasswordFault fault = new StalePasswordFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createStalePasswordFault(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof NotAuthenticatedException || e instanceof NotAuthorizedException) {
            UnauthorizedFault fault = new UnauthorizedFault();
            fault.setCode(Response.Status.UNAUTHORIZED.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(rax_of.createUnauthorized(fault)).status(Response.Status.UNAUTHORIZED).build();
        }
        
        if (e instanceof CloudAdminAuthorizationException) {
            AuthFault afault = new AuthFault();
            afault.setCode(405);
            afault.setMessage(e.getMessage());
            afault.setDetails(detail);
            return Response.ok(cloud_of.createAuthFault(afault)).status(405).build();
        }
        
        if (e instanceof ForbiddenException) {
            ForbiddenFault fault = new ForbiddenFault();
            fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(rax_of.createForbidden(fault)).status(Response.Status.FORBIDDEN).build();
        }

        if (e instanceof NotFoundException) {
            ItemNotFoundFault fault = new ItemNotFoundFault();
            fault.setCode(Response.Status.NOT_FOUND.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(rax_of.createItemNotFound(fault)).status(Response.Status.NOT_FOUND).build();
        }
        if (e instanceof com.sun.jersey.api.NotFoundException) {
            ItemNotFoundFault fault = new ItemNotFoundFault();
            fault.setCode(Response.Status.NOT_FOUND.getStatusCode());
            fault.setMessage("Resource Not Found");
            fault.setDetail(detail);
            return Response.ok(rax_of.createItemNotFound(fault)).status(Response.Status.NOT_FOUND).build();
        }

        if (e instanceof DuplicateUsernameException) {
            UsernameConflictFault fault = new UsernameConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createUsernameConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof DuplicateClientException || e instanceof ClientConflictException) {
            ApplicationNameConflictFault fault = new ApplicationNameConflictFault();
            fault.setCode(Response.Status.CONFLICT.getStatusCode());
            fault.setMessage(e.getMessage());
            fault.setDetail(detail);
            return Response.ok(ga_of.createApplicationNameConflict(fault)).status(Response.Status.CONFLICT).build();
        }

        if (e instanceof IdmException) {
            ServiceFault fault = new ServiceFault();
            fault.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            fault.setDetail(detail);
            return Response.ok(rax_of.createServiceFault(fault)).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (e instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) e;

            Throwable cause = wae.getCause();
            if (cause != null) {
                //
                // Common user errors
                //
                if (cause instanceof ClassCastException) {
                    BadRequestFault fault = new BadRequestFault();
                    fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
                    fault.setMessage(e.getMessage());
                    fault.setDetail(detail);
                    return Response.ok(rax_of.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
                }
            }

            switch (wae.getResponse().getStatus()) {
                case 400:
                    BadRequestFault fault = new BadRequestFault();
                    fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
                    fault.setMessage(wae.getMessage());
                    fault.setDetail(detail);
                    return Response.ok(rax_of.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
                case 401:
                    UnauthorizedFault ufault = new UnauthorizedFault();
                    ufault.setCode(401);
                    ufault.setMessage(wae.getMessage());
                    ufault.setDetail(detail);

                    return Response.ok(rax_of.createUnauthorized(ufault))
                        .status(401).build();
                case 403:
                    ForbiddenFault ffault = new ForbiddenFault();
                    ffault.setCode(403);
                    ffault.setMessage(wae.getMessage());
                    ffault.setDetail(detail);

                    return Response.ok(rax_of.createForbidden(ffault))
                        .status(403).build();
                case 404:
                    ItemNotFoundFault ifault = new ItemNotFoundFault();
                    ifault.setCode(404);
                    ifault.setMessage(wae.getMessage());
                    ifault.setDetail(detail);

                    return Response.ok(rax_of.createItemNotFound(ifault))
                        .status(404).build();
                case 405:
                    MethodNotAllowedFault mfault = new MethodNotAllowedFault();
                    mfault.setCode(405);
                    mfault.setMessage(wae.getMessage());
                    mfault.setDetail(detail);

                    return Response.ok(rax_of.createMethodNotAllowed(mfault))
                        .status(405).build();
                case 406:
                    List<Variant> variants = new ArrayList<Variant>();
                    variants.add(new Variant(MediaType.APPLICATION_XML_TYPE,
                        Locale.getDefault(), "UTF-8"));
                    variants.add(new Variant(MediaType.APPLICATION_JSON_TYPE,
                        Locale.getDefault(), "UTF-8"));
                    return Response.notAcceptable(variants).build();
                case 500:
                    ServiceFault sfault = new ServiceFault();
                    sfault.setCode(500);
                    sfault.setMessage(wae.getMessage());
                    sfault.setDetail(detail);

                    return Response.ok(rax_of.createServiceFault(sfault))
                        .status(500).build();
                case 503:
                    ServiceUnavailableFault sufault = new ServiceUnavailableFault();
                    sufault.setCode(503);
                    sufault.setMessage(wae.getMessage());
                    sufault.setDetail(detail);

                    return Response
                        .ok(rax_of.createServiceUnavailable(sufault))
                        .status(503).build();
                default:
                    ServiceUnavailableFault sufault2 = new ServiceUnavailableFault();
                    sufault2.setCode(503);
                    sufault2.setMessage(wae.getMessage());
                    sufault2.setDetail(detail);

                    return Response
                        .ok(rax_of.createServiceUnavailable(sufault2))
                        .status(503).build();
            }
        }

        logger.error(e.getCause() == null ? e.getMessage() : e.getCause()
            .getMessage());

        ServiceFault sfault = new ServiceFault();
        sfault.setCode(500);
        sfault.setMessage("Server Error");
        sfault.setDetail(detail);

        return Response.ok(rax_of.createServiceFault(sfault)).status(500)
            .build();
    }
}
