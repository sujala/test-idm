package com.rackspace.idm.api.resource.tenant;

import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.Consumes;

import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.rackspace.api.idm.v1.IdmFault;
import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.api.idm.v1.Tenant;

import com.rackspace.idm.api.converter.TenantConverter;

import com.rackspace.idm.api.resource.ParentResource;

import com.rackspace.idm.audit.Audit;

import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;

import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TenantsResource extends ParentResource {

	private final AuthorizationService authorizationService;
    private final TenantService tenantService;
    private final TenantConverter tenantConverter;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
	public TenantsResource(InputValidator inputValidator,
        AuthorizationService authorizationService,
        TenantService tenantService,
        TenantConverter tenantConverter) {
		super(inputValidator);
        this.authorizationService = authorizationService;
        this.tenantService = tenantService;
        this.tenantConverter = tenantConverter;
	}

    @POST
    @Path("")
    public Response createTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        Tenant tenant) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        // Our implmentation has the id and the name the same
        tenant.setId(tenant.getName());
        tenantService.addTenant(tenantConverter.toTenantDO(tenant));

        com.rackspace.idm.domain.entity.Tenant tenantObject = tenantService.getTenant(tenant.getName(), tenant.getScopeId());

        return Response.ok(objectFactory.createTenant(tenantConverter.toTenant(tenantObject))).build();
    }

    @GET
    @Path("{tenantId}/scope/{scopeId}")
    public Response getTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        @PathParam("tenantId") String tenantId,
        @PathParam("scopeId") String scopeId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        com.rackspace.idm.domain.entity.Tenant tenant;

        tenant = tenantService.getTenant(tenantId, scopeId);

        if(tenant == null) {
            String errMsg = String.format("Tenant with id/name: '%s' and scopeid: %s was not found.", tenantId, scopeId);
            logger.warn(errMsg);
            throw new WebApplicationException(new NotFoundException(errMsg), 404);
        }

        return Response.ok(objectFactory.createTenant(tenantConverter.toTenant(tenant))).build();
    }

    @DELETE
    @Path("{tenantId}/scope/{scopeId}")
    public Response deleteTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        @PathParam("tenantId") String tenantId,
        @PathParam("scopeId") String scopeId) {

        tenantService.deleteTenant(tenantId, scopeId);

        return Response.noContent().build();
    }

    @PUT
    @Path("{tenantId}/scope/{scopeId}")
    public Response updateTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        Tenant tenant) {

        tenantService.updateTenant(tenantConverter.toTenantDO(tenant));
        com.rackspace.idm.domain.entity.Tenant tenantObject = tenantService.getTenant(tenant.getName(), tenant.getScopeId());

        return Response.ok(objectFactory.createTenant(tenantConverter.toTenant(tenantObject))).build();
    }

}
