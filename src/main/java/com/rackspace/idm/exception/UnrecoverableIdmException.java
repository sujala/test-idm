package com.rackspace.idm.exception;


/**
 * Thrown when Identity encounters and error that cannot be recovered through
 * any action on the client side. This exception should typically result in
 * a 500 response to the user due to the nature of the error.
 */
public class UnrecoverableIdmException extends IdmException {
    public UnrecoverableIdmException() {
    }

    public UnrecoverableIdmException(String message) {
        super(message);
    }

    public UnrecoverableIdmException(Throwable cause) {
        super(cause);
    }

    public UnrecoverableIdmException(String message, Throwable cause) {
        super(message, cause);
    }

}
