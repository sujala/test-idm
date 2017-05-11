package com.rackspace.idm.domain.config;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter;
import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.service.IdentityPropertyService;
import com.rackspace.idm.exception.MissingRequiredConfigIdmException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class IdentityConfig {

    private static final String LOCALHOST = "localhost";
    private static final String PORT_25 = "25";
    public static final String CONFIG_FOLDER_SYS_PROP_NAME = "idm.properties.location";

    /**
     * Should be provided in seconds
     */
    public static final String RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME = "reloadable.docs.cache.timeout";

    //REQUIRED PROPERTIES
    private static final String GA_USERNAME = "ga.username";
    public static final String EMAIL_FROM_EMAIL_ADDRESS = "email.return.email.address";
    public static final String EMAIL_FROM_EMAIL_ADDRESS_DEFAULT = "no-reply@rackspace.com";

    private static final String EMAIL_MFA_ENABLED_SUBJECT = "email.mfa.enabled.subject";
    private static final String EMAIL_MFA_DISABLED_SUBJECT = "email.mfa.disabled.subject";
    private static final String EMAIL_LOCKED_OUT_SUBJECT = "email.locked.out.email.subject";
    public static final String EMAIL_HOST = "email.host";
    public static final String EMAIL_HOST_DEFAULT = "localhost";
    public static final String EMAIL_PORT = "email.port";
    public static final int EMAIL_PORT_DEFAULT = 25;
    public static final String EMAIL_HOST_USERNAME_PROP = "email.username";
    public static final String EMAIL_HOST_USERNAME_DEFAULT = "";
    public static final String EMAIL_HOST_PASSWORD_PROP = "email.password";
    public static final String EMAIL_HOST_PASSWORD_DEFAULT = "";

    private static final String EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES = "email.send.to.only.rackspace.addresses.enabled";
    private static final String SETUP_MFA_SCOPED_TOKEN_EXPIRATION_SECONDS = "token.scoped.expirationSeconds";
    public static final String FEATURE_FORGOT_PWD_ENABLED_PROP_NAME = "feature.forgot.pwd.enabled";
    private static final boolean FEATURE_FORGOT_PWD_ENABLED_DEFAULT = false;
    public static final String FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME = "token.forgot.password.validity.length";
    private static final int FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_DEFAULT = 600;
    public static final String FORGOT_PWD_VALID_PORTALS_PROP_NAME = "forgot.password.valid.portals";
    private static final List<String> FORGOT_PWD_VALID_PORTALS_DEFAULT = Collections.EMPTY_LIST;

    public static final String FEATURE_UPGRADE_USER_TO_CLOUD_PROP_NAME = "feature.upgrade.user.to.cloud.enabled";
    private static final boolean FEATURE_UPGRADE_USER_TO_CLOUD_DEFAULT = false;
    public static final String UPGRADE_USER_ELIGIBLE_ROLE_PROP = "upgrade.user.to.cloud.eligible.role";

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

    public static final String FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME = "feature.federated.domain.tokenLifetime.max";
    public static final int FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME_DEFAULT = 86400;

    public static final String FEDERATED_RESPONSE_MAX_AGE = "feature.federated.issueInstant.max.age";
    public static final int FEDERATED_RESPONSE_MAX_AGE_DEFAULT = 86400;

    public static final String FEDERATED_RESPONSE_MAX_SKEW = "feature.federated.issueInstant.max.skew";
    public static final int FEDERATED_RESPONSE_MAX_SKEW_DEFAULT = 5;

    /* Federated max number of users in IDP [CIDMDEV-5286:CIDMDEV-5305] */
    public static final String IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_PREFIX = "federated.provider.maxUserCount.per.domain.for.idp";
    public static final String IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_REG = IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_PREFIX + ".%s";
    public static final String IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP = "federated.provider.maxUserCount.per.domain.default";
    public static final int IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT = 1000;

    /**
     * The format of the property name to set the token format for a specific IDP. The '%s' is replaced by the IDP's labeledUri. This
     * means that each IDP has a custom property. If no such property exists for the IDP, the value for {@link #IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP}
     * is used.
     */
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX = "federated.provider.tokenFormat";
    public static final TokenFormat IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_SQL_OVERRIDE = TokenFormat.AE;
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG = IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX + ".%s";
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
    private static final String MISSING_REQUIRED_PROPERTY_ERROR_LOG_MESSAGE = "Configuration Property '%s' is invalid";
    private static final String MISSING_REQUIRED_PROPERTY_ERROR_RESPONSE_MESSAGE = "This service is currently unavailable in Identity.";
    public static final String FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP = "feature.allow.federated.impersonation";
    public static final String EXPOSE_V11_ADD_BASE_URL_PROP = "feature.v11.add.base.url.exposed";
    public static final String FEATURE_BASE_URL_RESPECT_ENABLED_FLAG = "feature.base.url.respect.enabled.flag";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP = "feature.endpoint.template.type.use.config.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP = "feature.endpoint.template.type.mosso.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP = "feature.endpoint.template.type.nast.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_PROP = "feature.endpoint.template.disable.name.type";
    public static final String FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED = "feature.global.endpoints.for.all.roles.enabled";
    public static final boolean FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED_DEFAULT = false;
    public static final boolean FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_DEFAULT = false;
    public static final String OTP_ISSUER = "feature.otp.issuer";
    public static final String OTP_ENTROPY = "feature.otp.entropy";
    public static final String OTP_CREATE_ENABLED = "feature.otp.create.enabled.flag";
    public static final boolean OTP_CREATE_ENABLED_DEFAULT = true;

    public static final String FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP = "feature.user.disabled.by.tenants.enabled";
    public static final boolean FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_DEFAULT = false;
    public static final String FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP = "feature.list.endpoints.for.token.filtered.for.terminator";
    public static final boolean FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_DEFAULT = false;
    public static final String FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP = "domain.restricted.to.one.user.admin.enabled";
    public static final String MAX_OTP_DEVICE_PER_USER_PROP = "max.otp.device.per.user";
    public static final int MAX_OTP_DEVICE_PER_USER_DEFAULT = 5;

    public static final String BYPASS_DEFAULT_NUMBER = "multifactor.bypass.default.number";
    public static final String BYPASS_MAXIMUM_NUMBER = "multifactor.bypass.maximum.number";
    public static final String LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_PROP = "local.multifactor.bypass.num.iterations";
    public static final int LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_DEFAULT = 10000;

    public static final String FEATURE_ENABLE_IMPLICIT_ROLE_PROP="feature.enable.implicit.roles";
    public static final String IMPLICIT_ROLE_PROP_PREFIX = "implicit.roles";
    public static final String IMPLICIT_ROLE_OVERRIDE_PROP_REG = IMPLICIT_ROLE_PROP_PREFIX + ".%s";

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

    public static final String AE_NODE_NAME_FOR_SIGNOFF_PROP = "ae.node.name.for.signoff"; //no default

    public static final String FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP = "feature.ae.sync.signoff.enabled";
    public static final boolean FEATURE_AE_SYNC_SIGNOFF_ENABLED = true;

    public static final String RACKER_IMPERSONATE_ROLE_NAME_PROP = "racker.impersonate.role";
    public static final String RACKER_IMPERSONATE_ROLE_NAME_DEFAULT = "cloud-identity-impersonate";

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

    public static final String FEATURE_SUPPORT_SAML_LOGOUT_PROP = "feature.support.saml.logout";
    public static final boolean FEATURE_SUPPORT_SAML_LOGOUT_DEFAULT = true;

    public static final String FEATURE_SUPPORT_IDENTITY_PROVIDER_MANAGEMENT_PROP = "feature.support.identity.provider.management";
    public static final boolean FEATURE_SUPPORT_IDENTITY_PROVIDER_MANAGEMENT_DEFAULT = true;

    public static final String FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP = "feature.enable.password.policy.services";
    public static final boolean FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_DEFAULT = true;

    public static final String FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP = "feature.enforce.password.policy.expiration";
    public static final boolean FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_DEFAULT = true;

    public static final String FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP = "feature.enforce.password.policy.history";
    public static final boolean FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_DEFAULT = true;

    public static final String FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP = "feature.maintain.password.history";
    public static final boolean FEATURE_MAINTAIN_PASSWORD_HISTORY_DEFAULT = true;


    public static final String PASSWORD_HISTORY_MAX_PROP = "password.history.max";
    public static final int PASSWORD_HISTORY_MAX_DEFAULT = 10;

    public static final String IDP_MAX_SEACH_RESULT_SIZE_PROP = "identity.provider.max.search.result.size";
    public static final int IDP_MAX_SEACH_RESULT_SIZE_DEFAULT = 1000;

    public static final String IDP_POLICY_MAX_KILOBYTE_SIZE_PROP = "identity.provider.policy.max.kilobyte.size";
    public static final int IDP_POLICY_MAX_KILOBYTE_SIZE_DEFAULT = 2;

    public static final String FEDERATED_DELTA_EXPIRATION_SECONDS_PROP = "federated.deltaExpiration.seconds";
    public static final int FEDERATED_DELTA_EXPIRATION_SECONDS_DEFAULT = 43200;

    public static final String FEATURE_FEDERATION_DELETION_MAX_DELAY_PROP = "feature.federation.deletion.max.delay";
    public static final int FEATURE_FEDERATION_DELETION_MAX_DELAY_DEFAULT = 1000;

    public static final String FEATURE_FEDERATION_DELETION_MAX_COUNT_PROP = "feature.federation.deletion.max.count";
    public static final int FEATURE_FEDERATION_DELETION_MAX_COUNT_DEFAULT = 1000;

    public static final String FEATURE_FEDERATION_DELETION_ROLE_PROP = "feature.federation.deletion.role";
    public static final String FEATURE_FEDERATION_DELETION_ROLE_DEFAULT = "identity:purge-federated";

    public static final String FEATURE_FEDERATION_DELETION_TIMEOUT_PROP = "feature.federation.deletion.timeout";
    public static final int FEATURE_FEDERATION_DELETION_TIMEOUT_DEFAULT = 3600000;

    public static final String FEATURE_SUPPORT_V11_LEGACY_PROP = "feature.support.v11.legacy";
    public static final boolean FEATURE_SUPPORT_V11_LEGACY_DEFAULT = false;

    public static final String FEATURE_USE_VELOCITY_FOR_MFA_EMAILS_PROP = "feature.use.velocity.for.mfa.emails";
    public static final boolean FEATURE_USE_VELOCITY_FOR_MFA_EMAILS_DEFAULT = false;

    public static final String FEATURE_LIST_GROUPS_FOR_SELF_PROP = "feature.list.groups.for.self";
    public static final boolean FEATURE_LIST_GROUPS_FOR_SELF_DEFAULT = false;
    public static final String FEATURE_LIST_ENDPOINTS_FOR_OWN_TOKEN_PROP = "feature.list.endpoints.for.own.token";
    public static final boolean FEATURE_LIST_ENDPOINTS_FOR_OWN_TOKEN_DEFAULT = false;

    public static final String FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP = "feature.issue.restricted.token.session.ids";
    public static final boolean FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_DEFAULT = false;

    public static final String SESSION_ID_LIFETIME_PROP = "multifactor.sessionid.lifetime";
    public static final Integer SESSION_ID_LIFETIME_DEFAULT = 5;

    public static final String FEATURE_DELETE_IDENTITY_ROLE_PREVENTION_ENABLED_PROP = "feature.delete.identity.role.prevention.enabled";
    public static final boolean FEATURE_DELETE_IDENTITY_ROLE_PREVENTION_ENABLED_DEFAULT = true;

    public static final String FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_PROP = "feature.prevent.racker.impersonate.api.key.access";
    public static final boolean FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_DEFAULT = false;

    public static final String PURGE_TRRS_MAX_DELAY_PROP = "purge.trrs.max.delay";
    public static final int PURGE_TRRS_MAX_DELAY_DEFAULT = 1000;
    public static final int PURGE_TRRS_DEFAULT_DELAY = 0;

    public static final String PURGE_TRRS_MAX_LIMIT_PROP = "purge.trrs.max.limit";
    public static final int PURGE_TRRS_MAX_LIMIT_DEFAULT = 1000;

    public static final String PURGE_TRRS_OBSOLETE_AFTER_PROP = "purge.trrs.after.lifetime.hours";
    public static final int PURGE_TRRS_OBSOLETE_AFTER_DEFAULT = 25;

    public static final String ATOM_HOPPER_URL_PROP = "atom.hopper.url";
    public static final String ATOM_HOPPER_DATA_CENTER_PROP = "atom.hopper.dataCenter";
    public static final String ATOM_HOPPER_REGION_PROP = "atom.hopper.region";

    public static final String FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP = "feature.return.json.specific.cloud.version";
    public static final boolean FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_DEFAULT = true;

    public static final String FEATURE_REUSE_JAXB_CONTEXT = "feature.reuse.jaxb.context";
    public static final boolean FEATURE_REUSE_JAXB_CONTEXT_DEFAULT = true;

    public static final String MAX_CA_DIRECTORY_PAGE_SIZE_PROP = "max.ca.directory.page.size";
    public static final int MAX_CA_DIRECTORY_PAGE_SIZE_DEFAULT = 1000;

    public static final String LIST_GLOBAL_ROLES_FOR_USER_PRECEDENCE_RESTRICTION_ENABLED_PROP = "feature.list.global.roles.for.user.precedence.restriction.enabled.prop";
    public static final boolean LIST_GLOBAL_ROLES_FOR_USER_PRECEDENCE_RESTRICTION_ENABLED_DEFAULT = true;

    public static final String FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_USAGE_PROP = "feature.restrict.user.manager.list.users.usage";
    public static final boolean FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_USAGE_DEFAULT = true;

    public static final String FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_EMAIL_USAGE_PROP = "feature.restrict.user.manager.list.users.by.email.usage";
    public static final boolean FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_EMAIL_USAGE_DEFAULT = true;

    public static final String FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_NAME_USAGE_PROP = "feature.restrict.user.manager.list.users.by.name.usage";
    public static final boolean FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_NAME_USAGE_DEFAULT = true;

    public static final String FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP = "feature.include.endpoints.based.on.rules";
    public static final boolean FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_DEFAULT = false;

    public static final String AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP = "auto.assign.role.on.domain.tenants.role.name";
    public static final String AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT = "identity:tenant-access";

    public static final String FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_PROP = "feature.restrict.create.user.in.default.domain";
    public static final boolean FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_DEFAULT = true;

    public static final String FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_PROP = "feature.restrict.create.user.in.disabled.domain";
    public static final boolean FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_DEFAULT = true;

    public static final String FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP = "feature.restrict.create.user.in.domain.with.users";
    public static final boolean FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_DEFAULT = true;

    public static final String FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_PROP = "feature.list.support.additional.role.properties";
    public static final boolean FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_DEFAULT = true;

    public static final String FEATURE_POST_IDP_FEED_EVENTS_PROP = "feature.post.idp.feed.events";
    public static final boolean FEATURE_POST_IDP_FEED_EVENTS_DEFAULT = true;

    public static final String FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_PROP = "feature.tenant.id.in.auth.response.v10";
    public static final boolean FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_DEFAULT = true;

    public static final String FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_PROP = "feature.tenant.id.in.auth.response.v11";
    public static final boolean FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_DEFAULT = true;

    public static final String FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_PROP = "feature.v2.federation.validate.origin.issue.instant";
    public static final boolean FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_DEFAULT = true;

    public static final String FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_PROP = "feature.allow.updating.approved.domain.ids.for.idp";
    public static final boolean FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_DEFAULT = true;

    public static final String DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_PROP = "domain.default.session.inactivity.timeout";
    public static final Duration DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_DEFAULT = Duration.parse("PT15M");

    public static final String SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_PROP = "session.inactivity.timeout.max.duration";
    public static final Duration SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_DEFAULT = Duration.parse("PT24H");

    public static final String SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_PROP = "session.inactivity.timeout.min.duration";
    public static final Duration SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_DEFAULT = Duration.parse("PT5M");

    public static final String FEATURE_PERFORMANT_SERVICE_CATALOG_PROP = "feature.performant.service.catalog";
    public static final boolean FEATURE_PERFORMANT_SERVICE_CATALOG_DEFAULT = true;

    public static final String FEATURE_INFER_DEFAULT_TENANT_TYPE_PROP = "feature.infer.default.tenant.type";
    public static final boolean FEATURE_INFER_DEFAULT_TENANT_TYPE_DEFAULT = true;

    public static final String CACHE_CLIENT_ROLES_BY_ID_TTL_PROP = "cache.client.role.by.id.ttl";
    public static final Duration CACHE_CLIENT_ROLES_BY_ID_TTL_DEFAULT = Duration.parse("PT10M");

    public static final String CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP = "cache.client.role.by.id.size";
    public static final int CACHE_CLIENT_ROLES_BY_ID_SIZE_DEFAULT = 200;

    public static final String FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_PROP = "feature.use.cached.client.roles.for.service.catalog";
    public static final boolean FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_DEFAULT = true;

    public static final String FEATURE_USE_CACHED_CLIENT_ROLES_FOR_VALIDATE_PROP = "feature.use.cached.client.roles.for.validate";
    public static final boolean FEATURE_USE_CACHED_CLIENT_ROLES_FOR_VALIDATE_DEFAULT = true;

    public static final String FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_PROP = "feature.force.standard.v2.exceptions.end.user.services";
    public static final boolean FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_DEFAULT = true;

    public static final String MAX_TENANT_TYPE_SIZE_PROP = "max.tenant.type.size";
    public static final int MAX_TENANT_TYPE_SIZE_DEFAULT = 999;

    public static final String FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP = "feature.set.default.tenant.type.on.creation";
    public static final boolean FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_DEFAULT = false;


    /**
     * Required static prop
     */
    public static final String ROLE_ID_RACKER_PROP = "cloudAuth.rackerRoleRsId";
    public static final String CLIENT_ID_FOUNDATION_PROP = "idm.clientId";

    public static final String MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME = "multifactor.key.location";
    public static final String MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT = "/etc/idm/config/keys";

    public static final String NAST_TENANT_PREFIX_PROP = "nast.tenant.prefix";
    public static final String NAST_TENANT_PREFIX_DEFAULT = "MossoCloudFS_";

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

    public static final String FEATURE_MIGRATION_SAVE_DELTA_ASYNC_PROP = "feature.migration.save.async";
    public static final Boolean FEATURE_MIGRATION_SAVE_DELTA_ASYNC_DEFAULT = false;

    public static final String MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_PROP = "handle.migration.change.events.default";
    public static final Boolean MIGRATION_LISTENER_DEFAULT_HANDLES_CHANGE_EVENTS_DEFAULT = false;
    public static final String MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP = "ignore.migration.change.events.of.type.default";
    public static final List MIGRATION_LISTENER_DEFAULT_IGNORES_CHANGE_EVENTS_OF_TYPE_DEFAULT = Collections.EMPTY_LIST;

    public static final String MIGRATION_LISTENER_HANDLES_CHANGE_EVENTS_PROP_PREFIX = "handle.migration.change.events.for.listener";
    public static final String MIGRATION_LISTENER_HANDLES_MIGRATION_CHANGE_EVENTS_PROP_REG = MIGRATION_LISTENER_HANDLES_CHANGE_EVENTS_PROP_PREFIX + ".%s";
    public static final String MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_PREFIX = "ignore.migration.change.events.of.type.for.listener";
    public static final String MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_REG = MIGRATION_LISTENER_IGNORES_CHANGE_EVENTS_OF_TYPE_PROP_PREFIX + ".%s";

    /* ************************
    FEEDS Connection Props. Feed calls are asynchronous so a larger default timeout is acceptable
     ************************** */
    public static final String FEEDS_USE_CONFIGURABLE_HTTPCLIENT_PROP = "feeds.use.configurable.httpclient";
    public static final boolean FEEDS_USE_CONFIGURABLE_HTTPCLIENT_DEFAULT = true;

    public static final String FEEDS_MAX_CONNECTIONS_PROP = "feeds.max.connections";
    public static final int FEEDS_MAX_CONNECTIONS_DEFAULT = 200;

    public static final String FEEDS_MAX_CONNECTIONS_PER_ROUTE_PROP = "feeds.max.connections.per.route";
    public static final int FEEDS_MAX_CONNECTIONS_PER_ROUTE_DEFAULT = 100;

    /**
     * Configures the time to  the connection was established; maximum time of inactivity between two data packets
     */
    public static final String FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_PROP = "feeds.new.connection.socket.timeout.ms";
    public static final int FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_DEFAULT = 20000;

    /**
     * Configures the time waiting for data after the connection was established; maximum time of inactivity between two data packets
     */
    public static final String FEEDS_SOCKET_TIMEOUT_MS_PROP = "feeds.socket.timeout.ms";
    public static final int FEEDS_SOCKET_TIMEOUT_DEFAULT = 20000;

    /**
     * Configures the maximum time to establish the connection with the remote host
     */
    public static final String FEEDS_CONNECTION_TIMEOUT_MS_PROP = "feeds.connection.timeout.ms";
    public static final int FEEDS_CONNECTION_TIMEOUT_MS_DEFAULT = 10000;

    /**
     * Configures the maximum time to wait to receive a connection from the connection pool before timing out
     */
    public static final String FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_PROP = "feeds.connection.request.timeout.ms";
    public static final int FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_DEFAULT = 30000;

    /**
     * How long a connection must be idle before it is checked for validity.
     */
    public static final String FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_PROP = "feeds.on.use.eviction.validate.after.ms";
    public static final int FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_DEFAULT = 10000;

    /* ************************
    MFA MIGRATION PROPS
     **************************/
    public static final String FEATURE_ENABLE_MFA_MIGRATION_SERVICES_PROP = "feature.enable.mfa.migration.services";
    public static final Boolean FEATURE_ENABLE_MFA_MIGRATION_SERVICES_DEFAULT = true;

    /**
     * SQL debug property
     */
    private static final String SQL_SHOW_SQL_PROP = "sql.showSql";
    private static final Boolean SQL_SHOW_DEFAULT = Boolean.FALSE;

    private static final String LDAP_SERVER_LIST_PROP = "ldap.serverList";
    private static final String LDAP_SERVER_USE_SSL_PROP = "ldap.server.useSSL";
    private static final String LDAP_SERVER_BIND_DN_PROP = "ldap.bind.dn";
    private static final String LDAP_SERVER_BIND_PASSWORD_PROP = "ldap.bind.password";

    private static final String LDAP_SERVER_POOL_SIZE_INIT_PROP = "ldap.server.pool.size.init";
    private static final int LDAP_SERVER_POOL_SIZE_INIT_DEFAULT = 1;

    private static final String LDAP_SERVER_POOL_SIZE_MAX_PROP = "ldap.server.pool.size.max";
    private static final int LDAP_SERVER_POOL_SIZE_MAX_DEFAULT = 100;

    private static final String LDAP_SERVER_POOL_AGE_MAX_PROP = "ldap.server.pool.age.max";
    private static final int LDAP_SERVER_POOL_AGE_MAX_DEFAULT = 0;

    private static final String LDAP_SERVER_POOL_CREATE_IF_NECESSARY_PROP = "ldap.server.pool.create.if.necessary";
    private static final boolean LDAP_SERVER_POOL_CREATE_IF_NECESSARY_DEFAULT = true;

    private static final String LDAP_SERVER_POOL_MAX_WAIT_TIME_PROP = "ldap.server.pool.max.wait.time";
    private static final long LDAP_SERVER_POOL_MAX_WAIT_TIME_DEFAULT = 0L;

    private static final String LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_PROP = "ldap.server.pool.health.check.interval";
    private static final long LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_DEFAULT = 60000L;

    private static final String LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_PROP = "ldap.server.pool.check.connection.age.on.release";
    private static final boolean LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_DEFAULT = false;

    private static final String LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_PROP = "ldap.server.pool.allow.concurrent.socketfactory.use";
    private static final boolean LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_DEFAULT = false;

    public static final String FEEDS_DEAMON_EVICTION_ENABLED_PROP = "feeds.daemon.eviction.enabled";
    public static final boolean FEEDS_DEAMON_ENABLED_DEFAULT = false;

    public static final String FEEDS_DAEMON_EVICTION_FREQUENCY_MS_PROP = "feeds.daemon.eviction.frequency.ms";
    public static final int FEEDS_DAEMON_EVICTION_FREQUENCY_MS_DEFAULT = 5000;

    public static final String FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_PROP = "feeds.daemon.eviction.close.idle.after.ms";
    public static final int FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_DEFAULT = 30000;

    public static final String FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_PROP = "feeds.allow.connection.keep.alive";
    public static final boolean FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_DEFAULT = false;

    public static final String FEEDS_CONNECTION_KEEP_ALIVE_MS_PROP = "feeds.connection.keep.alive.ms";
    public static final long FEEDS_CONNECTION_KEEP_ALIVE_MS_DEFAULT = 5000;

    /**
     * Identity Repository Properties
     */
    public static final String FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP = "federation.identity.provider.default.policy";

    @Qualifier("staticConfiguration")
    @Autowired
    private Configuration staticConfiguration;

    @Qualifier("reloadableConfiguration")
    @Autowired
    private Configuration reloadableConfiguration;

    @Autowired
    private RepositoryProfileResolver profileResolver;

    @Lazy
    @Autowired
    private IdentityPropertyService identityPropertyService;

    @Autowired
    private IdentityPropertyValueConverter propertyValueConverter;

    private static final Logger logger = LoggerFactory.getLogger(IdentityConfig.class);
    private final Map<String, Object> propertyDefaults;
    private final Map<String, IdentityPropertyValueType> propertyValueTypes;
    private StaticConfig staticConfig = new StaticConfig();
    private ReloadableConfig reloadableConfig = new ReloadableConfig();
    private RepositoryConfig repositoryConfig = new RepositoryConfig();

    public IdentityConfig() {
        propertyDefaults = setDefaults();
        propertyValueTypes = getIdentityPropertyValueTypes();
    }

    public IdentityConfig(Configuration staticConfiguration, Configuration reloadableConfiguration) {
        propertyDefaults = setDefaults();
        propertyValueTypes = getIdentityPropertyValueTypes();
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
        defaults.put(KEYCZAR_DN_CONFIG, "ou=keystore,o=configuration,dc=rackspace,dc=com");
        defaults.put(FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP, false);
        defaults.put(EXPOSE_V11_ADD_BASE_URL_PROP, true);
        defaults.put(FEATURE_BASE_URL_RESPECT_ENABLED_FLAG, false);
        defaults.put(FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP, false);
        defaults.put(OTP_ISSUER, "Rackspace");
        defaults.put(OTP_ENTROPY, 25);
        defaults.put(FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP, FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_DEFAULT);
        defaults.put(FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP, FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_DEFAULT);
        defaults.put(FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP, false);
        defaults.put(FEATURE_ENABLE_IMPLICIT_ROLE_PROP, false);
        defaults.put(FEATURE_AE_TOKENS_ENCRYPT, true);
        defaults.put(FEATURE_AE_TOKENS_DECRYPT, true);
        defaults.put(RELOADABLE_DOCS_CACHE_TIMEOUT_PROP_NAME, 60);
        defaults.put(BYPASS_DEFAULT_NUMBER, BigInteger.ONE);
        defaults.put(BYPASS_MAXIMUM_NUMBER, BigInteger.TEN);
        defaults.put(FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_DEFAULT);
        defaults.put(FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_DEFAULT);
        defaults.put(LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_PROP, LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_DEFAULT);
        defaults.put(MAX_OTP_DEVICE_PER_USER_PROP, MAX_OTP_DEVICE_PER_USER_DEFAULT);
        defaults.put(FEATURE_DELETE_UNUSED_DUO_PHONES_PROP, FEATURE_DELETE_UNUSED_DUO_PHONES_DEFAULT);
        defaults.put(RELOAD_AE_KEYS_FIXED_DELAY_SECONDS, RELOAD_AE_KEYS_FIXED_DELAY_SECONDS_DEFAULT_VALUE);
        defaults.put(FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_PROP, FEATURE_AUTO_RELOAD_AE_KEYS_ENABLED_DEFAULT_VALUE);
        defaults.put(AE_TOKEN_STORAGE_TYPE_PROP, AE_TOKEN_STORAGE_TYPE_DEFAULT_VALUE);
        defaults.put(SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME, SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_DEFAULT);
        defaults.put(FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP, FEATURE_AE_SYNC_SIGNOFF_ENABLED);
        defaults.put(RACKER_IMPERSONATE_ROLE_NAME_PROP, RACKER_IMPERSONATE_ROLE_NAME_DEFAULT);
        defaults.put(SQL_SHOW_SQL_PROP, SQL_SHOW_DEFAULT);
        defaults.put(FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_PROP, FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_DEFAULT);
        defaults.put(FEATURE_MIGRATION_SAVE_DELTA_ASYNC_PROP, FEATURE_MIGRATION_SAVE_DELTA_ASYNC_DEFAULT);
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
        defaults.put(FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME, FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME_DEFAULT);
        defaults.put(FEDERATED_RESPONSE_MAX_AGE, FEDERATED_RESPONSE_MAX_AGE_DEFAULT);
        defaults.put(FEDERATED_RESPONSE_MAX_SKEW, FEDERATED_RESPONSE_MAX_SKEW_DEFAULT);
        defaults.put(FEATURE_SUPPORT_SAML_LOGOUT_PROP, FEATURE_SUPPORT_SAML_LOGOUT_DEFAULT);
        defaults.put(FEATURE_SUPPORT_IDENTITY_PROVIDER_MANAGEMENT_PROP, FEATURE_SUPPORT_IDENTITY_PROVIDER_MANAGEMENT_DEFAULT);
        defaults.put(IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT);
        defaults.put(IDP_MAX_SEACH_RESULT_SIZE_PROP, IDP_MAX_SEACH_RESULT_SIZE_DEFAULT);
        defaults.put(FEDERATED_DELTA_EXPIRATION_SECONDS_PROP, FEDERATED_DELTA_EXPIRATION_SECONDS_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_MAX_DELAY_PROP, FEATURE_FEDERATION_DELETION_MAX_DELAY_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_MAX_COUNT_PROP, FEATURE_FEDERATION_DELETION_MAX_COUNT_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_ROLE_PROP, FEATURE_FEDERATION_DELETION_ROLE_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_TIMEOUT_PROP, FEATURE_FEDERATION_DELETION_TIMEOUT_DEFAULT);
        defaults.put(FEATURE_SUPPORT_V11_LEGACY_PROP, FEATURE_SUPPORT_V11_LEGACY_DEFAULT);
        defaults.put(FEATURE_ENABLE_MFA_MIGRATION_SERVICES_PROP, FEATURE_ENABLE_MFA_MIGRATION_SERVICES_DEFAULT);
        defaults.put(OTP_CREATE_ENABLED, OTP_CREATE_ENABLED_DEFAULT);

        defaults.put(FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_DEFAULT);
        defaults.put(FEATURE_FORGOT_PWD_ENABLED_PROP_NAME, FEATURE_FORGOT_PWD_ENABLED_DEFAULT);
        defaults.put(FEATURE_UPGRADE_USER_TO_CLOUD_PROP_NAME, FEATURE_UPGRADE_USER_TO_CLOUD_DEFAULT);
        defaults.put(FORGOT_PWD_VALID_PORTALS_PROP_NAME, FORGOT_PWD_VALID_PORTALS_DEFAULT);
        defaults.put(EMAIL_HOST, EMAIL_HOST_DEFAULT);
        defaults.put(EMAIL_PORT, EMAIL_PORT_DEFAULT);
        defaults.put(EMAIL_HOST_USERNAME_PROP, EMAIL_HOST_USERNAME_DEFAULT);
        defaults.put(EMAIL_HOST_PASSWORD_PROP, EMAIL_HOST_PASSWORD_DEFAULT);

        defaults.put(FEATURE_USE_VELOCITY_FOR_MFA_EMAILS_PROP, FEATURE_USE_VELOCITY_FOR_MFA_EMAILS_DEFAULT);
        defaults.put(FEATURE_LIST_GROUPS_FOR_SELF_PROP, FEATURE_LIST_GROUPS_FOR_SELF_DEFAULT);
        defaults.put(FEATURE_LIST_ENDPOINTS_FOR_OWN_TOKEN_PROP, FEATURE_LIST_ENDPOINTS_FOR_OWN_TOKEN_DEFAULT);

        defaults.put(FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_PROP, FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_DEFAULT);

        defaults.put(FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_DEFAULT);
        defaults.put(SESSION_ID_LIFETIME_PROP, SESSION_ID_LIFETIME_DEFAULT);

        defaults.put(MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME, MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT);
        defaults.put(EMAIL_FROM_EMAIL_ADDRESS, EMAIL_FROM_EMAIL_ADDRESS_DEFAULT);

        defaults.put(FEATURE_DELETE_IDENTITY_ROLE_PREVENTION_ENABLED_PROP, FEATURE_DELETE_IDENTITY_ROLE_PREVENTION_ENABLED_DEFAULT);
        defaults.put(PURGE_TRRS_MAX_DELAY_PROP, PURGE_TRRS_MAX_DELAY_DEFAULT);

        defaults.put(PURGE_TRRS_MAX_LIMIT_PROP, PURGE_TRRS_MAX_LIMIT_DEFAULT);
        defaults.put(PURGE_TRRS_OBSOLETE_AFTER_PROP, PURGE_TRRS_OBSOLETE_AFTER_DEFAULT);

        defaults.put(FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP, FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_DEFAULT);
        defaults.put(FEATURE_REUSE_JAXB_CONTEXT, FEATURE_REUSE_JAXB_CONTEXT_DEFAULT);

        defaults.put(MAX_CA_DIRECTORY_PAGE_SIZE_PROP, MAX_CA_DIRECTORY_PAGE_SIZE_DEFAULT);
        defaults.put(LIST_GLOBAL_ROLES_FOR_USER_PRECEDENCE_RESTRICTION_ENABLED_PROP, LIST_GLOBAL_ROLES_FOR_USER_PRECEDENCE_RESTRICTION_ENABLED_DEFAULT);

        defaults.put(FEEDS_MAX_CONNECTIONS_PROP, FEEDS_MAX_CONNECTIONS_DEFAULT);
        defaults.put(FEEDS_MAX_CONNECTIONS_PER_ROUTE_PROP, FEEDS_MAX_CONNECTIONS_PER_ROUTE_DEFAULT);
        defaults.put(FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_PROP, FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_DEFAULT);
        defaults.put(FEEDS_SOCKET_TIMEOUT_MS_PROP, FEEDS_SOCKET_TIMEOUT_DEFAULT);
        defaults.put(FEEDS_CONNECTION_TIMEOUT_MS_PROP, FEEDS_CONNECTION_TIMEOUT_MS_DEFAULT);
        defaults.put(FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_PROP, FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_DEFAULT);
        defaults.put(FEEDS_USE_CONFIGURABLE_HTTPCLIENT_PROP, FEEDS_USE_CONFIGURABLE_HTTPCLIENT_DEFAULT);
        defaults.put(FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_PROP, FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_DEFAULT);

        defaults.put(FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_PROP, FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_DEFAULT);

        defaults.put(LDAP_SERVER_POOL_SIZE_INIT_PROP, LDAP_SERVER_POOL_SIZE_INIT_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_SIZE_MAX_PROP, LDAP_SERVER_POOL_SIZE_MAX_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_AGE_MAX_PROP, LDAP_SERVER_POOL_AGE_MAX_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_CREATE_IF_NECESSARY_PROP, LDAP_SERVER_POOL_CREATE_IF_NECESSARY_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_MAX_WAIT_TIME_PROP, LDAP_SERVER_POOL_MAX_WAIT_TIME_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_PROP, LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_PROP, LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_PROP, LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_DEFAULT);

        defaults.put(FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED, FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED_DEFAULT);

        defaults.put(FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_USAGE_PROP, FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_USAGE_DEFAULT);
        defaults.put(FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_EMAIL_USAGE_PROP, FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_EMAIL_USAGE_DEFAULT);
        defaults.put(FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_NAME_USAGE_PROP, FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_NAME_USAGE_DEFAULT);
        defaults.put(FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_DEFAULT);
        defaults.put(FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_PROP, FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_DEFAULT);
        defaults.put(FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_PROP, FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_DEFAULT);
        defaults.put(FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_DEFAULT);
        defaults.put(FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_PROP, FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_DEFAULT);
        defaults.put(FEATURE_POST_IDP_FEED_EVENTS_PROP, FEATURE_POST_IDP_FEED_EVENTS_DEFAULT);
        defaults.put(FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_PROP, FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_DEFAULT);
        defaults.put(FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_PROP, FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_DEFAULT);

        defaults.put(AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP, AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT);

        defaults.put(IDP_POLICY_MAX_KILOBYTE_SIZE_PROP, IDP_POLICY_MAX_KILOBYTE_SIZE_DEFAULT);
        defaults.put(FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_PROP, FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_DEFAULT);

        defaults.put(FEEDS_DEAMON_EVICTION_ENABLED_PROP, FEEDS_DEAMON_ENABLED_DEFAULT);
        defaults.put(FEEDS_DAEMON_EVICTION_FREQUENCY_MS_PROP, FEEDS_DAEMON_EVICTION_FREQUENCY_MS_DEFAULT);
        defaults.put(FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_PROP, FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_DEFAULT);
        defaults.put(FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_PROP, FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_DEFAULT);
        defaults.put(FEEDS_CONNECTION_KEEP_ALIVE_MS_PROP, FEEDS_CONNECTION_KEEP_ALIVE_MS_DEFAULT);

        defaults.put(FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_PROP, FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_DEFAULT);
        defaults.put(FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, FEATURE_PERFORMANT_SERVICE_CATALOG_DEFAULT);

        defaults.put(DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_PROP, DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_DEFAULT);
        defaults.put(SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_PROP, SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_DEFAULT);
        defaults.put(SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_PROP, SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_DEFAULT);

        defaults.put(FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_PROP, FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_DEFAULT);
        defaults.put(FEATURE_USE_CACHED_CLIENT_ROLES_FOR_VALIDATE_PROP, FEATURE_USE_CACHED_CLIENT_ROLES_FOR_VALIDATE_DEFAULT);
        defaults.put(CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, CACHE_CLIENT_ROLES_BY_ID_TTL_DEFAULT);
        defaults.put(CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, CACHE_CLIENT_ROLES_BY_ID_SIZE_DEFAULT);
        defaults.put(FEATURE_INFER_DEFAULT_TENANT_TYPE_PROP, FEATURE_INFER_DEFAULT_TENANT_TYPE_DEFAULT);

        defaults.put(FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_PROP, FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_DEFAULT);
        defaults.put(MAX_TENANT_TYPE_SIZE_PROP, MAX_TENANT_TYPE_SIZE_DEFAULT);
        defaults.put(FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP, FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_DEFAULT);

        defaults.put(NAST_TENANT_PREFIX_PROP, NAST_TENANT_PREFIX_DEFAULT);

        defaults.put(FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_DEFAULT);
        defaults.put(FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_DEFAULT);
        defaults.put(FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_DEFAULT);
        defaults.put(FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, FEATURE_MAINTAIN_PASSWORD_HISTORY_DEFAULT);
        defaults.put(PASSWORD_HISTORY_MAX_PROP, PASSWORD_HISTORY_MAX_DEFAULT);

        return defaults;
    }

    private static Map<String, IdentityPropertyValueType> getIdentityPropertyValueTypes() {
        Map<String, IdentityPropertyValueType> valueTypes = new HashMap<>();

        // put repository property defaults here

        return valueTypes;
    }

    public Object getPropertyDefault(String key) {
        return propertyDefaults.get(key);
    }

    public IdentityPropertyValueType getPropertyValueType(String propertyName) {
        if (StringUtils.isNotBlank(propertyName) && propertyValueTypes.containsKey(propertyName)) {
            return propertyValueTypes.get(propertyName);
        }

        return null;
    }

    @PostConstruct
    private void verifyConfigs() {
        // Verify and Log Required Values
        verifyAndLogStaticProperty(GA_USERNAME, REQUIRED);

        verifyAndLogStaticProperty(EMAIL_LOCKED_OUT_SUBJECT, REQUIRED);
        verifyAndLogStaticProperty(EMAIL_MFA_ENABLED_SUBJECT, REQUIRED);
        verifyAndLogStaticProperty(EMAIL_MFA_DISABLED_SUBJECT, REQUIRED);
        verifyAndLogStaticProperty(EMAIL_HOST, OPTIONAL);
        verifyAndLogStaticProperty(EMAIL_PORT, OPTIONAL);
        verifyAndLogStaticProperty(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, OPTIONAL);
        verifyAndLogStaticProperty(SETUP_MFA_SCOPED_TOKEN_EXPIRATION_SECONDS, REQUIRED);
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

        verifyAndLogStaticProperty(LDAP_SERVER_LIST_PROP, REQUIRED);
        verifyAndLogStaticProperty(LDAP_SERVER_USE_SSL_PROP, REQUIRED);
        verifyAndLogStaticProperty(LDAP_SERVER_BIND_DN_PROP, REQUIRED);
        verifyRequiredStaticProperty(LDAP_SERVER_BIND_PASSWORD_PROP);


        logFederatedTokenFormatOverrides();

        verifyAndLogReloadableProperty(GROUP_DOMAINID_DEFAULT, REQUIRED);
        verifyAndLogReloadableProperty(TENANT_DOMAINID_DEFAULT, REQUIRED);
        verifyAndLogReloadableProperty(AE_NODE_NAME_FOR_SIGNOFF_PROP, REQUIRED);
        verifyAndLogReloadableProperty(IDENTITY_ROLE_TENANT_DEFAULT, REQUIRED);
        verifyAndLogReloadableProperty(ENDPOINT_REGIONID_DEFAULT, REQUIRED);
        verifyAndLogReloadableProperty(EMAIL_FROM_EMAIL_ADDRESS, OPTIONAL);
    }

    private void verifyRequiredStaticProperty(String property) {
        String readProperty = staticConfiguration.getString(property);
        if (readProperty == null) {
            logger.error(String.format(PROPERTY_ERROR_MESSAGE, property, PropertyFileConfiguration.CONFIG_FILE_NAME));
        }
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

    public RepositoryConfig getRepositoryConfig() {
        return repositoryConfig;
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
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (Integer) defaultValue;
        }
    }

    private Long getLongSafely(Configuration config, String prop) {
        Object defaultValue = propertyDefaults.get(prop);
        try {
            if (defaultValue == null) {
                return config.getLong(prop);
            } else {
                return config.getLong(prop, (Long) defaultValue);
            }
        } catch (NumberFormatException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (Long) defaultValue;
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

    private String getRequiredString(Configuration config, String prop) {
        try {
            String propValue = config.getString(prop);
            if (propValue == null) {
                throw new MissingRequiredConfigIdmException(MISSING_REQUIRED_PROPERTY_ERROR_RESPONSE_MESSAGE);
            }
            return propValue;
        } catch (ConversionException e) {
            logger.error(String.format(MISSING_REQUIRED_PROPERTY_ERROR_LOG_MESSAGE, prop));
            throw new MissingRequiredConfigIdmException(MISSING_REQUIRED_PROPERTY_ERROR_RESPONSE_MESSAGE);
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

    private Boolean getRepositoryBooleanSafely(String propertyName) {
        Object defaultValue = propertyDefaults.get(propertyName);
        try {
            IdentityProperty identityProperty = identityPropertyService.getIdentityPropertyByName(propertyName);
            if (identityProperty != null) {
                return (Boolean) propertyValueConverter.convertPropertyValue(identityProperty);
            } else {
                return (Boolean) defaultValue;
            }
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, propertyName));
            return (Boolean) defaultValue;
        }
    }

    /**
     * Supports migration of properties from static to reloadable by using the prop if in reloadable, but falling back to
     * value in static if not defined in reloadable.
     *
     * @param prop
     * @return
     * @deprecated - don't use this anymore. just causes confusion. Migrate the property and require the change to be
     * made in deployments
     */
    @Deprecated
    private Boolean getBooleanSafelyWithStaticFallBack(String prop) {
        Boolean val;
        if (reloadableConfiguration.containsKey(prop)) {
            val = reloadableConfiguration.getBoolean(prop);
        } else {
            val = getBooleanSafely(staticConfiguration, prop);
        }
        return val;
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

    private Duration getDurationSafely(Configuration config, String prop) {
        Duration result = (Duration) propertyDefaults.get(prop);
        try {
            String durStr = config.getString(prop);

            if (StringUtils.isNotEmpty(durStr)) {
                result = Duration.parse(durStr); // Throws IllegalArgumentException if invalid
            }
        } catch (DateTimeParseException | ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop), e);
        }
        return result;
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
     * Return properties and their values, as annotated by {@link com.rackspace.idm.domain.config.IdmProp}.
     *
     * Uses reflection to discover getters that have been annotated with {@link com.rackspace.idm.domain.config.IdmProp}
     * @return JSONObject properties
     */
    public List<IdmProperty> getPropertyInfoList() {
        List<IdmProperty> props = getPropertyInfoList(staticConfig, IdmPropertyType.STATIC);
        props.addAll(getPropertyInfoList(reloadableConfig, IdmPropertyType.RELOADABLE));

        return props;
    }

    private List<IdmProperty> getPropertyInfoList(Object obj, IdmPropertyType propertyType) {
        Validate.isTrue(propertyType == IdmPropertyType.STATIC || propertyType == IdmPropertyType.RELOADABLE);

        List<IdmProperty> props = new ArrayList<>();

        String propFileLocation;
        boolean reloadable;
        if (propertyType == IdmPropertyType.STATIC) {
            propFileLocation = PropertyFileConfiguration.CONFIG_FILE_NAME;
            reloadable = false;
        } else {
            propFileLocation = PropertyFileConfiguration.RELOADABLE_CONFIG_FILE_NAME;
            reloadable = true;
        }

        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(IdmProp.class)) {
                final IdmProp a = m.getAnnotation(IdmProp.class);
                final String msg = String.format("error getting the value of '%s'", a.key());
                try {
                    String description = a.description();
                    String versionAdded = a.versionAdded();
                    Object defaultValue = propertyDefaults.get(a.key());
                    Object value = m.invoke(obj);
                    String name = a.key();
                    IdmProperty idmProperty = new IdmProperty();
                    idmProperty.setType(propertyType);
                    idmProperty.setName(name);
                    idmProperty.setDescription(description);
                    idmProperty.setValue(value);
                    idmProperty.setDefaultValue(defaultValue);
                    idmProperty.setVersionAdded(versionAdded);
                    idmProperty.setSource(propFileLocation);
                    idmProperty.setReloadable(reloadable);
                    props.add(idmProperty);
                } catch (Exception e) {
                    logger.error(msg, e);
                }
            }
        }
        return props;
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
        public int getEmailPort() {
            return getIntSafely(staticConfiguration, EMAIL_PORT);
        }

        @IdmProp(key=EMAIL_HOST_USERNAME_PROP, description = "Email username for authentication to email server.", versionAdded = "3.2.0")
        public String getEmailUsername() {
            return getStringSafely(staticConfiguration, EMAIL_HOST_USERNAME_PROP);
        }

        //comment out IDMProp cause this is secret info
//        @IdmProp(key=EMAIL_HOST_PASSWORD_PROP, description = "Email password for authentication to email server.", versionAdded = "3.2.0")
        public String getEmailPassword() {
            return getStringSafely(staticConfiguration, EMAIL_HOST_PASSWORD_PROP);
        }

        @IdmProp(key=EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, description = "Flag that restricts outgoing emails to only rackspace.com emails. This will prevent any emails from being sent from staging.", versionAdded = "2.5.0")
        public boolean isSendToOnlyRackspaceAddressesEnabled() {
            return getBooleanSafely(staticConfiguration, EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES);
        }

        @IdmProp(key = SETUP_MFA_SCOPED_TOKEN_EXPIRATION_SECONDS, description = "Expiration time for Setup-MFA scoped tokens.", versionAdded = "2.9.0")
        public int getSetupMfaScopedTokenExpirationSeconds() {
            return getIntSafely(staticConfiguration, SETUP_MFA_SCOPED_TOKEN_EXPIRATION_SECONDS);
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

        @IdmProp(key = IDENTITY_PROVISIONED_TOKEN_FORMAT, description = "Defines the default token format for provisioned users tokens.", versionAdded = "2.12.0")
        public TokenFormat getIdentityProvisionedTokenFormat() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return IDENTITY_PROVISIONED_TOKEN_SQL_OVERRIDE;
            }
            return convertToTokenFormat(getStringSafely(staticConfiguration, IDENTITY_PROVISIONED_TOKEN_FORMAT));
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

        @IdmProp(key = FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP)
        public boolean allowFederatedImpersonation() {
            return getBooleanSafely(staticConfiguration, FEATURE_ALLOW_FEDERATED_IMPERSONATION_PROP);
        }

        @IdmProp(key = PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME)
        public int getReloadablePropertiesTTL() {
            return getIntSafely(staticConfiguration, PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME);
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

        /**
         * @deprecated
         * @return
         */
        @Deprecated
        @IdmProp(key = MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME, versionAdded = "2.5.0", description="The location of the legacy encryption keys")
        public String getMfaKeyLocation() {
            return getStringSafely(staticConfiguration, MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME);
        }

        @IdmProp(key = FEEDS_MAX_CONNECTIONS_PROP, versionAdded = "3.5.0", description = "The total http connections allowed by the HttpClient used to post feed events")
        public int getFeedsMaxTotalConnections() {
            return getIntSafely(staticConfiguration, FEEDS_MAX_CONNECTIONS_PROP);
        }

        @IdmProp(key = FEEDS_MAX_CONNECTIONS_PER_ROUTE_PROP, versionAdded = "3.5.0", description = "The total http connections allowed by the HttpClient for each route used to post feed events")
        public int getFeedsMaxConnectionsPerRoute() {
            return getIntSafely(staticConfiguration, FEEDS_MAX_CONNECTIONS_PER_ROUTE_PROP);
        }

        @IdmProp(key = FEEDS_USE_CONFIGURABLE_HTTPCLIENT_PROP, versionAdded = "3.5.0"
                , description = "Whether or not to configure the http client used to establish feed connections using " +
                "the other 'feeds*' properties")
        public boolean useFeedsConfigurableHttpClient() {
            return getBooleanSafely(staticConfiguration, FEEDS_USE_CONFIGURABLE_HTTPCLIENT_PROP);
        }

        @IdmProp(key = FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_PROP, versionAdded = "3.5.0", description = "The timeout to establish a new socket for non-blocking I/O operations ")
        public int getFeedsNewConnectionSocketTimeout() {
            return getIntSafely(staticConfiguration, FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_PROP);
        }

        @IdmProp(key = FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_PROP, versionAdded = "3.5.0", description = "After how long of inactivity, in ms, a connection will be checked for validity.")
        public int getFeedsOnUseEvictionValidateAfterInactivity() {
            return getIntSafely(reloadableConfiguration, FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_PROP);
        }

        @IdmProp(key = LDAP_SERVER_LIST_PROP, versionAdded = "1.0.14.8", description = "Comma or space delimited list of server addresses in host:port format, port default is 636")
        public String[] getLDAPServerList() {
            return staticConfiguration.getStringArray(LDAP_SERVER_LIST_PROP);
        }

        @IdmProp(key = LDAP_SERVER_USE_SSL_PROP, versionAdded = "1.0.14.8", description = "Specifies if the LDAP server should use SSL for the connection")
        public boolean getLDAPServerUseSSL() {
            return getBooleanSafely(staticConfiguration, LDAP_SERVER_USE_SSL_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_SIZE_INIT_PROP, versionAdded = "1.0.14.8", description = "The number of connections to initially establish when the pool is created.  It must be greater than or equal to zero.")
        public int getLDAPServerPoolSizeInit() {
            return getIntSafely(staticConfiguration, LDAP_SERVER_POOL_SIZE_INIT_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_SIZE_MAX_PROP, versionAdded = "1.0.14.8", description = "The maximum number of connections that should be maintained in the pool.  It must be greater than or equal to the initial number of connections, and must not be zero.")
        public int getLDAPServerPoolSizeMax() {
            return getIntSafely(staticConfiguration, LDAP_SERVER_POOL_SIZE_MAX_PROP);
        }

        @IdmProp(key = LDAP_SERVER_BIND_DN_PROP, versionAdded = "1.0.14.8", description = "The bind DN for LDAP server simple bind requests.")
        public String getLDAPServerBindDN() {
            return getStringSafely(staticConfiguration, LDAP_SERVER_BIND_DN_PROP);
        }

        //@IdmProp(key = LDAP_SERVER_BIND_PASSWORD_PROP , versionAdded = "1.0.14.8", description = "The password for LDAP server simple bind request.")
        public String getLDAPServerBindPassword() {
            return getStringSafely(staticConfiguration, LDAP_SERVER_BIND_PASSWORD_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_AGE_MAX_PROP, versionAdded = "1.0.14.8", description = "Specifies the maximum length of time in milliseconds that a connection in this pool may be established before it should be closed and replaced with another connection.")
        public int getLDAPServerPoolAgeMax() {
            return getIntSafely(staticConfiguration, LDAP_SERVER_POOL_AGE_MAX_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_CREATE_IF_NECESSARY_PROP, versionAdded = "3.6.0", description = "Specifies whether the connection pool should create a new connection if one is requested when there are none available.")
        public boolean getLDAPServerPoolCreateIfNecessary() {
            return getBooleanSafely(staticConfiguration, LDAP_SERVER_POOL_CREATE_IF_NECESSARY_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_MAX_WAIT_TIME_PROP, versionAdded = "3.6.0", description = "Specifies the maximum length of time in milliseconds to wait for a connection to become available when trying to obtain a connection from the pool.")
        public long getLDAPServerPoolMaxWaitTime() {
            return getLongSafely(staticConfiguration, LDAP_SERVER_POOL_MAX_WAIT_TIME_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_PROP, versionAdded = "3.6.0", description = "Specifies the length of time in milliseconds between periodic background health checks against the available connections in this pool.")
        public long getLDAPServerPoolHeathCheckInterval() {
            return getLongSafely(staticConfiguration, LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_PROP, versionAdded = "3.6.0", description = "Specifies whether to check the age of a connection against the configured maximum connection age whenever it is released to the pool.")
        public boolean getLDAPServerPoolCheckConnectionAgeOnRelease() {
            return getBooleanSafely(staticConfiguration, LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_PROP);
        }

        @IdmProp(key = LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_PROP, versionAdded = "3.6.0", description = " Indicates whether to allow a socket factory instance to be used to create multiple sockets concurrently.")
        public boolean getLDAPServerPoolAllowConcurrentSocketFactoryUse() {
            return getBooleanSafely(staticConfiguration, LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_PROP);
        }

        @IdmProp(key = FEEDS_DEAMON_EVICTION_ENABLED_PROP, versionAdded = "3.11.0", description = "Specifies whether to enable feeds deamon to evict expired connections from connection pool.")
        public boolean getFeedsDeamonEnabled() {
            return getBooleanSafely(staticConfiguration, FEEDS_DEAMON_EVICTION_ENABLED_PROP);
        }

        @IdmProp(key = CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, versionAdded = "3.11.0" , description = "The ttl of entries in the client role by id cache. A ttl of 0 means no cache.")
        public Duration getClientRoleByIdCacheTtl() {
            return getDurationSafely(staticConfiguration, CACHE_CLIENT_ROLES_BY_ID_TTL_PROP);
        }

        @IdmProp(key = CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, versionAdded = "3.11.0" , description = "The max size of the client role by id cache.")
        public int getClientRoleByIdCacheSize() {
            return getIntSafely(staticConfiguration, CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP);
        }

        @IdmProp(key = NAST_TENANT_PREFIX_PROP, versionAdded = "1.0.14.8"
                , description = "The prefix to append to nast tenant ids")
        public String getNastTenantPrefix() {
            return getStringSafely(staticConfiguration, NAST_TENANT_PREFIX_PROP);
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

        @IdmProp(key = FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP, versionAdded = "3.5.0", description = "Whether to strip out all the endpoints from the list endpoints for token call for a user that is SUSPENDED by terminator")
        public boolean getFeatureListEndpointsForTokenFilteredForTerminator() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP);
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

        @IdmProp(key = FEATURE_MIGRATION_SAVE_DELTA_ASYNC_PROP, description = "Whether the delta migration events should be recorded in an asynchronous fashion", versionAdded = "3.2.0")
        public boolean migrationSaveAsyncEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_MIGRATION_SAVE_DELTA_ASYNC_PROP);
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

        @IdmProp(key = FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME, versionAdded = "3.1.0", description = "The max token lifetime for a provisioned federated user token")
        public int getFederatedDomainTokenLifetimeMax() {
            return getIntSafely(reloadableConfiguration, FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME);
        }

        @IdmProp(key = FEDERATED_RESPONSE_MAX_AGE, versionAdded = "3.1.0", description = "The max age of a saml response for it to be considered valid.")
        public int getFederatedResponseMaxAge() {
            return getIntSafely(reloadableConfiguration, FEDERATED_RESPONSE_MAX_AGE);
        }

        @IdmProp(key = FEDERATED_RESPONSE_MAX_SKEW, versionAdded = "3.1.0", description = "The max skew +/- seconds of a saml response for it to still be considered valid.")
        public int getFederatedResponseMaxSkew() {
            return getIntSafely(reloadableConfiguration, FEDERATED_RESPONSE_MAX_SKEW);
        }

        @IdmProp(key = IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_PREFIX, versionAdded = "3.1.0", description = "The max number of users in IDP per domain.")
        public Integer getIdentityFederationMaxUserCountPerDomainForIdp(String idpLabeledUri) {
            int def = getIdentityFederationMaxUserCountPerDomainDefault();
            String propName = String.format(IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_REG, idpLabeledUri);
            try {
                return reloadableConfiguration.getInteger(propName, def);
            } catch (Exception ex) {
                logger.error(String.format("Error retrieving property '%s' as an integer. Returning default.", propName));
                return def;
            }
        }

        @IdmProp(key = IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, versionAdded = "3.1.0", description = "The default max number of users in IDP per domain.")
        public int getIdentityFederationMaxUserCountPerDomainDefault() {
            return getIntSafely(reloadableConfiguration, IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP);
        }

        @IdmProp(key = FEATURE_SUPPORT_SAML_LOGOUT_PROP, versionAdded = "3.1.0", description = "Whether or not to support SAML Federation Logout")
        public boolean isFederationLogoutSupported() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_SUPPORT_SAML_LOGOUT_PROP);
        }

        @IdmProp(key = FEATURE_SUPPORT_IDENTITY_PROVIDER_MANAGEMENT_PROP, versionAdded = "3.1.0", description = "Whether or not to support Identity Provider Management services")
        public boolean isIdentityProviderManagementSupported() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_SUPPORT_IDENTITY_PROVIDER_MANAGEMENT_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, versionAdded = "3.12.0", description = "Whether or not domain password policy support is enabled")
        public boolean isPasswordPolicyServicesEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP);
        }

        @IdmProp(key = FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, versionAdded = "3.12.0", description = "Whether or not to enforce password policy password rule")
        public boolean enforcePasswordPolicyPasswordExpiration() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP);
        }

        @IdmProp(key = FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, versionAdded = "3.12.0", description = "Whether or not to enforce password policy history")
        public boolean enforcePasswordPolicyPasswordHistory() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP);
        }

        @IdmProp(key = FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, versionAdded = "3.12.0", description = "Whether or not to maintain password history. If history enforcement is enabled, this is always true")
        public boolean maintainPasswordHistory() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP) || enforcePasswordPolicyPasswordHistory();
        }

        @IdmProp(key = IDP_MAX_SEACH_RESULT_SIZE_PROP, versionAdded = "3.1.0", description = "Maximum numbers of identity providers allowed to be returned in list providers call")
        public int getMaxListIdentityProviderSize() {
            return getIntSafely(reloadableConfiguration, IDP_MAX_SEACH_RESULT_SIZE_PROP);
        }

        @IdmProp(key = PURGE_TRRS_MAX_DELAY_PROP, versionAdded = "3.4.0", description = "Maximum time, in ms, caller can specify to delay between deleting token revocations records")
        public int getPurgeTokenRevocationRecordsMaxDelay() {
            return getIntSafely(reloadableConfiguration, PURGE_TRRS_MAX_DELAY_PROP);
        }

        @IdmProp(key = PURGE_TRRS_MAX_LIMIT_PROP, versionAdded = "3.4.0", description = "Maximum number of TRRs that will be deleted in the call")
        public int getPurgeTokenRevocationRecordsMaxLimit() {
            return getIntSafely(reloadableConfiguration, PURGE_TRRS_MAX_LIMIT_PROP);
        }

        @IdmProp(key = PURGE_TRRS_OBSOLETE_AFTER_PROP, versionAdded = "3.4.0", description = "Number of hours after creation, based on accessTokenExp attribute, that a TRR can be purged")
        public int getPurgeTokenRevocationRecordsObsoleteAfterHours() {
            return getIntSafely(reloadableConfiguration, PURGE_TRRS_OBSOLETE_AFTER_PROP);
        }

        @IdmProp(key = FEDERATED_DELTA_EXPIRATION_SECONDS_PROP, versionAdded = "3.1.0", description = "Delta time in seconds to be added for federated users deletion eligibility")
        public int getFederatedDeltaExpiration() {
            return getIntSafely(reloadableConfiguration, FEDERATED_DELTA_EXPIRATION_SECONDS_PROP);
        }

        @IdmProp(key = FEATURE_FEDERATION_DELETION_MAX_DELAY_PROP, versionAdded = "3.1.1", description = "Maximum time for federation deletion delta in milliseconds")
        public int getFederatedDeletionMaxDelay() {
            return getIntSafely(reloadableConfiguration, FEATURE_FEDERATION_DELETION_MAX_DELAY_PROP);
        }

        @IdmProp(key = FEATURE_FEDERATION_DELETION_MAX_COUNT_PROP, versionAdded = "3.1.1", description = "Maximum count for federation deletion")
        public int getFederatedDeletionMaxCount() {
            return getIntSafely(reloadableConfiguration, FEATURE_FEDERATION_DELETION_MAX_COUNT_PROP);
        }

        @IdmProp(key = FEATURE_FEDERATION_DELETION_ROLE_PROP, versionAdded = "3.1.1", description = "Federation deletion role")
        public String getFederatedDeletionRole() {
            return getStringSafely(reloadableConfiguration, FEATURE_FEDERATION_DELETION_ROLE_PROP);
        }

        @IdmProp(key = FEATURE_FEDERATION_DELETION_TIMEOUT_PROP, versionAdded = "3.1.1", description = "Timeout for federation deletion lock")
        public int getFederatedDeletionTimeout() {
            return getIntSafely(reloadableConfiguration, FEATURE_FEDERATION_DELETION_TIMEOUT_PROP);
        }

        @IdmProp(key = FEATURE_SUPPORT_V11_LEGACY_PROP, versionAdded = "3.1.1", description = "Enable v1.1 legacy calls")
        public boolean getV11LegacyEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_SUPPORT_V11_LEGACY_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_MFA_MIGRATION_SERVICES_PROP, versionAdded = "3.1.1", description = "Enable MFA SMS Migration services")
        public boolean areMfaMigrationServicesEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_MFA_MIGRATION_SERVICES_PROP);
        }

        @IdmProp(key = FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, versionAdded = "3.2.0", description = "Timeout for forgot password tokens")
        public int getForgotPasswordTokenLifetime() {
            return getIntSafely(reloadableConfiguration, FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME);
        }

        @IdmProp(key = FEATURE_UPGRADE_USER_TO_CLOUD_PROP_NAME, versionAdded = "3.3.0", description = "Whether or not the upgrade user to cloud service is enabled")
        public boolean isUpgradeUserToCloudEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_UPGRADE_USER_TO_CLOUD_PROP_NAME);
        }

        @IdmProp(key = UPGRADE_USER_ELIGIBLE_ROLE_PROP, versionAdded = "3.3.0", description = "The role a user must have to be eligible to be upgraded")
        public String getUpgradeUserEligibleRole() {
            return getRequiredString(reloadableConfiguration, UPGRADE_USER_ELIGIBLE_ROLE_PROP);
        }

        @IdmProp(key = FEATURE_FORGOT_PWD_ENABLED_PROP_NAME, versionAdded = "3.2.0", description = "Whether or not the forgot password flow is enabled")
        public boolean isForgotPasswordEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_FORGOT_PWD_ENABLED_PROP_NAME);
        }

        @IdmProp(key = FORGOT_PWD_VALID_PORTALS_PROP_NAME, versionAdded = "3.2.0", description = "Comma delimited list of valid portal values for forgot password")
        public Set<String> getForgotPasswordValidPortals() {
            return getSetSafely(reloadableConfiguration, FORGOT_PWD_VALID_PORTALS_PROP_NAME);
        }

        @IdmProp(key = FEATURE_USE_VELOCITY_FOR_MFA_EMAILS_PROP, versionAdded = "3.2.0", description = "Whether or not to use velocity templates for mfa emails. Will fall back to original if any error is encountered.")
        public boolean useVelocityForMfaEmail() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_USE_VELOCITY_FOR_MFA_EMAILS_PROP);
        }

        @IdmProp(key=EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, description = "(Migrated from static w/ fallback to static if not found in reloadable). Flag that restricts outgoing emails to only rackspace.com emails. This will prevent any emails from being sent from staging.", versionAdded = "3.2.0")
        public boolean isSendToOnlyRackspaceAddressesEnabled() {
            return getBooleanSafelyWithStaticFallBack(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES);
        }

        @IdmProp(key = EMAIL_FROM_EMAIL_ADDRESS, description = "Return email address to use when sending emails to customers. Was added as a static property in version 2.5.0, but was migrated to be a reloadable in this version.", versionAdded = "3.2.0")
        public String getEmailFromAddress() {
            return getStringSafely(reloadableConfiguration, EMAIL_FROM_EMAIL_ADDRESS);
        }

        @IdmProp(key = FEATURE_LIST_GROUPS_FOR_SELF_PROP, versionAdded = "3.3.0", description = "Whether or not the feature to allow for a user to list groups for self is enabled")
        public boolean isListGroupsForSelfEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_LIST_GROUPS_FOR_SELF_PROP);
        }

        @IdmProp(key = FEATURE_LIST_ENDPOINTS_FOR_OWN_TOKEN_PROP, versionAdded = "3.3.0", description = "Whether or not to allow for a user to list endpoints for their own token")
        public boolean isFeatureListEndpointsForOwnTokenEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_LIST_ENDPOINTS_FOR_OWN_TOKEN_PROP);
        }

        @IdmProp(key = FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, versionAdded = "3.4.0", description = "Whether or not to issued restricted AE Tokens w/ a sessionid scope for MFA X-Session-Ids")
        public boolean issueRestrictedTokenSessionIds() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP);
        }

        @IdmProp(key = SESSION_ID_LIFETIME_PROP, versionAdded = "3.4.0", description = "Lifetime, in minutes, of MFA sessionIds. Was added as static prop in 2.2.0, but switched from to reloadable prop in version 3.4.0")
        public int getMfaSessionIdLifetime() {
            return getIntSafely(reloadableConfiguration, SESSION_ID_LIFETIME_PROP);
        }

        @IdmProp(key = FEATURE_DELETE_IDENTITY_ROLE_PREVENTION_ENABLED_PROP, versionAdded = "3.4.0", description = "Whether or not to prevent a user from being able to delete the identity access role from another user. The user is still able to delete the identity:user-manage if this feature is enabled.")
        public boolean isDeleteIdentityAccessRolePreventionEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_DELETE_IDENTITY_ROLE_PREVENTION_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_ENCRYPT, versionAdded = "3.4.0", description = "Whether or not new AE Tokens can be issued. Was added as static prop in 2.12.0, but switched from to reloadable prop in version 3.4.0")
        public boolean getFeatureAETokensEncrypt() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return FEATURE_AE_TOKENS_ENCRYPT_SQL_OVERRIDE;
            }
            return getBooleanSafely(reloadableConfiguration, FEATURE_AE_TOKENS_ENCRYPT);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_DECRYPT, versionAdded = "3.4.0", description = "Whether or not existing AE Tokens can be received. Was added as static prop in 2.12.0, but switched from to reloadable prop in version 3.4.0")
        public boolean getFeatureAETokensDecrypt() {
            if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                return FEATURE_AE_TOKENS_DECRYPT_SQL_OVERRIDE;
            }
            return getFeatureAETokensEncrypt() || reloadableConfiguration.getBoolean(FEATURE_AE_TOKENS_DECRYPT);
        }

        @IdmProp(key = FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_PROP, versionAdded = "3.3.1", description = "Whether or not to allow racker impersonation requests to see user's API key credentials.")
        public boolean preventRackerImpersonationApiKeyAccess() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_PROP);
        }

        @IdmProp(key = ATOM_HOPPER_URL_PROP, versionAdded = "3.5.0", description = "The URL to use when posting events to atom hopper. This property was introduced as a static property in version 1.0.14.8 and migrated to a reloadable property in version 3.5.0.")
        public String getAtomHopperUrl() {
            return reloadableConfiguration.getString(ATOM_HOPPER_URL_PROP);
        }

        @IdmProp(key = ATOM_HOPPER_DATA_CENTER_PROP, versionAdded = "3.5.0", description = "The data center to use when posting events to atom hopper. This property was introduced as a static property in version 1.0.14.8 and migrated to a reloadable property in version 3.5.0.")
        public String getAtomHopperDataCenter() {
            return reloadableConfiguration.getString(ATOM_HOPPER_DATA_CENTER_PROP);
        }

        @IdmProp(key = ATOM_HOPPER_REGION_PROP, versionAdded = "3.5.0", description = "The region to use when posting events to atom hopper. This property was introduced as a static property in version 1.0.14.8 and migrated to a reloadable property in version 3.5.0.")
        public String getAtomHopperRegion() {
            return reloadableConfiguration.getString(ATOM_HOPPER_REGION_PROP);
        }

        @IdmProp(key = FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP, versionAdded = "3.3.2", description = "Whether or not to return the custom versions.json when GET /cloud is called and json is requested or translate the versions.xml to json")
        public boolean returnJsonSpecificCloudVersionResource() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP);
        }

        @IdmProp(key = FEATURE_REUSE_JAXB_CONTEXT, versionAdded = "3.3.3", description = "Whether or not to reuse JAXBContext across threads rather than creating new one for each use which causes a memory leak. This feature is only here to revert to existing functinality if required.")
        public boolean reuseJaxbContext() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_REUSE_JAXB_CONTEXT);
        }

        @IdmProp(key = MAX_CA_DIRECTORY_PAGE_SIZE_PROP, versionAdded = "3.5.0", description = "The maximum page size allowed to query the directory for. This property should be set to match the maximum query size as configured on " +
                "the CA directory side. It should also be noted that this property will also impact other services through the API. For example, the API call used for deleting TRRs will limit how many TRRs can be deleted in a single request based upon this configuration.")
        public int getMaxDirectoryPageSize() {
            return getIntSafely(reloadableConfiguration, MAX_CA_DIRECTORY_PAGE_SIZE_PROP);
        }

        @IdmProp(key = LIST_GLOBAL_ROLES_FOR_USER_PRECEDENCE_RESTRICTION_ENABLED_PROP, versionAdded = "3.5.0", description = "Whether or not to apply the usual precedence restriction to service and identity admins for the 'List global roles for user' API call." +
                "When enabled, this will prevent identity admins from listing roles for service admins and other identity admins. Service admins will also be prevented from listing roles for other service admins.")
        public boolean listGlobalRolesForUserPrecedenceRestrictionEnabled() {
            return getBooleanSafely(reloadableConfiguration, LIST_GLOBAL_ROLES_FOR_USER_PRECEDENCE_RESTRICTION_ENABLED_PROP);
        }

        @IdmProp(key = FEEDS_SOCKET_TIMEOUT_MS_PROP, versionAdded = "3.5.0"
                , description = "The timeout for waiting for data when sending feed requests - or, put differently, " +
                "a maximum period inactivity between two consecutive data packets. A timeout value of zero is " +
                "interpreted as an infinite")
        public int getFeedsSocketTimeout() {
            return getIntSafely(reloadableConfiguration, FEEDS_SOCKET_TIMEOUT_MS_PROP);
        }

        @IdmProp(key = FEEDS_CONNECTION_TIMEOUT_MS_PROP, versionAdded = "3.5.0"
                , description = "The timeout in milliseconds until a connection is established. A timeout value of " +
                "zero is interpreted as an infinite")
        public int getFeedsConnectionTimeout() {
            return getIntSafely(reloadableConfiguration, FEEDS_CONNECTION_TIMEOUT_MS_PROP);
        }

        @IdmProp(key = FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_PROP, versionAdded = "3.5.0"
                , description = "The timeout in milliseconds until a connection is retrieve from the connection pool.")
        public int getFeedsConnectionRequestTimeout() {
            return getIntSafely(reloadableConfiguration, FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_PROP);
        }

        @IdmProp(key = FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_PROP, versionAdded = "3.5.0", description = "Whether or not endpoint template creation is allowed using service name and type.")
        public boolean getFeatureEndpointTemplateDisableNameType() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_PROP);
        }

        @IdmProp(key = FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED, versionAdded = "3.7.0", description = "Whether or not a user will receive global endpoints associated will all roles the user has on any given tenant.")
        public boolean getFeatureGlobalEndpointsForAllRoles() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED);
        }

        @IdmProp(key = FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_USAGE_PROP, versionAdded = "3.7.0", description = "When true, if a user-manager calls the 'list users' service without a filter other user managers within the domain will not be returned.")
        public boolean restrictUserManagersFromListingOtherUserManagers() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_USAGE_PROP);
        }

        @IdmProp(key = FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_EMAIL_USAGE_PROP, versionAdded = "3.7.0", description = "When true, if a user-manager calls the 'list users' service with email filter other user managers within the domain will not be returned.")
        public boolean restrictUserManagersFromListingOtherUserManagersByEmail() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_EMAIL_USAGE_PROP);
        }

        @IdmProp(key = FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_NAME_USAGE_PROP, versionAdded = "3.7.0", description = "When true, if a user-manager calls the 'list users' service with name filter they can't get other user managers within the domain.")
        public boolean restrictUserManagersFromListingOtherUserManagersByName() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_NAME_USAGE_PROP);
        }

        @IdmProp(key = FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, versionAdded = "3.8.0", description = "When true, endpoints based on rules are included in 'authentication' and 'list endpoints for token'")
        public boolean includeEndpointsBasedOnRules() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP);
        }

        public boolean isAutomaticallyAssignUserRoleOnDomainTenantsEnabled() {
            boolean featureEnabled = true;
            if (StringUtils.isBlank(getAutomaticallyAssignUserRoleOnDomainTenantsRoleName())) {
                // If feature is enabled, but the role name is blank, then the feature can't actually be enabled
                logger.error(String.format("Can not implicitly assign role on all tenants in domain. The required property '%s' is not set."
                        , AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP));
                featureEnabled = false;
            } else if (!getAutomaticallyAssignUserRoleOnDomainTenantsRoleName().startsWith(GlobalConstants.IDENTITY_ROLE_PREFIX)) {
                // If feature is enabled, the role name must be an identity product role
                logger.error(String.format("Can not implicitly assign role on all tenants in domain. The required property '%s' is not set " +
                        "to a valid identity product role.",  AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP));
                featureEnabled = false;
            }
            return featureEnabled;
        }

        @IdmProp(key = AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP, versionAdded = "3.8.0", description = "The identity product role to automatically assign to the user on all tenants within the user's domain.")
        public String getAutomaticallyAssignUserRoleOnDomainTenantsRoleName() {
            return getStringSafely(reloadableConfiguration, AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP);
        }

        @IdmProp(key = FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_PROP, versionAdded = "3.8.0", description = "When true, generic account and cloud account creating cannot be created in a default domain")
        public boolean restrictCreateUserInDefaultDomain() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_PROP);
        }

        @IdmProp(key = FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_PROP, versionAdded = "3.8.0", description = "When true, generic account and cloud account creating cannot be created in a disabled domain")
        public boolean restrictCreateUserInDisabledDomain() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_PROP);
        }

        @IdmProp(key = FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP , versionAdded = "3.8.0", description = "When true, generic account and cloud account creating cannot be created in domain with users")
        public boolean restrictCreateUserInDomainWithUsers() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP );
        }

        @IdmProp(key = FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_PROP, versionAdded = "3.9.0", description = "When true, additional role attributes are returned.")
        public boolean listSupportAdditionalRoleProperties() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_PROP);
        }

        @IdmProp(key = FEATURE_POST_IDP_FEED_EVENTS_PROP, versionAdded = "3.9.0", description = "When true, whenever an IDP is created, updated, or deleted an IDP feed event is posted to the Identity feed.")
        public boolean postIdpFeedEvents() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_POST_IDP_FEED_EVENTS_PROP);
        }

        @IdmProp(key = IDP_POLICY_MAX_KILOBYTE_SIZE_PROP, versionAdded = "3.9.0", description = "Determines the max size in kilobytes for an IDP's policy file.")
        public int getIdpPolicyMaxSize() {
            return getIntSafely(reloadableConfiguration, IDP_POLICY_MAX_KILOBYTE_SIZE_PROP);
        }

        @IdmProp(key = FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_PROP, versionAdded = "3.10.0", description = "Determines if the X-Tenant-Id header should be populated in v1.0 authenticate calls.")
        public boolean shouldIncludeTenantInV10AuthResponse() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_PROP);
        }

        @IdmProp(key = FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_PROP, versionAdded = "3.10.0", description = "Determines if the X-Tenant-Id header should be populated in v1.1 authenticate calls.")
        public boolean shouldIncludeTenantInV11AuthResponse() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_PROP);
        }

        @IdmProp(key = FEEDS_DAEMON_EVICTION_FREQUENCY_MS_PROP, versionAdded = "3.11.0" , description = "When the feeds pool is using DAEMON eviction strategy, how often expired connections are removed from the pool.")
        public int getFeedsDaemonEvictionFrequency() {
            return getIntSafely(reloadableConfiguration, FEEDS_DAEMON_EVICTION_FREQUENCY_MS_PROP);
        }

        @IdmProp(key = FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_PROP, versionAdded = "3.11.0" , description = "When the feeds pool is using DAEMON eviction strategy, connections will be removed from the pool if they have been idle for this this many ms. A value <= 0 indicates idle connections will not be removed")
        public int getFeedsDaemonEvictionCloseIdleConnectionsAfter() {
            return getIntSafely(reloadableConfiguration, FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_PROP);
        }

        @IdmProp(key = FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_PROP, versionAdded = "3.11.0" , description = "When true, v2 federation calls will validate the issueInstant of the origin saml assertions.")
        public boolean shouldV2FederationValidateOriginIssueInstant() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_PROP);
        }

        @IdmProp(key = FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_PROP , versionAdded = "3.11.0" , description = "When true, allow updating approvedDomainIds for Identity provider.")
        public boolean getAllowUpdatingApprovedDomainIdsForIdp() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_PROP);
        }

        @IdmProp(key = DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_PROP, versionAdded = "3.11.0", description = "Default value for session inactivity timeout assigned to domains.")
        public Duration getDomainDefaultSessionInactivityTimeout() {
            return getDurationSafely(reloadableConfiguration, DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_PROP);
        }

        @IdmProp(key = FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_PROP, versionAdded = "3.11.0", description = "Specifies whether to enable keep alive for feed connections")
        public boolean getFeedsAllowConnectionKeepAlive() {
            return getBooleanSafely(staticConfiguration, FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_PROP);
        }

        @IdmProp(key = FEEDS_CONNECTION_KEEP_ALIVE_MS_PROP, versionAdded = "3.11.0", description = "Specifies the keep alive feed connections duration")
        public long getFeedsConnectionKeepAliveDuration() {
            return getLongSafely(staticConfiguration, FEEDS_CONNECTION_KEEP_ALIVE_MS_PROP);
        }

        @IdmProp(key = SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_PROP, versionAdded = "3.11.0", description = "Session inactivity timeout max duration in ISO 8601 format.")
        public Duration getSessionInactivityTimeoutMaxDuration() {
            return getDurationSafely(reloadableConfiguration, SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_PROP);
        }

        @IdmProp(key = SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_PROP, versionAdded = "3.11.0", description = "Session inactivity timeout min duration in ISO 8601 format.")
        public Duration getSessionInactivityTimeoutMinDuration() {
            return getDurationSafely(reloadableConfiguration, SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_PROP);
        }

        @IdmProp(key = FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, versionAdded = "3.11.0" , description = "Whether or not to use the performant version of the service catalog.")
        public boolean usePerformantServiceCatalog() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_PERFORMANT_SERVICE_CATALOG_PROP);
        }

        @IdmProp(key = PASSWORD_HISTORY_MAX_PROP, versionAdded = "3.12.0" , description = "The maximum number of password history entries Identity will store for a user. Will actually store 1 more than this to include the 'current' password.")
        public int getPasswordHistoryMax() {
            return getIntSafely(reloadableConfiguration, PASSWORD_HISTORY_MAX_PROP);
        }

        @IdmProp(key = FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_PROP, versionAdded = "3.11.0" , description = "Whether or not to use cached client roles for the service catalog. Only applicable when feature.performant.service.catalog=true")
        public boolean useCachedClientRolesInServiceCatalog() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_PROP);
        }

        @IdmProp(key = FEATURE_USE_CACHED_CLIENT_ROLES_FOR_VALIDATE_PROP, versionAdded = "3.11.0" , description = "Whether or not to use cached client roles in the validate call.")
        public boolean useCachedClientRolesInValidate() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_USE_CACHED_CLIENT_ROLES_FOR_VALIDATE_PROP);
        }

        @IdmProp(key = FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_PROP, versionAdded = "3.11.0", description = "Whether to change contract for set of services accessible to user-admin/default users which currently return non-standard v2.0 error objects to now return the standard errors")
        public boolean forceStandardV2ExceptionsEndUserServices() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_PROP);
        }

        @IdmProp(key = FEATURE_INFER_DEFAULT_TENANT_TYPE_PROP, versionAdded = "3.12.0", description = "Whether to infer a tenant type for tenants without a tenant type when applying RCN roles")
        public boolean inferTenantTypeForTenant() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_INFER_DEFAULT_TENANT_TYPE_PROP);
        }

        @IdmProp(key = MAX_TENANT_TYPE_SIZE_PROP, versionAdded = "3.12.0", description = "Maximum number of tenantTypes allowed to be created.  Maximum value allowed is 999.")
        public int getMaxTenantTypes() {
            return Math.min(getIntSafely(reloadableConfiguration, MAX_TENANT_TYPE_SIZE_PROP), MAX_TENANT_TYPE_SIZE_DEFAULT);
        }

        @IdmProp(key = FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP, versionAdded = "3.12.0", description = "Whether to set the default tenant type on the tenant upon creation based on the tenant name prefix.")
        public boolean shouldSetDefaultTenantTypeOnCreation() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP);
        }

    }

    public class RepositoryConfig {

        public String getIdentityProviderDefaultPolicy() {
            IdentityProperty prop = identityPropertyService.getIdentityPropertyByName(FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP);
            return (String) propertyValueConverter.convertPropertyValue(prop);
        }

    }

    @Deprecated
    public String getGaUsername() {
        return getStaticConfig().getGaUsername();
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
    public boolean getFeatureAeTokenCleanupUuidOnRevokes() {
        return getStaticConfig().getFeatureAeTokenCleanupUuidOnRevokes();
    }

    @Deprecated
    public String getKeyCzarDN() {
        return getStaticConfig().getKeyCzarDN();
    }

    @Deprecated
    public boolean allowFederatedImpersonation() {
        return getStaticConfig().allowFederatedImpersonation();
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
