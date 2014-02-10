package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseUserAuthenticationFactor implements UserAuthenticationFactor {
    @Autowired
    protected Configuration config;

    @Autowired
    protected ScopeAccessService scopeAccessService;

    @Override
    public AuthResponseTuple createScopeAccessForUserAuthenticationResult(UserAuthenticationResult userAuthenticationResult) {
        UserScopeAccess sa = scopeAccessService.getValidUserScopeAccessForClientId((User)userAuthenticationResult.getUser(), getCloudAuthClientId(), userAuthenticationResult.getAuthenticatedBy());

        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser((User)userAuthenticationResult.getUser());
        authResponseTuple.setUserScopeAccess(sa);

        return authResponseTuple;
    }

    protected String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    protected void validateUserAuthenticationResult(final UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            throw new NotAuthenticatedException(errorMessage);
        }
    }
}
