package com.rackspace.idm.util;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedUserRequest;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.util.predicate.UserEnabledPredicate;
import com.rackspace.idm.validation.PrecedenceValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

//TODO: Refactor this completely
@Component
public class SamlResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(SamlResponseValidator.class);

    public static final String DISABLED_DOMAIN_ERROR_MESSAGE = "Domain %s is disabled.";

    @Autowired
    SamlSignatureValidator samlSignatureValidator;

    @Autowired
    RoleService roleService;

    @Autowired
    DomainDao domainDao;

    @Autowired
    IdentityProviderDao identityProviderDao;

    @Autowired
    PrecedenceValidator precedenceValidator;

    @Autowired
    private Configuration config;

    @Autowired
    DomainService domainService;

    /**
     * Validate the samlResponse contains the required data. In addition it verifies the specified issuer exists, the
     * signature validates against that issuer, and the domain exists.
     * - a valid issuer
     *
     * @param samlResponseDecorator
     */
    public FederatedUserRequest validateAndPopulateRequest(SamlResponseDecorator samlResponseDecorator) {
        IdentityProvider provider = validateIssuer(samlResponseDecorator);
        validateSignatureForProvider(samlResponseDecorator, provider);

        //populate a federated user object based on saml data
        FederatedUserRequest request = new FederatedUserRequest();
        request.setIdentityProvider(provider);

        request.setFederatedUser(new FederatedUser());
        request.getFederatedUser().setFederatedIdpUri(provider.getUri());

        //validate assertion
        validateSamlAssertionAndPopulateRequest(samlResponseDecorator, request);

        return request;
    }

    private void validateSignatureForProvider(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider) {
        if (samlResponseDecorator.getSamlResponse().getSignature() == null) {
            throw new BadRequestException("No Signature specified");
        }
        try {
            samlSignatureValidator.validateSignature(samlResponseDecorator.getSamlResponse().getSignature(), provider.getPublicCertificate());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new BadRequestException("Signature is invalid");
        }
    }

    private void validateSamlAssertionAndPopulateRequest(SamlResponseDecorator samlResponseDecorator, FederatedUserRequest request) {
        if (samlResponseDecorator.getSamlResponse().getAssertions() == null || samlResponseDecorator.getSamlResponse().getAssertions().size() == 0) {
            throw new BadRequestException("No Assertions specified");
        }

        Assertion samlAssertion = samlResponseDecorator.getSamlResponse().getAssertions().get(0);

        request.getFederatedUser().setUsername(validateAndReturnUsername(samlAssertion));

        validateSubjectConfirmationNotOnOrAfterDateAndPopulateRequest(samlAssertion, request);

        validateAuthInstant(samlAssertion);
        validateAuthContextClassRef(samlAssertion);

        //validate and populate domain
        validateSamlDomainAndPopulateRequest(samlResponseDecorator, request);

        //validate and populate email
        validateSamlEmailAndPopulateRequest(samlResponseDecorator, request);

        validateRolesAndPopulateRequest(samlResponseDecorator.getAttribute(SAMLConstants.ATTR_ROLES), request);



    }

    private IdentityProvider validateIssuer(SamlResponseDecorator samlResponseDecorator) {
        if (samlResponseDecorator.getSamlResponse().getIssuer() == null || StringUtils.isBlank(samlResponseDecorator.getSamlResponse().getIssuer().getValue())) {
            throw new BadRequestException("Issuer is not specified");
        }

        IdentityProvider provider = identityProviderDao.getIdentityProviderByUri(samlResponseDecorator.getSamlResponse().getIssuer().getValue());

        if ( provider == null) {
            throw new BadRequestException("Issuer is unknown");
        }
        return provider;
    }

    private String validateAndReturnUsername(Assertion samlAssertion) {
        if (samlAssertion.getSubject() == null || samlAssertion.getSubject().getNameID() == null
                || StringUtils.isBlank(samlAssertion.getSubject().getNameID().getValue())) {
            throw new BadRequestException("Subject is not specified");
        }
        return samlAssertion.getSubject().getNameID().getValue();
    }

    private void validateSubjectConfirmationNotOnOrAfterDateAndPopulateRequest(Assertion samlAssertion, FederatedUserRequest request) {
        if (samlAssertion.getSubject().getSubjectConfirmations() == null ||
            samlAssertion.getSubject().getSubjectConfirmations().size() == 0 ||
            samlAssertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData() == null ||
            samlAssertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter() == null) {
            throw new BadRequestException("SubjectConfirmationData NotOnOrAfter is not specified");
        }

        DateTime expirationDate = samlAssertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter();
        if (expirationDate.isBeforeNow())  {
            throw new BadRequestException("SubjectConfirmationData NotOnOrAfter can not be in the past");
        }

        request.setRequestedTokenExpirationDate(expirationDate);
    }

    private void validateAuthInstant(Assertion samlAssertion) {
        if (samlAssertion.getAuthnStatements() == null ||
            samlAssertion.getAuthnStatements().size() == 0 ||
            samlAssertion.getAuthnStatements().get(0).getAuthnInstant() == null) {
            throw new BadRequestException("AuthnInstant is not specified");
        }
    }

    private void validateAuthContextClassRef(Assertion samlAssertion) {
        if (samlAssertion.getAuthnStatements().get(0).getAuthnContext() == null ||
            samlAssertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef() == null) {
            throw new BadRequestException("AuthnContextClassRef is not specified");
        }

        if ( !samlAssertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef().equals(SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS)) {
            throw new BadRequestException("Invalid AuthnContext value");
        }
    }

    /**
     * Returns the validated domain
     *
     * @return
     */
    private void validateSamlDomainAndPopulateRequest(SamlResponseDecorator decoratedResponse, FederatedUserRequest request) {
        List<String> domains = decoratedResponse.getAttribute(SAMLConstants.ATTR_DOMAIN);

        if (domains == null || domains.size() == 0) {
            throw new BadRequestException("Domain attribute is not specified");
        }

        if (domains.size() > 1) {
            throw new BadRequestException("Multiple domains specified");
        }

        String requestedDomain = domains.get(0);
        Domain domain = domainDao.getDomain(requestedDomain);
        if (domain == null) {
            throw new BadRequestException("Domain '" + requestedDomain + "' does not exist.");
        }
        else if (!domain.getEnabled()) {
            throw new BadRequestException(String.format(DISABLED_DOMAIN_ERROR_MESSAGE, requestedDomain));
        }

        List<User> userAdmins = domainService.getDomainAdmins(domain.getDomainId());

        if(userAdmins.size() == 0) {
            log.error("Unable to get roles for saml assertion due to no user admin for domain {}", domain.getDomainId());
            throw new IllegalStateException("no user admin exists for domain " + domain.getDomainId());
        }

        if(userAdmins.size() > 1 && getDomainRestrictedToOneUserAdmin()) {
            log.error("Unable to get roles for saml assertion due to more than one user admin for domain {}", domain.getDomainId());
            throw new IllegalStateException("more than one user admin exists for domain " + domain.getDomainId());
        }

        boolean enabledUserAdmin = org.apache.commons.collections4.CollectionUtils.exists(userAdmins, new UserEnabledPredicate());
        if(!enabledUserAdmin) {
            throw new BadRequestException(String.format(DISABLED_DOMAIN_ERROR_MESSAGE, requestedDomain));
        }

        request.getFederatedUser().setDomainId(domain.getDomainId());
    }

    /**
     * Validates and sets the email on the provided federateduserrequest.
     *
     * @return
     */
    private void validateSamlEmailAndPopulateRequest(SamlResponseDecorator decoratedResponse, FederatedUserRequest request) {
        List<String> emails = decoratedResponse.getAttribute(SAMLConstants.ATTR_EMAIL);

        if (emails == null || emails.size() == 0) {
            throw new BadRequestException("Email attribute is not specified");
        }

        if (emails.size() > 1) {
            throw new BadRequestException("Multiple emails specified");
        }

        String email = emails.get(0);

        request.getFederatedUser().setEmail(email);
    }

    private void validateRolesAndPopulateRequest(List<String> roleNames, FederatedUserRequest request) {
        if (CollectionUtils.isNotEmpty(roleNames)) {
            Map<String, TenantRole> roles = new HashMap<String, TenantRole>();
            for (String roleName : roleNames) {
                if (roles.containsKey(roleName)) {
                    throw new BadRequestException("role '" + roleName + "' specified more than once");
                }

                //TODO: Candidate for caching...
                ClientRole role = roleService.getRoleByName(roleName);
                if (role == null || role.getRsWeight() != PrecedenceValidator.RBAC_ROLES_WEIGHT) {
                    throw new BadRequestException("Invalid role '" + roleName + "'");
                }

                request.getRequestClientRoleCache().put(roleName, role);

                //create a new global role to add
                TenantRole tenantRole = new TenantRole();
                tenantRole.setRoleRsId(role.getId());
                tenantRole.setClientId(role.getClientId());
                tenantRole.setName(role.getName());
                tenantRole.setDescription(role.getDescription());
                roles.put(roleName, tenantRole);
            }
            request.getFederatedUser().getRoles().addAll(roles.values());
        }
    }

    private boolean getDomainRestrictedToOneUserAdmin() {
        return config.getBoolean("domain.restricted.to.one.user.admin.enabled", false);
    }

}