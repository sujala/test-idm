package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface TokenGetterDao<T extends Token> {

    T findByTokenString(String tokenString);

}