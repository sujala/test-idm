package com.rackspace.idm.multifactor.providers.duo.exception;

import com.rackspace.idm.multifactor.providers.exceptions.SendPinException;
import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;

/**
 * Represents an error sending a pin code to a device through Duo Security
 */
public class DuoSendPinException extends SendPinException implements DuoException{
    FailureResult failureResult = null;

    public DuoSendPinException(FailureResult failureResult) {
        super(failureResult.getMessage());
        this.failureResult = failureResult;
    }

    public DuoSendPinException(FailureResult failureResult, String message) {
        super(message);
        this.failureResult = failureResult;
    }

    public FailureResult getFailureResult() {
        return failureResult;
    }
}
