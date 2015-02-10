package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.security.TokenFormat;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Iterator;

@Component
public class IdentityConfig {

    private static final String LOCALHOST = "localhost";

    //REQUIRED PROPERTIES
    private static final String GA_USERNAME = "ga.username";

    private static final String EMAIL_FROM_EMAIL_ADDRESS = "email.return.email.address";
    private static final String EMAIL_MFA_ENABLED_SUBJECT = "email.mfa.enabled.subject";
    private static final String EMAIL_MFA_DISABLED_SUBJECT = "email.mfa.disabled.subject";
    private static final String EMAIL_LOCKED_OUT_SUBJECT = "email.locked.out.email.subject";
    private static final String EMAIL_HOST = "email.host";
    private static final String EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES = "email.send.to.only.rackspace.addresses.enabled";
    private static final String SCOPED_TOKEN_EXPIRATION_SECONDS = "token.scoped.expirationSeconds";
    private static final String CLOUD_AUTH_CLIENT_ID = "cloudAuth.clientId";

    public static final String IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP = "cloudAuth.adminRole";
    public static final String IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP = "cloudAuth.serviceAdminRole";
    public static final String IDENTITY_USER_ADMIN_ROLE_NAME_PROP = "cloudAuth.userAdminRole";
    public static final String IDENTITY_USER_MANAGE_ROLE_NAME_PROP = "cloudAuth.userManagedRole";
    public static final String IDENTITY_DEFAULT_USER_ROLE_NAME_PROP = "cloudAuth.userRole";

    public static final String IDENTITY_PROVISIONED_TOKEN_FORMAT = "feature.provisioned.defaultTokenFormat";
    private static final String IDENTITY_PROVISIONED_TOKEN_FORMAT_DEFAULT = "UUID";

    private static final String FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME = "feature.aetoken.cleanup.uuid.on.revokes";
    private static final boolean FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_DEFAULT_VALUE = true;

    /**
     * The property controlling the token format to use for IDPs that do not have an explicit format specified via the
     * override property {@link #IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG}
     */
    public static final String IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP = "feature.federated.provider.defaultTokenFormat";
    public static final String IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_VALUE = "UUID";

    /**
     * The format of the property name to set the token format for a specific IDP. The '%s' is replaced by the IDP's labeledUri. This
     * means that each IDP has a custom property. If no such property exists for the IDP, the value for {@link #IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP}
     * is used.
     */
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX = "federated.provider.tokenFormat";
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG = IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX + ".%s";

    private static final String IDENTITY_RACKER_TOKEN_FORMAT =  "feature.racker.defaultTokenFormat";
    private static final String IDENTITY_RACKER_TOKEN_FORMAT_DEFAULT =  "UUID";
    private static final String IDENTITY_RACKER_AE_TOKEN_ROLE = "racker.ae.tokens.role";
    private static final String IDENTITY_RACKER_AE_TOKEN_ROLE_DEFAULT = "cloud-identity-tokens-ae";

    private static final String KEYCZAR_DN_CONFIG = "feature.KeyCzarCrypterLocator.ldap.dn";
    private static final String KEYCZAR_DN_DEFAULT = "ou=keystore,o=configuration,dc=rackspace,dc=com";

    public static final String FEATURE_AE_TOKENS_ENCRYPT = "feature.ae.tokens.encrypt";
    public static final String FEATURE_AE_TOKENS_DECRYPT = "feature.ae.tokens.decrypt";

    //OPTIONAL PROPERTIES
    private static final boolean REQUIRED = true;
    private static final boolean OPTIONAL = false;
    private static final String PROPERTY_SET_MESSAGE = "Configuration Property '%s' set with value '%s'";
    private static final String PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is NOT set but is required";

    private static final Logger logger = LoggerFactory.getLogger(IdentityConfig.class);

    public static final String FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP = "feature.allow.federated.impersonation";
    public static final boolean FEATURE_ALLOW_FEDERATED_IMPERSONATION_DEFAULT = false;

    public static final String EXPOSE_V11_ADD_BASE_URL_PROP = "feature.v11.add.base.url.exposed";
    public static final boolean EXPOSE_V11_ADD_BASE_URL_DEFAULT = true;
    public static final String FEATURE_BASE_URL_RESPECT_ENABLED_FLAG = "feature.base.url.respect.enabled.flag";
    public static final boolean FEATURE_BASE_URL_RESPECT_ENABLED_FLAG_DEFAULT = false;

    @Autowired
    private Configuration config;

    @PostConstruct
    private void verifyConfigs() {
        // Verify and Log Required Values
        verifyAndLogProperty(GA_USERNAME, REQUIRED);

        verifyAndLogProperty(EMAIL_FROM_EMAIL_ADDRESS, REQUIRED);
        verifyAndLogProperty(EMAIL_LOCKED_OUT_SUBJECT, REQUIRED);
        verifyAndLogProperty(EMAIL_MFA_ENABLED_SUBJECT, REQUIRED);
        verifyAndLogProperty(EMAIL_MFA_DISABLED_SUBJECT, REQUIRED);
        verifyAndLogProperty(EMAIL_HOST, OPTIONAL);
        verifyAndLogProperty(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, OPTIONAL);
        verifyAndLogProperty(SCOPED_TOKEN_EXPIRATION_SECONDS, REQUIRED);
        verifyAndLogProperty(CLOUD_AUTH_CLIENT_ID, REQUIRED);

        verifyAndLogProperty(IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_USER_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_USER_MANAGE_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_DEFAULT_USER_ROLE_NAME_PROP, REQUIRED);

        verifyAndLogProperty(EXPOSE_V11_ADD_BASE_URL_PROP, OPTIONAL);

        logFederatedTokenFormatOverrides();
    }

