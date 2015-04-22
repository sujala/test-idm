package com.rackspace.idm.domain.config;

import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.domain.security.TokenFormat;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.math.BigInteger;
import java.util.Iterator;

@Component
public class IdentityConfig {

    private static final String LOCALHOST = "localhost";
    public static final String CONFIG_FOLDER_SYS_PROP_NAME = "idm.properties.location";
    public static final String FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_PROP_NAME = "feature.use.reloadable.docs";

    /**
     * Should be provided in seconds
     */
    public static final String RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME = "reloadable.docs.cache.timeout";

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
    private static final String FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME = "feature.aetoken.cleanup.uuid.on.revokes";
    public static final String PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME = "reloadable.properties.ttl.seconds";

    // left as static var to support external reference
    public static final int PROPERTY_RELOADABLE_PROPERTY_TTL_DEFAULT_VALUE = 30;

    /**
     * The property controlling the token format to use for IDPs that do not have an explicit format specified via the
     * override property {@link #IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG}
     */
    public static final String IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP = "feature.federated.provider.defaultTokenFormat";

    /**
     * The format of the property name to set the token format for a specific IDP. The '%s' is replaced by the IDP's labeledUri. This
     * means that each IDP has a custom property. If no such property exists for the IDP, the value for {@link #IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP}
     * is used.
     */
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX = "federated.provider.tokenFormat";
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG = IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX + ".%s";
    private static final String IDENTITY_RACKER_TOKEN_FORMAT =  "feature.racker.defaultTokenFormat";
    private static final String IDENTITY_RACKER_AE_TOKEN_ROLE = "racker.ae.tokens.role";
    private static final String KEYCZAR_DN_CONFIG = "feature.KeyCzarCrypterLocator.ldap.dn";
    public static final String FEATURE_AE_TOKENS_ENCRYPT = "feature.ae.tokens.encrypt";
    public static final String FEATURE_AE_TOKENS_DECRYPT = "feature.ae.tokens.decrypt";

    //OPTIONAL PROPERTIES
    private static final boolean REQUIRED = true;
    private static final boolean OPTIONAL = false;
    private static final String PROPERTY_SET_MESSAGE = "Configuration Property '%s' set with value '%s'";
    private static final String PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is NOT set but is required";
    private static final String INVALID_PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is invalid";
    public static final String FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP = "feature.allow.federated.impersonation";
    public static final String EXPOSE_V11_ADD_BASE_URL_PROP = "feature.v11.add.base.url.exposed";
    public static final String FEATURE_BASE_URL_RESPECT_ENABLED_FLAG = "feature.base.url.respect.enabled.flag";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP = "feature.endpoint.template.type.use.config.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP = "feature.endpoint.template.type.mosso.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP = "feature.endpoint.template.type.nast.mapping";
    public static final String OTP_ISSUER = "feature.otp.issuer";
    public static final String OTP_ENTROPY = "feature.otp.entropy";
    public static final String OTP_CREATE_ENABLED = "feature.otp.create.enabled.flag";
    public static final String FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP = "feature.user.disabled.by.tenants.enabled";
    public static final boolean FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_DEFAULT = false;
    public static final String FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP = "feature.identity.admin.create.subuser.enabled";
    public static final String FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP = "domain.restricted.to.one.user.admin.enabled";

    /**
     * Name of the property that specifies the name of the identity role users are assigned to gain access to MFA during
     * the beta period.
     */
    public static final String MULTIFACTOR_BETA_ROLE_NAME_PROP = "cloudAuth.multiFactorBetaRoleName";
    public static final String MULTIFACTOR_BETA_ENABLED_PROP = "multifactor.beta.enabled";

    public static final String MULTIFACTOR_SERVICES_ENABLED_PROP = "multifactor.services.enabled";
    public static final String BYPASS_DEFAULT_NUMBER = "multifactor.bypass.default.number";
    public static final String BYPASS_MAXIMUM_NUMBER = "multifactor.bypass.maximum.number";
    public static final String FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS = "feature.enable.local.multifactor.bypass";
    public static final boolean FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS_DEFAULT = false;

