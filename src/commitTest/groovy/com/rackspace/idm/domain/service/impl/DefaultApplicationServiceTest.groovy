package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultApplicationServiceTest extends RootServiceTest {

    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom
    @Shared CLIENT_ID = "clientId"

    @Shared ApplicationService service

    def setupSpec() {
        service = new DefaultApplicationService()
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        mockConfiguration(service)
        mockApplicationDao(service)
        mockApplicationRoleDao(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockRoleService(service)
    }

    def "add verifies that an application with name does not exist in directory"() {
        when:
        service.add(entityFactory.createApplication())

        then:
        1 * applicationDao.getApplicationByName(_) >> entityFactory.createApplication()
        thrown(DuplicateException)
    }

    def "add sets clientId and client secret before adding the application to the directory"() {
        given:
        def applicationMock = Mock(Application)
        applicationMock.getName() >> "applicationName"

        when:
        service.add(applicationMock)

        then:
        1 * applicationMock.setClientId(_)
        1 * applicationMock.setClientSecretObj(_)

        then:
        1 * applicationDao.addApplication(applicationMock)
    }

    def "delete throws NotFoundException if application does not exist within the directory"() {
        when:
        service.delete(CLIENT_ID)

        then:
        1 * applicationDao.getApplicationByClientId(CLIENT_ID) >> null
        thrown(NotFoundException)
    }

    def "delete deletes attached defined permissions and clientRoles before deleting application"() {
        given:
        def clientRole = entityFactory.createClientRole()

        applicationDao.getApplicationByClientId(CLIENT_ID) >> entityFactory.createApplication()

        applicationRoleDao.getClientRolesForApplication(entityFactory.createApplication()) >> [ clientRole ].asList()
        tenantService.getTenantRolesForClientRole(_) >> [ entityFactory.createTenantRole() ].asList()

        when:
        service.delete(CLIENT_ID)

        then:
        tenantService.deleteTenantRole(_) >> { arg1 ->
            assert(arg1.get(0) instanceof TenantRole)
        }

        applicationRoleDao.deleteClientRole(_) >> { arg1 ->
            assert(arg1.get(0) instanceof ClientRole)
        }

        then:
        1 * applicationDao.deleteApplication(_)
    }

    def "deleteClientRole gets associated tenantRoles via TenantService"() {
        given:
        def role = entityFactory.createClientRole()

        when:
        service.deleteClientRole(role)

        then:
        1 * tenantService.getTenantRolesForClientRole(_) >> [ ].asList()
    }

    def "deleteClientRole deletes associated tenantRoles before deleting specified clientrole"() {
        given:
        def role = entityFactory.createClientRole()
        def tenantRole = entityFactory.createTenantRole()

        tenantService.getTenantRolesForClientRole(role) >> [ tenantRole, tenantRole ].asList()

        when:
        service.deleteClientRole(role)

        then:
        2 * tenantService.deleteTenantRole(_)

        then:
        1 * applicationRoleDao.deleteClientRole(role)
    }

    def "getUserIdentityRole gets a list of identity roles"() {
        given:
        def user = entityFactory.createUser()

        when:
        service.getUserIdentityRole(user)

        then:
        1 * roleService.getIdentityAccessRoles() >> [].asList()
    }

    def "getUserIdentityRole gets users tenantRole matching identityRoles"() {
        given:
        def user = entityFactory.createUser()
        def clientRole = entityFactory.createClientRole().with {
            it.id = "id"
            return it
        }
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "id"
            return it
        }
        roleService.getIdentityAccessRoles() >> [ clientRole ].asList()

        when:
        service.getUserIdentityRole(user)

        then:
        1 * tenantService.getTenantRolesForUserById(_, _) >> [tenantRole]
    }

    def "getUserIdentityRole returns null if user has no identityRole"() {
        given:
        def user = entityFactory.createUser()
        def clientRole = entityFactory.createClientRole()

        roleService.getIdentityAccessRoles() >> [ clientRole ].asList()
        tenantService.getTenantRolesForUserById(_, _) >> [].asList()

        when:
        def result = service.getUserIdentityRole(user)

        then:
        result == null
    }

    def "calling getRoleWithLowestWeight returns clientRole with lowers weight"() {
        given:

        def defaultRole = entityFactory.createClientRole().with {
            it.name = "identity:default"
            it.rsWeight = 2000
            return it
        }

        def userManageRole = entityFactory.createClientRole().with {
            it.name = "identity:user-manage"
            it.rsWeight = 1500
            return it
        }

        when:
        def result = service.getRoleWithLowestWeight([defaultRole, userManageRole].asList())

        then:
        result == userManageRole
    }

    def "calling getRoleWithLowestWeight returns null with empty list"() {
        when:
        def result = service.getRoleWithLowestWeight([].asList())

        then:
        result == null
    }

    def "getApplicationByScopeAccess throws error when clientId is null" () {
        given:
        ScopeAccess scopeAccess = entityFactory.createScopeAccess()

        when:
        service.getApplicationByScopeAccess(scopeAccess)

        then:
        scopeAccessService.getClientIdForParent(_) >> null
        thrown(NotFoundException)
    }

    def "testing getCachedClientRoleById returning ImmutableClientRole"() {
        given:
        def clientRole = entityFactory.createClientRole().with {
            it.id = "id"
            return it
        }
        applicationRoleDao.getClientRole(_) >> clientRole

        when:
        def result = service.getCachedClientRoleById("id")

        then:
        1 * applicationRoleDao.getClientRole(_) >> clientRole
        result != null
        result.getId() == clientRole.id
    }

    def "testing getCachedClientRoleByName returning ImmutableClientRole"() {
        given:
        def clientRole = entityFactory.createClientRole().with {
            it.id = "id"
            it.name = "test"
            return it
        }
        applicationRoleDao.getRoleByName(_) >> clientRole

        when:
        def result = service.getCachedClientRoleByName("test")

        then:
        1 * applicationRoleDao.getRoleByName(_) >> clientRole
        result != null
        result.getName() == clientRole.name
    }
}
