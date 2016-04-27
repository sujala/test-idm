package com.rackspace.idm.domain.security;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.tokenproviders.TokenProvider;
import com.rackspace.idm.domain.service.AETokenRevocationService;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultAETokenService implements AETokenService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAETokenService.class);

    public static final String ERROR_CODE_UNMARSHALL_INVALID_BASE64 = "AEU-0001";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME = "AEU-0002";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME = "AEU-0003";

    @Autowired
    IdentityProviderDao identityProviderRepository;

    @Autowired
    private AETokenRevocationService aeTokenRevocationService;

    @Autowired
    private List<TokenProvider> tokenProviders;

    @Autowired
    private AETokenCache aeTokenCache;

    @Autowired
    private IdentityConfig identityConfig;

    /**
     * Returns whether the service support creating tokens of the specified type against the specified user.
     *
     * @param object
     * @param scopeAccess
     * @return
     */
    @Override
    public boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess) {
        return findFirstTokenProviderThatSupportsCreatingTokenFor(object, scopeAccess) != null;
    }

    @Override
    public String marshallTokenForUser(BaseUser user, ScopeAccess token) {
        //validate the request for minimum viable info across all tokens
        Validate.notNull(token, "Token can not be null");
        Validate.notNull(user, "User can not be null");
        Validate.notNull(token.getAccessTokenExp(), "Token must have an expiration date");
        Validate.notNull(token.getAuthenticatedBy(), "Token must not have a null authenticated by");

        TokenProvider provider = findFirstTokenProviderThatSupportsCreatingTokenFor(user, token);

        String marshalledToken = null;
        if (identityConfig.getReloadableConfig().cacheAETokens()) {
            marshalledToken = aeTokenCache.marshallTokenForUserWithProvider(user, token, provider);
        } else {
            marshalledToken = provider.marshallTokenForUser(user, token);
        }
        return marshalledToken;
    }

    @Override
    public ScopeAccess unmarshallToken(String webSafeToken) {
        Validate.notEmpty(webSafeToken);
        TokenProvider provider = findFirstTokenProviderThatSupportsTokenScheme(webSafeToken);
        return provider.unmarshallToken(webSafeToken);
    }

    @Override
    public ScopeAccess unmarshallTokenAndCheckRevoked(String webSafeToken) {
        TokenProvider provider = findFirstTokenProviderThatSupportsTokenScheme(webSafeToken);

        ScopeAccess access = provider.unmarshallToken(webSafeToken);
        if (access != null && aeTokenRevocationService.isTokenRevoked(access)) {
            LOG.debug(String.format("Token '%s' was revoked at some point and is no longer valid", webSafeToken));
            access = null;
        }
        return access;
    }

    private TokenProvider findFirstTokenProviderThatSupportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess) {
        for (TokenProvider tokenProvider : tokenProviders) {
            if (tokenProvider.supportsCreatingTokenFor(object, scopeAccess)) {
                return tokenProvider;
            }
        }
        return null;
    }

    private TokenProvider findFirstTokenProviderThatSupportsTokenScheme(String webSafeToken) {
        //decode the token from web base 64
        byte[] userTokenBytes = WebSafeTokenCoder.decodeTokenFromWebSafe(webSafeToken);

        //the first byte is the token scheme. Find a provider that supports it
        if (userTokenBytes.length < 2) {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME, "Token does not contain an encryption scheme or data");
        }
        //extract the encryption scheme
        byte tokenScheme = userTokenBytes[0];
        for (TokenProvider tokenProvider : tokenProviders) {
            if (tokenProvider.canDecryptScheme(tokenScheme)) {
                return tokenProvider;
            }
        }
        throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME, String.format("Unrecognized encryption scheme for token '%s'", tokenScheme));
    }
}
