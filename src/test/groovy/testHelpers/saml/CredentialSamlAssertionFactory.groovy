package testHelpers.saml

import com.rackspace.idm.Constants
import org.joda.time.DateTime
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.core.impl.ResponseMarshaller
import org.opensaml.xml.security.credential.Credential
import org.opensaml.xml.util.XMLHelper
import org.w3c.dom.Element

class CredentialSamlAssertionFactory {

    private Credential credential
    SamlProducer producer

    CredentialSamlAssertionFactory(Credential credential) {
        this.credential = credential
        producer = new SamlProducer(credential);
    }

    def generateSamlAssertion(issuer, subject, expirationDays, domain, roles) {
       return generateSamlAssertion(issuer, subject, expirationDays, domain, roles, Constants.DEFAULT_FED_EMAIL);
    }

    def generateSamlAssertion(issuer, subject, expirationDays, domain, roles, email) {
        HashMap<String, List<String>> attributes = SamlAttributeFactory.createAttributes(domain, roles, email)
        return generateSamlAssertion(issuer, subject, expirationDays, attributes)
    }

    def generateSamlAssertion(issuer, subject, expirationDays, Map<String, List<String>> attributes) {
        Response responseInitial = producer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationDays);

        ResponseMarshaller marshaller = new ResponseMarshaller();
        Element element = marshaller.marshall(responseInitial);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLHelper.writeNode(element, baos);
        return new String(baos.toByteArray());
    }

}
