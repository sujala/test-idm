package testHelpers.saml

import com.rackspace.idm.Constants
import org.joda.time.DateTime
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.core.impl.ResponseMarshaller
import org.opensaml.xml.util.XMLHelper
import org.w3c.dom.Element

import static com.rackspace.idm.Constants.*

class SamlAssertionFactory {

    def generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationDays, domain, roles, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        HashMap<String, List<String>> attributes = SamlAttributeFactory.createAttributes(domain, roles, email)
        return generateSamlAssertion(issuer, subject, expirationDays, attributes, privateKey, publicKey)
    }

    def generateSamlAssertionStringForFederatedUser(issuer, subject, expirationDays, domain, roles, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        Response response = generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationDays, domain, roles, email, privateKey, publicKey)
        return convertResponseToString(response)
    }

    def generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationDays, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        return generateSamlAssertion(issuer, subject, expirationDays, attributes, privateKey, publicKey)
    }

    def generateSamlAssertionStringForFederatedRacker(issuer, subject, expirationDays, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        Response response = generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationDays, privateKey, publicKey)
        return convertResponseToString(response)
    }

    def generateSamlAssertion(issuer, subject, expirationDays, Map<String, List<String>> attributes, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        SamlAssertionProducer producer = new SamlAssertionProducer(privateKey, publicKey);

        Response responseInitial = producer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationDays);
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
