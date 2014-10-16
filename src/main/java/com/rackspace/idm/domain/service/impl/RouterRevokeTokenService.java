package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.service.AERevokeTokenService;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.RevokeTokenService;
import com.rackspace.idm.domain.service.UUIDRevokeTokenService;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component(value = "revokeTokenService")
public class RouterRevokeTokenService implements RevokeTokenService {
    private final AERevokeTokenService aeRevokeTokenService;
    private final UUIDRevokeTokenService uuidRevokeTokenService;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    public RouterRevokeTokenService(AERevokeTokenService aeRevokeTokenService, UUIDRevokeTokenService uuidRevokeTokenService) {
        this.aeRevokeTokenService = aeRevokeTokenService;
        this.uuidRevokeTokenService = uuidRevokeTokenService;
    }

    private RevokeTokenService getRouteByUniqueIdAndScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        if (aeRevokeTokenService.supportsRevokingFor(object, scopeAccess)) {
            return getRouteByUniqueId(object);
        } else {
            return uuidRevokeTokenService;
        }
    }

    private RevokeTokenService getRouteByUniqueId(UniqueId object) {
        if (object instanceof BaseUser) {
            return getRouteByBaseUser((BaseUser) object);
        } else {
            return uuidRevokeTokenService;
        }
    }

    private RevokeTokenService getRouteByBaseUser(BaseUser user) {
        return getRouteByTokenFormat(tokenFormatSelector.formatForNewToken(user));
    }

    private RevokeTokenService getRouteByUserId(String userId) {
        return getRouteByBaseUser(identityUserService.getEndUserById(userId));
    }

    private RevokeTokenService getRouteForExistingScopeAccess(ScopeAccess scopeAccess) {
        return getRouteForExistingScopeAccess(scopeAccess.getAccessTokenString());
    }

    private RevokeTokenService getRouteForExistingScopeAccess(String accessToken) {
        if (accessToken == null || accessToken.length() < 1) {
            throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
        }
        return getRouteByTokenFormat(tokenFormatSelector.formatForExistingToken(accessToken));
    }

    private RevokeTokenService getRouteByTokenFormat(TokenFormat tokenFormat) {
        if (tokenFormat == TokenFormat.AE) {
            return aeRevokeTokenService;
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
    public void revokeTokensForEndUser(String userId, List<Set<String>> authenticatedByList) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeTokensForEndUser(user, authenticatedByList);
    }

    @Override
    public void revokeTokensForEndUser(EndUser user, List<Set<String>> authenticatedByList) {
        getRouteByBaseUser(user).revokeTokensForEndUser(user, authenticatedByList);
    }

    @Override
    public void revokeAllTokensForEndUser(String userId) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeAllTokensForEndUser(user);
    }

    @Override
    public void revokeAllTokensForEndUser(EndUser user) {
        getRouteByBaseUser(user).revokeAllTokensForEndUser(user);
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
