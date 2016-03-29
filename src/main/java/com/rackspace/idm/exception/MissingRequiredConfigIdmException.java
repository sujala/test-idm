package com.rackspace.idm.exception;

public class MissingRequiredConfigIdmException extends IdmException {

    public MissingRequiredConfigIdmException() {

    }

    public MissingRequiredConfigIdmException(String message) {
        super(message);
    }

    public MissingRequiredConfigIdmException(Throwable cause) {
        super(cause);
    }

    public MissingRequiredConfigIdmException(String message, Throwable cause) {
        super(message, cause);
    }

}
