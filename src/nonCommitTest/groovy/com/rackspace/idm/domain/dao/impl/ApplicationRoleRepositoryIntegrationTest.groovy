package com.rackspace.idm.domain.dao.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.ClientSecret
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.collections4.IteratorUtils
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

@WebAppConfiguration
@ContextConfiguration(locations = "classpath:app-config.xml")
class ApplicationRoleRepositoryIntegrationTest extends RootServiceTest {

    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom
    @Autowired Configuration config

    @Autowired
    private IdentityConfig identityConfig

    @Autowired
    ApplicationDao applicationDao

    @Autowired
    ApplicationRoleDao applicationRoleDao


    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    @Unroll
    def "roles are returned if their rsWeight is greater than maxWeightAvailable"() {
        given:
        def clientId = "clientId$sharedRandom"
        def roleName = "roleName1$sharedRandom"

        ClientSecret clientSecret = ClientSecret.newInstance("secret")

        def application = new Application(clientId, "name")

        def clientRole = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName
            it.name = roleName
            it.roleType = RoleTypeEnum.PROPAGATE
            it.rsWeight = rsWeight
            return it
        }

        applicationDao.addApplication(application)
        def createApplication = applicationDao.getApplicationByClientId(clientId)
        applicationRoleDao.addClientRole(createApplication, clientRole)

        when:
        def context = applicationRoleDao.getAvailableClientRolesPaged(0, 10000, maxWeightAvailable)
        boolean containsRole = context.valueList.name.contains(clientRole.name)

        then:
        containsRole == shouldContainRole

        cleanup:
        def createdClientRole = applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName)
        applicationRoleDao.deleteClientRole(createdClientRole)
        applicationDao.deleteApplication(createApplication)

        where:
        rsWeight    | maxWeightAvailable  | shouldContainRole
        500         | 0                   | true
        500         | 500                 | false
        500         | 1000                | false
    }

    def "can create a role that contains propagate flag"() {
        given:
        def clientId = "clientId$sharedRandom"
        def roleName1 = "roleName1$sharedRandom"
        def roleName2 = "roleName2$sharedRandom"

        ClientSecret clientSecret = ClientSecret.newInstance("secret")

        def application = new Application(clientId, "name")

        def clientRole1 = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName1
            it.name = roleName1
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }

        def clientRole2 = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName2
            it.name = roleName2
            it.roleType = RoleTypeEnum.STANDARD
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

        def application = new Application(clientId, "name")

        def clientRole = entityFactory.createClientRole().with {
            it.clientId = clientId
            it.id = roleName
            it.name = roleName
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

    def "get clientRoles from ids"() {
        given:
        List<String> roleIds = ["5", "6"].asList()
        List<ClientRole> clientRoles = new ArrayList<>()

        when:
        for(ClientRole role : applicationRoleDao.getClientRoles(roleIds)) {
            clientRoles.add(role)
        }

        then:
        clientRoles != null
        clientRoles.size() == 2
        clientRoles.id.contains("5")
        clientRoles.id.contains("6")
    }

    def "get clientRoles from emtpy list"() {
        given:
        List<String> roleIds = [].asList()
        List<ClientRole> clientRoles = new ArrayList<>()

        when:
        for(ClientRole role : applicationRoleDao.getClientRoles(roleIds)) {
            clientRoles.add(role)
        }

        then:
        clientRoles != null
        clientRoles.size() == 0
    }

    def "can retrieve all identity roles"() {
        given:
        List<String> minRoleNamesExpected = IdentityUserTypeEnum.getUserTypeRoleNames()

        when:
        List<ClientRole> identityRoles = IteratorUtils.toList(applicationRoleDao.getAllIdentityRoles().iterator())

        then:
        identityRoles.size() > 0
        for (String expectedRoleName : minRoleNamesExpected) {
            identityRoles.find {it.name == expectedRoleName} != null
        }

        //only returns "identity:" roles
        identityRoles.find {!(it.name.startsWith(GlobalConstants.IDENTITY_ROLE_PREFIX))} == null
    }
}
