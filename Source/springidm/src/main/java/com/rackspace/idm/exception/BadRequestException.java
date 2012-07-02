package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class BadRequestException extends IdmException {
    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }
}
