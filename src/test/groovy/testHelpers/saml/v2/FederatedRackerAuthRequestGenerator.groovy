package testHelpers.saml.v2

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
        return new HashMap<String, List<String>>()
    }
}
