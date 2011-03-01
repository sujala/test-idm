package com.rackspace.idm.dao;

import com.rackspace.idm.domain.entity.AccessToken;

public interface XdcAccessTokenDao {

    AccessToken findByTokenString(final String tokenString);

    void delete(final String tokenString);

    void deleteAllTokensForOwner(String owner);

    void deleteAllTokensForCustomer(String customerId);
}