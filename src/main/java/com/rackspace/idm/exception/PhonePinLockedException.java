package com.rackspace.idm.exception;

import com.rackspace.idm.ErrorCodes;

/**
 * Used to indicate a user's phone pin is locked.
 */
public class PhonePinLockedException extends IdmException {

    public PhonePinLockedException() {
        super(ErrorCodes.ERROR_MESSAGE_PHONE_PIN_LOCKED, ErrorCodes.ERROR_CODE_PHONE_PIN_LOCKED);
    }
}
