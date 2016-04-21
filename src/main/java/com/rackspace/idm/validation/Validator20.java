package com.rackspace.idm.validation;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.TenantRole;
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
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.impl.EmailValidator;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 8/3/12
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class Validator20 {

    public static final String USER_NULL_IMPERSONATION_ERROR_MSG = "User cannot be null for impersonation request";
    public static final String USERNAME_NULL_IMPERSONATION_ERROR_MSG = "Username cannot be null for impersonation request";
    public static final String USERNAME_EMPTY_IMPERSONATION_ERROR_MSG = "Username cannot be empty or blank";
    public static final String EXPIRE_IN_ELEMENT_LESS_ONE_IMPERSONATION_ERROR_MSG = "Expire in element cannot be less than 1.";
    public static final String EXPIRE_IN_ELEMENT_EXCEEDS_MAX_IMPERSONATION_ERROR_MSG = "Expire in element cannot be more than %s";
    public static final String TOKEN_CLOUD_AUTH_EXPIRATION_SECONDS_PROP_NAME = "token.cloudAuthExpirationSeconds";
    public static final String ROLE_NAME_INVALID = "Invalid role name. Naming convention is <product prefix>:<role name> " +
            "where both the prefix and role name must start with an alphanumeric character.  The rest of the prefix and " +
            "role name can be comprised of alphanumeric characters, '-', and '_'.";
    public static final Pattern ROLE_NAME_REGEX = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_\\-]*(?:\\:[a-zA-Z0-9][a-zA-Z0-9_\\-]*)?$");

    private EmailValidator emailValidator = new EmailValidator();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int MAX_LENGTH_64 = 64;
    public static final int MAX_LENGTH_255 = 255;

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int MAX_GROUP_NAME = MAX_LENGTH_64;
    public static final int MAX_GROUP_DESC = 1000;
    public static final int MAX_USERNAME = 100;

    public static final int MAX_ROLE_NAME = MAX_LENGTH_255;
    public static final int MAX_ROLE_DESC = MAX_LENGTH_255;

    public static final int MAX_IDENTITY_PROVIDER_ISSUER = Constants.MAX_255;
    public static final int MAX_IDENTITY_PROVIDER_AUTH_URL = Constants.MAX_255;
    public static final int MAX_IDENTITY_PROVIDER_DESCRIPTION = Constants.MAX_255;
    public static final String APPROVED_DOMAIN_GROUP_NAME = "approvedDomainGroup";
    public static final String APPROVED_DOMAINS = "approvedDomains";

    private static final String REQUIRED_ATTR_MESSAGE = "'%s' is a required attribute";
    private static final String INVALID_ATTR_MESSAGE = "'%s' is not a valid attribute for this service";

    public static final String LENGTH_EXCEEDED_ERROR_MSG = "%s length cannot exceed %s characters";

    @Autowired
    private TenantService tenantService;

    @Autowired
    private FederatedIdentityService federatedIdentityService;

    @Autowired
    private DomainService domainService;

    @Autowired
    Configuration config;

    @Autowired
    private RoleService roleService;

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

    public void validatePasswordForCreateOrUpdate(String password){
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

    public void validatePasswordCredentialsForCreateOrUpdate(PasswordCredentialsBase passwordCredentials){
        validatePasswordCredentials(passwordCredentials);
        validatePasswordForCreateOrUpdate(passwordCredentials.getPassword());
    }

    public void validateApiKeyCredentials(ApiKeyCredentials apiKeyCredentials) {
        validateUsername(apiKeyCredentials.getUsername());
        if (StringUtils.isBlank(apiKeyCredentials.getApiKey())) {
            String errMsg = "Expecting apiKey";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
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
        //need to verify that these values are supplied due to them being optional in the schema and the use of json
        if (endpoint.getId() == null) {
            throwBadRequestForMissingAttr("id");
        }
        if (StringUtils.isBlank(endpoint.getType())) {
            throwBadRequestForMissingAttr("type");
        }
        if (StringUtils.isBlank(endpoint.getPublicURL())) {
            throwBadRequestForMissingAttr("publicURL");
        }
        if (StringUtils.isBlank(endpoint.getName())) {
            throwBadRequestForMissingAttr("name");
        }
    }

    /**
     * Ideally would use the bean validation logic, but trying to do a quick and thorough implementation.
     * @param identityProvider
     */
    public void validateIdentityProviderForCreation(IdentityProvider identityProvider) {
        if (identityProvider == null) {
            throw new BadRequestException("Must provide an identity provider");
        }

        validateStringNotNullWithMaxLength("issuer", identityProvider.getIssuer(), MAX_IDENTITY_PROVIDER_ISSUER);
        if (federatedIdentityService.getIdentityProviderByIssuer(identityProvider.getIssuer()) != null) {
            throw new DuplicateException("Provider already exists with this issuer", ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS);
        }

        if (identityProvider.getFederationType() == null) {
            throwBadRequestForMissingAttrWithErrorCode("federationType");
        }

        //validate the authenticationUrl is specified and under max length
        validateStringNotNullWithMaxLength("authenticationUrl", identityProvider.getAuthenticationUrl(), MAX_IDENTITY_PROVIDER_AUTH_URL);

        validateStringMaxLength("description", identityProvider.getDescription(), MAX_IDENTITY_PROVIDER_DESCRIPTION);

        if (StringUtils.isNotBlank(identityProvider.getId())) {
            throw new BadRequestException("Do not provide an id when creating a new Identity Provider. An id will be generated.");
        }

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
                for (String providedApprovedDomain : providedApprovedDomains) {
                    Domain domain = domainService.getDomain(providedApprovedDomain);
                    if (domain == null) {
                        throw new BadRequestException(String.format("The provided approved domain '%s' does not exist", providedApprovedDomain), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN);
                    }
                }
            }
        } else {
            //racker provider don't contain approvedDomain stuff
            if (providedApprovedDomainGroup != null || providedApprovedDomainIds != null) {
                throw new BadRequestException(String.format("%s and %s are not valid attributes for this federation type", APPROVED_DOMAIN_GROUP_NAME, APPROVED_DOMAINS), ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS);
            }
        }
    }

    public void validateIdentityProviderForUpdate(IdentityProvider identityProvider) {
        if (identityProvider == null) {
            throw new BadRequestException("Must provide an identity provider");
        }

        validateAttributeisNull("issuer", identityProvider.getIssuer());
        validateAttributeisNull("federationType", identityProvider.getFederationType());
        validateAttributeisNull("description", identityProvider.getIssuer());
        validateAttributeisNull("id", identityProvider.getId());
        validateAttributeisNull("publicCertificates", identityProvider.getPublicCertificates());
        validateAttributeisNull("approvedDomainGroup", identityProvider.getApprovedDomainGroup());
        validateAttributeisNull("approvedDomainIds", identityProvider.getApprovedDomainIds());

        //allowed
        validateStringNotNullWithMaxLength("authenticationUrl", identityProvider.getAuthenticationUrl(), MAX_IDENTITY_PROVIDER_AUTH_URL);
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

    public void validateRoleForCreation(Role role) {
        if (role == null) {
            throwBadRequestForMissingAttrWithErrorCode("role");
        }

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

        if (administratorUserTypeRole != null) {
            //role must be user-admin, identity:admin, or service-admin
            if (!(administratorUserTypeRole == IdentityUserTypeEnum.USER_MANAGER
                    || administratorUserTypeRole == IdentityUserTypeEnum.IDENTITY_ADMIN
                    || administratorUserTypeRole == IdentityUserTypeEnum.SERVICE_ADMIN)) {
                throw new BadRequestException("Invalid administrator role");
            }
        }

        ClientRole dirClientRole = roleService.getRoleByName(role.getName());
        if(dirClientRole != null) {
            String errMsg = "Role with name " + role.getName() + " already exists";
            throw new BadRequestException(errMsg);
        }

        //roles can only be propagating if roleAdministrator is identity:admin or identity:service-admin
        if (BooleanUtils.isTrue(role.isPropagate()) && administratorUserTypeRole == IdentityUserTypeEnum.USER_MANAGER) {
            throw new BadRequestException("User Managers are not allowed to manage propagating roles");
        }
    }

    private X509Certificate convertPublicCertificateToX509(PublicCertificate publicCertificate) throws CertificateException {
        byte[] certBytes = Base64.decodeBase64(publicCertificate.getPemEncoded());
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));
    }

    private void throwBadRequestForMissingAttr(String attrName) {
        String errMsg = String.format(REQUIRED_ATTR_MESSAGE, attrName);
        logger.debug(errMsg);
        throw new BadRequestException(errMsg);
    }

    private void throwBadRequestForMissingAttrWithErrorCode(String attrName) {
        String errMsg = String.format(REQUIRED_ATTR_MESSAGE, attrName);
        logger.debug(errMsg);
        throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE);
    }

    private void throwBadRequestForProvidedAttrWithErrorCode(String attrName) {
        String errMsg = String.format(INVALID_ATTR_MESSAGE, attrName);
        logger.warn(errMsg);
        throw new BadRequestException(errMsg, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
    }

    public void validateEndpointTemplateForUpdate(EndpointTemplate endpoint) {
        if(endpoint.isDefault() != null && endpoint.isDefault() && endpoint.isGlobal() != null && endpoint.isGlobal()) {
            throw new BadRequestException("An endpoint template cannot be both global and default.");
        }
    }

    private void validateStringNotNullWithMaxLength(String propertyName, String value, int maxLength) {
        if (StringUtils.isBlank(value)) {
            throwBadRequestForMissingAttrWithErrorCode(propertyName);
        }
        validateStringMaxLength(propertyName, value, maxLength);
    }

    private void validateAttributeisNull(String propertyName, Object value) {
        if (value != null) {
            throwBadRequestForProvidedAttrWithErrorCode(propertyName);
        }
    }

    private void validateStringMaxLength(String propertyName, String value, int maxLength) {
        if (StringUtils.isNotBlank(value) && value.length() > maxLength) {
           throw new BadRequestException(generateLengthExceededMsg(propertyName, maxLength), ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED);
        }
    }

    private String generateLengthExceededMsg(String propertyName, int maxLength) {
        return String.format(LENGTH_EXCEEDED_ERROR_MSG, propertyName, maxLength);
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
}
