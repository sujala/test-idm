package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class TokenExpiredException extends IdmException {
    public TokenExpiredException() {
        super();
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(Throwable cause) {
        super(cause);
    }
}
