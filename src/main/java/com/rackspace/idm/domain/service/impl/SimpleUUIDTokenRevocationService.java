package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionResponse;
import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UUIDScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.security.TokenFormatSelector;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.UUIDTokenRevocationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UnrecognizedAuthenticationMethodException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Simple in the sense that it goes against a single backend persistence mechanism for UUID token revocation.
 *
 * When revoking tokens, updates the expiration date on all applicable tokens to immediately expire
 */
@LDAPComponent
public class SimpleUUIDTokenRevocationService implements UUIDTokenRevocationService {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private UUIDScopeAccessDao scopeAccessDao;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private TokenFormatSelector tokenFormatSelector;

    @Override
    public boolean supportsRevokingFor(Token sa) {
        return sa != null && (sa instanceof ScopeAccess) && tokenFormatSelector.formatForExistingToken(sa.getAccessTokenString()) == TokenFormat.UUID;
    }

    public void revokeToken(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        LOG.debug("Revoking access token {}", token);
        final ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByAccessToken(token);
        revokeToken(scopeAccess);
    }

    @Override
    public void revokeToken(Token token) {
        if (token == null) {
            return;
        } else if (!supportsRevokingFor(token)) {
            throw new UnsupportedOperationException(String.format("Revocation service does not support revoking tokens of type '%s'", token.getClass().getSimpleName()));
        }
        ScopeAccess scopeAccess =  (ScopeAccess) token;

        if (scopeAccess.getAccessTokenExp().after(new Date())) {
            try {
                /*
                TODO: This is here for backwards compatibility with existing (wrong, IMO) code. When a token is to be revoked
                if the user associated with the token is disabled (or associated domain is disabled) then the token will
                be updated (expired), but a 404 NotFoundException will be thrown. There's an existing test case for this
                behavior (see Cloud20RevokeTokenIntegrationTest) so need to maintain this behavior..for now. Will request
                that A DEFECT be created for this
                 */
                BaseUser user = userService.getUserByScopeAccess(scopeAccess, true);
                revokeToken(user, scopeAccess);
            } catch (NotFoundException e) {
                revokeTokenInternal(scopeAccess);
                throw e;
            }
        } else {
                /*
                TODO: This is here for backwards compatibility with existing (wrong, IMO) code. When a token is to be revoked
                if the user associated with the token is disabled (or associated domain is disabled) a 404 NotFoundException
                will be thrown regardless of whether or not the token actually needs to be expired. There's an existing test case for this
                behavior (see Cloud20RevokeTokenIntegrationTest) so need to maintain this behavior..for now. Will request
                that A DEFECT be created for this
                 */
            userService.getUserByScopeAccess(scopeAccess, true);
            LOG.debug("Revoking access token was not required. Already expired {}", scopeAccess);
        }
    }

    @Override
    public void revokeToken(BaseUser user, Token token) {
        if (token == null) {
            return;
        } else if (!supportsRevokingFor(token)) {
            throw new UnsupportedOperationException(String.format("Revocation service does not support revoking tokens of type '%s'", token.getClass().getSimpleName()));
        }
        ScopeAccess scopeAccess =  (ScopeAccess) token;

        LOG.debug("Revoking access token {}", scopeAccess);
        if (scopeAccess.getAccessTokenExp().after(new Date())) {
            revokeTokenInternal(scopeAccess);
            if(user != null && user instanceof User){
                sendRevokeTokenFeedEvent((User) user, scopeAccess.getAccessTokenString());
            }
        }
        LOG.debug("Done revoking access token {}", scopeAccess.getAccessTokenString());
    }

    private void revokeTokenInternal(ScopeAccess scopeAccess) {
        scopeAccess.setAccessTokenExpired();
        scopeAccessDao.updateScopeAccess(scopeAccess);
    }

    @Override
    public void revokeTokensForEndUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeTokensForEndUser(user, authenticatedByMethodGroups);
    }

    @Override
    public void revokeTokensForEndUser(EndUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        if (user == null) return;

        //first determine in need to revoke all
        boolean revokeAll = false;
        for (AuthenticatedByMethodGroup revokeAuthBy : authenticatedByMethodGroups) {
            if (revokeAuthBy.matches(AuthenticatedByMethodGroup.ALL)) {
                revokeAll = true;
            }
        }

        /*
        for every token, loop through to see if it matches one of the groups being revoked.
         */
        DateTime now = new DateTime();
        for (final ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(user)) {
            if (sa.isAccessTokenExpired(now)) {
                //no point revoking token already expired
                continue;
            }
            else if (revokeAll) {
                revokeToken(user, sa);
            } else {
                try {
                    AuthenticatedByMethodGroup tokenAuthBy  = AuthenticatedByMethodGroup.getGroup(sa.getAuthenticatedBy());
                    for (AuthenticatedByMethodGroup revokeAuthBy : authenticatedByMethodGroups) {
                        if (revokeAuthBy.matches(tokenAuthBy)) {
                            revokeToken(user, sa);
                        }
                    }
                } catch (UnrecognizedAuthenticationMethodException e) {
                    LOG.error("Error attempting to revoke token - unknown authBy", e);
                }
            }
        }
    }

    @Override
    public void revokeAllTokensForEndUser(String userId) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeAllTokensForEndUser(user);
    }

    @Override
    public void revokeAllTokensForEndUser(EndUser user) {
        if (user == null) return;

        for (final ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(user)) {
            revokeToken(user, sa);
        }
    }

    @Override
    public boolean isTokenRevoked(String token) {
        ScopeAccess sa = scopeAccessDao.getScopeAccessByAccessToken(token);
        return isTokenRevoked(sa);
    }

    @Override
    public boolean isTokenRevoked(Token token) {
        Validate.notNull(token);

        if (!supportsRevokingFor(token)) {
            throw new UnsupportedOperationException(String.format("Revocation service does not support revoking tokens of type '%s'", token.getClass().getSimpleName()));
        }

        return token.isAccessTokenExpired();
    }

    @Override
    public TokenRevocationRecordDeletionResponse purgeObsoleteTokenRevocationRecords(int limit, int delay) {
        String id = MDC.get(Audit.GUUID); //use request audit id if provided
        if (StringUtils.isBlank(id)) {
            id = UUID.randomUUID().toString();
        }

        TokenRevocationRecordDeletionResponse response = new TokenRevocationRecordDeletionResponse(); //UUIDs don't have TRRs. Just the tokens themselves.
        response.setId(UUID.randomUUID().toString());
        response.setDeleted(0);
        response.setErrors(0);

        return response;
    }

    private void sendRevokeTokenFeedEvent(User user, String tokenString) {
        if (user != null) {
            LOG.warn("Sending token feed to atom hopper.");
            atomHopperClient.asyncTokenPost(user, tokenString);
        }
    }
}
