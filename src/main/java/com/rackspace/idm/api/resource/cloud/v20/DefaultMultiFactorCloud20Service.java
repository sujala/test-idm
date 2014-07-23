package com.rackspace.idm.api.resource.cloud.v20;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.BypassCodes;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasscodeCredentials;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode;
import com.rackspace.identity.multifactor.domain.BasicPin;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse;
import com.rackspace.identity.multifactor.providers.duo.exception.DuoLockedOutException;
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.MobilePhoneConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.email.EmailClient;
import com.rackspace.idm.api.resource.cloud.v20.multifactor.SessionId;
import com.rackspace.idm.api.resource.cloud.v20.multifactor.SessionIdReaderWriter;
import com.rackspace.idm.api.resource.cloud.v20.multifactor.V1SessionId;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.multifactor.service.BasicMultiFactorService;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import com.rackspace.idm.util.DateHelper;
import com.rackspace.idm.validation.PrecedenceValidator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.UnauthorizedFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.datatype.Duration;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
@Component
public class DefaultMultiFactorCloud20Service implements MultiFactorCloud20Service {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMultiFactorCloud20Service.class);

    static final String BAD_REQUEST_MSG_MISSING_PHONE_NUMBER = "Must provide a telephone number";
    static final String BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT = "Can only configure multifactor on own account";
    static final String BAD_REQUEST_MSG_ALREADY_LINKED = "Already associated with a mobile phone";
    static final String BAD_REQUEST_MSG_INVALID_PHONE_NUMBER = "The provided phone number is invalid.";
    static final String BAD_REQUEST_MSG_INVALID_DEVICE = "The specified device is invalid";
    static final String BAD_REQUEST_MSG_ALREADY_VERIFIED = "The specified device has already been verified";
    static final String BAD_REQUEST_MSG_INVALID_PIN_OR_EXPIRED = "The provided pin is either invalid or expired.";
    static final String BAD_REQUEST_MSG_MISSING_VERIFICATION_CODE = "Must provide a verification code";
    static final String BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS = "Must provide a multifactor settings";
    static final String BAD_REQUEST_MSG_MULTIFACTOR_DISABLED = "User does not have multifactor enabled";

    static final String MULTIFACTOR_BETA_ROLE_NAME = "cloudAuth.multiFactorBetaRoleName";

    private static final Integer SESSION_ID_LIFETIME_DEFAULT = 5;
    private static final String SESSION_ID_LIFETIME_PROP_NAME = "multifactor.sessionid.lifetime";
    private static final String SESSION_ID_PRIMARY_VERSION_PROP_NAME = "multifactor.primary.sessionid.version";
    public static final String MFA_ADDITIONAL_AUTH_CREDENTIALS_REQUIRED_MSG = "Additional authentication credentials required";
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HEADER_WWW_AUTHENTICATE_VALUE = "OS-MF sessionId='%s', factor='PASSCODE'";
    public static final String HEADER_WWW_AUTHENTICATE_VALUE_SESSIONID_REGEX = String.format("^" + HEADER_WWW_AUTHENTICATE_VALUE + "$", "(.*)");
    public static final String INVALID_CREDENTIALS_GENERIC_ERROR_MSG = "Can not authenticate with credentials provided";
    public static final String INVALID_CREDENTIALS_SESSIONID_EXPIRED_ERROR_MSG = "Can not authenticate with credentials provided. Session has expired.";
    public static final String INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG = "Can not authenticate with credentials provided. The account has been locked due to excessive invalid attempts. Please contact an administrator";
    public static final String NON_STANDARD_MFA_DENY_ERROR_MSG_FORMAT = "Multifactor provider denied a mfa request for user '%s' due to a non-standard reason. Reason: '%s'; Message: '%s'";

    public static final String BYPASS_MAXIMUM_DURATION = "multifactor.bypass.maximum.duration.seconds";
    public static final String BYPASS_DEFAULT_DURATION = "multifactor.bypass.default.duration.seconds";

    /*
    Used for convenience only. TODO:// Refactor cloud20 service to extract common code.
     */
    @Autowired
    private DefaultCloud20Service cloud20Service;

    @Autowired
    private UserService userService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private Configuration config;

    @Autowired
    private MultiFactorService multiFactorService;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private MobilePhoneConverterCloudV20 mobilePhoneConverterCloudV20;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private SessionIdReaderWriter sessionIdReaderWriter;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private PrecedenceValidator precedenceValidator;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private DateHelper dateHelper;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Override
    public Response.ResponseBuilder addPhoneToUser(UriInfo uriInfo, String authToken, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone) {
        try {
            ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
            User requester = (User) userService.getUserByScopeAccess(token);
            validateAddPhoneToUserRequest(requester, userId, requestMobilePhone);
            Phonenumber.PhoneNumber phoneNumber = parseRequestPhoneNumber(requestMobilePhone.getNumber());

            MobilePhone mobilePhone = multiFactorService.addPhoneToUser(requester.getId(), phoneNumber);

            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String id = mobilePhone.getId();
            URI build = requestUriBuilder.path(id).build();
            Response.ResponseBuilder response = Response.created(build);
            response.entity(mobilePhoneConverterCloudV20.toMobilePhoneWeb(mobilePhone));
            return response;
        } catch (Exception ex) {
            LOG.error(String.format("Error adding a phone to user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder sendVerificationCode(UriInfo uriInfo, String authToken, String userId, String deviceId) {
        try {
            ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
            User requester = (User) userService.getUserByScopeAccess(token);
            validateSendVerificationCodeRequest(requester, userId, deviceId);
            multiFactorService.sendVerificationPin(userId, deviceId);
            return Response.status(Response.Status.ACCEPTED);
        } catch (MultiFactorDeviceNotAssociatedWithUserException ex) {
            return exceptionHandler.notFoundExceptionResponse(BAD_REQUEST_MSG_INVALID_DEVICE);
        } catch (MultiFactorDeviceAlreadyVerifiedException ex) {
            return exceptionHandler.badRequestExceptionResponse(BAD_REQUEST_MSG_ALREADY_VERIFIED);
        } catch (NotFoundException ex) {
            return exceptionHandler.notFoundExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            //sendpin, saveorupdate, and Invalidphonenumber exceptions would all run through this block. Since these are internal state issues
            //that the user can't do anything about, return as server side errors. Make sure to log the error
            LOG.error(String.format("Error sending verification code to device '%s'", deviceId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder verifyVerificationCode(UriInfo uriInfo, String authToken, String userId, String deviceId, VerificationCode verificationCode) {
        try {
            ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
            User requester = (User) userService.getUserByScopeAccess(token);
            validateVerifyVerificationCodeRequest(requester, userId, deviceId, verificationCode);
            multiFactorService.verifyPhoneForUser(userId, deviceId, new BasicPin(verificationCode.getCode()));
            return Response.status(Response.Status.NO_CONTENT);
        } catch (MultiFactorDeviceNotAssociatedWithUserException ex) {
            return exceptionHandler.notFoundExceptionResponse(BAD_REQUEST_MSG_INVALID_DEVICE);
        } catch (MultiFactorDevicePinValidationException ex) {
            return exceptionHandler.badRequestExceptionResponse(BAD_REQUEST_MSG_INVALID_PIN_OR_EXPIRED);
        } catch (NotFoundException ex) {
            return exceptionHandler.notFoundExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
           LOG.error(String.format("Error verifying code for device '%s'", deviceId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder updateMultiFactorSettings(UriInfo uriInfo, String authToken, String userId, MultiFactor multiFactor) {
        try {
            ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
            User requester = (User) userService.getUserByScopeAccess(token);
            validateUpdateMultiFactorSettingsRequest(requester, token, userId, multiFactor);
            multiFactorService.updateMultiFactorSettings(userId, multiFactor);
            return Response.status(Response.Status.NO_CONTENT);
        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error updating multifactor settings on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }

    }

    @Override
    public Response.ResponseBuilder deleteMultiFactor(UriInfo uriInfo, String authToken, String userId) {
        try {
            ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
            User requester = (User) userService.getUserByScopeAccess(token);
            validateRemoveMultiFactorRequest(requester, userId);
            multiFactorService.removeMultiFactorForUser(userId);
            return Response.status(Response.Status.NO_CONTENT);
        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error updating multifactor settings on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder performMultiFactorChallenge(User user, List<String> alreadyAuthenticatedBy) {
        /*
        only supported option is SMS passcode so send it and return sessionid header
         */
        DateTime created = new DateTime();
        DateTime expiration = created.plusMinutes(getSessionIdLifetime());

        V1SessionId sessionId = new V1SessionId();
        sessionId.setVersion(getPrimarySessionIdVersion());
        sessionId.setUserId(user.getId());
        sessionId.setCreatedDate(created);
        sessionId.setExpirationDate(expiration);
        sessionId.setAuthenticatedBy(alreadyAuthenticatedBy);

        //generate the new sessionId first
        String encodedSessionId = sessionIdReaderWriter.writeEncoded(sessionId);

        //now send the passcode
        try {
            multiFactorService.sendSmsPasscode(user.getId());
        } catch (DuoLockedOutException lockedOutException) {
            emailClient.asyncSendMultiFactorLockedOutMessage(user);
            user.setMultiFactorState(BasicMultiFactorService.MULTI_FACTOR_STATE_LOCKED);
            userService.updateUserForMultiFactor(user);
            throw new ForbiddenException(INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG);
        }

        /*
        Create unauthorized fault and response
         */
        UnauthorizedFault fault = objFactories.getOpenStackIdentityV2Factory().createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(MFA_ADDITIONAL_AUTH_CREDENTIALS_REQUIRED_MSG);
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).entity(
                objFactories.getOpenStackIdentityV2Factory().createUnauthorized(fault).getValue())
                .header(HEADER_WWW_AUTHENTICATE, createWwwAuthenticateHeaderValue(encodedSessionId));
    }

    private String createWwwAuthenticateHeaderValue(String sessionId) {
        return String.format(HEADER_WWW_AUTHENTICATE_VALUE, sessionId);
    }

    @Override
    public AuthResponseTuple authenticateSecondFactor(String encodedSessionId, CredentialType credential) {
        if (!(credential instanceof PasscodeCredentials)) {
            throw new BadRequestException("Not a valid credential. Only passcode credential supported for multifactor");
        }
        PasscodeCredentials passcodeCredentials = (PasscodeCredentials) credential;

        String passcode = passcodeCredentials.getPasscode();
        SessionId sessionId;

        try {
            sessionId = sessionIdReaderWriter.readEncoded(encodedSessionId);
        }
        catch (Exception ex) {
            LOG.info("Invalid sessionId provided", ex);
            throw new NotAuthenticatedException(INVALID_CREDENTIALS_GENERIC_ERROR_MSG);
        }

        if (sessionId.getExpirationDate() == null || sessionId.getExpirationDate().isBefore(new DateTime())) {
            throw new NotAuthenticatedException(INVALID_CREDENTIALS_SESSIONID_EXPIRED_ERROR_MSG);
        }

        //verify user is valid
        User user = userService.getUserById(sessionId.getUserId());
        userService.validateUserIsEnabled(user);

        if(!isMultiFactorEnabledForUser(user)) {
            throw new BadRequestException(INVALID_CREDENTIALS_GENERIC_ERROR_MSG);
        }

        MfaAuthenticationResponse response = multiFactorService.verifyPasscode(sessionId.getUserId(), passcode);
        if (response.getDecision() == MfaAuthenticationDecision.ALLOW) {
            return createSuccessfulSecondFactorResponse(user, response, sessionId);
        } else if (response.getDecisionReason() == MfaAuthenticationDecisionReason.LOCKEDOUT) {
            emailClient.asyncSendMultiFactorLockedOutMessage(user);
            user.setMultiFactorState(BasicMultiFactorService.MULTI_FACTOR_STATE_LOCKED);
            userService.updateUserForMultiFactor(user);
            throw createFailedSecondFactorException(response, sessionId);
        } else {
            //2-factor request denied. Determine appropriate exception/message for user
            throw createFailedSecondFactorException(response, sessionId);
        }
    }

    private AuthResponseTuple createSuccessfulSecondFactorResponse(User user, MfaAuthenticationResponse mfaResponse, SessionId sessionId) {
        //return a token with the necessary authenticated by to reflect 2 factor authentication
        Set<String> authBySet = new HashSet<String>();

        if (!CollectionUtils.isEmpty(sessionId.getAuthenticatedBy())) {
            authBySet.addAll(sessionId.getAuthenticatedBy());
        }
        authBySet.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE);

        UserScopeAccess scopeAccess = scopeAccessService.updateExpiredUserScopeAccess(user, getCloudAuthClientId(), new ArrayList<String>(authBySet));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(scopeAccess);

        return authResponseTuple;
    }

    /**
     * Throws appropriately formatted exception that
     * @param mfaResponse
     * @param sessionId
     * @return
     */
    private RuntimeException createFailedSecondFactorException(MfaAuthenticationResponse mfaResponse, SessionId sessionId) {
        //2-factor request denied. Determine appropriate exception/message for user
        RuntimeException exceptionToThrow;
        switch (mfaResponse.getDecisionReason()) {
            case DENY:
                exceptionToThrow = new NotAuthenticatedException(INVALID_CREDENTIALS_GENERIC_ERROR_MSG);
                break;
            case LOCKEDOUT:
                exceptionToThrow = new ForbiddenException(INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG);
                break;
            default:
                String msg = String.format(NON_STANDARD_MFA_DENY_ERROR_MSG_FORMAT, sessionId.getUserId(), mfaResponse.getDecisionReason(), mfaResponse.getMessage());
                LOG.error(msg);
                exceptionToThrow = new MultiFactorDeniedException();
        }
        return exceptionToThrow;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    public Response.ResponseBuilder listDevicesForUser(UriInfo uriInfo, String authToken, String userId) {

        try {
            ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
            User requester = (User) userService.getUserByScopeAccess(token);
            validateListDevicesForUser(requester, userId);

            //NOTE: Since this call is restricted to a user listing his own devices (ie requesters userId must
            //      equal the passed in userId), its perfectly fine to save an LDAP call and send in the requestor
            //      as the parameter in the call below in order to get the List of MobilePhones. If that restriction
            //      is ever removed we'll need to add a call to get the userById and pass that user into the call.
            List<MobilePhone> phoneList = multiFactorService.getMobilePhonesForUser(requester);
            return Response.ok().entity(mobilePhoneConverterCloudV20.toMobilePhonesWebIncludingVerifiedFlag(phoneList, requester));

        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error listing devices on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private void validateListDevicesForUser(User requester, String userId) {
        if (requester == null || !(requester.getId().equals(userId))) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
            throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
        }
    }

    private void validateRemoveMultiFactorRequest(User requester, String userId) {
        if (requester == null || !(requester.getId().equals(userId))) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
            throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
        }
    }

    private void validateUpdateMultiFactorSettingsRequest(User requester, ScopeAccess token, String userId, MultiFactor multiFactor) {
        if (multiFactor == null) {
            LOG.debug(BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS);
        }

        //if enabling mfa, you can only do it on yourself
        if (multiFactor.isEnabled() != null) {
            if (requester == null || !(requester.getId().equals(userId))) {
                LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
                throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
            }
        }

        //if unlocking mfa on user, can only do it on OTHER users
        if (multiFactor.isUnlock() != null && multiFactor.isUnlock()) {
            //can not unlock own account
            if (requester == null || requester.getId().equals(userId)) {
                LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
                throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
            }

            //check that the requester is either an identity admin or a service admin
            authorizationService.verifyIdentityAdminLevelAccess(token);

            //now verify the requester can unlock the user
            User userBeingUnlocked = userService.checkAndGetUserById(userId);
            precedenceValidator.verifyCallerPrecedenceOverUser(requester, userBeingUnlocked);
        }
    }

    /**
     * Validate the sendVerificationCodeRequest
     *
     * @param requester
     * @param userId
     * @param deviceId
     * @throws Exception if invalid request found
     */
    private void validateSendVerificationCodeRequest(User requester, String userId, String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_DEVICE); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_DEVICE);
        }
        else if (requester == null || !(requester.getId().equals(userId))) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
            throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
        }
    }

    private void validateAddPhoneToUserRequest(User requester, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone) {
        if (requestMobilePhone == null || StringUtils.isBlank(requestMobilePhone.getNumber())) {
            LOG.debug(BAD_REQUEST_MSG_MISSING_PHONE_NUMBER); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MISSING_PHONE_NUMBER);
        }
        else if (requester == null || !(requester.getId().equals(userId))) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
            throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
        }
        else if (requester.getMultiFactorMobilePhoneRsId() != null) {
            LOG.debug(BAD_REQUEST_MSG_ALREADY_LINKED); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_ALREADY_LINKED);
        }
    }

    private void validateVerifyVerificationCodeRequest(User requester, String userId, String deviceId, VerificationCode verificationCode) {
        if (StringUtils.isBlank(deviceId)) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_DEVICE); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_DEVICE);
        }
        else if (verificationCode == null || StringUtils.isBlank(verificationCode.getCode())) {
            LOG.debug(BAD_REQUEST_MSG_MISSING_VERIFICATION_CODE); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MISSING_VERIFICATION_CODE);
        }
        else if (requester == null || !(requester.getId().equals(userId))) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
            throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
        }
        else if (requester.getMultiFactorMobilePhoneRsId() == null || !requester.getMultiFactorMobilePhoneRsId().equals(deviceId)) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_DEVICE); //logged as debug because this is a bad request, not an error in app
            throw new NotFoundException(BAD_REQUEST_MSG_INVALID_DEVICE);
        }
    }

    @Override
    public boolean isMultiFactorGloballyEnabled() {
        return isMultiFactorEnabled() && !isMultiFactorBetaEnabled();
    }

    @Override
    public boolean isMultiFactorEnabled() {
        return config.getBoolean("multifactor.services.enabled", false);
    }

    private boolean isMultiFactorBetaEnabled() {
        return config.getBoolean("multifactor.beta.enabled", false);
    }

    @Override
    public boolean isMultiFactorEnabledForUser(BaseUser user) {
        if(!isMultiFactorEnabled()) {
            return false;
        } else if(isMultiFactorBetaEnabled()) {
            if(userHasMultiFactorBetaRole(user)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Parses the provided string based phone number into standard phone number representation.
     *
     * @param rawPhoneNumber
     * @throws BadRequestException if the number could not be parsed as a phone number
     * @return
     */
    private Phonenumber.PhoneNumber parseRequestPhoneNumber(String rawPhoneNumber) {
        Assert.notNull(rawPhoneNumber);
        try {
            Phonenumber.PhoneNumber phoneNumber = IdmPhoneNumberUtil.getInstance().parsePhoneNumber(rawPhoneNumber);
            return phoneNumber;
        } catch (com.rackspace.identity.multifactor.exceptions.InvalidPhoneNumberException ex) {
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_PHONE_NUMBER, ex);
        }
    }

    private String getPrimarySessionIdVersion() {
        String version = config.getString(SESSION_ID_PRIMARY_VERSION_PROP_NAME);
        if (version == null) {
            throw new IllegalStateException(String.format("Configuration is missing property '%s'", SESSION_ID_PRIMARY_VERSION_PROP_NAME));
        }
        return version;
    }

    public int getSessionIdLifetime() {
        return config.getInt(SESSION_ID_LIFETIME_PROP_NAME, SESSION_ID_LIFETIME_DEFAULT);
    }

    private boolean userHasMultiFactorBetaRole(BaseUser user) {
        List<TenantRole> userGlobalRoles = tenantService.getGlobalRolesForUser(user);

        if(userGlobalRoles != null && !userGlobalRoles.isEmpty()) {
            for(TenantRole role : userGlobalRoles) {
                if(role.getName().equals(config.getString(MULTIFACTOR_BETA_ROLE_NAME))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Response.ResponseBuilder generateBypassCodes(UriInfo uriInfo, String authToken, String userId, BypassCodes bypassCodes) {
        final User user = userService.checkAndGetUserById(userId);
        final boolean isSelfService = validateGenerateBypassCodesRequest(authToken, user);

        final Integer validSecs = getValidSecs(bypassCodes.getValidityDuration(), isSelfService);
        final List<String> codes = new ArrayList<String>();
        if (isSelfService) {
            codes.addAll(multiFactorService.getSelfServiceBypassCodes(user, validSecs, bypassCodes.getNumberOfCodes()));
        } else {
            codes.add(multiFactorService.getBypassCode(user, validSecs));
        }

        final BypassCodes entity = new BypassCodes();
        entity.getCodes().addAll(codes);
        entity.setValidityDuration(dateHelper.getDurationFromSeconds(validSecs));

        final Response.ResponseBuilder response = Response.ok();
        response.entity(entity);
        return response;
    }

    private Integer getValidSecs(Duration duration, boolean isSelfService) {
        final BigInteger max = config.getBigInteger(BYPASS_MAXIMUM_DURATION, BigInteger.valueOf(10800));
        if (duration == null) {
            return isSelfService ? null : config.getBigInteger(BYPASS_DEFAULT_DURATION, BigInteger.valueOf(1800)).max(BigInteger.ONE).intValue();
        } else {
            return max.min(dateHelper.getSecondsFromDuration(duration)).max(BigInteger.ONE).intValue();
        }
    }

    private boolean validateGenerateBypassCodesRequest(String authToken, User user) {
        final ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
        final User requester = (User) userService.getUserByScopeAccess(token);

        // Verify if the user is valid
        userService.validateUserIsEnabled(user);
        if (!user.isMultiFactorEnabled()) {
            throw new BadRequestException(BAD_REQUEST_MSG_MULTIFACTOR_DISABLED);
        }

        final String userId = user.getId();
        if (userId != null && userId.equals(requester.getId())) {
            // Self-service request
            return !requestContextHolder.isImpersonated();
        } else {
            // Verify requester/user precedence level
            authorizationService.verifyUserAdminLevelAccess(token);
            precedenceValidator.verifyCallerPrecedenceOverUser(requester, user);
            if (authorizationService.authorizeCloudUserAdmin(token)) {
                authorizationService.verifyDomain(requester, user);
            }
            return false;
        }
    }

}
