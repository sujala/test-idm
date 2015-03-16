package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.security.TokenFormat;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Iterator;

@Component
public class IdentityConfig {

    private static final String LOCALHOST = "localhost";

    public static final String CONFIG_FOLDER_SYS_PROP_NAME = "idm.properties.location";

    public static final String FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_PROP_NAME = "feature.use.reloadable.docs";
    public static final boolean FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_DEFAULT_VALUE = true;

    /**
     * Should be provided in seconds
     */
    public static final String RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME = "reloadable.docs.cache.timeout";

    /**
     * In seconds
     */
    public static final int RELOADABLE_DOCS_CACHE_TIMEOUT_DEFAULT_VALUE = 60;

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

    public static final String IDENTITY_ACCESS_ROLE_NAMES_PROP = "cloudAuth.accessRoleNames";
    public static final String IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP = "cloudAuth.adminRole";
    public static final String IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP = "cloudAuth.serviceAdminRole";
    public static final String IDENTITY_USER_ADMIN_ROLE_NAME_PROP = "cloudAuth.userAdminRole";
    public static final String IDENTITY_USER_MANAGE_ROLE_NAME_PROP = "cloudAuth.userManagedRole";
    public static final String IDENTITY_DEFAULT_USER_ROLE_NAME_PROP = "cloudAuth.userRole";

    public static final String IDENTITY_PROVISIONED_TOKEN_FORMAT = "feature.provisioned.defaultTokenFormat";
    private static final String IDENTITY_PROVISIONED_TOKEN_FORMAT_DEFAULT = "UUID";

    private static final String FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME = "feature.aetoken.cleanup.uuid.on.revokes";
    private static final boolean FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_DEFAULT_VALUE = true;

    public static final String PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME = "reloadable.properties.ttl.seconds";
    public static final int PROPERTY_RELOADABLE_PROPERTY_TTL_DEFAULT_VALUE = 30;

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
    private static final String INVALID_PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is invalid";

    private static final Logger logger = LoggerFactory.getLogger(IdentityConfig.class);

    public static final String FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP = "feature.allow.federated.impersonation";
    public static final boolean FEATURE_ALLOW_FEDERATED_IMPERSONATION_DEFAULT = false;

    public static final String EXPOSE_V11_ADD_BASE_URL_PROP = "feature.v11.add.base.url.exposed";
    public static final boolean EXPOSE_V11_ADD_BASE_URL_DEFAULT = true;
    public static final String FEATURE_BASE_URL_RESPECT_ENABLED_FLAG = "feature.base.url.respect.enabled.flag";
    public static final boolean FEATURE_BASE_URL_RESPECT_ENABLED_FLAG_DEFAULT = false;

    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP = "feature.endpoint.template.type.use.config.mapping";
    public static final boolean FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_DEFAULT = false;
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP = "feature.endpoint.template.type.mosso.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP = "feature.endpoint.template.type.nast.mapping";

    public static final String OTP_ISSUER = "feature.otp.issuer";
    public static final String OTP_ISSUER_DEFAULT = "Rackspace";

    public static final String OTP_ENTROPY = "feature.otp.entropy";
    public static final int OTP_ENTROPY_DEFAULT = 25;
    public static final String OTP_CREATE_ENABLED = "feature.otp.create.enabled.flag";
    public static final boolean OTP_CREATE_ENABLED_DEFAULT = false;

    public static final String FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP = "feature.user.disabled.by.tenants.enabled";
    public static final boolean FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_DEFAULT = false;

    public static final String FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP = "feature.identity.admin.create.subuser.enabled";
    public static final boolean FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_DEFUALT = false;

    public static final String FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP = "domain.restricted.to.one.user.admin.enabled";
    public static final boolean FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_DEFAULT = false;

    public static final String MULTIFACTOR_BETA_ROLE_NAME_PROP = "cloudAuth.multiFactorBetaRoleName";

    public static final String FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_PROP="feature.enable.validate.token.global.role";
    public static final boolean FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_DEFAULT=false;

    public static final String FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_PROP="feature.enable.get.token.endpoints.global.role";
    public static final boolean FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_DEFAULT=false;

    @Qualifier("staticConfiguration")
    @Autowired
    private Configuration staticConfiguration;

    @Qualifier("reloadableConfiguration")
    @Autowired
    private Configuration reloadableConfiguration;

    private StaticConfig staticConfig = new StaticConfig();

    private ReloadableConfig reloadableConfig = new ReloadableConfig();

    public IdentityConfig() {
    }

    public IdentityConfig(Configuration staticConfiguration, Configuration reloadableConfiguration) {
        this.staticConfiguration = staticConfiguration;
        this.reloadableConfiguration = reloadableConfiguration;
    }

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

