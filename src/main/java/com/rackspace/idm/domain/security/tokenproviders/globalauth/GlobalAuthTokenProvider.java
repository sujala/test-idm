package com.rackspace.idm.domain.security.tokenproviders.globalauth;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.security.WebSafeTokenCoder;
import com.rackspace.idm.domain.security.encrypters.AuthenticatedMessageProvider;
import com.rackspace.idm.domain.security.tokenproviders.BaseAETokenProvider;
import com.rackspace.idm.domain.security.tokenproviders.TokenDataPacker;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GlobalAuthTokenProvider extends BaseAETokenProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalAuthTokenProvider.class);

    public static final byte TOKEN_SCHEME = 0;

    public static final byte DATA_PACKING_SCHEME_MESSAGE_PACK = 0;

    @Autowired
    @Qualifier(value = "globalAuthMessagePackTokenDataPacker")
    private TokenDataPacker tokenDataPacker;

    @Autowired
    private AuthenticatedMessageProvider authenticatedMessageProvider;

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public byte getTokenScheme() {
        return TOKEN_SCHEME;
    }

    @Override
    public AuthenticatedMessageProvider getAuthenticatedMessageProvider() {
        return authenticatedMessageProvider;
    }

    /**
     * Returns whether the service supports creating tokens of the specified type against the specified user.
     *
     * @param object
     * @param scopeAccess
     * @return
     */
    @Override
    public boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess) {
        final boolean isProvisionedUser = object instanceof User;
        final boolean isFederatedUser = object instanceof FederatedUser;
        final boolean isImpersonationToken = scopeAccess instanceof ImpersonatedScopeAccess;
        final boolean isUserToken = scopeAccess instanceof UserScopeAccess;
        final boolean isRackerUser = object instanceof Racker;
        final boolean isRackerToken = scopeAccess instanceof RackerScopeAccess;
        
        // AE service supports
        // - federated and provisioned user "unrestricted" tokens
        // - provision user "restricted" and "un tokens
        // - provisioned users & rackers creating "impersonation" tokens (against fed/provisioned users)
        // - racker users creating "racker" tokens

        if (!identityConfig.getReloadableConfig().getFeatureAETokensEncrypt() && TokenScopeEnum.fromScope(scopeAccess.getScope()) != TokenScopeEnum.MFA_SESSION_ID) {
            return false; //if ae tokens are disabled then only support mfa session ids
        }

        return (isProvisionedUser && (isImpersonationToken || isUserToken))
                || (isFederatedUser && isUserToken)
                || (isRackerUser && (isImpersonationToken || isRackerToken));
    }

    @Override
    public ScopeAccess unmarshallToken(String webSafeToken) {
        ScopeAccess sa;
        if (!identityConfig.getReloadableConfig().getFeatureAETokensDecrypt()) {
            //decrypting all ae tokens except mfa session ids is disabled, so catch any exceptions
            try {
                sa = super.unmarshallToken(webSafeToken);
                if (TokenScopeEnum.fromScope(sa.getScope()) != TokenScopeEnum.MFA_SESSION_ID) {
                    sa = null;
                }
            } catch (Exception ex) {
                LOG.warn("Decrypting potential AE token when AE tokens are disabled through exception. Returning null.", ex);
                sa =  null;
            }
        } else {
            sa =  super.unmarshallToken(webSafeToken);
        }
        return sa;
    }

    @Override
    protected byte[] packProtectedTokenData(BaseUser user, ScopeAccess token) {
        byte[] packedBytes = tokenDataPacker.packTokenDataForUser(user, token);
        return appendPackingSchemeToPackedBytes(packedBytes);
    }

    private byte[] appendPackingSchemeToPackedBytes(byte[] packedBytes) {
        byte[] dataBytes = new byte[1 + packedBytes.length];
        dataBytes[0] = DATA_PACKING_SCHEME_MESSAGE_PACK;
        System.arraycopy(packedBytes, 0, dataBytes, 1, packedBytes.length);
        return dataBytes;
    }

    @Override
    protected ScopeAccess unpackProtectedTokenData(String webSafeToken, byte[] packedDataBytes) {
        if (packedDataBytes.length < 2) {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME, "Token does not contain a packing scheme or data");
        }

        //extract the packing scheme
        byte packingScheme = packedDataBytes[0];

        byte[] packedBytes = new byte[packedDataBytes.length - 1];
        System.arraycopy(packedDataBytes, 1, packedBytes, 0, packedBytes.length);

        ScopeAccess scopeAccess;
        if (packingScheme == DATA_PACKING_SCHEME_MESSAGE_PACK) {
            scopeAccess = tokenDataPacker.unpackTokenData(webSafeToken, packedBytes);
            scopeAccess.setAccessTokenString(webSafeToken);
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME, String.format("Unrecognized data packing scheme '%s'", packingScheme));
        }
        return scopeAccess;
    }

}
