package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseUserAuthenticationFactor implements UserAuthenticationFactor {

    @Autowired
    protected ScopeAccessService scopeAccessService;

    protected void validateUserAuthenticationResult(final UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            throw new NotAuthenticatedException(ErrorCodes.ERROR_CODE_AUTH_INVALID_CREDENTIALS_MSG, ErrorCodes.ERROR_CODE_AUTH_INVALID_CREDENTIALS);
        }
    }
}