    public static final String FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_PROP="feature.enable.validate.token.global.role";
    public static final String FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_PROP="feature.enable.get.token.endpoints.global.role";
    public static final String FEATURE_ENABLE_GET_USER_ROLES_GLOBAL_ROLE_PROP="feature.enable.get.user.roles.global.role";
    public static final String FEATURE_ENABLE_GET_USER_GROUPS_GLOBAL_ROLE_PROP="feature.enable.get.user.groups.global.role";
    public static final String FEATURE_ENABLE_IMPLICIT_ROLE_PROP="feature.enable.implicit.roles";
    public static final String IMPLICIT_ROLE_PROP_PREFIX = "implicit.roles";
    public static final String IMPLICIT_ROLE_OVERRIDE_PROP_REG = IMPLICIT_ROLE_PROP_PREFIX + ".%s";
    public static final String FEATURE_RACKER_USERNAME_ON_AUTH_ENABLED_PROP = "feature.racker.username.auth.enabled";

    public static final String FEATURE_MULTIFACTOR_LOCKING_ENABLED_PROP = "feature.multifactor.locking.enabled";
    public static final boolean FEATURE_MULTIFACTOR_LOCKING_ENABLED_DEFAULT = false;
    public static final String FEATURE_MULTIFACTOR_LOCKING_EXPIRATION_PROP = "feature.multifactor.locking.expirationInSeconds";
    public static final int FEATURE_MULTIFACTOR_LOCKING_EXPIRATION_DEFAULT = 1800;
    public static final String FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP = "feature.multifactor.locking.attempts.maximumNumber";
    public static final int FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_DEFAULT = 3;

    @Qualifier("staticConfiguration")
    @Autowired
    private Configuration staticConfiguration;

    @Qualifier("reloadableConfiguration")
    @Autowired
    private Configuration reloadableConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(IdentityConfig.class);
    private final Map<String,Object> propertyDefaults;
    private final StaticConfig staticConfig = new StaticConfig();
    private final ReloadableConfig reloadableConfig = new ReloadableConfig();

    public IdentityConfig() {
        propertyDefaults = setDefaults();
    }

    public IdentityConfig(Configuration staticConfiguration, Configuration reloadableConfiguration) {
        propertyDefaults = setDefaults();
        this.staticConfiguration = staticConfiguration;
        this.reloadableConfiguration = reloadableConfiguration;
    }

