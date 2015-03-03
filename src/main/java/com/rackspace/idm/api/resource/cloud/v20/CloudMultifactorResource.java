package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.idm.domain.config.IdentityConfig;
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
public class CloudMultifactorResource {

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Autowired
    private IdentityConfig config;

    /**
     * The multifactor service to list devices for a given user.
     *
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    @GET
    @Path("mobile-phones")
    public Response listDevicesForUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return multiFactorCloud20Service.listDevicesForUser(uriInfo, authToken, userId).build();
    }

    /**
     * The multifactor service to associate a phone with a given user.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param mobilePhone
     * @return
     */
    @POST
    @Path("mobile-phones")
    public Response addMultiFactorMobilePhone(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            MobilePhone mobilePhone) {
        return multiFactorCloud20Service.addPhoneToUser(uriInfo, authToken, userId, mobilePhone).build();
    }

    /**
     * The multifactor service to send a verification code to a device.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param deviceId
     * @return
     */
    @POST
    @Path("mobile-phones/{deviceId}/verificationcode")
    public Response sendVerificationCode(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId) {
        return multiFactorCloud20Service.sendVerificationCode(uriInfo, authToken, userId, deviceId).build();
    }

    @POST
    @Path("mobile-phones/{deviceId}/verify")
    public Response verifyVerificationCode(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId,
            VerificationCode verificationCode) {
        return multiFactorCloud20Service.verifyVerificationCode(uriInfo, authToken, userId, deviceId, verificationCode).build();
    }

    @PUT
    public Response updateMultiFactorSettings(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            MultiFactor multiFactor) {
        return multiFactorCloud20Service.updateMultiFactorSettings(uriInfo, authToken, userId, multiFactor).build();
    }

    @DELETE
    public Response deleteMultiFactor(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return multiFactorCloud20Service.deleteMultiFactor(uriInfo, authToken, userId).build();
    }

    @POST
    @Path("bypass-codes")
    public Response generateBypassCodes(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            BypassCodes bypassCodes) {
        return multiFactorCloud20Service.generateBypassCodes(uriInfo, authToken, userId, bypassCodes).build();
    }

    @POST
    @Path("otp-devices")
    public Response addOTPDeviceToUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            OTPDevice otpDevice) {
        if (config.getStaticConfig().getOTPCreateEnabled()) {
            return multiFactorCloud20Service.addOTPDeviceToUser(uriInfo, authToken, userId, otpDevice).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("otp-devices/{deviceId}")
    public Response getOTPDeviceFromUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId) {
        return multiFactorCloud20Service.getOTPDeviceFromUser(uriInfo, authToken, userId, deviceId).build();
    }

    @DELETE
    @Path("otp-devices/{deviceId}")
    public Response deleteOTPDeviceFromUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId) {
        return multiFactorCloud20Service.deleteOTPDeviceFromUser(uriInfo, authToken, userId, deviceId).build();
    }

    @POST
    @Path("otp-devices/{deviceId}/verify")
    public Response verifyOTPCode(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId,
            VerificationCode verificationCode) {
        return multiFactorCloud20Service.verifyOTPCode(authToken, userId, deviceId, verificationCode).build();
    }

}
