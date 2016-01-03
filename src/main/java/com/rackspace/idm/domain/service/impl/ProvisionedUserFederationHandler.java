package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.FederatedUser;

public interface ProvisionedUserFederationHandler extends FederationHandler {

    /**
     * Deletes the federated user and pushes that info to the proper logger and event queue.
     *
     * @param user
     */
    void deleteFederatedUser(FederatedUser user);

}
