package com.rackspace.idm.domain.security.tokenproviders.keystone_keyczar;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.security.TokenDNCalculator;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.security.tokenproviders.TokenDataPacker;
import org.apache.commons.lang.ArrayUtils;
import org.joda.time.DateTime;
import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Internal class to help encapsulate the data packing duties from everything else. Tradeoff between KISS and encapsulation.
 */
@Component()
public class KeystoneAEMessagePackTokenDataPacker implements TokenDataPacker {
    private static final Logger LOG = LoggerFactory.getLogger(KeystoneAEMessagePackTokenDataPacker.class);
    public static final String ERROR_CODE_UNPACK_KEYSTONE_AE_UNSUPPORTED_TOKEN_TYPE_FORMAT = "MPUK-0000";
    public static final String ERROR_CODE_UNPACK_KEYSTONE_AE_UNSUPPORTED_TOKEN_FORMAT = "MPUK-0001";

    private static final Map<Integer, String> AUTH_BY_UNMARSHALL = new HashMap<Integer, String>();

    private static final byte TOKEN_TYPE_UNSCOPED_USER = 0;
    private static final byte UNSCOPED_USER_TOKEN_FORMAT_1 = 1;
    private static final byte PROJECT_SCOPED_USER_TOKEN_FORMAT_3 = 3;

    private static final BigDecimal BIG_DECIMAL_1000 = new BigDecimal(1000);

    @Autowired
    private IdentityConfig identityConfig;

    static {
        AUTH_BY_UNMARSHALL.put(1, AuthenticatedByMethodEnum.PASSWORD.getValue());
    }

    @Override
    public byte[] packTokenDataForUser(BaseUser user, ScopeAccess token) {
        return new byte[0];
    }

