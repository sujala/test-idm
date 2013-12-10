package com.rackspace.idm.multifactor.providers.duo.exception;

import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;
import com.rackspace.idm.multifactor.providers.exceptions.CreateUserException;
import com.rackspace.idm.multifactor.providers.exceptions.SendPinException;

/**
 * Represents an error creating a user through Duo Security
 */
public class DuoCreateUserException extends CreateUserException implements DuoException{
    FailureResult failureResult = null;

    public DuoCreateUserException(FailureResult failureResult) {
        super(failureResult.getMessage());
        this.failureResult = failureResult;
    }

    public DuoCreateUserException(FailureResult failureResult, String message) {
        super(message);
        this.failureResult = failureResult;
    }

    public FailureResult getFailureResult() {
        return failureResult;
    }
}
