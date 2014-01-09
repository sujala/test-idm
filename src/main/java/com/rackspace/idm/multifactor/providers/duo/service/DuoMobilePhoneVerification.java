package com.rackspace.idm.multifactor.providers.duo.service;

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.multifactor.domain.Pin;
import com.rackspace.idm.multifactor.providers.MobilePhoneVerification;
import com.rackspace.idm.multifactor.providers.duo.config.VerifyApiConfig;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoPin;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoResponse;
import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;
import com.rackspace.idm.multifactor.providers.duo.exception.DuoSendPinException;
import com.rackspace.idm.multifactor.providers.duo.util.DuoJsonResponseReader;
import com.rackspace.idm.multifactor.providers.duo.util.InMemoryDuoJsonResponseReader;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Duo Security implementation of the MobilePhoneVerification. Relies on duo to provide a random 4-digit pin code that is sent to the specified device. Duo
 * will then return the generated pin as part of the response.
 */
@Component
public class DuoMobilePhoneVerification implements MobilePhoneVerification {
    private static final String VERIFY_ENDPOINT_BASE_PATH = "/verify/v1";

    /*
     * Wire in a reader if a bean exists. If not, use the default in-memory implementation
     */
    @Autowired(required = false)
    private DuoJsonResponseReader duoJsonResponseReader = new InMemoryDuoJsonResponseReader();

    @Autowired(required = false)
    DuoRequestHelperFactory duoRequestHelperFactory = new SingletonDuoRequestHelperFactory();

    @Autowired
    @Getter
    @Setter
    private VerifyApiConfig verifyApiConfig;

    private DuoRequestHelper duoRequestHelper;
    private WebResource VERIFY_ENDPOINT_BASE_RESOURCE;
    private WebResource SMS_ENDPOINT_RESOURCE;

    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    /**
     * Must be called after dependencies are injected and before the services are used.
     */
    @PostConstruct
    protected void init() {
        this.duoRequestHelper = duoRequestHelperFactory.getInstance(verifyApiConfig);

        VERIFY_ENDPOINT_BASE_RESOURCE = duoRequestHelper.createWebResource(VERIFY_ENDPOINT_BASE_PATH);
        SMS_ENDPOINT_RESOURCE = VERIFY_ENDPOINT_BASE_RESOURCE.path("sms");
    }

    /**
     * This implementation returns a 4 digit numeric pin between 0-9999 (left padded with zeroes as necessary)
     *
     * @return
     */
    @Override
    public Pin sendPin(Phonenumber.PhoneNumber phoneNumber) {
        if (!verifyApiConfig.allowServicesThatCostMoney()) {
            throw new RuntimeException("Consumption of services that cost money has been disabled. Attempting to send SMS message that would incur cost");
        }
        return sendSmsToPhone(phoneNumber, verifyApiConfig.getPhoneVerificationMessage());
    }

    /**
     * Sends an sms text to the specified phone with the given message.
     *
     * @param phoneNumber
     * @param message must contain '<pin>' in it somewhere or duo security will return an error.
     * @return The pin that was sent to the phone
     */
    private DuoPin sendSmsToPhone(Phonenumber.PhoneNumber phoneNumber, String message) {
        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("phone", phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)) //Duo Security requires this format
                .put("message", message)
                .build();

        ClientResponse clientResponse = duoRequestHelper.makePostRequest(SMS_ENDPOINT_RESOURCE, map, ClientResponse.class);
        DuoResponse<DuoPin> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoPin.class);

        if (response.isFailure()) {
            FailureResult failedResult = response.getFailureResult();
            throw new DuoSendPinException(failedResult);
        }

        return response.getSuccessResult();
    }
}