    @Override
    public ScopeAccess unpackTokenData(String webSafeToken, byte[] packedData) {
        final ByteArrayInputStream in = new ByteArrayInputStream(packedData);
        final MessagePack messagePack = new MessagePack();
        Unpacker unpacker = messagePack.createUnpacker(in);

        ScopeAccess token;
        try {
            unpacker.readArrayBegin();

            //read the token type byte
            byte type = unpacker.readByte();
            byte format = unpacker.readByte();

            if (type == TOKEN_TYPE_UNSCOPED_USER && format == UNSCOPED_USER_TOKEN_FORMAT_1) {
                token = unpackProvisionedUserUnScopedToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_UNSCOPED_USER && format == PROJECT_SCOPED_USER_TOKEN_FORMAT_3) {
                token = unpackProvisionedUserProjectScopedToken(webSafeToken, unpacker);
            } else {
                throw new UnmarshallTokenException(ERROR_CODE_UNPACK_KEYSTONE_AE_UNSUPPORTED_TOKEN_TYPE_FORMAT, String.format("Unrecognized keystone v3 token type '%s' and format '%s' combination", type, format));
            }

            // End reading
            unpacker.readArrayEnd();
        } catch (IOException e) {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATA_CONTENTS, "Error encountered unpacking token", e);
        } catch (MessageTypeException ex) {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATA_CONTENTS, "Error encountered unpacking token", ex);
        } catch (Exception ex) {
            LOG.error("Unexpected error unpacking token", ex); //log this because not sure exactly what error causing this.
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATA_CONTENTS, "Error encountered unpacking token", ex);
        }

        return token;
    }


    /**
     *  Unscoped V1           /*
     b_user_id = cls.attempt_to_convert_uuid_hex_to_bytes(user_id)
     auth_by_list = cls._compress_methods_list_into_auth_by_list(methods)
     created_at_int = cls._convert_time_string_to_int(created_at)
     expires_at_int = cls._convert_time_string_to_int(expires_at)
     b_audit_ids = list(map(provider.random_urlsafe_str_to_bytes,
     audit_ids))
     return (b_user_id, auth_by_list, created_at_int, expires_at_int,
     b_audit_ids)

     versioned_payload = (token_type,) + (format_version,) + payload
     # NOTE(lbragstad): Global Auth pre-appends the packing scheme to the
     # serialized payload after it has been msgpack'd. We are doing the same
     # thing here with chr(0).
     serialized_payload = chr(0) + msgpack.packb(versioned_payload)

     */
    private ScopeAccess unpackProvisionedUserUnScopedToken(String webSafeToken, Unpacker unpacker) throws IOException {
        //extract the data from the pack
        Value rawUserIdValue = unpacker.readValue(); //a UUID, but could be compressed as bytes or just the UUID string
        List<String> authByList = decompressAuthenticatedBy(safeRead(unpacker, Integer[].class));
        Value createdAtRawValue = unpacker.readValue();
        Value expiresAtRawValue = unpacker.readValue();

        DateTime createdAt = convertToDateVal(createdAtRawValue);
        DateTime expiresAt = convertToDateVal(expiresAtRawValue);

        //an array of UUIDs, but could be compressed as bytes or just the UUID string. Ignored.
        Value b_audit_ids = unpacker.readValue();

        //construct the user token
        UserScopeAccess scopeAccess = new UserScopeAccess();

        // Timestamps
        scopeAccess.setAccessTokenExp(expiresAt.toDate());
        scopeAccess.setCreateTimestamp(createdAt.toDate());

        // ScopeAccess
        scopeAccess.setAuthenticatedBy(authByList);

        // UserScopeAccess
        scopeAccess.setUserRsId(uncompressUUID(rawUserIdValue));

        // DN
        scopeAccess.setUniqueId(TokenDNCalculator.calculateProvisionedUserTokenDN(scopeAccess.getUserRsId(), webSafeToken));
        scopeAccess.setClientId(identityConfig.getStaticConfig().getCloudAuthClientId());

        return scopeAccess;
    }

    /**
     * """Assemble the payload of a Keystone project scoped token.

     :param user_id: identifier of the user in the token request
     :param methods: list of authentication methods used
     :param project_id: identifier of the project to scope to
     :param created_at: datetime of the token's creation
     :param expires_at: datetime of the token's expiration
     :param audit_ids: list of the token's audit IDs
     :returns: the payload of a Keystone unscoped token

     """
     * @param webSafeToken
     * @param unpacker
     * @return
     * @throws IOException
     */
    private ScopeAccess unpackProvisionedUserProjectScopedToken(String webSafeToken, Unpacker unpacker) throws IOException {
        //extract the data from the pack
        Value rawUserIdValue = unpacker.readValue(); //a UUID, but could be compressed as bytes or just the UUID string
        List<String> authByList = decompressAuthenticatedBy(safeRead(unpacker, Integer[].class));
        Value projectId = unpacker.readValue(); //ignored

        Value createdAtRawValue = unpacker.readValue();
        Value expiresAtRawValue = unpacker.readValue();

        DateTime createdAt = convertToDateVal(createdAtRawValue);
        DateTime expiresAt = convertToDateVal(expiresAtRawValue);

        //an array of UUIDs, but could be compressed as bytes or just the UUID string. Ignored.
        Value b_audit_ids = unpacker.readValue();

        //construct the user token
        UserScopeAccess scopeAccess = new UserScopeAccess();

        // Timestamps
        scopeAccess.setAccessTokenExp(expiresAt.toDate());
        scopeAccess.setCreateTimestamp(createdAt.toDate());

        // ScopeAccess
        scopeAccess.setAuthenticatedBy(authByList);

        // UserScopeAccess
        scopeAccess.setUserRsId(uncompressUUID(rawUserIdValue));

        // DN
        scopeAccess.setUniqueId(TokenDNCalculator.calculateProvisionedUserTokenDN(scopeAccess.getUserRsId(), webSafeToken));
        scopeAccess.setClientId(identityConfig.getStaticConfig().getCloudAuthClientId());

        return scopeAccess;
    }

    private DateTime convertToDateVal(Value rawVal) {
        BigDecimal convertedDouble = new BigDecimal(rawVal.toString());
        return new DateTime(BIG_DECIMAL_1000.multiply(convertedDouble).longValue());
    }

    private <T> T safeRead(Unpacker unpacker, Class<T> clazz) throws IOException {
        return unpacker.trySkipNil() ? null : unpacker.read(clazz);
    }

    private List<String> decompressAuthenticatedBy(Integer[] authBy) {
        final List<String> authenticatedBy = new ArrayList<String>();
        if (ArrayUtils.isNotEmpty(authBy)) {
            for (Integer auth : authBy) {
                String authByVal = AUTH_BY_UNMARSHALL.get(auth);
                if (authByVal == null) {
                    throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_AUTHBY_DATA_CONTENTS, String.format("Unrecognized authby '%s'", auth));
                }
                authenticatedBy.add(AUTH_BY_UNMARSHALL.get(auth));
            }
        }
        return Collections.unmodifiableList(authenticatedBy);
    }

    /**
     * Takes in
     * @return
     */
    private String uncompressUUID(Value msgPackValue) {
        String result = null;
        if (msgPackValue.isRawValue()) {
            byte[] bytes = msgPackValue.asRawValue().getByteArray();
            try {
                result = getGuidFromByteArray(bytes);
            } catch (Exception e) {
                String raw = msgPackValue.toString();
                result = stripSurroundingQuotations(raw);
            }
        } else {
            result = msgPackValue.toString();
        }
        return result;
    }

    private String stripSurroundingQuotations(String raw) {
        String result = null;
        if (raw != null) {
            int start = 0;
            int end = raw.length();
            if (raw.startsWith("\"")) {
                start++;
            }
            if (raw.endsWith("\"")) {
                end--;
            }

            result = raw.substring(start, end);
        }
        return result;

    }

    public static String getGuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        UUID uuid = new UUID(high, low);
        return uuid.toString().replaceAll("-", "");
    }

}
