package testHelpers.saml

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.decorator.SAMLAuthContext
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.StringUtils
import org.joda.time.DateTime
import org.opensaml.saml2.core.LogoutRequest
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.core.impl.LogoutRequestMarshaller
import org.opensaml.saml2.core.impl.ResponseMarshaller
import org.opensaml.xml.util.XMLHelper
import org.w3c.dom.Element

import static com.rackspace.idm.Constants.*

class SamlFactory {

    def generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        HashMap<String, List<String>> attributes = SamlAttributeFactory.createAttributes(domain, roles, email)
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey, publicKey, issueInstant)
    }

    def generateSamlAssertionStringForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        Response response = generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, roles, email, privateKey, publicKey, issueInstant)
        return convertResponseToString(response)
    }

    def generateSamlAssertionStringForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, SamlProducer samlProducer) {
        def response = generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, roles, email, samlProducer)
        return convertResponseToString(response)
    }

    def generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, SamlProducer samlProducer) {
        HashMap<String, List<String>> attributes = SamlAttributeFactory.createAttributes(domain, roles, email)
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, samlProducer)
    }

    def generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationSeconds, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, authnContextClassRef, privateKey, publicKey)
    }

    def generateSamlAssertionStringForFederatedRacker(issuer, subject, expirationSeconds, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        Response response = generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationSeconds, authnContextClassRef, privateKey, publicKey)
        return convertResponseToString(response)
    }

    def generateSamlAssertion(issuer, subject, expirationSeconds, Map<String, List<String>> attributes, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        SamlProducer producer = new SamlProducer(privateKey, publicKey);

        Response responseInitial = producer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationSeconds, authnContextClassRef, issueInstant);
        return responseInitial;
    }

    def generateSamlAssertion(issuer, subject, expirationSeconds, Map<String, List<String>> attributes, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, SamlProducer samlProducer) {
        return samlProducer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationSeconds, authnContextClassRef);
    }

    def convertResponseToString(Response samlResponse) {
        ResponseMarshaller marshaller = new ResponseMarshaller();
        Element element = marshaller.marshall(samlResponse);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLHelper.writeNode(element, baos);
        return new String(baos.toByteArray());
    }

    def generateLogoutRequestForFederatedUser(issuer, subject, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        SamlProducer producer = new SamlProducer(privateKey, publicKey);

        LogoutRequest logoutRequest = producer.createSAMLLogoutRequest(subject, issuer, issueInstant);
        return logoutRequest;
    }

    def generateLogoutRequestStringForFederatedUser(issuer, subject, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        LogoutRequest logoutRequest = generateLogoutRequestForFederatedUser(issuer, subject, privateKey, publicKey, issueInstant)
        return convertLogoutRequestToString(logoutRequest)
    }

    def generateLogoutRequestEncoded(issuer, subject, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        String logoutRequest = generateLogoutRequestStringForFederatedUser(issuer, subject, privateKey, publicKey, issueInstant)
        return org.apache.xml.security.utils.Base64.encode(StringUtils.getBytesUtf8(logoutRequest))
    }

    def convertLogoutRequestToString(LogoutRequest logoutRequest) {
        LogoutRequestMarshaller marshaller = new LogoutRequestMarshaller();
        Element element = marshaller.marshall(logoutRequest);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLHelper.writeNode(element, baos);
        return new String(baos.toByteArray());
    }
}
