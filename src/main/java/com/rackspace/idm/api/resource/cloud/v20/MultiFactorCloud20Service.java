package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode;
import org.openstack.docs.identity.api.v2.CredentialType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 */
public interface MultiFactorCloud20Service {
    static final String X_SESSION_ID_HEADER_NAME = "X-SessionId";

    /**
     * Whether or not multi-factor services are enabled or not.
     */
    boolean isMultiFactorEnabled();

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
     * with an encrypted sessionId string to be passed into the {@link #authenticateSecondFactor(String, org.openstack.docs.identity.api.v2.CredentialType)}.
     *
     * @throws com.rackspace.idm.exception.MultiFactorNotEnabledException - if multifactor is not enabled for this user account
     */
    Response.ResponseBuilder performMultiFactorChallenge(String userId, List<String> alreadyAuthenticatedBy);

    /**
     * authenticates a 2-factor credential
     *
     * @param sessionId
     * @param credential
     * @throws IllegalArgumentException If the credential provided is not a supported 2-factor credential or sessionId is null
     * @throws com.rackspace.idm.exception.NotAuthenticatedException If the supplied sessionId and credential are not valid
     * @return AuthResponseTuple - Only returned in the authentication is successful. Exceptions should be thrown otherwise.
     *
     * @throws com.rackspace.idm.exception.MultiFactorNotEnabledException - if multifactor is not enabled for this user account
     */
    AuthResponseTuple authenticateSecondFactor(String sessionId, CredentialType credential);

     /**
     * The multifactor service to list devices for a given user.
     *
     *
     * @param uriInfo
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder listDevicesForUser(UriInfo uriInfo, String authToken, String userId);
}
