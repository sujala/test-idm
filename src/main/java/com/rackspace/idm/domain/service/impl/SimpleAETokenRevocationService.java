package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.service.AETokenRevocationService;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.UUIDTokenRevocationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Simple in the sense that it goes against a single backend persistence mechanism for ae token revocation.
 */
@Component
public class SimpleAETokenRevocationService implements AETokenRevocationService {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Qualifier("tokenRevocationRecordPersistenceStrategy")
    @Autowired
    private TokenRevocationRecordPersistenceStrategy tokenRevocationRecordPersistenceStrategy;

    @Autowired
    private AETokenService aeTokenService;

    @Autowired(required = false)
    private UUIDTokenRevocationService uuidTokenRevocationService;

    @Autowired
    private IdentityConfig identityConfig;

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    @Override
    public boolean supportsRevokingFor(Token token) {
        //must be a ScopeAccess instance due to need to integrate with legacy services that expect ScopeAccess instances
        return token != null && token instanceof ScopeAccess && (token instanceof BaseUserToken)  && tokenFormatSelector.formatForExistingToken(token.getAccessTokenString()) == TokenFormat.AE;
    }

    @Override
    public void revokeToken(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            return;
        }
        try {
            //unmarshall the token to make sure it's a "real" token
            ScopeAccess token = aeTokenService.unmarshallToken(tokenString);
            revokeToken(token);
        } catch (UnmarshallTokenException e) {
            LOG.warn("Error unmarshalling token provided to revoke.", e);
            return;
        }
    }

    @Override
    public void revokeToken(Token token) {
        if (token == null) {
            return;
        } else if (!supportsRevokingFor(token)) {
            throw new UnsupportedOperationException(String.format("Revocation service does not support revoking tokens of type '%s'", token.getClass().getSimpleName()));
        }

        boolean tokenWasRevoked = revokeTokenIfNecessary(token);

        //only send revoke token for user based tokens.
        if (tokenWasRevoked && token instanceof BaseUserToken && token instanceof ScopeAccess) {
            try {
                //need to cast token to ScopeAccess to integrate with user service.
                BaseUser user = userService.getUserByScopeAccess((ScopeAccess)token, false);
                sendRevokeTokenFeedIfNecessary(user, token);
            } catch (NotFoundException e) {
                //ignore
            }
        }
    }

    @Override
    public void revokeToken(BaseUser user, Token token) {
        if (token == null) {
            return;
        } else if (!supportsRevokingFor(token)) {
            throw new UnsupportedOperationException(String.format("Revocation service does not support revoking tokens of type '%s'", token.getClass().getSimpleName()));
        }

        LOG.debug("Revoking access token {}", token);
        boolean tokenWasRevoked = revokeTokenIfNecessary(token);
        if (tokenWasRevoked && token instanceof BaseUserToken && token instanceof ScopeAccess) {
            //we just add a new record - regardless of whether there's already an existing record covering this token.
            tokenRevocationRecordPersistenceStrategy.addTokenTrrRecord(token.getAccessTokenString());
            sendRevokeTokenFeedIfNecessary(user, token);
        }
        LOG.debug("Done revoking access token {}", token.getAccessTokenString());
    }

    private boolean revokeTokenIfNecessary(Token token) {
        if (isTokenValid(token)) {
            tokenRevocationRecordPersistenceStrategy.addTokenTrrRecord(token.getAccessTokenString());
            return true;
        }
        return false;
    }

    private boolean isTokenValid(Token token) {
        return token.getAccessTokenExp().after(new Date()) && !isTokenRevoked(token);
    }

    private void sendRevokeTokenFeedIfNecessary(BaseUser user, Token token) {
        if (user != null && user instanceof User) {
            sendRevokeTokenFeedEvent((User) user, token.getAccessTokenString());
        }
    }

    @Override
    public void revokeTokensForEndUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeTokensForEndUser(user, authenticatedByMethodGroups);
    }

    @Override
    public void revokeTokensForEndUser(EndUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        TokenRevocationRecord trr = tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.getId(), authenticatedByMethodGroups);
        sendUserTrrFeedEventIfNecessary(user, trr);

        if (identityConfig.getFeatureAeTokenCleanupUuidOnRevokes()) {
            uuidTokenRevocationService.revokeTokensForEndUser(user, authenticatedByMethodGroups);
        }
    }

    @Override
    public void revokeAllTokensForEndUser(String userId) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeAllTokensForEndUser(user);
    }

    @Override
    public void revokeAllTokensForEndUser(EndUser user) {
        TokenRevocationRecord trr = tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.getId(), Arrays.asList(AuthenticatedByMethodGroup.ALL));
        sendUserTrrFeedEventIfNecessary(user, trr);

        if (identityConfig.getFeatureAeTokenCleanupUuidOnRevokes()) {
            uuidTokenRevocationService.revokeAllTokensForEndUser(user);
        }
    }

    @Override
    public boolean isTokenRevoked(String tokenStr) {
        ScopeAccess token = aeTokenService.unmarshallToken(tokenStr);
        return isTokenRevoked(token);
    }

    @Override
    public boolean isTokenRevoked(Token token) {
        Validate.notNull(token);

        if (!supportsRevokingFor(token)) {
            throw new UnsupportedOperationException(String.format("Revocation service does not support revoking tokens of type '%s'", token.getClass().getSimpleName()));
        }
        return tokenRevocationRecordPersistenceStrategy.doesActiveTokenRevocationRecordExistMatchingToken(token);
    }

    private void sendRevokeTokenFeedEvent(User user, String tokenString) {
        if (user != null) {
            LOG.warn("Sending token feed to atom hopper.");
            atomHopperClient.asyncTokenPost(user, tokenString);
        }
    }

    private void sendUserTrrFeedEventIfNecessary(BaseUser user, TokenRevocationRecord trr) {
        if (user != null && user instanceof User) {
            sendUserTrrFeedEvent((User) user, trr);
        }
    }

    private void sendUserTrrFeedEvent(User user, TokenRevocationRecord trr) {
        if (user != null) {
            LOG.warn("Sending User TRR event to atom hopper.");
            atomHopperClient.asyncPostUserTrr(user, trr);
        }
    }
}
