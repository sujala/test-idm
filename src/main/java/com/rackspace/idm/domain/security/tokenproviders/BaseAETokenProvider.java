package com.rackspace.idm.domain.security.tokenproviders;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.TokenDNCalculator;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.security.WebSafeTokenCoder;
import com.rackspace.idm.domain.security.encrypters.AuthenticatedMessageProvider;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;

/**
 * A base AE token provider that issues tokens using KeyCzar as the encryption/hashing mechanism and which the tokens are
 * <ul>
 *     <li>Prefixed with a unique byte indicating the provider</li>
 *     <li>Encoded via web safe base64</li>
 * </ul>
 *
 * This base class takes care of common operations around encrypting/hashing the token in the standard "AE" format as well
 * as prefixing the token with a byte to identify the provider during subsequent decryption.
 */
public abstract class BaseAETokenProvider implements TokenProvider {
    public static final String ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME = "AEU-0002";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME = "AEU-0003";

    @Autowired
    IdentityProviderDao identityProviderRepository;

    @Autowired
    protected IdentityConfig identityConfig;

    @Override
    public boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess) {
        return false;
    }

    @Override
    public boolean canDecryptScheme(byte scheme) {
        return scheme == getTokenScheme();
    }

    @Override
    public String marshallTokenForUser(BaseUser user, ScopeAccess token) {
        if (!supportsCreatingTokenFor(user, token)) {
            throw new UnsupportedOperationException(String.format("Creating a token for user type '%s' and token type " +
                    "'%s' is not supported by this provider", user.getClass().getSimpleName(), token.getClass().getSimpleName()));
        }

        //validate the request for minimum viable info across all tokens
        Validate.notNull(token, "Token can not be null");
        Validate.notNull(user, "User can not be null");
        Validate.notNull(token.getAccessTokenExp(), "Token must have an expiration date");
        Validate.notNull(token.getAuthenticatedBy(), "Token must not have a null authenticated by");

        //set the creation timestamp
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        token.setCreateTimestamp(cal.getTime());

        byte[] dataBytes = packProtectedTokenData(user, token);
        byte[] userTokenBytes = secureTokenData(dataBytes);

        //convert to web safe version
        String userToken = WebSafeTokenCoder.encodeTokenToWebSafe(userTokenBytes);

        //replace the token string with the generated one
        token.setAccessTokenString(userToken);
        token.setUniqueId(calculateTokenDN(user, userToken));

        return userToken;
    }

    protected abstract byte[] packProtectedTokenData(BaseUser user, ScopeAccess token);

    protected abstract ScopeAccess unpackProtectedTokenData(String webSafeToken, byte[] protectedTokenData);


    protected byte[] secureTokenData(byte[] dataBytes) {
        final byte[] securedTokenBytes = getAuthenticatedMessageProvider().encrypt(dataBytes);
        return appendEncryptionScheme(securedTokenBytes);
    }

    private byte[] appendEncryptionScheme(byte[] securedTokenBytes) {
        byte[] userTokenBytes = new byte[1 + securedTokenBytes.length];
        userTokenBytes[0] = getTokenScheme();
        System.arraycopy(securedTokenBytes, 0, userTokenBytes, 1, securedTokenBytes.length);
        return userTokenBytes;
    }

    @Override
    public ScopeAccess unmarshallToken(String webSafeToken) {
        Validate.notEmpty(webSafeToken);

        byte[] userTokenBytes = WebSafeTokenCoder.decodeTokenFromWebSafe(webSafeToken);
        byte[] dataBytes = unsecureTokenData(userTokenBytes);

        ScopeAccess scopeAccess = unpackProtectedTokenData(webSafeToken, dataBytes);

        return scopeAccess;
    }

    private byte[] unsecureTokenData(byte[] userTokenBytes) {
        if (userTokenBytes.length < 2) {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME, "Token does not contain an encryption scheme or data");
        }

        //extract the encryption scheme
        byte encryptionScheme = userTokenBytes[0];

        if (encryptionScheme == getTokenScheme()) {
            byte[] encryptionBytes = new byte[userTokenBytes.length - 1];
            System.arraycopy(userTokenBytes, 1, encryptionBytes, 0, encryptionBytes.length);
            return getAuthenticatedMessageProvider().decrypt(encryptionBytes);
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME, String.format("Unrecognized encryption scheme '%s'", encryptionScheme));
        }
    }

    protected abstract byte getTokenScheme();

    protected abstract AuthenticatedMessageProvider getAuthenticatedMessageProvider();

    private String calculateTokenDN(BaseUser user, String webSafeToken) {
        if (user instanceof User) {
            User user1 = (User) user;
            return TokenDNCalculator.calculateProvisionedUserTokenDN(user1.getId(), webSafeToken);
        } else if (user instanceof FederatedUser) {
            FederatedUser user1 = (FederatedUser) user;
            IdentityProvider provider = identityProviderRepository.getIdentityProviderByUri(user1.getFederatedIdpUri());
            return TokenDNCalculator.calculateFederatedUserTokenDN(user1.getUsername(), provider.getName(), webSafeToken);
        } else if (user instanceof Racker) {
            return TokenDNCalculator.calculateRackerTokenDN(((Racker) user).getRackerId(), webSafeToken);
        }
        throw new RuntimeException("Unsupported user type '" + user.getClass());
    }
}
