package com.rackspace.idm.exception;

/**
 * @deprecated use IdmException class which now includes errorCode functionality
 */
@Deprecated
public class ErrorCodeIdmException extends IdmException {

    public ErrorCodeIdmException(String message) {
        super(message);
    }

    public ErrorCodeIdmException(String errorCode, String message) {
        super(message, errorCode);
    }

    public ErrorCodeIdmException(String errorCode, String message, Throwable cause) {
        super(message, errorCode, cause);
    }

}
