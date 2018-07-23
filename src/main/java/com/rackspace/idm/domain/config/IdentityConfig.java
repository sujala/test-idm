package com.rackspace.idm.domain.config;

import com.google.common.base.Splitter;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter;
import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.domain.security.TokenFormat;
import com.rackspace.idm.domain.service.IdentityPropertyService;
import com.rackspace.idm.event.NewRelicCustomAttributesEnum;
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
import javax.ws.rs.core.MediaType;
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
    public static final String MAX_NUM_USERS_IN_DOMAIN = "maxNumberOfUsersInDomain";

    private static final String EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES = "email.send.to.only.rackspace.addresses.enabled";
    private static final String SETUP_MFA_SCOPED_TOKEN_EXPIRATION_SECONDS = "token.scoped.expirationSeconds";
    public static final String FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME = "token.forgot.password.validity.length";
    private static final int FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_DEFAULT = 600;
    public static final String FORGOT_PWD_VALID_PORTALS_PROP_NAME = "forgot.password.valid.portals";
    private static final List<String> FORGOT_PWD_VALID_PORTALS_DEFAULT = Collections.EMPTY_LIST;

    private static final String CLOUD_AUTH_CLIENT_ID = "cloudAuth.clientId";
    public static final String IDENTITY_ACCESS_ROLE_NAMES_PROP = "cloudAuth.accessRoleNames";
    public static final String IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP = "cloudAuth.adminRole";
    public static final String IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP = "cloudAuth.serviceAdminRole";
    public static final String IDENTITY_USER_ADMIN_ROLE_NAME_PROP = "cloudAuth.userAdminRole";
    public static final String IDENTITY_USER_MANAGE_ROLE_NAME_PROP = "cloudAuth.userManagedRole";
    public static final String IDENTITY_DEFAULT_USER_ROLE_NAME_PROP = "cloudAuth.userRole";
    public static final String IDENTITY_PROVISIONED_TOKEN_FORMAT = "feature.provisioned.defaultTokenFormat";
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

    public static final String IDENTITY_FEDERATED_MAX_IDP_PER_DOMAIN_PROP = "federated.max.identity.provider.per.domain";
    public static final int IDENTITY_FEDERATED_MAX_IDP_PER_DOMAIN_DEFAULT = 10;

    public static final String IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP = "feature.enable.external.user.idp.management";
    public static final boolean IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_DEFAULT = false;

    /**
     * The format of the property name to set the token format for a specific IDP. The '%s' is replaced by the IDP's labeledUri. This
     * means that each IDP has a custom property. If no such property exists for the IDP, the value for {@link #IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP}
     * is used.
     */
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX = "federated.provider.tokenFormat";
    public static final String IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG = IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_PREFIX + ".%s";
    private static final String KEYCZAR_DN_CONFIG = "feature.KeyCzarCrypterLocator.ldap.dn";
    public static final String FEATURE_AE_TOKENS_ENCRYPT = "feature.ae.tokens.encrypt";
    public static final String FEATURE_AE_TOKENS_DECRYPT = "feature.ae.tokens.decrypt";

    //OPTIONAL PROPERTIES
    private static final boolean REQUIRED = true;
    private static final boolean OPTIONAL = false;
    private static final String PROPERTY_SET_MESSAGE = "Configuration Property '%s' set with value '%s' in '%s'";
    private static final String PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is NOT set but is required in '%s'";

    private static final String INVALID_PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is invalid";
    private static final String MISSING_REQUIRED_PROPERTY_ERROR_LOG_MESSAGE = "Configuration Property '%s' is invalid";
    private static final String MISSING_REQUIRED_PROPERTY_ERROR_RESPONSE_MESSAGE = "This service is currently unavailable in Identity.";
    public static final String EXPOSE_V11_ADD_BASE_URL_PROP = "feature.v11.add.base.url.exposed";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP = "feature.endpoint.template.type.use.config.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP = "feature.endpoint.template.type.mosso.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP = "feature.endpoint.template.type.nast.mapping";
    public static final String FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_PROP = "feature.endpoint.template.disable.name.type";
    public static final boolean FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_DEFAULT = false;
    public static final String OTP_ISSUER = "feature.otp.issuer";
    public static final String OTP_ENTROPY = "feature.otp.entropy";

    public static final String FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP = "domain.restricted.to.one.user.admin.enabled";
    public static final String MAX_OTP_DEVICE_PER_USER_PROP = "max.otp.device.per.user";
    public static final int MAX_OTP_DEVICE_PER_USER_DEFAULT = 5;

    public static final String BYPASS_DEFAULT_NUMBER = "multifactor.bypass.default.number";
    public static final String BYPASS_MAXIMUM_NUMBER = "multifactor.bypass.maximum.number";
    public static final String LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_PROP = "local.multifactor.bypass.num.iterations";
    public static final int LOCAL_MULTIFACTOR_BYPASS_NUM_ITERATION_DEFAULT = 10000;

    public static final String IMPLICIT_ROLE_PROP_PREFIX = "implicit.roles";
    public static final String IMPLICIT_ROLE_OVERRIDE_PROP_REG = IMPLICIT_ROLE_PROP_PREFIX + ".%s";


    public static final String FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP = "feature.enable.tenant.role.whitelist.visibility.filter";
    public static final boolean FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_DEFAULT = false;
    public static final String TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX = "tenant.role.whitelist.visibility.filter";
    public static final String TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP_REG = TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + ".%s";

    public static final String FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP = "feature.multifactor.locking.login.failure.ttl.in.seconds";
    public static final int FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_DEFAULT = 1800;
    public static final String FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP = "feature.multifactor.locking.attempts.maximumNumber";
    public static final int FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_DEFAULT = 3;

    public static final String FEATURE_ENABLE_ALWAYS_RETURN_APPROVED_DOMAINIDS_FOR_LIST_IDPS_PROP = "feature.enable.always.return.approved.domainids.for.list.idps";
    public static final boolean FEATURE_ENABLE_ALWAYS_RETURN_APPROVED_DOMAINIDS_FOR_LIST_IDPS_DEFAULT = true;

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

    public static final String MAPPING_POLICY_ACCEPT_FORMATS_PROP = "mapping.policy.accept.formats";
    public static final List<String> MAPPING_POLICY_ACCEPT_FORMATS_DEFAULT = Arrays.asList(MediaType.APPLICATION_JSON, GlobalConstants.TEXT_YAML);

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

    public static final String FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP = "feature.issue.restricted.token.session.ids";
    public static final boolean FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_DEFAULT = false;

    public static final String SESSION_ID_LIFETIME_PROP = "multifactor.sessionid.lifetime";
    public static final Integer SESSION_ID_LIFETIME_DEFAULT = 5;

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

    public static final String FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP = "feature.include.endpoints.based.on.rules";
    public static final boolean FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_DEFAULT = false;

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

    public static final String FEATURE_INFER_DEFAULT_TENANT_TYPE_PROP = "feature.infer.default.tenant.type";
    public static final boolean FEATURE_INFER_DEFAULT_TENANT_TYPE_DEFAULT = true;

    public static final String CACHE_CLIENT_ROLES_BY_ID_TTL_PROP = "cache.client.role.by.id.ttl";
    public static final Duration CACHE_CLIENT_ROLES_BY_ID_TTL_DEFAULT = Duration.parse("PT10M");

    public static final String CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP = "cache.client.role.by.id.size";
    public static final int CACHE_CLIENT_ROLES_BY_ID_SIZE_DEFAULT = 200;

    public static final String CACHE_USER_LOCKOUT_TTL_PROP = "ldap.auth.password.lockout.cache.ttl";
    public static final Duration CACHE_USER_LOCKOUT_TTL_DEFAULT = Duration.parse("PT2S");

    public static final String CACHE_USER_LOCKOUT_SIZE_PROP = "ldap.auth.password.lockout.cache.size";
    public static final int CACHE_USER_LOCKOUT_SIZE_DEFAULT = 200;

    public static final String FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_PROP = "feature.force.standard.v2.exceptions.end.user.services";
    public static final boolean FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_DEFAULT = true;

    public static final String MAX_TENANT_TYPE_SIZE_PROP = "max.tenant.type.size";
    public static final int MAX_TENANT_TYPE_SIZE_DEFAULT = 999;

    public static final String FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP = "feature.set.default.tenant.type.on.creation";
    public static final boolean FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_DEFAULT = false;

    public static final String FEATURE_ALLOW_USERNAME_UPDATE_PROP = "feature.allow.username.updates";
    public static final boolean FEATURE_ALLOW_USERNAME_UPDATE_DEFAULT = false;

    public static final String FEATURE_CACHE_ROLES_WITHOUT_APPLICATION_RESTART = "feature.cache.roles.without.application.restart";
    public static final boolean FEATURE_CACHE_ROLES_WITHOUT_APPLICATION_RESTART_DEFAULT = true;

    public static final String FEATURE_MAX_USER_GROUPS_IN_DOMAIN_PROP = "user.groups.max.in.domain";
    public static final int FEATURE_MAX_USER_GROUPS_IN_DOMAIN_DEFAULT = 20;

    public static final String FEATURE_ALLOW_UPDATE_DOMAIN_RCN_ON_UPDATE_DOMAIN_PROP = "allow.update.domain.rcn.on.update.domain";
    public static final boolean FEATURE_ALLOW_UPDATE_DOMAIN_RCN_ON_UPDATE_DOMAIN_DEFAULT = false;

    public static final String LIST_USERS_BY_ROLE_LIMIT_NAME = "list.users.by.role.limit";
    public static final int LIST_USERS_BY_ROLE_LIMIT_DEFAULT_VALUE = 100;

    public static final String FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP = "enable.user.groups.globally";
    public static final boolean FEATURE_ENABLE_USER_GROUPS_GLOBALLY_DEFAULT = false;

    public static final String FEATURE_ENABLE_USE_REPOSE_REQUEST_ID_PROP = "feature.enable.use.repose.request.id";
    public static final boolean FEATURE_ENABLE_USE_REPOSE_REQUEST_ID_DEFAULT = true;

    public static final String FEATURE_ENABLE_SEND_NEW_RELIC_CUSTOM_DATA_PROP = "feature.enable.send.new.relic.custom.data";
    public static final boolean FEATURE_ENABLE_SEND_NEW_RELIC_CUSTOM_DATA_DEFAULT = true;

    public static final String FEATURE_ENABLE_OPEN_TRACING_WEB_RESOURCES_PROP = "feature.enable.open.tracing.web.resources";
    public static final boolean FEATURE_ENABLE_OPEN_TRACING_WEB_RESOURCES_DEFAULT = true;

    public static final String FEATURE_ENABLE_OPEN_TRACING_DAO_RESOURCES_PROP = "feature.enable.open.tracing.dao.resources";
    public static final boolean FEATURE_ENABLE_OPEN_TRACING_DAO_RESOURCES_DEFAULT = true;

    public static final String FEATURE_OPEN_TRACING_INCLUDE_WEB_RESOURCES_PROP = "open.tracing.include.web.resources";
    public static final List<String> FEATURE_OPEN_TRACING_INCLUDE_WEB_RESOURCES_DEFAULT = Arrays.asList("*");

    public static final String FEATURE_OPEN_TRACING_EXCLUDE_WEB_RESOURCES_PROP = "open.tracing.exclude.web.resources";
    public static final List<String> FEATURE_OPEN_TRACING_EXCLUDE_WEB_RESOURCES_DEFAULT = Arrays.asList();

    public static final String FEATURE_INCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP = "new.relic.include.auth.resource.attributes";
    public static final List<String> FEATURE_INCLUDE_AUTH_RESOURCE_ATTRIBUTES_DEFAULT = Arrays.asList("*");

    public static final String FEATURE_EXCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP = "new.relic.exclude.auth.resource.attributes";
    public static final List<String> FEATURE_EXCLUDE_AUTH_RESOURCE_ATTRIBUTES_DEFAULT = Arrays.asList();

    public static final String FEATURE_INCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP = "new.relic.include.private.resource.attributes";
    public static final List<String> FEATURE_INCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_DEFAULT = Arrays.asList("*");

    public static final String FEATURE_EXCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP = "new.relic.exclude.private.resource.attributes";
    public static final List<String> FEATURE_EXCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_DEFAULT = Arrays.asList();

    public static final String FEATURE_INCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP = "new.relic.include.public.resource.attributes";
    public static final List<String> FEATURE_INCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_DEFAULT = Arrays.asList("*");

    public static final String FEATURE_EXCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP = "new.relic.exclude.public.resource.attributes";
    public static final List<String> FEATURE_EXCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_DEFAULT = Arrays.asList();

    public static final String NEW_RELIC_SECURED_API_RESOURCE_ATTRIBUTES_PROP = "new.relic.secured.api.resource.attributes";
    public static final List<String> NEW_RELIC_SECURED_API_RESOURCE_ATTRIBUTES_DEFAULT = Arrays.asList(NewRelicCustomAttributesEnum.CALLER_USERNAME.getNewRelicAttributeName()
            , NewRelicCustomAttributesEnum.CALLER_USER_TYPE.getNewRelicAttributeName()
            , NewRelicCustomAttributesEnum.EFFECTIVE_CALLER_USERNAME.getNewRelicAttributeName()
            , NewRelicCustomAttributesEnum.EFFECTIVE_CALLER_USER_TYPE.getNewRelicAttributeName());

    public static final String NEW_RELIC_SECURE_API_RESOURCE_KEY_PROP = "new.relic.secured.api.resource.key";
    public static final String NEW_RELIC_SECURE_API_RESOURCE_KEY_DEFAULT = "";

    public static final String NEW_RELIC_SECURED_API_USE_SHA256_PROP = "feature.enable.new.relic.sha256.hmac";
    public static final boolean NEW_RELIC_SECURED_API_USE_SHA256_DEFAULT = true;

    public static final String FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP = "tenant.prefixes.to.exclude.auto.assign.role.from";

    public static final String FEATURE_ENABLE_LIST_USERS_FOR_OWN_DOMAIN_ONLY_PROP = "feature.enable.list.users.for.own.domain.only";
    public static final boolean FEATURE_ENABLE_LIST_USERS_FOR_OWN_DOMAIN_ONLY_DEFAULT = true;

    public static final String FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP = "feature.enable.delegation.agreement.services";
    public static final boolean FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_DEFAULT = true;

    public static final String FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_PROP = "feature.enable.global.root.da.creation";
    public static final boolean FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_DEFAULT = false;


    public static final String FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP = "enable.delegation.agreements.for.all.rcns";
    public static final boolean FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_DEFAULT = false;

    public static final String FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_PROP = "feature.enable.grant.roles.to.user.service";
    public static final boolean FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_DEFAULT = true;

    public static final String FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP = "feature.enable.user.admin.look.up.by.domain";
    public static final boolean FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_DEFAULT = false;

    public static final String ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_PROP = "role.assignments.max.tenant.assignments.per.request";
    public static final int ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_DEFAULT = 10;

    public static final String FEATURE_ENABLE_DELEGATION_AUTHENTICATION_PROP = "feature.enable.delegation.authentication";
    public static final boolean FEATURE_ENABLE_DELEGATION_AUTHENTICATION_DEFAULT = false;

    public static final String FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP = "feature.enable.delegation.grant.roles.to.nested.da";
    public static final boolean FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_DEFAULT = false;


    public static final String FEATURE_DELEGATION_MAX_NUMBER_OF_DELEGATES_PER_DA_PROP = "delegation.max.number.of.delegates.per.da";
    public static final int FEATURE_DELEGATION_MAX_NUMBER_OF_DELEGATES_PER_DA_DEFAULT = 5;
    public static final String DELEGATION_MAX_NEST_LEVEL_PROP = "delegation.max.nest.level";
    public static final int DELEGATION_MAX_NEST_LEVEL_DEFAULT = 3;

    public static final String FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP = "delegation.max.number.of.da.per.principal";
    public static final int FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_DEFAULT = 5;

    public static final String FEATURE_ENABLE_AUTHORIZATION_ADVICE_ASPECT_PROP = "feature.enable.authorization.advice.aspect";
    public static final boolean FEATURE_ENABLE_AUTHORIZATION_ADVICE_ASPECT_DEFAULT  = true;

    public static final String FEATURE_ENABLE_ROLE_HIERARCHY_PROP = "feature.enable.role.hierarchy";
    public static final boolean FEATURE_ENABLE_ROLE_HIERARCHY_DEFAULT = true;

    public static final String NESTED_DELEGATION_AGREEMENT_ROLE_HIERARCHY_PROP = "nested.delegation.agreement.role.hierarchy";
    public static final String NESTED_DELEGATION_AGREEMENT_ROLE_HIERARCHY_DEFAULT = null;

    /**
     * Required static prop
     */
    public static final String ROLE_ID_RACKER_PROP = "cloudAuth.rackerRoleRsId";
    public static final String CLIENT_ID_FOUNDATION_PROP = "idm.clientId";

    public static final String MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME = "multifactor.key.location";
    public static final String MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT = "/etc/idm/config/keys";

    public static final String NAST_TENANT_PREFIX_PROP = "nast.tenant.prefix";
    public static final String NAST_TENANT_PREFIX_DEFAULT = "MossoCloudFS_";

    /* ************************
    FEEDS Connection Props. Feed calls are asynchronous so a larger default timeout is acceptable
     ************************** */
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

    public static final String LDAP_PAGING_LIMIT_DEFAULT_PROP = "ldap.paging.limit.default";
    public static final int LDAP_PAGING_LIMIT_DEFAULT_VALUE = 25;

    public static final String LDAP_PAGING_LIMIT_MAX_PROP = "ldap.paging.limit.max";
    public static final int LDAP_PAGING_LIMIT_MAX_DEFAULT = 1000;

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

    private static final String LDAP_SERVER_POOL_MIN_DISCONNECT_INTERVAL_TIME_PROP = "ldap.server.pool.min.disconnect.interval.time";
    private static final long LDAP_SERVER_POOL_MIN_DISCONNECT_INTERVAL_TIME_DEFAULT = 0L;

    private static final String LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_PROP = "ldap.server.pool.health.check.interval";
    private static final long LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_DEFAULT = 60000L;

    private static final String LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_PROP = "ldap.server.pool.check.connection.age.on.release";
    private static final boolean LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_DEFAULT = false;

    private static final String LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_PROP = "ldap.server.pool.allow.concurrent.socketfactory.use";
    private static final boolean LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_DEFAULT = false;

    public static final String FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP = "feature.enable.ldap.auth.password.lockout.cache";
    public static final boolean FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_DEFAULT = true;

    public static final String LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP = "ldap.auth.password.lockout.retries";
    public static final int LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_DEFAULT = 6;

    public static final String LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP = "ldap.auth.password.lockout.duration";
    public static final Duration LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_DEFAULT = Duration.parse("PT1S"); // In seconds

    private static final String EDIR_BIND_DN = "edir.bind.dn";
    private static final String EDIR_BIND_PASSWORD = "edir.bind.password";

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

    public static final String FEATURE_ENABLE_ISSUED_IN_RESPONSE_PROP = "feature.enable.issued_at.in.response";
    public static final boolean FEATURE_ENABLE_ISSUED_IN_RESPONSE_DEFAULT = true;

    public static final String FEATURE_SHOULD_DISPLAY_SERVICE_CATALOG_FOR_SUSPENDED_USER_IMPERSONATE_TOKENS_PROP = "feature.should.display.service.catalog.for.suspended.user.impersonate.tokens";
    public static final boolean FEATURE_SHOULD_DISPLAY_SERVICE_CATALOG_FOR_SUSPENDED_USER_IMPERSONATE_TOKENS_DEFAULT = false;

    public static final String FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP = "feature.enable.ldap.health.check.new.connection";
    public static final boolean FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_DEFAULT = false;

    public static final String FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP = "feature.enable.ldap.health.check.connection.for.continued.use";
    public static final boolean FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_DEFAULT = false;
  
    public static final String FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_PROPNAME = "feature.use.subtree.delete.control.for.subtree.deletion.enabled";
    public static final boolean FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_DEFAULT_VALUE = true;

    public static final String FEATURE_ENABLE_IGNORE_COMMENTS_FOR_SAML_PARSER_PROP = "feature.enable.ignore.comments.for.saml.parser";
    public static final boolean FEATURE_ENABLE_IGNORE_COMMENTS_FOR_SAML_PARSER_DEFAULT = true;

    public static final String FEATURE_ENABLE_INCLUDE_PASSWORD_EXPIRATION_DATE_PROP = "feature.enable.include.password.expiration.date";
    public static final boolean FEATURE_ENABLE_INCLUDE_PASSWORD_EXPIRATION_DATE_DEFAULT = false;

    public static final String FEATURE_POST_CREDENTIAL_FEED_EVENTS_ENABLED_PROP = "feature.enable.post.credential.feed.events";
    public static final boolean FEATURE_POST_CREDENTIAL_FEED_EVENTS_ENABLED_DEFAULT = false;

    public static final String FEATURE_DELETE_ALL_TENANTS_WHEN_TENANT_IS_REMOVED_FROM_DOMAIN_PROP = "feature.delete.all.tenants.when.tenant.removed.from.domain";
    public static final boolean FEATURE_DELETE_ALL_TENANTS_WHEN_TENANT_IS_REMOVED_FROM_DOMAIN_DEFAULT = true;

    public static final String FEATURE_ENABLE_CREATE_INVITES_PROP = "feature.enable.create.invites";
    public static final boolean FEATURE_ENABLE_CREATE_INVITES_DEFAULT = false;

    /**
     * Identity Repository Properties
     */
    public static final String FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP = "federation.identity.provider.default.policy";

    public static final String ENABLED_DOMAINS_FOR_USER_GROUPS_PROP = "enable.user.groups.for.domains";
    public static final String ENABLED_DOMAINS_FOR_USER_GROUPS_DEFAULT = "";

    public static final String ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_PROP = "enable.delegation.agreements.for.rcns";
    public static final String ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_DEFAULT = "";

    public static final String FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP = "feature.enable.phone.pin.on.user";
    public static final boolean FEATURE_ENABLE_PHONE_PIN_ON_USER_DEFAULT = false;

    public static final String USER_PHONE_PIN_SIZE_PROP = "user.phone.pin.size";
    public static final int USER_PHONE_PIN_SIZE_DEFAULT = 6;

    public static final String EDIR_LDAP_SERVER_TRUSTED_PROP = "ldap.server.trusted";
    public static final boolean EDIR_LDAP_SERVER_TRUSED_DEFAULT = false;

    public static final String INVITES_SUPPORTED_FOR_RCNS_PROP = "invites.supported.for.rcns";
    public static final String INVITES_SUPPORTED_FOR_RCNS_DEFAULT = "";


    /**
     * Opentracing properties
     */

    public static final String OPENTRACING_ENABLED_PROP = "opentracing.enabled";
    public static final boolean OPENTRACING_ENABLED_DEFAULT = false;

    public static final String OPENTRACING_SERVICE_NAME_PROP = "opentracing.service.name";
    public static final String OPENTRACING_SERVICE_NAME_DEFAULT = "Customer Identity API";

    public static final String OPENTRACING_TRACER_PROP = "opentracing.tracer";
    public static final OpenTracingTracerEnum OPENTRACING_TRACER_DEFAULT = OpenTracingTracerEnum.JAEGER;

    public static final String OPENTRACING_AGENT_HOST_PROP = "opentracing.agent.host";
    public static final String OPENTRACING_AGENT_PORT_PROP = "opentracing.agent.port";
    public static final String OPENTRACING_COLLECTOR_ENDPOINT_PROP = "opentracing.collector.endpoint";
    public static final String OPENTRACING_COLLECTOR_USERNAME_PROP = "opentracing.collector.username";
    public static final String OPENTRACING_COLLECTOR_PASSWORD_PROP = "opentracing.collector.password";
    public static final String OPENTRACING_COLLECTOR_TOKEN_PROP = "opentracing.collector.token";
    public static final String OPENTRACING_CONSTANT_TOGGLE_PROP = "opentracing.sampling.constant.toggle";
    public static final String OPENTRACING_RATE_LIMITING_LIMIT_PROP = "opentracing.sampling.rate.limiting.traces_per_second";
    public static final String OPENTRACING_PROBABILITY_PROP = "opentracing.sampling.probability";

    public static final String OPENTRACING_LOGGING_ENABLED_PROP = "opentracing.logging.enabled";
    public static final boolean OPENTRACING_LOGGING_ENABLED_DEFAULT = false;

    public static final String OPENTRACING_FLUSH_INTERVAL_MS_PROP = "opentracing.flush.interval.ms";
    public static final Integer OPENTRACING_FLUSH_INTERVAL_MS_DEFAULT = 1000; // 1 second

    public static final String OPENTRACING_MAX_BUFFER_SIZE_PROP = "opentracing.max.buffer.size";
    public static final Integer OPENTRACING_MAX_BUFFER_SIZE_DEFAULT = 10000; // 10k


    @Qualifier("staticConfiguration")
    @Autowired
    private Configuration staticConfiguration;

    @Qualifier("reloadableConfiguration")
    @Autowired
    private Configuration reloadableConfiguration;

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
        defaults.put(PROPERTY_RELOADABLE_PROPERTY_TTL_PROP_NAME, PROPERTY_RELOADABLE_PROPERTY_TTL_DEFAULT_VALUE);
        defaults.put(IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, "UUID");
        defaults.put(KEYCZAR_DN_CONFIG, "ou=keystore,o=configuration,dc=rackspace,dc=com");
        defaults.put(EXPOSE_V11_ADD_BASE_URL_PROP, true);
        defaults.put(FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP, false);
        defaults.put(OTP_ISSUER, "Rackspace");
        defaults.put(OTP_ENTROPY, 25);
        defaults.put(FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP, false);
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
        defaults.put(IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT);
        defaults.put(IDENTITY_FEDERATED_MAX_IDP_PER_DOMAIN_PROP, IDENTITY_FEDERATED_MAX_IDP_PER_DOMAIN_DEFAULT);
        defaults.put(IDP_MAX_SEACH_RESULT_SIZE_PROP, IDP_MAX_SEACH_RESULT_SIZE_DEFAULT);
        defaults.put(FEDERATED_DELTA_EXPIRATION_SECONDS_PROP, FEDERATED_DELTA_EXPIRATION_SECONDS_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_MAX_DELAY_PROP, FEATURE_FEDERATION_DELETION_MAX_DELAY_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_MAX_COUNT_PROP, FEATURE_FEDERATION_DELETION_MAX_COUNT_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_ROLE_PROP, FEATURE_FEDERATION_DELETION_ROLE_DEFAULT);
        defaults.put(FEATURE_FEDERATION_DELETION_TIMEOUT_PROP, FEATURE_FEDERATION_DELETION_TIMEOUT_DEFAULT);
        defaults.put(FEATURE_SUPPORT_V11_LEGACY_PROP, FEATURE_SUPPORT_V11_LEGACY_DEFAULT);

        defaults.put(FEATURE_ENABLE_LIST_USERS_FOR_OWN_DOMAIN_ONLY_PROP, FEATURE_ENABLE_LIST_USERS_FOR_OWN_DOMAIN_ONLY_DEFAULT);

        defaults.put(FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_DEFAULT);
        defaults.put(FORGOT_PWD_VALID_PORTALS_PROP_NAME, FORGOT_PWD_VALID_PORTALS_DEFAULT);
        defaults.put(EMAIL_HOST, EMAIL_HOST_DEFAULT);
        defaults.put(EMAIL_PORT, EMAIL_PORT_DEFAULT);
        defaults.put(EMAIL_HOST_USERNAME_PROP, EMAIL_HOST_USERNAME_DEFAULT);
        defaults.put(EMAIL_HOST_PASSWORD_PROP, EMAIL_HOST_PASSWORD_DEFAULT);

        defaults.put(FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_PROP, FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_DEFAULT);

        defaults.put(FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_DEFAULT);
        defaults.put(SESSION_ID_LIFETIME_PROP, SESSION_ID_LIFETIME_DEFAULT);

        defaults.put(MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME, MULTIFACTOR_ENCRYPTION_KEY_LOCATION_DEFAULT);
        defaults.put(EMAIL_FROM_EMAIL_ADDRESS, EMAIL_FROM_EMAIL_ADDRESS_DEFAULT);

        defaults.put(FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP, FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_DEFAULT);

        defaults.put(PURGE_TRRS_MAX_DELAY_PROP, PURGE_TRRS_MAX_DELAY_DEFAULT);

        defaults.put(PURGE_TRRS_MAX_LIMIT_PROP, PURGE_TRRS_MAX_LIMIT_DEFAULT);
        defaults.put(PURGE_TRRS_OBSOLETE_AFTER_PROP, PURGE_TRRS_OBSOLETE_AFTER_DEFAULT);

        defaults.put(FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP, FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_DEFAULT);
        defaults.put(FEATURE_REUSE_JAXB_CONTEXT, FEATURE_REUSE_JAXB_CONTEXT_DEFAULT);

        defaults.put(MAX_CA_DIRECTORY_PAGE_SIZE_PROP, MAX_CA_DIRECTORY_PAGE_SIZE_DEFAULT);

        defaults.put(FEEDS_MAX_CONNECTIONS_PROP, FEEDS_MAX_CONNECTIONS_DEFAULT);
        defaults.put(FEEDS_MAX_CONNECTIONS_PER_ROUTE_PROP, FEEDS_MAX_CONNECTIONS_PER_ROUTE_DEFAULT);
        defaults.put(FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_PROP, FEEDS_NEW_CONNECTION_SOCKET_TIMEOUT_MS_DEFAULT);
        defaults.put(FEEDS_SOCKET_TIMEOUT_MS_PROP, FEEDS_SOCKET_TIMEOUT_DEFAULT);
        defaults.put(FEEDS_CONNECTION_TIMEOUT_MS_PROP, FEEDS_CONNECTION_TIMEOUT_MS_DEFAULT);
        defaults.put(FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_PROP, FEEDS_CONNECTION_REQUEST_TIMEOUT_MS_DEFAULT);
        defaults.put(FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_PROP, FEEDS_ON_USE_EVICTION_VALIDATE_AFTER_MS_DEFAULT);

        defaults.put(FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_PROP, FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_DEFAULT);

        defaults.put(LDAP_SERVER_POOL_SIZE_INIT_PROP, LDAP_SERVER_POOL_SIZE_INIT_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_SIZE_MAX_PROP, LDAP_SERVER_POOL_SIZE_MAX_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_AGE_MAX_PROP, LDAP_SERVER_POOL_AGE_MAX_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_CREATE_IF_NECESSARY_PROP, LDAP_SERVER_POOL_CREATE_IF_NECESSARY_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_MAX_WAIT_TIME_PROP, LDAP_SERVER_POOL_MAX_WAIT_TIME_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_MIN_DISCONNECT_INTERVAL_TIME_PROP, LDAP_SERVER_POOL_MIN_DISCONNECT_INTERVAL_TIME_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_PROP, LDAP_SERVER_POOL_HEALTH_CHECK_INTERVAL_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_PROP, LDAP_SERVER_POOL_CHECK_CONNECTION_AGE_ON_RELEASE_DEFAULT);
        defaults.put(LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_PROP, LDAP_SERVER_POOL_ALLOW_CONCURRENT_SOCKETFACTORY_USE_DEFAULT);
        defaults.put(LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_DEFAULT);
        defaults.put(LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_DEFAULT);

        defaults.put(FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_DEFAULT);

        defaults.put(FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_DEFAULT);
        defaults.put(FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_PROP, FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_DEFAULT);
        defaults.put(FEATURE_POST_IDP_FEED_EVENTS_PROP, FEATURE_POST_IDP_FEED_EVENTS_DEFAULT);
        defaults.put(FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_PROP, FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_DEFAULT);
        defaults.put(FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_PROP, FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_DEFAULT);

        defaults.put(IDP_POLICY_MAX_KILOBYTE_SIZE_PROP, IDP_POLICY_MAX_KILOBYTE_SIZE_DEFAULT);
        defaults.put(FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_PROP, FEATURE_V2_FEDERATION_VALIDATE_ORIGIN_ISSUE_INSTANT_DEFAULT);

        defaults.put(FEATURE_ENABLE_ALWAYS_RETURN_APPROVED_DOMAINIDS_FOR_LIST_IDPS_PROP, FEATURE_ENABLE_ALWAYS_RETURN_APPROVED_DOMAINIDS_FOR_LIST_IDPS_DEFAULT);

        defaults.put(FEEDS_DEAMON_EVICTION_ENABLED_PROP, FEEDS_DEAMON_ENABLED_DEFAULT);
        defaults.put(FEEDS_DAEMON_EVICTION_FREQUENCY_MS_PROP, FEEDS_DAEMON_EVICTION_FREQUENCY_MS_DEFAULT);
        defaults.put(FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_PROP, FEEDS_DAEMON_EVICTION_CLOSE_IDLE_AFTER_MS_DEFAULT);
        defaults.put(FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_PROP, FEEDS_ALLOW_CONNECTION_KEEP_ALIVE_DEFAULT);
        defaults.put(FEEDS_CONNECTION_KEEP_ALIVE_MS_PROP, FEEDS_CONNECTION_KEEP_ALIVE_MS_DEFAULT);

        defaults.put(FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_PROP, FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_DEFAULT);

        defaults.put(DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_PROP, DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_DEFAULT);
        defaults.put(SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_PROP, SESSION_INACTIVITY_TIMEOUT_MAX_DURATION_DEFAULT);
        defaults.put(SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_PROP, SESSION_INACTIVITY_TIMEOUT_MIN_DURATION_DEFAULT);

        defaults.put(CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, CACHE_CLIENT_ROLES_BY_ID_TTL_DEFAULT);
        defaults.put(CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, CACHE_CLIENT_ROLES_BY_ID_SIZE_DEFAULT);
        defaults.put(CACHE_USER_LOCKOUT_TTL_PROP, CACHE_USER_LOCKOUT_TTL_DEFAULT);
        defaults.put(CACHE_USER_LOCKOUT_SIZE_PROP, CACHE_USER_LOCKOUT_SIZE_DEFAULT);

        defaults.put(FEATURE_INFER_DEFAULT_TENANT_TYPE_PROP, FEATURE_INFER_DEFAULT_TENANT_TYPE_DEFAULT);

        defaults.put(FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_PROP, FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_DEFAULT);
        defaults.put(MAX_TENANT_TYPE_SIZE_PROP, MAX_TENANT_TYPE_SIZE_DEFAULT);
        defaults.put(FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP, FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_DEFAULT);
        defaults.put(FEATURE_ALLOW_USERNAME_UPDATE_PROP, FEATURE_ALLOW_USERNAME_UPDATE_DEFAULT);

        defaults.put(NAST_TENANT_PREFIX_PROP, NAST_TENANT_PREFIX_DEFAULT);

        defaults.put(FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_DEFAULT);
        defaults.put(FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_DEFAULT);
        defaults.put(FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_DEFAULT);
        defaults.put(FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, FEATURE_MAINTAIN_PASSWORD_HISTORY_DEFAULT);
        defaults.put(PASSWORD_HISTORY_MAX_PROP, PASSWORD_HISTORY_MAX_DEFAULT);

        defaults.put(IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP, IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_DEFAULT);
        defaults.put(FEATURE_CACHE_ROLES_WITHOUT_APPLICATION_RESTART, FEATURE_CACHE_ROLES_WITHOUT_APPLICATION_RESTART_DEFAULT);
        defaults.put(FEATURE_MAX_USER_GROUPS_IN_DOMAIN_PROP, FEATURE_MAX_USER_GROUPS_IN_DOMAIN_DEFAULT);
        defaults.put(FEATURE_ALLOW_UPDATE_DOMAIN_RCN_ON_UPDATE_DOMAIN_PROP, FEATURE_ALLOW_UPDATE_DOMAIN_RCN_ON_UPDATE_DOMAIN_DEFAULT);
        defaults.put(LIST_USERS_BY_ROLE_LIMIT_NAME, LIST_USERS_BY_ROLE_LIMIT_DEFAULT_VALUE);

        defaults.put(MAPPING_POLICY_ACCEPT_FORMATS_PROP, MAPPING_POLICY_ACCEPT_FORMATS_DEFAULT);
        defaults.put(FEATURE_ENABLE_ISSUED_IN_RESPONSE_PROP, FEATURE_ENABLE_ISSUED_IN_RESPONSE_DEFAULT);

        defaults.put(LDAP_PAGING_LIMIT_DEFAULT_PROP, LDAP_PAGING_LIMIT_DEFAULT_VALUE);
        defaults.put(LDAP_PAGING_LIMIT_MAX_PROP, LDAP_PAGING_LIMIT_MAX_DEFAULT);
        defaults.put(FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, FEATURE_ENABLE_USER_GROUPS_GLOBALLY_DEFAULT);

        defaults.put(FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_DEFAULT);
        defaults.put(ENABLED_DOMAINS_FOR_USER_GROUPS_PROP, ENABLED_DOMAINS_FOR_USER_GROUPS_DEFAULT);
        defaults.put(ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_PROP, ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_DEFAULT);
        defaults.put(FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_DEFAULT);
        defaults.put(FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_PROP, FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_DEFAULT);
        defaults.put(DELEGATION_MAX_NEST_LEVEL_PROP, DELEGATION_MAX_NEST_LEVEL_DEFAULT);
        defaults.put(FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_DEFAULT);
        defaults.put(INVITES_SUPPORTED_FOR_RCNS_PROP, INVITES_SUPPORTED_FOR_RCNS_DEFAULT);

        defaults.put(FEATURE_ENABLE_USE_REPOSE_REQUEST_ID_PROP, FEATURE_ENABLE_USE_REPOSE_REQUEST_ID_DEFAULT);
        defaults.put(FEATURE_ENABLE_SEND_NEW_RELIC_CUSTOM_DATA_PROP, FEATURE_ENABLE_SEND_NEW_RELIC_CUSTOM_DATA_DEFAULT);

        defaults.put(FEATURE_ENABLE_OPEN_TRACING_WEB_RESOURCES_PROP, FEATURE_ENABLE_OPEN_TRACING_WEB_RESOURCES_DEFAULT);
        defaults.put(FEATURE_OPEN_TRACING_INCLUDE_WEB_RESOURCES_PROP, FEATURE_OPEN_TRACING_INCLUDE_WEB_RESOURCES_DEFAULT);
        defaults.put(FEATURE_OPEN_TRACING_EXCLUDE_WEB_RESOURCES_PROP, FEATURE_OPEN_TRACING_EXCLUDE_WEB_RESOURCES_DEFAULT);

        defaults.put(FEATURE_ENABLE_OPEN_TRACING_DAO_RESOURCES_PROP, FEATURE_ENABLE_OPEN_TRACING_DAO_RESOURCES_DEFAULT);

        defaults.put(FEATURE_INCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP, FEATURE_INCLUDE_AUTH_RESOURCE_ATTRIBUTES_DEFAULT);
        defaults.put(FEATURE_EXCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP, FEATURE_EXCLUDE_AUTH_RESOURCE_ATTRIBUTES_DEFAULT);

        defaults.put(FEATURE_INCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP, FEATURE_INCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_DEFAULT);
        defaults.put(FEATURE_EXCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP, FEATURE_EXCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_DEFAULT);

        defaults.put(FEATURE_INCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP, FEATURE_INCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_DEFAULT);
        defaults.put(FEATURE_EXCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP, FEATURE_EXCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_DEFAULT);

        defaults.put(NEW_RELIC_SECURED_API_RESOURCE_ATTRIBUTES_PROP, NEW_RELIC_SECURED_API_RESOURCE_ATTRIBUTES_DEFAULT);
        defaults.put(NEW_RELIC_SECURE_API_RESOURCE_KEY_PROP, NEW_RELIC_SECURE_API_RESOURCE_KEY_DEFAULT);
        defaults.put(NEW_RELIC_SECURED_API_USE_SHA256_PROP, NEW_RELIC_SECURED_API_USE_SHA256_DEFAULT);

        defaults.put(FEATURE_SHOULD_DISPLAY_SERVICE_CATALOG_FOR_SUSPENDED_USER_IMPERSONATE_TOKENS_PROP, FEATURE_SHOULD_DISPLAY_SERVICE_CATALOG_FOR_SUSPENDED_USER_IMPERSONATE_TOKENS_DEFAULT);
        defaults.put(FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_PROPNAME , FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_DEFAULT_VALUE);

        defaults.put(FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP, FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_DEFAULT);
        defaults.put(FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP, FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_DEFAULT);

        defaults.put(FEATURE_ENABLE_INCLUDE_PASSWORD_EXPIRATION_DATE_PROP, FEATURE_ENABLE_INCLUDE_PASSWORD_EXPIRATION_DATE_DEFAULT);
        defaults.put(FEATURE_ENABLE_IGNORE_COMMENTS_FOR_SAML_PARSER_PROP, FEATURE_ENABLE_IGNORE_COMMENTS_FOR_SAML_PARSER_DEFAULT);

        defaults.put(FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_PROP, FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_DEFAULT);

        defaults.put(FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, FEATURE_ENABLE_PHONE_PIN_ON_USER_DEFAULT);
        defaults.put(USER_PHONE_PIN_SIZE_PROP, USER_PHONE_PIN_SIZE_DEFAULT);

        defaults.put(EDIR_LDAP_SERVER_TRUSTED_PROP, EDIR_LDAP_SERVER_TRUSED_DEFAULT);

        defaults.put(FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_DEFAULT);
        defaults.put(ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_PROP, ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_DEFAULT);
        defaults.put(FEATURE_ENABLE_DELEGATION_AUTHENTICATION_PROP, FEATURE_ENABLE_DELEGATION_AUTHENTICATION_DEFAULT);
        defaults.put(FEATURE_DELEGATION_MAX_NUMBER_OF_DELEGATES_PER_DA_PROP, FEATURE_DELEGATION_MAX_NUMBER_OF_DELEGATES_PER_DA_DEFAULT);
        defaults.put(FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP, FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_DEFAULT);
        defaults.put(FEATURE_ENABLE_AUTHORIZATION_ADVICE_ASPECT_PROP, FEATURE_ENABLE_AUTHORIZATION_ADVICE_ASPECT_DEFAULT);
        defaults.put(FEATURE_POST_CREDENTIAL_FEED_EVENTS_ENABLED_PROP, FEATURE_POST_CREDENTIAL_FEED_EVENTS_ENABLED_DEFAULT);
        defaults.put(FEATURE_ENABLE_ROLE_HIERARCHY_PROP, FEATURE_ENABLE_ROLE_HIERARCHY_DEFAULT);
        defaults.put(NESTED_DELEGATION_AGREEMENT_ROLE_HIERARCHY_PROP, NESTED_DELEGATION_AGREEMENT_ROLE_HIERARCHY_DEFAULT);
        defaults.put(FEATURE_DELETE_ALL_TENANTS_WHEN_TENANT_IS_REMOVED_FROM_DOMAIN_PROP, FEATURE_DELETE_ALL_TENANTS_WHEN_TENANT_IS_REMOVED_FROM_DOMAIN_DEFAULT);
        defaults.put(FEATURE_ENABLE_CREATE_INVITES_PROP, FEATURE_ENABLE_CREATE_INVITES_DEFAULT);

        /**
         * OpenTracing defaults
         */

        defaults.put(OPENTRACING_ENABLED_PROP, OPENTRACING_ENABLED_DEFAULT);
        defaults.put(OPENTRACING_SERVICE_NAME_PROP, OPENTRACING_SERVICE_NAME_DEFAULT);
        defaults.put(OPENTRACING_TRACER_PROP, OPENTRACING_TRACER_DEFAULT);
        defaults.put(OPENTRACING_LOGGING_ENABLED_PROP, OPENTRACING_LOGGING_ENABLED_DEFAULT);
        defaults.put(OPENTRACING_FLUSH_INTERVAL_MS_PROP, OPENTRACING_MAX_BUFFER_SIZE_DEFAULT);
        defaults.put(OPENTRACING_MAX_BUFFER_SIZE_PROP, OPENTRACING_MAX_BUFFER_SIZE_DEFAULT);

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


        /**
         * OpenTracing properties
         */
        verifyAndLogStaticProperty(OPENTRACING_ENABLED_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_SERVICE_NAME_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_TRACER_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_AGENT_HOST_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_AGENT_PORT_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_COLLECTOR_ENDPOINT_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_COLLECTOR_USERNAME_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_COLLECTOR_PASSWORD_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_COLLECTOR_TOKEN_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_CONSTANT_TOGGLE_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_RATE_LIMITING_LIMIT_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_PROBABILITY_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_LOGGING_ENABLED_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_FLUSH_INTERVAL_MS_PROP, OPTIONAL);
        verifyAndLogStaticProperty(OPENTRACING_MAX_BUFFER_SIZE_PROP, OPTIONAL);
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

    private Double getDoubleSafely(Configuration config, String prop) {
        Object defaultValue = propertyDefaults.get(prop);
        try {
            if (defaultValue == null) {
                return config.getDouble(prop);
            } else {
                return config.getDouble(prop, (Double) defaultValue);
            }
        } catch (NumberFormatException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (Double) defaultValue;
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, prop));
            return (Double) defaultValue;
        }
    }

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

    private String getRepositoryStringSafely(String propertyName) {
        Object defaultValue = propertyDefaults.get(propertyName);
        try {
            IdentityProperty identityProperty = identityPropertyService.getIdentityPropertyByName(propertyName);
            if (identityProperty != null) {
                return (String) propertyValueConverter.convertPropertyValue(identityProperty);
            } else {
                return (String) defaultValue;
            }
        } catch (ConversionException e) {
            logger.error(String.format(INVALID_PROPERTY_ERROR_MESSAGE, propertyName));
            return (String) defaultValue;
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
        return TokenFormat.UUID;
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

        @IdmProp(key = MAX_NUM_USERS_IN_DOMAIN, description = "The max number of users allowed in a domain.", versionAdded = "1.0.14.8")
        public int getMaxNumberOfUsersInDomain() {
            return getIntSafely(staticConfiguration, MAX_NUM_USERS_IN_DOMAIN);
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
            return convertToTokenFormat(getStringSafely(staticConfiguration, IDENTITY_PROVISIONED_TOKEN_FORMAT));
        }

        @IdmProp(key = LDAP_PAGING_LIMIT_DEFAULT_PROP)
        public int getLdapPagingDefault() {
            return getIntSafely(staticConfiguration, LDAP_PAGING_LIMIT_DEFAULT_PROP);
        }

        @IdmProp(key = LDAP_PAGING_LIMIT_MAX_PROP)
        public int getLdapPagingMaximum() {
            return getIntSafely(staticConfiguration, LDAP_PAGING_LIMIT_MAX_PROP);
        }

        @IdmProp(key = KEYCZAR_DN_CONFIG)
        public String getKeyCzarDN() {
            return getStringSafely(staticConfiguration, KEYCZAR_DN_CONFIG);
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

        public Set<IdentityRole> getImplicitRolesForRole(String roleName) {
            Set<IdentityRole> result = Collections.EMPTY_SET;

            String[] implicitRolesNames = null;
            implicitRolesNames = staticConfiguration.getStringArray(String.format(IMPLICIT_ROLE_OVERRIDE_PROP_REG, roleName));

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

        @IdmProp(key = EDIR_BIND_DN, versionAdded = "3.15.0", description = "The bind DN for eDir authenticated connections.")
        public String getEdirBindDn() {
            return getStringSafely(staticConfiguration, EDIR_BIND_DN);
        }

        // Intentionally did not include an @IdmProp annotation here. That would cause this property to be exposed in the Identity props API in plain text
        public String getEdirBindPassword() {
            return getStringSafely(staticConfiguration, EDIR_BIND_PASSWORD);
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

        @IdmProp(key = CACHE_USER_LOCKOUT_TTL_PROP, versionAdded = "3.19.0" , description = "The ttl of entries in the user lockout cache. A ttl of 0 means no cache.")
        public Duration getUserLockoutCacheTtl() {
            return getDurationSafely(staticConfiguration, CACHE_USER_LOCKOUT_TTL_PROP);
        }

        @IdmProp(key = CACHE_USER_LOCKOUT_SIZE_PROP, versionAdded = "3.19.0" , description = "The max size of the user lockout cache.")
        public int getUserLockoutCacheSize() {
            return getIntSafely(staticConfiguration, CACHE_USER_LOCKOUT_SIZE_PROP);
        }

        @IdmProp(key = NAST_TENANT_PREFIX_PROP, versionAdded = "1.0.14.8"
                , description = "The prefix to append to nast tenant ids")
        public String getNastTenantPrefix() {
            return getStringSafely(staticConfiguration, NAST_TENANT_PREFIX_PROP);
        }

        @IdmProp(key = LIST_USERS_BY_ROLE_LIMIT_NAME, versionAdded = "1.0.14.8", description = "The limit to the number of users allowed to be assigned a role before the v2 list users with role API call will return an error.")
        public int getUsersByRoleLimit() {
            return getIntSafely(staticConfiguration, LIST_USERS_BY_ROLE_LIMIT_NAME);
        }

        @IdmProp(key = LDAP_SERVER_POOL_MIN_DISCONNECT_INTERVAL_TIME_PROP, versionAdded = "3.19.0", description = "Specifies the minimum length of time in milliseconds that should pass between connections closed because they have been established for longer than the maximum connection age.")
        public long getLDAPServerPoolMinDisconnectIntervalTime() {
            return getLongSafely(staticConfiguration, LDAP_SERVER_POOL_MIN_DISCONNECT_INTERVAL_TIME_PROP);
        }

        @IdmProp(key = EDIR_LDAP_SERVER_TRUSTED_PROP, versionAdded = "1.0.14.8", description = "Specifies if the edir connection is trusted")
        public boolean getEDirServerTrusted() {
            return getBooleanSafely(staticConfiguration, EDIR_LDAP_SERVER_TRUSTED_PROP);
        }

        /**
         * OpenTracing configurations
         */
        
        @IdmProp(key = OPENTRACING_ENABLED_PROP, versionAdded = "3.24.0", description = "Enable OpenTracing")
        public boolean getOpenTracingEnabledFlag() {
            return getBooleanSafely(staticConfiguration, OPENTRACING_ENABLED_PROP);
        }

        @IdmProp(key = OPENTRACING_SERVICE_NAME_PROP, versionAdded = "3.24.0", description = "Set OpenTracing service name")
        public String getOpenTracingServiceName() {
            return getStringSafely(staticConfiguration, OPENTRACING_SERVICE_NAME_PROP);
        }

        @IdmProp(key = OPENTRACING_TRACER_PROP, versionAdded = "3.24.0", description = "Set OpenTracing tracer (e.g. jaeger)")
        public OpenTracingTracerEnum getOpenTracingTracer() {
            return getEnumSafely(staticConfiguration, OPENTRACING_TRACER_PROP, OpenTracingTracerEnum.class);
        }

        @IdmProp(key = OPENTRACING_AGENT_HOST_PROP, versionAdded = "3.24.0", description = "Set OpenTracing agent host (when using agent setup)")
        public String getOpenTracingAgentHost() {
            return getStringSafely(staticConfiguration, OPENTRACING_AGENT_HOST_PROP);
        }

        @IdmProp(key = OPENTRACING_AGENT_PORT_PROP, versionAdded = "3.24.0", description = "Set OpenTracing agent port (when using agent setup)")
        public Integer getOpenTracingAgentPort() {
            return getIntSafely(staticConfiguration, OPENTRACING_AGENT_PORT_PROP);
        }

        @IdmProp(key = OPENTRACING_COLLECTOR_ENDPOINT_PROP, versionAdded = "3.24.0", description = "Set OpenTracing collector endpoint (when pushing directly to collector)")
        public String getOpenTracingCollectorEndpoint() {
            return getStringSafely(staticConfiguration, OPENTRACING_COLLECTOR_ENDPOINT_PROP);
        }

        @IdmProp(key = OPENTRACING_COLLECTOR_USERNAME_PROP, versionAdded = "3.24.0", description = "Set OpenTracing collector username (optional basicauth when pushing directly to collector)")
        public String getOpenTracingCollectorUsername() {
            return getStringSafely(staticConfiguration, OPENTRACING_COLLECTOR_USERNAME_PROP);
        }

        @IdmProp(key = OPENTRACING_COLLECTOR_PASSWORD_PROP, versionAdded = "3.24.0", description = "Set OpenTracing collector password (optional basicauth when pushing directly to collector)")
        public String getOpenTracingCollectorPassword() {
            return getStringSafely(staticConfiguration, OPENTRACING_COLLECTOR_PASSWORD_PROP);
        }

        @IdmProp(key = OPENTRACING_COLLECTOR_TOKEN_PROP, versionAdded = "3.24.0", description = "Set OpenTracing collector token (optional bearer token when pushing directly to collector)")
        public String getOpenTracingCollectorToken() {
            return getStringSafely(staticConfiguration, OPENTRACING_COLLECTOR_TOKEN_PROP);
        }

        @IdmProp(key = OPENTRACING_CONSTANT_TOGGLE_PROP, versionAdded = "3.24.0", description = "Set OpenTracing constant sampling toggle (uses constant sampling)")
        public Integer getOpenTracingConstantToggle() {
            return getIntSafely(staticConfiguration, OPENTRACING_CONSTANT_TOGGLE_PROP);
        }

        @IdmProp(key = OPENTRACING_RATE_LIMITING_LIMIT_PROP, versionAdded = "3.24.0", description = "Set OpenTracing rate-limiting sampling traces per second limit (uses rate limiting sampling)")
        public Double getOpenTracingRateLimitingLimit() {
            return getDoubleSafely(staticConfiguration, OPENTRACING_RATE_LIMITING_LIMIT_PROP);
        }

        @IdmProp(key = OPENTRACING_PROBABILITY_PROP, versionAdded = "3.24.0", description = "Set OpenTracing probabilistic sampling probability (uses probabilistic sampling)")
        public Double getOpenTracingProbability() {
            return getDoubleSafely(staticConfiguration, OPENTRACING_PROBABILITY_PROP);
        }

        @IdmProp(key = OPENTRACING_LOGGING_ENABLED_PROP, versionAdded = "3.24.0", description = "Set OpenTracing logging")
        public Boolean getOpenTracingLoggingEnabled() {
            return getBooleanSafely(staticConfiguration, OPENTRACING_LOGGING_ENABLED_PROP);
        }

        @IdmProp(key = OPENTRACING_FLUSH_INTERVAL_MS_PROP, versionAdded = "3.24.0", description = "Set OpenTracing flush interval ms (flushes buffer of traces after this time)")
        public Integer getOpenTracingFlushIntervalMs() {
            return getIntSafely(staticConfiguration, OPENTRACING_FLUSH_INTERVAL_MS_PROP);
        }

        @IdmProp(key = OPENTRACING_MAX_BUFFER_SIZE_PROP, versionAdded = "3.24.0", description = "Set OpenTracing max buffer size (flushes buffer of traces when reached this size)")
        public Integer getOpenTracingMaxBufferSize() {
            return getIntSafely(staticConfiguration, OPENTRACING_MAX_BUFFER_SIZE_PROP);
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

        @IdmProp(key = FEATURE_ENABLE_IGNORE_COMMENTS_FOR_SAML_PARSER_PROP, versionAdded = "3.20.1", description = "Enable the document loader that loads saml object to strip comments when loading.")
        public boolean ignoreCommentsWhenLoadingSaml() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_IGNORE_COMMENTS_FOR_SAML_PARSER_PROP);
        }

        public String getNodeName() {
            return getAENodeNameForSignoff();
        }

        public TokenFormat getIdentityFederationRequestTokenFormatForIdp(String idpLabeledUri) {
            return convertToTokenFormat(reloadableConfiguration.getString(String.format(IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG, idpLabeledUri), "${" + IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP + "}"));
        }

        @IdmProp(key = IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, description = "When an override property does not exist for a given federated provider, this determines the token format to use for that provide's federated users. AE | UUID", versionAdded = "2.13.0")
        public TokenFormat getIdentityFederatedUserDefaultTokenFormat() {
            return convertToTokenFormat(getStringSafely(reloadableConfiguration, IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP));
        }

        @IdmProp(key = GROUP_DOMAINID_DEFAULT, description = "Default domain_id when creating a group", versionAdded = "3.0.0")
        public String getGroupDefaultDomainId() {
            return getStringSafely(reloadableConfiguration, GROUP_DOMAINID_DEFAULT);
        }

        @IdmProp(key = TENANT_DOMAINID_DEFAULT, description = "Default domain_id when creating a tenant", versionAdded = "3.0.0")
        public String getTenantDefaultDomainId() {
            return getStringSafely(reloadableConfiguration, TENANT_DOMAINID_DEFAULT);
        }

        @IdmProp(key = ENDPOINT_REGIONID_DEFAULT, description = "Default region_id when creating an endpoint", versionAdded = "3.0.0")
        public String getEndpointDefaultRegionId() {
            return getStringSafely(reloadableConfiguration, ENDPOINT_REGIONID_DEFAULT);
        }

        @IdmProp(key = IDENTITY_ROLE_TENANT_DEFAULT, description = "Identity role default tenant", versionAdded = "3.0.0")
        public String getIdentityRoleDefaultTenant() {
            return getStringSafely(reloadableConfiguration, IDENTITY_ROLE_TENANT_DEFAULT);
        }

        @IdmProp(key = FEATURE_CACHE_AE_TOKENS_PROP, versionAdded = "3.0.1")
        public Boolean cacheAETokens() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_CACHE_AE_TOKENS_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_USE_REPOSE_REQUEST_ID_PROP, versionAdded = "3.17.1", description = "Whether or not to use the value supplied in the X-Request-Id header as the log transaction id. If set to false (or set to true but the header is null or blank), Identity generates a GUUID for the transaction id.")
        public boolean isFeatureUseReposeRequestIdEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_USE_REPOSE_REQUEST_ID_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_SEND_NEW_RELIC_CUSTOM_DATA_PROP, versionAdded = "3.17.1", description = "Whether or not to push custom attributes to New Relic for each API transaction")
        public boolean isFeatureSendNewRelicCustomDataEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_SEND_NEW_RELIC_CUSTOM_DATA_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_OPEN_TRACING_WEB_RESOURCES_PROP, versionAdded = "3.24.0", description = "Whether or not to send open tracing data for web resources. Also requires 'opentracing.enabled' to be set to 'true'")
        public boolean isOpenTracingForWebResourcesFeatureEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_OPEN_TRACING_WEB_RESOURCES_PROP) &&
                    staticConfig.getOpenTracingEnabledFlag();
        }

        @IdmProp(key = FEATURE_ENABLE_OPEN_TRACING_DAO_RESOURCES_PROP, versionAdded = "3.24.0", description = "Whether or not to send open tracing data for dao resources. Requires 'feature.enable.open.tracing.web.resources' and 'opentracing.enabled' to also be enabled.")
        public boolean isOpenTracingForDaoResourcesFeatureEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_OPEN_TRACING_DAO_RESOURCES_PROP) &&
                    isOpenTracingForWebResourcesFeatureEnabled();
        }

        @IdmProp(key = FEATURE_OPEN_TRACING_INCLUDE_WEB_RESOURCES_PROP, versionAdded = "3.24.0", description = "The web resources to include for open tracing. '*' means all resources")
        public Set<String> getOpenTracingIncludedWebResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_OPEN_TRACING_INCLUDE_WEB_RESOURCES_PROP);
        }

        @IdmProp(key = FEATURE_OPEN_TRACING_EXCLUDE_WEB_RESOURCES_PROP, versionAdded = "3.24.0", description = "The web resources to exclude from open tracing (overrides inclusion). '*' means all resources")
        public Set<String> getOpenTracingExcludedWebResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_OPEN_TRACING_EXCLUDE_WEB_RESOURCES_PROP);
        }
            
        @IdmProp(key = FEATURE_INCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP, versionAdded = "3.19.0", description = "The custom attributes to push for auth api resources. '*' means all available")
        public Set<String> getIncludedNewRelicCustomDataAttributesForAuthResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_INCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP);
        }

        @IdmProp(key = FEATURE_EXCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP, versionAdded = "3.19.0", description = "The custom attributes to exclude from auth api resources (overrides inclusion). '*' means all available")
        public Set<String> getExcludedNewRelicCustomDataAttributesForAuthResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_EXCLUDE_AUTH_RESOURCE_ATTRIBUTES_PROP);
        }

        @IdmProp(key = FEATURE_INCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP, versionAdded = "3.19.0", description = "The custom attributes to push for private api resources. '*' means all available")
        public Set<String> getIncludedNewRelicCustomDataAttributesForPrivateResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_INCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP);
        }

        @IdmProp(key = FEATURE_EXCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP, versionAdded = "3.19.0", description = "The custom attributes to exclude from private api resources (overrides inclusion). '*' means all available")
        public Set<String> getExcludedNewRelicCustomDataAttributesForPrivateResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_EXCLUDE_PRIVATE_RESOURCE_ATTRIBUTES_PROP);
        }

        @IdmProp(key = FEATURE_INCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP, versionAdded = "3.19.0", description = "The custom attributes to push for public api resources. '*' means all available")
        public Set<String> getIncludedNewRelicCustomDataAttributesForPublicResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_INCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP);
        }

        @IdmProp(key = FEATURE_EXCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP, versionAdded = "3.19.0", description = "The custom attributes to exclude from public api resources (overrides inclusion). '*' means all available")
        public Set<String> getExcludedNewRelicCustomDataAttributesForPublicResources() {
            return getSetSafely(reloadableConfiguration, FEATURE_EXCLUDE_PUBLIC_RESOURCE_ATTRIBUTES_PROP);
        }

        /*
         * Commenting out the IdmProp as the value should not be returned via the devops query props service. Currently
         * all props annotated with @IdmProp are queryable and the values returned. Ideally could specify that the
         * values are "secured" and not to be returned (neither default nor effective).
         */
