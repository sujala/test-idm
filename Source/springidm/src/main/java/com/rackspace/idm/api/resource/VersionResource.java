package com.rackspace.idm.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.auth.AuthResource;
import com.rackspace.idm.api.resource.authorize.AuthorizeResource;
import com.rackspace.idm.api.resource.baseurl.BaseUrlsResource;
import com.rackspace.idm.api.resource.customer.CustomersResource;
import com.rackspace.idm.api.resource.mosso.MossoUserResource;
import com.rackspace.idm.api.resource.nast.NastUserResource;
import com.rackspace.idm.api.resource.passwordrule.PasswordRulesResource;
import com.rackspace.idm.api.resource.token.TokenResource;
import com.rackspace.idm.api.resource.user.UsersResource;
import com.rackspace.idm.api.resource.scope.ScopeResource;
import com.rackspace.idm.domain.service.ApiDocService;

/**
 * API Version
 * 
 */
@Path("/")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class VersionResource {

    private final AuthorizeResource authorizeResource;
    private final AuthResource authResource;
    private final UsersResource usersResource;
    private final CustomersResource customersResource;
    private final MossoUserResource mossoUserResource;
    private final NastUserResource nastUserResource;
    private final PasswordRulesResource passwordRulesResource;
    private final TokenResource tokenResource;
    private final BaseUrlsResource baseUrlsResource;
    private final ScopeResource scopeAccessResource;
    private final ApiDocService apiDocService;
    private final Configuration config;

    @Autowired
    public VersionResource(AuthorizeResource authorizeResource,AuthResource authResource, UsersResource usersResource,
        CustomersResource customersResource, MossoUserResource mossoUserResource,
        NastUserResource nastUserResource, PasswordRulesResource passwordRulesResource,
        TokenResource tokenResource, BaseUrlsResource baseUrlsResource, ScopeResource scopeAccessResource, ApiDocService apiDocService,
        Configuration config) {
        this.authorizeResource = authorizeResource;
        this.authResource = authResource;
        this.usersResource = usersResource;
        this.customersResource = customersResource;
        this.mossoUserResource = mossoUserResource;
        this.nastUserResource = nastUserResource;
        this.passwordRulesResource = passwordRulesResource;
        this.tokenResource = tokenResource;
        this.baseUrlsResource = baseUrlsResource;
        this.scopeAccessResource = scopeAccessResource;
        this.apiDocService = apiDocService;
        this.config = config;
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
     */
    @GET
    public Response getVersionInfo() {
        com.rackspace.idm.jaxb.Version version = new com.rackspace.idm.jaxb.Version();
        version.setDocURL(config.getString("app.version.doc.url"));
        version.setId(config.getString("app.version"));
        version.setStatus(Enum.valueOf(com.rackspace.idm.jaxb.VersionStatus.class,
            config.getString("app.version.status").toUpperCase()));
        version.setWadl(config.getString("app.version.wadl.url"));

        return Response.ok(version).build();
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

    @Path("auth")
    public AuthResource getAuthResource() {
        return authResource;
    }

    @Path("token")
    public TokenResource getTokenResource() {
        return tokenResource;
    }

    @Path("baseurls")
    public BaseUrlsResource getBaseUrlsResource() {
        return baseUrlsResource;
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
    
    @GET
    @Path("scopeAccess")
    public ScopeResource getScopeAccesses() {
        return scopeAccessResource;   
    }
    
    @Path("authorize")
    public AuthorizeResource getAuthorizeResource() {
        return authorizeResource;
    }
}
