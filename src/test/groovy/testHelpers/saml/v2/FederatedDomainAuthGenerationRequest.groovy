package testHelpers.saml.v2

import com.rackspace.idm.domain.decorator.SAMLAuthContext
import org.joda.time.DateTime

class FederatedDomainAuthGenerationRequest extends FederatedAuthGenerationRequest {
    String domainId
    String email
    Set<String> roleNames = Collections.EMPTY_SET
}
