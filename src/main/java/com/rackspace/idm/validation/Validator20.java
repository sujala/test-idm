package com.rackspace.idm.validation;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificate;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificates;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.FederatedIdentityService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.entity.Constants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.impl.EmailValidator;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
    private EmailValidator emailValidator = new EmailValidator();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int MAX_GROUP_NAME = 64;
    public static final int MAX_GROUP_DESC = 1000;

    public static final int MAX_IDENTITY_PROVIDER_ISSUER = Constants.MAX_255;
    public static final int MAX_IDENTITY_PROVIDER_DESCRIPTION = Constants.MAX_255;


    private static final String ENDPOINT_TEMPLATE_REQUIRED_ATTR_MESSAGE = "'%s' is a required attribute";

    public static final String LENGTH_EXCEEDED_ERROR_MSG = "%s length cannot exceed %s characters";

    @Autowired
    private TenantService tenantService;

    @Autowired
    private FederatedIdentityService federatedIdentityService;

    @Autowired
    Configuration config;

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

    public void validateUserForCreate(User user) {
        validateUsername(user.getUsername());
        validateUsernameForUpdateOrCreate(user.getUsername());
        validateEmail(user.getEmail());
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
            throw new DuplicateException("Provider already exists with this issuer");
        }

        if (identityProvider.getFederationType() == null) {
            throwBadRequestForMissingAttr("federationType");
        }

        validateStringMaxLength("description", identityProvider.getDescription(), MAX_IDENTITY_PROVIDER_DESCRIPTION);

        if (StringUtils.isNotBlank(identityProvider.getId())) {
            throw new BadRequestException("Do not provide an id when creating a new Identity Provider. An id will be generated.");
        }

        PublicCertificates publicCertificatesWrapper = identityProvider.getPublicCertificates();
        if (publicCertificatesWrapper != null && CollectionUtils.isNotEmpty(publicCertificatesWrapper.getPublicCertificate())) {
            for (PublicCertificate publicCertificate : publicCertificatesWrapper.getPublicCertificate()) {
                //validate each public certificate is an actual cert
            }
        }
    }

    private void throwBadRequestForMissingAttr(String attrName) {
        String errMsg = String.format(ENDPOINT_TEMPLATE_REQUIRED_ATTR_MESSAGE, attrName);
        logger.warn(errMsg);
        throw new BadRequestException(errMsg);
    }

    public void validateEndpointTemplateForUpdate(EndpointTemplate endpoint) {
        if(endpoint.isDefault() != null && endpoint.isDefault() && endpoint.isGlobal() != null && endpoint.isGlobal()) {
            throw new BadRequestException("An endpoint template cannot be both global and default.");
        }
    }

    private void validateStringNotNullWithMaxLength(String propertyName, String value, int maxLength) {
        if (StringUtils.isBlank(value)) {
            throwBadRequestForMissingAttr(propertyName);
        }
        validateStringMaxLength(propertyName, value, maxLength);
    }

    private void validateStringMaxLength(String propertyName, String value, int maxLength) {
        if (value != null) {
            if (value.length() > maxLength) {
                throw new BadRequestException(generateLengthExceededMsg(propertyName, maxLength));
            }
        }
    }

    private String generateLengthExceededMsg(String propertyName, int maxLength) {
        return String.format(LENGTH_EXCEEDED_ERROR_MSG, propertyName, maxLength);
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
}
