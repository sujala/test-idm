package com.rackspace.idm.domain.decorator;

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;

import java.util.ArrayList;
import java.util.List;

/*
 decorator class around saml response, that contains a lot of convenience
 methods to retrieve attributes from a saml response
 */
public class SamlResponseDecorator {

    private Response samlResponse;

    public SamlResponseDecorator(Response samlResponse) {
        this.samlResponse = samlResponse;
    }

    public Response getSamlResponse() {
        return samlResponse;
    }

    public String getIdpUri() {
        Assertion samlAssertion = samlResponse.getAssertions().get(0);

        return samlAssertion.getIssuer().getValue();
    }

    public String getUsername() {
        Assertion samlAssertion = samlResponse.getAssertions().get(0);

        return samlAssertion.getSubject().getNameID().getValue();
    }

    public List<String> getAttribute(String attributeName) {
        Assertion samlAssertion = samlResponse.getAssertions().get(0);
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
