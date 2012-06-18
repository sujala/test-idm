package com.rackspace.idm.exception;

import javax.ws.rs.WebApplicationException;

import com.rackspace.idm.api.error.ApiError;

public class ApiException extends WebApplicationException {
    
    private static final long serialVersionUID = -227560195697678114L;
    private ApiError err;
    
    public ApiException(int code, String message, String details) {
        super(code);
        ApiError err = new ApiError(code, message, details);
        this.err = err;
    }

    @Override
    public String toString() {
        return String.format("%s [err=%s]",
            getMessage(), err);
    }
}
