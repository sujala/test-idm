package com.rackspace.idm.api.resource.cloud.v20;

import com.google.i18n.phonenumbers.Phonenumber;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice;
import com.rackspace.identity.multifactor.domain.BasicPin;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason;
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse;
import com.rackspace.identity.multifactor.providers.duo.exception.DuoLockedOutException;
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.MobilePhoneConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.MultiFactorDeviceConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.OTPDeviceConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.NewRelicTransactionNames;
import com.rackspace.idm.api.resource.cloud.email.EmailClient;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.MultiFactorDevice;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import com.rackspace.idm.util.DateHelper;
import com.rackspace.idm.validation.PrecedenceValidator;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
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

@Component
public class DefaultMultiFactorCloud20Service implements MultiFactorCloud20Service {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMultiFactorCloud20Service.class);

    static final String BAD_REQUEST_MSG_MISSING_PHONE_NUMBER = "Must provide a telephone number";
    static final String BAD_REQUEST_MSG_INVALID_TARGET_OWN_ACCOUNT = "Can not make this multifactor change on own account";
    static final String BAD_REQUEST_MSG_MFA_ENABLED = "Cannot replace device with multifactor enabled";
    static final String BAD_REQUEST_MSG_INVALID_PHONE_NUMBER = "The provided phone number is invalid.";
    static final String BAD_REQUEST_MSG_INVALID_DEVICE = "The specified device is invalid";
    static final String BAD_REQUEST_MSG_ALREADY_VERIFIED = "The specified device has already been verified";
    static final String BAD_REQUEST_MSG_INVALID_PIN_OR_EXPIRED = "The provided pin is either invalid or expired.";
    static final String BAD_REQUEST_MSG_INVALID_OTP_DEVICE_NAME = "Must provide a name for an OTP device";
    static final String BAD_REQUEST_MSG_MISSING_VERIFICATION_CODE = "Must provide a verification code";
    static final String BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS = "Must provide a multifactor settings";
    static final String FORBIDDEN_MSG_MULTIFACTOR_DISABLED = "You must have Multi-Factor Authentication set as optional or enabled for your profile to enforce Multi-Factor Authentication for the domain";
    static final String BAD_REQUEST_MSG_MISSING_DOMAIN_MFA_SETTINGS = "Must provide domain multifactor enforcement level";
    static final String BAD_REQUEST_MSG_INVALID_USER = "Invalid user";

    public static final String NOT_AUTHORIZED_ERROR_MSG = "Not Authorized";

    private static final String SESSION_ID_PRIMARY_VERSION_PROP_NAME = "multifactor.primary.sessionid.version";
    public static final String MFA_ADDITIONAL_AUTH_CREDENTIALS_REQUIRED_MSG = "Additional authentication credentials required";
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HEADER_WWW_AUTHENTICATE_VALUE = "OS-MF sessionId='%s', factor='%s'";
    public static final String HEADER_WWW_AUTHENTICATE_VALUE_SESSIONID_REGEX = String.format("^" + HEADER_WWW_AUTHENTICATE_VALUE + "$", "(.*)", "(.*)");
    public static final String INVALID_CREDENTIALS_GENERIC_ERROR_MSG = "Can not authenticate with credentials provided";
    public static final String INVALID_CREDENTIALS_SESSIONID_EXPIRED_ERROR_MSG = "Can not authenticate with credentials provided. Session has expired.";
    public static final String INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG = "Can not authenticate with credentials provided. The account has been locked due to excessive invalid attempts. Please contact an administrator";
    public static final String NON_STANDARD_MFA_DENY_ERROR_MSG_FORMAT = "Multifactor provider denied a mfa request for user '%s' due to a non-standard reason. Reason: '%s'; Message: '%s'";

    public static final String BYPASS_MAXIMUM_DURATION = "multifactor.bypass.maximum.duration.seconds";
    public static final String BYPASS_DEFAULT_DURATION = "multifactor.bypass.default.duration.seconds";

    private static final String ERROR_VERIFYING_CODE_FOR_DEVICE = "Error verifying code for device '%s'";

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private MultiFactorService multiFactorService;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private MobilePhoneConverterCloudV20 mobilePhoneConverterCloudV20;

    @Autowired
    private OTPDeviceConverterCloudV20 otpDeviceConverterCloudV20;

    @Autowired
    private MultiFactorDeviceConverterCloudV20 multiFactorDeviceConverterCloudV20;

    @Autowired
    private JAXBObjectFactories objFactories;

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

    private boolean isSelfCall(BaseUser caller, BaseUser user) {
        return caller.getId().equals(user.getId());
    }

    private void verifyEffectiveCallerAccessToTargetUser(BaseUser user) {
        BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();
        if (!isSelfCall(requester, user)) {
            precedenceValidator.verifyCallerPrecedenceOverUser(requester, user);
            IdentityUserTypeEnum callerType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
            if (callerType == IdentityUserTypeEnum.USER_ADMIN || callerType == IdentityUserTypeEnum.USER_MANAGER) {
                authorizationService.verifyDomain(requester, user);
            }
        }
    }

    @Override
    public Response.ResponseBuilder addPhoneToUser(UriInfo uriInfo, String authToken, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            User user = requestContextHolder.checkAndGetTargetUser(userId);
            Validator20.validateItsNotUnverifiedUser(user);

            verifyEffectiveCallerAccessToTargetUser(user);
            validateAddPhoneToUserRequest(user, requestMobilePhone);
            Phonenumber.PhoneNumber phoneNumber = parseRequestPhoneNumber(requestMobilePhone.getNumber());

            MobilePhone mobilePhone = multiFactorService.addPhoneToUser(user.getId(), phoneNumber);

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
    public Response.ResponseBuilder getPhoneFromUser(UriInfo uriInfo, String authToken, String userId, String mobilePhoneId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify if the user is valid
            userService.validateUserIsEnabled(requester);

            //if the target user is not a provisioned user (e.g. - a fed user), throw bad request cause fed users can't have MFA
            EndUser endUser = requestContextHolder.getAndCheckTargetEndUser(userId);
            if (endUser instanceof FederatedUser) {
                throw new ForbiddenException("Cannot target federated user");
            }
            if (!(endUser instanceof User)) {
                throw new BadRequestException("Federated users do not store multi-factor information within Identity");
            }
            User user = (User) endUser;
            verifyEffectiveCallerAccessToTargetUser(user);

            MobilePhone phone = multiFactorService.checkAndGetMobilePhoneFromUser(user, mobilePhoneId);
            return Response.ok().entity(mobilePhoneConverterCloudV20.toMobilePhoneWebIncludingVerifiedFlag(phone, user));
        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error retrieving phone on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder deletePhoneFromUser(UriInfo uriInfo, String authToken, String userId, String mobilePhoneId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify if the user is valid
            userService.validateUserIsEnabled(requester);

            //if the target user is not a provisioned user (e.g. - a fed user), throw bad request cause fed users can't have MFA
            EndUser endUser = requestContextHolder.getAndCheckTargetEndUser(userId);
            if (endUser instanceof FederatedUser) {
                throw new ForbiddenException("Cannot target federated user");
            }
            if (!(endUser instanceof User)) {
                throw new BadRequestException("Federated users do not store multi-factor information within Identity");
            }
            User user = (User) endUser;
            verifyEffectiveCallerAccessToTargetUser(user);

            multiFactorService.deleteMobilePhoneFromUser(user, mobilePhoneId);
            return Response.status(Response.Status.NO_CONTENT);
        } catch (Exception ex) {
            LOG.error(String.format("Error deleting device '%s' on user '%s'", mobilePhoneId, userId), ex);
            if (ex instanceof ErrorCodeIdmException
                    && (ErrorCodes.ERROR_CODE_DELETE_MOBILE_PHONE_FORBIDDEN_STATE.equals(((ErrorCodeIdmException) ex).getErrorCode()))) {
                //if can't delete due to user's MFA state, it's a bad request.
                return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
            }
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder sendVerificationCode(UriInfo uriInfo, String authToken, String userId, String deviceId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            User user = requestContextHolder.checkAndGetTargetUser(userId);

            verifyEffectiveCallerAccessToTargetUser(user);

            validateSendVerificationCodeRequest(deviceId);
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
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            User user = requestContextHolder.checkAndGetTargetUser(userId);
            verifyEffectiveCallerAccessToTargetUser(user);
            validateVerifyVerificationCodeRequest(user, deviceId, verificationCode);
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
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken); //will throw NotAuthorizedException if not found or expired
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled(); //will throw NotFoundException if not found
            User user = requestContextHolder.checkAndGetTargetUser(userId); //will throw NotFoundException if not found

            validateUpdateMultiFactorSettingsRequest(user, multiFactor);
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
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            User user = requestContextHolder.checkAndGetTargetUser(userId);
            verifyEffectiveCallerAccessToTargetUser(user);

            multiFactorService.removeMultiFactorForUser(userId);
            return Response.status(Response.Status.NO_CONTENT);
        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error updating multifactor settings on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Trace
    @Override
    public Response.ResponseBuilder performMultiFactorChallenge(User user, List<String> alreadyAuthenticatedBy) {
        try {
            if (multiFactorService.isMultiFactorTypeOTP(user)) {
                NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthMfaFirstOtp.getTransactionName());
            } else if (multiFactorService.isMultiFactorTypePhone(user)) {
                NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthMfaFirstSms.getTransactionName());
            }
        } catch (Exception ex) {
            // Eat
        }

        if (multiFactorService.isUserLocalLocked(user)) {
            throw new NotAuthorizedException(INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG);
        }

        DateTime created = new DateTime();

        int sessionIdLifetimeMinutes = identityConfig.getReloadableConfig().getMfaSessionIdLifetime();
        ScopeAccess sa = scopeAccessService.addScopedScopeAccess(user,
                identityConfig.getCloudAuthClientId(),
                alreadyAuthenticatedBy,
                sessionIdLifetimeMinutes * 60,
                TokenScopeEnum.MFA_SESSION_ID.getScope());
        String encodedSessionId = sa.getAccessTokenString();

        //now send the passcode (if SMS used)
        String secondFactor;
        if (multiFactorService.isMultiFactorTypePhone(user)) {
            LOG.debug(String.format("Sending SMS challenge to user '%s'", user.getId()));
            secondFactor = AuthenticatedByMethodEnum.PASSCODE.getValue();
            try {
                //TODO: Implement CID-84 to unlock in Duo and retry call if Duo fails call due to Duo locking
                multiFactorService.sendSmsPasscode(user.getId());
            } catch (DuoLockedOutException lockedOutException) {
                emailClient.asyncSendMultiFactorLockedOutMessage(user);
                throw new ForbiddenException(INVALID_CREDENTIALS_LOCKOUT_ERROR_MSG);
            }
        } else {
            LOG.debug(String.format("User '%s' requires OTP MFA", user.getId()));
            secondFactor = AuthenticatedByMethodEnum.OTPPASSCODE.getValue();
        }

        /*
        Create unauthorized fault and response
         */
        UnauthorizedFault fault = objFactories.getOpenStackIdentityV2Factory().createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(MFA_ADDITIONAL_AUTH_CREDENTIALS_REQUIRED_MSG);
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).entity(
                objFactories.getOpenStackIdentityV2Factory().createUnauthorized(fault).getValue())
                .header(HEADER_WWW_AUTHENTICATE, createWwwAuthenticateHeaderValue(encodedSessionId, secondFactor));
    }

    private String createWwwAuthenticateHeaderValue(String sessionId, String factor) {
        return String.format(HEADER_WWW_AUTHENTICATE_VALUE, sessionId, factor);
    }

    @Trace
    @Override
    public AuthResponseTuple authenticateSecondFactor(String encodedSessionId, CredentialType credential) {
        if (!(credential instanceof PasscodeCredentials)) {
            throw new BadRequestException("Not a valid credential. Only passcode credential supported for multifactor");
        }
        PasscodeCredentials passcodeCredentials = (PasscodeCredentials) credential;

        String passcode = passcodeCredentials.getPasscode();

        String userId;
        List<String> authenticatedBy;

        //first try to decode as an AE token. If fails, try session id
        ScopeAccess restrictedToken = scopeAccessService.unmarshallScopeAccess(encodedSessionId);
        if (restrictedToken == null) {
            LOG.debug("Invalid sessionId");
            throw new NotAuthenticatedException(INVALID_CREDENTIALS_GENERIC_ERROR_MSG);
        }

        //if resolves as an unrevoked AE token must validate as "correct" restricted token for this use
        if (!(restrictedToken instanceof UserScopeAccess)) {
            LOG.debug("Invalid sessionid. Not a user scope restricted token!");
            throw new ForbiddenException(INVALID_CREDENTIALS_GENERIC_ERROR_MSG);
        } else if (TokenScopeEnum.fromScope(restrictedToken.getScope()) != TokenScopeEnum.MFA_SESSION_ID) {
            LOG.debug("Invalid sessionid. Not a MFA SessionId restricted token!");
            throw new ForbiddenException(INVALID_CREDENTIALS_GENERIC_ERROR_MSG);
        } else if (restrictedToken.isAccessTokenExpired()) {
            LOG.debug("Invalid sessionid. Expired restricted token sessionid!");
            throw new NotAuthenticatedException(INVALID_CREDENTIALS_SESSIONID_EXPIRED_ERROR_MSG);
        }

        //is valid restricted token.
        UserScopeAccess token = (UserScopeAccess)restrictedToken;
        authenticatedBy = restrictedToken.getAuthenticatedBy();
        userId = token.getIssuedToUserId();

        LOG.debug("Session ID Validated");

        //verify user is valid
        User user = userService.getUserById(userId);
        if (user != null) {
            requestContextHolder.getAuthenticationContext().setUsername(user.getUsername());
            try {
                if (multiFactorService.isMultiFactorTypeOTP(user)) {
                    NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthMfaSecondOtp.getTransactionName());
                } else if (multiFactorService.isMultiFactorTypePhone(user)) {
                    NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthMfaSecondSms.getTransactionName());
                }
            } catch (Exception ex) {
                // Eat
            }
        }

        userService.validateUserIsEnabled(user);

        //TODO: FIXME: pass the user not the ID
        MfaAuthenticationResponse response = multiFactorService.verifyPasscode(userId, passcode);
        Audit mfaAudit = Audit.authUser(String.format("User(rsId=%s):(PASSCODE)", userId));
        if (response.getDecision() == MfaAuthenticationDecision.ALLOW) {
            mfaAudit.succeed();
            return createSuccessfulSecondFactorResponse(user, response, authenticatedBy);
        } else {
            if (response.getDecisionReason() == MfaAuthenticationDecisionReason.LOCKEDOUT) {
                emailClient.asyncSendMultiFactorLockedOutMessage(user);
            }
            //2-factor request denied. Determine appropriate exception/message for user
            mfaAudit.fail();
            throw createFailedSecondFactorException(response, userId);
        }
    }

    private AuthResponseTuple createSuccessfulSecondFactorResponse(User user, MfaAuthenticationResponse mfaResponse, List<String> authenticatedBy) {
        //return a token with the necessary authenticated by to reflect 2 factor authentication
        Set<String> authBySet = new HashSet<String>();

        if (!CollectionUtils.isEmpty(authenticatedBy)) {
            authBySet.addAll(authenticatedBy);
        }

        /*
        on successful auth, set the authenticatedBy on the token based on the type of MFA user is configured for. Even
        if the user was auth'd via bypass code, we still set to the type the user is configured for rather than show
        bypass code
         */
        AuthenticatedByMethodEnum authByMethod;
        if (multiFactorService.isMultiFactorTypeOTP(user) ) {
            authByMethod = AuthenticatedByMethodEnum.OTPPASSCODE;
        } else {
            authByMethod = AuthenticatedByMethodEnum.PASSCODE;
        }
        authBySet.add(authByMethod.getValue());

        UserScopeAccess scopeAccess = scopeAccessService.updateExpiredUserScopeAccess(user, getCloudAuthClientId(), new ArrayList<String>(authBySet));

        return new AuthResponseTuple(user, scopeAccess);
    }

    /**
     * Throws appropriately formatted exception that
     * @param mfaResponse
     * @param userId
     * @return
     */
    private RuntimeException createFailedSecondFactorException(MfaAuthenticationResponse mfaResponse, String userId) {
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
                String msg = String.format(NON_STANDARD_MFA_DENY_ERROR_MSG_FORMAT, userId, mfaResponse.getDecisionReason(), mfaResponse.getMessage());
                LOG.error(msg);
                exceptionToThrow = new MultiFactorDeniedException();
        }
        return exceptionToThrow;
    }

    @Override
    public Response.ResponseBuilder listMobilePhoneDevicesForUser(UriInfo uriInfo, String authToken, String userId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            EndUser user = requestContextHolder.getAndCheckTargetEndUser(userId);
            if (user instanceof FederatedUser) {
                throw new ForbiddenException("Cannot target federated user");
            }
            verifyEffectiveCallerAccessToTargetUser(user);

            List<MobilePhone> phoneList = multiFactorService.getMobilePhonesForUser((User)user);
            return Response.ok().entity(mobilePhoneConverterCloudV20.toMobilePhonesWebIncludingVerifiedFlag(phoneList, (User)user));

        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error listing devices on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder generateBypassCodes(UriInfo uriInfo, String authToken, String userId, BypassCodes bypassCodes) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            final BaseUser requester = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            final User user = requestContextHolder.checkAndGetTargetUser(userId);

            validateGenerateBypassCodesRequest(user);
            final boolean isSelfService = isSelfCall(requester, user) && !requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest();

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
        } catch (Exception ex) {
            LOG.error(String.format("Error listing devices on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder updateMultiFactorDomainSettings(UriInfo uriInfo, String authToken, String domainId, MultiFactorDomain multiFactorDomain) {
        try {
            //verify correct props are provided
            if (multiFactorDomain.getDomainMultiFactorEnforcementLevel() == null) {
                throw new BadRequestException(BAD_REQUEST_MSG_MISSING_DOMAIN_MFA_SETTINGS);
            }

            //get the token
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            /*
            this throws a NotFoundException (token not found) if the associated user or domain is disabled.
             */
            final BaseUser requester = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            EndUser caller;
            if (requester instanceof User || requester instanceof FederatedUser) {
                caller = (EndUser) requester;
            } else {
                throw new ForbiddenException(NOT_AUTHORIZED_ERROR_MSG);
            }

            // Verify user level
            IdentityUserTypeEnum requesterIdentityRole = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER);

            //if necessary, verify domain being changed is appropriate for user
            if (requesterIdentityRole.isDomainBasedAccessLevel() && (caller.getDomainId() == null || !caller.getDomainId().equalsIgnoreCase(domainId))) {
                throw new ForbiddenException(NOT_AUTHORIZED_ERROR_MSG);
            }

            if (requester instanceof User) {
                User user = (User) requester;
                /*
                For a user-admin/user-manager to change the state of MFA on a domain, the caller must have MFA enabled or have his/her user enforcement set to OPTIONAL. This guarantees
                that at least one user-admin/manager in the domain can continue to log-in after enabling domain level enforcement. Requiring it to disable
                domain level enforcement is for consistency and to allow a user who may have mistakenly disable it on the domain to immediately re-enable it.
                 */
                if (requesterIdentityRole.isDomainBasedAccessLevel() && !user.isMultiFactorEnabled() && !GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equals(user.getUserMultiFactorEnforcementLevel())) {
                    throw new ForbiddenException(FORBIDDEN_MSG_MULTIFACTOR_DISABLED);
                }
            }

            //counting on this method to throw NotFoundException if domainId does not exist
            multiFactorService.updateMultiFactorDomainSettings(domainId, multiFactorDomain);

            return Response.noContent();
        } catch (Exception ex) {
            LOG.error(String.format("Error changing multi-factor settings on domain '%s'", domainId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder addOTPDeviceToUser(UriInfo uriInfo, String authToken, String userId, OTPDevice otpDevice) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            final User user = requestContextHolder.checkAndGetTargetUser(userId);
            Validator20.validateItsNotUnverifiedUser(user);

            if (StringUtils.isBlank(otpDevice.getName())) {
                throw new BadRequestException(BAD_REQUEST_MSG_INVALID_OTP_DEVICE_NAME);
            }

            verifyEffectiveCallerAccessToTargetUser(user);

            final com.rackspace.idm.domain.entity.OTPDevice entity = multiFactorService.addOTPDeviceToUser(userId, otpDevice.getName());

            final UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            final String id = entity.getId();
            final URI build = requestUriBuilder.path(id).build();
            final Response.ResponseBuilder response = Response.created(build);

            response.entity(otpDeviceConverterCloudV20.toOTPDeviceForCreate(entity));
            return response;
        } catch (Exception ex) {
            LOG.error(String.format("Error adding an OTP device to user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder listOTPDevicesForUser(UriInfo uriInfo, String authToken, String userId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();
            userService.checkUserDisabled(requester); //must verify user/domain is enabled
            User user = requestContextHolder.checkAndGetTargetUser(userId);

            verifyEffectiveCallerAccessToTargetUser(user);

            List<com.rackspace.idm.domain.entity.OTPDevice> otpList = multiFactorService.getOTPDevicesForUser(user);
            return Response.ok().entity(otpDeviceConverterCloudV20.toOTPDevicesForWeb(otpList));
        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error listing otp devices on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getOTPDeviceFromUser(UriInfo uriInfo, String authToken, String userId, String deviceId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();
            User user = requestContextHolder.checkAndGetTargetUser(userId);
            verifyEffectiveCallerAccessToTargetUser(user);

            final com.rackspace.idm.domain.entity.OTPDevice entity = multiFactorService.checkAndGetOTPDeviceFromUserById(userId, deviceId);
            return Response.ok().entity(otpDeviceConverterCloudV20.toOTPDeviceForWeb(entity));
        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error retrieving device on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder deleteOTPDeviceFromUser(UriInfo uriInfo, String authToken, String userId, String deviceId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();
            User user = requestContextHolder.checkAndGetTargetUser(userId);
            verifyEffectiveCallerAccessToTargetUser(user);
            multiFactorService.deleteOTPDeviceForUser(userId, deviceId);
            return Response.status(Response.Status.NO_CONTENT);
        } catch (Exception ex) {
            LOG.error(String.format("Error deleting device '%s' on user '%s'", deviceId, userId), ex);
            if (ex instanceof ErrorCodeIdmException
                    && (ErrorCodes.ERROR_CODE_DELETE_OTP_DEVICE_FORBIDDEN_STATE.equals(((ErrorCodeIdmException) ex).getErrorCode()))) {
                //if can't delete due to user's MFA state, it's a bad request.
                return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
            }
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder verifyOTPCode(String authToken, String userId, String deviceId, VerificationCode verificationCode) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();
            final User user = requestContextHolder.checkAndGetTargetUser(userId);
            verifyEffectiveCallerAccessToTargetUser(user);

            validateVerifyRequest(deviceId, verificationCode);
            multiFactorService.verifyOTPDeviceForUserById(userId, deviceId, verificationCode.getCode());

            return Response.status(Response.Status.NO_CONTENT);
        } catch (MultiFactorDeviceAlreadyVerifiedException ex) {
            return exceptionHandler.badRequestExceptionResponse(BAD_REQUEST_MSG_ALREADY_VERIFIED);
        } catch (MultiFactorDevicePinValidationException ex) {
            return exceptionHandler.badRequestExceptionResponse(BAD_REQUEST_MSG_INVALID_PIN_OR_EXPIRED);
        } catch (NotFoundException ex) {
            return exceptionHandler.notFoundExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format(ERROR_VERIFYING_CODE_FOR_DEVICE, deviceId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder listMultiFactorDevicesForUser(UriInfo uriInfo, String authToken, String userId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);
            BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();
            userService.checkUserDisabled(requester); //must verify caller user/domain is enabled
            User user = requestContextHolder.checkAndGetTargetUser(userId);

            verifyEffectiveCallerAccessToTargetUser(user);

            List<MultiFactorDevice> deviceList = multiFactorService.getMultiFactorDevicesForUser(user);
            return Response.ok().entity(multiFactorDeviceConverterCloudV20.toMultiFactorDevicesForWeb(deviceList, user));
        } catch (IllegalStateException ex) {
            return exceptionHandler.badRequestExceptionResponse(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(String.format("Error listing multifactor devices on user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    /**
     * Validates the 3 types of update settings on a user.
     * @param targetUser
     * @param multiFactor
     */
    private void validateUpdateMultiFactorSettingsRequest(User targetUser, MultiFactor multiFactor) {
        Validate.notNull(targetUser);

        if (multiFactor == null) {
            LOG.debug(BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS);
        }

        //determine what updates are being made
        boolean isUserUnlockRequest = multiFactor.isUnlock() != null;
        boolean isUserMfaEnableRequest = multiFactor.isEnabled() != null;
        boolean isUserEnforcementLevelRequest = multiFactor.getUserMultiFactorEnforcementLevel() != null;

        if (isUserUnlockRequest) {
            validateUnlockMfaRequest(targetUser, multiFactor);
        }

        if (isUserMfaEnableRequest) {
            validateModifyUserMfaEnablement(targetUser);
        }

        if (isUserEnforcementLevelRequest) {
            validateUpdateUserEnforcementLevelRequest(targetUser);
        }
    }

    private void validateModifyUserMfaEnablement(User targetUser) {
        //Can not perform this action on unverified users
        Validator20.validateItsNotUnverifiedUser(targetUser);
        verifyEffectiveCallerAccessToTargetUser(targetUser);
    }

    private void validateUnlockMfaRequest(User targetUser, MultiFactor multiFactor) {
        //first verify precedence over user if not a self call
        verifyEffectiveCallerAccessToTargetUser(targetUser);

        //Can not perform this action on unverified users
        Validator20.validateItsNotUnverifiedUser(targetUser);

        BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();
        //Can not perform action on self
        if (multiFactor.isUnlock()) {
            if (requester.getId().equals(targetUser.getId())) {
                LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_OWN_ACCOUNT); //logged as debug because this is a bad request, not an error in app
                throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_OWN_ACCOUNT);
            }
        }
    }

    private void validateUpdateUserEnforcementLevelRequest(User targetUser) {
        BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();
        EndUser caller;
        if (requester instanceof EndUser) {
            caller = (EndUser) requester;
        } else {
            throw new ForbiddenException(NOT_AUTHORIZED_ERROR_MSG);
        }
        //what type of users are in play
        ClientRole requesterIdentityClientRole = applicationService.getUserIdentityRole(caller);
        IdentityUserTypeEnum requesterIdentityRole = authorizationService.getIdentityTypeRoleAsEnum(requesterIdentityClientRole);

        boolean isSelfRequest = isSelfCall(caller, targetUser);

        if (requesterIdentityRole == IdentityUserTypeEnum.DEFAULT_USER) {
            throw new ForbiddenException(NOT_AUTHORIZED_ERROR_MSG);
        }
        if (isSelfRequest && (requesterIdentityRole == IdentityUserTypeEnum.SERVICE_ADMIN || requesterIdentityRole == IdentityUserTypeEnum.IDENTITY_ADMIN)) {
            throw new ForbiddenException(NOT_AUTHORIZED_ERROR_MSG);
        }

        if (isSelfRequest && (requesterIdentityRole == IdentityUserTypeEnum.USER_ADMIN
                || requesterIdentityRole == IdentityUserTypeEnum.USER_MANAGER)) {
            //authorized
        } else {
            //need to retrieve target user role to authorize request
            ClientRole targetIdentityClientRole = applicationService.getUserIdentityRole(targetUser);
            IdentityUserTypeEnum targetIdentityRole = authorizationService.getIdentityTypeRoleAsEnum(targetIdentityClientRole);

            if (requesterIdentityRole == IdentityUserTypeEnum.SERVICE_ADMIN || requesterIdentityRole == IdentityUserTypeEnum.IDENTITY_ADMIN) {
                precedenceValidator.verifyHasGreaterAccess(requesterIdentityClientRole, targetIdentityClientRole);
            } else if (requesterIdentityRole == IdentityUserTypeEnum.USER_ADMIN) {
                precedenceValidator.verifyHasGreaterOrEqualAccess(requesterIdentityClientRole, targetIdentityClientRole);
                authorizationService.verifyDomain(caller, targetUser);
            } else if (requesterIdentityRole == IdentityUserTypeEnum.USER_MANAGER) {
                //breaks from standard as user-managers can change setting on user-admin
                if (!precedenceValidator.hasGreaterOrEqualAccess(requesterIdentityClientRole, targetIdentityClientRole)
                        && targetIdentityRole != IdentityUserTypeEnum.USER_ADMIN) {
                    throw new ForbiddenException(DefaultAuthorizationService.NOT_AUTHORIZED_MSG);
                }
                authorizationService.verifyDomain(caller, targetUser);
            } else {
                throw new ForbiddenException(DefaultAuthorizationService.NOT_AUTHORIZED_MSG);
            }
        }
    }

    /**
     * Validate the sendVerificationCodeRequest
     *
     * @param deviceId
     * @throws Exception if invalid request found
     */
    private void validateSendVerificationCodeRequest(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_DEVICE); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_DEVICE);
        }
    }

    private void validateAddPhoneToUserRequest(User user, com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone) {
        if (requestMobilePhone == null || StringUtils.isBlank(requestMobilePhone.getNumber())) {
            LOG.debug(BAD_REQUEST_MSG_MISSING_PHONE_NUMBER); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MISSING_PHONE_NUMBER);
        }
        else if (multiFactorService.userHasMultiFactorDevices(user) && user.isMultiFactorEnabled() && multiFactorService.isMultiFactorTypePhone(user)) {
            LOG.debug(BAD_REQUEST_MSG_MFA_ENABLED); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MFA_ENABLED);
        }
    }

    private void validateVerifyRequest(String deviceId, VerificationCode verificationCode) {
        if (StringUtils.isBlank(deviceId)) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_DEVICE); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_DEVICE);
        }
        else if (verificationCode == null || StringUtils.isBlank(verificationCode.getCode())) {
            LOG.debug(BAD_REQUEST_MSG_MISSING_VERIFICATION_CODE); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MISSING_VERIFICATION_CODE);
        }
    }

    private void validateVerifyVerificationCodeRequest(User targetUser, String deviceId, VerificationCode verificationCode) {
        validateVerifyRequest(deviceId, verificationCode);
        if (targetUser.getMultiFactorMobilePhoneRsId() == null || !targetUser.getMultiFactorMobilePhoneRsId().equals(deviceId)) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_DEVICE); //logged as debug because this is a bad request, not an error in app
            throw new NotFoundException(BAD_REQUEST_MSG_INVALID_DEVICE);
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

    private Integer getValidSecs(Duration duration, boolean isSelfService) {
        final BigInteger max = config.getBigInteger(BYPASS_MAXIMUM_DURATION, BigInteger.valueOf(10800));
        if (duration == null) {
            return isSelfService ? null : config.getBigInteger(BYPASS_DEFAULT_DURATION, BigInteger.valueOf(1800)).max(BigInteger.ONE).intValue();
        } else {
            return max.min(dateHelper.getSecondsFromDuration(duration)).max(BigInteger.ONE).intValue();
        }
    }

    private void validateGenerateBypassCodesRequest(User user) {
        if (user == null) {
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_USER);
        }

        BaseUser requester = requestContextHolder.getRequestContext().getEffectiveCaller();
        if (!isSelfCall(requester, user)) {
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER);
            verifyEffectiveCallerAccessToTargetUser(user);
        }

        // Verify if the user is valid
        userService.validateUserIsEnabled(user);

        //can only generate codes for mfa enabled users
        if (!user.isMultiFactorEnabled()) {
            throw new BadRequestException(FORBIDDEN_MSG_MULTIFACTOR_DISABLED);
        }
    }


    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

}
