package com.rackspace.idm.domain.service.federation.v2;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.xmlsec.signature.Signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 Decorator class around saml response used for Identity Fed v2 API, that contains various convenience
 methods to retrieve attributes from a saml response while avoiding NPEs.
 */
public class SamlResponseWrapper {
    private Response samlResponse;

    public SamlResponseWrapper(Response samlResponse) {
        this.samlResponse = samlResponse;
    }

    public Response getSamlResponse() {
        return samlResponse;
    }

    public String getBrokerIssuer() {
        if (samlResponse.getIssuer() == null || StringUtils.isBlank(samlResponse.getIssuer().getValue())) {
            return null;
        }
        return samlResponse.getIssuer().getValue();
    }

    public String getOriginIssuer() {
        String result = null;
        List<Assertion> originAssertions = getOriginAssertions();
        if (CollectionUtils.isNotEmpty(originAssertions)) {
            Issuer samlOriginIssuer = originAssertions.get(0).getIssuer();
            if (samlOriginIssuer != null) {
                result = samlOriginIssuer.getValue();
            }
        }
        return result;
    }

    public Signature getBrokerSignature() {
        return samlResponse.getSignature();
    }

    /**
     * Returns the Origin IDP assertions within the Response. Origin assertions are defined as all assertions in the SAML Response other than
     * the first one. Will return an empty list if no Origin assertions exist. The first assertion is considered the
     * Broker assertion.
     */
    public List<Assertion> getOriginAssertions() {
        List<Assertion> assertions = samlResponse.getAssertions();

        if (CollectionUtils.isEmpty(assertions) || assertions.size() <= 1) {
            return Collections.EMPTY_LIST;
        }

        return samlResponse.getAssertions().subList(1, assertions.size());
    }

    public Assertion getBrokerAssertion() {
        List<Assertion> assertions = samlResponse.getAssertions();
        if (CollectionUtils.isEmpty(assertions)) {
            return null;
        }
        return samlResponse.getAssertions().get(0);
    }

    public DateTime getResponseIssueInstant() {
        DateTime issueInstant = getSamlResponse().getIssueInstant();

        if (issueInstant == null) {
            return null;
        }

        return issueInstant;
    }

    public DateTime getRequestedTokenExpiration() {
        if (getSubject() == null ||
                getSubject().getSubjectConfirmations() == null ||
                getSubject().getSubjectConfirmations().size() == 0 ||
                getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData() == null ||
                getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter() == null) {
            return null;
        }

        DateTime expirationDate = getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter();
        return expirationDate;
    }

    public String getUsername() {
        if (getSubject() == null || getSubject().getNameID() == null
                || StringUtils.isBlank(getSubject().getNameID().getValue())) {
            return null;
        }

        return getSubject().getNameID().getValue();
    }

    public Subject getSubject() {
        if (getBrokerAssertion() == null) {
            return null;
        }
        return getBrokerAssertion().getSubject();
    }

    public DateTime getBrokerAuthnInstant() {
        if (    getBrokerFirstAuthnStatement() == null ||
                getBrokerFirstAuthnStatement().getAuthnInstant() == null) {
            return null;
        }
        return getBrokerFirstAuthnStatement().getAuthnInstant();
    }

    public AuthnContextClassRef getBrokerAuthContextClassRef() {
        if (    getBrokerAuthnContext() == null ||
                getBrokerAuthnContext().getAuthnContextClassRef() == null) {
            return null;
        }
        return getBrokerAuthnContext().getAuthnContextClassRef();
    }

    private AuthnStatement getBrokerFirstAuthnStatement() {
        if (    getBrokerAssertion() == null ||
                CollectionUtils.isEmpty(getBrokerAssertion().getAuthnStatements())) {
            return null;
        }
        return getBrokerAssertion().getAuthnStatements().get(0);
    }

    private AuthnContext getBrokerAuthnContext() {
        if (    getBrokerFirstAuthnStatement() == null ||
                getBrokerFirstAuthnStatement().getAuthnContext() == null) {
            return null;
        }
        return getBrokerFirstAuthnStatement().getAuthnContext();
    }


    /**
     * Gets a list of values for the specified attribute within the assertion. Returns null if the attribute does not
     * exist within assertion.
     *
     * @param samlAssertion
     * @param attributeName
     * @return
     */
    public List<String> getAttributeWithinAssertion(Assertion samlAssertion, String attributeName) {
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
            values.add(getStringValueFromXMLObject(value));
        }
        return values;
    }

    /**
     * Gets the XML value of an XML object as String
     * @param xmlObj XML object
     * @return XML value as String
     */
    public static String getStringValueFromXMLObject(XMLObject xmlObj) {
        if (xmlObj == null) {
            return null;
        }

        if (xmlObj instanceof XSString) {
            return ((XSString) xmlObj).getValue();
        } else if (xmlObj instanceof XSInteger) {
            Integer val = ((XSInteger) xmlObj).getValue();
            return val != null ? val.toString() : null;
        } else if (xmlObj instanceof XSAny) {
            return ((XSAny) xmlObj).getTextContent();
        }
        return null;
    }
}
