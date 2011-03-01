package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Token;

public interface TokenDao<T extends Token> {

    void save(T token);
    
    T findByTokenString(String tokenString);
    
    void delete(String tokenString);
}