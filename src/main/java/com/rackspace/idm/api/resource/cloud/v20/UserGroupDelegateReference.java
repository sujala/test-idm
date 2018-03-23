package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.DelegateType;
import lombok.Getter;

/**
 * A reference to a user group.
 */
public class UserGroupDelegateReference implements DelegateReference {
    @Getter
    private String id;

    public UserGroupDelegateReference(String id) {
        this.id = id;
    }

    @Override
    public DelegateType getDelegateType() {
        return DelegateType.USER_GROUP;
    }
}
