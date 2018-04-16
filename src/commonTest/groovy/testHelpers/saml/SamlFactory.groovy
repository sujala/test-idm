package testHelpers.saml

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.decorator.SAMLAuthContext
import net.shibboleth.utilities.java.support.xml.SerializeSupport
import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.collections.CollectionUtils
import org.joda.time.DateTime
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.io.Marshaller
import org.opensaml.core.xml.io.MarshallerFactory
import org.opensaml.saml.saml2.core.LogoutRequest
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.core.impl.LogoutRequestMarshaller
import org.opensaml.saml.saml2.core.impl.ResponseMarshaller
import org.opensaml.saml.saml2.metadata.EntityDescriptor
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor
import org.opensaml.saml.saml2.metadata.KeyDescriptor
import org.opensaml.saml.saml2.metadata.SingleSignOnService
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorImpl
import org.opensaml.saml.saml2.metadata.impl.IDPSSODescriptorImpl
import org.opensaml.saml.saml2.metadata.impl.KeyDescriptorImpl
import org.opensaml.saml.saml2.metadata.impl.SingleSignOnServiceImpl
import org.opensaml.security.credential.UsageType
import org.opensaml.xmlsec.signature.KeyInfo
import org.opensaml.xmlsec.signature.X509Certificate
import org.opensaml.xmlsec.signature.X509Data
import org.opensaml.xmlsec.signature.impl.KeyInfoImpl
import org.opensaml.xmlsec.signature.impl.X509CertificateImpl
import org.opensaml.xmlsec.signature.impl.X509DataImpl
import org.w3c.dom.Document
import org.w3c.dom.Element

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import static com.rackspace.idm.Constants.DEFAULT_IDP_PRIVATE_KEY
import static com.rackspace.idm.Constants.DEFAULT_IDP_PUBLIC_KEY
import static org.opensaml.saml.common.xml.SAMLConstants.*;

class SamlFactory {

