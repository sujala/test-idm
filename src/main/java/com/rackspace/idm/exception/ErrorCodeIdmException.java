package com.rackspace.idm.exception;

import lombok.Getter;

public class ErrorCodeIdmException extends IdmException {

    @Getter
    private String errorCode;

    public ErrorCodeIdmException(String errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCodeIdmException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCodeIdmException(String errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public ErrorCodeIdmException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return String.format("Error code: '%s'; %s", errorCode, super.getMessage());
    }
}
