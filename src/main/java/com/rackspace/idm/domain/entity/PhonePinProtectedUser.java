package com.rackspace.idm.domain.entity;

public interface PhonePinProtectedUser {
    String getPhonePin();
    void setPhonePin(String phonePin);

    PhonePinStateEnum getPhonePinState();

    /**
     * The user has failed PIN authentication. Make the necessary state changes on the user to record the failure.
     * Callers must then persist the user.
     */
    void recordFailedPinAuthentication();

    /**
     * The user has successfully performed a PIN authentication. Make the necessary state changes on the user to record the success.
     * Callers must then persist the user.
     */
    void recordSuccessfulPinAuthentication();
}
