package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

public interface ScopeAccessDao {

    void addScopeAccess(UniqueId object, ScopeAccess scopeAccess);

    @Deprecated
    void deleteScopeAccess(ScopeAccess scopeAccess);

    @Deprecated
    void updateScopeAccess(ScopeAccess scopeAccess);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    @Deprecated
    Iterable<ScopeAccess> getScopeAccessesByUserId(String userId);

    @Deprecated
    Iterable<ScopeAccess> getScopeAccesses(UniqueId object);

    @Deprecated
    Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUser(BaseUser user);

    @Deprecated
    Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByRsId(BaseUser user, String rsImpersonatingRsId);

    @Deprecated
    Iterable<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId);

    String getClientIdForParent(ScopeAccess scopeAccess);

}
