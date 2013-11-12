package com.rackspace.idm.domain.dao.impl
import com.rackspace.idm.domain.entity.FederatedToken
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.ScopeAccessService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapFederatedUserRepositoryIntegrationTest extends Specification {
    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    ScopeAccessService scopeAccessService

    @Shared def IDP_NAME = "dedicated";

    @Shared def random
    @Shared def username

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
        username = "someName"+random
    }

    def "add and get a federated user by username"() {
        given:
        def id = UUID.randomUUID()
        def user = createUser(random, username)

        when:
        ldapFederatedUserRepository.addUser(user, IDP_NAME)

        then:
        def addedUser = ldapFederatedUserRepository.getUserByUsername(username, IDP_NAME)

        addedUser.id == user.id
        addedUser.username == user.username
    }

    def "get federated user by token"() {
        given:
        def id = UUID.randomUUID()
        def user = createUser(random, username)
        def federatedToken = createFederatedToken(random, username, random, new Date().plus(1), IDP_NAME)

        ldapFederatedUserRepository.addUser(user, IDP_NAME)
        scopeAccessService.addUserScopeAccess(user, federatedToken)

        when:
        def retrievedUser = ldapFederatedUserRepository.getUserByToken(federatedToken)

        then:
        retrievedUser.id == user.id
        retrievedUser.username == user.username
    }

    def createFederatedToken(String userId, String username, String tokenStr, Date expiration, String idpName)   {
        new FederatedToken().with {
            it.userRsId = userId
            it.accessTokenString = tokenStr
            it.accessTokenExp = expiration
            it.username = username
            it.clientId = "fakeClientId"
            it.idpName = idpName
            return it
        }
    }

    def createUser(String id, String username) {
        new User().with {
            it.id = id
            it.username = username
            return it
        }
    }
}
