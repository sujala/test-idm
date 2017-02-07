package testHelpers.saml.v2

import net.shibboleth.utilities.java.support.xml.SerializeSupport
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.core.config.InitializationService
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.schema.XSAny
import org.opensaml.core.xml.schema.XSString
import org.opensaml.core.xml.schema.impl.XSAnyBuilder
import org.opensaml.core.xml.schema.impl.XSStringBuilder
import org.opensaml.saml.common.SAMLVersion
import org.opensaml.saml.saml2.core.*
import org.opensaml.saml.saml2.core.impl.*
import org.opensaml.security.credential.Credential
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.impl.SignatureBuilder
import org.opensaml.xmlsec.signature.support.SignatureConstants
import org.opensaml.xmlsec.signature.support.SignatureValidator
import org.opensaml.xmlsec.signature.support.Signer
import org.w3c.dom.Element
import testHelpers.saml.SamlCredentialUtils

class FederatedDomainAuthRequestGenerator extends AbstractFederatedAuthRequestGenerator<FederatedDomainAuthGenerationRequest> {
    private static final Logger logger = Logger.getLogger(FederatedDomainAuthRequestGenerator.class)

    FederatedDomainAuthRequestGenerator(String brokerPublicKeyLocation, String brokerPrivateKeyLocation, String originPublicKeyLocation, String originPrivateKeyLocation) {
        super(brokerPublicKeyLocation, brokerPrivateKeyLocation, originPublicKeyLocation, originPrivateKeyLocation)
    }

    FederatedDomainAuthRequestGenerator(Credential brokerCredential, Credential originCredential) {
        super(brokerCredential, originCredential)
    }

    @Override
    HashMap<String, List<String>> createAttributes(FederatedDomainAuthGenerationRequest genRequest) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        if (StringUtils.isNotBlank(genRequest.email)) {
            attributes.put("email", [genRequest.email])
        }
        if (StringUtils.isNotBlank(genRequest.domainId)) {
            attributes.put("domain", [genRequest.domainId])
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(genRequest.roleNames)) {
            attributes.put("roles", genRequest.roleNames)
        }
        attributes.putAll(genRequest.otherAttributes)
        return attributes
    }
}
