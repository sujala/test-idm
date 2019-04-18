package com.rackspace.idm.domain.security.tokenproviders.globalauth;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.TokenDNCalculator;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.security.tokenproviders.TokenDataPacker;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ErrorCodeIdmException;
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
@Component(value = "globalAuthMessagePackTokenDataPacker")
public class MessagePackTokenDataPacker implements TokenDataPacker {
    private static final Logger LOG = LoggerFactory.getLogger(MessagePackTokenDataPacker.class);

    private static final byte VERSION_0 = 0;
    private static final byte VERSION_1 = 1;

    private static final byte TOKEN_TYPE_PROVISIONED_USER = 0;
    private static final byte TOKEN_TYPE_PROVISIONED_USER_IMPERSONATING_ENDUSER = 1;
    private static final byte TOKEN_TYPE_FEDERATED_USER = 2;
    private static final byte TOKEN_TYPE_RACKER = 3;
    private static final byte TOKEN_TYPE_RACKER_IMPERSONATING_ENDUSER = 4;
    private static final byte TOKEN_TYPE_PROVISIONED_USER_DELEGATE = 5;

    private static final Map<String, Integer> AUTH_BY_MARSHALL = new HashMap<String, Integer>();
    private static final Map<Integer, String> AUTH_BY_UNMARSHALL = new HashMap<Integer, String>();

    private static final Map<String, Integer> SCOPE_MARSHALL = new HashMap<String, Integer>();
    private static final Map<Integer, String> SCOPE_UNMARSHALL = new HashMap<Integer, String>();

    public static final String CLOUD_AUTH_CLIENT_ID_PROP_NAME = "cloudAuth.clientId";

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    IdentityProviderDao identityProviderRepository;

    /*
    This adds circular reference since AETokenService needs the message packer...
     */
    @Autowired
    private AETokenService aeTokenService;

