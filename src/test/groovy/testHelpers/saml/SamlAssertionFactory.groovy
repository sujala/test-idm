package testHelpers.saml

import org.joda.time.DateTime
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.core.impl.ResponseMarshaller
import org.opensaml.xml.util.XMLHelper
import org.w3c.dom.Element

class SamlAssertionFactory {

    private static final String PRIVATE_KEY = "saml.pkcs8"
    private static final String PUBLIC_KEY = "saml.crt"

    def generateSamlAssertion(issuer, subject, expirationDays, domain, roles) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put("domain", [domain].asList());
        attributes.put("roles", roles);

        SamlAssertionProducer producer = new SamlAssertionProducer();

        producer.setPrivateKeyLocation(PRIVATE_KEY);
        producer.setPublicKeyLocation(PUBLIC_KEY);

        Response responseInitial = producer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationDays);

        ResponseMarshaller marshaller = new ResponseMarshaller();
        Element element = marshaller.marshall(responseInitial);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLHelper.writeNode(element, baos);
        return new String(baos.toByteArray());
    }

}
