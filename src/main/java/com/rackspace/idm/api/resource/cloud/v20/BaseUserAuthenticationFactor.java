package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseUserAuthenticationFactor implements UserAuthenticationFactor {

    @Autowired
    protected ScopeAccessService scopeAccessService;

    protected void validateUserAuthenticationResult(final UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            throw new NotAuthenticatedException(errorMessage);
        }
    }
}
