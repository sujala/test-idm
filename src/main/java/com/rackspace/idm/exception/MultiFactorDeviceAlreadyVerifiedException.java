package com.rackspace.idm.exception;

public class MultiFactorDeviceAlreadyVerifiedException extends IdmException {
    public MultiFactorDeviceAlreadyVerifiedException() {
    }

    public MultiFactorDeviceAlreadyVerifiedException(String message) {
        super(message);
    }

    public MultiFactorDeviceAlreadyVerifiedException(Throwable cause) {
        super(cause);
    }

    public MultiFactorDeviceAlreadyVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
