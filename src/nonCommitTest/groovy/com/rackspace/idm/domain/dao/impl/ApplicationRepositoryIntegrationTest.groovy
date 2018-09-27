package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientSecret
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Shared
import spock.lang.Specification

@WebAppConfiguration
@ContextConfiguration(locations = "classpath:app-config.xml")
class ApplicationRepositoryIntegrationTest extends Specification {
    @Autowired
    ApplicationDao applicationDao

    @Shared def random

    String password = "Secret"
    String password2 = "Secret2"
    ClientSecret clientSecret = ClientSecret.newInstance(password)
    String name = "somerandomname"
    String customerId = "customerId"
    String encryptionSalt = "a1 b1"
    String version = "0"

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
    }

    def "application crud"() {
        given:
        def app = getApp(random)
        def updateApp = getUpdateApp(random)

        when:
        applicationDao.addApplication(app)
        Application retrievedApp = applicationDao.getApplicationByClientId(random)
        Application updatingApp = applicationDao.getApplicationByClientId(random)
        Application namedApp = applicationDao.getApplicationByName(name)

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
    }

    def getApp(id) {
        new Application(id, name).with {
            it.description = "description"
            it.enabled = true
            it.openStackType = openStackType
            return it
        }
    }

    def getUpdateApp(id) {
        new Application(id, name).with {
            it.clientSecretObj.toExisting()
            it.description = "description2"
            it.enabled = false
            it.openStackType = openStackType
            return it
        }
    }
}
