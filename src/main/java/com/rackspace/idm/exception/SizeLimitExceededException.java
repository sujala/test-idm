package com.rackspace.idm.exception;

import com.rackspace.idm.ErrorCodes;

public class SizeLimitExceededException extends IdmException {

    public SizeLimitExceededException(String message) {
        super(message, ErrorCodes.ERROR_CODE_MAX_SEARCH_RESULT_SIZE_EXCEEDED);
    }

    public SizeLimitExceededException(String message, Throwable cause) {
        super(message, ErrorCodes.ERROR_CODE_MAX_SEARCH_RESULT_SIZE_EXCEEDED, cause);
    }
}
