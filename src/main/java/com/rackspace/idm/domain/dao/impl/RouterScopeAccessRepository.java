package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.AEScopeAccessDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;

public class RouterScopeAccessRepository implements ScopeAccessDao {
    private final AEScopeAccessDao aeScopeAccessDao;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    public RouterScopeAccessRepository(AEScopeAccessDao aeScopeAccessDao) {
        this.aeScopeAccessDao = aeScopeAccessDao;
    }

    private ScopeAccessDao getRouteForExistingScopeAccess(ScopeAccess scopeAccess) {
        return getRouteForExistingScopeAccess(scopeAccess.getAccessTokenString());
    }

    private ScopeAccessDao getRouteForExistingScopeAccess(String accessToken) {
        if (accessToken == null || accessToken.length() < 1) {
            throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
        }
        return getRouteByTokenFormat(tokenFormatSelector.formatForExistingToken(accessToken));
    }

    private ScopeAccessDao getRouteByTokenFormat(TokenFormat tokenFormat) {
        if (tokenFormat == TokenFormat.AE) {
            return aeScopeAccessDao;
        } else {
            throw new IllegalArgumentException("Unrecognized token format");
        }
    }

    @Override
    public void addScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        aeScopeAccessDao.addScopeAccess(object, scopeAccess);
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        return getRouteForExistingScopeAccess(accessToken).getScopeAccessByAccessToken(accessToken);
    }

    @Override
    public String getClientIdForParent(ScopeAccess scopeAccess) {
        return getRouteForExistingScopeAccess(scopeAccess).getClientIdForParent(scopeAccess);
    }
}