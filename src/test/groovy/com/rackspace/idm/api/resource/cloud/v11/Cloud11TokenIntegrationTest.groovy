package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspacecloud.docs.auth.api.v1.AuthData
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.*

class Cloud11TokenIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessService scopeAccessService

    def "Authenticate user within a disable domain should return 403" () {
        given:
        def user = utils11.createUser()
        def domainId = String.valueOf(user.mossoId)
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }

        when:
        utils11.authenticateWithKey(user.id, user.key)
        utils.updateDomain(domainId, domain)

        def cred = v1Factory.createUserKeyCredentials(user.id, user.key)
        def response = cloud11.authenticate(cred)

        def mossoCred = v1Factory.createMossoCredentials(user.mossoId, user.key)
        def mossoResponse = cloud11.adminAuthenticate(mossoCred)

        def nastCred = v1Factory.createNastCredentials(user.nastId, user.key)
        def nastResponse = cloud11.adminAuthenticate(nastCred)

        then:
        response.status ==  SC_FORBIDDEN
        mossoResponse.status ==  SC_FORBIDDEN
        nastResponse.status ==  SC_FORBIDDEN

        cleanup:
        utils11.deleteUser(user)
        utils.deleteTenantById(String.valueOf(user.mossoId))
        utils.deleteTenantById(user.nastId)
        utils.deleteDomain(domainId)
    }

    /**
     *  Produce tokens that contain userRsId.
     *
     */
    def "auth produces token with populated userRsId"() {
        def user = utils11.createUser()
        AuthData authData = utils11.authenticateWithKey(user.id, user.key)

        when:
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(authData.token.id)

        then:
        token != null
        token.userRsId != null
    }

}