    Response generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        HashMap<String, List<String>> attributes = SamlAttributeFactory.createAttributes(domain, roles, email)
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey, publicKey, issueInstant)
    }

    String generateSamlAssertionStringForFederatedUser(issuer, subject, expirationSeconds, HashMap<String, List<String>> attributes, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        Response response = generateSamlAssertion(issuer, subject, expirationSeconds, attributes, SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey, publicKey, issueInstant)
        return convertResponseToString(response)
    }

    String generateSamlAssertionStringForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        Response response = generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, roles, email, privateKey, publicKey, issueInstant)
        return convertResponseToString(response)
    }

    String generateSamlAssertionStringForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, SamlProducer samlProducer, Collection<String> userGroups = Collections.EMPTY_LIST) {
        def response = generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, roles, email, samlProducer)
        return convertResponseToString(response)
    }

    Response generateSamlAssertionResponseForFederatedUser(issuer, subject, expirationSeconds, domain, List<String> roles = Collections.EMPTY_LIST, email = Constants.DEFAULT_FED_EMAIL, SamlProducer samlProducer, Collection<String> userGroups = Collections.EMPTY_LIST) {
        HashMap<String, List<String>> attributes = SamlAttributeFactory.createAttributes(domain, roles, email, userGroups)
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, samlProducer)
    }

    Response generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationSeconds, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        return generateSamlAssertion(issuer, subject, expirationSeconds, attributes, authnContextClassRef, privateKey, publicKey)
    }

    String generateSamlAssertionStringForFederatedRacker(issuer, subject, expirationSeconds, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY) {
        Response response = generateSamlAssertionResponseForFederatedRacker(issuer, subject, expirationSeconds, authnContextClassRef, privateKey, publicKey)
        return convertResponseToString(response)
    }

    def generateSamlAssertion(issuer, subject, expirationSeconds, Map<String, List<String>> attributes, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, privateKey = DEFAULT_IDP_PRIVATE_KEY, publicKey = DEFAULT_IDP_PUBLIC_KEY, issueInstant = new DateTime()) {
        SamlProducer producer = new SamlProducer(privateKey, publicKey);

        Response responseInitial = producer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationSeconds, authnContextClassRef, issueInstant);
        return responseInitial
    }

    def generateSamlAssertion(issuer, subject, expirationSeconds, Map<String, List<String>> attributes, String authnContextClassRef = SAMLAuthContext.PASSWORD.samlAuthnContextClassRef, SamlProducer samlProducer) {
        return samlProducer.createSAMLResponse(subject, new DateTime(), attributes, issuer, expirationSeconds, authnContextClassRef);
    }

    def convertResponseToString(Response samlResponse) {
        ResponseMarshaller marshaller = new ResponseMarshaller()
        Element element = marshaller.marshall(samlResponse)

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        SerializeSupport.writeNode(element, baos)
        return new String(baos.toByteArray())
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

    def generateLogoutRequestEncodedForSamlProducer(issuer, subject, producer, issueInstant = new DateTime()) {
        LogoutRequest logoutRequest = producer.createSAMLLogoutRequest(subject, issuer, issueInstant);
        def requestString = convertLogoutRequestToString(logoutRequest)
        return org.apache.xml.security.utils.Base64.encode(StringUtils.getBytesUtf8(requestString))
    }

    def generateMetadataXMLForIDP(issuer, authenticationUrl, Collection<String> pubKeys = null) {
        EntityDescriptor entityDescriptor = new EntityDescriptorImpl(SAML20MD_NS, "EntityDescriptor", "md")
        entityDescriptor.entityID = issuer

        IDPSSODescriptor idpSSODescriptor = new IDPSSODescriptorImpl(SAML20MD_NS, "IDPSSODescriptor", "md")
        idpSSODescriptor.addSupportedProtocol(SAML20P_NS)

        if (CollectionUtils.isEmpty(pubKeys)) {
            // Create a public key
            def keyPair = SamlCredentialUtils.generateKeyPair()
            def cert = SamlCredentialUtils.generateCertificate(keyPair)

            // Add the public key to the list of keys
            idpSSODescriptor.keyDescriptors.add(createKeyDescriptor(SamlCredentialUtils.getCertificateAsPEMString(cert)))
        } else {
            for (key in pubKeys) {
                // Add the public key to the list of keys
                idpSSODescriptor.keyDescriptors.add(createKeyDescriptor(key))
            }
        }

        // Create SingleSignOnService
        SingleSignOnService singleSignOnService = new SingleSignOnServiceImpl(SAML20MD_NS, "SingleSignOnService", "md")
        singleSignOnService.binding = SAML2_REDIRECT_BINDING_URI
        singleSignOnService.location = authenticationUrl
        idpSSODescriptor.singleSignOnServices.add(singleSignOnService)

        entityDescriptor.roleDescriptors.add(idpSSODescriptor)

        return convertEntityDescriptorToString(entityDescriptor);
    }

    static KeyDescriptor createKeyDescriptor(String pubKey) {
        KeyDescriptor keyDescriptor = new KeyDescriptorImpl(SAML20MD_NS, "KeyDescriptor", "md")
        keyDescriptor.use = UsageType.SIGNING
        X509Data x509Data = new X509DataImpl("http://www.w3.org/2000/09/xmldsig#", "X509Data", "md1")
        KeyInfo keyInfo = new KeyInfoImpl("http://www.w3.org/2000/09/xmldsig#", "KeyInfo", "md1")
        X509Certificate x509Certificate = new X509CertificateImpl("http://www.w3.org/2000/09/xmldsig#", "X509Certificate", "md1")
        x509Certificate.value = pubKey
        x509Data.x509Certificates.add(x509Certificate)
        keyInfo.XMLObjects.add(x509Data)
        keyDescriptor.keyInfo = keyInfo
        return keyDescriptor
    }

    def convertEntityDescriptorToString(EntityDescriptor entityDescriptor) {
        // Get apropriate unmarshaller
        MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
        Marshaller marshaller = marshallerFactory.getMarshaller(entityDescriptor);

        // convert EntityDescriptor to XML document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder()
        Document document = builder.newDocument()
        marshaller.marshall(entityDescriptor, document)

        // Convert XML document to string
        StringWriter sw = new StringWriter();
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(sw));
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }

        return sw.toString();
    }

    def convertLogoutRequestToString(LogoutRequest logoutRequest) {
        LogoutRequestMarshaller marshaller = new LogoutRequestMarshaller();
        Element element = marshaller.marshall(logoutRequest);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SerializeSupport.writeNode(element, baos);
        return new String(baos.toByteArray());
    }
}
