package com.rackspace.idm.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionRequest;
import com.rackspace.idm.api.resource.cloud.devops.DevOpsService;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.event.ApiResourceType;
import com.rackspace.idm.event.IdentityApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DevOpsResource {

    @Autowired
    private DevOpsService devOpsService;

    @Autowired
    private IdentityConfig identityConfig;

    public static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";
    public static final String X_SUBJECT_TOKEN = "X-SUBJECT-TOKEN";

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Encrypt users ")
    @PUT
    @Path("cloud/users/encrypt")
    public Response encryptUsers(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        devOpsService.encryptUsers(authToken);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    /**
     * Retrieves a log of ldap calls made while processing a previous request where the X-LOG-LDAP header (with a value of true) to the request
     *
     * Only callable by service admins and when the configuration property "allow.ldap.logging" is set to true in the configuration files (will
     * return 404 otherwise)
     *
     * @param uriInfo
     * @param authToken
     * @param logName
     * @return
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Get LDAP log")
    @GET
    @Path("/ldap/log/{logName}")
    @Produces({MediaType.APPLICATION_XML})
    public Response getLog(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("logName") String logName) {
        return devOpsService.getLdapLog(uriInfo, authToken, logName).build();
    }

    /**
     * Retrieves the current node state for KeyCzar cached keys.
     *
     * @return The metadata format is JSON only since it is just for internal use.
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Get AE metadata")
    @GET
    @Path("/keystore/meta")
    public Response getKeyMetadata(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return devOpsService.getKeyMetadata(authToken).build();
    }

    /**
     * Reset the KeyCzar cache.
     *
     * @return The metadata format is JSON only since it is just for internal use.
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Reset AE metadata")
    @PUT
    @Path("/keystore/meta")
    public Response resetKeyMetadata(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return devOpsService.resetKeyMetadata(authToken).build();
    }

    /**
     * Retrieves the IDM properties.
     *
     * @return IDM properties as JSON; JSON only since it is just for internal use.
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps List configuration properties")
    @GET
    @Path("/props")
    public Response getIdmProps(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                @QueryParam("versions") List<String> versions,
                                @QueryParam("name") String name) {
        return devOpsService.getIdmPropsByQuery(authToken, versions, name).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Add configuration property")
    @POST
    @Path("/props")
    public Response createIdmProp(@HeaderParam(X_AUTH_TOKEN) String authToken, IdentityProperty identityProperty) {
        return devOpsService.createIdmProperty(authToken, identityProperty).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Update configuration property")
    @PUT
    @Path("/props/{propertyId}")
    public Response updateIdmProp(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                  @PathParam("propertyId") String propertyId,
                                  IdentityProperty identityProperty) {
        return devOpsService.updateIdmProperty(authToken, propertyId, identityProperty).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Delete configuration property")
    @DELETE
    @Path("/props/{propertyId}")
    public Response deleteIdmProp(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                  @PathParam("propertyId") String propertyId) {
        return devOpsService.deleteIdmProperty(authToken, propertyId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Delete expired federated users")
    @POST
    @Path("/federation/deletion")
    public Response expiredFederatedUsersDeletion(@HeaderParam(X_AUTH_TOKEN) String authToken, FederatedUsersDeletionRequest request) {
        return devOpsService.expiredFederatedUsersDeletion(authToken, request).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Delete expired TRRs")
    @POST
    @Path("/token-revocation-record/deletion")
    public Response purgeObsoleteTrrs(@HeaderParam(X_AUTH_TOKEN) String authToken, TokenRevocationRecordDeletionRequest request) {
        return devOpsService.purgeObsoleteTrrs(authToken, request).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Analyze token")
    @GET
    @Path("/tokens/analyze")
    @Produces({MediaType.APPLICATION_JSON})
    public Response analyzeToken(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                 @HeaderParam(X_SUBJECT_TOKEN) String subjectToken,
                                 @QueryParam("tokenId") String tokenId) {
        return devOpsService.analyzeToken(authToken, subjectToken).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "DevOps Update domain user-admin reference")
    @PUT
    @Path("/migrate/domains/{domainId}/admin")
    @Produces({MediaType.APPLICATION_JSON})
    public Response migrateDomainAdmin(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                      @PathParam("domainId") String domainId) {
        return devOpsService.migrateDomainAdmin(authToken, domainId).build();
    }
}
