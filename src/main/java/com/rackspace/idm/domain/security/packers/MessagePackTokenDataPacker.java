package com.rackspace.idm.domain.security.packers;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.dao.impl.LdapIdentityProviderRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AeTokenService;
import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.UserService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Internal class to help encapsulate the data packing duties from everything else. Tradeoff between KISS and encapsulation.
 *
 * Eventually, if another data packer is chosen in the future or different versions are used
 */
@Component
public class MessagePackTokenDataPacker implements TokenDataPacker {
    private static final Logger LOG = LoggerFactory.getLogger(MessagePackTokenDataPacker.class);

    private static final byte VERSION_0 = 0;

    private static final byte TOKEN_TYPE_PROVISIONED_USER = 0;
    private static final byte TOKEN_TYPE_PROVISIONED_USER_IMPERSONATING_ENDUSER = 1;
    private static final byte TOKEN_TYPE_FEDERATED_USER = 2;

    private static final Map<String, Integer> AUTH_BY_MARSHALL = new HashMap<String, Integer>();
    private static final Map<Integer, String> AUTH_BY_UNMARSHALL = new HashMap<Integer, String>();

    private static final Map<String, Integer> SCOPE_MARSHALL = new HashMap<String, Integer>();
    private static final Map<Integer, String> SCOPE_UNMARSHALL = new HashMap<Integer, String>();

    public static final String CLOUD_AUTH_CLIENT_ID_PROP_NAME = "cloudAuth.clientId";

    public static final String ERROR_CODE_PACKING_EXCEPTION = "MPP-0001";
    public static final String ERROR_CODE_PACKING_INVALID_SCOPE_EXCEPTION = "MPP-0002";
    public static final String ERROR_CODE_PACKING_INVALID_AUTHENTICATEDBY_EXCEPTION = "MPP-0003";

    public static final String ERROR_CODE_UNPACKING_EXCEPTION = "MPU-0000";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_BASE64 = "MPU-0001";
    public static final String ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME = "MPU-0002";
    public static final String ERROR_CODE_UNPACK_INVALID_DATAPACKING_SCHEME = "MPU-0003";
    public static final String ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION = "MPU-0004";
    public static final String ERROR_CODE_UNPACK_INVALID_DATA_CONTENTS = "MPU-0005";
    public static final String ERROR_CODE_UNPACK_INVALID_SCOPE_DATA_CONTENTS = "MPU-0006";
    public static final String ERROR_CODE_UNPACK_INVALID_AUTHBY_DATA_CONTENTS = "MPU-0007";
    public static final String ERROR_CODE_UNPACK_INVALID_IMPERSONATED_DATA_CONTENTS = "MPU-0008";

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private UserService provisionedUserService;

    @Autowired
    LdapIdentityProviderRepository identityProviderRepository;

    /*
    This adds circular reference since AeTOkenService needs the message packer...
     */
    @Autowired
    private AeTokenService aeTokenService;

    static {
        AUTH_BY_MARSHALL.put(GlobalConstants.AUTHENTICATED_BY_PASSWORD, 1);
        AUTH_BY_MARSHALL.put(GlobalConstants.AUTHENTICATED_BY_APIKEY, 2);
        AUTH_BY_MARSHALL.put(GlobalConstants.AUTHENTICATED_BY_FEDERATION, 3);
        AUTH_BY_MARSHALL.put(GlobalConstants.AUTHENTICATED_BY_PASSCODE, 4);
        AUTH_BY_MARSHALL.put(GlobalConstants.AUTHENTICATED_BY_RSAKEY, 5);
        AUTH_BY_MARSHALL.put(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION, 6);
        for (String key : AUTH_BY_MARSHALL.keySet()) {
            AUTH_BY_UNMARSHALL.put(AUTH_BY_MARSHALL.get(key), key);
        }

        SCOPE_MARSHALL.put(GlobalConstants.SETUP_MFA_SCOPE, 1);
        for (String key : SCOPE_MARSHALL.keySet()) {
            SCOPE_UNMARSHALL.put(SCOPE_MARSHALL.get(key), key);
        }
    }

