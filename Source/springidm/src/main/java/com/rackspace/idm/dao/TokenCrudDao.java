package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface TokenCrudDao<T extends Token> extends GenericTokenDao<T> {

    void save(T token);

    void delete(String tokenString);
}