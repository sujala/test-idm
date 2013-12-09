package com.rackspace.idm.multifactor.providers.duo.service;

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.multifactor.domain.BasicPin;
import com.rackspace.idm.multifactor.domain.Pin;
import com.rackspace.idm.multifactor.providers.MobilePhoneVerification;
import com.rackspace.idm.multifactor.providers.duo.config.VerifyApiConfig;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoResponse;
import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;
import com.rackspace.idm.multifactor.providers.duo.util.DuoJsonResponseReader;
import com.rackspace.idm.multifactor.providers.duo.util.InMemoryDuoJsonResponseReader;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Duo Security implementation of the MobilePhoneVerification. Relies on duo to provide a random 4-digit pin code that is sent to the specified device. Duo
 * will then return the generated pin as part of the response.
 */
@Component
public class DuoMobilePhoneVerification implements MobilePhoneVerification {

    /*
     * Wire in a reader if a bean exists. If not, use the default in-memory implementation
     */
    @Autowired(required = false)
    private DuoJsonResponseReader duoJsonResponseReader = new InMemoryDuoJsonResponseReader();

    private Configuration globalConfig;

    private DuoRequestHelper duoRequestHelper;

    private final String VERIFY_ENDPOINT_BASE_PATH = "/verify/v1";
    private final WebResource VERIFY_ENDPOINT_BASE_RESOURCE;
    private final WebResource SMS_ENDPOINT_RESOURCE;

    private final WebResource STATUS_ENDPOINT_RESOURCE;

    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static final String PHONE_VERIFICATION_MESSAGE_PROP_NAME = "duo.security.verify.verification.message";

    public static final String PROP_NAME_DUO_TELEPHONY_ENABLED = "duo.telephony.enabled";

    /**
     * This is the default implementation that will be autowired by spring to use the global property file to configure the class
     * based on the AdminApiConfig which uses the global config.
     *
     * @param globalConfig
     */
    @Autowired
    public DuoMobilePhoneVerification(Configuration globalConfig) {
        this(new DuoRequestHelper(new VerifyApiConfig(globalConfig)), globalConfig);
    }

    public DuoMobilePhoneVerification(DuoRequestHelper duoRequestHelper, Configuration globalConfig) {
        this.duoRequestHelper = duoRequestHelper;
        this.globalConfig = globalConfig;

        VERIFY_ENDPOINT_BASE_RESOURCE = duoRequestHelper.createWebResource(VERIFY_ENDPOINT_BASE_PATH);
        SMS_ENDPOINT_RESOURCE = VERIFY_ENDPOINT_BASE_RESOURCE.path("sms");
        STATUS_ENDPOINT_RESOURCE = VERIFY_ENDPOINT_BASE_RESOURCE.path("status");
    }

    /**
     * This implementation returns a 4 digit numeric pin between 0-9999 (left padded with zeroes as necessary)
     *
     * @return
     */
    @Override
    public Pin sendPin(Phonenumber.PhoneNumber phoneNumber) {
        if (!globalConfig.getBoolean(PROP_NAME_DUO_TELEPHONY_ENABLED, false)) {
            throw new RuntimeException("Attempting to send SMS message that would incur cost when property '" + PROP_NAME_DUO_TELEPHONY_ENABLED + "' has this disabled (default)! Set this property to true if SMS messages should be sent!");
        }
        return sendSmsToPhone(phoneNumber, getPhoneVerificationMessage());
    }

    /**
     * Sends an sms text to the specified phone with the given message.
     *
     * @param phoneNumber
     * @param message must contain '<pin>' in it somewhere or duo security will return an error.
     * @return The pin that was sent to the phone
     */
    private BasicPin sendSmsToPhone(Phonenumber.PhoneNumber phoneNumber, String message) {
        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("phone", phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)) //Duo Security requires this format
                .put("message", message)
                .build();

        ClientResponse clientResponse = duoRequestHelper.makePostRequest(SMS_ENDPOINT_RESOURCE, map, ClientResponse.class);
        DuoResponse<BasicPin> response = duoJsonResponseReader.fromDuoResponse(clientResponse, BasicPin.class);

        if (response.isFailure()) {
            FailureResult failedResult = response.getFailureResult();
            throw new DuoSendPinException(failedResult);
        }

        return response.getSuccessResult();
    }

    private String getPhoneVerificationMessage() {
        return globalConfig.getString(PHONE_VERIFICATION_MESSAGE_PROP_NAME);
    }
}