//        @IdmProp(key = NEW_RELIC_SECURE_API_RESOURCE_KEY_PROP, versionAdded = "3.17.1", description = "When secure attributes are enabled, the key to use for securing the props")
        public String getNewRelicSecuredApiResourceAttributesKey() {
            return getStringSafely(reloadableConfiguration, NEW_RELIC_SECURE_API_RESOURCE_KEY_PROP);
        }

        @IdmProp(key = NEW_RELIC_SECURED_API_RESOURCE_ATTRIBUTES_PROP, versionAdded = "3.17.1", description = "When secure attributes are enabled, a comma delimited list to secure")
        public Set<String> getNewRelicSecuredApiResourceAttributes() {
            return getSetSafely(reloadableConfiguration, NEW_RELIC_SECURED_API_RESOURCE_ATTRIBUTES_PROP);
        }

        @IdmProp(key = NEW_RELIC_SECURED_API_USE_SHA256_PROP, versionAdded = "3.18.0", description = "When secure attributes are enabled, whether to use SHA-256 HMAC or fallback to SHA1")
        public boolean getNewRelicSecuredApiResourceAttributesUsingSha256() {
            return getBooleanSafely(reloadableConfiguration, NEW_RELIC_SECURED_API_USE_SHA256_PROP);
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

        @IdmProp(key = FEATURE_ENABLE_ALWAYS_RETURN_APPROVED_DOMAINIDS_FOR_LIST_IDPS_PROP, versionAdded = "3.20.1", description = "Whether or not list idps should always return an approvedDomainId attribute for all idps.")
        public boolean listIdpsAlwaysReturnsApprovedDomainIds() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_ALWAYS_RETURN_APPROVED_DOMAINIDS_FOR_LIST_IDPS_PROP);
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

        @IdmProp(key = FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, versionAdded = "3.16.0", description = "Whether or not user groups are supported for all domains for management and considered during effective role calculation")
        public boolean areUserGroupsGloballyEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, versionAdded = "3.20.0", description = "Whether or not delegation agreement services are enabled")
        public boolean areDelegationAgreementServicesEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, versionAdded = "3.20.0", description = "Whether or not delegation agreements are supported for all rcns")
        public boolean areDelegationAgreementsEnabledForAllRcns() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, versionAdded = "3.22.0", description = "Whether or not to allow roles to be assigned to nested delegation agreements")
        public boolean canRolesBeAssignedToNestedDelegationAgreements() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_PROP, versionAdded = "3.23.0", description = "Whether or not to allow all users to create root delegation agreements. If false, then only user admin or above can create root DA")
        public boolean isGlobalRootDelegationAgreementCreationEnabled () {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_PROP);
        }

        public boolean areDelegationAgreementsEnabledForRcn(String rcn) {
            return reloadableConfig.areDelegationAgreementsEnabledForAllRcns()
                    || (!StringUtils.isBlank(rcn) && repositoryConfig.getRCNsExplicitlyEnabledForDelegationAgreements().contains(rcn.toLowerCase()));
        }

        @IdmProp(key = DELEGATION_MAX_NEST_LEVEL_PROP, versionAdded = "3.22.0", description = "The maximum allowed level of delegation agreement nesting allowed by the system.")
        public int getMaxDelegationAgreementNestingLevel() {
            return getIntSafely(reloadableConfiguration, DELEGATION_MAX_NEST_LEVEL_PROP);
        }


        @IdmProp(key = FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP, versionAdded = "3.17.0", description = "The list of tenant prefixes to exclude the auto-assigned (identity:tenant-access) role from.")
        public List<String> getTenantPrefixesToExcludeAutoAssignRoleFrom() {
            return getListSafely(reloadableConfiguration, FEATURE_TENANT_PREFIXES_TO_EXCLUDE_AUTO_ASSIGN_ROLE_FROM_PROP);
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

        @IdmProp(key = FEATURE_ENABLE_LIST_USERS_FOR_OWN_DOMAIN_ONLY_PROP, versionAdded = "3.20.0", description = "When enabled, list users service call can only be used to list users of own domain regardless of user type")
        public boolean restrictListUsersToOwnDomain() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_LIST_USERS_FOR_OWN_DOMAIN_ONLY_PROP);
        }

        @IdmProp(key = FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, versionAdded = "3.2.0", description = "Timeout for forgot password tokens")
        public int getForgotPasswordTokenLifetime() {
            return getIntSafely(reloadableConfiguration, FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME);
        }

        @IdmProp(key = FORGOT_PWD_VALID_PORTALS_PROP_NAME, versionAdded = "3.2.0", description = "Comma delimited list of valid portal values for forgot password")
        public Set<String> getForgotPasswordValidPortals() {
            return getSetSafely(reloadableConfiguration, FORGOT_PWD_VALID_PORTALS_PROP_NAME);
        }

        @IdmProp(key=EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, description = "(Migrated from static w/ fallback to static if not found in reloadable). Flag that restricts outgoing emails to only rackspace.com emails. This will prevent any emails from being sent from staging.", versionAdded = "3.2.0")
        public boolean isSendToOnlyRackspaceAddressesEnabled() {
            return getBooleanSafelyWithStaticFallBack(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES);
        }

        @IdmProp(key = EMAIL_FROM_EMAIL_ADDRESS, description = "Return email address to use when sending emails to customers. Was added as a static property in version 2.5.0, but was migrated to be a reloadable in this version.", versionAdded = "3.2.0")
        public String getEmailFromAddress() {
            return getStringSafely(reloadableConfiguration, EMAIL_FROM_EMAIL_ADDRESS);
        }

        @IdmProp(key = FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP, versionAdded = "3.4.0", description = "Whether or not to issued restricted AE Tokens w/ a sessionid scope for MFA X-Session-Ids")
        public boolean issueRestrictedTokenSessionIds() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ISSUE_RESTRICTED_TOKEN_SESSION_IDS_PROP);
        }

        @IdmProp(key = SESSION_ID_LIFETIME_PROP, versionAdded = "3.4.0", description = "Lifetime, in minutes, of MFA sessionIds. Was added as static prop in 2.2.0, but switched from to reloadable prop in version 3.4.0")
        public int getMfaSessionIdLifetime() {
            return getIntSafely(reloadableConfiguration, SESSION_ID_LIFETIME_PROP);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_ENCRYPT, versionAdded = "3.4.0", description = "Whether or not new AE Tokens can be issued. Was added as static prop in 2.12.0, but switched from to reloadable prop in version 3.4.0")
        public boolean getFeatureAETokensEncrypt() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_AE_TOKENS_ENCRYPT);
        }

        @IdmProp(key = FEATURE_AE_TOKENS_DECRYPT, versionAdded = "3.4.0", description = "Whether or not existing AE Tokens can be received. Was added as static prop in 2.12.0, but switched from to reloadable prop in version 3.4.0")
        public boolean getFeatureAETokensDecrypt() {
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

        @IdmProp(key = FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, versionAdded = "3.8.0", description = "When true, endpoints based on rules are included in 'authentication' and 'list endpoints for token'")
        public boolean includeEndpointsBasedOnRules() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP);
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

        @IdmProp(key = FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, versionAdded = "3.19.0" , description = "Whether or not to cache user pwd lockouts and not attempt to bind to LDAP again until the specified amount of time has elapsed. Initially only lockouts for disabled users are cached.")
        public boolean isLdapAuthPasswordLockoutCacheEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP);
        }

        @IdmProp(key = LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, versionAdded = "3.19.0" , description = "LDAP will lock" +
                " out a user after a threshold of invalid pwd auth attempts for a configured amount of time. This" +
                " setting is analogous to the LDAP setting 'password-retries' to specify how many attempts a user has until the" +
                " account is locked. This value must match the LDAP setting for a consistency with LDAP. If higher than" +
                " LDAP, the app will not log appropriate error messages and the lockout cache will not be used. If lower," +
                " the app will consider users locked out while CA doesn't.")
        public int getLdapAuthPasswordLockoutRetries() {
            return getIntSafely(reloadableConfiguration, LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP);
        }

        @IdmProp(key = LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, versionAdded = "3.19.0" , description = "LDAP will lock" +
                " out a user after a threshold of invalid pwd auth attempts for a configured amount of time. This" +
                " setting mirrors the LDAP setting in order to provide valid auth messages and to cache that the user" +
                " is locked out until the specified amount of time has elapsed without hitting LDAP again. This value" +
                " must match the LDAP configuration setting. If higher than LDAP, then the app will incorrectly think a user is" +
                " still locked out and not send the bind request to LDAP. ")
        public Duration getLdapAuthPasswordLockoutDuration() {
            return getDurationSafely(reloadableConfiguration, LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP);
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

        @IdmProp(key = PASSWORD_HISTORY_MAX_PROP, versionAdded = "3.12.0" , description = "The maximum number of password history entries Identity will store for a user. Will actually store 1 more than this to include the 'current' password.")
        public int getPasswordHistoryMax() {
            return getIntSafely(reloadableConfiguration, PASSWORD_HISTORY_MAX_PROP);
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

        @IdmProp(key = FEATURE_ALLOW_USERNAME_UPDATE_PROP, versionAdded = "3.13.0", description = "Whether to allow a provisioned user's username to be updated.")
        public boolean isUsernameUpdateAllowed() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ALLOW_USERNAME_UPDATE_PROP);
        }

        @IdmProp(key = IDENTITY_FEDERATED_MAX_IDP_PER_DOMAIN_PROP, versionAdded = "3.13.0", description = "Maximum number of explicit IDPs per domain")
        public int getIdentityFederatedMaxIDPPerDomain() {
            return getIntSafely(reloadableConfiguration, IDENTITY_FEDERATED_MAX_IDP_PER_DOMAIN_PROP);
        }


        @IdmProp(key = FEATURE_MAX_USER_GROUPS_IN_DOMAIN_PROP, versionAdded = "3.16.0", description = "Maximum number of user groups that can be created per domain")
        public int getMaxUsersGroupsPerDomain() {
            return getIntSafely(reloadableConfiguration, FEATURE_MAX_USER_GROUPS_IN_DOMAIN_PROP);
        }

        @IdmProp(key = FEATURE_ALLOW_UPDATE_DOMAIN_RCN_ON_UPDATE_DOMAIN_PROP, versionAdded = "3.16.0", description = "Whether to allow updating an domain's RCN using the update domain API call.")
        public boolean isUpdateDomainRcnOnUpdateDomainAllowed() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ALLOW_UPDATE_DOMAIN_RCN_ON_UPDATE_DOMAIN_PROP);
        }

        @IdmProp(key = IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP, versionAdded = "3.13.0", description = "Maximum number of explicit IDPs per domain")
        public boolean getEnableExternalUserIdpManagement() {
            return getBooleanSafely(reloadableConfiguration, IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP);
        }

        @IdmProp(key = FEATURE_CACHE_ROLES_WITHOUT_APPLICATION_RESTART, versionAdded = "3.15.0", description = "Whether or not to allow caching client roles retrieved by id/name instead of loading them into memory at application startup.")
        public boolean getCacheRolesWithoutApplicationRestartFlag() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_CACHE_ROLES_WITHOUT_APPLICATION_RESTART);
        }

        @IdmProp(key = MAPPING_POLICY_ACCEPT_FORMATS_PROP, versionAdded = "3.15.0", description = "Specify the acceptable media types for mapping policies")
        public Set getMappingPolicyAcceptFormats() {
            return getSetSafely(reloadableConfiguration, MAPPING_POLICY_ACCEPT_FORMATS_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_ISSUED_IN_RESPONSE_PROP, versionAdded = "3.15.0", description = "Specify if issued will be included in authenticate, impersonate and validate response.")
        public boolean getEnableIssuedInResponse() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_ISSUED_IN_RESPONSE_PROP);
        }

        @IdmProp(key = FEATURE_SHOULD_DISPLAY_SERVICE_CATALOG_FOR_SUSPENDED_USER_IMPERSONATE_TOKENS_PROP, versionAdded = "3.17.0", description = "Whether or not to filter the service catalog for impersonation tokens of suspended users (users that belong to a domain with all domain tenants disabled)")
        public boolean shouldDisplayServiceCatalogForSuspendedUserImpersonationTokens() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_SHOULD_DISPLAY_SERVICE_CATALOG_FOR_SUSPENDED_USER_IMPERSONATE_TOKENS_PROP);
        }

        @IdmProp(key = FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_PROPNAME, versionAdded = "3.18.0", description = "Whether to use subtree delete control for subtree deletion.")
        public boolean useSubtreeDeleteControlForSubtreeDeletion() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_USE_SUBTREE_DELETE_CONTROL_FOR_SUBTREE_DELETION_PROPNAME);
        }

        @IdmProp(key = FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP, versionAdded = "3.19.0", description = "Whether to enable health check on new LDAP connection.")
        public boolean getEnableLDAPHealthCheckNewConnection() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP, versionAdded = "3.19.0", description = "Whether to enable health check on valid connection for continued use.")
        public boolean getEnableLDAPHealthCheckConnectionForContinuedUse() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_INCLUDE_PASSWORD_EXPIRATION_DATE_PROP, versionAdded = "3.20.0", description = "Specifies whether to return the user's password expiration on get user by ID and get user by name responses.")
        public boolean isIncludePasswordExpirationDateForGetUserResponsesEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_INCLUDE_PASSWORD_EXPIRATION_DATE_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_PROP, versionAdded = "3.20.0", description = "Whether to enable the grant multiple roles to user service.")
        public boolean isGrantRolesToUserServiceEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, versionAdded = "3.21.0", description = "Specifies whether to generate and store a user's phone PIN when the user is created. Phone PINs are generated for provisioned users created through v2.0 create user calls and Domain federated users.")
        public boolean getEnablePhonePinOnUserFlag() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP);
        }

        @IdmProp(key = USER_PHONE_PIN_SIZE_PROP, versionAdded = "3.21.0", description = "Specifies the length, in characters, of the phone PIN.")
        public int getUserPhonePinSize() {
            return getIntSafely(reloadableConfiguration, USER_PHONE_PIN_SIZE_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, versionAdded = "3.21.0", description = "Whether to enable user-admin look up by domain.")
        public boolean isUserAdminLookUpByDomain() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP);
        }

        @IdmProp(key = ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_PROP, versionAdded = "3.21.1", description = "Maximum number tenant assignment in request that grant roles.")
        public int getRoleAssignmentsMaxTenantAssignmentsPerRequest() {
            return getIntSafely(reloadableConfiguration, ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_DELEGATION_AUTHENTICATION_PROP, versionAdded = "3.21.1", description = "Whether to allow authentication with a delegation agreement.")
        public boolean isDelegationAuthenticationEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_DELEGATION_AUTHENTICATION_PROP);
        }

        @IdmProp(key = FEATURE_DELEGATION_MAX_NUMBER_OF_DELEGATES_PER_DA_PROP, versionAdded = "3.22.0", description = "The maximum number of delegates allowed on a delegation agreement.")
        public int getDelegationMaxNumberOfDelegatesPerDa() {
            return getIntSafely(reloadableConfiguration, FEATURE_DELEGATION_MAX_NUMBER_OF_DELEGATES_PER_DA_PROP);
        }

        @IdmProp(key = FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP, versionAdded = "3.22.0", description = "The maximum number of delegation agreements per principal.")
        public int getDelegationMaxNumberOfDaPerPrincipal() {
            return getIntSafely(reloadableConfiguration, FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_AUTHORIZATION_ADVICE_ASPECT_PROP, versionAdded = "3.22.0", description = "Whether to enable authorization advice aspect")
        public boolean getAuthorizationAdviceAspectEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_AUTHORIZATION_ADVICE_ASPECT_PROP);
        }

        @IdmProp(key = FEATURE_POST_CREDENTIAL_FEED_EVENTS_ENABLED_PROP, versionAdded = "3.22.0", description = "Whether to post credential change events when a user's credentials are changed.")
        public boolean isPostCredentialChangeFeedEventsEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_POST_CREDENTIAL_FEED_EVENTS_ENABLED_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_ROLE_HIERARCHY_PROP, versionAdded = "3.23.0", description = "Whether to enable role hierarchy support.")
        public boolean isRoleHierarchyEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_ROLE_HIERARCHY_PROP);
        }

        /**
         * This property represents the sub-parents role relationship map used for the role hierarchy in nested DAs. The
         * value is pipe delimited representing a list of sub-parents role relationship which are delimited by a
         * semicolon. The key will represent the sub role name, and the value will represent the list of parent role
         * names delimited by a comma.
         * @return
         */
        @IdmProp(key = NESTED_DELEGATION_AGREEMENT_ROLE_HIERARCHY_PROP, versionAdded = "3.23.0", description = "Role assignment hierarchy for nested DAs")
        public Map<String, List<String>> getNestedDelegationAgreementRoleHierarchyMap() {
            String rawValue = getStringSafely(reloadableConfiguration, NESTED_DELEGATION_AGREEMENT_ROLE_HIERARCHY_PROP);
            Map<String, List<String>> nestedDARoleHierarchyMap = new HashMap<>();

            // Create role hierarchy map with key set to the sub role name and the value set the a string
            // representing the list of parent role names.
            Map<String, String> roleHierarchyMap = Splitter.on("|").omitEmptyStrings().withKeyValueSeparator(";").split(rawValue);

            // Convert parent role names into a list
            for (Map.Entry roleHierarchy : roleHierarchyMap.entrySet()) {
                nestedDARoleHierarchyMap.put(roleHierarchy.getKey().toString(), Splitter.on(",").omitEmptyStrings().splitToList(roleHierarchy.getValue().toString()));
            }

            return nestedDARoleHierarchyMap;
        }

        /**
         * Due to how Apache Configuration works with determining lists from a string value
         * @return
         */
        @IdmProp(key = TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX, versionAdded = "3.24.0", description = "Set of whitelisted roles per tenant type")
        public Map<String, Set<String>> getTenantTypeRoleWhitelistFilterMap() {
            Map<String, Set<String>> result = new HashMap<>();

            Iterator<String> propKeys = reloadableConfiguration.getKeys(TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX);

            while (propKeys.hasNext()) {
                String key = propKeys.next();
                String tenantType = StringUtils.removeStart(key, TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + ".");
                Set<String> visibilityRoles = getSetSafely(reloadableConfiguration, key);
                visibilityRoles.removeIf(String::isEmpty);
                result.put(tenantType, visibilityRoles);
            }

            return result;
        }

        @IdmProp(key = FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP, versionAdded = "3.24.0", description = "Whether to filter user tenants and roles based on role visibility filter.")
        public boolean isTenantRoleWhitelistVisibilityFilterEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP);
        }

        @IdmProp(key = FEATURE_DELETE_ALL_TENANTS_WHEN_TENANT_IS_REMOVED_FROM_DOMAIN_PROP, versionAdded = "3.23.0", description = "Whether to delete all tenant roles when tenant is removed from domain.")
        public boolean getDeleteAllTenantRolesWhenTenantIsRemovedFromDomain() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_DELETE_ALL_TENANTS_WHEN_TENANT_IS_REMOVED_FROM_DOMAIN_PROP);
        }

        @IdmProp(key = FEATURE_ENABLE_CREATE_INVITES_PROP, versionAdded = "3.24.0", description = "Whether to allow for the creation of invite users")
        public boolean isCreationOfInviteUsersEnabled() {
            return getBooleanSafely(reloadableConfiguration, FEATURE_ENABLE_CREATE_INVITES_PROP);
        }

    }

    public class RepositoryConfig {

        public IdentityProperty getIdentityProviderDefaultPolicy() {
            return identityPropertyService.getIdentityPropertyByName(FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP);
        }

        /**
         * This property represents a list of domains for which user group are explicitly enabled. The value for the prop
         * in the backend is comma delimited list of domainIds. This method will trim all whitespace from all individual
         * values in the list. Empty values are ignored. As an example:
         *
         * <ul>
         *     <li>"a,b" = ["a","b"]</li>
         *     <li>"  a  ,  b " = ["a","b"]</li>
         *     <li>"a,,b" = ["a","b"]</li>
         *     <li>",b" = ["b"]</li>
         *     <li>"," = []</li>
         * </ul>
         *
         * Note - Callers should consider this property in tandem with the reloadable property 'enable.user.groups.globally'
         * returned via getReloadableConfig().areUserGroupsGloballyEnabled(). When this latter property is set to true,
         * all groups are enabled.
         *
         * @return
         */
        @IdmProp(key = ENABLED_DOMAINS_FOR_USER_GROUPS_PROP, versionAdded = "3.16.0", description = "A comma delimited list of domains for which user groups will be allowed")
        public List<String> getExplicitUserGroupEnabledDomains() {
            String rawValue = getRepositoryStringSafely(ENABLED_DOMAINS_FOR_USER_GROUPS_PROP);
            List<String> domainIds = Collections.emptyList();
            if (StringUtils.isNotBlank(rawValue)) {
                domainIds = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(rawValue);
            }
            return domainIds;
        }

        /**
         * This property represents a list of RCNs for which delegation agreements are explicitly enabled. The value for the prop
         * in the backend is comma delimited list of RCNs. This method will trim all whitespace from all individual
         * values in the list. Empty values are ignored. As an example:
         *
         * <ul>
         *     <li>"a,b" = ["a","b"]</li>
         *     <li>"  a  ,  b " = ["a","b"]</li>
         *     <li>"a,,b" = ["a","b"]</li>
         *     <li>",b" = ["b"]</li>
         *     <li>"," = []</li>
         * </ul>
         *
         * Note - Callers should consider this property in tandem with the reloadable property 'enable.user.groups.globally'
         * returned via getReloadableConfig().areUserGroupsGloballyEnabled(). When this latter property is set to true,
         * all groups are enabled.
         *
         * @return
         */
        @IdmProp(key = ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_PROP, versionAdded = "3.20.0", description = "A comma delimited list of rcns for which delegation agreements will be allowed")
        public List<String> getRCNsExplicitlyEnabledForDelegationAgreements() {
            String rawValue = getRepositoryStringSafely(ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_PROP);
            return splitStringPropIntoList(rawValue);
        }

        /**
         * This property represents a list of RCNs for which the creation of invite users are explicitly enabled.
         * The value for the prop in the backend is a comma delimited list of RCNs. This method will trim all whitespace
         * from all individual values in the list. Empty values are ignored. As an example:
         *
         * <ul>
         *     <li>"a,b" = ["a","b"]</li>
         *     <li>"  a  ,  b " = ["a","b"]</li>
         *     <li>"a,,b" = ["a","b"]</li>
         *     <li>",b" = ["b"]</li>
         *     <li>"," = []</li>
         * </ul>
         *
         * Note - A wildcard of '*' can be used to indicate that all RCNs are allowed to create invite users.
         *
         * @return
         */
        @IdmProp(key = INVITES_SUPPORTED_FOR_RCNS_PROP, versionAdded = "3.24.0", description = "A comma delimited list of RCNs for which the creation of invite users will be allowed")
        public List<String> getInvitesSupportedForRCNs() {
            String rawValue = getRepositoryStringSafely(INVITES_SUPPORTED_FOR_RCNS_PROP);
            return splitStringPropIntoList(rawValue);
        }
    }

    /**
     * This method will take a string containing comma delimited values, split the values by the comma delimiter,
     * and trim all whitespace from all individual values in the list. Empty values are ignored. As an example:
     *
     * <ul>
     *     <li>"a,b" = ["a","b"]</li>
     *     <li>"  a  ,  b " = ["a","b"]</li>
     *     <li>"a,,b" = ["a","b"]</li>
     *     <li>",b" = ["b"]</li>
     *     <li>"," = []</li>
     * </ul>
     *
     * @param rawValue
     * @return
     */
    private List<String> splitStringPropIntoList(String rawValue) {
        if (StringUtils.isNotBlank(rawValue)) {
            rawValue = rawValue.toLowerCase();
        }

        List<String> values = Collections.emptyList();
        if (StringUtils.isNotBlank(rawValue)) {
            values = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(rawValue);
        }

        return values;
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
    public String getKeyCzarDN() {
        return getStaticConfig().getKeyCzarDN();
    }

    @Deprecated
    public int reloadableDocsTimeOutInSeconds() {
        return getStaticConfig().reloadableDocsTimeOutInSeconds();
    }

    @Deprecated
    public boolean getV11AddBaseUrlExposed() {
        return getStaticConfig().getV11AddBaseUrlExposed();
    }

    public String getConfigRoot() {
        return System.getProperty(CONFIG_FOLDER_SYS_PROP_NAME);
    }
}
