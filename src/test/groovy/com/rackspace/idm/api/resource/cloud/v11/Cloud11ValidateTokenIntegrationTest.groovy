package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspacecloud.docs.auth.api.v1.AuthData
import com.rackspacecloud.docs.auth.api.v1.Token
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_OK

class Cloud11ValidateTokenIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    LdapScopeAccessRepository scopeAccessDao

    @Shared def defaultUser, users

    def "Validate user's token within a disabled domain should return 404" () {
        given:
        def user = utils11.createUser()
        def domainId = Integer.toString(user.mossoId)
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }

        when:
        String key = utils11.getUserKey(user).key
        def user20 = utils.getUserByName(user.id)
        def authResponse = utils.authenticateApiKey(user20, key)
        utils.updateDomain(domainId, domain)
        def response = cloud11.validateToken(authResponse.token.id)

        then:
        response.status == SC_NOT_FOUND

        cleanup:
        utils11.deleteUser(user)
        utils.deleteTenantById(String.valueOf(user.mossoId))
        utils.deleteTenantById(user.nastId)
        utils.deleteDomain(domainId)
    }

    def "Validate Impersonated token should return 200 even if user is in a disabled domain" () {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        utils.updateDomain(domainId, domain)
        def resp = cloud11.validateToken(token)

        then:
        token != null
        resp.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "validate token with userid populated"() {
        def user = utils11.createUser()
        AuthData authData = utils11.authenticateWithKey(user.id, user.key)

        def tokenId = authData.token.id

        when:
        def rawResponse = cloud11.validateToken(tokenId)
        assert rawResponse.status == HttpStatus.SC_OK
        Token valResponse = rawResponse.getEntity(Token) //the service actually returns a FullToken, but Jaxb wants to unmarshall it as the Token class

        then:
        valResponse.id == tokenId
    }
}
