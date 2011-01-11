package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface GenericTokenDao<T extends Token> {

    void save(T token);

    T findByTokenString(String tokenString);

    void delete(String tokenString);
}