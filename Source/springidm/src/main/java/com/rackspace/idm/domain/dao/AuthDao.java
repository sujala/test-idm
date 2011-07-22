package com.rackspace.idm.domain.dao;

import java.util.List;

public interface AuthDao {
    boolean authenticate(String username, String password);
    
    List<String> getRackerRoles(String username);
}
