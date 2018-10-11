package com.rackspace.idm.exception;

/**
 * An error encountered when accessing a server IDM depends upon for fulfilling the request.
 */
public class GatewayException extends IdmException {
    public GatewayException(String message, String errorCode) {
        super(message, errorCode);
    }

    public GatewayException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}