    static {
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.PASSWORD.getValue(), 1);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.APIKEY.getValue(), 2);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.FEDERATION.getValue(), 3);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.PASSCODE.getValue(), 4);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.RSAKEY.getValue(), 5);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.IMPERSONATION.getValue(), 6);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.SYSTEM.getValue(), 7);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.OTPPASSCODE.getValue(), 8);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.EMAIL.getValue(), 9);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.TOKEN.getValue(), 10);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.OTHER.getValue(), 11);
        AUTH_BY_MARSHALL.put(AuthenticatedByMethodEnum.DELEGATE.getValue(), 12);

        for (String key : AUTH_BY_MARSHALL.keySet()) {
            AUTH_BY_UNMARSHALL.put(AUTH_BY_MARSHALL.get(key), key);
        }

        SCOPE_MARSHALL.put(GlobalConstants.SETUP_MFA_SCOPE, 1);
        SCOPE_MARSHALL.put(TokenScopeEnum.PWD_RESET.getScope(), 2);
        SCOPE_MARSHALL.put(TokenScopeEnum.MFA_SESSION_ID.getScope(), 3);
        for (String key : SCOPE_MARSHALL.keySet()) {
            SCOPE_UNMARSHALL.put(SCOPE_MARSHALL.get(key), key);
        }
    }

    @Override
    public byte[] packTokenDataForUser(BaseUser user, ScopeAccess saToken) {
        if (!(saToken instanceof BaseUserToken)) {
            throw new UnsupportedOperationException("Unsupported " + user.getClass().getSimpleName() + " token:" + saToken.getClass().getSimpleName());
        }
        BaseUserToken token = (BaseUserToken) saToken;

        final boolean isProvisionedUserDelegate = user instanceof ProvisionedUserDelegate;
        final boolean isProvisionedUser = user instanceof User;
        final boolean isFederatedUser = user instanceof FederatedUser;
        final boolean isRackerUser = user instanceof Racker;
        final boolean isImpersonationToken = token instanceof ImpersonatedScopeAccess;
        final boolean isUserToken = token instanceof UserScopeAccess;
        final boolean isRackerToken = token instanceof RackerScopeAccess;

        List<Object> packingItems = new ArrayList<Object>();
        if (token.isDelegationToken()) {
            if (!isProvisionedUserDelegate || !isUserToken) {
                throw new UnsupportedOperationException("Unsupported delegate token for " + user.getClass().getSimpleName() + " token:" + token.getClass().getSimpleName());
            }
            packingItems.addAll(packDelegateToken((ProvisionedUserDelegate) user, (UserScopeAccess) token));
        } else if (isUserToken && isProvisionedUser) {
            packingItems.add(TOKEN_TYPE_PROVISIONED_USER);
            packingItems.addAll(packProvisionedUserToken((User) user, (UserScopeAccess) token));
        } else if (isUserToken && isFederatedUser) {
            packingItems.add(TOKEN_TYPE_FEDERATED_USER);
            packingItems.addAll(packFederatedUserToken((FederatedUser) user, (UserScopeAccess) token));
        } else if (isImpersonationToken && isProvisionedUser) {
            packingItems.add(TOKEN_TYPE_PROVISIONED_USER_IMPERSONATING_ENDUSER);
            packingItems.addAll(packProvisionedUserImpersonationToken((User) user, (ImpersonatedScopeAccess) token));
        } else if (isRackerToken && isRackerUser) {
            packingItems.add(TOKEN_TYPE_RACKER);
            packingItems.addAll(packRackerToken(((Racker) user), (RackerScopeAccess) token));
        } else if (isImpersonationToken && isRackerUser) {
            packingItems.add(TOKEN_TYPE_RACKER_IMPERSONATING_ENDUSER);
            packingItems.addAll(packRackerImpersonationToken((Racker) user, (ImpersonatedScopeAccess) token));
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
                token = unpackProvisionedUserToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_FEDERATED_USER) {
                token = unpackFederatedUserToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_PROVISIONED_USER_IMPERSONATING_ENDUSER) {
                token = unpackProvisionedUserImpersonationToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_RACKER_IMPERSONATING_ENDUSER) {
                token = unpackRackerImpersonationToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_RACKER) {
                token = unpackRackerToken(webSafeToken, unpacker);
            } else if (type == TOKEN_TYPE_PROVISIONED_USER_DELEGATE) {
                token = unpackDelegationToken(webSafeToken, unpacker);
            } else {
                throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_SCHEME, String.format("Unrecognized data packing scheme '%s'", type));
            }

            // End reading
            unpacker.readArrayEnd();

        } catch (ErrorCodeIdmException e) {
            throw e; //just rethrow. No transformation required
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
     * @deprecated Delegation agreements/tokens are not in use and shouldn't be put into use
     *
     * @param user
     * @param scopeAccess
     * @return
     */
    @Deprecated
    private List<Object> packDelegateToken(ProvisionedUserDelegate user, UserScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getId().equals(scopeAccess.getUserRsId()), "Token userId must match user userId");

        List<Object> packingItems = new ArrayList<Object>();
        packingItems.add(TOKEN_TYPE_PROVISIONED_USER_DELEGATE);

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

        // Delegation
        packingItems.add(scopeAccess.getDelegationAgreementId());

        return packingItems;
    }

    /**
     * @deprecated Delegation agreements/tokens are not in use and shouldn't be put into use
     * @return
     */
    private ScopeAccess unpackDelegationToken(String webSafeToken, Unpacker unpacker) throws IOException {
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

            // Delegation
            scopeAccess.setDelegationAgreementId(safeRead(unpacker, String.class));

            // DN
            scopeAccess.setUniqueId(TokenDNCalculator.calculateProvisionedUserTokenDN(scopeAccess.getUserRsId(), webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private List<Object> packProvisionedUserToken(User user, UserScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getId().equals(scopeAccess.getUserRsId()), "Token userId must match user userId");

        List<Object> packingItems = new ArrayList<Object>();

        // Whether or not to store domain in token. Store locally to ensure same value throughout method
        boolean shouldWriteDomainTokens = identityConfig.getRepositoryConfig().shouldWriteDomainTokens();

        //packed version
        if (shouldWriteDomainTokens) {
            packingItems.add(VERSION_1);
        } else {
            packingItems.add(VERSION_0);
        }

        // Timestamps
        packingItems.add(scopeAccess.getAccessTokenExp().getTime());
        packingItems.add(scopeAccess.getCreateTimestamp().getTime());

        // ScopeAccess
        packingItems.add(compressAuthenticatedBy(scopeAccess.getAuthenticatedBy()));
        packingItems.add(compressScope(scopeAccess.getScope()));

        // UserScopeAccess
        packingItems.add(scopeAccess.getUserRsId());

        // Version 1 writes domain info at end
        if (shouldWriteDomainTokens) {
            packingItems.add(scopeAccess.getAuthenticationDomainId());
        }

        return packingItems;
    }

    private ScopeAccess unpackProvisionedUserToken(String webSafeToken, Unpacker unpacker) throws IOException {
        UserScopeAccess scopeAccess = new UserScopeAccess();

        //packed version format (for future use...)
        byte version = unpacker.readByte();
        if (version == VERSION_0 || (version == VERSION_1 && identityConfig.getRepositoryConfig().shouldReadDomainTokens())) {
            // Timestamps
            scopeAccess.setAccessTokenExp(new Date(unpacker.readLong()));
            scopeAccess.setCreateTimestamp(new Date(unpacker.readLong()));

            // ScopeAccess
            scopeAccess.setAuthenticatedBy(decompressAuthenticatedBy(safeRead(unpacker, Integer[].class)));
            scopeAccess.setScope(decompressScope(safeRead(unpacker, Integer.class)));

            // UserScopeAccess
            scopeAccess.setUserRsId(safeRead(unpacker, String.class));

            // DN
            scopeAccess.setUniqueId(TokenDNCalculator.calculateProvisionedUserTokenDN(scopeAccess.getUserRsId(), webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());

            if (version == VERSION_1) {
                scopeAccess.setAuthenticationDomainId(safeRead(unpacker, String.class));
            }
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private List<Object> packFederatedUserToken(FederatedUser user, UserScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getId().equals(scopeAccess.getUserRsId()), "Token userId must match user userId");

        List<Object> packingItems = new ArrayList<Object>();

        // Whether or not to store domain in token. Store locally to ensure same value throughout method
        boolean shouldWriteDomainTokens = identityConfig.getRepositoryConfig().shouldWriteDomainTokens();

        //packed version
        if (shouldWriteDomainTokens) {
            packingItems.add(VERSION_1);
        } else {
            packingItems.add(VERSION_0);
        }

        // Timestamps
        packingItems.add(scopeAccess.getAccessTokenExp().getTime());
        packingItems.add(scopeAccess.getCreateTimestamp().getTime());

        // ScopeAccess
        packingItems.add(compressAuthenticatedBy(scopeAccess.getAuthenticatedBy()));

        // UserScopeAccess
        packingItems.add(scopeAccess.getUserRsId());

        // Version 1 writes domain info at end
        if (shouldWriteDomainTokens) {
            packingItems.add(scopeAccess.getAuthenticationDomainId());
        }

        return packingItems;
    }

    private ScopeAccess unpackFederatedUserToken(String webSafeToken, Unpacker unpacker) throws IOException {
        UserScopeAccess scopeAccess = new UserScopeAccess();

        //packed version format (for future use...)
        byte version = unpacker.readByte();
        if (version == VERSION_0 || (version == VERSION_1 && identityConfig.getRepositoryConfig().shouldReadDomainTokens())) {
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
            if (user == null) {
                throw new UnmarshallTokenException(ErrorCodes.ERROR_CODE_FEDERATION_USER_NOT_FOUND, String.format("The associated user with id '%s' for the token does not exist", scopeAccess.getUserRsId()));
            }

            IdentityProvider idp = identityProviderRepository.getIdentityProviderByUri(user.getFederatedIdpUri());

            scopeAccess.setUniqueId(TokenDNCalculator.calculateFederatedUserTokenDN(user.getUsername(), idp.getProviderId(), webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());

            if (version == VERSION_1) {
                scopeAccess.setAuthenticationDomainId(safeRead(unpacker, String.class));
            }
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private List<Object> packProvisionedUserImpersonationToken(User user, ImpersonatedScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getId().equals(scopeAccess.getIssuedToUserId()), "specified token not issued to specified user");
        Validate.notNull(scopeAccess.getRsImpersonatingRsId(), "impersonating user required");

        List<Object> packingItems = new ArrayList<Object>();

        // Whether or not to store domain in token. Store locally to ensure same value throughout method
        boolean shouldWriteDomainTokens = identityConfig.getRepositoryConfig().shouldWriteDomainTokens();

        //packed version
        if (shouldWriteDomainTokens) {
            packingItems.add(VERSION_1);
        } else {
            packingItems.add(VERSION_0);
        }

        // Timestamps
        packingItems.add(scopeAccess.getAccessTokenExp().getTime());
        packingItems.add(scopeAccess.getCreateTimestamp().getTime());

        //Auth info
        packingItems.add(compressAuthenticatedBy(scopeAccess.getAuthenticatedBy()));
        packingItems.add(compressScope(scopeAccess.getScope()));

        //user info
        packingItems.add(user.getId());
        packingItems.add(scopeAccess.getRsImpersonatingRsId());

        // Version 1 writes domain info at end
        if (shouldWriteDomainTokens) {
            packingItems.add(scopeAccess.getAuthenticationDomainId());
        }

        return packingItems;
    }

    private ScopeAccess unpackProvisionedUserImpersonationToken(String webSafeToken, Unpacker unpacker) throws IOException {
        ImpersonatedScopeAccess scopeAccess = new ImpersonatedScopeAccess();

        //packed version format (for future use...)
        byte version = unpacker.readByte();
        if (version == VERSION_0 || (version == VERSION_1 && identityConfig.getRepositoryConfig().shouldReadDomainTokens())) {
            // Timestamps
            scopeAccess.setAccessTokenExp(new Date(unpacker.readLong()));
            scopeAccess.setCreateTimestamp(new Date(unpacker.readLong()));

            //Auth info
            scopeAccess.setAuthenticatedBy(decompressAuthenticatedBy(safeRead(unpacker, Integer[].class)));
            scopeAccess.setScope(decompressScope(safeRead(unpacker, Integer.class)));

            //populate impersonator user info
            String impersonatorId = safeRead(unpacker, String.class);
            scopeAccess.setUserRsId(impersonatorId);

            //populate impersonated information
            String impersonatedUserId = safeRead(unpacker, String.class);
            scopeAccess.setRsImpersonatingRsId(impersonatedUserId);

            //generate dynamic ae token for the user being impersonated
            EndUser impersonatedUser = identityUserService.getEndUserById(impersonatedUserId);
            if (impersonatedUser == null) {
                throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_IMPERSONATED_DATA_CONTENTS, String.format("Impersonated user '%s' not found.", scopeAccess.getIssuedToUserId()));
            }
            UserScopeAccess usa = new UserScopeAccess();
            usa.setUserRsId(impersonatedUser.getId());
            usa.getAuthenticatedBy().add(AuthenticatedByMethodEnum.IMPERSONATION.getValue());
            usa.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            aeTokenService.marshallTokenForUser(impersonatedUser, usa);
            scopeAccess.setImpersonatingToken(usa.getAccessTokenString());

            // DN
            scopeAccess.setUniqueId(TokenDNCalculator.calculateProvisionedUserTokenDN(impersonatorId, webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());

            if (version == VERSION_1) {
                scopeAccess.setAuthenticationDomainId(safeRead(unpacker, String.class));
            }
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private List<Object> packRackerImpersonationToken(Racker user, ImpersonatedScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(user.getId(), "user id required");
        Validate.isTrue(user.getId().equals(scopeAccess.getIssuedToUserId()), "specified token not issued to specified user");
        Validate.notNull(scopeAccess.getRsImpersonatingRsId(), "impersonating user required");

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
        packingItems.add(scopeAccess.getRsImpersonatingRsId());

        return packingItems;
    }

    private ScopeAccess unpackRackerImpersonationToken(String webSafeToken, Unpacker unpacker) throws IOException {
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
            scopeAccess.setRackerId(impersonatorId);

            //populate impersonated information
            String impersonatedUserId = safeRead(unpacker, String.class);
            scopeAccess.setRsImpersonatingRsId(impersonatedUserId);

            //generate dynamic ae token for the user being impersonated
            EndUser impersonatedUser = identityUserService.getEndUserById(impersonatedUserId);
            if (impersonatedUser == null) {
                throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_IMPERSONATED_DATA_CONTENTS, String.format("Impersonated user '%s' not found.", scopeAccess.getIssuedToUserId()));
            }
            UserScopeAccess usa = new UserScopeAccess();
            usa.setUserRsId(impersonatedUser.getId());
            usa.getAuthenticatedBy().add(AuthenticatedByMethodEnum.IMPERSONATION.getValue());
            usa.setAccessTokenExp(scopeAccess.getAccessTokenExp());
            aeTokenService.marshallTokenForUser(impersonatedUser, usa);
            scopeAccess.setImpersonatingToken(usa.getAccessTokenString());

            // DN
            scopeAccess.setUniqueId(TokenDNCalculator.calculateRackerTokenDN(impersonatorId, webSafeToken));
            scopeAccess.setClientId(getCloudAuthClientId());

            scopeAccess.setAuthenticationDomainId(RackerScopeAccess.RACKSPACE_DOMAIN);
        } else {
            throw new UnmarshallTokenException(ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION, String.format("Unrecognized data version '%s'", version));
        }
        return scopeAccess;
    }

    private List<Object> packRackerToken(Racker racker, RackerScopeAccess scopeAccess) {
        //validate additional user specific stuff for this packing strategy
        Validate.notNull(racker.getRackerId(), "rackerId required");
        Validate.isTrue(racker.getRackerId().equals(scopeAccess.getRackerId()), "Token rackerId must match racker rackerId");

        List<Object> packingItems = new ArrayList<Object>();

        //packed version format (for future use...)
        packingItems.add(VERSION_0);

        // Timestamps
        packingItems.add(scopeAccess.getAccessTokenExp().getTime());
        packingItems.add(scopeAccess.getCreateTimestamp().getTime());

        // ScopeAccess
        packingItems.add(compressAuthenticatedBy(scopeAccess.getAuthenticatedBy()));
        packingItems.add(compressScope(scopeAccess.getScope()));

        // RackerScopeAccess
        packingItems.add(scopeAccess.getRackerId());

        return packingItems;
    }

    private ScopeAccess unpackRackerToken(String webSafeToken, Unpacker unpacker) throws IOException {
        RackerScopeAccess scopeAccess = new RackerScopeAccess();

        //packed version format (for future use...)
        byte version = unpacker.readByte();
        if (version == VERSION_0) {
            // Timestamps
            scopeAccess.setAccessTokenExp(new Date(unpacker.readLong()));
            scopeAccess.setCreateTimestamp(new Date(unpacker.readLong()));

            // ScopeAccess
            scopeAccess.setAuthenticatedBy(decompressAuthenticatedBy(safeRead(unpacker, Integer[].class)));
            scopeAccess.setScope(decompressScope(safeRead(unpacker, Integer.class)));

            // RackerScopeAccess
            scopeAccess.setRackerId(safeRead(unpacker, String.class));

            // DN
            scopeAccess.setUniqueId(TokenDNCalculator.calculateRackerTokenDN(scopeAccess.getRackerId(), webSafeToken));
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

    private String getCloudAuthClientId() {
        return identityConfig.getStaticConfig().getCloudAuthClientId();
    }
}
