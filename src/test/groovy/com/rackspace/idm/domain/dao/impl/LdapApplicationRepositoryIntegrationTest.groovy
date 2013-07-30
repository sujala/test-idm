package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientSecret
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapApplicationRepositoryIntegrationTest extends Specification {
    @Autowired
    LdapApplicationRepository applicationDao

    @Shared def random

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
    }

    def "add application is null throws illegal argument"() {

    }

    def "can create application with salt and encryption versionId"() {
        given:
        def clientId = random
        def salt = "a1 b1"
        def encryptionVersion = "0"

        ClientSecret clientSecret = ClientSecret.newInstance("secret")

        def application = new Application(clientId, clientSecret, "name", "RCN-123").with {
            it.salt = salt
            it.encryptionVersion = encryptionVersion
            it
        }

        when:
        applicationDao.addClient(application)
        def client = applicationDao.getApplicationByClientId(clientId)
        applicationDao.deleteApplication(client)

        then:
        client.salt == salt
        client.encryptionVersion == encryptionVersion
    }
}
