package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class BaseUrlConflictException extends IdmException {
    public BaseUrlConflictException() {
        super();
    }

    public BaseUrlConflictException(String message) {
        super(message);
    }
}
