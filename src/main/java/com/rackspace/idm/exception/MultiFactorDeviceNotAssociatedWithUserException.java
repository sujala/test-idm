package com.rackspace.idm.exception;

public class MultiFactorDeviceNotAssociatedWithUserException extends IdmException {
    public MultiFactorDeviceNotAssociatedWithUserException() {
    }

    public MultiFactorDeviceNotAssociatedWithUserException(String message) {
        super(message);
    }

    public MultiFactorDeviceNotAssociatedWithUserException(Throwable cause) {
        super(cause);
    }

    public MultiFactorDeviceNotAssociatedWithUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
