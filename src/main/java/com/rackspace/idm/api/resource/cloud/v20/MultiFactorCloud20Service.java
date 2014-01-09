package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 */
public interface MultiFactorCloud20Service {

    /**
     * Associates the specified phone to the user specified by userId.
     *
     * Initially, the caller, represented by the provided authToken, can only associate a phone with his/her own account. Attempting to call this service
     * on any other user will result in a 401 error.
     *
     * <p>
     * If the specified mobile phone, uniquely identified by canonicalizing the telephoneNumber to the E.123 standard, does not exist within IDM, a new mobilePhone will
     * be created, linked to the user, and returned in the response body. If the specified phone already exists, the existing phone will be linked
     * to the user and returned in the response body.
     * </p>
     *
     * @param uriInfo
     * @param authToken
     * @param mobilePhone
     * @throws com.rackspace.idm.exception.ForbiddenException if the caller attempting to link to an account other than his/her own.
     * @return
     */
    Response.ResponseBuilder addPhoneToUser(UriInfo uriInfo, String authToken, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone mobilePhone);

    /**
     * Sends a verification code (PIN) to the specified device.
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param deviceId
     * @return
     */
    Response.ResponseBuilder sendVerificationCode(UriInfo uriInfo, String authToken, String userId, String deviceId);

    /**
     * Verify the code against that sent to the device via the {@link #sendVerificationCode(javax.ws.rs.core.UriInfo, String, String, String)} request
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param deviceId
     * @param verificationCode
     * @return
     */
    Response.ResponseBuilder verifyVerificationCode(UriInfo uriInfo, String authToken, String userId, String deviceId, VerificationCode verificationCode);

    /**
     * Updates multifactor settings on the specified user account.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param multiFactor
     * @return
     */
    Response.ResponseBuilder updateMultiFactorSettings(UriInfo uriInfo, String authToken, String userId, MultiFactor multiFactor);
}
