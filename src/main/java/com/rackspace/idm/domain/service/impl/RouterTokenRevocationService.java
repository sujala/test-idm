package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.TokenRevocationService;
import com.rackspace.idm.domain.service.UUIDTokenRevocationService;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component(value = "tokenRevocationService")
public class RouterTokenRevocationService implements TokenRevocationService {
    private final AETokenRevocationService aeTokenRevocationService;
    private final UUIDTokenRevocationService uuidRevokeTokenService;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    public RouterTokenRevocationService(AETokenRevocationService aeTokenRevocationService, UUIDTokenRevocationService uuidRevokeTokenService) {
        this.aeTokenRevocationService = aeTokenRevocationService;
        this.uuidRevokeTokenService = uuidRevokeTokenService;
    }

    private TokenRevocationService getRouteByUniqueIdAndScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        if (aeTokenRevocationService.supportsRevokingFor(object, scopeAccess)) {
            return getRouteByUniqueId(object);
        } else {
            return uuidRevokeTokenService;
        }
    }

    private TokenRevocationService getRouteByUniqueId(UniqueId object) {
        if (object instanceof BaseUser) {
            return getRouteByBaseUser((BaseUser) object);
        } else {
            return uuidRevokeTokenService;
        }
    }

    private TokenRevocationService getRouteByBaseUser(BaseUser user) {
        return getRouteByTokenFormat(tokenFormatSelector.formatForNewToken(user));
    }

    private TokenRevocationService getRouteByUserId(String userId) {
        return getRouteByBaseUser(identityUserService.getEndUserById(userId));
    }

    private TokenRevocationService getRouteForExistingScopeAccess(ScopeAccess scopeAccess) {
        return getRouteForExistingScopeAccess(scopeAccess.getAccessTokenString());
    }

    private TokenRevocationService getRouteForExistingScopeAccess(String accessToken) {
        if (accessToken == null || accessToken.length() < 1) {
            throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
        }
        return getRouteByTokenFormat(tokenFormatSelector.formatForExistingToken(accessToken));
    }

    private TokenRevocationService getRouteByTokenFormat(TokenFormat tokenFormat) {
        if (tokenFormat == TokenFormat.AE) {
            return aeTokenRevocationService;
        } else {
            return uuidRevokeTokenService;
        }
    }

    @Override
    public void revokeToken(String tokenString) {
        getRouteForExistingScopeAccess(tokenString).revokeToken(tokenString);
    }

    @Override
    public void revokeToken(ScopeAccess token) {
        getRouteForExistingScopeAccess(token).revokeToken(token);
    }

    @Override
    public void revokeToken(BaseUser user, ScopeAccess scopeAccess) {
        getRouteForExistingScopeAccess(scopeAccess).revokeToken(user, scopeAccess);
    }

    @Override
    public void revokeTokensForBaseUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        //TODO: exapnd this to support rackers. Original implementation only supported EndUsers
        EndUser user = identityUserService.getEndUserById(userId);
        revokeTokensForBaseUser(user, authenticatedByMethodGroups);
    }

    @Override
    public void revokeTokensForBaseUser(BaseUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        getRouteByBaseUser(user).revokeTokensForBaseUser(user, authenticatedByMethodGroups);
    }

    @Override
    public void revokeAllTokensForBaseUser(String userId) {
        //TODO: exapnd this to support rackers. Original implementation only supported EndUsers
        EndUser user = identityUserService.getEndUserById(userId);
        revokeAllTokensForBaseUser(user);
    }

    @Override
    public void revokeAllTokensForBaseUser(BaseUser user) {
        getRouteByBaseUser(user).revokeAllTokensForBaseUser(user);
    }

    @Override
    public boolean isTokenRevoked(String token) {
        return getRouteForExistingScopeAccess(token).isTokenRevoked(token);
    }

    @Override
    public boolean isTokenRevoked(ScopeAccess token) {
        return getRouteForExistingScopeAccess(token).isTokenRevoked(token);
    }
}
