package com.rackspace.idm.multifactor.providers.duo.exception;

import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;
import com.rackspace.idm.multifactor.providers.exceptions.DeleteUserException;

/**
 * Represents an error creating a user through Duo Security
 */
public class DuoDeleteUserException extends DeleteUserException implements DuoException{
    FailureResult failureResult = null;

    public DuoDeleteUserException(FailureResult failureResult) {
        super(failureResult.getMessage());
        this.failureResult = failureResult;
    }

    public DuoDeleteUserException(FailureResult failureResult, String message) {
        super(message);
        this.failureResult = failureResult;
    }

    public FailureResult getFailureResult() {
        return failureResult;
    }
}
