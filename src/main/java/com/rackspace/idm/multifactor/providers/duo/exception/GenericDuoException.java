package com.rackspace.idm.multifactor.providers.duo.exception;

import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;
import com.rackspace.idm.multifactor.providers.exceptions.CreateUserException;

/**
 * Represents an error through Duo Security
 */
public class GenericDuoException extends IdmException implements DuoException{
    FailureResult failureResult = null;

    public GenericDuoException(FailureResult failureResult) {
        super(failureResult.getMessage());
        this.failureResult = failureResult;
    }

    public GenericDuoException(FailureResult failureResult, String message) {
        super(message);
        this.failureResult = failureResult;
    }

    public FailureResult getFailureResult() {
        return failureResult;
    }
}
