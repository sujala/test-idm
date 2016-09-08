package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import lombok.Data;
import lombok.Getter;

/**
 * This class represents the data around a successful authenticate response for an end user (non-racker user).
 * This data includes the user who authenticated, the user's returned scope access, and the impersonated scope
 * access if the request was an impersonation request.
 */
@Getter
public class AuthResponseTuple {

    private EndUser user;
    private UserScopeAccess userScopeAccess;
    private ImpersonatedScopeAccess impersonatedScopeAccess;

    public AuthResponseTuple(EndUser user, UserScopeAccess userScopeAccess) {
        this.user = user;
        this.userScopeAccess = userScopeAccess;
    }

    public AuthResponseTuple(EndUser user, UserScopeAccess userScopeAccess, ImpersonatedScopeAccess impersonatedScopeAccess) {
        this.user = user;
        this.userScopeAccess = userScopeAccess;
        this.impersonatedScopeAccess = impersonatedScopeAccess;
    }

    public AuthResponseTuple() {}

    public boolean isImpersonation() {
        return impersonatedScopeAccess != null;
    }
}
