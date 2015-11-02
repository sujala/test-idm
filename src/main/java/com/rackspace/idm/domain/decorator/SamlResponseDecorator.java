package com.rackspace.idm.domain.decorator;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.*;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.signature.Signature;

import java.util.ArrayList;
import java.util.List;

/*
 decorator class around saml response, that contains a lot of convenience
 methods to retrieve attributes from a saml response.

 class assumes that only one saml assertion exists in the saml response.
 */
public class SamlResponseDecorator {
    private Response samlResponse;

    public SamlResponseDecorator(Response samlResponse) {
        this.samlResponse = samlResponse;
    }

    public Response getSamlResponse() {
        return samlResponse;
    }

    public String checkAndGetIssuer() {
        String issuer = getIssuer();
        if (StringUtils.isBlank(issuer)) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_ISSUER, "Issuer is not specified"));
        }
        return issuer;
    }

    public String getIssuer() {
        if (samlResponse.getIssuer() == null || StringUtils.isBlank(samlResponse.getIssuer().getValue())) {
            return null;
        }
        return samlResponse.getIssuer().getValue();
    }

    public Signature checkAndGetSignature() {
        Signature sig = getSignature();
        if (sig == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_SIGNATURE, "No Signature specified"));
        }
        return sig;
    }

    public Signature getSignature() {
        return samlResponse.getSignature();
    }

    public Assertion checkAndGetAssertion() {
        Assertion assertion = getAssertion();
        if (assertion == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_ASSERTION, "No Assertions specified"));
        }
        return assertion;
    }

    public Assertion getAssertion() {
        if (samlResponse.getAssertions() == null || samlResponse.getAssertions().size() == 0) {
            return null;
        }
        return samlResponse.getAssertions().get(0);
    }

    public Subject checkAndGetSubject() {
        Subject subject = getSubject();
        if (subject == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_SUBJECT, "Subject is not specified"));
        }
        return subject;
    }

    public Subject getSubject() {
        if (checkAndGetAssertion().getSubject() == null) {
            return null;
        }

        return checkAndGetAssertion().getSubject();
    }

    public String checkAndGetUsername() {
        String username = getUsername();
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_USERNAME, "Subject is not specified"));
        }
        return getAssertion().getSubject().getNameID().getValue();
    }

    public String getUsername() {
        if (checkAndGetSubject() == null || checkAndGetSubject().getNameID() == null
                || StringUtils.isBlank(checkAndGetSubject().getNameID().getValue())) {
            return null;
        }

        return checkAndGetSubject().getNameID().getValue();
    }

    public DateTime checkAndGetSubjectConfirmationNotOnOrAfterDate() {
        DateTime notOnOrAfter = getSubjectConfirmationNotOnOrAfterDate();

        if (notOnOrAfter == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_SUBJECT_NOTONORAFTER, "SubjectConfirmationData NotOnOrAfter is not specified"));
        } else if (notOnOrAfter.isBeforeNow())  {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_SUBJECT_NOTONORAFTER, "SubjectConfirmationData NotOnOrAfter can not be in the past"));
        }

        return notOnOrAfter;
    }

    public DateTime checkAndGetIssueInstant() {
        DateTime issueInstant = getSamlResponse().getIssueInstant();

        if (issueInstant == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_AUTH_INSTANT, "IssueInstant is not specified"));
        }

        return issueInstant;
    }

    private DateTime getSubjectConfirmationNotOnOrAfterDate() {
        if (checkAndGetSubject().getSubjectConfirmations() == null ||
                checkAndGetSubject().getSubjectConfirmations().size() == 0 ||
                checkAndGetSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData() == null ||
                checkAndGetSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter() == null) {
            return null;
        }

        DateTime expirationDate = checkAndGetSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter();
        return expirationDate;
    }

    public DateTime checkAndGetAuthnInstant() {
        DateTime instant = getAuthnInstant();
        if (instant == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_AUTH_INSTANT, "AuthnInstant is not specified"));
        }
        return instant;
    }

    public DateTime getAuthnInstant() {
        if (checkAndGetAssertion().getAuthnStatements() == null ||
                checkAndGetAssertion().getAuthnStatements().size() == 0 ||
                checkAndGetAssertion().getAuthnStatements().get(0).getAuthnInstant() == null) {
            return null;
        }
        return checkAndGetAssertion().getAuthnStatements().get(0).getAuthnInstant();
    }


    public SAMLAuthContext checkAndGetAuthContextClassRef() {
        AuthnContextClassRef ref = getAuthContextClassRef();
        if (ref == null || StringUtils.isBlank(ref.getAuthnContextClassRef())) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_AUTH_CONTEXT_CLASSREF, "AuthnContextClassRef is not specified"));
        }

        SAMLAuthContext samlAuthContext = SAMLAuthContext.fromSAMLAuthnContextClassRef(ref.getAuthnContextClassRef());
        if (samlAuthContext == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_AUTH_CONTEXT_CLASSREF, "Invalid AuthnContext value"));
        }

        return samlAuthContext;
    }

    public AuthnContextClassRef getAuthContextClassRef() {
        if (checkAndGetAssertion().getAuthnStatements().get(0).getAuthnContext() == null ||
                checkAndGetAssertion().getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef() == null) {
            return null;
        }
        return checkAndGetAssertion().getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef();
    }

    public List<String> getAttribute(String attributeName) {
        Assertion samlAssertion = checkAndGetAssertion();
        if (samlAssertion == null || samlAssertion.getAttributeStatements() == null
                || samlAssertion.getAttributeStatements().size() == 0
                || samlAssertion.getAttributeStatements().get(0) == null) {
            return null;
        }
        AttributeStatement attributeStatement = samlAssertion.getAttributeStatements().get(0);

        for (Attribute attribute : attributeStatement.getAttributes()) {
            if (attributeName.equals(attribute.getName())) {
                return getAttributeValues(attribute);
            }
        }
        return null;
    }

    private List<String> getAttributeValues(Attribute attribute) {
        List<String> values = new ArrayList<String>();
        for (XMLObject value : attribute.getAttributeValues()) {
            XSString stringAttr = (XSString) value;
            values.add(stringAttr.getValue());
        }

        return values;
    }
}
