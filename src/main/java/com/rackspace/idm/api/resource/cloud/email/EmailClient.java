package com.rackspace.idm.api.resource.cloud.email;

import com.rackspace.idm.domain.entity.User;

public interface EmailClient {
    boolean sendMultiFactorLockoutOutMessage(User user);
    void asyncSendMultiFactorLockedOutMessage(User user);

    boolean sendMultiFactorEnabledMessage(User user);
    void asyncSendMultiFactorEnabledMessage(User user);

    boolean sendMultiFactorDisabledMessage(User user);
    void asyncSendMultiFactorDisabledMessage(User user);
}
