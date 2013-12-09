package com.rackspace.idm.multifactor.providers.duo.service;

import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;

/**
 * An exception thrown by the Duo Security provider that provides duo specific information about the error
 */
public interface DuoException {
    FailureResult getFailureResult();
}

