package testHelpers.saml.v2

import com.rackspace.idm.domain.decorator.SAMLAuthContext
import org.joda.time.DateTime

class FederatedAuthGenerationRequest {
    String brokerIssuer
    String originIssuer
    DateTime requestIssueInstant
    int validitySeconds
    String username
    Map<String, List<String>> otherAttributes = Collections.EMPTY_MAP
    String authContextRefClass
}
