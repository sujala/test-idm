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

    /*
     * In 2.10.x the username property in tokens has been deprecated in favor of using the userRsId property. These tests
     * verify that 2.10.x can correctly use tokens that match the following:
     * <ol>
     *     <li>Tokens that contain both username and userRsId (2.9.x and earlier format)</li>
     *     <li>Tokens that only contain userRsId (2.11.x or later will remove the population of username)</li>
     * </ol>
     *
     * Furthermore, 2.10.x MUST produce tokens that contain both username and userRsId in order to be backward compatible
     * with 2.9.x (whose code expects both username and userId to be populated)
     *
     */
    def "validate token with userid and username populated"() {
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

    def "validate token with userid populated and null username"() {
        def saToken = utils.getServiceAdminToken()
        def user = utils11.createUser()
        AuthData authData = utils11.authenticateWithKey(user.id, user.key)

        UserScopeAccess origToken = scopeAccessService.getScopeAccessByAccessToken(authData.token.id)

        //null out the username
        origToken.username = null
        scopeAccessDao.updateObjectAsIs(origToken)

        when:
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(authData.token.id)

        then:
        token != null
        token.username == null
        token.userRsId != null

        when:
        def rawResponse = cloud11.validateToken(token.accessTokenString)
        assert rawResponse.status == HttpStatus.SC_OK
        Token valResponse = rawResponse.getEntity(Token) //the service actually returns a FullToken, but Jaxb wants to unmarshall it as the Token class

        then:
        valResponse.id == authData.token.id
    }

}
