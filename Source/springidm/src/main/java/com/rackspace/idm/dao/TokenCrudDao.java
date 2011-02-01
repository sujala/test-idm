package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface TokenCrudDao<T extends Token> extends TokenGetterDao<T> {

    void save(T token);

    void delete(String tokenString);
}