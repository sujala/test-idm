package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientSecret
import com.rackspace.idm.exception.ClientConflictException

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapTenantRoleRepositoryIntegrationTest extends Specification {

    @Autowired
    private LdapTenantRoleRepository roleRepository

    @Autowired
    private LdapApplicationRepository applicationRepository;

    @Autowired
    private LdapUserRepository userRepository

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared User user;
    @Shared Application application;

    def setupSpec(){
        sharedRandom = ("$sharedRandomness").replace('-',"")
    }

    def cleanupSpec(){
    }

    def setup() {
        userRepository.addUser(getUser("$sharedRandom"))
        user = userRepository.getUserById("$sharedRandom")

        applicationRepository.addClient(getApplication("app$sharedRandom"))
        application = applicationRepository.getClientByClientId("app$sharedRandom")
    }

    def cleanup() {
        userRepository.deleteUser(user)
        applicationRepository.deleteClient(application)
    }

    def "tenant role crud for user"() {
        given:
        def applicationId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        def tenantId = "1"
        def tenantIds = [ tenantId ]
        def tenantRole = getTenantRole("name", "2", tenantIds, applicationId, user.id)

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole)
        List<TenantRole> roles = roleRepository.getTenantRolesForUser(user, tenantRole.clientId)
        TenantRole role = roles.get(0)
        TenantRole role2 = roleRepository.getTenantRolesForUser(user, applicationId, tenantId)
        roleRepository.deleteTenantRole(roles.get(0))
        List<TenantRole> rolesAfterDelete = roleRepository.getTenantRolesForUser(user)

        then:
        roles.size() == 1
        rolesAfterDelete.size() == 0
        def newTenantRole = roles.get(0)
        newTenantRole.roleRsId == tenantRole.roleRsId
        newTenantRole.tenantIds == tenantRole.tenantIds
        newTenantRole.clientId == tenantRole.clientId
        newTenantRole.userId == tenantRole.userId
    }

    def "tenant role crud for application"() {
        given:
        def tenantIds = [ "1" ]
        def tenantRole = getTenantRole("name", "2", tenantIds, application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToApplication(application, tenantRole)
        List<TenantRole> roles = roleRepository.getTenantRolesForApplication(application, tenantRole.clientId)
        TenantRole role = roles.get(0)
        TenantRole role2 = roleRepository.getTenantRoleForApplication(application, role.roleRsId)
        roleRepository.deleteTenantRole(role)
        List<TenantRole> rolesAfterDelete = roleRepository.getTenantRolesForApplication(application)

        then:
        roles.size() == 1
        role2 != null
        rolesAfterDelete.size() == 0
        def newTenantRole = roles.get(0)
        newTenantRole.roleRsId == tenantRole.roleRsId
        newTenantRole.tenantIds == tenantRole.tenantIds
        newTenantRole.clientId == tenantRole.clientId
        newTenantRole.userId == tenantRole.userId
    }

    def "add tenant role updates existing tenant role"() {
        given:
        def tenantId1 = "1"
        def tenantId2 = "2"
        def tenantRole1 = getTenantRole("name", "2", [ tenantId1 ], application.clientId, "userId")
        def tenantRole2 = getTenantRole("name", "2", [ tenantId2 ], application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole1)
        roleRepository.addTenantRoleToUser(user, tenantRole2)
        List<TenantRole> roles = roleRepository.getTenantRolesForUser(user)
        TenantRole role = roles.get(0)
        roleRepository.deleteTenantRole(role)

        then:
        roles.size() == 1
        role.tenantIds == ["1", "2"]
    }

    def "add tenant role already exists throws conflict exception"() {
        given:
        def tenantId = "1"
        def tenantRole = getTenantRole("name", "2", [ tenantId ], application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole)
        roleRepository.addTenantRoleToUser(user, tenantRole)

        then:
        thrown(ClientConflictException)
    }

    def "delete tenant deletes one at a time"() {
        given:
        def tenantId1 = "1"
        def tenantId2 = "2"
        def tenantRole1 = getTenantRole("name", "2", [ tenantId1 ], application.clientId, "userId")
        def tenantRole2 = getTenantRole("name", "2", [ tenantId2 ], application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole1)
        roleRepository.addTenantRoleToUser(user, tenantRole2)
        List<TenantRole> roles = roleRepository.getTenantRolesForUser(user)
        TenantRole role = roles.get(0)
        role.tenantIds = ["1"]
        roleRepository.deleteTenantRoleForUser(user, role)
        List<TenantRole> rolesAfterDelete1 = roleRepository.getTenantRolesForUser(user)

        role.tenantIds = ["2"]
        roleRepository.deleteTenantRoleForUser(user, role)
        List<TenantRole> rolesAfterDelete2 = roleRepository.getTenantRolesForUser(user)

        then:
        roles.size() == 1
        rolesAfterDelete1.size() == 1
        rolesAfterDelete1.get(0).tenantIds == ["2"]
        rolesAfterDelete2.size() == 0
    }

    def getTenantRole(name, roleRsId, tenantIds, clientId, userId) {
        new TenantRole().with {
            it.roleRsId = roleRsId
            it.tenantIds = tenantIds
            it.clientId = clientId
            it.userId = userId
            return it
        }
    }

    def getUser(id) {
        new User().with {
            it.id = id
            it.customerId = "customerId"
            it.username = "username$id"
            it.email = "username@test.com"
            it.password = 'Password1'
            return it
        }
    }

    def getApplication(id) {
        new Application().with {
            it.clientId = id
            it.name = id
            it.clientSecretObj = new ClientSecret()
            it.clientSecretObj.value = "secret"
            return it;
        }
    }
}
