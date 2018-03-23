package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.DelegateType;
import lombok.Getter;

/**
 * A reference to an end user delegate. This includes both provisioned and federated users.
 */
@Getter
public class EndUserDelegateReference implements DelegateReference {
    private String id;

    public EndUserDelegateReference(String id) {
        this.id = id;
    }

    @Override
    public DelegateType getDelegateType() {
        return DelegateType.USER;
    }

}
