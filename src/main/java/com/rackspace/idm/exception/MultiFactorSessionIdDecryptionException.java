package com.rackspace.idm.exception;

public class MultiFactorSessionIdDecryptionException extends IdmException {
    public MultiFactorSessionIdDecryptionException() {
    }

    public MultiFactorSessionIdDecryptionException(String message) {
        super(message);
    }

    public MultiFactorSessionIdDecryptionException(Throwable cause) {
        super(cause);
    }

    public MultiFactorSessionIdDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
