package com.rackspace.idm.exception;

import com.rackspace.idm.ErrorCodes;

/**
 * Used to indicate a user doesn't have a pin set.
 */
public class NoPinSetException extends IdmException {

    public NoPinSetException() {
        super(ErrorCodes.ERROR_CODE_PHONE_PIN_NOT_FOUND_MSG, ErrorCodes.ERROR_CODE_PHONE_PIN_NOT_FOUND);
    }
}
