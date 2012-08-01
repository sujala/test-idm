package com.rackspace.idm.api.resource.customeridentityprofile;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("identityProfileApplicationsResource")
public class ApplicationsResource extends ParentResource {

    private final ApplicationConverter applicationConverter;
    private final AuthorizationService authorizationService;
    private final ApplicationService applicationService;

    @Autowired
    public ApplicationsResource(
            InputValidator inputValidator, ApplicationConverter applicationConverter,
            AuthorizationService authorizationService, ApplicationService applicationService) {

        super(inputValidator);
        this.applicationConverter = applicationConverter;
        this.authorizationService = authorizationService;
        this.applicationService = applicationService;
    }

    /**
     * Gets applications that are bound to a specific customer
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId customer Id
     */
    @GET
    public Response getApplications(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("customerId") String customerId,
            @QueryParam("offset") Integer offset,
            @QueryParam("limit") Integer limit) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        Applications applications = this.applicationService.getByCustomerId(customerId, (offset == null ? -1 : offset), (limit == null ? -1 : limit));

        return Response.ok(applicationConverter.toClientListJaxb(applications)).build();
    }
}
