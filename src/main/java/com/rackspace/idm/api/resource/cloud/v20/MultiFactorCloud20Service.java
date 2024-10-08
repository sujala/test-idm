package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.BypassCodes;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.User;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.CredentialType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * <p>
 * Acts as a bridge between the REST services exposed via the {@link com.rackspace.idm.api.resource.cloud.v20.CloudMultifactorResource} and
 * {@link com.rackspace.idm.api.resource.cloud.v20.CloudMultifactorDomainResource} to the underlying multifactor business service
 * {@link com.rackspace.idm.multifactor.service.MultiFactorService}.
 * </p>
 * <p>
 * Authorizes and validates the REST requests before calling the appropriate underlying MultiFactorService
 * to perform that actual business logic. Will then return the appropriate response to the REST request.
 * </p>
 */
public interface MultiFactorCloud20Service {
    static final String X_SESSION_ID_HEADER_NAME = "X-SessionId";

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
     * Get the specified phone registered with the user
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param mobilePhoneId
     * @return
     */
    Response.ResponseBuilder getPhoneFromUser(UriInfo uriInfo, String authToken, String userId, String mobilePhoneId);

    /**
     * Delete the specified phone from the user. Only allowed if phone is not actively being used for MFA authentication
     * on that user.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param mobilePhoneId
     * @return
     */
    Response.ResponseBuilder deletePhoneFromUser(UriInfo uriInfo, String authToken, String userId, String mobilePhoneId);

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
     * Modifies MFA settings on the specified user account. The method supports 3 types of requests:
     *
     * <ol>
     *     <li>Unlocking a user <b>other than oneself</b> (must adhere to standard user precedence rules such as an identity:admin not being able to unlock another identity:admin)</li>
     *     <li>Enabling MFA on user. Can do on self. If not doing on self, must adhere to standard user precedence rules</li>
     *     <li>Setting MFA enforcement level on a user. Modified precedence rules in effect for this use:
     *          <ul>
     *              <li>Default users can NOT modify this setting at all.</li>
     *              <li>identity:admin/identity:service-admin can modify this setting on standard lower precedence users, but not self</li>
     *              <li>user-admins can modify this setting on self AND all user-admin/user-manage/default subusers within their domain</li>
     *              <li>user-managers can modify this setting on self AND all user-admin/user-manage/default subusers within their domain</li>
     *          </ul>
     *     </li>
     * </ol>
     *
     * A single request CAN include multiple changes as long as the authorization restrictions are met for both types of changes. For example,
     * <ul>
     *     <li>Allowed: As identity:admin unlock a user-admin AND set the user-admins enforcement level</li>
     *     <li>NOT Allowed: As user-admin unlock my account AND set my enforcement level (denied because can not unlock own account)</li>
     * </ul>
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param multiFactor
     * @return
     */
    Response.ResponseBuilder updateMultiFactorSettings(UriInfo uriInfo, String authToken, String userId, MultiFactor multiFactor);

    /**
     * Removes all traces of multifactor configuration for the user's account. This includes unlinking all associated devices,
     * removing any profiles from 3rd party providers, and resetting the user's account to the state it would be had s/he
     * never configured multi-factor in the first place. If multi-factor has never been set up for the account, this is, in effect,
     * a no-op
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder deleteMultiFactor(UriInfo uriInfo, String authToken, String userId);

    /**
     * Perform multi-factor challenge against the specified user. It is assumed the user successfully authenticated via
     * the mechanisms provided in the alreadyAuthenticatedBy list.
     *
     * Ultimately the user could be approved immediately with the provided information from the first request (Mobile Passcode)
     * or through the Mobile Push mechanism, so this must return the appropriate response that should be returned to the
     * user.
     *
     * If the request requires a second authentication request a WWW-Authenticate header must be returned in the response
     * with an encrypted sessionId string to be passed into the {@link #authenticateSecondFactor(String, AuthenticationRequest)}.
     *
     * @throws com.rackspace.idm.exception.MultiFactorNotEnabledException - if multifactor is not enabled for this user account
     */
    Response.ResponseBuilder performMultiFactorChallenge(User user, List<String> alreadyAuthenticatedBy);

    /**
     * authenticates a 2-factor credential
     *
     * @param sessionId
     * @param authenticationRequest
     * @throws IllegalArgumentException If the credential provided is not a supported 2-factor credential or sessionId is null
     * @throws com.rackspace.idm.exception.NotAuthenticatedException If the supplied sessionId and credential are not valid
     * @return AuthResponseTuple - Only returned in the authentication is successful. Exceptions should be thrown otherwise.
     *
     * @throws com.rackspace.idm.exception.MultiFactorNotEnabledException - if multifactor is not enabled for this user account
     */
    AuthResponseTuple authenticateSecondFactor(String sessionId, AuthenticationRequest authenticationRequest);

     /**
     * The multifactor service to list devices for a given user.
     *
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder listMobilePhoneDevicesForUser(UriInfo uriInfo, String authToken, String userId);

    /**
     * The multifactor service to generate bypass codes for a given user.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder generateBypassCodes(UriInfo uriInfo, String authToken, String userId, BypassCodes bypassCodes);

    /**
     * Updates multifactor settings on the specified domain.
     *
     * @param uriInfo
     * @param authToken
     * @param domainId
     * @param multiFactorDomain
     * @return
     */
    Response.ResponseBuilder updateMultiFactorDomainSettings(UriInfo uriInfo, String authToken, String domainId, MultiFactorDomain multiFactorDomain);

    /**
     * Associates the specified OTP device to the user specified by userId.
     *
     * @param uriInfo
     * @param authToken
     * @param otpDevice
     * @throws com.rackspace.idm.exception.ForbiddenException if the caller attempting to link to an account other than his/her own.
     * @return
     */
    Response.ResponseBuilder addOTPDeviceToUser(UriInfo uriInfo, String authToken, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice otpDevice);

    /**
     * List the multifactor otp devices associated with a given user.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder listOTPDevicesForUser(UriInfo uriInfo, String authToken, String userId);


    /**
     * Retrieves the OTP device associated with the user specified by userId.
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder getOTPDeviceFromUser(UriInfo uriInfo, String authToken, String userId, String deviceId);

    /**
     * Deletes the OTP device associated with the user specified by userId. The device can only be deleted in the following
     * circumstances:
     *
     * <ul>
     *     <li>The user has another mobile passcode (OTP) device associated to their profile</li>
     *     <li>The user has multi-factor type set to [SMS text passcode] for their profile</li>
     *     <li>The user has multi-factor authentication [Disabled] for their profile</li>
     *     <li>The device has not been verified</li>
     * </ul>
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @param deviceId
     * @return
     */
    Response.ResponseBuilder deleteOTPDeviceFromUser(UriInfo uriInfo, String authToken, String userId, String deviceId);

    /**
     * Verify the code against that sent to the device via the {@link #sendVerificationCode(javax.ws.rs.core.UriInfo, String, String, String)} request
     *
     * @param authToken
     * @param userId
     * @param deviceId
     * @param verificationCode
     * @return
     */
    Response.ResponseBuilder verifyOTPCode(String authToken, String userId, String deviceId, VerificationCode verificationCode);

    /**
     * Retrieve all multifactor devices associated with the user
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder listMultiFactorDevicesForUser(UriInfo uriInfo, String authToken, String userId);
}
