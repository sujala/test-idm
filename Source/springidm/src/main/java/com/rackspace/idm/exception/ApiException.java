package com.rackspace.idm.exception;

import com.rackspace.idm.api.error.ApiError;

import javax.ws.rs.WebApplicationException;

public class ApiException extends WebApplicationException {
    
    private static final long serialVersionUID = -227560195697678114L;
    private ApiError err;
    
    public ApiException(int code, String message, String details) {
        super(code);
        ApiError error = new ApiError(code, message, details);
        this.err = error;
    }

    public ApiException(int code, String message, String details, Throwable cause) {
        super(cause, code);
        ApiError error = new ApiError(code, message, details);
        this.err = error;
    }

    @Override
    public String toString() {
        return String.format("%s [err=%s]",
            getMessage(), err);
    }
}
