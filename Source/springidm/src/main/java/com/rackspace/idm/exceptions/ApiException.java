package com.rackspace.idm.exceptions;

import javax.ws.rs.WebApplicationException;

import com.rackspace.idm.errors.ApiError;

public class ApiException extends WebApplicationException {
    
    private static final long serialVersionUID = -227560195697678114L;
    private ApiError err;

    public ApiException(ApiError err) {
        super(err.getCode());
        this.err = err;
    }
    
    public ApiException(int code, String message, String details) {
        super(code);
        ApiError err = new ApiError(code, message, details);
        this.err = err;
    }

    public ApiError getError() {
        return err;
    }

    @Override
    public String toString() {
        return String.format("%s [err=%s]",
            getMessage(), err);
    }
}
