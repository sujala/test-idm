package com.rackspace.idm.domain.config;

import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.security.TokenFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang.ArrayUtils;
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
    private static final String PORT_25 = "25";
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
    private static final String EMAIL_PORT = "email.port";
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
    public static final TokenFormat IDENTITY_PROVISIONED_TOKEN_SQL_OVERRIDE = TokenFormat.AE;
    private static final String FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME = "feature.aetoken.cleanup.uuid.on.revokes";
    private static final boolean FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_SQL_OVERRIDE = false;
    public static final String PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME = "reloadable.properties.ttl.seconds";
    public static final String GROUP_DOMAINID_DEFAULT = "group.domainId.default";
    public static final String TENANT_DOMAINID_DEFAULT = "tenant.domainId.default";
    public static final String IDENTITY_ROLE_TENANT_DEFAULT = "identity.role.tenant.default";
    public static final String ENDPOINT_REGIONID_DEFAULT = "endpoint.regionId.default";

    // left as static var to support external reference
    public static final int PROPERTY_RELOADABLE_PROPERTY_TTL_DEFAULT_VALUE = 30;

    /**
     * The property controlling the token format to use for IDPs that do not have an explicit format specified via the
     * override property {@link #IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG}
     */
    public static final String IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP = "feature.federated.provider.defaultTokenFormat";
    public static final String IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_SQL_OVERRIDE = "feature.federated.provider.defaultTokenFormat";

    /**
     * The format of the property name to set the token format for a specific IDP. The '%s' is replaced by the IDP's labeledUri. This
     * means that each IDP has a custom property. If no such property exists for the IDP, the value for {@link #IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP}
     * is used.
     */
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX = "federated.provider.tokenFormat";
    public static final TokenFormat IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_SQL_OVERRIDE = TokenFormat.AE;
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG = IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX + ".%s";
    private static final String IDENTITY_RACKER_TOKEN_FORMAT =  "feature.racker.defaultTokenFormat";
    private static final TokenFormat IDENTITY_RACKER_TOKEN_SQL_OVERRIDE =  TokenFormat.AE;
    private static final String IDENTITY_RACKER_AE_TOKEN_ROLE = "racker.ae.tokens.role";
    private static final String KEYCZAR_DN_CONFIG = "feature.KeyCzarCrypterLocator.ldap.dn";
    public static final String FEATURE_AE_TOKENS_ENCRYPT = "feature.ae.tokens.encrypt";
    public static final boolean FEATURE_AE_TOKENS_ENCRYPT_SQL_OVERRIDE = true;
    public static final String FEATURE_AE_TOKENS_DECRYPT = "feature.ae.tokens.decrypt";
    public static final boolean FEATURE_AE_TOKENS_DECRYPT_SQL_OVERRIDE = true;

    //OPTIONAL PROPERTIES
    private static final boolean REQUIRED = true;
    private static final boolean OPTIONAL = false;
    private static final String PROPERTY_SET_MESSAGE = "Configuration Property '%s' set with value '%s' in '%s'";
    private static final String PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is NOT set but is required in '%s'";

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
    public static final String MAX_OTP_DEVICE_PER_USER_PROP = "max.otp.device.per.user";
    public static final int MAX_OTP_DEVICE_PER_USER_DEFAULT = 5;

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
    public static final String LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_PROP = "local.multifactor.bypass.num.iterations";
    public static final int LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_DEFAULT = 10000;

    public static final String FEATURE_ENABLE_VALIDATE_TOKEN_GLOBAL_ROLE_PROP="feature.enable.validate.token.global.role";
    public static final String FEATURE_ENABLE_GET_TOKEN_ENDPOINTS_GLOBAL_ROLE_PROP="feature.enable.get.token.endpoints.global.role";
    public static final String FEATURE_ENABLE_GET_USER_ROLES_GLOBAL_ROLE_PROP="feature.enable.get.user.roles.global.role";
    public static final String FEATURE_ENABLE_GET_USER_GROUPS_GLOBAL_ROLE_PROP="feature.enable.get.user.groups.global.role";
    public static final String FEATURE_ENABLE_IMPLICIT_ROLE_PROP="feature.enable.implicit.roles";
    public static final String IMPLICIT_ROLE_PROP_PREFIX = "implicit.roles";
    public static final String IMPLICIT_ROLE_OVERRIDE_PROP_REG = IMPLICIT_ROLE_PROP_PREFIX + ".%s";

    public static final String FEATURE_MULTIFACTOR_LOCKING_ENABLED_PROP = "feature.multifactor.locking.enabled";
    public static final boolean FEATURE_MULTIFACTOR_LOCKING_ENABLED_DEFAULT = false;
    public static final String FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP = "feature.multifactor.locking.login.failure.ttl.in.seconds";
    public static final int FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_DEFAULT = 1800;
    public static final String FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP = "feature.multifactor.locking.attempts.maximumNumber";
    public static final int FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_DEFAULT = 3;

    public static final String FEATURE_DELETE_UNUSED_DUO_PHONES_PROP = "feature.delete.unused.duo.phones";
    public static final boolean FEATURE_DELETE_UNUSED_DUO_PHONES_DEFAULT = true;

    public static final String RELOAD_AE_KEYS_FIXED_DELAY_SECONDS = "ae.auto.reload.keys.in.seconds";
    public static final int RELOAD_AE_KEYS_FIXED_DELAY_SECONDS_DEFAULT_VALUE = 300;

    public static final String FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_PROP = "feature.ae.auto.reload.keys.enabled";
    public static final boolean FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_DEFAULT_VALUE = true;

    public static final String AE_TOKEN_STORAGE_TYPE_PROP = "feature.KeyCzarCrypterLocator.storage";
    public static final AEKeyStorageType AE_TOKEN_STORAGE_TYPE_DEFAULT_VALUE = AEKeyStorageType.FILE;

    public static final String SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME = EncryptedSessionIdReaderWriter.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME;
    public static final String SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_DEFAULT = EncryptedSessionIdReaderWriter.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT;

    public static final String FEATURE_MFA_RETURN_IMMEDIATE_ERROR_WHEN_ACCOUNT_LOCKED_ENABLED_PROP = "feature.mfa.return.immediate.error.when.account.locked";
    public static final boolean FEATURE_MFA_RETURN_IMMEDIATE_ERROR_WHEN_ACCOUNT_LOCKED_ENABLED_DEFAULT_VALUE = true;

    public static final String FEATURE_DIFFERENTIATE_OTP_IN_WWW_AUTH_HEADER_PROP = "feature.differentiate.otp.in.www.auth.header.enabled";
    public static final boolean FEATURE_DIFFERENTIATE_OTP_IN_WWW_AUTH_HEADER_DEFAULT_VALUE = true;
    public static final String AE_NODE_NAME_FOR_SIGNOFF_PROP = "ae.node.name.for.signoff"; //no default

    public static final String FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP = "feature.ae.sync.signoff.enabled";
    public static final boolean FEATURE_AE_SYNC_SIGNOFF_ENABLED = true;

    public static final String FEATURE_TERMINATOR_AUTH_WITH_TENANT_SUPPORT_PROP = "feature.terminator.support.for.auth.with.tenant.enabled";
    public static final boolean FEATURE_TERMINATOR_AUTH_WITH_TENANT_SUPPORT_DEFAULT = true;
    public static final String RACKER_IMPERSONATE_ROLE_NAME_PROP = "racker.impersonate.role";
    public static final String RACKER_IMPERSONATE_ROLE_NAME_DEFAULT = "cloud-identity-impersonate";

    public static final String FEATURE_PERSIST_RACKERS_PROP = "feature.persist.rackers.enabled";
    public static final boolean FEATURE_PERSIST_RACKERS_SQL_OVERRIDE = false;
    public static final boolean FEATURE_PERSIST_RACKERS_DEFAULT = true;

    public static final String FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_PROP = "feature.enforce.delete.domain.rule.must.be.disabled";
    public static final boolean FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_DEFAULT = false;

    public static final String FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP = "feature.support.v3.provisioned.user.tokens";
    public static final boolean FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_DEFAULT = false;

    public static final String FEATURE_CACHE_AE_TOKENS_PROP = "feature.cache.ae.tokens";
    public static final boolean FEATURE_CACHE_AE_TOKENS_DEFAULT = false;

    public static final String CACHED_AE_TOKEN_TTL_SECONDS_PROP = "cached.ae.token.ttl.seconds";
    public static final int CACHED_AE_TOKEN_TTL_SECONDS_DEFAULT = 60;

    public static final String CACHED_AE_TOKEN_CACHE_CONCURRENCY_LEVEL_PROP = "cached.ae.token.cache.concurrency.level";
    public static final int CACHED_AE_TOKEN_CACHE_CONCURRENCY_LEVEL_DEFAULT = 50;

    public static final String CACHED_AE_TOKEN_CACHE_MAX_SIZE_PROP = "cached.ae.token.cache.max.size";
    public static final int CACHED_AE_TOKEN_CACHE_MAX_SIZE_DEFAULT = 10000;

    public static final String CACHED_AE_TOKEN_CACHE_INITIAL_CAPACITY_PROP = "cached.ae.token.cache.initial.capacity";
    public static final int CACHED_AE_TOKEN_CACHE_INITIAL_CAPACITY_DEFAULT = 5000;

    public static final String CACHED_AE_TOKEN_CACHE_RECORD_STATS_PROP = "cached.ae.token.cache.record.stats";
    public static final boolean CACHED_AE_TOKEN_CACHE_RECORD_STATS_DEFAULT = true;

    /**
     * Required static prop
     */
    public static final String ROLE_ID_RACKER_PROP = "cloudAuth.rackerRoleRsId";
    public static final String CLIENT_ID_FOUNDATION_PROP = "idm.clientId";

    /**
     * SQL config properties
     */
    private static final String SQL_DRIVER_CLASS_NAME_PROP = "sql.driverClassName";
    private static final String SQL_URL_PROP = "sql.url";
    private static final String SQL_USERNAME_PROP = "sql.username";
    private static final String SQL_PASSWORD_PROP = "sql.password";
    private static final String SQL_INITIAL_SIZE_PROP = "sql.initialSize";
    private static final int SQL_INITIAL_SIZE_DEFAULT = 2;
    private static final String SQL_MAX_ACTIVE_PROP = "sql.maxActive";
    private static final int SQL_MAX_ACTIVE_DEFAULT = 10;
    private static final String SQL_MAX_IDLE_PROP = "sql.maxIdle";
    private static final int SQL_MAX_IDLE_DEFAULT = 5;
    private static final String SQL_MIN_IDLE_PROP = "sql.minIdle";
    private static final int SQL_MIN_IDLE_DEFAULT = 3;

    /* ************************
     * MIGRATION PROPS
     **************************/
    public static final String FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_PROP = "feature.migration.read.only.mode.enabled";
    public static final Boolean FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_DEFAULT = false;

    public static final String MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP = "handle.migration.change.events.default";
    public static final Boolean MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_DEFAULT = false;
    public static final String MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP = "ignore.migration.change.events.of.type.default";
    public static final List MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_DEFAULT = Collections.EMPTY_LIST;

    public static final String MIGRATION_LISTENER_HANDLES_CHANGE_EVENTS_PROP_PREFIX = "handle.migration.change.events.for.listener";
    public static final String MIGRATION_LISTENER_HANDLES_MIGRATION_CHANGE_EVENTS_PROP_REG = MIGRATION_LISTENER_HANDLES_CHANGE_EVENTS_PROP_PREFIX + ".%s";
    public static final String MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_PREFIX = "ignore.migration.change.events.of.type.for.listener";
    public static final String MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_REG = MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_PREFIX + ".%s";


    /* ************************
     **************************/

    /**
     * SQL debug property
     */
    private static final String SQL_SHOW_SQL_PROP = "sql.showSql";
    private static final Boolean SQL_SHOW_DEFAULT = Boolean.FALSE;

    @Qualifier("staticConfiguration")
    @Autowired
    private Configuration staticConfiguration;

    @Qualifier("reloadableConfiguration")
    @Autowired
    private Configuration reloadableConfiguration;

    @Autowired
    private RepositoryProfileResolver profileResolver;

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
        defaults.put(EMAIL_PORT, PORT_25);
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
        defaults.put(FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_DEFAULT);
        defaults.put(FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_DEFAULT);
        defaults.put(LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_PROP, LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_DEFAULT);
        defaults.put(MAX_OTP_DEVICE_PER_USER_PROP, MAX_OTP_DEVICE_PER_USER_DEFAULT);
        defaults.put(FEATURE_DELETE_UNUSED_DUO_PHONES_PROP, FEATURE_DELETE_UNUSED_DUO_PHONES_DEFAULT);
        defaults.put(RELOAD_AE_KEYS_FIXED_DELAY_SECONDS, RELOAD_AE_KEYS_FIXED_DELAY_SECONDS_DEFAULT_VALUE);
        defaults.put(FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_PROP, FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_DEFAULT_VALUE);
        defaults.put(AE_TOKEN_STORAGE_TYPE_PROP, AE_TOKEN_STORAGE_TYPE_DEFAULT_VALUE);
        defaults.put(SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME, SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_DEFAULT);
        defaults.put(FEATURE_MFA_RETURN_IMMEDIATE_ERROR_WHEN_ACCOUNT_LOCKED_ENABLED_PROP, FEATURE_MFA_RETURN_IMMEDIATE_ERROR_WHEN_ACCOUNT_LOCKED_ENABLED_DEFAULT_VALUE);
        defaults.put(FEATURE_DIFFERENTIATE_OTP_IN_WWW_AUTH_HEADER_PROP, FEATURE_DIFFERENTIATE_OTP_IN_WWW_AUTH_HEADER_DEFAULT_VALUE);
        defaults.put(FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP, FEATURE_AE_SYNC_SIGNOFF_ENABLED);
        defaults.put(FEATURE_TERMINATOR_AUTH_WITH_TENANT_SUPPORT_PROP, FEATURE_TERMINATOR_AUTH_WITH_TENANT_SUPPORT_DEFAULT);
        defaults.put(RACKER_IMPERSONATE_ROLE_NAME_PROP, RACKER_IMPERSONATE_ROLE_NAME_DEFAULT);
        defaults.put(SQL_SHOW_SQL_PROP, SQL_SHOW_DEFAULT);
        defaults.put(FEATURE_PERSIST_RACKERS_PROP, FEATURE_PERSIST_RACKERS_DEFAULT);
        defaults.put(FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_PROP, FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_DEFAULT);
        defaults.put(MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP, MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_DEFAULT);
        defaults.put(MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP, MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_DEFAULT);
        defaults.put(SQL_INITIAL_SIZE_PROP, SQL_INITIAL_SIZE_DEFAULT);
        defaults.put(SQL_MAX_ACTIVE_PROP, SQL_MAX_ACTIVE_DEFAULT);
        defaults.put(SQL_MAX_IDLE_PROP, SQL_MAX_IDLE_DEFAULT);
        defaults.put(SQL_MIN_IDLE_PROP, SQL_MIN_IDLE_DEFAULT);
        defaults.put(FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_PROP, FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_DEFAULT);
        defaults.put(FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_DEFAULT);
        defaults.put(FEATURE_CACHE_AE_TOKENS_PROP, FEATURE_CACHE_AE_TOKENS_DEFAULT);
        defaults.put(CACHED_AE_TOKEN_TTL_SECONDS_PROP, CACHED_AE_TOKEN_TTL_SECONDS_DEFAULT);
        defaults.put(CACHED_AE_TOKEN_CACHE_MAX_SIZE_PROP, CACHED_AE_TOKEN_CACHE_MAX_SIZE_DEFAULT);
        defaults.put(CACHED_AE_TOKEN_CACHE_CONCURRENCY_LEVEL_PROP, CACHED_AE_TOKEN_CACHE_CONCURRENCY_LEVEL_DEFAULT);
        defaults.put(CACHED_AE_TOKEN_CACHE_INITIAL_CAPACITY_PROP, CACHED_AE_TOKEN_CACHE_INITIAL_CAPACITY_DEFAULT);
        defaults.put(CACHED_AE_TOKEN_CACHE_RECORD_STATS_PROP, CACHED_AE_TOKEN_CACHE_RECORD_STATS_DEFAULT);

        return defaults;
    }

    public Object getPropertyDefault(String key) {
        return propertyDefaults.get(key);
    }

    @PostConstruct
    private void verifyConfigs() {
        // Verify and Log Required Values
        verifyAndLogStaticProperty(GA_USERNAME, REQUIRED);

        verifyAndLogStaticProperty(EMAIL_FROM_EMAIL_ADDRESS, REQUIRED);
        verifyAndLogStaticProperty(EMAIL_LOCKED_OUT_SUBJECT, REQUIRED);
        verifyAndLogStaticProperty(EMAIL_MFA_ENABLED_SUBJECT, REQUIRED);
        verifyAndLogStaticProperty(EMAIL_MFA_DISABLED_SUBJECT, REQUIRED);
        verifyAndLogStaticProperty(EMAIL_HOST, OPTIONAL);
        verifyAndLogStaticProperty(EMAIL_PORT, OPTIONAL);
        verifyAndLogStaticProperty(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, OPTIONAL);
        verifyAndLogStaticProperty(SCOPED_TOKEN_EXPIRATION_SECONDS, REQUIRED);
        verifyAndLogStaticProperty(CLOUD_AUTH_CLIENT_ID, REQUIRED);

        verifyAndLogStaticProperty(IDENTITY_ACCESS_ROLE_NAMES_PROP, REQUIRED);
        verifyAndLogStaticProperty(IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogStaticProperty(IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogStaticProperty(IDENTITY_USER_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogStaticProperty(IDENTITY_USER_MANAGE_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogStaticProperty(IDENTITY_DEFAULT_USER_ROLE_NAME_PROP, REQUIRED);

        verifyAndLogStaticProperty(ROLE_ID_RACKER_PROP, REQUIRED);
        verifyAndLogStaticProperty(CLIENT_ID_FOUNDATION_PROP, REQUIRED);

        verifyAndLogStaticProperty(EXPOSE_V11_ADD_BASE_URL_PROP, OPTIONAL);

        verifyAndLogStaticProperty(SQL_DRIVER_CLASS_NAME_PROP, REQUIRED);
        verifyAndLogStaticProperty(SQL_URL_PROP, REQUIRED);

        logFederatedTokenFormatOverrides();

        verifyAndLogReloadableProperty(GROUP_DOMAINID_DEFAULT, REQUIRED);
        verifyAndLogReloadableProperty(TENANT_DOMAINID_DEFAULT, REQUIRED);
        verifyAndLogReloadableProperty(AE_NODE_NAME_FOR_SIGNOFF_PROP, REQUIRED);
        verifyAndLogReloadableProperty(FEATURE_PERSIST_RACKERS_PROP, OPTIONAL);
        verifyAndLogReloadableProperty(IDENTITY_ROLE_TENANT_DEFAULT, REQUIRED);
        verifyAndLogReloadableProperty(ENDPOINT_REGIONID_DEFAULT, REQUIRED);
    }

    private void verifyAndLogStaticProperty(String property, boolean required) {
        String readProperty = staticConfiguration.getString(property);
        if (required && readProperty == null) {
            logger.error(String.format(PROPERTY_ERROR_MESSAGE, property, PropertyFileConfiguration.CONFIG_FILE_NAME));
        } else {
            logger.warn(String.format(PROPERTY_SET_MESSAGE, property, readProperty, PropertyFileConfiguration.CONFIG_FILE_NAME));
        }
    }

    private void verifyAndLogReloadableProperty(String property, boolean required) {
        Object readProperty = reloadableConfiguration.getProperty(property);
        if (required && readProperty == null) {
            logger.error(String.format(PROPERTY_ERROR_MESSAGE, property, PropertyFileConfiguration.RELOADABLE_CONFIG_FILE_NAME));
        } else {
            logger.warn(String.format(PROPERTY_SET_MESSAGE, property, readProperty, PropertyFileConfiguration.RELOADABLE_CONFIG_FILE_NAME));
        }
    }

    private void logFederatedTokenFormatOverrides() {
        Iterator<String> fedOverrideUris = staticConfiguration.getKeys(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX);
        while (fedOverrideUris.hasNext()) {
            String fedOverrideProperty = fedOverrideUris.next();
            String fedUri = fedOverrideProperty.substring(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX.length()+1); //add 1 to skip '.'
            TokenFormat tf = getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(fedUri);
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

    private <T extends Enum<T>> T getEnumSafely(Configuration config, String prop, Class<T> enumType) {
        T defaultValue = enumType.cast(propertyDefaults.get(prop));
        T result;
        try {
            String name = config.getString(prop);
            if (!StringUtils.isBlank(name)) {
                //convert to enum
                result = Enum.valueOf(enumType, name);
            } else {
                result = defaultValue;
            }
        } catch (Exception e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop), e);
            result = defaultValue;
        }
        return result;
    }

    private String[] getStringArraySafely(Configuration config, String prop) {
        String[] defaultValue = (String[]) propertyDefaults.get(prop);
        String[] setVal;
        try {
            if (defaultValue == null) {
                setVal = config.getStringArray(prop);
            } else {
                setVal = config.getStringArray(prop);
                /*
                An empty array is returned when the property is not defined OR if the property exists but does not contain
                any values. Want to use the default ONLY if the property does not exist.
                 */
                if (ArrayUtils.isEmpty(setVal) && !config.containsKey(prop)) {
                    setVal = defaultValue;
                }
            }
            return setVal;
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return defaultValue;
        }
    }

    /**
     * Guaranteed to return a non-null value. Will return an empty list if the parameter is not defined.
     *
     * @param config
     * @param prop
     * @return
     */
    private List getListSafely(Configuration config, String prop) {
        List defaultValue = (List) propertyDefaults.get(prop);
        List setVal;
        try {
            setVal = config.getList(prop);

            if (defaultValue != null && CollectionUtils.isEmpty(setVal) && !config.containsKey(prop)) {
               /*
                An empty list is returned when the property is not defined OR if the property exists but does not contain
                any values. Want to use the default ONLY if the property does not exist.
                 */
               setVal = defaultValue;
            }
            return setVal;
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return defaultValue;
        }
    }

    private Set getSetSafely(Configuration config, String prop) {
        List asList = getListSafely(config, prop);
        return new HashSet(asList);
    }

    private TokenFormat convertToTokenFormat(String strFormat) {
        for (TokenFormat tokenFormat : TokenFormat.values()) {
            if (tokenFormat.name().equalsIgnoreCase(strFormat)) {
                return tokenFormat;
            }
        }
        if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
            return TokenFormat.AE;
        } else {
            return TokenFormat.UUID;
        }
    }

    private ChangeType convertToChangeType(String strFormat) {
        for (ChangeType tokenFormat : ChangeType.values()) {
            if (tokenFormat.name().equalsIgnoreCase(strFormat)) {
                return tokenFormat;
            }
        }
        return null;
    }

    private Set<ChangeType> convertToChangeType(Set<String> strFormats) {
        Set<ChangeType> result = new HashSet<ChangeType>(strFormats.size());
        for (String changeTypeStr : strFormats) {
            ChangeType converted = convertToChangeType(changeTypeStr);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
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
                    prop.put(description, a.description());
                    prop.put(versionAdded, a.versionAdded());

                    Object defaultValueValue = propertyDefaults.get(a.key());
                    Object convertedDefaultValue = valueToAddToJSON(defaultValueValue);
                    prop.put(defaultValue, convertedDefaultValue);

                    Object value = m.invoke(config);
                    Object convertedValue = valueToAddToJSON(value);
                    prop.put(propValue, convertedValue);
                    props.put(a.key(), prop);
                } catch (Exception e) {
                    logger.error(msg, e);
                }
            }
        }
        return props;
    }

    private Object valueToAddToJSON(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        } else if (value instanceof String[] ) {
            JSONArray valueArray = new JSONArray();
            for (String val : (String[])value) {
                valueArray.add(val);
            }
            return valueArray;
        } else if (value instanceof Enum) {
            return ((Enum)value).name();
        } else {
            return value.toString();
        }
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

        @IdmProp(key=EMAIL_PORT, description = "Email port to use when sending emails.", versionAdded = "3.0.0")
        public String getEmailPort() {
            return getStringSafely(staticConfiguration, EMAIL_PORT);
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
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return IDENTITY_PROVISIONED_TOKEN_SQL_OVERRIDE;
            }
            return convertToTokenFormat(getStringSafely(staticConfiguration, IDENTITY_PROVISIONED_TOKEN_FORMAT));
        }

        @IdmProp(key = IDENTITY_RACKER_TOKEN_FORMAT, description = "Defines the default token format for eDir Racker tokens. If racker persistence is disable, is AE", versionAdded = "2.12.0")
        public TokenFormat getIdentityRackerTokenFormat() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return IDENTITY_RACKER_TOKEN_SQL_OVERRIDE;
            }
            if (!reloadableConfig.shouldPersistRacker()) {
                //if we're not persisting rackers, the only viable format is AE
                return TokenFormat.AE;
            }
            return convertToTokenFormat(getStringSafely(staticConfiguration, IDENTITY_RACKER_TOKEN_FORMAT));
        }

        @IdmProp(key = IDENTITY_RACKER_AE_TOKEN_ROLE)
        public String getIdentityRackerAETokenRole() {
            return getStringSafely(staticConfiguration, IDENTITY_RACKER_AE_TOKEN_ROLE);
        }

        @IdmProp(key = FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME)
        public boolean getFeatureAeTokenCleanupUuidOnRevokes() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_SQL_OVERRIDE;
            }
            return getBooleanSafely(staticConfiguration, FEATURE_AETOKEN_CLEANUP_UUID_ON_REVOKES_PROP_NAME);
        }

        @IdmProp(key = KEYCZAR_DN_CONFIG)
        public String getKeyCzarDN() {
            return getStringSafely(staticConfiguration, KEYCZAR_DN_CONFIG);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_ENCRYPT)
        public boolean getFeatureAETokensEncrypt() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return FEATURE_AE_TOKENS_ENCRYPT_SQL_OVERRIDE;
            }
            return getBooleanSafely(staticConfiguration, FEATURE_AE_TOKENS_ENCRYPT);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_DECRYPT)
        public boolean getFeatureAETokensDecrypt() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return FEATURE_AE_TOKENS_DECRYPT_SQL_OVERRIDE;
            }
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

        @IdmProp(key = RELOAD_AE_KEYS_FIXED_DELAY_SECONDS, description = "How often to check for AE key changes and reload if found. This is how long after the last time the check was made completes before checking again.", versionAdded = "2.16.0")
        public int getAEKeysReloadDelay() {
            return getIntSafely(staticConfiguration, RELOAD_AE_KEYS_FIXED_DELAY_SECONDS);
        }
        @IdmProp(key = AE_TOKEN_STORAGE_TYPE_PROP, description = "Whether to load keys from FILE or LDAP", versionAdded = "2.13.0")
        public AEKeyStorageType getAETokenStorageType() {
            return getEnumSafely(staticConfiguration, AE_TOKEN_STORAGE_TYPE_PROP, AEKeyStorageType.class);
        }
        @IdmProp(key = SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME, description = "When FILE is used for AE key storage, where the keys are located", versionAdded = "2.13.0")
        public String getAEFileStorageKeyLocation() {
            return getStringSafely(staticConfiguration, SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME);
        }

        @IdmProp(key = ROLE_ID_RACKER_PROP, description = "The rsid id of the racker role", versionAdded = "1.0.14.8")
        public String getRackerRoleId() {
            return staticConfiguration.getString(ROLE_ID_RACKER_PROP);
        }

        @IdmProp(key = CLIENT_ID_FOUNDATION_PROP, description = "The foundation client id", versionAdded = "1.0.14.8")
        public String getFoundationClientId() {
            return staticConfiguration.getString(CLIENT_ID_FOUNDATION_PROP);
        }

        @IdmProp(key = RACKER_IMPERSONATE_ROLE_NAME_PROP, description = "The group name in EDir to determine whether racker has authorization to impersonate", versionAdded = "2.3.0")
        public String getRackerImpersonateRoleName() {
            return getStringSafely(staticConfiguration, RACKER_IMPERSONATE_ROLE_NAME_PROP);
        }

        @IdmProp(key = SQL_DRIVER_CLASS_NAME_PROP, versionAdded = "3.0.0")
        public String getSqlDriverClassName() {
            return getStringSafely(staticConfiguration, SQL_DRIVER_CLASS_NAME_PROP);
        }

        @IdmProp(key = SQL_URL_PROP, versionAdded = "3.0.0")
        public String getSqlUrl() {
            return getStringSafely(staticConfiguration, SQL_URL_PROP);
        }

        @IdmProp(key = SQL_USERNAME_PROP, versionAdded = "3.0.0")
        public String getSqlUsername() {
            return getStringSafely(staticConfiguration, SQL_USERNAME_PROP);
        }

        @IdmProp(key = SQL_PASSWORD_PROP, versionAdded = "3.0.0")
        public String getSqlPassword() {
            return getStringSafely(staticConfiguration, SQL_PASSWORD_PROP);
        }

        @IdmProp(key = SQL_SHOW_SQL_PROP, versionAdded = "3.0.0")
        public Boolean getSqlShowSql() {
            return getBooleanSafely(staticConfiguration, SQL_SHOW_SQL_PROP);
        }

        @IdmProp(key = SQL_INITIAL_SIZE_PROP, versionAdded = "3.0.0")
        public int getSqlInitialSize() {
            return getIntSafely(staticConfiguration, SQL_INITIAL_SIZE_PROP);
        }

        @IdmProp(key = SQL_MAX_ACTIVE_PROP, versionAdded = "3.0.0")
        public int getSqlMaxActive() {
            return getIntSafely(staticConfiguration, SQL_MAX_ACTIVE_PROP);
        }

        @IdmProp(key = SQL_MAX_IDLE_PROP, versionAdded = "3.0.0")
        public int getSqlMaxIdle() {
            return getIntSafely(staticConfiguration, SQL_MAX_IDLE_PROP);
        }

        @IdmProp(key = SQL_MIN_IDLE_PROP, versionAdded = "3.0.1")
        public int getSqlMinIdle() {
            return getIntSafely(staticConfiguration, SQL_MIN_IDLE_PROP);
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

        @IdmProp(key = FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS, versionAdded = "2.14.0", description = "enable local multifactor bypass codes")
        public boolean getFeatureLocalMultifactorBypassEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_LOCAL_MULTIFACTOR_BYPASS);
        }

        @IdmProp(key = FEATURE_MULTIFACTOR_LOCKING_ENABLED_PROP, versionAdded = "2.15.0", description = "enable local multifactor locking")
        public boolean getFeatureMultifactorLockingEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_MULTIFACTOR_LOCKING_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, versionAdded = "2.15.0", description = "How long, in seconds, after which the last invalid MFA logic attempt will be ignored. This affects when an account will be automatically unlocked when using local locking")
        public int getFeatureMultifactorLoginFailureTtl() {
            return getIntSafely(reloadableConfiguration, FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP);
        }

        @IdmProp(key = FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, versionAdded = "2.15.0", description = "local multifactor locking maximum number of attempts")
        public int getFeatureMultifactorLockingMax() {
            return getIntSafely(reloadableConfiguration, FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP);
        }

        @IdmProp(key = LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_PROP, versionAdded = "2.15.0", description = "Number of hashing iterations to perform before storing bypass codes")
        public int getLocalBypassCodeIterationCount() {
            return getIntSafely(reloadableConfiguration, LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_PROP);
        }

        @IdmProp(key = MAX_OTP_DEVICE_PER_USER_PROP, versionAdded = "2.15.0", description = "Maximum number of OTP devices a user can associate with his/her account")
        public int getMaxOTPDevicesPerUser() {
            return getIntSafely(reloadableConfiguration, MAX_OTP_DEVICE_PER_USER_PROP);
        }

        @IdmProp(key = FEATURE_DELETE_UNUSED_DUO_PHONES_PROP, versionAdded = "2.15.0", description = "Whether or not to delete a Duo phone that is not linked to by any Identity user")
        public boolean getFeatureDeleteUnusedDuoPhones() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_DELETE_UNUSED_DUO_PHONES_PROP);
        }

        @IdmProp(key = FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_PROP, description = "Whether or not to periodically check whether a newer version of AE keys exist and automatically reload", versionAdded = "2.16.0")
        public boolean getAutoReloadOfAEKeys() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_MFA_RETURN_IMMEDIATE_ERROR_WHEN_ACCOUNT_LOCKED_ENABLED_PROP, description = "Whether to throw an error on initial password auth when a user is locally locked. If true, a 401 will be returned; otherwise a sessionId will be returned.", versionAdded = "2.16.0")
        public boolean getThrowErrorOnInitialAuthWhenLocked() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_MFA_RETURN_IMMEDIATE_ERROR_WHEN_ACCOUNT_LOCKED_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_DIFFERENTIATE_OTP_IN_WWW_AUTH_HEADER_PROP, description = "Whether or not to differentiate OTP factor from SMS factor in WWW-Authenticate header on initial login", versionAdded = "2.16.0")
        public boolean differentiateOTPInWWWAuthHeader() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_DIFFERENTIATE_OTP_IN_WWW_AUTH_HEADER_PROP);
        }

        @IdmProp(key = AE_NODE_NAME_FOR_SIGNOFF_PROP, description = "The unique name for this API Node. This is used for both signoff on the AE keys loaded into cache by this node, and to record the node making changes", versionAdded = "2.16.0")
        public String getAENodeNameForSignoff() {
            return reloadableConfiguration.getString(AE_NODE_NAME_FOR_SIGNOFF_PROP); //required property so no default
        }

        @IdmProp(key = FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP, description = "Whether or not to keep the signoff object in sync with the loaded AE Key cache", versionAdded = "2.16.0")
        public boolean getAESyncSignOffEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP);
        }

        public String getNodeName() {
            return getAENodeNameForSignoff();
        }

        @IdmProp(key = FEATURE_TERMINATOR_AUTH_WITH_TENANT_SUPPORT_PROP, description = "Whether or not terminator is active for authentication requests where a tenant is provided", versionAdded = "2.16.0")
        public boolean getTerminatorSupportedForAuthWithToken() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_TERMINATOR_AUTH_WITH_TENANT_SUPPORT_PROP);
        }

        public TokenFormat getIdentityFederationRequestTokenFormatForIdp(String idpLabeledUri) {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_SQL_OVERRIDE;
            }
            return convertToTokenFormat(reloadableConfiguration.getString(String.format(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG, idpLabeledUri), "${" + IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP + "}"));
        }

        @IdmProp(key = IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, description = "When an override property does not exist for a given federated provider, this determines the token format to use for that provide's federated users. AE | UUID", versionAdded = "2.13.0")
        public TokenFormat getIdentityFederatedUserDefaultTokenFormat() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_SQL_OVERRIDE;
            }
            return convertToTokenFormat(getStringSafely(reloadableConfiguration, IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP));
        }

        @IdmProp(key = FEATURE_PERSIST_RACKERS_PROP, description = "Whether shell Racker users are persisted within Identity", versionAdded = "3.0.0")
        public boolean shouldPersistRacker() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return FEATURE_PERSIST_RACKERS_SQL_OVERRIDE;
            }
            return getBooleanSafely(reloadableConfiguration, FEATURE_PERSIST_RACKERS_PROP);
        }

        @IdmProp(key = GROUP_DOMAINID_DEFAULT, description = "Default domain_id when creating a group in sql", versionAdded = "3.0.0")
        public String getGroupDefaultDomainId() {
            return getStringSafely(reloadableConfiguration, GROUP_DOMAINID_DEFAULT);
        }

        @IdmProp(key = TENANT_DOMAINID_DEFAULT, description = "Default domain_id when creating a tenant in sql", versionAdded = "3.0.0")
        public String getTenantDefaultDomainId() {
            return getStringSafely(reloadableConfiguration, TENANT_DOMAINID_DEFAULT);
        }

        @IdmProp(key = ENDPOINT_REGIONID_DEFAULT, description = "Default region_id when creating an endpoint", versionAdded = "3.0.0")
        public String getEndpointDefaultRegionId() {
            return getStringSafely(reloadableConfiguration, ENDPOINT_REGIONID_DEFAULT);
        }

        @IdmProp(key = FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_PROP, description = "Whether entities that can be switched to read-only should be switched", versionAdded = "3.0.0")
        public boolean migrationReadOnlyEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_PROP);
        }

        @IdmProp(key = IDENTITY_ROLE_TENANT_DEFAULT, description = "Identity role default tenant", versionAdded = "3.0.0")
        public String getIdentityRoleDefaultTenant() {
            return getStringSafely(reloadableConfiguration, IDENTITY_ROLE_TENANT_DEFAULT);
        }

        public boolean isMigrationListenerEnabled(String listenerName) {
            return reloadableConfiguration.getBoolean(String.format(MIGRATION_LISTENER_HANDLES_MIGRATION_CHANGE_EVENTS_PROP_REG, listenerName), areMigrationListenersEnabledByDefault());
        }

        public Set<ChangeType> getIgnoredChangeTypesForMigrationListener(String listenerName) {
            String dynamicPropName = String.format(MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_REG, listenerName);
            Set configuredVal = getSetSafely(reloadableConfiguration, dynamicPropName);

            Set<ChangeType> changeTypes = null;
            if (!CollectionUtils.isEmpty(configuredVal) || reloadableConfiguration.containsKey(dynamicPropName)) {
                changeTypes = convertToChangeType(configuredVal);
            } else {
                changeTypes = getDefaultMigrationListenerIgnoredChangeTypes();
            }
            return changeTypes;
        }

        @IdmProp(key = MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP, description = "Whether a migration listener is enabled by default", versionAdded = "3.0.0")
        public boolean areMigrationListenersEnabledByDefault() {
            return getBooleanSafely(reloadableConfiguration, MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP);
        }

        @IdmProp(key = MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP, description = "What change types types migration listeners should ignore by default", versionAdded = "3.0.0")
        public Set<ChangeType> getDefaultMigrationListenerIgnoredChangeTypes() {
            return convertToChangeType(getSetSafely(reloadableConfiguration, MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP));
        }

        @IdmProp(key = FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_PROP, description = "Whether domains must be disabled before they can be deleted", versionAdded = "3.0.0")
        public boolean enforceDomainDeleteRuleMustBeDisabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_PROP);
        }

        @IdmProp(key = FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, description = "Whether v3 provisioned user tokens can be used within v2 services", versionAdded = "3.0.1")
        public boolean supportV3ProvisionedUserTokens() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP);
        }
        @IdmProp(key = FEATURE_CACHE_AE_TOKENS_PROP, versionAdded = "3.0.1")
        public Boolean cacheAETokens() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_CACHE_AE_TOKENS_PROP);
        }

        @IdmProp(key = CACHED_AE_TOKEN_TTL_SECONDS_PROP, versionAdded = "3.0.3", description = "The time an entry will exist in the AE token cache before naturally expiring")
        public int cachedAETokenTTLSeconds() {
            return getIntSafely(reloadableConfiguration, CACHED_AE_TOKEN_TTL_SECONDS_PROP);
        }

        @IdmProp(key = CACHED_AE_TOKEN_CACHE_MAX_SIZE_PROP, versionAdded = "3.0.3", description = "The maximum size of the AE Token cache")
        public int cachedAETokenCacheMaxSize() {
            return getIntSafely(reloadableConfiguration, CACHED_AE_TOKEN_CACHE_MAX_SIZE_PROP);
        }

        @IdmProp(key = CACHED_AE_TOKEN_CACHE_INITIAL_CAPACITY_PROP, versionAdded = "3.0.3", description = "The initial capacity of the AE Token cache. A higher value prevents unnecessary resizing later at the cost of more upfront memory")
        public int cachedAETokenCacheInitialCapacity() {
            return getIntSafely(reloadableConfiguration, CACHED_AE_TOKEN_CACHE_INITIAL_CAPACITY_PROP);
        }

        @IdmProp(key = CACHED_AE_TOKEN_CACHE_CONCURRENCY_LEVEL_PROP, versionAdded = "3.0.3", description = "The concurrency level of the AE Token cache. Should roughly how many threads will attempt to concurrently update the cache.")
        public int cachedAETokenCacheConcurrencyLevel() {
            return getIntSafely(reloadableConfiguration, CACHED_AE_TOKEN_CACHE_CONCURRENCY_LEVEL_PROP);
        }

        @IdmProp(key = CACHED_AE_TOKEN_CACHE_RECORD_STATS_PROP, versionAdded = "3.0.3", description = "Whether the AE Token cache will record stats.")
        public boolean cachedAETokenCacheRecordStats() {
            return getBooleanSafely(reloadableConfiguration, CACHED_AE_TOKEN_CACHE_RECORD_STATS_PROP);
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
