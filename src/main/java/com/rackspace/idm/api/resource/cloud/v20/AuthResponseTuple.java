package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import lombok.Data;

@Data
public class AuthResponseTuple {
    private User user;
    private UserScopeAccess userScopeAccess;
    private ImpersonatedScopeAccess impersonatedScopeAccess;
}
