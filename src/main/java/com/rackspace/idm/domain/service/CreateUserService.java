package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.User;

public interface CreateUserService {

    /**
     * Given a User request object and the user to base defaults off of the service will:
     * - Convert the request object to a user entity object
     * - Set defaults on the user for the given user type and the userForDefaults
     * - Persist the user to the backing-store
     *
     * @param userForCreate
     * @param userForDefaults
     * @return
     */
    User setDefaultsAndCreateUser(org.openstack.docs.identity.api.v2.User userForCreate, User userForDefaults);

}
