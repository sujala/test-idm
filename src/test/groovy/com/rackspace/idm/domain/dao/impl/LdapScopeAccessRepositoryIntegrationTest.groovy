package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientSecret
import com.rackspace.idm.domain.entity.ScopeAccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapScopeAccessRepositoryIntegrationTest extends Specification {

    @Autowired
    def LdapScopeAccessRepository scopeAccessDao

    @Autowired
    def LdapApplicationRepository applicationDao

    @Shared def randomness = UUID.randomUUID()
    @Shared def random

    @Shared def clientRCN
    @Shared def accessToken

    def setupSpec() {
        random = ("$randomness").replace('-',"")
        clientRCN = "clientRCN$random"
        accessToken = random
    }

    def "scopeAccess can store authenticatedBy field"() {
        when:
        def clientId = "client$random"
        def client = createClient(clientId)
        applicationDao.addClient(client)

        def scopeAccess = createScopeAccess(clientId, input)
        scopeAccessDao.addDirectScopeAccess(client.getUniqueId(), scopeAccess)
        def retrievedScopeAccess = scopeAccessDao.getScopeAccessByAccessToken(accessToken)
        scopeAccessDao.deleteScopeAccess(retrievedScopeAccess);
        applicationDao.deleteClient(client)

        then:
        expected as Set == retrievedScopeAccess.authenticatedBy as Set

        where:
        input                        | expected
        ["RSA"].asList()             | ["RSA"]
        ["RSA", "Password"].asList() | ["RSA", "Password"]
        null                         | []
    }

    def createClient(clientId) {
        new Application(clientId, ClientSecret.newInstance("secret"), "name", clientRCN)
    }

    def createScopeAccess(clientId, authenticatedBy) {
        new ScopeAccess().with {
            it.clientId = clientId
            it.clientRCN = clientRCN
            it.accessTokenExp = new Date()
            it.accessTokenString = accessToken
            it.authenticatedBy = authenticatedBy
            return it
        }
    }
}
