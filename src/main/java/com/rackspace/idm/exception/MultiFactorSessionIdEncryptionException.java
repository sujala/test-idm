package com.rackspace.idm.exception;

public class MultiFactorSessionIdEncryptionException extends IdmException {
    public MultiFactorSessionIdEncryptionException() {
    }

    public MultiFactorSessionIdEncryptionException(String message) {
        super(message);
    }

    public MultiFactorSessionIdEncryptionException(Throwable cause) {
        super(cause);
    }

    public MultiFactorSessionIdEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
