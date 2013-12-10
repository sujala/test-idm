package com.rackspace.idm.multifactor.providers.exceptions;

import com.rackspace.idm.exception.IdmException;

/**
 * Simple marker class to indicate that there was a problem sending a pin.
 */
public class SendPinException extends IdmException {
    public SendPinException() {
    }

    public SendPinException(String message) {
        super(message);
    }

    public SendPinException(Throwable cause) {
        super(cause);
    }

    public SendPinException(String message, Throwable cause) {
        super(message, cause);
    }
}
