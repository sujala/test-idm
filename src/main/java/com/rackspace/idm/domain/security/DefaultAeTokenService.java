package com.rackspace.idm.domain.security;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.security.encrypters.AuthenticatedMessageProvider;
import com.rackspace.idm.domain.security.packers.TokenDataPacker;
import org.apache.commons.lang.Validate;
import org.keyczar.exceptions.Base64DecodingException;
import org.keyczar.util.Base64Coder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DefaultAeTokenService implements AeTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAeTokenService.class);

    private static final byte ENCRYPTION_SCHEME_KEYCZAR = 0;

    private static final byte DATA_PACKING_SCHEME_MESSAGE_PACK = 0;

    public static final String ERROR_CODE_MARSHALL_PACKING_EXCEPTION = "AEM-0001";
    public static final String ERROR_CODE_MARSHALL_INVALID_SCOPE_EXCEPTION = "AEM-0003";

    public static final String ERROR_CODE_UNMARSHALL_UNPACKING_EXCEPTION = "AEM-0000";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_BASE64 = "AEU-0001";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME = "AEU-0002";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME = "AEU-0003";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_VERSION = "AEU-0004";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_DATA_CONTENTS = "AEU-0005";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_SCOPE_DATA_CONTENTS = "AEU-0006";

    @Autowired
    private TokenDataPacker tokenDataPacker;

    @Autowired
    private AuthenticatedMessageProvider authenticatedMessageProvider;

    @Override
    public String marshallTokenForUser(BaseUser user, ScopeAccess token) {
        //validate the request for minimum viable info across all tokens
        Validate.notNull(token, "Token can not be null");
        Validate.notNull(user, "User can not be null");
        Validate.notNull(token.getAccessTokenExp(), "Token must have an expiration date");
        Validate.notNull(token.getAuthenticatedBy(), "Token must not have a null authenticated by");

        //set the creation timestamp
        token.setCreateTimestamp(new Date());

        byte[] dataBytes = packTokenData(user, token);
        byte[] userTokenBytes = secureTokenData(dataBytes);

        //convert to web safe version
        String userToken = Base64Coder.encodeWebSafe(userTokenBytes);

        //replace the token string with the generated one
        token.setAccessTokenString(userToken);

        token.setUniqueId(calculateTokenDN(user, userToken));

        return userToken;
    }

    @Override
    public ScopeAccess unmarshallToken(String webSafeToken) {
        Validate.notEmpty(webSafeToken);

        byte[] userTokenBytes;
        try {
            userTokenBytes = Base64Coder.decodeWebSafe(webSafeToken);
        } catch (Base64DecodingException e) {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_BASE64, "Error encountered decoding web token", e);
        }

        byte[] dataBytes = unsecureTokenData(userTokenBytes);
        ScopeAccess scopeAccess = unpackTokenData(webSafeToken, dataBytes);

        return scopeAccess;
    }

    private byte[] secureTokenData(byte[] dataBytes) {
        final byte[] securedTokenBytes = authenticatedMessageProvider.encrypt(dataBytes);
        return appendEncryptionScheme(securedTokenBytes);
    }

    private byte[] unsecureTokenData(byte[] userTokenBytes) {
        if (userTokenBytes.length < 2) {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME, "Token does not contain an encryption scheme or data");
        }

        //extract the encryption scheme
        byte encryptionScheme = userTokenBytes[0];

        if (encryptionScheme == ENCRYPTION_SCHEME_KEYCZAR) {
            byte[] encryptionBytes = new byte[userTokenBytes.length - 1];
            System.arraycopy(userTokenBytes, 1, encryptionBytes, 0, encryptionBytes.length);
            return authenticatedMessageProvider.decrypt(encryptionBytes);
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME, String.format("Unrecognized encryption scheme '%s'", encryptionScheme));
        }
    }

    private byte[] packTokenData(BaseUser user, ScopeAccess token) {
        byte[] packedBytes = tokenDataPacker.packTokenDataForUser(user, token);
        return appendPackingSchemeToPackedBytes(packedBytes);
    }

    private ScopeAccess unpackTokenData(String webSafeToken, byte[] packedDataBytes) {
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

    private byte[] appendPackingSchemeToPackedBytes(byte[] packedBytes) {
        byte[] dataBytes = new byte[1 + packedBytes.length];
        dataBytes[0] = DATA_PACKING_SCHEME_MESSAGE_PACK;
        System.arraycopy(packedBytes, 0, dataBytes, 1, packedBytes.length);
        return dataBytes;
    }

    private byte[] appendEncryptionScheme(byte[] securedTokenBytes) {
        byte[] userTokenBytes = new byte[1 + securedTokenBytes.length];
        userTokenBytes[0] = ENCRYPTION_SCHEME_KEYCZAR;
        System.arraycopy(securedTokenBytes, 0, userTokenBytes, 1, securedTokenBytes.length);
        return userTokenBytes;
    }

    private String calculateTokenDN(BaseUser user, String webSafeToken) {
        if (user instanceof User) {
            User user1 = (User) user;
            return calculateProvisionedUserTokenDN(user1.getId(), webSafeToken);
        }
        throw new RuntimeException("Unsupported user type '" + user.getClass());
    }

    private String calculateProvisionedUserTokenDN(String userRsId, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", webSafeToken, userRsId);
    }
}
