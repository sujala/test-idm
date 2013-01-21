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
import spock.lang.Specification
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 19/12/12
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */
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
        mockDaos()
    }

    def "add verifies that an application with name does not exist in directory"() {
        when:
        service.add(entityFactory.createApplication())

        then:
        1 * applicationDao.getClientByClientname(_) >> entityFactory.createApplication()
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
        1 * applicationDao.getClientByClientId(CLIENT_ID) >> null
        thrown(NotFoundException)
    }

    def "delete deletes attached defined permissions and clientRoles before deleting application"() {
        given:
        def definedPermission = entityFactory.createDefinedPermission()
        def permission = entityFactory.createPermission()
        def clientRole = entityFactory.createClientRole()

        applicationDao.getClientByClientId(CLIENT_ID) >> entityFactory.createApplication()

        scopeAccessDao.getPermissionsByParentAndPermission(_, _) >> [ definedPermission ].asList()
        scopeAccessDao.getPermissionsByPermission(_) >> [ permission ].asList()

        applicationDao.getClientRolesByClientId(CLIENT_ID) >> [ clientRole ].asList()
        tenantDao.getAllTenantRolesForClientRole(_) >> [ entityFactory.createTenantRole() ].asList()

        when:
        service.delete(CLIENT_ID)

        then:
        scopeAccessDao.removePermissionFromScopeAccess(_) >> { arg1 ->
            assert(arg1.get(0) instanceof Permission)
            return true
        }

        tenantDao.deleteTenantRole(_) >> { arg1 ->
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

    def mockDaos() {
        mockScopeAccessDao(service)
        mockApplicationDao(service)
        mockCustomerDao(service)
        mockUserDao(service)
        mockTenantDao(service)
        mockApplicationRoleDao(service)
        mockTenantRoleDao(service)
    }
}
