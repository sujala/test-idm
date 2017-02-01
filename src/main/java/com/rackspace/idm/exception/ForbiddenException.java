package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class ForbiddenException extends IdmException {
    public ForbiddenException() {
        super();
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, String errorCode) {
        super(message, errorCode);
    }
}