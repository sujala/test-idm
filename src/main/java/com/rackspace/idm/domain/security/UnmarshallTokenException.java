package com.rackspace.idm.domain.security;

import com.rackspace.idm.exception.ErrorCodeIdmException;

public class UnmarshallTokenException extends ErrorCodeIdmException {
    public UnmarshallTokenException(String message) {
        super(message);
    }

    public UnmarshallTokenException(String errorCode, String message) {
        super(errorCode, message);
    }

    public UnmarshallTokenException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
