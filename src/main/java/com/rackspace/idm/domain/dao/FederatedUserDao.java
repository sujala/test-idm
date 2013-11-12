package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.FederatedToken;
import com.rackspace.idm.domain.entity.User;

public interface FederatedUserDao {

    public void addUser(User user, String idpName);

    public User getUserByToken(FederatedToken token);

    public User getUserByUsername(String username, String idp);
}
