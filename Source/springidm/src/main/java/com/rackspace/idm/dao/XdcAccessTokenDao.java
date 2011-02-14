package com.rackspace.idm.dao;

import com.rackspace.idm.entities.AccessToken;

public interface XdcAccessTokenDao {

    AccessToken findByTokenString(final String tokenString);

    void delete(final String tokenString);

    void deleteAllTokensForOwner(String owner);
}