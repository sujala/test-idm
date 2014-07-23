package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.IdentityUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultIdentityUserService implements IdentityUserService {

    @Autowired
    private IdentityUserDao identityUserRepository;

    @Override
    public EndUser getEndUserById(String userId) {
        return identityUserRepository.getEndUserById(userId);
    }

    @Override
    public User getProvisionedUserById(String userId) {
        return identityUserRepository.getProvisionedUserById(userId);
    }

    @Override
    public FederatedUser getFederatedUserById(String userId) {
        return identityUserRepository.getFederatedUserById(userId);
    }
}
