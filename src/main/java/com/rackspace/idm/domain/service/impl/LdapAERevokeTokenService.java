package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AERevokeTokenService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class LdapAERevokeTokenService implements AERevokeTokenService {
    @Override
    public boolean supportsRevokingFor(UniqueId obj, ScopeAccess sa) {
        return false;
    }

    @Override
    public void revokeToken(String tokenString) {
    }

    @Override
    public void revokeToken(ScopeAccess tokenString) {
    }

    @Override
    public void revokeToken(BaseUser user, ScopeAccess scopeAccess) {
    }

    @Override
    public void revokeTokensForEndUser(String userId, List<Set<String>> authenticatedByList) {
    }

    @Override
    public void revokeTokensForEndUser(EndUser user, List<Set<String>> authenticatedByList) {
    }

    @Override
    public void revokeAllTokensForEndUser(String userId) {
    }

    @Override
    public void revokeAllTokensForEndUser(EndUser user) {
    }

    @Override
    public boolean isTokenRevoked(String token) {
        return false;
    }

    @Override
    public boolean isTokenRevoked(ScopeAccess token) {
        return false;
    }
}
