package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
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

    @Autowired
    AuthorizationService authorizationService

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

    def "Authenticate racker"() {
        when:
        AuthenticateResponse auth = utils.authenticateRacker(RACKER, RACKER_PASSWORD)

        then:
        auth != null
        auth.user != null
        auth.user.id == RACKER
        assert auth.user.name == RACKER

        cleanup:
        reloadableConfiguration.reset()
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

    def "identity:get-token-endpoint-global can be used to retrieve endpoints for tokens"() {
        //start with role enabled
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)
        String uaToken = token.accessTokenString
        String iaToken = utils.getToken(users[0].username)

        def endpointRole = authorizationService.getCachedIdentityRoleByName(IdentityRole.GET_TOKEN_ENDPOINTS_GLOBAL.getRoleName())

        when: "user admin tries to load own endpoints w/ different tokens"
        def uaResponse = cloud20.listEndpointsForToken(uaToken, iaToken)

        then:
        uaResponse.status == HttpStatus.SC_FORBIDDEN

        when: "user admin tries to load own endpoints w/ same tokens"
        uaResponse = cloud20.listEndpointsForToken(uaToken, uaToken)

        then:
        uaResponse.status == HttpStatus.SC_OK

        when: "give user global endpoint role"
        utils.addRoleToUser(userAdmin, endpointRole.id)
        uaResponse = cloud20.listEndpointsForToken(uaToken, iaToken)

        then: "user admin can now access token endpoints"
        uaResponse != null
        uaResponse.status == HttpStatus.SC_OK

        when: "identity admin tries to load token endpoints"
        def iaResponse = cloud20.listEndpointsForToken(iaToken, uaToken)

        then: "allowed"
        iaResponse.status == HttpStatus.SC_OK

        cleanup:
        reloadableConfiguration.reset()
        try {
            utils.deleteUsers(users)
        } catch (Exception ex) {/*ignore*/
        }
    }

}
