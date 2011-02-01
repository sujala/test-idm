package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface GenericTokenDao<T extends Token> {

    T findByTokenString(String tokenString);

}