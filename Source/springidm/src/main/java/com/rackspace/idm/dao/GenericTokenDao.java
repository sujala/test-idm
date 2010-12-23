package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface GenericTokenDao {

    void save(Token token);

    Token findByTokenString(String tokenString);

    void delete(String tokenString);
}