package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain;
import com.rackspace.idm.event.ApiResourceType;
import com.rackspace.idm.event.IdentityApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CloudMultifactorDomainResource {

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 Update domain mfa configuration")
    @PUT
    public Response updateMultiFactorDomainSettings(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            MultiFactorDomain multiFactorDomain) {
        return multiFactorCloud20Service.updateMultiFactorDomainSettings(uriInfo, authToken, domainId, multiFactorDomain).build();
    }
}
