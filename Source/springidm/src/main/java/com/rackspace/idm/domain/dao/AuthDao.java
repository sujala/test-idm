package com.rackspace.idm.domain.dao;

public interface AuthDao {
    boolean authenticate(String username, String password);
}
