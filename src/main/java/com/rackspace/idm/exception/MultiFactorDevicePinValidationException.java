package com.rackspace.idm.exception;

/**
 * Thrown when an error occurred validating the multifactor device's pin. For example, pins don't match, pin expired, etc.
 */
public class MultiFactorDevicePinValidationException extends IdmException{
    public MultiFactorDevicePinValidationException() {
    }

    public MultiFactorDevicePinValidationException(String message) {
        super(message);
    }

    public MultiFactorDevicePinValidationException(Throwable cause) {
        super(cause);
    }

    public MultiFactorDevicePinValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
