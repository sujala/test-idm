package com.rackspace.idm.domain.migration.dao;

import com.rackspace.idm.domain.migration.ChangeType;

import java.util.List;

public interface DeltaDao {

    public void save(ChangeType type, String event, String ldif);

    public List<?> findByType(String type);

    public void deleteAll();

}
