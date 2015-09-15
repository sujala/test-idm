package com.rackspace.idm.domain.security.tokenproviders.keystone_keyczar;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.security.encrypters.AuthenticatedMessageProvider;
import com.rackspace.idm.domain.security.tokenproviders.BaseAETokenProvider;
import com.rackspace.idm.domain.security.tokenproviders.TokenDataPacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class KeystoneAETokenProvider extends BaseAETokenProvider {
    private static final Logger LOG = LoggerFactory.getLogger(KeystoneAETokenProvider.class);
    private static final byte TOKEN_SCHEME = 1;

    private static final byte DATA_PACKING_SCHEME_MESSAGE_PACK = 0;

    @Autowired
    private AuthenticatedMessageProvider authenticatedMessageProvider;

    @Autowired
    @Qualifier(value = "keystoneAEMessagePackTokenDataPacker")
    private TokenDataPacker tokenDataPacker;

    /**
     * Returns whether the service supports creating tokens of the specified type against the specified user.
     *
     * @param object
     * @param scopeAccess
     * @return
     */
    @Override
    public boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess) {
        return false; //do not generate any tokens using this provider
    }

    @Override
    protected ScopeAccess unpackProtectedTokenData(String webSafeToken, byte[] protectedTokenData) {
        if (protectedTokenData.length < 2) {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME, "Token does not contain a packing scheme or data");
        }

        //extract the packing scheme
        byte packingScheme = protectedTokenData[0];

        byte[] packedBytes = new byte[protectedTokenData.length - 1];
        System.arraycopy(protectedTokenData, 1, packedBytes, 0, packedBytes.length);

        ScopeAccess scopeAccess;
        if (packingScheme == DATA_PACKING_SCHEME_MESSAGE_PACK) {
            scopeAccess = tokenDataPacker.unpackTokenData(webSafeToken, packedBytes);
            scopeAccess.setAccessTokenString(webSafeToken);
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME, String.format("Unrecognized data packing scheme '%s'", packingScheme));
        }
        return scopeAccess;
    }

    @Override
    protected byte[] packProtectedTokenData(BaseUser user, ScopeAccess token) {
        throw new UnsupportedOperationException("Provider does not support the generation of these tokens");
    }

    @Override
    protected byte getTokenScheme() {
        return TOKEN_SCHEME;
    }

    /**
     * This class supports the v3 keystone scheme, but only if that support is enabled.
     * @param scheme
     * @return
     */
    public boolean canDecryptScheme(byte scheme) {
        return scheme == TOKEN_SCHEME
                && identityConfig.getReloadableConfig().supportV3ProvisionedUserTokens();
    }

    @Override
    public AuthenticatedMessageProvider getAuthenticatedMessageProvider() {
        return authenticatedMessageProvider;
    }
}
