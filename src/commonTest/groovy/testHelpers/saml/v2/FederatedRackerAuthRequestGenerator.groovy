package testHelpers.saml.v2

import org.apache.commons.collections4.CollectionUtils
import org.opensaml.security.credential.Credential

class FederatedRackerAuthRequestGenerator extends AbstractFederatedAuthRequestGenerator<FederatedRackerAuthGenerationRequest>{

    FederatedRackerAuthRequestGenerator(String brokerPublicKeyLocation, String brokerPrivateKeyLocation, String originPublicKeyLocation, String originPrivateKeyLocation) {
        super(brokerPublicKeyLocation, brokerPrivateKeyLocation, originPublicKeyLocation, originPrivateKeyLocation)
    }

    FederatedRackerAuthRequestGenerator(Credential brokerCredential, Credential originCredential) {
        super(brokerCredential, originCredential)
    }

    @Override
    HashMap<String, List<String>> createAttributes(FederatedRackerAuthGenerationRequest genRequest) {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>()
        if (CollectionUtils.isNotEmpty(genRequest.groupNames)) {
            attributes.put("groups", genRequest.groupNames)
        }
        attributes.putAll(genRequest.otherAttributes)
        return attributes
    }
}
