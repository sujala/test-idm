package com.rackspace.idm.exception;

public class InvalidPasswordPolicyException extends IdmException {
    public InvalidPasswordPolicyException(String message) {
        super(message);
    }

    public InvalidPasswordPolicyException(Throwable cause) {
        super(cause);
    }

    public InvalidPasswordPolicyException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPasswordPolicyException(String message, String errorCode) {
        super(message, errorCode);
    }

    public InvalidPasswordPolicyException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
