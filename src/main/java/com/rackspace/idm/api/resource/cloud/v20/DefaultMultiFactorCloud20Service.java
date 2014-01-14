package com.rackspace.idm.api.resource.cloud.v20;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode;
import com.rackspace.idm.api.converter.cloudv20.MobilePhoneConverterCloudV20;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.multifactor.domain.BasicPin;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import com.rackspace.idm.multifactor.util.IdmPhoneNumberUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

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

    /*
    Used for convenience only. TODO:// Refactor cloud20 service to extract common code.
     */
    @Autowired
    private DefaultCloud20Service cloud20Service;

    @Autowired
    private UserService userService;

    @Autowired
    private Configuration config;

    @Autowired
    private MultiFactorService multiFactorService;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private MobilePhoneConverterCloudV20 mobilePhoneConverterCloudV20;

    @Override
    public Response.ResponseBuilder addPhoneToUser(UriInfo uriInfo, String authToken, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone) {
        verifyMultifactorServicesEnabled();

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
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder sendVerificationCode(UriInfo uriInfo, String authToken, String userId, String deviceId) {
        verifyMultifactorServicesEnabled();

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
        verifyMultifactorServicesEnabled();

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
        verifyMultifactorServicesEnabled();

        try {
            ScopeAccess token = cloud20Service.getScopeAccessForValidToken(authToken);
            User requester = (User) userService.getUserByScopeAccess(token);
            validateUpdateMultiFactorSettingsRequest(requester, userId, multiFactor);
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
        verifyMultifactorServicesEnabled();

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

    private void validateRemoveMultiFactorRequest(User requester, String userId) {
        if (requester == null || !(requester.getId().equals(userId))) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
            throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
        }
    }

    private void validateUpdateMultiFactorSettingsRequest(User requester, String userId, MultiFactor multiFactor) {
        if (multiFactor == null) {
            LOG.debug(BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS); //logged as debug because this is a bad request, not an error in app
            throw new BadRequestException(BAD_REQUEST_MSG_MISSING_MULTIFACTOR_SETTINGS);
        }
        else if (requester == null || !(requester.getId().equals(userId))) {
            LOG.debug(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT); //logged as debug because this is a bad request, not an error in app
            throw new ForbiddenException(BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT);
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

    private void verifyMultifactorServicesEnabled() {
        if (!config.getBoolean("multifactor.services.enabled", false)) {
            throw new WebApplicationException(404);
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
        } catch (InvalidPhoneNumberException ex) {
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_PHONE_NUMBER, ex);
        }
    }
}
