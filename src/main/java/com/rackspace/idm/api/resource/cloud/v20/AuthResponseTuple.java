package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import lombok.Data;

@Data
public class AuthResponseTuple {
    private EndUser user;
    private UserScopeAccess userScopeAccess;
    private ImpersonatedScopeAccess impersonatedScopeAccess;

    public AuthResponseTuple(EndUser user, UserScopeAccess userScopeAccess) {
        this.user = user;
        this.userScopeAccess = userScopeAccess;
    }

    public AuthResponseTuple(EndUser user, ImpersonatedScopeAccess impersonatedScopeAccess) {
        this.user = user;
        this.impersonatedScopeAccess = impersonatedScopeAccess;
    }

    public AuthResponseTuple() {
    }
}
