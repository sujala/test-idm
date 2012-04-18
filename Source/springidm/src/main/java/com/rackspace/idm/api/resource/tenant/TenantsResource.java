package com.rackspace.idm.api.resource.tenant;

import java.net.URI;

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

import com.rackspace.idm.domain.entity.ScopeAccess;

import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TenantsResource extends ParentResource {

	private final AuthorizationService authorizationService;
    private final ScopeAccessService scopeAccessService;
    private final TenantService tenantService;
    private final TenantConverter tenantConverter;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
	public TenantsResource(InputValidator inputValidator,
        AuthorizationService authorizationService,
        ScopeAccessService scopeAccessService,
        TenantService tenantService,
        TenantConverter tenantConverter) {
		super(inputValidator);
        this.authorizationService = authorizationService;
        this.tenantService = tenantService;
        this.tenantConverter = tenantConverter;
        this.scopeAccessService = scopeAccessService;
	}

    @POST
    @Path("")
    public Response createTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        Tenant tenant) {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

        validateTenantId(tenant.getId());
        updateTenantFields(tenant, tenant.getId());

        tenantService.addTenant(tenantConverter.toTenantDO(tenant));

        com.rackspace.idm.domain.entity.Tenant tenantObject = tenantService.getTenant(tenant.getName());

        String locationUri = String.format("%s", tenantObject.getTenantId());
        
        return Response.ok(objectFactory.createTenant(tenantConverter.toTenant(tenantObject))).location(URI.create(locationUri)).status(HttpServletResponse.SC_CREATED).build();
    }

	@GET
    @Path("{tenantId}")
    public Response getTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        @PathParam("tenantId") String tenantId) {


        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

        com.rackspace.idm.domain.entity.Tenant tenant;

        tenant = tenantService.getTenant(tenantId);

        if(tenant == null) {
            String errMsg = String.format("Tenant with id/name: '%s' was not found.", tenantId);
            logger.warn(errMsg);
            throw new WebApplicationException(new NotFoundException(errMsg), 404);
        }

        return Response.ok(objectFactory.createTenant(tenantConverter.toTenant(tenant))).build();
    }

    @DELETE
    @Path("{tenantId}")
    public Response deleteTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        @PathParam("tenantId") String tenantId) {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

        tenantService.deleteTenant(tenantId);

        return Response.noContent().build();
    }

    @PUT
    @Path("{tenantId}")
    public Response updateTenant(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId,
        @QueryParam("applicationId") String applicationId,
        @PathParam("tenantId") String tenantId,
        Tenant tenant) {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

        updateTenantFields(tenant, tenantId);
        com.rackspace.idm.domain.entity.Tenant tenantObject = checkAndGetTenant(tenantId);

        tenantObject.setDescription(tenant.getDescription());
        tenantObject.setDisplayName(tenant.getDisplayName());
        tenantObject.setEnabled(tenant.isEnabled());

        tenantService.updateTenant(tenantObject);
        tenantObject = tenantService.getTenant(tenant.getName());

        return Response.ok(objectFactory.createTenant(tenantConverter.toTenant(tenantObject))).build();
    }

    private void validateTenantId(String tenantId) {
        if(tenantId != null) {
            int index = tenantId.indexOf(":");

            String namespace = null;
            String id = null;

            if (index != -1) {
                namespace = tenantId.substring(0, index);
                id = tenantId.substring(index + 1);
            }

            if (namespace == null || namespace.length() == 0 ||
                id == null || id.length() == 0) {

                String errMsg = String.format("Invalid Tenant id/name: '%s'.", tenantId);
                throw new WebApplicationException(new BadRequestException(errMsg), 404);
            }
        } else {
            String errMsg = String.format("Invalid Tenant id/name: '%s'.", tenantId);
            throw new WebApplicationException(new BadRequestException(errMsg), 404);
        }
	}

    private void updateTenantFields(Tenant tenant, String tenantId) {
        tenant.setId(tenantId);
        tenant.setName(tenantId);

        if (tenant.getDescription() != null && tenant.getDescription().length() == 0) {
            tenant.setDescription(null);
        }

        if (tenant.getDisplayName() != null && tenant.getDisplayName().length() == 0) {
            tenant.setDisplayName(null);
        }
    }

    com.rackspace.idm.domain.entity.Tenant checkAndGetTenant(String tenantId) {
        com.rackspace.idm.domain.entity.Tenant tenant = this.tenantService.getTenant(tenantId);

        if (tenant == null) {
            String errMsg = String.format("Tenant with id/name: '%s' was not found.", tenantId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenant;
    }
}
