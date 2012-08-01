package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Client application resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("applicationApplicationsResource")
public class ApplicationsResource extends ParentResource {

	private final ApplicationResource applicationResource;
    private final ApplicationConverter applicationConverter;
    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    
    @Autowired
    public ApplicationsResource(
    	ApplicationResource applicationResource,
        ApplicationService applicationService,
        ApplicationConverter applicationConverter,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
        super(inputValidator);
        this.applicationResource = applicationResource;
        this.applicationService = applicationService;
        this.applicationConverter = applicationConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets applications
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param name application name
     */
    @GET
    public Response getApplications(
        @QueryParam("name") String name,
        @QueryParam("offset") Integer offset,
        @QueryParam("limit") Integer limit,
        @HeaderParam("X-Auth-Token") String authHeader) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        List<FilterParam> filters = null;
    	if (!StringUtils.isBlank(name)) {
    		filters = new ArrayList<FilterParam>();
            filters.add(new FilterParam(FilterParamName.APPLICATION_NAME, name));
    	}
    	
    	//TODO: Filter application list based on current user if exposing to public
    	Applications applications = this.applicationService.getAllApplications(filters, (offset == null ? -1 : offset), (limit == null ? -1 : limit));
    	
    	return Response.ok(this.applicationConverter.toClientListJaxb(applications)).build();
    }

    
    /**
     * Add an application
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     */
    @POST
    public Response addApplication(
        @HeaderParam("X-Auth-Token") String authHeader, 
        EntityHolder<com.rackspace.api.idm.v1.Application> holder) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        try {
        	validateRequestBody(holder);
            
            Application applicationDO = applicationConverter.toClientDO(holder.getEntity());
            applicationDO.setDefaults();
            validateDomainObject(applicationDO);

            applicationService.add(applicationDO);
            getLogger().info("Added Application: {}", applicationDO);

            String location = applicationDO.getClientId();
            
            return Response.ok(applicationConverter.toClientJaxbWithPermissionsAndCredentials(applicationDO)).location(URI.create(location)).status(HttpServletResponse.SC_CREATED).build();
        } 
        catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            getLogger().warn(errorMsg);
            throw new ClientConflictException(errorMsg, ex);
        }  
    }
    
    @Path("{applicationId}")
    public ApplicationResource getApplicationResource() {
        return applicationResource;
    }
}
