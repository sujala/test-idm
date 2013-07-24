package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.pagination.PaginatorContext
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Permission
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
        mockCustomerService(service)
        mockTenantService(service)
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
        1 * applicationDao.addClient(applicationMock)
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
        def definedPermission = entityFactory.createDefinedPermission()
        def permission = entityFactory.createPermission()
        def clientRole = entityFactory.createClientRole()

        applicationDao.getApplicationByClientId(CLIENT_ID) >> entityFactory.createApplication()

        scopeAccessService.getPermissionsForParentByPermission(_, _) >> [ definedPermission ].asList()
        scopeAccessService.getPermissionsByPermission(_) >> [ permission ].asList()

        applicationDao.getClientRolesByClientId(CLIENT_ID) >> [ clientRole ].asList()
        tenantService.getTenantRolesForClientRole(_) >> [ entityFactory.createTenantRole() ].asList()

        when:
        service.delete(CLIENT_ID)

        then:
        scopeAccessService.removePermissionFromScopeAccess(_) >> { arg1 ->
            assert(arg1.get(0) instanceof Permission)
            return true
        }

        tenantService.deleteTenantRole(_) >> { arg1 ->
            assert(arg1.get(0) instanceof TenantRole)
        }

        applicationRoleDao.deleteClientRole(_) >> { arg1 ->
            assert(arg1.get(0) instanceof ClientRole)
        }

        then:
        1 * applicationDao.deleteClient(_)
    }

    def "getting available clientRoles paged calls applicationRoleDao method"() {
        given:
        def contextMock = Mock(PaginatorContext)

        when:
        service.getAvailableClientRolesPaged("applicationId", 0, 10, 1000)
        service.getAvailableClientRolesPaged(0, 10, 1000)

        then:
        1 * applicationRoleDao.getAvailableClientRolesPaged(0, 10, 1000) >> contextMock
        1 * applicationRoleDao.getAvailableClientRolesPaged("applicationId", 0, 10, 1000) >> contextMock
    }

    def "addDefinedPermission gets Customer from CustomerService; throws IllegalStateException if not found"() {
        given:
        def permission = entityFactory.createDefinedPermission()

        when:
        service.addDefinedPermission(permission)

        then:
        0 * customerDao.getCustomerByCustomerId(_)
        1 * customerService.getCustomer(_) >> null

        then:
        thrown(IllegalStateException)
    }

    def "addDefinedPermission gets Application; throws IllegalStateException if not found"() {
        given:
        def permission = entityFactory.createDefinedPermission()
        def customer = entityFactory.createCustomer()

        customerService.getCustomer(_) >> customer

        when:
        service.addDefinedPermission(permission)

        then:
        1 * applicationDao.getApplicationByClientId(_) >> null

        then:
        thrown(IllegalStateException)
    }
    
    def "addDefinedPermission adds new ScopeAccess if ScopeAccess for caller does not exist"() {
        given:
        def permission = entityFactory.createDefinedPermission()
        def customer = entityFactory.createCustomer()
        def application = entityFactory.createApplication()
        def scopeAccess = createScopeAccess()

        customerService.getCustomer(_) >> customer
        applicationDao.getApplicationByClientId(_) >> application
        scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> null

        when:
        service.addDefinedPermission(permission)

        then:
        0 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessService.addDirectScopeAccess(_, _) >> scopeAccess
    }

    def "addDefinedPermission gets permission from ScopeAccessService; throws DuplicateException if it already exists"() {
        given:
        def permission = entityFactory.createDefinedPermission()
        def customer = entityFactory.createCustomer()
        def application = entityFactory.createApplication()
        def scopeAccess = createScopeAccess()

        customerService.getCustomer(_) >> customer
        applicationDao.getApplicationByClientId(_) >> application
        scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> scopeAccess

        when:
        service.addDefinedPermission(permission)

        then:
        0 * scopeAccessDao.getPermissionsByPermission(_, _)
        1 * scopeAccessService.getPermissionForParent(_, _) >> permission

        then:
        thrown(DuplicateException)
    }

    def "addDefinedPermission adds permission"() {
        given:
        def permission = entityFactory.createDefinedPermission()
        def customer = entityFactory.createCustomer()
        def application = entityFactory.createApplication()
        def scopeAccess = createScopeAccess()

        customerService.getCustomer(_) >> customer
        applicationDao.getApplicationByClientId(_) >> application
        scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> scopeAccess
        scopeAccessService.getPermissionForParent(_, _) >> null

        when:
        service.addDefinedPermission(permission)

        then:
        0 * scopeAccessDao.definePermission(_, _)
        1 * scopeAccessService.definePermission(_, _) >> permission
    }

    def "deleteDefinedPermission gets a list of permissions using the DefinedPermission to be deleted"() {
        given:
        def permission = entityFactory.createDefinedPermission().with {
            it.clientId = "clientId"
            it.customerId = "customerId"
            it.permissionId = "permissionId"
            return it
        }

        when:
        service.deleteDefinedPermission(permission)

        then:
        0 * scopeAccessDao.getPermissionsByPermission(_)
        1 * scopeAccessService.getPermissionsByPermission(_) >> { Permission arg ->
            assert(arg.getClientId().equals(permission.getClientId()))
            assert(arg.getCustomerId().equals(permission.getCustomerId()))
            assert(arg.getPermissionId().equals(permission.getPermissionId()))
            def newPermission = entityFactory.createPermission()
            return [ newPermission ].asList()
        }
    }

    def "deleteDefinedPermission deletes all permissions connected to Permission to be deleted"() {
        given:
        def definedPermission = entityFactory.createDefinedPermission().with {
            it.clientId = "clientId"
            it.customerId = "customerId"
            it.permissionId = "permissionId"
            return it
        }
        def permission = entityFactory.createPermission()

        scopeAccessService.getPermissionsByPermission(_) >> [ permission, permission ].asList()

        when:
        service.deleteDefinedPermission(definedPermission)

        then:
        0 * scopeAccessDao.removePermissionFromScopeAccess(_)
        2 * scopeAccessService.removePermission(_)
    }

    def "getDefinedPermissionByClientIdAndPermissionId gets permission permission by clientId and permissionId"() {
        given:
        def application = entityFactory.createApplication().with {
            it.rcn = "rcn"
            return it
        }

        when:
        service.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId")

        then:
        1 * applicationDao.getApplicationByClientId(_) >> application
        1 * scopeAccessService.getPermissionForParent(_, _) >> { arg1, Permission arg2 ->
            assert(arg2.getPermissionId().equals("permissionId"))
            assert(arg2.getCustomerId().equals(application.getRCN()))
            assert(arg2.getClientId().equals(application.getClientId()))
        }
    }

    def "getDefinedPermissionByClient gets list of permissions by clientId and permissionId"() {
        given:
        def application = entityFactory.createApplication().with {
            it.rcn = "rcn"
            return it
        }

        when:
        service.getDefinedPermissionsByClient(application)

        then:
        1 * scopeAccessService.getPermissionsForParentByPermission(_, _) >> { arg1, Permission arg2 ->
            assert(arg2.getCustomerId().equals(application.getRCN()))
            assert(arg2.getClientId().equals(application.getClientId()))
            return [ ].asList()
        }
    }

    def "updateDefinedPermission uses scopeAccessService to update permission"() {
        given:
        def definedPermission = entityFactory.createDefinedPermission()

        when:
        service.updateDefinedPermission(definedPermission)

        then:
        0 * scopeAccessDao.updatePermissionForScopeAccess(definedPermission)
        1 * scopeAccessService.updatePermission(definedPermission)
    }

    def "getClientService gets list of ScopeAccess for application from ScopeAccessService"() {
        given:
        def application = entityFactory.createApplication()
        def scopeAccess = createScopeAccess().with {
            it.clientId = "clientId"
            return it
        }

        when:
        def result = service.getClientServices(application)

        then:
        0 * scopeAccessDao.getScopeAccessesByParent(_)
        1 * scopeAccessService.getScopeAccessesForParent(_) >> [ scopeAccess ].asList()

        then:
        1 * applicationDao.getApplicationByClientId(_) >> application
        result.getClients().size() == 1
        result.getClients().get(0).name.equals(application.getName())
    }

    def "deleteClientRole gets associated tenantRoles via TenantService"() {
        given:
        def role = entityFactory.createClientRole()

        when:
        service.deleteClientRole(role)

        then:
        0 * tenantDao.getAllTenantRolesForClientRole(_)
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

    def "getUserIdentityRole gets Identity Application and list of identity roles"() {
        given:
        def application = entityFactory.createApplication()
        def user = entityFactory.createUser()

        when:
        service.getUserIdentityRole(user)

        then:
        1 * applicationDao.getApplicationByClientId(_) >> application
        1 * applicationRoleDao.getIdentityRoles(application, _) >> [].asList()
    }

    def "getUserIdentityRole gets users tenantRole matching identityRoles"() {
        given:
        def application = entityFactory.createApplication()
        def user = entityFactory.createUser()
        def clientRole = entityFactory.createClientRole().with {
            it.id = "id"
            return it
        }
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "id"
            return it
        }

        applicationDao.getApplicationByClientId(_) >> application
        applicationRoleDao.getIdentityRoles(_, _) >> [ clientRole ].asList()

        when:
        service.getUserIdentityRole(user)

        then:
        1 * tenantService.getTenantRoleForUserById(_, _) >> tenantRole
    }

    def "getUserIdentityRole returns null if user has no identityRole"() {
        given:
        def application = entityFactory.createApplication()
        def user = entityFactory.createUser()
        def clientRole = entityFactory.createClientRole()

        applicationDao.getApplicationByClientId(_) >> application
        applicationRoleDao.getIdentityRoles(_, _) >> [ clientRole ].asList()
        tenantService.getTenantRoleForUserById(_, _) >> null

        when:
        def result = service.getUserIdentityRole(user)

        then:
        result == null
    }

    def "authenticate passes control to the Dao level"() {
        when:
        service.authenticate("clientId", "Secret")

        then:
        1 * applicationDao.authenticate("clientId", "Secret")
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
}