    @Override
    public byte[] packTokenDataForUser(BaseUser user, ScopeAccess token) {
        List<Object> packingItems = new ArrayList<Object>();

        if (token instanceof UserScopeAccess && user instanceof User) {
            packingItems.add(TOKEN_TYPE_PROVISIONED_USER);
            packingItems.addAll(packProvisionedUserToken((User) user, (UserScopeAccess) token));
        } else if (token instanceof UserScopeAccess && user instanceof FederatedUser) {
            packingItems.add(TOKEN_TYPE_FEDERATED_USER);
            packingItems.addAll(packFederatedUserToken((FederatedUser) user, (UserScopeAccess) token));
        } else if (token instanceof ImpersonatedScopeAccess && user instanceof User) {
            packingItems.add(TOKEN_TYPE_PROVISIONED_USER_IMPERSONATING_ENDUSER);
            packingItems.addAll(packProvisionedUserImpersonationToken((User) user, (ImpersonatedScopeAccess) token));
        } else {
            throw new UnsupportedOperationException("Unsupported " + user.getClass().getSimpleName() + " token:" + token.getClass().getSimpleName());
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final MessagePack messagePack = new MessagePack();
        final Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(packingItems.size());
            for (Object packingItem : packingItems) {
                packer.write(packingItem);
            }
            // Close array (verify if things were encoded properly)
            packer.writeArrayEnd();
            return out.toByteArray();
        } catch (IOException e) {
            throw new MarshallTokenException(ERROR_CODE_PACKING_EXCEPTION, "Encountered error packing the token data", e);
        }
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

            if (type == TOKEN_TYPE_PROVISIONED_USER) {
                token = unPackProvisionedUserToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_FEDERATED_USER) {
                token = unPackFederatedUserToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_PROVISIONED_USER_IMPERSONATING_ENDUSER) {
                token = unpackProvisionedUserImpersonationToken(webSafeToken, unpacker);
            } else {
                throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_SCHEME, String.format("Unrecognized data packing scheme '%s'", type));
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

    private List<Object> packProvisionedUserToken(User user, UserScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getUsername(), "username required");
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getUsername().equals(scopeAccess.getUsername()), "Token username must match user username");
        Validate.isTrue(user.getId().equals(scopeAccess.getUserRsId()), "Token userId must match user userId");

        List<Object> packingItems = new ArrayList<Object>();

        //packed version format (for future use...)
        packingItems.add(VERSION_0);

        // Timestamps
        packingItems.add(scopeAccess.getAccessTokenExp().getTime());
        packingItems.add(scopeAccess.getCreateTimestamp().getTime());

        // ScopeAccess
        packingItems.add(compressAuthenticatedBy(scopeAccess.getAuthenticatedBy()));
        packingItems.add(compressScope(scopeAccess.getScope()));

        // UserScopeAccess
        packingItems.add(scopeAccess.getUserRsId());

