package com.rackspace.idm.domain.security;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSortedSet;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.security.tokenproviders.TokenProvider;
import com.rackspace.idm.domain.service.AETokenRevocationService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
public class AETokenCache {
    private static final Logger LOG = LoggerFactory.getLogger(AETokenCache.class);

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired(required = false)
    private Ticker ticker;

    @Autowired
    private AETokenRevocationService aeTokenRevocationService;

    private Cache<String,TokenCacheEntry> cachedTokens;

    @PostConstruct
    public void init() {
        Ticker theTicker = ticker != null ? ticker : Ticker.systemTicker();
        cachedTokens = CacheBuilder.newBuilder()
                .maximumSize(identityConfig.getStaticConfig().cachedAETokenCacheMaxSize())
                .initialCapacity(identityConfig.getStaticConfig().cachedAETokenCacheInitialCapacity())
                .expireAfterWrite(identityConfig.getStaticConfig().cachedAETokenTTLSeconds(), TimeUnit.SECONDS)
                .concurrencyLevel(identityConfig.getStaticConfig().cachedAETokenCacheConcurrencyLevel())
                .ticker(theTicker)
                .build();
    }

    public String marshallTokenForUserWithProvider(final BaseUser user, final ScopeAccess token, final TokenProvider provider) {
        String webSafeToken = null;

        //only cache provisioned user regular tokens
        if (user instanceof User && token instanceof UserScopeAccess) {
            try {
                String userId = user.getId();
                String flatAuthBy = flattenAuthBy(token.getAuthenticatedBy());
                String key = userId + ":" + flatAuthBy;
                webSafeToken = getOrCreateTokenFromCache(user, token, key, provider);
            } catch (Exception e) {
                //if encounter any exception with cache, fallback to original
                webSafeToken =  provider.marshallTokenForUser(user, token);
            }
        } else {
            webSafeToken =  provider.marshallTokenForUser(user, token);
        }
        return webSafeToken;
    }

    private String flattenAuthBy(List<String> authByList) {
        ImmutableSortedSet<String> authByMethods =
                ImmutableSortedSet.<String>naturalOrder()
                        .addAll(authByList)
                        .build();

        String flatAuthBy = StringUtils.join(authByMethods, ",");
        return flatAuthBy;
    }

    private String getOrCreateTokenFromCache(final BaseUser user, final ScopeAccess token, final String key, final TokenProvider provider) {
        return getOrCreateTokenFromCacheWithInvalidationCheck(user, token, key, provider, 0);
    }

    private String getOrCreateTokenFromCacheWithInvalidationCheck(final BaseUser user, final ScopeAccess token, final String key, final TokenProvider provider, int recursionDepth) {
        /*
         failsafe recursion check. This should never happen as loading a newly generated token wouldn't be
         checked for revocation, but just to be on absolute safe side since consequences would be an
         infinite loop...
         */
        if (recursionDepth > 1) {
            throw new IllegalStateException(String.format("Hit recursion level '%s' trying to generate a " +
                    "retrieve AE token and placing in cache. Will fall back to issuing new token and ignoring cache ", recursionDepth));
        }
        //only cache provisioned user regular tokens
        if (user instanceof User && token instanceof UserScopeAccess) {
            try {
                AETokenGeneratorCallable aeTokenGeneratorCallable = new AETokenGeneratorCallable(provider, user, token);
                TokenCacheEntry tokenCacheEntry = cachedTokens.get(key, aeTokenGeneratorCallable);
                if (aeTokenGeneratorCallable.wasCacheHit()) {
                    if (aeTokenRevocationService.isTokenRevoked(tokenCacheEntry.accessTokenStr)) {
                        //Token has been revoked. Invalidate cache and just remake original call
                        LOG.debug("Token in cache was revoked. Retrieving new token and populating in cache");
                        cachedTokens.invalidate(key);

                        //recursively call this method to get retrieve the new token
                        return getOrCreateTokenFromCacheWithInvalidationCheck(user, token, key, provider, ++recursionDepth);
                    }
                    processCacheHit(tokenCacheEntry, user, token);
                }
                return token.getAccessTokenString();
            } catch (Exception e) {
                //if encounter any exception with cache, fallback to original
                LOG.error("Error using AE Token Cache. Falling back to issuing a unique AE Token.", e);
            }
        }
        return provider.marshallTokenForUser(user, token);
    }

    /**
     * This simulates the changes that the TokenProvider would make to the passed in ScopeAccess object when generating
     * a new token.
     *
     * @param tokenCacheEntry
     * @param user
     * @param token
     * @return
     */
    private void processCacheHit(TokenCacheEntry tokenCacheEntry, BaseUser user, ScopeAccess token) {
        //cache hit. Token is still good. Need to update the passed in token with the new info similar to
        //the changes the TokenProvider would do when generating a new token
        token.setAccessTokenExp(tokenCacheEntry.expirationDate);
        token.setAccessTokenString(tokenCacheEntry.accessTokenStr);
        token.setCreateTimestamp(tokenCacheEntry.creationDate);
        token.setUniqueId(tokenCacheEntry.uniqueId);
    }

    private class AETokenGeneratorCallable implements Callable<TokenCacheEntry>{
        final TokenProvider tokenProvider;
        final BaseUser user;
        final ScopeAccess token;

        final private CacheHit cacheHit = new CacheHit();

        public AETokenGeneratorCallable(TokenProvider tokenProvider, BaseUser user, ScopeAccess token) {
            this.tokenProvider = tokenProvider;
            this.user = user;
            this.token = token;
        }

        @Override
        public TokenCacheEntry call() {
            cacheHit.cacheHit = false; //generating a new token, so not a cache hit
            tokenProvider.marshallTokenForUser(user, token);
            return new TokenCacheEntry(token);
        }

        public boolean wasCacheHit() {
            return cacheHit.cacheHit;
        }
    }

    private class CacheHit {
        boolean cacheHit = true;
    }

    private class TokenCacheEntry {
        String accessTokenStr;
        Date creationDate;
        Date expirationDate;
        String uniqueId;

        public TokenCacheEntry(String accessTokenStr, Date creationDate, Date expirationDate, String uniqueId) {
            this.accessTokenStr = accessTokenStr;
            this.creationDate = creationDate;
            this.expirationDate = expirationDate;
            this.uniqueId = uniqueId;
        }

        public TokenCacheEntry(ScopeAccess token) {
            this.accessTokenStr = token.getAccessTokenString();
            this.creationDate = token.getCreateTimestamp();
            this.expirationDate = token.getAccessTokenExp();
            this.uniqueId = token.getUniqueId();
        }
    }
}
