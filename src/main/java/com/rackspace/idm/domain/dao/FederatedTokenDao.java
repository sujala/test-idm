package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.FederatedToken;

public interface FederatedTokenDao {

    public Iterable<FederatedToken> getFederatedTokensByUserId(String userId);

}
