package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.helpers.CloudTestUtils
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static com.rackspace.idm.Constants.*

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapFederatedTokenRepositoryIntegrationTest extends Specification {

    @Autowired
    CloudTestUtils testUtils

    @Autowired
    LdapFederatedTokenRepository ldapFederatedTokenRepository

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    @Autowired
    ScopeAccessDao scopeAccessDao

    @Autowired
    Configuration configuration

    private IdentityProvider commonIdentityProvider;

    def setup() {
        commonIdentityProvider = ldapIdentityProviderRepository.getIdentityProviderByName(Constants.DEFAULT_IDP_NAME)
    }

    def "getFederatedTokensByUserId gets federated tokens for user"() {
        given:
        def userId = testUtils.getRandomUUID()
        def username = testUtils.getRandomUUID("user")
        def user = createFederatedUser(userId, username)
        ldapFederatedUserRepository.addUser(commonIdentityProvider, user)
        def addedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, Constants.DEFAULT_IDP_NAME)
        def token = createFederatedToken(addedUser)
        scopeAccessDao.addScopeAccess(addedUser, token)

        when:
        def tokens = ldapFederatedTokenRepository.getFederatedTokensByUserId(user.id).toList()

        then:
        tokens.accessTokenString.contains(token.accessTokenString)

        cleanup:
        ldapFederatedUserRepository.deleteObject(addedUser)
    }

    def createFederatedUser(String id, String username) {
        new FederatedUser().with {
            it.id = id
            it.username = username
            it.domainId = "123"
            it.region="ORD"
            it.email="test@rackspace.com"
            it.federatedIdpUri = DEFAULT_IDP_URI
            return it
        }
    }

    def createFederatedToken(user) {
        UserScopeAccess token = new UserScopeAccess();
        token.setUserRsId(user.getId());
        token.setAccessTokenString(testUtils.getRandomUUID());
        token.setAccessTokenExp(new DateTime().plusSeconds(100).toDate());
        token.setClientId(configuration.getString("cloudAuth.clientId"));
        token.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION)

        return token
    }

    def createIdentityProvider() {
        IdentityProvider provider = new IdentityProvider()
    }

}
