package com.rackspace.idm.api.resource.cloud.v20;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.api.converter.cloudv20.MobilePhoneConverterCloudV20;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    static final String BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT = "Can only associate a mobile phone with own account";
    static final String BAD_REQUEST_MSG_ALREADY_LINKED = "Already associated with a mobile phone";
    static final String BAD_REQUEST_MSG_INVALID_PHONE_NUMBER = "The provided phone number is invalid.";

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

    /**
     * Validate the addPhoneToUserRequest
     *
     * @param requester
     * @param userId
     * @param requestMobilePhone
     * @throws Exception if invalid request found
     */
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

    /**
     * Parses the provided string based phone number into standard phone number representation.
     *
     * @param rawPhoneNumber
     * @throws BadRequestException if the number could not be parsed as a phone number
     * @return
     */
    private Phonenumber.PhoneNumber parseRequestPhoneNumber(String rawPhoneNumber) {
        try {
            Phonenumber.PhoneNumber phoneNumber = multiFactorService.parsePhoneNumber(rawPhoneNumber);
            return phoneNumber;
        } catch (InvalidPhoneNumberException ex) {
            throw new BadRequestException(BAD_REQUEST_MSG_INVALID_PHONE_NUMBER, ex);
        }
    }
}
