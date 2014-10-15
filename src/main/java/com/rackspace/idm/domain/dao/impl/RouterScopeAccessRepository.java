package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.AEScopeAccessDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UUIDScopeAccessDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class RouterScopeAccessRepository implements ScopeAccessDao {
    private final AEScopeAccessDao aeScopeAccessDao;
    private final UUIDScopeAccessDao uuidScopeAccessDao;

    @Autowired
    private LdapUserRepository ldapUserRepository;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    public RouterScopeAccessRepository(AEScopeAccessDao aeScopeAccessDao, UUIDScopeAccessDao uuidScopeAccessDao) {
        this.aeScopeAccessDao = aeScopeAccessDao;
        this.uuidScopeAccessDao = uuidScopeAccessDao;
    }

    private ScopeAccessDao getRouteByUniqueIdAndScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        if (aeScopeAccessDao.supportsCreatingTokenFor(object, scopeAccess)) {
            return getRouteByUniqueId(object);
        } else {
            return uuidScopeAccessDao;
        }
    }

    private ScopeAccessDao getRouteByUniqueId(UniqueId object) {
        if (object instanceof BaseUser) {
            return getRouteByBaseUser((BaseUser) object);
        } else {
            return uuidScopeAccessDao;
        }
    }

    private ScopeAccessDao getRouteByBaseUser(BaseUser user) {
        return getRouteByTokenFormat(tokenFormatSelector.formatForNewToken(user));
    }

    private ScopeAccessDao getRouteByUserId(String userId) {
        return getRouteByBaseUser(ldapUserRepository.getUserById(userId));
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
            return uuidScopeAccessDao;
        }
    }

    @Override
    public void addScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        getRouteByUniqueIdAndScopeAccess(object, scopeAccess).addScopeAccess(object, scopeAccess);
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
        return getRouteByBaseUser(user).getMostRecentScopeAccessForUser(user);
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId) {
        return getRouteByUniqueId(object).getMostRecentScopeAccessByClientId(object, clientId);
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessForUserOfUser(BaseUser user, String impersonatingUsername) {
        return getRouteByBaseUser(user).getMostRecentImpersonatedScopeAccessForUserOfUser(user, impersonatingUsername);
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessForUserRsId(BaseUser user, String impersonatingRsId) {
        return getRouteByBaseUser(user).getMostRecentImpersonatedScopeAccessForUserRsId(user, impersonatingRsId);
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessByClientIdAndAuthenticatedBy(UniqueId object, String clientId, List<String> authenticatedBy) {
        return getRouteByUniqueId(object).getMostRecentScopeAccessByClientIdAndAuthenticatedBy(object, clientId, authenticatedBy);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByUserId(String userId) {
        return getRouteByUserId(userId).getScopeAccessesByUserId(userId);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccesses(UniqueId object) {
        return getRouteByUniqueId(object).getScopeAccesses(object);
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUser(BaseUser user) {
        return getRouteByBaseUser(user).getAllImpersonatedScopeAccessForUser(user);
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByRsId(BaseUser user, String impersonatingRsId) {
        return getRouteByBaseUser(user).getAllImpersonatedScopeAccessForUserOfUserByRsId(user, impersonatingRsId);
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByUsername(BaseUser user, String impersonatingUsername) {
        return getRouteByBaseUser(user).getAllImpersonatedScopeAccessForUserOfUserByRsId(user, impersonatingUsername);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId) {
        return getRouteByUniqueId(object).getScopeAccessesByClientId(object, clientId);
    }

    @Override
    public String getClientIdForParent(ScopeAccess scopeAccess) {
        return getRouteForExistingScopeAccess(scopeAccess).getClientIdForParent(scopeAccess);
    }

    @Override
    @Deprecated
    /**
     * Foundation code. Irrelevant/unused.
     */
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        return uuidScopeAccessDao.getScopeAccessByRefreshToken(refreshToken);
    }

}