    private static final Map<String,Object> setDefaults() {
        Map<String,Object> defaults = new HashMap<String, Object>();
        defaults.put(EMAIL_HOST, LOCALHOST);
        defaults.put(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, true);
        defaults.put(IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID");
        defaults.put(FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME, true);
        defaults.put(PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME, PROPERTY_RELOADABLE_PROPERTY_TTL_DEFAULT_VALUE);
        defaults.put(IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, "UUID");
        defaults.put(IDENTITY_RACKER_TOKEN_FORMAT, "UUID");
        defaults.put(IDENTITY_RACKER_AE_TOKEN_ROLE, "cloud-identity-tokens-ae");
        defaults.put(KEYCZAR_DN_CONFIG, "ou=keystore,o=configuration,dc=rackspace,dc=com");
        defaults.put(FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, false);
        defaults.put(EXPOSE_V11_ADD_BASE_URL_PROP, true);
        defaults.put(FEATURE_BASE_URL_RESPECT_ENABLED_FLAG, false);
        defaults.put(FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP, false);
        defaults.put(OTP_ISSUER, "Rackspace");
        defaults.put(OTP_ENTROPY, 25);
        defaults.put(FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP, FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_DEFAULT);
        defaults.put(FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP, false);
        defaults.put(FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP, false);
        defaults.put(FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_PROP, false);
        defaults.put(FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_PROP, false);
        defaults.put(FEATURE_ENABLE_GET_USER_ROLES_GLOBAL_ROLE_PROP, false);
        defaults.put(FEATURE_ENABLE_GET_USER_GROUPS_GLOBAL_ROLE_PROP, false);
        defaults.put(FEATURE_ENABLE_IMPLICIT_ROLE_PROP, false);
        defaults.put(FEATURE_RACKER_USERNAME_ON_AUTH_ENABLED_PROP, false);
        defaults.put(FEATURE_AE_TOKENS_ENCRYPT, true);
        defaults.put(FEATURE_AE_TOKENS_DECRYPT, true);
        defaults.put(FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_PROP_NAME, true);
        defaults.put(RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME, 60);
        defaults.put(MULTIFACTOR_BETA_ENABLED_PROP, false);
        defaults.put(MULTIFACTOR_SERVICES_ENABLED_PROP, false);
        defaults.put(BYPASS_DEFAULT_NUMBER, BigInteger.ONE);
        defaults.put(BYPASS_MAXIMUM_NUMBER, BigInteger.TEN);
        defaults.put(FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS, FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS_DEFAULT);
        defaults.put(FEATURE_MULTIFACTOR_LOCKING_ENABLED_PROP, FEATURE_MULTIFACTOR_LOCKING_ENABLED_DEFAULT);
        defaults.put(FEATURE_MULTIFACTOR_LOCKING_EXPIRATION_PROP, FEATURE_MULTIFACTOR_LOCKING_EXPIRATION_DEFAULT);
        defaults.put(FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_DEFAULT);
        return defaults;
    }

    public Object getPropertyDefault(String key) {
        return propertyDefaults.get(key);
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
     * To maintain existing application logic, the safe getters continue to return null
     * when a default doesn't exist.
     */

    private Integer getIntSafely(Configuration config, String prop) {
        Object defaultValue = propertyDefaults.get(prop);
        try {
            if (defaultValue == null) {
                return config.getInt(prop);
            } else {
                return config.getInt(prop, (Integer) defaultValue);
            }
        } catch (NumberFormatException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (Integer) defaultValue;
        }
    }

    private BigInteger getBigIntegerSafely(Configuration config, String prop) {
        Object defaultValue = propertyDefaults.get(prop);
        try {
            if (defaultValue == null) {
                return config.getBigInteger(prop);
            } else {
                return config.getBigInteger(prop, (BigInteger) defaultValue);
            }
        } catch (NumberFormatException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (BigInteger) defaultValue;
        }
    }

    private String getStringSafely(Configuration config, String prop) {
        Object defaultValue = propertyDefaults.get(prop);
        try {
            if (defaultValue == null) {
                return config.getString(prop);
            } else {
                return config.getString(prop, (String) defaultValue);
            }
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (String) defaultValue;
        }
    }

    private Boolean getBooleanSafely(Configuration config, String prop) {
        Object defaultValue = propertyDefaults.get(prop);
        try {
            if (defaultValue == null) {
                return config.getBoolean(prop);
            } else {
                return config.getBoolean(prop, (Boolean) defaultValue);
            }
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (Boolean) defaultValue;
        }
    }

    /**
     * Return JSON representation of properties and their values, as annotated by {@link com.rackspace.idm.domain.config.IdmProp}.
     *
     * Uses reflection to discover getters that have been annotated with {@link com.rackspace.idm.domain.config.IdmProp}
     * @return JSONObject properties
     */
    private JSONObject toJSONObject(Object config) {
        final String description = "description";
        final String versionAdded= "versionAdded";
        final String propValue = "value";
        final String defaultValue = "defaultValue";
        JSONObject props = new JSONObject();
        for (Method m : config.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(IdmProp.class)) {
                final IdmProp a = m.getAnnotation(IdmProp.class);
                final String msg = String.format("error getting the value of '%s'", a.key());
                JSONObject prop = new JSONObject();
                try {
                    Object value = m.invoke(config);
                    prop.put(description, a.description());
                    prop.put(versionAdded, a.versionAdded());
                    prop.put(defaultValue, propertyDefaults.get(a.key()));
                    if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                        prop.put(propValue, value);
                    } else if (value instanceof String[] ) {
                        JSONArray valueArray = new JSONArray();
                        for (String val : (String[])value) {
                            valueArray.add(val);
                        }
                        prop.put(propValue, valueArray);
                    } else {
                        prop.put(propValue, value.toString());
                    }
                    props.put(a.key(), prop);
                } catch (Exception e) {
                    logger.error(msg, e);
                }
            }
        }
        return props;
    }

