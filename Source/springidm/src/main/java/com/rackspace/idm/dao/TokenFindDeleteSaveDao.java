package com.rackspace.idm.dao;

import com.rackspace.idm.oauthAuthentication.Token;

public interface TokenFindDeleteSaveDao<T extends Token> extends TokenFindDeleteDao<T> {

    void save(T token);
}