package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

class Cloud20TokenIntegrationTest extends RootIntegrationTest {

    @Autowired
    Configuration configuration

    @Autowired
    ScopeAccessService scopeAccessService

    @Shared def userAdmin, users

    def "FederatedIdp should not appear in persistent user authentication response" () {
        when:
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        then:
        auth != null
        assert auth.user.federatedIdp == null

        cleanup:
        utils.deleteUsers(users)
    }

    def "Authenticate racker with no groups"() {
        when:
        def auth = utils.authenticateRacker(RACKER_NOGROUP, RACKER_NOGROUP_PASSWORD)

        then:
        auth != null
    }

    /**
     *  2.10.x MUST produce tokens that contain both username and userRsId in order to be backward compatible
     * with 2.9.x (whose code expects both username and userId to be populated).
     *
     */
    def "auth produces token with populated userRsId and username"() {
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        when:
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)

        then:
        token != null
        token.username != null
        token.userRsId != null
    }

}