    /**
     * Return serialized JSON representation of IdentityConfig for use by API calls.
     * @return String JSON representation of IdentityConfig
     */
    public String toJSONString() {
        JSONObject props = new JSONObject();
        props.put("configPath", getConfigRoot());
        props.put(PropertyFileConfiguration.CONFIG_FILE_NAME, toJSONObject(staticConfig));
        props.put(PropertyFileConfiguration.RELOADABLE_CONFIG_FILE_NAME, toJSONObject(reloadableConfig));
        return props.toJSONString();
    }

    /**
     * Wrapper around the static configuration properties. Users of these properties may cache the value between requests
     * as the value of these properties will remain constant throughout the lifetime of the running application.
     */
    public class StaticConfig {

        @IdmProp(key = GA_USERNAME, description = "Cloud Identity Admin user", versionAdded = "1.0.14.8")
        public String getGaUsername() {
            return getStringSafely(staticConfiguration, GA_USERNAME);
        }

        @IdmProp(key = EMAIL_FROM_EMAIL_ADDRESS, description = "Return email address to use when sending emails to customers.", versionAdded = "2.5.0")
        public String getEmailFromAddress() {
            return getStringSafely(staticConfiguration, EMAIL_FROM_EMAIL_ADDRESS);
        }

        @IdmProp(key = EMAIL_LOCKED_OUT_SUBJECT, description = "Subject to use when sending MFA locked out email to customer.", versionAdded = "2.5.0")
        public String getEmailLockedOutSubject() {
            return getStringSafely(staticConfiguration, EMAIL_LOCKED_OUT_SUBJECT);
        }

        @IdmProp(key=EMAIL_MFA_ENABLED_SUBJECT, description = "Subject to use when sending MFA enabled email to customer.", versionAdded = "2.5.0")
        public String getEmailMFAEnabledSubject() {
            return getStringSafely(staticConfiguration, EMAIL_MFA_ENABLED_SUBJECT);
        }

        @IdmProp(key=EMAIL_MFA_DISABLED_SUBJECT, description = "Subject to use when sending MFA disabled email to customer.", versionAdded = "2.5.0")
        public String getEmailMFADisabledSubject() {
            return getStringSafely(staticConfiguration, EMAIL_MFA_DISABLED_SUBJECT);
        }

        @IdmProp(key=EMAIL_HOST, description = "Email host to use when sending emails.", versionAdded = "2.5.0")
        public String getEmailHost() {
            return getStringSafely(staticConfiguration, EMAIL_HOST);
        }

        @IdmProp(key=EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, description = "Flag that restricts outgoing emails to only rackspace.com emails. This will prevent any emails from being sent from staging.", versionAdded = "2.5.0")
        public boolean isSendToOnlyRackspaceAddressesEnabled() {
            return getBooleanSafely(staticConfiguration, EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES);
        }

        @IdmProp(key = SCOPED_TOKEN_EXPIRATION_SECONDS, description = "Expiration time for scoped tokens.", versionAdded = "2.9.0")
        public int getScopedTokenExpirationSeconds() {
            return getIntSafely(staticConfiguration, SCOPED_TOKEN_EXPIRATION_SECONDS);
        }

