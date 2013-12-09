package com.rackspace.idm.multifactor.providers.duo.domain;

import lombok.Getter;

/**
 * A wrapper around the response received from Duo Security for all requests. Provides a standard way to determine whether the request succeeded/failed and to retrieve
 * the appropriate object based on the result.
*/
@Getter
public class DuoResponse<T> {
    private DuoStatus resultStatus;
    private T successResult;
    private FailureResult failureResult;

    public DuoResponse(DuoStatus resultStatus, T successResult) {
        this.resultStatus = resultStatus;
        this.successResult = successResult;
    }

    public DuoResponse(DuoStatus resultStatus, FailureResult failureResult) {
        this.resultStatus = resultStatus;
        this.failureResult = failureResult;
    }

    public boolean isSuccess() {
        return resultStatus == DuoStatus.OK;
    }

    public boolean isFailure() {
        return resultStatus == DuoStatus.FAIL;
    }
}
