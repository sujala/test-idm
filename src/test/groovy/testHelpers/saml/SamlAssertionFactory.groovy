package testHelpers.saml

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.decorator.SAMLAuthContext
import org.joda.time.DateTime
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.core.impl.ResponseMarshaller
import org.opensaml.xml.util.XMLHelper
import org.w3c.dom.Element

import static com.rackspace.idm.Constants.*

class SamlAssertionFactory {

    def generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, roles, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        HashMap<String, List<String>> attributes = SamlAttributeFactory.createAttributes(domain, roles, email)
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey, publicKey)
    }

    def generateSamlAssertionStringForFederatedUser(issuer, subject, expirationSeconds, domain, roles, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        Response response = generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, roles, email, privateKey, publicKey)
        return convertResponseToString(response)
    }

    def generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationSeconds, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, authnContextClassRef, privateKey, publicKey)
    }

    def generateSamlAssertionStringForFederatedRacker(issuer, subject, expirationSeconds, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        Response response = generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationSeconds, authnContextClassRef, privateKey, publicKey)
        return convertResponseToString(response)
    }

    def generateSamlAssertion(issuer, subject, expirationSeconds, Map<String, List<String>> attributes, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        SamlAssertionProducer producer = new SamlAssertionProducer(privateKey, publicKey);

        Response responseInitial = producer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationSeconds, authnContextClassRef);
        return responseInitial;
    }

    def convertResponseToString(Response samlResponse) {
        ResponseMarshaller marshaller = new ResponseMarshaller();
        Element element = marshaller.marshall(samlResponse);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLHelper.writeNode(element, baos);
        return new String(baos.toByteArray());
    }
}
