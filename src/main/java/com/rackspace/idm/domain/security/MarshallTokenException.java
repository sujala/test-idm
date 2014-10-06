package com.rackspace.idm.domain.security;

import com.rackspace.idm.exception.ErrorCodeIdmException;

public class MarshallTokenException extends ErrorCodeIdmException {
    public MarshallTokenException(String errorCode) {
        super(errorCode);
    }

    public MarshallTokenException(String errorCode, String message) {
        super(errorCode, message);
    }

    public MarshallTokenException(String errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public MarshallTokenException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
