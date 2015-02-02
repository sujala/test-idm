package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.ScopeAccess;

public interface AEScopeAccessDao extends ScopeAccessDao {
    boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess);
}