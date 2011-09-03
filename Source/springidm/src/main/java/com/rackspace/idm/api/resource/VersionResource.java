package com.rackspace.idm.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.CloudVersionsResource;
import com.rackspace.idm.api.resource.customer.CustomersResource;
import com.rackspace.idm.api.resource.mosso.MossoUserResource;
import com.rackspace.idm.api.resource.nast.NastUserResource;
import com.rackspace.idm.api.resource.passwordrule.PasswordRulesResource;
import com.rackspace.idm.api.resource.racker.RackersResource;
import com.rackspace.idm.api.resource.scope.ScopesResource;
import com.rackspace.idm.api.resource.token.TokenResource;
import com.rackspace.idm.api.resource.user.UsersResource;
import com.rackspace.idm.api.serviceprofile.CanonicalContractDescriptionBuilder;
import com.rackspace.idm.api.serviceprofile.ServiceProfileDescriptionBuilder;
import com.rackspace.idm.domain.service.ApiDocService;

/**
 * API Version
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class VersionResource {

    private final UsersResource usersResource;
    private final CustomersResource customersResource;
    private final MossoUserResource mossoUserResource;
    private final NastUserResource nastUserResource;
    private final PasswordRulesResource passwordRulesResource;
    private final TokenResource tokenResource;
    private final ScopesResource scopeAccessResource;
    private final RackersResource rackersResource;
    private final CloudVersionsResource cloudVersionsResource;
    private final ApiDocService apiDocService;
    private final Configuration config;
    private final CanonicalContractDescriptionBuilder canonicalContractDescriptionBuilder;

    @Context
    private UriInfo uriInfo;
    
    @Autowired
    public VersionResource(UsersResource usersResource,
        CustomersResource customersResource, MossoUserResource mossoUserResource,
        NastUserResource nastUserResource, PasswordRulesResource passwordRulesResource,
        TokenResource tokenResource, ScopesResource scopeAccessResource,
        CloudVersionsResource cloudVersionsResource, ApiDocService apiDocService,
        RackersResource rackersResource, Configuration config,
        CanonicalContractDescriptionBuilder canonicalContractDescriptionBuilder) {
        this.usersResource = usersResource;
        this.customersResource = customersResource;
        this.mossoUserResource = mossoUserResource;
        this.nastUserResource = nastUserResource;
        this.passwordRulesResource = passwordRulesResource;
        this.tokenResource = tokenResource;
        this.scopeAccessResource = scopeAccessResource;
        this.rackersResource = rackersResource;
        this.cloudVersionsResource = cloudVersionsResource;
        this.apiDocService = apiDocService;
        this.config = config;
        this.canonicalContractDescriptionBuilder = canonicalContractDescriptionBuilder;
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
     * Gets the API Version info for public consumers.
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
    @Path("public")
    public Response getPublicVersionInfo(@PathParam("versionId") String versionId) {
      	final String responseXml = canonicalContractDescriptionBuilder.buildPublicVersionPage(versionId, uriInfo);
    	return Response.ok(responseXml).build();
    }
    
    @Path("customers")
    public CustomersResource getCustomersResource() {
        return customersResource;
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return usersResource;
    }

    @Path("mosso")
    public MossoUserResource getMossoUserResource() {
        return mossoUserResource;
    }

    @Path("nast")
    public NastUserResource getNastUserResource() {
        return nastUserResource;
    }

    @Path("passwordrules")
    public PasswordRulesResource getPasswordRulesResource() {
        return passwordRulesResource;
    }

    @Path("token")
    public TokenResource getTokenResource() {
        return tokenResource;
    }

    @Path("scopes")
    public ScopesResource getScopeAccesses() {
        return scopeAccessResource;   
    }
    
    @Path("rackers")
    public RackersResource getRackersResource() {
        return rackersResource;   
    }

    @GET
    @Path("xsd/{fileName}")
    public Response getXSD(@PathParam("fileName") String fileName) {
        String xsdContent = apiDocService.getXsd(fileName);
        return Response.ok(xsdContent).build();
    }

    @GET
    @Path("xslt/{fileName}")
    public Response getXSLT(@PathParam("fileName") String fileName) {
        String xsltContent = apiDocService.getXslt();
        return Response.ok(xsltContent).build();
    }

    @GET
    @Path("application.wadl")
    public Response getWadl2() {
        return getWadl();
    }

    @GET
    @Path("idm.wadl")
    public Response getWadl() {
        String myString = apiDocService.getWadl();
        return Response.ok(myString).build();
    }
}
