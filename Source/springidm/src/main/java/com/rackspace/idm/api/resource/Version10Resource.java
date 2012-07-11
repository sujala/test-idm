package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.application.ApplicationsResource;
import com.rackspace.idm.api.resource.cloud.CloudVersionsResource;
import com.rackspace.idm.api.resource.customeridentityprofile.CustomerIdentityProfilesResource;
import com.rackspace.idm.api.resource.passwordrule.PasswordRulesResource;
import com.rackspace.idm.api.resource.roles.RolesResource;
import com.rackspace.idm.api.resource.tenant.TenantsResource;
import com.rackspace.idm.api.resource.token.TokensResource;
import com.rackspace.idm.api.resource.user.RackerResource;
import com.rackspace.idm.api.resource.user.UsersResource;
import com.rackspace.idm.api.serviceprofile.CanonicalContractDescriptionBuilder;
import com.rackspace.idm.domain.service.ApiDocService;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * API Version
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Version10Resource {

	private final ApiDocService apiDocService;
	private final RolesResource rolesResource;
    private final UsersResource usersResource;
    private final CustomerIdentityProfilesResource customerIdentityProfilesResource;
    private final PasswordRulesResource passwordRulesResource;
    private final TokensResource tokenResource;
    private final ApplicationsResource applicationsResource;
    private final CanonicalContractDescriptionBuilder canonicalContractDescriptionBuilder;
    private final TenantsResource tenantsResource;
    private final RackerResource rackerResource;

    @Context
    private UriInfo uriInfo;
    
    @Autowired
    public Version10Resource(UsersResource usersResource,
        CustomerIdentityProfilesResource customersResource, PasswordRulesResource passwordRulesResource,
        TokensResource tokenResource, RolesResource rolesResource,
        CloudVersionsResource cloudVersionsResource, ApiDocService apiDocService,
        Configuration config,
        CanonicalContractDescriptionBuilder canonicalContractDescriptionBuilder,
        ApplicationsResource applicationsResource,
        TenantsResource tenantsResource,
        RackerResource rackerResource) {
        this.usersResource = usersResource;
        this.customerIdentityProfilesResource = customersResource;
        this.passwordRulesResource = passwordRulesResource;
        this.tokenResource = tokenResource;
        this.canonicalContractDescriptionBuilder = canonicalContractDescriptionBuilder;
        this.applicationsResource = applicationsResource;
        this.rolesResource = rolesResource;
        this.apiDocService = apiDocService;
        this.tenantsResource = tenantsResource;
        this.rackerResource = rackerResource;
    }

    /**
     * Gets the API Version info.
     *
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}version
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param versionId Version Number
     */
    @GET
    public Response getInternalVersionInfo(@PathParam("versionId") String versionId) {
      	final String responseXml = canonicalContractDescriptionBuilder.buildInternalVersionPage(versionId, uriInfo);
    	return Response.ok(responseXml).build();
    }

    /**
     * Gets the public documentations for this version of the API.
     *
     * @param versionId Version Number
     */
    @GET
    @Path("public")
    public Response getPublicVersionInfo(@PathParam("versionId") String versionId) {
      	final String responseXml = canonicalContractDescriptionBuilder.buildPublicVersionPage(versionId, uriInfo);
    	return Response.ok(responseXml).build();
    }
    
    @Path("customeridentityprofiles")
    public CustomerIdentityProfilesResource getCustomerIdentityProfilesResource() {
        return customerIdentityProfilesResource;
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return usersResource;
    }
   
    @Path("passwordrules")
    public PasswordRulesResource getPasswordRulesResource() {
        return passwordRulesResource;
    }

    @Path("tokens")
    public TokensResource getTokenResource() {
        return tokenResource;
    }

    @Path("tenants")
    public TenantsResource getTenantResource() {
        return tenantsResource;
    }
    
    @Path("applications")
    public ApplicationsResource getApplicationsResource() {
        return applicationsResource;
    }
    
    @Path("roles")
    public RolesResource getRolesResource() {
        return rolesResource;
    }

    @Path("rackers")
    public RackerResource getRackerResource() {
        return rackerResource;
    }

    @GET
    @Path("idm.wadl")
    public Response getWadl() {
        String myString = apiDocService.getWadl();
        return Response.ok(myString).build();
    }
}
