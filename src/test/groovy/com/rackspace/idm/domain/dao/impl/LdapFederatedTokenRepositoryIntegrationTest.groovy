package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.FederatedToken
import com.rackspace.idm.domain.entity.User
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
    ScopeAccessDao scopeAccessDao

    @Autowired
    Configuration configuration

    def "getFederatedTokensByUserId gets federated tokens for user"() {
        given:
        def userId = testUtils.getRandomUUID()
        def username = testUtils.getRandomUUID("user")
        def user = createUser(userId, username)
        ldapFederatedUserRepository.addUser(user, DEFAULT_IDP_NAME)
        def addedUser = ldapFederatedUserRepository.getUserByUsername(username, DEFAULT_IDP_NAME)
        def token = createFederatedToken(addedUser)
        scopeAccessDao.addScopeAccess(addedUser, token)

        when:
        def tokens = ldapFederatedTokenRepository.getFederatedTokensByUserId(user.id).toList()

        then:
        tokens.accessTokenString.contains(token.accessTokenString)

        cleanup:
        ldapFederatedUserRepository.deleteObject(addedUser)
    }

    def createUser(String id, String username) {
        new User().with {
            it.id = id
            it.username = username
            return it
        }
    }

    def createFederatedToken(user) {
        FederatedToken token = new FederatedToken();
        token.setUserRsId(user.getId());
        token.setAccessTokenString(testUtils.getRandomUUID());
        token.setAccessTokenExp(new DateTime().plusSeconds(100).toDate());
        token.setUsername(user.getUsername());
        token.setClientId(configuration.getString("cloudAuth.clientId"));
        token.setIdpName(DEFAULT_IDP_NAME);
        return token
    }

}
