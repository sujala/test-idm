package testHelpers.saml.v2

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger
import org.opensaml.security.credential.Credential

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
        if (CollectionUtils.isNotEmpty(genRequest.roleNames)) {
            attributes.put("roles", genRequest.roleNames)
        }
        if (CollectionUtils.isNotEmpty(genRequest.groupNames)) {
            attributes.put("groups", genRequest.groupNames)
        }
        attributes.putAll(genRequest.otherAttributes)
        return attributes
    }
}