        verifyAndLogProperty(IDENTITY_ACCESS_ROLE_NAMES_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_USER_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_USER_MANAGE_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_DEFAULT_USER_ROLE_NAME_PROP, REQUIRED);

        verifyAndLogProperty(EXPOSE_V11_ADD_BASE_URL_PROP, OPTIONAL);

        logFederatedTokenFormatOverrides();
    }

    private void verifyAndLogProperty(String property, boolean required) {
        String readProperty = staticConfiguration.getString(property);
        if (required && StringUtils.isBlank(readProperty)) {
            logger.error(String.format(PROPERTY_ERROR_MESSAGE, property));
        } else {
            logger.warn(String.format(PROPERTY_SET_MESSAGE, property, readProperty));
        }
    }

    private void logFederatedTokenFormatOverrides() {
        Iterator<String> fedOverrideUris = staticConfiguration.getKeys(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX);
        while (fedOverrideUris.hasNext()) {
            String fedOverrideProperty = fedOverrideUris.next();
            String fedUri = fedOverrideProperty.substring(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX.length()+1); //add 1 to skip '.'
            TokenFormat tf = getStaticConfig().getIdentityFederatedUserTokenFormatForIdp(fedUri);
            logger.warn(String.format("Federated Provider Token Format Override: Identity provider '%s' will receive '%s' formatted tokens",fedUri, tf.name()));
        }
    }

    public StaticConfig getStaticConfig() {
        return staticConfig;
    }

    public ReloadableConfig getReloadableConfig() {
        return reloadableConfig;
    }

    /**
     * Wrapper around the static configuration properties. Users of these properties may cache the value between requests
     * as the value of these properties will remain constant throughout the lifetime of the running application.
     */
    public class StaticConfig {
        public String getGaUsername() {
            return staticConfiguration.getString(GA_USERNAME);
        }

        public String getEmailFromAddress() {
            return staticConfiguration.getString(EMAIL_FROM_EMAIL_ADDRESS);
        }

        public String getEmailLockedOutSubject() {
            return staticConfiguration.getString(EMAIL_LOCKED_OUT_SUBJECT);
        }

        public String getEmailMFAEnabledSubject() {
            return staticConfiguration.getString(EMAIL_MFA_ENABLED_SUBJECT);
        }

        public String getEmailMFADisabledSubject() {
            return staticConfiguration.getString(EMAIL_MFA_DISABLED_SUBJECT);
        }

        public String getEmailHost() {
            return staticConfiguration.getString(EMAIL_HOST, LOCALHOST);
        }

        public boolean isSendToOnlyRackspaceAddressesEnabled() {
            return staticConfiguration.getBoolean(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, true);
        }

        public int getScopedTokenExpirationSeconds() {
            return staticConfiguration.getInt(SCOPED_TOKEN_EXPIRATION_SECONDS);
        }

        public String getCloudAuthClientId() {
            return staticConfiguration.getString(CLOUD_AUTH_CLIENT_ID);
        }

        public String getIdentityUserAdminRoleName() {
            return staticConfiguration.getString(IDENTITY_USER_ADMIN_ROLE_NAME_PROP);
        }

        public String[] getIdentityAccessRoleNames() {
            return staticConfiguration.getStringArray(IDENTITY_ACCESS_ROLE_NAMES_PROP);
        }

        public String getIdentityIdentityAdminRoleName() {
            return staticConfiguration.getString(IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP);
        }

        public String getIdentityServiceAdminRoleName() {
            return staticConfiguration.getString(IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP);
        }

        public String getIdentityDefaultUserRoleName() {
            return staticConfiguration.getString(IDENTITY_DEFAULT_USER_ROLE_NAME_PROP);
        }

        public String getIdentityUserManagerRoleName() {
            return staticConfiguration.getString(IDENTITY_USER_MANAGE_ROLE_NAME_PROP);
        }

        public String getMultiFactorBetaRoleName() {
            return staticConfiguration.getString(MULTIFACTOR_BETA_ROLE_NAME_PROP);
        }

        public TokenFormat getIdentityProvisionedTokenFormat() {
            return convertToTokenFormat(staticConfiguration.getString(IDENTITY_PROVISIONED_TOKEN_FORMAT, IDENTITY_PROVISIONED_TOKEN_FORMAT_DEFAULT));
        }

        public TokenFormat getIdentityRackerTokenFormat() {
            return convertToTokenFormat(staticConfiguration.getString(IDENTITY_RACKER_TOKEN_FORMAT, IDENTITY_RACKER_TOKEN_FORMAT_DEFAULT));
        }

        public TokenFormat getIdentityFederatedUserDefaultTokenFormat() {
            return convertToTokenFormat(staticConfiguration.getString(IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_VALUE));
        }

        public TokenFormat getIdentityFederatedUserTokenFormatForIdp(String idpLabeledUri) {
            return convertToTokenFormat(staticConfiguration.getString(String.format(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG, idpLabeledUri), "${" + IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP + "}"));
        }

        public String getIdentityRackerAETokenRole() {
            return staticConfiguration.getString(IDENTITY_RACKER_AE_TOKEN_ROLE, IDENTITY_RACKER_AE_TOKEN_ROLE_DEFAULT);
        }

        public boolean getFeatureAeTokenCleanupUuidOnRevokes() {
            return staticConfiguration.getBoolean(FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME, FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_DEFAULT_VALUE);
        }

        public String getKeyCzarDN() {
            return staticConfiguration.getString(KEYCZAR_DN_CONFIG, KEYCZAR_DN_DEFAULT);
        }

        public boolean getFeatureAETokensEncrypt() {
            return staticConfiguration.getBoolean(FEATURE_AE_TOKENS_ENCRYPT, true);
        }

        public boolean getFeatureAETokensDecrypt() {
            return getFeatureAETokensEncrypt() || staticConfiguration.getBoolean(FEATURE_AE_TOKENS_DECRYPT, true);
        }

        public boolean allowFederatedImpersonation() {
            return staticConfiguration.getBoolean(FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, FEATURE_ALLOW_FEDERATED_IMPERSONATION_DEFAULT);
        }

        public int getReloadablePropertiesTTL() {
            return staticConfiguration.getInt(PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME, PROPERTY_RELOADABLE_PROPERTY_TTL_DEFAULT_VALUE);
        }

        public boolean useReloadableDocs() {
            return staticConfiguration.getBoolean(FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_PROP_NAME, FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_DEFAULT_VALUE);
        }

        public int reloadableDocsTimeOutInSeconds() {
            return staticConfiguration.getInt(RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME, RELOADABLE_DOCS_CACHE_TIMEOUT_DEFAULT_VALUE);
        }

        public boolean getV11AddBaseUrlExposed() {
            return staticConfiguration.getBoolean(EXPOSE_V11_ADD_BASE_URL_PROP, EXPOSE_V11_ADD_BASE_URL_DEFAULT);
        }

        public boolean getBaseUlrRespectEnabledFlag() {
            return staticConfiguration.getBoolean(FEATURE_BASE_URL_RESPECT_ENABLED_FLAG, FEATURE_BASE_URL_RESPECT_ENABLED_FLAG_DEFAULT);
        }

        private TokenFormat convertToTokenFormat(String strFormat) {
            for (TokenFormat tokenFormat : TokenFormat.values()) {
                if (tokenFormat.name().equalsIgnoreCase(strFormat)) {
                    return tokenFormat;
                }
            }
            return TokenFormat.UUID;
        }

        public String getOTPIssuer() {
            return staticConfiguration.getString(OTP_ISSUER, OTP_ISSUER_DEFAULT);
        }

        public boolean getDomainRestrictedToOneUserAdmin() {
            return staticConfiguration.getBoolean(FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP, FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_DEFAULT);
        }

    }

    /**
     * Wrapper around the reloadable configuration properties. Users of these properties must ensure that they always
     * lookup up the property each time before use and must NOT store the value of the property.
     */
    public class ReloadableConfig {
        public String getTestPing() {
            return reloadableConfiguration.getString("reload.test");
        }

        public boolean getBaseUrlUseTypeMappingFlag() {
            return reloadableConfiguration.getBoolean(FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP, FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_DEFAULT);
        }

        public String[] getBaseUrlMossoTypeMapping() {
            return reloadableConfiguration.getStringArray(FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP);
        }

        public String[] getBaseUrlNastTypeMapping() {
            return reloadableConfiguration.getStringArray(FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP);
        }

        public int getOTPEntropy() {
            try {
                return reloadableConfiguration.getInt(OTP_ENTROPY, OTP_ENTROPY_DEFAULT);
            } catch (NumberFormatException e) {
                logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, OTP_ENTROPY));
                return OTP_ENTROPY_DEFAULT;
            }
        }

        public boolean getOTPCreateEnabled() {
            return getBooleanSafely(OTP_CREATE_ENABLED, OTP_CREATE_ENABLED_DEFAULT);
        }

        public boolean getFeatureUserDisabledByTenantsEnabled() {
            return getBooleanSafely(FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP, FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_DEFAULT);
        }

        public boolean getIdentityAdminCreateSubuserEnabled() {
            return getBooleanSafely(FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP, FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_DEFUALT);
        }

        public boolean isValidateTokenGlobalRoleEnabled() {
            return getBooleanSafely(FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_PROP, FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_DEFAULT);
        }

        public boolean isGetTokenEndpointsGlobalRoleEnabled() {
            return getBooleanSafely(FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_PROP, FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_DEFAULT);
        }

        private boolean getBooleanSafely(String prop, boolean defaultValue) {
            try {
                return reloadableConfiguration.getBoolean(prop, defaultValue);
            } catch (ConversionException e) {
                logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
                return defaultValue;
            }
        }
    }

    @Deprecated
    public String getGaUsername() {
        return getStaticConfig().getGaUsername();
    }

    @Deprecated
    public String getEmailFromAddress() {
        return getStaticConfig().getEmailFromAddress();
    }

    @Deprecated
    public String getEmailLockedOutSubject() {
        return getStaticConfig().getEmailLockedOutSubject();
    }

    @Deprecated
    public String getEmailMFAEnabledSubject() {
        return getStaticConfig().getEmailMFAEnabledSubject();
    }

    @Deprecated
    public String getEmailMFADisabledSubject() {
        return getStaticConfig().getEmailMFADisabledSubject();
    }

    @Deprecated
    public String getEmailHost() {
        return getStaticConfig().getEmailHost();
    }

    @Deprecated
    public boolean isSendToOnlyRackspaceAddressesEnabled() {
        return getStaticConfig().isSendToOnlyRackspaceAddressesEnabled();
    }

    @Deprecated
    public int getScopedTokenExpirationSeconds() {
        return getStaticConfig().getScopedTokenExpirationSeconds();
    }

    @Deprecated
    public String getCloudAuthClientId() {
        return getStaticConfig().getCloudAuthClientId();
    }

    @Deprecated
    public String getIdentityUserAdminRoleName() {
        return getStaticConfig().getIdentityUserAdminRoleName();
    }

    @Deprecated
    public String getIdentityIdentityAdminRoleName() {
        return getStaticConfig().getIdentityIdentityAdminRoleName();
    }

    @Deprecated
    public String getIdentityServiceAdminRoleName() {
        return getStaticConfig().getIdentityServiceAdminRoleName();
    }

    @Deprecated
    public String getIdentityDefaultUserRoleName() {
        return getStaticConfig().getIdentityDefaultUserRoleName();
    }

    @Deprecated
    public String getIdentityUserManagerRoleName() {
        return getStaticConfig().getIdentityUserManagerRoleName();
    }

    @Deprecated
    public TokenFormat getIdentityProvisionedTokenFormat() {
        return getStaticConfig().getIdentityProvisionedTokenFormat();
    }

    @Deprecated
    public TokenFormat getIdentityRackerTokenFormat() {
        return getStaticConfig().getIdentityRackerTokenFormat();
    }

    @Deprecated
    public TokenFormat getIdentityFederatedUserDefaultTokenFormat() {
        return getStaticConfig().getIdentityFederatedUserDefaultTokenFormat();
    }

    @Deprecated
    public TokenFormat getIdentityFederatedUserTokenFormatForIdp(String idpLabeledUri) {
        return getStaticConfig().getIdentityFederatedUserTokenFormatForIdp(idpLabeledUri);
    }

    @Deprecated
    public String getIdentityRackerAETokenRole() {
        return getStaticConfig().getIdentityRackerAETokenRole();
    }

    @Deprecated
    public boolean getFeatureAeTokenCleanupUuidOnRevokes() {
        return getStaticConfig().getFeatureAeTokenCleanupUuidOnRevokes();
    }

    @Deprecated
    public String getKeyCzarDN() {
        return getStaticConfig().getKeyCzarDN();
    }

    @Deprecated
    public boolean getFeatureAETokensEncrypt() {
        return getStaticConfig().getFeatureAETokensEncrypt();
    }

    @Deprecated
    public boolean getFeatureAETokensDecrypt() {
        return getStaticConfig().getFeatureAETokensDecrypt();
    }

    @Deprecated
    public boolean allowFederatedImpersonation() {
        return getStaticConfig().allowFederatedImpersonation();
    }

    @Deprecated
    public boolean useReloadableDocs() {
        return getStaticConfig().useReloadableDocs();
    }

    @Deprecated
    public int reloadableDocsTimeOutInSeconds() {
        return getStaticConfig().reloadableDocsTimeOutInSeconds();
    }

    @Deprecated
    public boolean getV11AddBaseUrlExposed() {
        return getStaticConfig().getV11AddBaseUrlExposed();
    }

    @Deprecated
    public boolean getBaseUlrRespectEnabledFlag() {
        return getStaticConfig().getBaseUlrRespectEnabledFlag();
    }

    public String getConfigRoot() {
        return System.getProperty(CONFIG_FOLDER_SYS_PROP_NAME);
    }
}
