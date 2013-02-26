package com.rackspace.idm.api.resource.roles

import com.rackspace.api.idm.v1.Role
import com.rackspace.idm.api.converter.RolesConverter
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.validation.InputValidator
import spock.lang.Shared
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.exception.ForbiddenException
import testHelpers.RootServiceTest

class RolesResourceTest extends RootServiceTest {

    @Shared RolesResource service
    @Shared def SPECIAL_RSWEIGHT = "cloudAuth.special.rsWeight"

    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def authToken = "authToken"
    @Shared def role

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }
    def setup() {
        applicationService = Mock(ApplicationService)
        authorizationService = Mock(AuthorizationService)
        rolesConverter = Mock(RolesConverter)
        rolesConverter.toRoleJaxbFromClientRole(_) >> jaxbMock
        rolesConverter.toRoleJaxbFromTenantRole(_) >> jaxbMock
        rolesConverter.toRoleJaxbFromRoleString(_) >> jaxbMock
        rolesConverter.toClientRole(_) >> entityFactory.createClientRole()

        service = new RolesResource(rolesConverter, authorizationService, applicationService, Mock(InputValidator))

        mockPrecedenceValidator(service)
        mockApplicationRolePaginator(service)
        mockConfiguration(service)
        mockUserService(service)
        mockScopeAccessService(service)

        role = createRole("role", "applicationId")
    }

    def "addRole sets rsWeight"() {
        when:
        service.addRole(authToken, role)

        then:
        1 * config.getInt(SPECIAL_RSWEIGHT) >> 500
        1 * applicationService.addClientRole(_) >> { ClientRole arg ->
            assert(arg.getRsWeight() == 500)
            arg.id = "id"
            return arg
        }
    }

    def "updateRole verifies that application for cRole is not null"() {
        given:
        def scopeAccess = createUserScopeAccess()

        when:
        service.updateRole(authToken, "roleId", role)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess
        1 * applicationService.getById(_) >> null

        then:
        thrown(BadRequestException)
    }

    def "updateRole verifies that cRole exists"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def application = entityFactory.createApplication()

        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess
        applicationService.getById(_) >> application

        when:
        service.updateRole(authToken, "roleId", role)

        then:
        1 * applicationService.getClientRoleById("roleId") >> null

        then:
        thrown(NotFoundException)
    }

    def "updateRole throws forbidden exception when attempting to update identity:cRole"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def application = entityFactory.createApplication()
        def cRole = entityFactory.createClientRole("identity:role")

        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess
        applicationService.getById(_) >> application
        applicationService.getClientRoleById("roleId") >> cRole

        when:
        service.updateRole(authToken, "roleId", role)

        then:
        thrown(ForbiddenException)
    }

    def "updateRole validates callers precedence"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def application = entityFactory.createApplication()
        def cRole = entityFactory.createClientRole("someRole")
        def user = entityFactory.createUser()

        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess
        applicationService.getById(_) >> application
        applicationService.getClientRoleById("roleId") >> cRole

        when:
        service.updateRole(authToken, "roleId", role)

        then:
        1 * userService.getUserByAuthToken(authToken) >> user
        1 * precedenceValidator.verifyCallerRolePrecedence(_, _)
    }

    def "updatRole updates cRole"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def application = entityFactory.createApplication()
        def cRole = entityFactory.createClientRole("someRole")
        def user = entityFactory.createUser()

        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess
        applicationService.getById(_) >> application
        applicationService.getClientRoleById("roleId") >> cRole
        userService.getUserByAuthToken(authToken) >> user

        when:
        service.updateRole(authToken, "roleId", role)

        then:
        1 * applicationService.updateClientRole(_)
    }

    def "deleteRole verifies that clientRole exists"() {
        given:
        def scopeAccess = createUserScopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess

        when:
        service.deleteRole(authToken, "roleId")

        then:
        1 * applicationService.getClientRoleById("roleId") >> null

        then:
        thrown(NotFoundException)
    }

    def "deleteRole prevents deleting cRoles with name 'identity:'"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def cRole = entityFactory.createClientRole("identity:role")

        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess

        when:
        service.deleteRole(authToken, "roleId")

        then:
        1 * applicationService.getClientRoleById("roleId") >> cRole

        then:
        thrown(ForbiddenException)
    }

    def "deleteRole verifies callers precedence"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def cRole = entityFactory.createClientRole("someRole")
        def user = entityFactory.createUser()

        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess
        applicationService.getClientRoleById("roleId") >> cRole

        when:
        service.deleteRole(authToken, "roleId")

        then:
        1 * userService.getUserByAuthToken(authToken) >> user
        1 * precedenceValidator.verifyCallerRolePrecedence(user, cRole)
    }

    def "deleteRole deletes cRole successfully"() {
        given:
        def scopeAccess = createUserScopeAccess()
        def cRole = entityFactory.createClientRole("someRole")
        def user = entityFactory.createUser()

        scopeAccessService.getScopeAccessByAccessToken(authToken) >> scopeAccess
        applicationService.getClientRoleById("roleId") >> cRole
        userService.getUserByAuthToken(authToken) >> user

        when:
        service.deleteRole(authToken, "roleId")

        then:
        1 * applicationService.deleteClientRole(cRole)
    }

    def createRole(String name, String applicationId) {
        new Role().with {
            it.name = name
            it.applicationId = applicationId
            return it
        }
    }
}

