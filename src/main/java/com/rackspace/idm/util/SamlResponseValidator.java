package com.rackspace.idm.util;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SamlResponseValidator {

    @Autowired
    SamlSignatureValidator samlSignatureValidator;

    @Autowired
    ApplicationRoleDao roleDao;

    @Autowired
    DomainDao domainDao;

    @Autowired
    IdentityProviderDao identityProviderDao;

    public void validate(SamlResponseDecorator samlResponseDecorator) throws Throwable {
        validateIssuer(samlResponseDecorator);
        validateSignature(samlResponseDecorator);
        validateAssertion(samlResponseDecorator);

        Assertion samlAssertion = samlResponseDecorator.getSamlResponse().getAssertions().get(0);
        validateSubject(samlAssertion);
        validateSubjectConfirmationNotOnOrAfterDate(samlAssertion);
        validateAuthInstant(samlAssertion);
        validateAuthContextClassRef(samlAssertion);
        validateDomain(samlResponseDecorator.getAttribute(SAMLConstants.ATTR_DOMAIN));
        validateRoles(samlResponseDecorator.getAttribute(SAMLConstants.ATTR_ROLES));
    }

    private void validateSignature(SamlResponseDecorator samlResponseDecorator) throws Throwable{
        if (samlResponseDecorator.getSamlResponse().getSignature() == null) {
            throw new BadRequestException("No Signature specified");
        }

        IdentityProvider provider = identityProviderDao.getIdentityProviderByUri(samlResponseDecorator.getIdpUri());

        try {
            samlSignatureValidator.validateSignature(samlResponseDecorator.getSamlResponse().getSignature(), provider.getPublicCertificate());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new BadRequestException("Signature is invalid");
        }
    }

    private void validateAssertion(SamlResponseDecorator samlResponseDecorator) throws Throwable {
        if (samlResponseDecorator.getSamlResponse().getAssertions() == null || samlResponseDecorator.getSamlResponse().getAssertions().size() == 0) {
            throw new BadRequestException("No Assertions specified");
        }
    }

    private void validateIssuer(SamlResponseDecorator samlResponseDecorator) throws Throwable {
        if (samlResponseDecorator.getSamlResponse().getIssuer() == null || StringUtils.isBlank(samlResponseDecorator.getSamlResponse().getIssuer().getValue())) {
            throw new BadRequestException("Issuer is not specified");
        }

        if (identityProviderDao.getIdentityProviderByUri(samlResponseDecorator.getSamlResponse().getIssuer().getValue()) == null) {
            throw new BadRequestException("Issuer is unknown");
        }
    }

    private void validateSubject(Assertion samlAssertion) throws Throwable {
        if (samlAssertion.getSubject() == null || samlAssertion.getSubject().getNameID() == null
                || StringUtils.isBlank(samlAssertion.getSubject().getNameID().getValue())) {
            throw new BadRequestException("Subject is not specified");
        }
    }

    private void validateSubjectConfirmationNotOnOrAfterDate(Assertion samlAssertion) throws Throwable {
        if (samlAssertion.getSubject().getSubjectConfirmations() == null ||
            samlAssertion.getSubject().getSubjectConfirmations().size() == 0 ||
            samlAssertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData() == null ||
            samlAssertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter() == null) {
            throw new BadRequestException("SubjectConfirmationData NotOnOrAfter is not specified");
        }

        if (samlAssertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter().isBeforeNow())  {
            throw new BadRequestException("SubjectConfirmationData NotOnOrAfter can not be in the past");
        }
    }

    private void validateAuthInstant(Assertion samlAssertion) throws Throwable {
        if (samlAssertion.getAuthnStatements() == null ||
            samlAssertion.getAuthnStatements().size() == 0 ||
            samlAssertion.getAuthnStatements().get(0).getAuthnInstant() == null) {
            throw new BadRequestException("AuthnInstant is not specified");
        }
    }

    private void validateAuthContextClassRef(Assertion samlAssertion) throws Throwable {
        if (samlAssertion.getAuthnStatements().get(0).getAuthnContext() == null ||
            samlAssertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef() == null) {
            throw new BadRequestException("AuthContextClassRef is not specified");
        }

        if ( !samlAssertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef().equals(SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS)) {
            throw new BadRequestException("Invalid AuthContext value");
        }
    }

    private void validateDomain(List<String> domain) throws Throwable {
        if (domain == null || domain.size() == 0) {
            throw new BadRequestException("domain attribute is not specified");
        }

        if (domain.size() > 1) {
            throw new BadRequestException("multiple domains specified");
        }

        if (domainDao.getDomain(domain.get(0)) == null) {
            throw new BadRequestException("domain '" + domain.get(0) + "' does not exist");
        }
    }

    private void validateRoles(List<String> roles) throws Throwable {
        if (roles == null || roles.size() == 0) {
            throw new BadRequestException("roles attribute is not specified");
        }

        Set<String> roleNames = new HashSet<String>();

        for (String role : roles) {
            if (roleDao.getRoleByName(role) == null) {
                throw new BadRequestException("role '" + role + "' does not exist");
            }

            if (roleNames.contains(role)) {
                throw new BadRequestException("role '" + role + "' specified more than once");
            }

            roleNames.add(role);
        }
    }
}