package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface TokenFindDeleteDao<T extends Token> {

    T findByTokenString(String tokenString);
    
    void delete(String tokenString);
}