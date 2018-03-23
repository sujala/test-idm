package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.DelegateType;

/**
 * An abstraction to provide a degree of "typing" to an id in order to specify the type of object the ID represents. It
 * allows searches to be partitioned and made more efficiently.
 */
public interface DelegateReference {
    DelegateType getDelegateType();

    String getId();
}
