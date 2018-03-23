package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.event.ApiResourceType;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.exception.ExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.ServiceUnavailableException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Component
public class DelegationAgreementResource {

    @Autowired
    private DelegationCloudService delegationCloudService;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @POST
    public Response addDelegationAgreement (
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(GlobalConstants.X_AUTH_TOKEN) String authToken,
            DelegationAgreement agreement) {
        try {
            verifyServiceEnabled();
            return delegationCloudService.addAgreement(uriInfo, authToken, agreement);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @GET
    @Path("/{agreementId}")
    public Response getDelegationAgreement (
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("agreementId") String agreementId,
            @HeaderParam(GlobalConstants.X_AUTH_TOKEN) String authToken) {
        try {
            verifyServiceEnabled();
            return delegationCloudService.getAgreement(authToken, agreementId);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @DELETE
    @Path("/{agreementId}")
    public Response deleteDelegationAgreement (
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("agreementId") String agreementId,
            @HeaderParam(GlobalConstants.X_AUTH_TOKEN) String authToken) {
        try {
            verifyServiceEnabled();
            return delegationCloudService.deleteAgreement(authToken, agreementId);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @PUT
    @Path("/{agreementId}/delegates/users/{userId}")
    public Response addUserDelegate (
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("agreementId") String agreementId,
            @PathParam("userId") String userId,
            @HeaderParam(GlobalConstants.X_AUTH_TOKEN) String authToken) {
        try {
            verifyServiceEnabled();
            return delegationCloudService.addDelegate(authToken, agreementId, new EndUserDelegateReference(userId));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @DELETE
    @Path("/{agreementId}/delegates/users/{userId}")
    public Response deleteUserDelegate (
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("agreementId") String agreementId,
            @PathParam("userId") String userId,
            @HeaderParam(GlobalConstants.X_AUTH_TOKEN) String authToken) {
        try {
            verifyServiceEnabled();
            return delegationCloudService.deleteDelegate(authToken, agreementId, new EndUserDelegateReference(userId));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @PUT
    @Path("/{agreementId}/delegates/groups/{groupId}")
    public Response addUserGroupDelegate (
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("agreementId") String agreementId,
            @PathParam("groupId") String groupId,
            @HeaderParam(GlobalConstants.X_AUTH_TOKEN) String authToken) {
        try {
            verifyServiceEnabled();
            return delegationCloudService.addDelegate(authToken, agreementId, new UserGroupDelegateReference(groupId));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE)
    @DELETE
    @Path("/{agreementId}/delegates/groups/{groupId}")
    public Response deleteUserGroupDelegate (
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("agreementId") String agreementId,
            @PathParam("groupId") String groupId,
            @HeaderParam(GlobalConstants.X_AUTH_TOKEN) String authToken) {
        try {
            verifyServiceEnabled();
            return delegationCloudService.deleteDelegate(authToken, agreementId, new UserGroupDelegateReference(groupId));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    private void verifyServiceEnabled() throws ServiceUnavailableException {
        if (!identityConfig.getReloadableConfig().areDelegationAgreementServicesEnabled()) {
            throw new ServiceUnavailableException(GlobalConstants.ERROR_MSG_SERVICE_NOT_FOUND);
        }
    }
}