    private void verifyAndLogProperty(String property, boolean required) {
        String readProperty = config.getString(property);
        if (required && StringUtils.isBlank(readProperty)) {
            logger.error(String.format(PROPERTY_ERROR_MESSAGE, property));
        } else {
            logger.warn(String.format(PROPERTY_SET_MESSAGE, property, readProperty));
        }
    }

    private void logFederatedTokenFormatOverrides() {
        Iterator<String> fedOverrideUris = config.getKeys(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX);
        while (fedOverrideUris.hasNext()) {
            String fedOverrideProperty = fedOverrideUris.next();
            String fedUri = fedOverrideProperty.substring(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX.length()+1); //add 1 to skip '.'
            TokenFormat tf = getIdentityFederatedUserTokenFormatForIdp(fedUri);
            logger.warn(String.format("Federated Provider Token Format Override: Identity provider '%s' will receive '%s' formatted tokens",fedUri, tf.name()));
        }
    }

    public String getGaUsername() {
        return config.getString(GA_USERNAME);
    }

    public String getEmailFromAddress() {
        return config.getString(EMAIL_FROM_EMAIL_ADDRESS);
    }

    public String getEmailLockedOutSubject() {
        return config.getString(EMAIL_LOCKED_OUT_SUBJECT);
    }

    public String getEmailMFAEnabledSubject() {
        return config.getString(EMAIL_MFA_ENABLED_SUBJECT);
    }

    public String getEmailMFADisabledSubject() {
        return config.getString(EMAIL_MFA_DISABLED_SUBJECT);
    }

    public String getEmailHost() {
        return config.getString(EMAIL_HOST, LOCALHOST);
    }

    public boolean isSendToOnlyRackspaceAddressesEnabled() {
        return config.getBoolean(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, true);
    }

    public int getScopedTokenExpirationSeconds() {
        return config.getInt(SCOPED_TOKEN_EXPIRATION_SECONDS);
    }

    public String getCloudAuthClientId() {
      return config.getString(CLOUD_AUTH_CLIENT_ID);
    }

    public String getIdentityUserAdminRoleName() {
        return config.getString(IDENTITY_USER_ADMIN_ROLE_NAME_PROP);
    }

    public String getIdentityIdentityAdminRoleName() {
        return config.getString(IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP);
    }

    public String getIdentityServiceAdminRoleName() {
        return config.getString(IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP);
    }

    public String getIdentityDefaultUserRoleName() {
        return config.getString(IDENTITY_DEFAULT_USER_ROLE_NAME_PROP);
    }

    public String getIdentityUserManagerRoleName() {
        return config.getString(IDENTITY_USER_MANAGE_ROLE_NAME_PROP);
    }

    public TokenFormat getIdentityProvisionedTokenFormat() {
        return convertToTokenFormat(config.getString(IDENTITY_PROVISIONED_TOKEN_FORMAT, IDENTITY_PROVISIONED_TOKEN_FORMAT_DEFAULT));
    }

    public TokenFormat getIdentityRackerTokenFormat() {
        return convertToTokenFormat(config.getString(IDENTITY_RACKER_TOKEN_FORMAT, IDENTITY_RACKER_TOKEN_FORMAT_DEFAULT));
    }

    public TokenFormat getIdentityFederatedUserDefaultTokenFormat() {
        return convertToTokenFormat(config.getString(IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_VALUE));
    }

    public TokenFormat getIdentityFederatedUserTokenFormatForIdp(String idpLabeledUri) {
        return convertToTokenFormat(config.getString(String.format(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG, idpLabeledUri), "${" + IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP + "}"));
    }

    public String getIdentityRackerAETokenRole() {
        return config.getString(IDENTITY_RACKER_AE_TOKEN_ROLE, IDENTITY_RACKER_AE_TOKEN_ROLE_DEFAULT);
    }

    public boolean getFeatureAeTokenCleanupUuidOnRevokes() {
        return config.getBoolean(FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME, FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_DEFAULT_VALUE);
    }

    public String getKeyCzarDN() {
        return config.getString(KEYCZAR_DN_CONFIG, KEYCZAR_DN_DEFAULT);
    }

    public boolean getFeatureAETokensEncrypt() {
        return config.getBoolean(FEATURE_AE_TOKENS_ENCRYPT, true);
    }

    public boolean getFeatureAETokensDecrypt() {
        return getFeatureAETokensEncrypt() || config.getBoolean(FEATURE_AE_TOKENS_DECRYPT, true);
    }

    public boolean allowFederatedImpersonation() {
        return config.getBoolean(FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, FEATURE_ALLOW_FEDERATED_IMPERSONATION_DEFAULT);
    }

    private TokenFormat convertToTokenFormat(String strFormat) {
        for (TokenFormat tokenFormat : TokenFormat.values()) {
            if (tokenFormat.name().equalsIgnoreCase(strFormat)) {
                return tokenFormat;
            }
        }
        return TokenFormat.UUID;
    }

    public boolean getV11AddBaseUrlExposed() {
        return config.getBoolean(EXPOSE_V11_ADD_BASE_URL_PROP, EXPOSE_V11_ADD_BASE_URL_DEFAULT);
    }

    public boolean getBaseUlrRespectEnabledFlag() {
        return config.getBoolean(FEATURE_BASE_URL_RESPECT_ENABLED_FLAG, FEATURE_BASE_URL_RESPECT_ENABLED_FLAG_DEFAULT);
    }

}
