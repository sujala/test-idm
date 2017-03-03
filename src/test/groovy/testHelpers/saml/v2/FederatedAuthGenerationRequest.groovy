package testHelpers.saml.v2

import org.joda.time.DateTime

class FederatedAuthGenerationRequest {
    String brokerIssuer
    String originIssuer
    DateTime originIssueInstant = new DateTime()
    DateTime responseIssueInstant
    int validitySeconds
    String username
    Map<String, List<String>> otherAttributes = Collections.EMPTY_MAP
    String authContextRefClass
}
