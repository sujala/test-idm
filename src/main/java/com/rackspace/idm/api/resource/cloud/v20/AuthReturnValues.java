package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import lombok.Data;

@Data
public class AuthReturnValues {
    User user;
    UserScopeAccess usa;
    ScopeAccess impsa;
}
