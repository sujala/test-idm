package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.dao.impl.RackerAuthResult;

import java.util.List;

public interface RackerAuthDao {

    RackerAuthResult authenticateWithCache(String userName, String password);

    boolean authenticate(String username, String password);
    
    List<String> getRackerRoles(String username);

    List<String> getRackerRolesWithCache(String userName);
}
