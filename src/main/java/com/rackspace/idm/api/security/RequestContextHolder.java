package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;

public interface RequestContextHolder {
    void setImpersonated(boolean flag);

    boolean isImpersonated();

    void setEndUser(EndUser user);

    EndUser getEndUser(String userId);

    EndUser checkAndGetEndUser(String userId);

    User getUser(String userId);

    User checkAndGetUser(String userId);

    ScopeAccess getEffectiveCallerScopeAccess(String tokenString);

    void setEffectiveCallerScopeAccess(ScopeAccess scopeAccess);

    Domain getEffectiveCallerDomain(String domainId);
}