        @IdmProp(key = CLOUD_AUTH_CLIENT_ID, description = "Cloud Identity Application ID.", versionAdded = "1.0.14.8")
        public String getCloudAuthClientId() {
            return getStringSafely(staticConfiguration, CLOUD_AUTH_CLIENT_ID);
        }

        @IdmProp(key = IDENTITY_USER_ADMIN_ROLE_NAME_PROP, description = "User admin role name.", versionAdded = "1.0.14.8")
        public String getIdentityUserAdminRoleName() {
            return getStringSafely(staticConfiguration, IDENTITY_USER_ADMIN_ROLE_NAME_PROP);
        }

        public String[] getIdentityAccessRoleNames() {
            return staticConfiguration.getStringArray(IDENTITY_ACCESS_ROLE_NAMES_PROP);
        }

        @IdmProp(key = IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP, description = "Identity admin role name.", versionAdded = "1.0.14.8")
        public String getIdentityIdentityAdminRoleName() {
            return getStringSafely(staticConfiguration, IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP);
        }

        @IdmProp(key = IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP, description = "Service admin role name (super user).", versionAdded = "1.0.14.8")
        public String getIdentityServiceAdminRoleName() {
            return getStringSafely(staticConfiguration, IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP);
        }

        @IdmProp(key = IDENTITY_DEFAULT_USER_ROLE_NAME_PROP, description = "Default user role name.", versionAdded = "1.0.14.8")
        public String getIdentityDefaultUserRoleName() {
            return getStringSafely(staticConfiguration, IDENTITY_DEFAULT_USER_ROLE_NAME_PROP);
        }

        @IdmProp(key = IDENTITY_USER_MANAGE_ROLE_NAME_PROP, description = "User manager role name.", versionAdded = "1.0.14.8")
        public String getIdentityUserManagerRoleName() {
            return getStringSafely(staticConfiguration, IDENTITY_USER_MANAGE_ROLE_NAME_PROP);
        }

        @IdmProp(key = MULTIFACTOR_BETA_ROLE_NAME_PROP)
        public String getMultiFactorBetaRoleName() {
            return getStringSafely(staticConfiguration, MULTIFACTOR_BETA_ROLE_NAME_PROP);
        }

        @IdmProp(key = MULTIFACTOR_SERVICES_ENABLED_PROP)
        public boolean getMultiFactorServicesEnabled() {
            return getBooleanSafely(staticConfiguration, MULTIFACTOR_SERVICES_ENABLED_PROP);
        }

        @IdmProp(key = MULTIFACTOR_BETA_ENABLED_PROP)
        public boolean getMultiFactorBetaEnabled() {
            return getBooleanSafely(staticConfiguration, MULTIFACTOR_BETA_ENABLED_PROP);
        }

        @IdmProp(key = IDENTITY_PROVISIONED_TOKEN_FORMAT, description = "Defines the default token format for provisioned users tokens.", versionAdded = "2.12.0")
        public TokenFormat getIdentityProvisionedTokenFormat() {
            return convertToTokenFormat(getStringSafely(staticConfiguration, IDENTITY_PROVISIONED_TOKEN_FORMAT));
        }

        @IdmProp(key = IDENTITY_RACKER_TOKEN_FORMAT, description = "Defines the default token format for eDir Racker tokens.", versionAdded = "2.12.0")
        public TokenFormat getIdentityRackerTokenFormat() {
            return convertToTokenFormat(getStringSafely(staticConfiguration, IDENTITY_RACKER_TOKEN_FORMAT));
        }

        @IdmProp(key = IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP)
        public TokenFormat getIdentityFederatedUserDefaultTokenFormat() {
            return convertToTokenFormat(getStringSafely(staticConfiguration, IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP));
        }

        public TokenFormat getIdentityFederatedUserTokenFormatForIdp(String idpLabeledUri) {
            return convertToTokenFormat(staticConfiguration.getString(String.format(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG, idpLabeledUri), "${" + IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP + "}"));
        }

