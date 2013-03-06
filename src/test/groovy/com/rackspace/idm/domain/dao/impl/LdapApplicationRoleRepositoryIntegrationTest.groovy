package com.rackspace.idm.domain.dao.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 3/6/13
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapApplicationRoleRepositoryIntegrationTest extends RootServiceTest {

    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom

    @Autowired
    LdapApplicationRepository applicationDao

    @Autowired
    LdapApplicationRoleRepository applicationRoleDao


    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    def "can create a role that contains propagate flag"() {
        given:
        def clientId = "clientId$sharedRandom"
        def roleName1 = "roleName1$sharedRandom"
        def roleName2 = "roleName2$sharedRandom"

        def application = entityFactory.createApplication().with {
            it.clientId = clientId
            it.clientSecret = entityFactory.createClientSecret()
            return it
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

        applicationDao.addClient(application)
        def createApplication = applicationDao.getClientByClientId(clientId)
        applicationRoleDao.addClientRole(createApplication, clientRole1)
        applicationRoleDao.addClientRole(createApplication, clientRole2)

        when:
        def createdClientRole1 = applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName1)
        def createdClientRole2 = applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName2)

        applicationRoleDao.deleteClientRole(createdClientRole1)
        applicationRoleDao.deleteClientRole(createdClientRole2)
        applicationDao.deleteClient(createApplication)

        then:
        createdClientRole1.propagate == true
        createdClientRole2.propagate == false
    }

    def "can create a role that does not contain propagate flag"() {
        given:
        def clientId = "clientId$sharedRandom"
        def roleName = "roleName$sharedRandom"

        def application = entityFactory.createApplication().with {
            it.clientId = clientId
            it.clientSecret = entityFactory.createClientSecret()
            return it
        }

        def clientRole = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName
            it.name = roleName
            it.propagate = null
            return it
        }

        applicationDao.addClient(application)
        def createApplication = applicationDao.getClientByClientId(clientId)
        applicationRoleDao.addClientRole(createApplication, clientRole)

        when:
        def createdClientRole = applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName)

        applicationRoleDao.deleteClientRole(createdClientRole)
        applicationDao.deleteClient(createApplication)

        then:
        createdClientRole.propagate == false
    }
}