        return packingItems;
    }

    private ScopeAccess unPackProvisionedUserToken(String webSafeToken, Unpacker unpacker) throws IOException {
        UserScopeAccess scopeAccess = new UserScopeAccess();

        //packed version format (for future use...)
        byte version = unpacker.readByte();
        if (version == VERSION_0) {
            // Timestamps
            scopeAccess.setAccessTokenExp(new Date(unpacker.readLong()));
            scopeAccess.setCreateTimestamp(new Date(unpacker.readLong()));

            // ScopeAccess
            scopeAccess.setAuthenticatedBy(decompressAuthenticatedBy(safeRead(unpacker, Integer[].class)));
            scopeAccess.setScope(decompressScope(safeRead(unpacker, Integer.class)));

            // UserScopeAccess
            scopeAccess.setUserRsId(safeRead(unpacker, String.class));

            // DN
            scopeAccess.setUniqueId(calculateProvisionedUserTokenDN(scopeAccess.getUserRsId(), webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());

            //retrieve username from CA
            User user = identityUserService.getProvisionedUserById(scopeAccess.getUserRsId());
            scopeAccess.setUsername(user.getUsername());
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private List<Object> packFederatedUserToken(FederatedUser user, UserScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getUsername(), "username required");
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getUsername().equals(scopeAccess.getUsername()), "Token username must match user username");
        Validate.isTrue(user.getId().equals(scopeAccess.getUserRsId()), "Token userId must match user userId");

        List<Object> packingItems = new ArrayList<Object>();

        //packed version format (for future use...)
        packingItems.add(VERSION_0);

        // Timestamps
        packingItems.add(scopeAccess.getAccessTokenExp().getTime());
        packingItems.add(scopeAccess.getCreateTimestamp().getTime());

        // ScopeAccess
        packingItems.add(compressAuthenticatedBy(scopeAccess.getAuthenticatedBy()));

        // UserScopeAccess
        packingItems.add(scopeAccess.getUserRsId());

        return packingItems;
    }

    private ScopeAccess unPackFederatedUserToken(String webSafeToken, Unpacker unpacker) throws IOException {
        UserScopeAccess scopeAccess = new UserScopeAccess();

        //packed version format (for future use...)
        byte version = unpacker.readByte();
        if (version == VERSION_0) {
            // Timestamps
            scopeAccess.setAccessTokenExp(new Date(unpacker.readLong()));
            scopeAccess.setCreateTimestamp(new Date(unpacker.readLong()));

            // ScopeAccess
            scopeAccess.setAuthenticatedBy(decompressAuthenticatedBy(safeRead(unpacker, Integer[].class)));

            // UserScopeAccess
            scopeAccess.setUserRsId(safeRead(unpacker, String.class));

            // DN
            //TODO: Make this more efficient!
            FederatedUser user = identityUserService.getFederatedUserById(scopeAccess.getUserRsId());
            IdentityProvider idp = identityProviderRepository.getIdentityProviderByUri(user.getFederatedIdpUri());

            scopeAccess.setUniqueId(calculateFederatedUserTokenDN(user.getUsername(), idp.getName(), webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());
            scopeAccess.setUsername(user.getUsername());
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private List<Object> packProvisionedUserImpersonationToken(User user, ImpersonatedScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getUsername(), "username required");
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getUsername().equals(scopeAccess.getUsername()), "Token username must match user username");
        Validate.isTrue(StringUtils.isNotBlank(scopeAccess.getImpersonatingUsername()), "Impersonating username required");

        List<Object> packingItems = new ArrayList<Object>();

        //packed version format (for future use...)
        packingItems.add(VERSION_0);

        // Timestamps
        packingItems.add(scopeAccess.getAccessTokenExp().getTime());
        packingItems.add(scopeAccess.getCreateTimestamp().getTime());

        //Auth info
        packingItems.add(compressAuthenticatedBy(scopeAccess.getAuthenticatedBy()));
        packingItems.add(compressScope(scopeAccess.getScope()));

        //user info
        packingItems.add(user.getId());
        packingItems.add(scopeAccess.getImpersonatingRsId());

        return packingItems;
    }

    private ScopeAccess unpackProvisionedUserImpersonationToken(String webSafeToken, Unpacker unpacker) throws IOException {
        ImpersonatedScopeAccess scopeAccess = new ImpersonatedScopeAccess();

        //packed version format (for future use...)
        byte version = unpacker.readByte();
        if (version == VERSION_0) {
            // Timestamps
            scopeAccess.setAccessTokenExp(new Date(unpacker.readLong()));
            scopeAccess.setCreateTimestamp(new Date(unpacker.readLong()));

            //Auth info
            scopeAccess.setAuthenticatedBy(decompressAuthenticatedBy(safeRead(unpacker, Integer[].class)));
            scopeAccess.setScope(decompressScope(safeRead(unpacker, Integer.class)));

            //populate impersonator user info
            String impersonatorId = safeRead(unpacker, String.class);
            User impersonator = identityUserService.getProvisionedUserById(impersonatorId);
            scopeAccess.setUsername(impersonator.getUsername());

            //populate impersonator information
            String impersonatedUserId = safeRead(unpacker, String.class);
            EndUser impersonatedUser = identityUserService.getEndUserById(impersonatedUserId);
            if (impersonatedUser == null) {
                throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_IMPERSONATED_DATA_CONTENTS, String.format("Impersonated user '%s' not found.", scopeAccess.getImpersonatingUsername()));
            }
            scopeAccess.setImpersonatingRsId(impersonatedUserId);
            scopeAccess.setImpersonatingUsername(impersonatedUser.getUsername());

            //generate dynamic ae token for the user being impersonated
            UserScopeAccess usa = new UserScopeAccess();
            usa.setUserRsId(impersonatedUser.getId());
            usa.setUsername(impersonatedUser.getUsername());
            usa.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION);
            usa.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            aeTokenService.marshallTokenForUser(impersonatedUser, usa);
            scopeAccess.setImpersonatingToken(usa.getAccessTokenString());

            // DN
            scopeAccess.setUniqueId(calculateProvisionedUserTokenDN(impersonatorId, webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private <T> T safeRead(Unpacker unpacker, Class<T> clazz) throws IOException {
        return unpacker.trySkipNil() ? null : unpacker.read(clazz);
    }

    private Integer[] compressAuthenticatedBy(List<String> authenticatedBy) {
        Integer[] authBy = null;
        if (authenticatedBy != null || authenticatedBy.size() > 0) {
            authBy = new Integer[authenticatedBy.size()];
            for (int i = 0; i < authenticatedBy.size(); i++) {
                Integer authByVal = AUTH_BY_MARSHALL.get(authenticatedBy.get(i));
                if (authByVal == null) {
                    throw new MarshallTokenException(ERROR_CODE_PACKING_INVALID_AUTHENTICATEDBY_EXCEPTION, String.format("Unrecognized authby '%s'", authenticatedBy.get(i)));
                }
                authBy[i] = authByVal;
            }
        }
        return authBy;
    }

    private List<String> decompressAuthenticatedBy(Integer[] authBy) {
        final List<String> authenticatedBy = new ArrayList<String>();
        if (authBy != null || authBy.length > 0) {
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

    private Integer compressScope(String scope) {
        if (StringUtils.isNotBlank(scope)) {
            Integer val = SCOPE_MARSHALL.get(scope);
            if (val == null) {
                throw new MarshallTokenException(ERROR_CODE_PACKING_INVALID_SCOPE_EXCEPTION, String.format("Unrecognized scope '%s'", scope));
            }
            return val;
        }
        return null;
    }

    private String decompressScope(Integer scope) {
        if (scope != null) {
            String val = SCOPE_UNMARSHALL.get(scope);
            if (val == null) {
                throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_SCOPE_DATA_CONTENTS, String.format("Unrecognized scope '%s'", scope));
            }
            return val;
        }
        return null;
    }

    private String calculateProvisionedUserTokenDN(String userRsId, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", webSafeToken, userRsId);
    }

    private String calculateFederatedUserTokenDN(String username, String idpName, String webSafeToken) {
        return String.format("accessToken=%s,cn=TOKENS,uid=%s,ou=users,ou=%s,o=externalproviders,o=rackspace,dc=rackspace,dc=com", webSafeToken, username, idpName);
    }

    private String getCloudAuthClientId() {
        return config.getString(CLOUD_AUTH_CLIENT_ID_PROP_NAME);
    }
}
