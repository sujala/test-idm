package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.ScopeAccess;

public interface ScopeAccessDao {

    void addScopeAccess(UniqueId object, ScopeAccess scopeAccess);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);
}
