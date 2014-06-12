package com.rackspace.idm.api.resource.cloud.email;

import com.rackspace.idm.domain.entity.User;

public interface EmailClient {
    boolean sendMultiFactorLockoutOutMessage(User user);
    void asyncSendMultiFactorLockedOutMessage(User user);
}
