package testHelpers.saml.v2

import com.rackspace.idm.domain.decorator.SAMLAuthContext
import org.joda.time.DateTime

class FederatedDomainAuthGenerationRequest {
    String brokerIssuer
    String originIssuer
    DateTime requestIssueInstant
    int validitySeconds
    String username
    String domainId
    String email
    Set<String> roleNames = Collections.EMPTY_SET
    Map<String, List<String>> otherAttributes = Collections.EMPTY_MAP
    SAMLAuthContext samlAuthContext
}
