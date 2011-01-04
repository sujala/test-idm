package com.rackspace.idm.dao;

public interface AuthDao {
    boolean authenticate(String username, String password);
}
