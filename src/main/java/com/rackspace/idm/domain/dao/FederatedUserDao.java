package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.FederatedToken;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

public interface FederatedUserDao {

    public void addUser(User user, String idpName);

    public User getUserByToken(FederatedToken token);

    public User getUserByUsername(String username, String idp);

    public Iterable<User> getUsersByDomainId(String domainId);

}
