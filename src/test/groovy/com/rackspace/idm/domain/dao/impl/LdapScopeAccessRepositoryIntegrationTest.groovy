package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientScopeAccess
import com.rackspace.idm.domain.entity.ClientSecret
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
        applicationDao.addApplication(client)

        def scopeAccess = createScopeAccess(clientId, input)
        scopeAccessDao.addScopeAccess(client, scopeAccess)
        def retrievedScopeAccess = scopeAccessDao.getScopeAccessByAccessToken(accessToken)
        scopeAccessDao.deleteScopeAccess(retrievedScopeAccess);
        applicationDao.deleteApplication(client)

        then:
        expected as Set == retrievedScopeAccess.authenticatedBy as Set

        where:
        input                        | expected
        ["RSA"].asList()             | ["RSA"]
        ["RSA", "Password"].asList() | ["RSA", "Password"]
        []                           | []
        null                         | []
    }

    def "ScopeAccess returns the create date" () {
        when:
        def clientId = "otherClient$random"
        def client = createClient(clientId)
        applicationDao.addApplication(client)

        def scopeAccess = createScopeAccess(clientId, ["RSA"].asList())
        scopeAccessDao.addScopeAccess(client, scopeAccess)
        def retrievedScopeAccess1 = scopeAccessDao.getScopeAccessByAccessToken(accessToken)
        def retrievedScopeAccess2 = scopeAccessDao.getScopeAccessByAccessToken(accessToken)
        def retrievedScopeAccess3 = scopeAccessDao.getScopeAccessByAccessToken(accessToken)
        scopeAccessDao.deleteScopeAccess(retrievedScopeAccess1);
        applicationDao.deleteApplication(client)

        then:
        retrievedScopeAccess1.createTimestamp != null
        retrievedScopeAccess1.createTimestamp == retrievedScopeAccess2.createTimestamp
        retrievedScopeAccess2.createTimestamp == retrievedScopeAccess3.createTimestamp
        retrievedScopeAccess2.createTimestamp == retrievedScopeAccess3.createTimestamp
    }

    def createClient(clientId) {
        new Application(clientId, ClientSecret.newInstance("secret"), "name", clientRCN)
    }

    def createScopeAccess(clientId, authenticatedBy) {
        new ClientScopeAccess().with {
            it.clientId = clientId
            it.clientRCN = clientRCN
            it.accessTokenExp = new Date()
            it.accessTokenString = accessToken
            it.authenticatedBy = authenticatedBy
            return it
        }
    }
}
