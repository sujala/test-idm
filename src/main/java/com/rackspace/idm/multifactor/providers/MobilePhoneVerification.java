package com.rackspace.idm.multifactor.providers;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.multifactor.domain.Pin;

public interface MobilePhoneVerification {

    /**
     * Sends a device a PIN (verification code). Returns the code that was sent to allow callers to verify
     *
     * @return
     * @throws SendPinException if there was a problem sending the pin to the phone
     */
    Pin sendPin(Phonenumber.PhoneNumber phoneNumber);
}
