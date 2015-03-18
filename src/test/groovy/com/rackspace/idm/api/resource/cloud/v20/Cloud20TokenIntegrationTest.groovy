package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
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

    @Unroll
    def "Authenticate racker, exposeUsername = #exposeUsername"() {
        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RACKER_USERNAME_ON_AUTH_ENABLED_PROP, exposeUsername)
        AuthenticateResponse auth = utils.authenticateRacker(RACKER, RACKER_PASSWORD)

        then:
        auth != null
        auth.user != null
        auth.user.id == RACKER
        if(exposeUsername) {
            assert auth.user.name == RACKER
        } else {
            assert auth.user.name == null
        }

        cleanup:
        reloadableConfiguration.reset()

        where:
        exposeUsername | _
        true           | _
        false          | _
    }

    def "Authenticate racker with no groups"() {
        when:
        def auth = utils.authenticateRacker(RACKER_NOGROUP, RACKER_NOGROUP_PASSWORD)

        then:
        auth != null
    }

    /**
     *  MUST produce tokens that contain userRsId
     *
     */
    def "auth produces token with populated userRsId and username"() {
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        when:
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)

        then:
        token != null
        token.userRsId != null
    }
}
