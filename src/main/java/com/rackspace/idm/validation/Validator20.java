package com.rackspace.idm.validation;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.IdentityProviderConverterCloudV20;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.TenantTypeDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.TenantType;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.entity.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.validator.constraints.impl.EmailValidator;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.NOT_AUTHORIZED;

@Component
public class Validator20 {

    public static final String USER_NULL_IMPERSONATION_ERROR_MSG = "User cannot be null for impersonation request";
    public static final String USERNAME_NULL_IMPERSONATION_ERROR_MSG = "Username cannot be null for impersonation request";
    public static final String USERNAME_EMPTY_IMPERSONATION_ERROR_MSG = "Username cannot be empty or blank";
    public static final String EXPIRE_IN_ELEMENT_LESS_ONE_IMPERSONATION_ERROR_MSG = "Expire in element cannot be less than 1.";
    public static final String EXPIRE_IN_ELEMENT_EXCEEDS_MAX_IMPERSONATION_ERROR_MSG = "Expire in element cannot be more than %s";
    public static final String ENDPOINT_TEMPLATE_EXTRA_ATTRIBUTES_ERROR_MSG = "If serviceId is provided, neither name nor type may be supplied.";
    public static final String ENDPOINT_TEMPLATE_ACCEPTABLE_ASSIGNMENT_TYPE_ERROR_MSG = "Assignment type must be specified; Acceptable values are: %s.";
    public static final String ENDPOINT_TEMPLATE_EMPTY_SERVICE_ID_ERROR_MSG = "A serviceId must be provided if assignmentType is supplied.";
    public static final String ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_ERROR_MSG = "Using attributes name and type is no longer supported on endpoint creation; Please use serviceId and assignmentType.";
    public static final String ENDPOINT_TEMPLATE_REQUIRED_ATTR_ERROR_MSG = "'serviceId' and 'RAX-AUTH:assignmentType' are required attributes.";
    public static final String TOKEN_CLOUD_AUTH_EXPIRATION_SECONDS_PROP_NAME = "token.cloudAuthExpirationSeconds";
    public static final String ROLE_NAME_INVALID = "Invalid role name. Naming convention is <product prefix>:<role name> " +
            "for product roles and <role name> for global roles. Both the prefix and role name must start with " +
            "an alphanumeric character. The rest of the prefix and role name can be comprised of alphanumeric " +
            "characters, '-', and '_'.";
    public static final Pattern ROLE_NAME_REGEX = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_\\-]*(?:\\:[a-zA-Z0-9][a-zA-Z0-9_\\-]*)?$");
    public static final String INVALID_IDENTITY_PROVIDER_NAME_ERROR_MSG = "Identity provider name must consist of only alphanumeric, '.', and '-' characters.";
    public static final String DUPLICATE_IDENTITY_PROVIDER_NAME_ERROR_MSG = "Identity provider with name %s already exist.";
    public static final String DUPLICATE_IDENTITY_PROVIDER_EMAIL_DOMAIN_ERROR_MSG = "Email domain '%s' already belongs to another identity provider.";
    public static final String INVALID_IDENTITY_PROVIDER_APPROVED_DOMAIN_ERROR_MSG = "The provided approved domain '%s' does not exist.";
    public static final String EMPTY_IDENTITY_PROVIDER_APPROVED_DOMAIN_ERROR_MSG = "ApprovedDomainIds must contain at least one valid domain Id.";
    public static final Pattern IDENTITY_PROVIDER_NAME_REGEX = Pattern.compile("[\\p{Alnum}.\\-_:']*");

    public static final String ERROR_TENANT_REQUIRED_WHEN_TYPE_IS_RCN = "Tenant types are required when role type is 'rcn'.";

    public static final String ERROR_TENANT_RCN_ROLE_MUST_HAVE_GLOBAL_ASSIGNMENT = "An RCN role must have global assignment.";

    public static final String ERROR_TENANT_TYPE_CANNOT_EXCEED_MAXIMUM = "A maximum of 16 unique tenant types can be assigned.";

    public static final String ERROR_TENANT_TYPE_DESCRIPTION_MUST_BE_CORRECT_SIZE = "TenantType description must possess a length > 0 and <= 255";

    public static final String ERROR_TENANT_TYPE_NAME_MUST_BE_SPECIFIED = "TenantType name must be specified";

    public static final String ERROR_TENANT_TYPE_WAS_NOT_FOUND = "TenantType with name: '%s' was not found.";

    public static final String ALL_TENANT_TYPE = "*";
    public static final String ERROR_TENANT_TYPE_INVALID_CHARS = "Tenant type can only contain lower case alphanumeric characters, underscores, and/or hyphens.";
    public static final String TENANT_TYPE_NAME_REGEX = "^[a-z|0-9|_|-]+$";
    public static final Pattern TENANT_TYPE_NAME_PATTERN = Pattern.compile(TENANT_TYPE_NAME_REGEX);
    public static final int TENANT_TYPE_NAME_MAX_LENGTH = 16;

    public static final String ERROR_TENANT_TYPE_MUST_BE_CORRECT_SIZE = "Tenant type must possess a length > 0 and <= " + TENANT_TYPE_NAME_MAX_LENGTH;

    private EmailValidator emailValidator = new EmailValidator();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int MAX_LENGTH_32 = 32;
    public static final int MAX_LENGTH_64 = 64;
    public static final int MAX_LENGTH_255 = 255;

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int MAX_GROUP_NAME = MAX_LENGTH_64;
    public static final int MAX_GROUP_DESC = 1000;
    public static final int MAX_USERNAME = 100;
    public static final int MAX_APIKEY = 100;

    public static final int MAX_ROLE_NAME = MAX_LENGTH_255;
    public static final int MAX_ROLE_DESC = MAX_LENGTH_255;

    public static final int MAX_RCN_LENGTH = MAX_LENGTH_32;

    public static final int MAX_IDENTITY_PROVIDER_NAME = Constants.MAX_255;
    public static final int MAX_IDENTITY_PROVIDER_ISSUER = Constants.MAX_255;
    public static final int MAX_IDENTITY_PROVIDER_AUTH_URL = Constants.MAX_255;
    public static final int MAX_IDENTITY_PROVIDER_DESCRIPTION = Constants.MAX_255;
    public static final String APPROVED_DOMAIN_GROUP_NAME = "approvedDomainGroup";
    public static final String APPROVED_DOMAINS = "approvedDomains";
    private static final String EMAIL_DOMAIN = "emailDomain";
    public static final String BROKER_DOMAIN_GROUP_NAME = "GLOBAL";

    public static final String REQUIRED_ATTR_MESSAGE = "'%s' is a required attribute";
    private static final String INVALID_ATTR_MESSAGE = "'%s' is not a valid attribute for this service";
    public static final String EMPTY_ATTR_MESSAGE = "If provided, '%s' attribute cannot be empty";

    public static final String BLANK_ERROR_MSG = "%s cannot be empty or blank.";
    public static final String WHITESPACE_CHARACTERS_ERROR_MSG = "%s cannot contain whitespace characters.";
    public static final String LENGTH_EXCEEDED_ERROR_MSG = "%s length cannot exceed %s characters";
    public static final String VALUE_EXCEEDED_ERROR_MSG = "%s value must be between %s and %s";

    public static final String ERROR_APPROVED_DOMAIN_GROUP_NAME_SHOULD_BE_GLOBAL = "When BROKER IDP is specified, the approvedDomainGroup must be set, and specified as GLOBAL";
    public static final String FEDERATION_IDP_POLICY_INVALID_JSON_ERROR_MESSAGE = "Policy contains invalid json.";
    public static final String FEDERATION_IDP_POLICY_MAX_SIZE_EXCEED_ERROR_MESSAGE = "Max size exceed. Policy file must be less than %s Kilobytes.";

    public static final String SESSION_INACTIVITY_TIMEOUT_RANGE_ERROR_MESSAGE = "Session inactivity timeout must be between %s and %s seconds.";

    @Autowired
    private TenantService tenantService;

    @Autowired
    private FederatedIdentityService federatedIdentityService;

    @Autowired
    private DomainService domainService;

    @Autowired
    Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private RoleService roleService;

    @Autowired
    private JsonValidator jsonValidator;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    TenantTypeDao tenantTypeDao;

    @Autowired
    IdentityProviderConverterCloudV20 identityProviderConverterCloudV20;

    @Autowired
    PasswordBlacklistService passwordBlacklistService;

    public void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            String errorMsg = "Expecting username";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        if (username.contains(" ")) {
            String errorMsg = "Username should not contain white spaces";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    public void validateUsernameForUpdateOrCreate(String username) {
        Pattern alphaNumberic = Pattern.compile("[a-zA-z0-9-_.@]*");
        if (!alphaNumberic.matcher(username).matches()) {
            throw new BadRequestException("Username has invalid characters.");
        }
    }

    public void validateEmail(String email) {

        if (StringUtils.isBlank(email) || !emailValidator.isValid(email, null)) {
            String errorMsg = "Expecting valid email address";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    public void validatePasswordCredentials(PasswordCredentialsBase passwordCredentials) {
        String username = passwordCredentials.getUsername();
        String password = passwordCredentials.getPassword();
        validateUsername(username);
        if (StringUtils.isBlank(password)) {
            String errMsg = "Expecting Password";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    public void validatePasswordForCreateOrUpdate(String password) {
        // TODO: we probably need to delete this method and use the password pattern validation in Validator.validatePasswordForCreateOrUpdate
        String errMsg = "Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character.";
        if (password.length() < PASSWORD_MIN_LENGTH) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[A-Z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[a-z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[0-9].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    public void validatePasswordCredentialsForCreateOrUpdate(PasswordCredentialsBase passwordCredentials) {
        validatePasswordCredentials(passwordCredentials);
        validatePasswordForCreateOrUpdate(passwordCredentials.getPassword());
        if (identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled()) {
            validatePasswordIsNotBlacklisted(passwordCredentials.getPassword());
        }
    }

    public void validateApiKeyCredentials(ApiKeyCredentials apiKeyCredentials) {
        validateUsername(apiKeyCredentials.getUsername());
        if (StringUtils.isBlank(apiKeyCredentials.getApiKey())) {
            String errMsg = "Expecting apiKey";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        validateStringMaxLength(JSONConstants.API_KEY, apiKeyCredentials.getApiKey(), MAX_APIKEY);
    }

    public void validateImpersonationRequestForRacker(ImpersonationRequest impersonationRequest) {
        validateImpersonationRequestInternal(impersonationRequest, config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_RACKER_MAX_SECONDS_PROP_NAME));
    }

    public void validateImpersonationRequestForService(ImpersonationRequest impersonationRequest) {
        validateImpersonationRequestInternal(impersonationRequest, config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME));
    }

    private void validateImpersonationRequestInternal(ImpersonationRequest impersonationRequest, int maxRequestedExpireTimeForType) {
        int maxUserTokenLifetime = config.getInt(TOKEN_CLOUD_AUTH_EXPIRATION_SECONDS_PROP_NAME);

        if (maxUserTokenLifetime < maxRequestedExpireTimeForType) {
            //if the max user token lifetime is less than the max impersonation token lifetime, must use the user token
            // lifetime as the impersonation max
            maxRequestedExpireTimeForType = maxUserTokenLifetime;
        }

        if (impersonationRequest.getUser() == null) {
            throw new BadRequestException(USER_NULL_IMPERSONATION_ERROR_MSG);
        } else if (impersonationRequest.getUser().getUsername() == null) {
            throw new BadRequestException(USERNAME_NULL_IMPERSONATION_ERROR_MSG);
        } else if (impersonationRequest.getUser().getUsername().isEmpty() || StringUtils.isBlank(impersonationRequest.getUser().getUsername())) {
            throw new BadRequestException(USERNAME_EMPTY_IMPERSONATION_ERROR_MSG);
        } else if (impersonationRequest.getExpireInSeconds() != null) {
            if (impersonationRequest.getExpireInSeconds() < 1) {
                throw new BadRequestException(EXPIRE_IN_ELEMENT_LESS_ONE_IMPERSONATION_ERROR_MSG);
            } else if (impersonationRequest.getExpireInSeconds() > maxRequestedExpireTimeForType) {
                throw new BadRequestException(String.format(EXPIRE_IN_ELEMENT_EXCEEDS_MAX_IMPERSONATION_ERROR_MSG, maxRequestedExpireTimeForType));
            }
        }
    }

    public void validateKsGroup(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group) {
        String checkName = group.getName();
        if (StringUtils.isBlank(checkName)) {
            throw new BadRequestException("Missing group name");
        }
        if (checkName.length() > MAX_GROUP_NAME) {
            throw new BadRequestException("Group name length cannot exceed " + MAX_GROUP_NAME + " characters");
        }

        if (group.getDescription() == null) {
            throw new BadRequestException("Missing description");
        }

        if (group.getDescription().length() > MAX_GROUP_DESC) {
            throw new BadRequestException("Group description length cannot exceed 1000 characters");
        }
    }

    public void validateTenantIdInRoles(String tenantId, List<TenantRole> roles) {
        if (!StringUtils.isBlank(tenantId) && !tenantService.isTenantIdContainedInTenantRoles(tenantId, roles)) {
            String errMsg = String.format("Token doesn't belong to Tenant with Id/Name: '%s'", tenantId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
    }

    public void validateToken(String token) {
        if (!token.matches("^[A-Za-z0-9-]+$")) {
            String errMsg = "Invalid token";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    public void validateEndpointTemplate(EndpointTemplate endpoint) {
        if (identityConfig.getReloadableConfig().getFeatureEndpointTemplateDisableNameType()) {
            if (endpoint.getName() != null || endpoint.getType() != null) {
                logger.info(ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_ERROR_MSG);
                throw new BadRequestException(ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_ERROR_MSG);
            }
            if (StringUtils.isBlank(endpoint.getServiceId()) || endpoint.getAssignmentType() == null) {
                logger.info(ENDPOINT_TEMPLATE_REQUIRED_ATTR_ERROR_MSG);
                throw new BadRequestException(ENDPOINT_TEMPLATE_REQUIRED_ATTR_ERROR_MSG);
            }
        }

        //need to verify that these values are supplied due to them being optional in the schema and the use of json
        if (!StringUtils.isBlank(endpoint.getServiceId())) {
            // Make sure that service name and type are empty when passing in a serviceId in endpointTemplate creation
            if (StringUtils.isNotBlank(endpoint.getName()) || StringUtils.isNotBlank(endpoint.getType())) {
                logger.warn(ENDPOINT_TEMPLATE_EXTRA_ATTRIBUTES_ERROR_MSG);
                throw new BadRequestException(ENDPOINT_TEMPLATE_EXTRA_ATTRIBUTES_ERROR_MSG);
            }
            // NOTE: Jaxb will set unsupported enum assignment type to null
            if (endpoint.getAssignmentType() == null) {
                String errMsg = String.format(ENDPOINT_TEMPLATE_ACCEPTABLE_ASSIGNMENT_TYPE_ERROR_MSG, Arrays.asList(EndpointTemplateAssignmentTypeEnum.values()));
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
        } else {
            // assignmentType cannot be provided without a serviceId.
            if (endpoint.getAssignmentType() != null && !StringUtils.isBlank(endpoint.getAssignmentType().value())) {
                logger.warn(ENDPOINT_TEMPLATE_EMPTY_SERVICE_ID_ERROR_MSG);
                throw new BadRequestException(ENDPOINT_TEMPLATE_EMPTY_SERVICE_ID_ERROR_MSG);
            }
            if (StringUtils.isBlank(endpoint.getType())) {
                throwBadRequestForMissingAttr("type");
            }
            if (StringUtils.isBlank(endpoint.getName())) {
                throwBadRequestForMissingAttr("name");
            }
        }
        if (endpoint.getId() == null) {
            throwBadRequestForMissingAttr("id");
        }
        if (StringUtils.isBlank(endpoint.getPublicURL())) {
            throwBadRequestForMissingAttr("publicURL");
        }
    }

    /**
     * Ideally would use the bean validation logic, but trying to do a quick and thorough implementation.
     *
     * @param identityProvider
     */
    public void validateIdentityProviderForCreation(IdentityProvider identityProvider) {
        if (identityProvider == null) {
            throw new BadRequestException("Must provide an identity provider");
        }

        validateIdentityProviderNameChangeWithDupCheck(identityProvider.getName(), null);

        validateIdentityProviderIssuerWithDupCheck(identityProvider);

        if (identityProvider.getFederationType() == null) {
            throwBadRequestForMissingAttrWithErrorCode("federationType");
        }

        //validate the authenticationUrl is specified and under max length
        validateIdentityProviderAuthenticationUrl(identityProvider);

        validateIdentityProviderDescription(identityProvider);

        if (StringUtils.isNotBlank(identityProvider.getId())) {
            throw new BadRequestException("Do not provide an id when creating a new Identity Provider. An id will be generated.");
        }

        validateIdentityProviderEmailDomainsOnCreation(identityProvider);

        PublicCertificates publicCertificatesWrapper = identityProvider.getPublicCertificates();
        if (publicCertificatesWrapper != null && CollectionUtils.isNotEmpty(publicCertificatesWrapper.getPublicCertificate())) {
            for (PublicCertificate publicCertificate : publicCertificatesWrapper.getPublicCertificate()) {
                validatePublicCertificate(publicCertificate);
            }
        }

        //validate approvedDomainGroup/approvedDomains. Can only provide one element
        String providedApprovedDomainGroup = identityProvider.getApprovedDomainGroup();
        ApprovedDomainIds providedApprovedDomainIds = identityProvider.getApprovedDomainIds();

        if (identityProvider.getFederationType() == IdentityProviderFederationTypeEnum.DOMAIN) {
            if (providedApprovedDomainGroup != null && providedApprovedDomainIds != null) {
                throw new BadRequestException(String.format("You must provide either %s or %s, but not both", APPROVED_DOMAIN_GROUP_NAME, APPROVED_DOMAINS), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS);
            } else if (providedApprovedDomainGroup == null && providedApprovedDomainIds == null) {
                throw new BadRequestException(String.format("You must provide either %s or %s", APPROVED_DOMAIN_GROUP_NAME, APPROVED_DOMAINS), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS);
            } else if (providedApprovedDomainGroup != null) {
                if (ApprovedDomainGroupEnum.lookupByStoredValue(providedApprovedDomainGroup) == null) {
                    throw new BadRequestException(String.format("The provided value is not a supported %s", APPROVED_DOMAIN_GROUP_NAME), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_GROUP);
                }
            } else {
                List<String> providedApprovedDomains = providedApprovedDomainIds != null ? providedApprovedDomainIds.getApprovedDomainId() : Collections.EMPTY_LIST;
                if (providedApprovedDomainIds != null && CollectionUtils.isEmpty(providedApprovedDomains)) {
                    throw new BadRequestException(String.format("When providing %s, you must provide a valid domain id", APPROVED_DOMAINS), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS);
                }
                validateIdentityProviderApprovedDomainIds(providedApprovedDomains);
            }
        } else if (identityProvider.getFederationType() == IdentityProviderFederationTypeEnum.BROKER) {
            if (providedApprovedDomainGroup == null || !providedApprovedDomainGroup.equals(BROKER_DOMAIN_GROUP_NAME)) {
                throw new BadRequestException(ERROR_APPROVED_DOMAIN_GROUP_NAME_SHOULD_BE_GLOBAL, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS);
            }
        } else {
            //racker provider don't contain approvedDomain stuff
            if (providedApprovedDomainGroup != null || providedApprovedDomainIds != null) {
                throw new BadRequestException(String.format("%s and %s are not valid attributes for this federation type", APPROVED_DOMAIN_GROUP_NAME, APPROVED_DOMAINS), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS);
            }
        }
    }

    /**
     * Validates portions of the Identity Provider w/ knowledge that code pre-sets some values (e.g. approvedDomainIds)
     * so no need to validate that.
     *
     * Validates:
     * - name - (without dup check)
     * - issuer (with dup check)
     * - description (with length)
     * - auth url (length)
     * - pub certs (convertable to pub cert)
     *
     *
     * @param identityProvider
     * @param domainId
     */
    public void validateIdentityProviderMetadataForCreation(IdentityProvider identityProvider, String domainId) {
        validateIdentityProviderExplicitThreshold(domainId);

        // Validate names - except dup check. Calling method must ensure the name is globally unique
        // TODO Refactor all this update logic across all the different methods
        validateIdentityProviderNameWithoutDupCheck(identityProvider.getName());

        // Validate issuer
        validateIdentityProviderIssuerWithDupCheck(identityProvider);

        // Validate description
        validateIdentityProviderDescription(identityProvider);

        validateIdentityProviderAuthenticationUrl(identityProvider);

        validateIdentityProviderPublicCertificates(identityProvider);
    }

    public void validateIdentityProviderIssuerWithDupCheck(IdentityProvider identityProvider) {
        validateStringNotNullWithMaxLength("issuer", identityProvider.getIssuer(), MAX_IDENTITY_PROVIDER_ISSUER);
        if (federatedIdentityService.getIdentityProviderByIssuer(identityProvider.getIssuer()) != null) {
            throw new DuplicateException(ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS_MSG, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS);
        }
    }

    public void validateIdentityProviderAuthenticationUrl(IdentityProvider identityProvider) {
        // Validate authenticationUrl
        validateStringNotNullWithMaxLength("authenticationUrl",
                identityProvider.getAuthenticationUrl(),
                MAX_IDENTITY_PROVIDER_AUTH_URL);
    }

    public void validateIdentityProviderDescription(IdentityProvider identityProvider) {
        validateStringMaxLength("description", identityProvider.getDescription(), MAX_IDENTITY_PROVIDER_DESCRIPTION);
    }

    public void validateIdentityProviderPublicCertificates(IdentityProvider identityProvider) {
        // Validate public certificates
        PublicCertificates publicCertificatesWrapper = identityProvider.getPublicCertificates();
        if (publicCertificatesWrapper != null && CollectionUtils.isNotEmpty(publicCertificatesWrapper.getPublicCertificate())) {
            for (PublicCertificate publicCertificate : publicCertificatesWrapper.getPublicCertificate()) {
                validatePublicCertificate(publicCertificate);
            }
        }
    }

    private void validateIdentityProviderExplicitThreshold(String domainId) {
        List<com.rackspace.idm.domain.entity.IdentityProvider> identityProviderList =
                federatedIdentityService.findIdentityProvidersExplicitlyApprovedForDomain(domainId);

        if (identityProviderList.size() >= identityConfig.getReloadableConfig().getIdentityFederatedMaxIDPPerDomain()) {
            String errMsg = String.format("Maximum number of explicit IDPs already exist for domain %s.", domainId);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg, ErrorCodes.ERROR_CODE_IDP_LIMIT_PER_DOMAIN);
        }
    }

    /**
     * Validates the name meets name character restrictions, but does not perform a duplicate check.
     *
     * @param name
     */
    private void validateIdentityProviderNameWithoutDupCheck(String name) {
        validateStringNotNullWithMaxLength("name", name, MAX_IDENTITY_PROVIDER_NAME);

        if (!IDENTITY_PROVIDER_NAME_REGEX.matcher(name).matches()) {
            throw new BadRequestException(INVALID_IDENTITY_PROVIDER_NAME_ERROR_MSG, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
        }
    }

    /**
     * Verifies the supplied newName is an acceptable identity provider name.
     *
     * @param newName
     * @param existingProvider
     */
    private void validateIdentityProviderNameChangeWithDupCheck(String newName, com.rackspace.idm.domain.entity.IdentityProvider existingProvider) {
        /*
         If the existing provider is provided but the new name is null, the code will not update the name. A null value
         on the name field for an update will cause unboundId to not update the name. As such, don't need to perform
         any validation for this case.
         */
        if (existingProvider != null && newName == null) {
            return;
        }

        /*
            We need to do a full check on name:
            1. When existing provider is null as it means we are creating a new provider (so no existing name)
            2. If updating a name on an existing provider, perform name validation if the new name is different than the
             current name. Not performing the check when the name is the same was explicitly done to allow legacy IDPs
             with names that didn't match new restrictions to still be updated when a name is provided as long as the
             name matches the existing name.
         */
        if (existingProvider == null || !newName.equalsIgnoreCase(existingProvider.getName())) {
            validateIdentityProviderNameWithoutDupCheck(newName);
        }

        /*
         Always perform the dup check if caller provides a name. In probably 99.999% of the cases the same IDP will
         be returned. However, the call is inexpensive, IDPs aren't updated often, and the consequences of 2 IDPs being
         returned for the same name results in a DOS on both IDPs so we need to avoid that condition.

         If an existing provider isn't provided (meaning we're adding a new provider), ANY found idp will result in dup error
         If an existing provider is provided (meaning updating a provider), no error is thrown as long as the found IDP
         is the same provider.
         */
        com.rackspace.idm.domain.entity.IdentityProvider searchedIdp = federatedIdentityService.getIdentityProviderByName(newName);
        String acceptableFoundIdp = existingProvider != null ? existingProvider.getProviderId() : null;
        if (searchedIdp != null && !searchedIdp.getProviderId().equalsIgnoreCase(acceptableFoundIdp)) {
            throw new DuplicateException(String.format(DUPLICATE_IDENTITY_PROVIDER_NAME_ERROR_MSG, newName), ErrorCodes.ERROR_CODE_IDP_NAME_ALREADY_EXISTS);
        }
    }

    private void validateIdentityProviderApprovedDomainIds(List<String> approvedDomainIds) {
        for(String domainId : approvedDomainIds){
            Domain domain = domainService.getDomain(domainId);
            if(domain == null) {
                throw new BadRequestException(String.format(INVALID_IDENTITY_PROVIDER_APPROVED_DOMAIN_ERROR_MSG, domainId), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN);
            }
        }
    }

    /**
     * Validates the identity provider's emailDomains list has values of <=255 characters and are only set on a single
     * IDP on creation.
     *
     * @param identityProvider
     */
    private void validateIdentityProviderEmailDomainsOnCreation(IdentityProvider identityProvider) {
        EmailDomains emailDomains = identityProvider.getEmailDomains();

        if (emailDomains != null) {
            // Remove null elements
            emailDomains.getEmailDomain().removeAll(Collections.singleton(null));

            if (CollectionUtils.isEmpty(emailDomains.getEmailDomain())) {
                throw new BadRequestException("When providing emailDomains, you must provide a valid emailDomain.", ErrorCodes.ERROR_CODE_IDP_INVALID_EMAIL_DOMAIN_OPTIONS);
            }

            // Avoid duplicates
            Set<String> emailDomainSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            emailDomainSet.addAll(emailDomains.getEmailDomain());
            for (String emailDomain : emailDomainSet) {
                validateStringNotBlank(EMAIL_DOMAIN, emailDomain);
                validateStringNonWhitespace(EMAIL_DOMAIN, emailDomain);
                validateStringMaxLength(EMAIL_DOMAIN, emailDomain, MAX_LENGTH_255);

                com.rackspace.idm.domain.entity.IdentityProvider foundIdentityProvider = federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain);
                if (foundIdentityProvider != null) {
                    throw new DuplicateException(String.format(DUPLICATE_IDENTITY_PROVIDER_EMAIL_DOMAIN_ERROR_MSG, emailDomain), ErrorCodes.ERROR_CODE_IDP_EMAIL_DOMAIN_ALREADY_ASSIGNED);
                }
            }
        }
    }

    /**
     * Validates the identity provider's emailDomains list has values of <=255 characters and are only set on a single
     * IDP on update.
     *
     * @param identityProvider
     * @param existingIdentityProvider
     */
    private void validateIdentityProviderEmailDomainsOnUpdate(IdentityProvider identityProvider, com.rackspace.idm.domain.entity.IdentityProvider existingIdentityProvider) {
        EmailDomains emailDomains = identityProvider.getEmailDomains();

        if (emailDomains != null && !emailDomains.getEmailDomain().isEmpty()) {
            // Remove null elements
            emailDomains.getEmailDomain().removeAll(Collections.singleton(null));
            // Avoid duplicates
            Set<String> emailDomainSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            emailDomainSet.addAll(emailDomains.getEmailDomain());

            for (String emailDomain : emailDomainSet) {
                validateStringNotBlank(EMAIL_DOMAIN, emailDomain);
                validateStringNonWhitespace(EMAIL_DOMAIN, emailDomain);
                validateStringMaxLength(EMAIL_DOMAIN, emailDomain, MAX_LENGTH_255);

                // Check if existing IDP already contains the emailDomain provided.
                if (existingIdentityProvider.getEmailDomains() == null ||
                        !containsIgnoreCase(existingIdentityProvider.getEmailDomains(), emailDomain)) {

                    com.rackspace.idm.domain.entity.IdentityProvider foundIdentityProvider = federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain);
                    if (foundIdentityProvider != null) {
                        throw new DuplicateException(String.format(DUPLICATE_IDENTITY_PROVIDER_EMAIL_DOMAIN_ERROR_MSG, emailDomain), ErrorCodes.ERROR_CODE_IDP_EMAIL_DOMAIN_ALREADY_ASSIGNED);
                    }
                }
            }
        }
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        for (String s : list) {
            if (s.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    /**
     * Validates the provided identity provider for update provides valid values
     *
     * This performs validation applicable for identity-provider-admin. The following fields can be updated:
     * <ul>
     *     <li>name</li>
     *     <li>description</li>
     *     <li>authenticationUrl</li>
     *     <li>approvedDomainIds (if approvedDomainGroup == null)</li>
     * </ul>
     *
     *
     * @param identityProvider
     * @param existingProvider
     */
    public void validateIdentityProviderForUpdateForIdentityProviderManager(IdentityProvider identityProvider, com.rackspace.idm.domain.entity.IdentityProvider existingProvider) {
        if (identityProvider == null) {
            throw new BadRequestException("Must provide an identity provider");
        }

        validateAttributeisNull("issuer", identityProvider.getIssuer());
        validateAttributeisNull("federationType", identityProvider.getFederationType());
        validateAttributeisNull("id", identityProvider.getId());
        validateAttributeisNull("publicCertificates", identityProvider.getPublicCertificates());
        validateAttributeisNull("approvedDomainGroup", identityProvider.getApprovedDomainGroup());

        // Allowed attributes for update
        validateAttributeIsNotEmpty("name", identityProvider.getName());
        validateAttributeIsNotEmpty("authenticationUrl", identityProvider.getAuthenticationUrl());

        if(identityProvider.getApprovedDomainIds() != null) {

            if(!identityConfig.getReloadableConfig().getAllowUpdatingApprovedDomainIdsForIdp()) {
                throwBadRequestForProvidedAttrWithErrorCode("approvedDomainIds");
            }

            if(existingProvider.getApprovedDomainGroup() != null) {
                throw new BadRequestException("Cannot update approvedDomainIds if approvedDomainGroup is set.", ErrorCodes.ERROR_CODE_IDP_EXISTING_APPROVED_DOMAIN_GROUP);
            }

            List<String> approvedDomainIds = identityProvider.getApprovedDomainIds().getApprovedDomainId();
            if(approvedDomainIds.isEmpty()){
                throw new BadRequestException(EMPTY_IDENTITY_PROVIDER_APPROVED_DOMAIN_ERROR_MSG, ErrorCodes.ERROR_CODE_IDP_EMPTY_APPROVED_DOMAIN);
            }

            validateIdentityProviderApprovedDomainIds(approvedDomainIds);
        }

        // If a value is provided for name, must validate it. Null value means no update
        if (identityProvider.getName() != null) {
            validateIdentityProviderNameChangeWithDupCheck(identityProvider.getName(), existingProvider);
        }

        validateIdentityProviderDescription(identityProvider);
        validateIdentityProviderEmailDomainsOnUpdate(identityProvider, existingProvider);

        if (identityProvider.getAuthenticationUrl() != null && !identityProvider.getAuthenticationUrl().equalsIgnoreCase(existingProvider.getAuthenticationUrl())) {
            validateStringMaxLength("authenticationUrl", identityProvider.getAuthenticationUrl(), MAX_IDENTITY_PROVIDER_AUTH_URL);
        }
    }

    public void validateIdentityProviderForUpdateForUserAdminOrUserManage(IdentityProvider identityProvider, com.rackspace.idm.domain.entity.IdentityProvider existingProvider) {
        // If a value is provided for name, must validate it. Null value means no update
        if (identityProvider.getName() != null) {
            validateIdentityProviderNameChangeWithDupCheck(identityProvider.getName(), existingProvider);
        }

        validateIdentityProviderDescription(identityProvider);
        validateIdentityProviderEmailDomainsOnUpdate(identityProvider, existingProvider);
    }

    public void validateIdentityProviderForUpdateForRcnAdmin(IdentityProvider identityProvider, com.rackspace.idm.domain.entity.IdentityProvider existingProvider) {
        validateAttributeIsNotEmpty("name", identityProvider.getName());
        validateAttributeIsNotEmpty("description", identityProvider.getDescription());

        if (identityProvider.getName() != null) {
            validateIdentityProviderNameChangeWithDupCheck(identityProvider.getName(), existingProvider);
        }

        validateIdentityProviderDescription(identityProvider);
        validateIdentityProviderEmailDomainsOnUpdate(identityProvider, existingProvider);

        ApprovedDomainIds providedApprovedDomainIds = identityProvider.getApprovedDomainIds();

        if (providedApprovedDomainIds != null) {
            List<String> approvedDomainIds = providedApprovedDomainIds.getApprovedDomainId();
            // THIS fails on updating > 1 approved domain ids for rcn admin
            if (approvedDomainIds.size() != 1) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            String domainId = identityProvider.getApprovedDomainIds().getApprovedDomainId().get(0);
            Domain domain = domainService.getDomain(domainId);
            if (domain == null) {
                throw new BadRequestException(String.format(INVALID_IDENTITY_PROVIDER_APPROVED_DOMAIN_ERROR_MSG, domainId), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN);
            }

            Domain existingDomain = domainService.getDomain(existingProvider.getApprovedDomainIds().get(0));

            if (domain.getRackspaceCustomerNumber() == null || !domain.getRackspaceCustomerNumber().equalsIgnoreCase(existingDomain.getRackspaceCustomerNumber())) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            if (!existingProvider.getApprovedDomainIds().contains(domainId)) {
                validateIdentityProviderExplicitThreshold(domainId);
            }
        }
    }

    public void validatePublicCertificate(PublicCertificate publicCertificate) {
        if (publicCertificate == null || StringUtils.isBlank(publicCertificate.getPemEncoded())) {
            throw new BadRequestException("Public certificate is invalid");
        }

        try {
            X509Certificate cert = convertPublicCertificateToX509(publicCertificate);

            //check to see if the cert is expired
            DateTime now = new DateTime();
            DateTime certValidityExp = new DateTime(cert.getNotAfter());
            if(now.isAfter(certValidityExp)) {
                throw new BadRequestException("Public certificate is invalid");
            }
        } catch (CertificateException e) {
            throw new BadRequestException("Public certificate is invalid");
        }
    }

    public void validatePublicCertificateForIdentityProvider(PublicCertificate publicCertificate, com.rackspace.idm.domain.entity.IdentityProvider identityProvider) {
        //first, validate that the certificate is valid
        validatePublicCertificate(publicCertificate);

        //now validate that the identity provider does not already have this certificate
        if (CollectionUtils.isEmpty(identityProvider.getUserCertificates())) return;

        //certificates are uniquely identified by their sha1 so compare for duplicates with the sha1
        byte[] certBytes = Base64.decodeBase64(publicCertificate.getPemEncoded());
        String certId = DigestUtils.sha1Hex(certBytes);
        for (byte[] currCert : identityProvider.getUserCertificates()) {
            String currCertId = DigestUtils.sha1Hex(currCert);
            if (currCertId.equals(certId)) {
                throw  new DuplicateException("Cannot add duplicate public certificates to identity provider");
            }
        }
    }

    public void validateIdpPolicy(String policy, IdpPolicyFormatEnum idpPolicyFormatEnum) {
        // Ensure policy does not exceed max size allowed
        if (!stringDoesNotExceedSize(policy, identityConfig.getReloadableConfig().getIdpPolicyMaxSize())) {
            throw new BadRequestException(getIdpPolicyMaxSizeExceededErrorMessage());
        }

        // Ensure JSON policy contains valid format
        if(idpPolicyFormatEnum.equals(IdpPolicyFormatEnum.JSON) && !jsonValidator.isValidJson(policy)) {
            throw new BadRequestException(FEDERATION_IDP_POLICY_INVALID_JSON_ERROR_MESSAGE);
        }
    }

    public String getIdpPolicyMaxSizeExceededErrorMessage() {
        return  String.format(FEDERATION_IDP_POLICY_MAX_SIZE_EXCEED_ERROR_MESSAGE, identityConfig.getReloadableConfig().getIdpPolicyMaxSize());
    }

    public void validateDomainForCreation(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        if (StringUtils.isBlank(domain.getName())) {
            throwBadRequestForMissingAttr("name");
        }
        validateDomainType(domain);
        validateDomainSessionInactivityTimeout(domain);
        validateStringMaxLength("rackspaceCustomerNumber", domain.getRackspaceCustomerNumber(), MAX_LENGTH_32);
    }

    private void validateDomainType(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        if (domain.getType() != null) {
            Set<String> domainTypes = identityConfig.getReloadableConfig().getDomainTypes();
            if (!domainTypes.contains(domain.getType())) {
                String errMsg = String.format("Invalid value for domain type. Acceptable values are: %s", domainTypes);
                throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST);
            }
        }
    }

    public void validateDomainForUpdate(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain, String domainId) {
        if (StringUtils.isNotBlank(domain.getId()) && !domainId.equalsIgnoreCase(domain.getId())) {
            throw new BadRequestException("Domain Id does not match.");
        }
        validateDomainType(domain);
        validateDomainSessionInactivityTimeout(domain);
        validateDomainRcn(domain.getRackspaceCustomerNumber());
    }

    public void validateDomainRcn(String rcn) {
        validateStringMaxLength("rackspaceCustomerNumber", rcn, MAX_RCN_LENGTH);
    }

    private void validateDomainSessionInactivityTimeout(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        if(domain.getSessionInactivityTimeout() != null) {
            Duration maxDuration = identityConfig.getReloadableConfig().getSessionInactivityTimeoutMaxDuration();
            Duration minDuration = identityConfig.getReloadableConfig().getSessionInactivityTimeoutMinDuration();

            Duration duration = Duration.parse(domain.getSessionInactivityTimeout().toString());
            if (duration.getSeconds() > maxDuration.getSeconds() || duration.getSeconds() < minDuration.getSeconds()) {
                String errorMsg = String.format(SESSION_INACTIVITY_TIMEOUT_RANGE_ERROR_MESSAGE, minDuration.getSeconds(), maxDuration.getSeconds());
                throw new BadRequestException(errorMsg);
            }
        }
    }

    public void validateRoleForCreation(Role role) {
        if (role == null) {
            throwBadRequestForMissingAttrWithErrorCode("role");
        }

        /* ****************************
            Set global defaults when optional params not specified
           *************************** */
        if (StringUtils.isBlank(role.getServiceId())) {
            role.setServiceId(config.getString("cloudAuth.globalRoles.clientId"));
        }
        if (StringUtils.isBlank(role.getAdministratorRole())) {
            IdentityUserTypeEnum callerUserType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
            role.setAdministratorRole(callerUserType.getRoleName());
        }
        if (role.getRoleType() == null) {
            role.setRoleType(RoleTypeEnum.STANDARD);
        }

        // Set roleType defaults
        role.setPropagate(false);
        if (role.getRoleType() == RoleTypeEnum.RCN) {
            // Default role assignment to being global for RCN roles if not specified
            if (role.getAssignment() == null) {
                role.setAssignment(RoleAssignmentEnum.GLOBAL);
            }
        } else if (role.getRoleType() == RoleTypeEnum.PROPAGATE) {
            /*
             Migration code to move away from using propagation attribute to PROPAGATE roleType. Must temporarily set the
             propagation attribute
             */
            role.setPropagate(true);
        }

        /* ****************************
        Perform role type agnostic validation
        *************************** */
        String proposedName = role.getName();
        validateStringNotNullWithMaxLength("name", proposedName, MAX_ROLE_NAME);

        //validate name constraints
        if (!ROLE_NAME_REGEX.matcher(proposedName).matches()) {
            //invalid role name
            throw new BadRequestException(ROLE_NAME_INVALID, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
        }

        //TODO: Validate name matches character set and other reqs
        validateStringMaxLength("description", role.getDescription(), MAX_ROLE_DESC);

        String adminRoleName = role.getAdministratorRole();
        IdentityUserTypeEnum administratorUserTypeRole = IdentityUserTypeEnum.fromRoleName(adminRoleName);
        if (administratorUserTypeRole == null) {
            throw new BadRequestException(String.format("Unrecognized administrator role of '%s'", role.getAdministratorRole()));
        }
        else if (!(administratorUserTypeRole == IdentityUserTypeEnum.USER_MANAGER
                    || administratorUserTypeRole == IdentityUserTypeEnum.IDENTITY_ADMIN
                    || administratorUserTypeRole == IdentityUserTypeEnum.SERVICE_ADMIN)) {
            // Role must be user-manager, identity:admin, or service-admin
            throw new BadRequestException("Invalid administrator role");
        }

        ClientRole dirClientRole = roleService.getRoleByName(role.getName());
        if(dirClientRole != null) {
            String errMsg = "Role with name " + role.getName() + " already exists";
            throw new BadRequestException(errMsg);
        }

        /* ****************************
        Perform role type specific validation
        *************************** */
        if (role.getRoleType() == RoleTypeEnum.RCN) {
            // RCN roles can only be assigned as global roles
            if (role.getAssignment() != RoleAssignmentEnum.GLOBAL) {
                String errMsg = String.format(ERROR_TENANT_RCN_ROLE_MUST_HAVE_GLOBAL_ASSIGNMENT);
                throw new BadRequestException(errMsg);
            }

            // RCN roles must have at least one tenant type specified
            if (role.getTypes() == null || role.getTypes().getType().size() == 0) {
                String errMsg = String.format(ERROR_TENANT_REQUIRED_WHEN_TYPE_IS_RCN);
                throw new BadRequestException(errMsg);
            }
            validateTypes(role.getTypes(), true);
            validateTenantTypesFound(role.getTypes());
        } else if (role.getRoleType() == RoleTypeEnum.PROPAGATE) {
            // Propagating roles can only be created if roleAdministrator is identity:admin or identity:service-admin
            if (administratorUserTypeRole != IdentityUserTypeEnum.IDENTITY_ADMIN && administratorUserTypeRole != IdentityUserTypeEnum.SERVICE_ADMIN) {
                throw new BadRequestException("Only identity and service admins are allowed to manage propagating roles");
            }

            // Propagating roles can't have tenant types
            if (role.getTypes() != null && !CollectionUtils.isEmpty(role.getTypes().getType())) {
                throw new BadRequestException("Propagating roles can not have tenant types");
            }
        } else if (role.getRoleType() == RoleTypeEnum.STANDARD) {
            // Standard roles can't have tenant types
            if (role.getTypes() != null && !CollectionUtils.isEmpty(role.getTypes().getType())) {
                throw new BadRequestException("Standard roles can not have tenant types");
            }
        }
    }

    private X509Certificate convertPublicCertificateToX509(PublicCertificate publicCertificate) throws CertificateException {
        byte[] certBytes = Base64.decodeBase64(publicCertificate.getPemEncoded());
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));
    }

    public void throwBadRequestForEmptyAttribute(String attrName) {
        String errMsg = String.format(EMPTY_ATTR_MESSAGE, attrName);
        logger.debug(errMsg);
        throw new BadRequestException(errMsg);
    }

    public void throwBadRequestForMissingAttr(String attrName) {
        String errMsg = String.format(REQUIRED_ATTR_MESSAGE, attrName);
        logger.debug(errMsg);
        throw new BadRequestException(errMsg);
    }

    public void throwBadRequestForMissingAttrWithErrorCode(String attrName) {
        String errMsg = String.format(REQUIRED_ATTR_MESSAGE, attrName);
        logger.debug(errMsg);
        throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE);
    }

    public void throwBadRequestForProvidedAttrWithErrorCode(String attrName) {
        String errMsg = String.format(INVALID_ATTR_MESSAGE, attrName);
        logger.warn(errMsg);
        throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
    }

    public void validateEndpointTemplateForUpdate(EndpointTemplate endpoint) {
        if(endpoint.isDefault() != null && endpoint.isDefault() && endpoint.isGlobal() != null && endpoint.isGlobal()) {
            throw new BadRequestException("An endpoint template cannot be both global and default.");
        }
    }

    public void validateStringNotNullWithMaxLength(String propertyName, String value, int maxLength) {
        if (StringUtils.isBlank(value)) {
            throwBadRequestForMissingAttrWithErrorCode(propertyName);
        }
        validateStringMaxLength(propertyName, value, maxLength);
    }

    public void validateAttributeisNull(String propertyName, Object value) {
        if (value != null) {
            throwBadRequestForProvidedAttrWithErrorCode(propertyName);
        }
    }

    public void validateAttributeCollectionIsNullOrEmpty(String propertyName, Collection value) {
        if (CollectionUtils.isNotEmpty(value)) {
            String errMsg = String.format(INVALID_ATTR_MESSAGE, propertyName);
            throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_INVALID_VALUE);
        }
    }

    public void validateAttributeNotNull(String propertyName, Object value) {
        if (value == null) {
            String errMsg = String.format(BLANK_ERROR_MSG, propertyName);
            throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_INVALID_VALUE);
        }
    }

    public void validateAttributeIsNotEmpty(String propertyName, String value) {
        if (value != null && value.isEmpty()) {
            throwBadRequestForEmptyAttribute(propertyName);
        }
    }

    public void validateStringMaxLength(String propertyName, String value, int maxLength) {
        if (StringUtils.isNotBlank(value) && value.length() > maxLength) {
           throw new BadRequestException(generateLengthExceededMsg(propertyName, maxLength), ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED);
        }
    }

    public void validateIntegerMinMax(String propertyName, int value, int min, int max) {
        if (value < min || value > max) {
            throw new BadRequestException(generateValueExceededMsg(propertyName, min, max), ErrorCodes.ERROR_CODE_INVALID_VALUE);
        }
    }

    private void validateStringNonWhitespace(String propertyName, String value) {
        if (StringUtils.isNotBlank(value) && value.matches(".*[\\s].*")) {
            String errMsg = String.format(WHITESPACE_CHARACTERS_ERROR_MSG, propertyName);
            throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_INVALID_VALUE);
        }
    }

    public void validateStringNotBlank(String propertyName, String value) {
        if (StringUtils.isBlank(value)) {
            String errMsg = String.format(BLANK_ERROR_MSG, propertyName);
            throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_INVALID_VALUE);
        }
    }

    public void validateRequiredAttribute(String attrName, String value) {
        if (StringUtils.isBlank(value)) {
            String errMsg = String.format(REQUIRED_ATTR_MESSAGE, attrName);
            throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE);
        }
    }

    private String generateLengthExceededMsg(String propertyName, int maxLength) {
        return String.format(LENGTH_EXCEEDED_ERROR_MSG, propertyName, maxLength);
    }

    private String generateValueExceededMsg(String propertyName, int min, int max) {
        return String.format(VALUE_EXCEEDED_ERROR_MSG, propertyName, min, max);
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void validateTenantType(Tenant tenant) {
        validateTypes(tenant.getTypes(), false);
        validateTenantTypesFound(tenant.getTypes());
    }

    private void validateTenantTypesFound(Types tenantTypes) {
        if (tenantTypes != null) {
            for (String type : tenantTypes.getType()) {
                TenantType tenantType = tenantTypeDao.getTenantType(type);
                if (tenantType == null) {
                    String errMsg = String.format(ERROR_TENANT_TYPE_WAS_NOT_FOUND, type);
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
            }
        }
    }

    private void validateTypes(Types types, boolean allowAllTenantTypes) {
        Set<String> tenantTypes = new HashSet<String>();

        if (types == null) {
            return;
        }

        for(String type : types.getType()) {
            if (type != null) {
                tenantTypes.add(type.toLowerCase());
            }
        }

        types.getType().clear();
        types.getType().addAll(tenantTypes);

        if(types.getType().size() > 16) {
            String errMsg = String.format(ERROR_TENANT_TYPE_CANNOT_EXCEED_MAXIMUM);
            throw new BadRequestException(errMsg);
        }

        for (String type : types.getType()) {
            validateTenantType(allowAllTenantTypes, type);
        }
    }

    private void validateTenantType(boolean allowAllTenantTypes, String type) {
        if (allowAllTenantTypes) {
            if (type.equals(ALL_TENANT_TYPE)) {
                return;
            }
        }

        if (type == null || type.length() == 0 || type.length() > TENANT_TYPE_NAME_MAX_LENGTH) {
            String errMsg = String.format(ERROR_TENANT_TYPE_MUST_BE_CORRECT_SIZE);
            throw new BadRequestException(errMsg);
        }

        if (!TENANT_TYPE_NAME_PATTERN.matcher(type).matches()) {
            String errMsg = String.format(ERROR_TENANT_TYPE_INVALID_CHARS);
            throw new BadRequestException(errMsg);
        }
    }

    public void validateTenantType(TenantType tenantType) {
        if(StringUtils.isBlank(tenantType.getName())){
            throw new BadRequestException(ERROR_TENANT_TYPE_NAME_MUST_BE_SPECIFIED);
        }

        validateTenantType(true, tenantType.getName());

        if (tenantType.getDescription() == null || tenantType.getDescription().length() == 0 || tenantType.getDescription().length() > 255) {
            String errMsg = String.format(ERROR_TENANT_TYPE_DESCRIPTION_MUST_BE_CORRECT_SIZE);
            throw new BadRequestException(errMsg);
        }
    }

    /**
     * Validates that the provided string is shorter than the provided size in kilobytes
     *
     * @param value
     * @param sizeInKilobytes
     * @throws IllegalArgumentException if string is null sizeInKilobytes is negative
     * @return
     */
    public boolean stringDoesNotExceedSize(String value, long sizeInKilobytes) {
        Validate.isTrue(value != null);
        Validate.isTrue(sizeInKilobytes >= 0);

        byte [] jsonBytes = value.getBytes(StandardCharsets.UTF_8);
        if(jsonBytes.length > sizeInKilobytes * 1024) {
            return false;
        }

        return true;
    }

    /**
     * A reusable method to restrict unverified user
     * and throws ForbiddenException if user is unverified
     * @param user
     */
    public static void validateItsNotUnverifiedUser (User user) {
        if (user!= null && user.isUnverified()) {
            throw new ForbiddenException(GlobalConstants.RESTRICT_UNVERIFIED_USER_MESSAGE, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
        }
    }

    /**
     * A reusable method to validate password is not in password blacklist data set.
     * @param password
     */
    public void validatePasswordIsNotBlacklisted(String password){
        if (passwordBlacklistService.isPasswordInBlacklist(password)) {

            throw new BadRequestException(ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG,
                    ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD);
        }
    }
}
