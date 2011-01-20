package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.GlobalConstants;

/**
 * API Version
 * 
 */
@Path("/")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class VersionResource {

    private AuthResource authResource;
    private UsersResource usersResource;
    private CustomersResource customersResource;
    private MossoUserResource mossoUserResource;
    private NastUserResource nastUserResource;
    private PasswordRulesResource passwordRulesResource;
    private TokenResource tokenResource;
    private BaseUrlsResource baseUrlsResource;

    @Autowired
    public VersionResource(AuthResource authResource,
        UsersResource usersResource, CustomersResource customersResource,
        MossoUserResource mossoUserResource, NastUserResource nastUserResource,
        PasswordRulesResource passwordRulesResource,
        TokenResource tokenResource, BaseUrlsResource baseUrlsResource) {
        this.authResource = authResource;
        this.usersResource = usersResource;
        this.customersResource = customersResource;
        this.mossoUserResource = mossoUserResource;
        this.nastUserResource = nastUserResource;
        this.passwordRulesResource = passwordRulesResource;
        this.tokenResource = tokenResource;
        this.baseUrlsResource = baseUrlsResource;
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
        version.setDocURL(GlobalConstants.DOC_URL);
        version.setId(GlobalConstants.VERSION);
        version.setStatus(Enum.valueOf(
            com.rackspace.idm.jaxb.VersionStatus.class,
            GlobalConstants.VERSION_STATUS.toUpperCase()));
        version.setWadl(GlobalConstants.WADL_URL);

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
}
