package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientAuthenticationResult
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

    String password = "Secret"
    String password2 = "Secret2"
    ClientSecret clientSecret = ClientSecret.newInstance(password)
    ClientSecret clientSecret2 = ClientSecret.newInstance(password)
    String name = "somerandomname"
    String customerId = "customerId"
    String encryptionSalt = "a1 b1"
    String version = "0"
    String tokenScope = "scope"

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
    }

    def "application crud"() {
        given:
        def app = getApp(random)
        def updateApp = getUpdateApp(random)

        when:
        applicationDao.addClient(app)
        Application retrievedApp = applicationDao.getApplicationByClientId(random)
        Application updatingApp = applicationDao.getApplicationByClientId(random)
        Application namedApp = applicationDao.getApplicationByName(name)
        Application customerAndClientApp = applicationDao.getApplicationByCustomerIdAndClientId(customerId, random)
        Application scopeApp = applicationDao.getApplicationByScope(tokenScope)

        updatingApp.setDescription("description2")
        updatingApp.setClientSecretObj(ClientSecret.newInstance(password2))
        updatingApp.setEnabled(false)

        applicationDao.updateApplication(updatingApp)
        Application updatedApp = applicationDao.getApplicationByClientId(random)

        applicationDao.deleteApplication(updatedApp)
        Application deletedApp = applicationDao.getApplicationByClientId(random)

        then:
        deletedApp == null
        namedApp == retrievedApp
        customerAndClientApp == retrievedApp
        scopeApp == retrievedApp
    }

    def "can soft delete and unsoftdelete app"() {
        given:
        def app = getApp(random)

        when:
        applicationDao.addClient(app)
        Application retrievedApp = applicationDao.getApplicationByClientId(random)
        applicationDao.softDeleteApplication(app)

        Application notFoundApp = applicationDao.getApplicationByClientId(random)
        Application softDeletedById = applicationDao.getSoftDeletedApplicationById(random)
        Application softDeletedByName = applicationDao.getSoftDeletedClientByName(name)

        Application unsoftDeletingApp = applicationDao.getSoftDeletedApplicationById(random)
        applicationDao.unSoftDeleteApplication(unsoftDeletingApp)
        Application foundApp = applicationDao.getApplicationByClientId(random)
        applicationDao.deleteApplication(app)

        then:
        notFoundApp == null
        softDeletedById == softDeletedByName
        foundApp == retrievedApp
    }

    def getApp(id) {
        new Application(id, clientSecret, name, customerId).with {
            it.description = "description"
            it.clearPassword = password
            it.salt = encryptionSalt
            it.encryptionVersion = version
            it.enabled = true
            it.openStackType = openStackType
            it.scope = tokenScope
            return it
        }
    }

    def getUpdateApp(id) {
        new Application(id, clientSecret, name, customerId).with {
            it.clientSecretObj.toExisting()
            it.description = "description2"
            it.clearPassword = password2
            it.salt = encryptionSalt
            it.setEncryptionVersion(version)
            it.enabled = false
            it.openStackType = openStackType
            it.scope = tokenScope
            return it
        }
    }
}
