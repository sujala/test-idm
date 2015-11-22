package com.rackspace.idm.exception;

import com.rackspace.idm.ErrorCodes;
import lombok.Getter;

@SuppressWarnings("serial")
public class IdmException extends RuntimeException {
    @Getter
    private String errorCode;

    public IdmException() {
        super();
    }

    public IdmException(String message) {
        super(message);
    }

    public IdmException(Throwable cause) {
        super(cause);
    }

    public IdmException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdmException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public IdmException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage()  {
        return ErrorCodes.generateErrorCodeFormattedMessage(errorCode, super.getMessage());
    }

}