        @IdmProp(key = IDENTITY_RACKER_AE_TOKEN_ROLE)
        public String getIdentityRackerAETokenRole() {
            return getStringSafely(staticConfiguration, IDENTITY_RACKER_AE_TOKEN_ROLE);
        }

        @IdmProp(key = FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME)
        public boolean getFeatureAeTokenCleanupUuidOnRevokes() {
            return getBooleanSafely(staticConfiguration, FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME);
        }

        @IdmProp(key = KEYCZAR_DN_CONFIG)
        public String getKeyCzarDN() {
            return getStringSafely(staticConfiguration, KEYCZAR_DN_CONFIG);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_ENCRYPT)
        public boolean getFeatureAETokensEncrypt() {
            return getBooleanSafely(staticConfiguration, FEATURE_AE_TOKENS_ENCRYPT);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_DECRYPT)
        public boolean getFeatureAETokensDecrypt() {
            return getFeatureAETokensEncrypt() || staticConfiguration.getBoolean(FEATURE_AE_TOKENS_DECRYPT);
        }

        @IdmProp(key = FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP)
        public boolean allowFederatedImpersonation() {
            return getBooleanSafely(staticConfiguration, FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP);
        }

        @IdmProp(key = PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME)
        public int getReloadablePropertiesTTL() {
            return getIntSafely(staticConfiguration, PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME);
        }

        @IdmProp(key = FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_PROP_NAME)
        public boolean useReloadableDocs() {
            return getBooleanSafely(staticConfiguration, FEATURE_USE_RELOADABLE_DOCS_FROM_CONFIG_PROP_NAME);
        }

        @IdmProp(key = RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME)
        public int reloadableDocsTimeOutInSeconds() {
            return getIntSafely(staticConfiguration, RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME);
        }

        @IdmProp(key = EXPOSE_V11_ADD_BASE_URL_PROP)
        public boolean getV11AddBaseUrlExposed() {
            return getBooleanSafely(staticConfiguration, EXPOSE_V11_ADD_BASE_URL_PROP);
        }

        @IdmProp(key = FEATURE_BASE_URL_RESPECT_ENABLED_FLAG)
        public boolean getBaseUlrRespectEnabledFlag() {
            return getBooleanSafely(staticConfiguration, FEATURE_BASE_URL_RESPECT_ENABLED_FLAG);
        }

        @IdmProp(key = FEATURE_ENABLE_IMPLICIT_ROLE_PROP)
        public boolean isImplicitRoleSupportEnabled() {
            return getBooleanSafely(staticConfiguration, FEATURE_ENABLE_IMPLICIT_ROLE_PROP);
        }

        public Set<IdentityRole> getImplicitRolesForRole(String roleName) {
            Set<IdentityRole> result = Collections.EMPTY_SET;

            String[] implicitRolesNames = null;
            if (isImplicitRoleSupportEnabled()) {
                implicitRolesNames = staticConfiguration.getStringArray(String.format(IMPLICIT_ROLE_OVERRIDE_PROP_REG, roleName));
            }

            if (implicitRolesNames != null && implicitRolesNames.length > 0) {
                result = new HashSet<IdentityRole>();
                for (String implicitRoleName : implicitRolesNames) {
                    IdentityRole implicitRole = IdentityRole.fromRoleName(implicitRoleName);
                    if (implicitRole == null) {
                        logger.warn(String.format("Role '%s' has invalid implicit role '%s' configured. Role not found. Ignoring implicit role.", roleName, implicitRoleName));
                    } else {
                        result.add(implicitRole);
                    }
                }
            }

            return result;
        }

        private TokenFormat convertToTokenFormat(String strFormat) {
            for (TokenFormat tokenFormat : TokenFormat.values()) {
                if (tokenFormat.name().equalsIgnoreCase(strFormat)) {
                    return tokenFormat;
                }
            }
            return TokenFormat.UUID;
        }

