package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionResponse;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.TokenRevocationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class RouterTokenRevocationService implements TokenRevocationService {

    @Autowired
    private AETokenRevocationService aeTokenRevocationService;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    @Autowired
    private IdentityUserService identityUserService;

    @Override
    public boolean supportsRevokingFor(Token token) {
        return aeTokenRevocationService.supportsRevokingFor(token);
    }

    private TokenRevocationService getRouteByBaseUser(BaseUser user) {
        return getRouteByTokenFormat(tokenFormatSelector.formatForNewToken(user));
    }

    private TokenRevocationService getRouteForExistingScopeAccess(Token token) {
        if (!supportsRevokingFor(token)) {
            throw new UnsupportedOperationException(String.format("Revocation service does not support revoking tokens of type '%s'", token.getClass().getSimpleName()));
        }
        return getRouteForExistingScopeAccess(token.getAccessTokenString());
    }

    /**
     * Base decision on provided token string. Do NOT load the token.
     * @param accessToken
     * @return
     */
    private TokenRevocationService getRouteForExistingScopeAccess(String accessToken) {
        return getRouteByTokenFormat(tokenFormatSelector.formatForExistingToken(accessToken));
    }

    private TokenRevocationService getRouteByTokenFormat(TokenFormat tokenFormat) {
        return aeTokenRevocationService;
    }

    @Override
    public void revokeToken(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            return;
        }
        getRouteForExistingScopeAccess(tokenString).revokeToken(tokenString);
    }

    @Override
    public void revokeToken(Token token) {
        if (token ==  null) {
            return;
        }
        getRouteForExistingScopeAccess(token).revokeToken(token);
    }

    @Override
    public void revokeToken(BaseUser user, Token token) {
        if (token ==  null) {
            return;
        }
        getRouteForExistingScopeAccess(token).revokeToken(user, token);
    }

    @Override
    public void revokeTokensForEndUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        //TODO: expand this to support rackers. Original UUID implementation only supported EndUsers
        EndUser user = identityUserService.getEndUserById(userId);
        revokeTokensForEndUser(user, authenticatedByMethodGroups);
    }

    @Override
    public void revokeTokensForEndUser(EndUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        getRouteByBaseUser(user).revokeTokensForEndUser(user, authenticatedByMethodGroups);
    }

    @Override
    public void revokeAllTokensForEndUser(String userId) {
        //TODO: expand this to support rackers. Original UUID implementation only supported EndUsers
        EndUser user = identityUserService.getEndUserById(userId);
        revokeAllTokensForEndUser(user);
    }

    @Override
    public void revokeAllTokensForEndUser(EndUser user) {
        getRouteByBaseUser(user).revokeAllTokensForEndUser(user);
    }

    @Override
    public void revokeAllTokensForIdentityProvider(String identityProviderId) {
        // We are not checking the route b/c UUID tokens are no longer supported
        aeTokenRevocationService.revokeAllTokensForIdentityProvider(identityProviderId);
    }

    @Override
    public boolean isTokenRevoked(String token) {
        return getRouteForExistingScopeAccess(token).isTokenRevoked(token);
    }

    @Override
    public boolean isTokenRevoked(Token token) {
        return getRouteForExistingScopeAccess(token).isTokenRevoked(token);
    }

    @Override
    public TokenRevocationRecordDeletionResponse purgeObsoleteTokenRevocationRecords(int limit, int delay) {
        //only ae token service stores TRRs. So just return it
        return aeTokenRevocationService.purgeObsoleteTokenRevocationRecords(limit, delay);
    }
}
