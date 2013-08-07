package com.rackspace.idm.api.resource.user

import spock.lang.Shared
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import testHelpers.RootServiceTest

class UserGlobalRoleResourceTest extends RootServiceTest {


    @Shared UserGlobalRoleResource service

    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def AUTH_TOKEN = "authToken"
    @Shared def USER_ID = "userId"
    @Shared def ROLE_ID = "roleId"
    @Shared def TENANT_ID = "tenantId"

    def setupSpec() {
        service = new UserGlobalRoleResource()
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        mockScopeAccessService(service)
        mockUserService(service)
        mockApplicationService(service)
        mockAuthorizationService(service)
        mockTenantService(service)
        mockPrecedenceValidator(service)
        mockConfiguration(service)
    }

    def "grantTenantRoleToUser verifies tenantRole"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def tenant= entityFactory.createTenant()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenant(TENANT_ID) >> tenant

        when:
        service.grantTenantRoleToUser(AUTH_TOKEN, USER_ID, TENANT_ID, ROLE_ID)

        then:
        1 * applicationService.getClientRoleById(ROLE_ID) >> null
        then:
        thrown(BadRequestException)
    }

    def "grantTenantRoleToUser verifies caller precedence"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def tenant= entityFactory.createTenant()
        def cRole = entityFactory.createClientRole()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenant(TENANT_ID) >> tenant
        applicationService.getClientRoleById(ROLE_ID) >> cRole

        when:
        service.grantTenantRoleToUser(AUTH_TOKEN, USER_ID, TENANT_ID, ROLE_ID)

        then:
        1 * applicationService.getIdentityRoleNames() >> [].asList()
        1 * userService.getUserByAuthToken(AUTH_TOKEN)
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_, _)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(_, _)
    }

    def "grantTenantRoleToUser adds tenantRole to user"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def tenant= entityFactory.createTenant()
        def cRole = entityFactory.createClientRole()

        userService.loadUser(USER_ID) >> user
        tenantService.getTenant(TENANT_ID) >> tenant
        applicationService.getClientRoleById(ROLE_ID) >> cRole
        userService.getUserByAuthToken(AUTH_TOKEN)

        when:
        service.grantTenantRoleToUser(AUTH_TOKEN, USER_ID, TENANT_ID, ROLE_ID)

        then:
        1 * applicationService.getIdentityRoleNames() >> [].asList()
        1 * tenantService.addTenantRoleToUser(user, _)
    }

    def "checkAndGetTenantRole verifies that user exists"() {
        given:
        allowUserAccess()

        when:
        service.checkAndGetTenantRole(TENANT_ID, ROLE_ID)

        then:
        1 * applicationService.getClientRoleById(ROLE_ID) >> null
        then:
        thrown(BadRequestException)
    }

    def "checkAndGetTenantRole sets tenantRole information from clientRole"() {
        given:
        allowUserAccess()

        def cRole = entityFactory.createClientRole("roleName").with {
            it.clientId = "clientId"
            it.id = "id"
            return it
        }
        applicationService.getClientRoleById(ROLE_ID) >> cRole

        when:
        def tRole = service.checkAndGetTenantRole(TENANT_ID, ROLE_ID)

        then:
        tRole.getClientId().equals(cRole.getClientId())
        tRole.getRoleRsId().equals(cRole.getId())
        tRole.getName().equals(cRole.getName())
        tRole.getTenantIds().contains(TENANT_ID)
    }

    def "getIdentityRoleNames gets a list of known identityRole names"() {
        when:
        service.getIdentityRoleNames()

        then:
        1 * config.getString("cloudAuth.userRole") >> ""
        1 * config.getString("cloudAuth.userAdminRole") >> ""
        1 * config.getString("cloudAuth.adminRole") >> ""
        1 * config.getString("cloudAuth.serviceAdminRole") >> ""
    }
}
