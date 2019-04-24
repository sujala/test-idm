package com.rackspace.idm.domain.security.globalauth

import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.DefaultAETokenServiceBaseIntegrationTest
import com.rackspace.idm.domain.service.EndUserTokenRequest
import org.apache.commons.lang.StringUtils

class GlobalAuthTokenProviderComponentTest extends DefaultAETokenServiceBaseIntegrationTest {

    def "can create ae token"() {
        User user = entityFactory.createUser()
        EndUserTokenRequest request = EndUserTokenRequest.builder().issuedToUser(user)
                .clientId(identityConfig.getStaticConfig().getCloudAuthClientId())
                .expireAfterCreation(10)
                .authenticationDomainId("domainId")
                .authenticatedByMethodGroup(AuthenticatedByMethodGroup.APIKEY)
                .build()
        UserScopeAccess sa = request.generateShellScopeAccessForRequest()
        assert StringUtils.isBlank(sa.accessTokenString)

        when:
        globalAuthTokenProvider.marshallTokenForUser(user, sa)

        then: "The token in passed in scope access is updated"
        StringUtils.isNotBlank(sa.getAccessTokenString())

        when:
        UserScopeAccess unmarshalledSa = globalAuthTokenProvider.unmarshallToken(sa.getAccessTokenString())

        then:
        validateUserScopeAccessesEqual(sa, unmarshalledSa)
    }
}
