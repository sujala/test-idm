package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientSecret
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootServiceTest

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapApplicationRoleRepositoryIntegrationTest extends RootServiceTest {

    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom
    @Autowired Configuration config

    @Autowired
    LdapApplicationRepository applicationDao

    @Autowired
    LdapApplicationRoleRepository applicationRoleDao


    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    def "getNextId returns UUID"() {
        given:
        def success = false
        applicationRoleDao.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",true)

        when:
        def id = applicationRoleDao.getNextId(LdapRepository.NEXT_ROLE_ID)
        try {
            Long.parseLong(id)
        } catch (Exception) {
            success = true
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "getNextId returns Long"() {
        given:
        def success = false
        applicationRoleDao.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",false)

        when:
        def id = applicationRoleDao.getNextId(LdapRepository.NEXT_ROLE_ID)
        try {
            Long.parseLong(id)
            success = true
        } catch (Exception) {
            //no-op
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "can create a role that contains propagate flag"() {
        given:
        def clientId = "clientId$sharedRandom"
        def roleName1 = "roleName1$sharedRandom"
        def roleName2 = "roleName2$sharedRandom"

        ClientSecret clientSecret = ClientSecret.newInstance("secret")

        def application = new Application(clientId, clientSecret, "name", "RCN-123").with {
            it.salt = "a1 b1"
            it.encryptionVersion = "0"
            it
        }

        def clientRole1 = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName1
            it.name = roleName1
            it.propagate = true
            return it
        }

        def clientRole2 = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName2
            it.name = roleName2
            it.propagate = false
            return it
        }

        applicationDao.addApplication(application)
        def createApplication = applicationDao.getApplicationByClientId(clientId)
        applicationRoleDao.addClientRole(createApplication, clientRole1)
        applicationRoleDao.addClientRole(createApplication, clientRole2)

        when:
        def createdClientRole1 = applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName1)
        def createdClientRole2 = applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName2)

        applicationRoleDao.deleteClientRole(createdClientRole1)
        applicationRoleDao.deleteClientRole(createdClientRole2)
        applicationDao.deleteApplication(createApplication)

        then:
        createdClientRole1.propagate == true
        createdClientRole2.propagate == false
    }

    def "can create a role that does not contain propagate flag"() {
        given:
        def clientId = "clientId$sharedRandom"
        def roleName = "roleName$sharedRandom"

        ClientSecret clientSecret = ClientSecret.newInstance("secret")

        def application = new Application(clientId, clientSecret, "name", "RCN-123").with {
            it.salt = "a1 b1"
            it.encryptionVersion = "0"
            it
        }

        def clientRole = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName
            it.name = roleName
            it.propagate = null
            return it
        }

        applicationDao.addApplication(application)
        def createApplication = applicationDao.getApplicationByClientId(clientId)
        applicationRoleDao.addClientRole(createApplication, clientRole)

        when:
        def createdClientRole = applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName)

        applicationRoleDao.deleteClientRole(createdClientRole)
        applicationDao.deleteApplication(createApplication)

        then:
        createdClientRole.propagate == false
    }
}
