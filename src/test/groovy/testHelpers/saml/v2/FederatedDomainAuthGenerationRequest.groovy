package testHelpers.saml.v2

class FederatedDomainAuthGenerationRequest extends FederatedAuthGenerationRequest {
    String domainId
    String email
    Set<String> roleNames = Collections.EMPTY_SET
}
