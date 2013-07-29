package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Racker;

public interface RackerDao {

    void addRacker(Racker racker);

    void deleteRacker(String rackerId);

    Racker getRackerByRackerId(String rackerId);
}
