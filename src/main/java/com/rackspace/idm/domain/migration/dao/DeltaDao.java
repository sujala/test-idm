package com.rackspace.idm.domain.migration.dao;

import com.rackspace.idm.domain.migration.ChangeType;

import java.util.List;

public interface DeltaDao {

    void save(ChangeType type, String event, String ldif);

    List<?> findByType(String type);

    void deleteAll();

}
