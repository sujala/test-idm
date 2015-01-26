package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.UserScopeAccess
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
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    @Autowired
    ScopeAccessService scopeAccessService

    @Shared def random
    @Shared def username

    private IdentityProvider commonIdentityProvider;

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
        username = "someName"+random

        commonIdentityProvider = ldapIdentityProviderRepository.getIdentityProviderByName(Constants.DEFAULT_IDP_NAME)
    }

    def "add and get a federated user"() {
        given:
        def user = createFederatedUser(random, username)

        when:
        ldapFederatedUserRepository.addUser(commonIdentityProvider, user)

        then:
        user.id != null
        user.getLdapEntry() != null

        when:
        def addedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, Constants.DEFAULT_IDP_NAME)

        then:
        addedUser != null
        addedUser.id == user.id
        addedUser.username == user.username
    }

    def "add federated user with inconsistent uri throws exception"() {
        given:
        def user = createFederatedUser(random, username).with({
            it.federatedIdpUri = "wrongUri"
            return it
        })

        when:
        ldapFederatedUserRepository.addUser(commonIdentityProvider, user)

        then:
        IllegalArgumentException ex = thrown()
    }

    def "get federated user by token"() {
        given:
        def user = createFederatedUser(random, username)
        ldapFederatedUserRepository.addUser(commonIdentityProvider, user)

        def federatedToken = createFederatedToken(user.id, username, random, new Date().plus(1), Constants.DEFAULT_IDP_NAME)
        scopeAccessService.addUserScopeAccess(user, federatedToken)

        when:
        def retrievedUser = ldapFederatedUserRepository.getUserByToken(federatedToken)

        then:
        retrievedUser.id == user.id
        retrievedUser.username == user.username
    }

    def createFederatedToken(String userId, String username, String tokenStr, Date expiration, String idpName)   {
        new UserScopeAccess().with {
            it.userRsId = userId
            it.username = username
            it.accessTokenString = tokenStr
            it.accessTokenExp = expiration
            it.clientId = "fakeClientId"
            it.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
            return it
        }
    }

    def createFederatedUser(String id, String username) {
        new FederatedUser().with {
            it.id = id
            it.username = username
            it.domainId = "123"
            it.region="ORD"
            it.email="test@rackspace.com"
            it.federatedIdpUri = Constants.DEFAULT_IDP_URI
            return it
        }
    }

}
