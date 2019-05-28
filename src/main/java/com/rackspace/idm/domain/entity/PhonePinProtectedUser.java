package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum;

public interface PhonePinProtectedUser {
    String getPhonePin();

    /**
     * Standard setter for the phone pin. If the phone pin is currently not null, and the phone pin is being changed, must reset the
     * failure count
     *
     * @param phonePin
     */
    void setPhonePin(String phonePin);

    /**
     * Returns the phone pin state for the user.
     * @return
     */
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

    /**
     * Updates the phone pin on the user. If the phone pin is currently not null and the phone pin is being changed,
     * must reset the failure count. The caller must subsequently save the user.
     *
     * @param phonePin
     */
    void updatePhonePin(String phonePin);

    /**
     * Unlocks the phone pin for the user.
     */
    void unlockPhonePin();
}
