package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.AEScopeAccessDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UUIDScopeAccessDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

public class RouterScopeAccessRepository implements ScopeAccessDao {

    // TODO: Add AE support.
    private final AEScopeAccessDao aeScopeAccessDao;
    private final UUIDScopeAccessDao uuidScopeAccessDao;

    public RouterScopeAccessRepository(AEScopeAccessDao aeScopeAccessDao, UUIDScopeAccessDao uuidScopeAccessDao) {
        this.aeScopeAccessDao = aeScopeAccessDao;
        this.uuidScopeAccessDao = uuidScopeAccessDao;
    }

    private ScopeAccessDao getRouteByUsername(String username) {
        // TODO: Add AE support.
        return uuidScopeAccessDao;
    }

    private ScopeAccessDao getRouteByUserId(String userId) {
        // TODO: Add AE support.
        return uuidScopeAccessDao;
    }

    private ScopeAccessDao getRouteForExistingScopeAccess(ScopeAccess scopeAccess) {
        return getRouteForExistingScopeAccess(scopeAccess.getAccessTokenString());
    }

    private ScopeAccessDao getRouteForExistingScopeAccess(String accessToken) {
        // TODO: Add AE support.
        return uuidScopeAccessDao;
    }

    @Override
    public void addScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        getRouteByUsername(scopeAccess.getUsername()).addScopeAccess(object, scopeAccess);
    }

    @Override
    public void deleteScopeAccess(ScopeAccess scopeAccess) {
        getRouteForExistingScopeAccess(scopeAccess).deleteScopeAccess(scopeAccess);
    }

    @Override
    public void updateScopeAccess(ScopeAccess scopeAccess) {
        getRouteForExistingScopeAccess(scopeAccess).updateScopeAccess(scopeAccess);
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        return getRouteForExistingScopeAccess(accessToken).getScopeAccessByAccessToken(accessToken);
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessForUser(User user) {
        return getRouteByUsername(user.getUsername()).getMostRecentScopeAccessForUser(user);
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        // Foundation code. Irrelevant/unused.
        return uuidScopeAccessDao.getScopeAccessByRefreshToken(refreshToken);
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId) {
        ScopeAccessDao dao = uuidScopeAccessDao;
        if (object instanceof BaseUser) {
            dao = getRouteByUsername(((BaseUser) object).getUsername());
        }

        return dao.getMostRecentScopeAccessByClientId(object, clientId);
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessForUserOfUser(BaseUser user, String impersonatingUsername) {
        return getRouteByUsername(user.getUsername()).getMostRecentImpersonatedScopeAccessForUserOfUser(user, impersonatingUsername);
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessForUserRsId(BaseUser user, String impersonatingRsId) {
        return getRouteByUsername(user.getUsername()).getMostRecentImpersonatedScopeAccessForUserRsId(user, impersonatingRsId);
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessByClientIdAndAuthenticatedBy(UniqueId object, String clientId, List<String> authenticatedBy) {
        ScopeAccessDao dao = uuidScopeAccessDao;
        if (object instanceof BaseUser) {
            dao = getRouteByUsername(((BaseUser) object).getUsername());
        }

        return dao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(object, clientId, authenticatedBy);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByUserId(String userId) {
        return getRouteByUserId(userId).getScopeAccessesByUserId(userId);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccesses(UniqueId object) {
        ScopeAccessDao dao = uuidScopeAccessDao;
        if (object instanceof BaseUser) {
            dao = getRouteByUsername(((BaseUser) object).getUsername());
        }

        return dao.getScopeAccesses(object);
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUser(BaseUser user) {
        return getRouteByUsername(user.getUsername()).getAllImpersonatedScopeAccessForUser(user);
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByRsId(BaseUser user, String impersonatingRsId) {
        return getRouteByUsername(user.getUsername()).getAllImpersonatedScopeAccessForUserOfUserByRsId(user, impersonatingRsId);
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByUsername(BaseUser user, String impersonatingUsername) {
        return getRouteByUsername(user.getUsername()).getAllImpersonatedScopeAccessForUserOfUserByRsId(user, impersonatingUsername);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId) {
        ScopeAccessDao dao = uuidScopeAccessDao;
        if (object instanceof BaseUser) {
            dao = getRouteByUsername(((BaseUser) object).getUsername());
        }

        return dao.getScopeAccessesByClientId(object, clientId);
    }

    @Override
    public String getClientIdForParent(ScopeAccess scopeAccess) {
        return getRouteForExistingScopeAccess(scopeAccess).getClientIdForParent(scopeAccess);
    }

}