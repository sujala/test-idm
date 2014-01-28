package com.rackspace.idm.multifactor.providers.simulator;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.identity.multifactor.domain.Pin;
import com.rackspace.identity.multifactor.providers.MobilePhoneVerification;

/**
 * A simple simulator to emulate a multi-factor provider's phone verification service by returning a constant pin without
 * actually sending a text to the phone.
 */
public class SimulatorMobilePhoneVerification implements MobilePhoneVerification {

    private Pin constantPin;

    public SimulatorMobilePhoneVerification(Pin constantPin) {
        this.constantPin = constantPin;
    }

    @Override
    public Pin sendPin(Phonenumber.PhoneNumber phoneNumber) {
        return constantPin;
    }

    public Pin getConstantPin() {
        return constantPin;
    }

    public void setConstantPin(Pin constantPin) {
        this.constantPin = constantPin;
    }
}
