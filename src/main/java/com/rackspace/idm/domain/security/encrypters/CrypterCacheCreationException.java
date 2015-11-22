package com.rackspace.idm.domain.security.encrypters;

import com.rackspace.idm.exception.ErrorCodeIdmException;

class CrypterCacheCreationException extends ErrorCodeIdmException {
    public CrypterCacheCreationException(String message) {
        super(message);
    }

    public CrypterCacheCreationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public CrypterCacheCreationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
