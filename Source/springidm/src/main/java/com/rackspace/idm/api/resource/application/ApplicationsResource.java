package com.rackspace.idm.api.resource.application;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Client application resource.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("applicationApplicationsResource")
public class ApplicationsResource extends ParentResource {

	private final ApplicationResource applicationResource;
    private final ScopeAccessService scopeAccessService;
    private final ApplicationConverter applicationConverter;
    private final ApplicationService applicationService;
    private final AuthorizationService authorizationService;
    
    @Autowired
    public ApplicationsResource(
    	ApplicationResource applicationResource,
        ApplicationService applicationService, ScopeAccessService scopeAccessService,
        ApplicationConverter applicationConverter,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
        super(inputValidator);
        this.applicationResource = applicationResource;
        this.applicationService = applicationService;
        this.scopeAccessService = scopeAccessService;
        this.applicationConverter = applicationConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets applications
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param gets all the applications
     */
    @GET
    public Response getApplications(@Context Request request,
        @Context UriInfo uriInfo,
        @QueryParam("name") String name,
        @QueryParam("offset") Integer offset,
        @QueryParam("limit") Integer limit,
        @HeaderParam("X-Auth-Token") String authHeader) {
    	
    	ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
    	// Racker's, Specific Clients and Admins are authorized
    	//TODO: Implement authorization rules
    	//authorizationService.authorizeToken(token, uriInfo);
    	
    	FilterParam[] filters = null;
    	if (!StringUtils.isBlank(name)) {
    		filters = new FilterParam[] { new FilterParam(FilterParamName.APPLICATION_NAME, name)};
    	}
    	
    	//TODO: Filter application list based on current user if exposing to public
    	Applications applications = this.applicationService.getAllApplications(filters, (offset == null ? -1 : offset), (limit == null ? -1 : limit));
    	
    	return Response.ok(this.applicationConverter.toClientListJaxb(applications)).build();
    }

    
    /**
     * Add an application
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param application
     */
    @POST
    public Response addApplication(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader, 
        EntityHolder<com.rackspace.api.idm.v1.Application> holder) {

        try {
        	validateRequestBody(holder);
        	
            ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
            // Racker's, Specific Clients and Admins are authorized
            //TODO: Implement authorization rules
            //authorizationService.authorizeToken(token, uriInfo);
            
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
            throw new ClientConflictException(errorMsg);
        }  
    }
    
    @Path("{applicationId}")
    public ApplicationResource getApplicationResource() {
        return applicationResource;
    }
}