        @IdmProp(key = OTP_ISSUER)
        public String getOTPIssuer() {
            return getStringSafely(staticConfiguration, OTP_ISSUER);
        }

        @IdmProp(key = FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP)
        public boolean getDomainRestrictedToOneUserAdmin() {
            return getBooleanSafely(staticConfiguration, FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP);
        }

        @IdmProp(key = BYPASS_DEFAULT_NUMBER)
        public BigInteger getBypassDefaultNumber() {
            return getBigIntegerSafely(staticConfiguration, BYPASS_DEFAULT_NUMBER);
        }

        @IdmProp(key = BYPASS_MAXIMUM_NUMBER)
        public BigInteger getBypassMaximumNumber() {
            return getBigIntegerSafely(staticConfiguration, BYPASS_MAXIMUM_NUMBER);
        }

        @IdmProp(key = OTP_CREATE_ENABLED)
        public boolean getOTPCreateEnabled() {
            return getBooleanSafely(staticConfiguration, OTP_CREATE_ENABLED);
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

        @IdmProp(key = FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP)
        public boolean getBaseUrlUseTypeMappingFlag() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP);
        }

        @IdmProp(key = FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP)
        public String[] getBaseUrlMossoTypeMapping() {
            return reloadableConfiguration.getStringArray(FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP);
        }

        @IdmProp(key = FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP)
        public String[] getBaseUrlNastTypeMapping() {
            return reloadableConfiguration.getStringArray(FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP);
        }

        @IdmProp(key = OTP_ENTROPY)
        public int getOTPEntropy() {
            return getIntSafely(reloadableConfiguration, OTP_ENTROPY);
        }

        @IdmProp(key = OTP_CREATE_ENABLED)
        public boolean getOTPCreateEnabled() {
            return getBooleanSafely(reloadableConfiguration, OTP_CREATE_ENABLED);
        }

        @IdmProp(key = FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP)
        public boolean getFeatureUserDisabledByTenantsEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP)
        public boolean getIdentityAdminCreateSubuserEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_PROP)
        public boolean isValidateTokenGlobalRoleEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_PROP)
        public boolean isGetTokenEndpointsGlobalRoleEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_GET_USER_ROLES_GLOBAL_ROLE_PROP)
        public boolean isGetUserRolesGlobalRoleEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_GET_USER_ROLES_GLOBAL_ROLE_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_GET_USER_GROUPS_GLOBAL_ROLE_PROP)
        public boolean isGetUserGroupsGlobalRoleEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_GET_USER_GROUPS_GLOBAL_ROLE_PROP);
        }

        @IdmProp(key = FEATURE_RACKER_USERNAME_ON_AUTH_ENABLED_PROP)
        public boolean getFeatureRackerUsernameOnAuthEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RACKER_USERNAME_ON_AUTH_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS, versionAdded = "2.14.0", description = "enable local multifactor bypass codes")
        public boolean getFeatureLocalMultifactorBypassEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS);
        }

        @IdmProp(key = FEATURE_MULTIFACTOR_LOCKING_ENABLED_PROP, versionAdded = "2.15.0", description = "enable local multifactor locking")
        public boolean getFeatureMultifactorLockingEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_MULTIFACTOR_LOCKING_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_MULTIFACTOR_LOCKING_EXPIRATION_PROP, versionAdded = "2.15.0", description = "local multifactor locking expiration in seconds")
        public int getFeatureMultifactorLockingExpiration() {
            return getIntSafely(reloadableConfiguration, FEATURE_MULTIFACTOR_LOCKING_EXPIRATION_PROP);
        }

        @IdmProp(key = FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, versionAdded = "2.15.0", description = "local multifactor locking maximum number of attempts")
        public int getFeatureMultifactorLockingMax() {
            return getIntSafely(reloadableConfiguration, FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP);
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
