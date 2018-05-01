package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
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

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CloudMultifactorResource {

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Autowired
    private IdentityConfig config;

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA List devices")
    @GET
    @Path("devices")
    public Response listMultiFactorDevicesForUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return multiFactorCloud20Service.listMultiFactorDevicesForUser(uriInfo, authToken, userId).build();
    }

    /**
     * The multifactor service to list mobile phone devices for a given user.
     *
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA List phone devices for user")
    @GET
    @Path("devices/mobile-phones")
    public Response listMobilePhoneDevicesForUserAlternative(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return multiFactorCloud20Service.listMobilePhoneDevicesForUser(uriInfo, authToken, userId).build();
    }

    /**
     * The multifactor service to list mobile phone devices for a given user.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA List phone devices for user (alias)")
    @GET
    @Path("mobile-phones")
    public Response listMobilePhoneDevicesForUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return listMobilePhoneDevicesForUserAlternative(uriInfo, authToken, userId);
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
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Add phone for user")
    @POST
    @Path("mobile-phones")
    public Response addMultiFactorMobilePhone(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            MobilePhone mobilePhone) {
        return multiFactorCloud20Service.addPhoneToUser(uriInfo, authToken, userId, mobilePhone).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Get phone for user")
    @GET
    @Path("mobile-phones/{mobilePhoneId}")
    public Response getMultiFactorMobilePhoneFromUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("mobilePhoneId") String mobilePhoneId) {
        return multiFactorCloud20Service.getPhoneFromUser(uriInfo, authToken, userId, mobilePhoneId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Delete phone from user")
    @DELETE
    @Path("mobile-phones/{mobilePhoneId}")
    public Response deleteMultiFactorMobilePhoneFromUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("mobilePhoneId") String mobilePhoneId) {
        return multiFactorCloud20Service.deletePhoneFromUser(uriInfo, authToken, userId, mobilePhoneId).build();
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
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Send phone verification code")
    @POST
    @Path("mobile-phones/{deviceId}/verificationcode")
    public Response sendVerificationCode(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId) {
        return multiFactorCloud20Service.sendVerificationCode(uriInfo, authToken, userId, deviceId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Verify phone")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Update user settings")
    @PUT
    public Response updateMultiFactorSettings(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            MultiFactor multiFactor) {
        return multiFactorCloud20Service.updateMultiFactorSettings(uriInfo, authToken, userId, multiFactor).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Remove from user")
    @DELETE
    public Response deleteMultiFactor(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return multiFactorCloud20Service.deleteMultiFactor(uriInfo, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Create bypass codes for user")
    @POST
    @Path("bypass-codes")
    public Response generateBypassCodes(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            BypassCodes bypassCodes) {
        return multiFactorCloud20Service.generateBypassCodes(uriInfo, authToken, userId, bypassCodes).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Add OTP device for user")
    @POST
    @Path("otp-devices")
    public Response addOTPDeviceToUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            OTPDevice otpDevice) {
        return multiFactorCloud20Service.addOTPDeviceToUser(uriInfo, authToken, userId, otpDevice).build();
    }

    /**
     * The multifactor service to list otp devices for a given user.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA List OTP devices for user")
    @GET
    @Path("otp-devices")
    public Response listOTPDevicesForUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return multiFactorCloud20Service.listOTPDevicesForUser(uriInfo, authToken, userId).build();
    }

    /**
     * The multifactor service to list devices for a given user.
     *
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA List OTP devices for user (alias)")
    @GET
    @Path("devices/otp-devices")
    public Response listOTPDevicesForUserAlternative(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return listOTPDevicesForUser(uriInfo, authToken, userId);
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Get OTP device for user")
    @GET
    @Path("otp-devices/{deviceId}")
    public Response getOTPDeviceFromUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId) {
        return multiFactorCloud20Service.getOTPDeviceFromUser(uriInfo, authToken, userId, deviceId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Delete OTP device from user")
    @DELETE
    @Path("otp-devices/{deviceId}")
    public Response deleteOTPDeviceFromUser(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId) {
        return multiFactorCloud20Service.deleteOTPDeviceFromUser(uriInfo, authToken, userId, deviceId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name = "v2.0 MFA Verity OTP for user")
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
