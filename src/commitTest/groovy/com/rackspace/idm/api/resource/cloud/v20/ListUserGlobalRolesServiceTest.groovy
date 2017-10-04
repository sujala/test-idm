package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.ForbiddenException
import org.opensaml.core.config.InitializationService
import org.openstack.docs.identity.api.v2.RoleList
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.xml.bind.JAXBElement

class ListUserGlobalRolesServiceTest extends RootServiceTest {
    @Shared DefaultCloud20Service service

    def setupSpec() {
        InitializationService.initialize()
    }

    def setup() {
        //service being tested
        service = new DefaultCloud20Service()
        mockIdentityConfig(service)
        mockRequestContextHolder(service)
        mockExceptionHandler(service)
        mockUserService(service)
        mockAuthorizationService(service)
        mockIdentityUserService(service)
        mockTenantService(service)
        mockJAXBObjectFactories(service)
        mockRoleConverter(service)
    }

    /**
     * Verifies service calls standard authorization services and the required exception
     * handler to deal with errors.
     *
     * TODO: Expand this to include additional authorization aspects of this service such as verifying user-admins
     * are required to belong to same domain as target user unless have specific elevated role.
     */
    def "listUserGlobalRoles: Calls appropriate authorization services and exception handler"() {
        given:
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def userId = "userId"
        def applyRcnRoles = true

        when:
        service.listUserGlobalRoles(mockHttpHeaders, token, userId, applyRcnRoles)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token) >> new UserScopeAccess()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER) >> {throw new ForbiddenException()}
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    /**
     * Verifies service calls appropriate backend service to retrieve effective roles including group membership. The
     * test gives the caller the requisite elevated role in order to bypass additional authorization checks
     */
    def "listUserGlobalRoles: Verifies user exists, retrieves effective roles, and coverts results when not using applyRcnRoles "() {
        given:
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def targetUserId = "userId"
        def applyRcnRoles = false

        User targetUser = new User().with {
            it.id = targetUserId
            it.username = "target"
            it
        }

        UserScopeAccess callerToken = new UserScopeAccess().with {
            it.userRsId = "otherUser"
            it
        }

        User caller = new User().with {
            it.id = callerToken.userRsId
            it.username = "caller"
            it
        }

        List<TenantRole> roles = []
        RoleList roleList = new RoleList();

        when:
        service.listUserGlobalRoles(mockHttpHeaders, token, targetUserId, applyRcnRoles)

        then:
        securityContext.getAndVerifyEffectiveCallerToken(token) >> callerToken
        1 * identityUserService.checkAndGetEndUserById(targetUserId) >> targetUser // Verifies target user is a end user that exists
        1 * requestContext.getEffectiveCaller() >> caller
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName()) >> true
        1 * tenantService.getEffectiveGlobalRolesForUser(targetUser) >> roles
        1 * roleConverter.toRoleListJaxb(roles) >> roleList
        1 * openStackIdentityV2Factory.createRoles(roleList) >> Mock(JAXBElement)
    }

    /**
     * Verifies service calls appropriate backend service to retrieve effective roles including group membership. The
     * test gives the caller the requisite elevated role in order to bypass additional authorization checks
     */
    def "listUserGlobalRoles: Verifies user exists, retrieves effective roles, and coverts results when use applyRcnRoles "() {
        given:
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def targetUserId = "userId"
        def applyRcnRoles = true

        User targetUser = new User().with {
            it.id = targetUserId
            it.username = "target"
            it
        }

        UserScopeAccess callerToken = new UserScopeAccess().with {
            it.userRsId = "otherUser"
            it
        }

        User caller = new User().with {
            it.id = callerToken.userRsId
            it.username = "caller"
            it
        }

        List<TenantRole> roles = []
        RoleList roleList = new RoleList();

        when:
        service.listUserGlobalRoles(mockHttpHeaders, token, targetUserId, applyRcnRoles)

        then:
        securityContext.getAndVerifyEffectiveCallerToken(token) >> callerToken
        1 * identityUserService.checkAndGetEndUserById(targetUserId) >> targetUser // Verifies target user is a provisioned user that exists
        1 * requestContext.getEffectiveCaller() >> caller // Verify the caller is checked to be a provisioned  user
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName()) >> true
        1 * tenantService.getEffectiveGlobalRolesForUserApplyRcnRoles(targetUser) >> roles
        1 * roleConverter.toRoleListJaxb(roles) >> roleList
        1 * openStackIdentityV2Factory.createRoles(roleList) >> Mock(JAXBElement)
    }

    /**
     * Verifies service calls standard authorization services and the required exception
     * handler to deal with errors
     */
    def "listUserGlobalRolesByServiceId: Calls appropriate authorization services and exception handler"() {
        given:
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def userId = "userId"
        def applyRcnRoles = true
        def serviceId = "serviceId"

        when:
        service.listUserGlobalRolesByServiceId(mockHttpHeaders, token, userId, serviceId, applyRcnRoles)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token) >> new UserScopeAccess()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN) >> {throw new ForbiddenException()}
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    /**
     * Verifies service calls appropriate backend service to retrieve effective roles including group membership.
     */
    def "listUserGlobalRolesByServiceId: Verifies user exists, retrieves effective roles, and coverts results when not using applyRcnRoles "() {
        given:
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def targetUserId = "userId"
        def applyRcnRoles = false
        def serviceId = "serviceId"

        User targetUser = new User().with {
            it.id = targetUserId
            it.username = "target"
            it
        }

        UserScopeAccess callerToken = new UserScopeAccess().with {
            it.userRsId = "otherUser"
            it
        }

        List<TenantRole> roles = []
        RoleList roleList = new RoleList()

        when:
        service.listUserGlobalRolesByServiceId(mockHttpHeaders, token, targetUserId, serviceId, applyRcnRoles)

        then:
        securityContext.getAndVerifyEffectiveCallerToken(token) >> callerToken
        1 * identityUserService.checkAndGetEndUserById(targetUserId) >> targetUser // Verifies target user is a provisioned user that exists
        1 * tenantService.getEffectiveGlobalRolesForUser(targetUser, serviceId) >> roles
        1 * roleConverter.toRoleListJaxb(roles) >> roleList
        1 * openStackIdentityV2Factory.createRoles(roleList) >> Mock(JAXBElement)
    }

    /**
     * Verifies service calls appropriate backend service to retrieve effective roles including group membership.
     */
    def "listUserGlobalRolesByServiceId: Verifies user exists, retrieves effective roles, and coverts results when use applyRcnRoles "() {
        given:
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def targetUserId = "userId"
        def applyRcnRoles = true
        def serviceId = "serviceId"

        User targetUser = new User().with {
            it.id = targetUserId
            it.username = "target"
            it
        }

        UserScopeAccess callerToken = new UserScopeAccess().with {
            it.userRsId = "otherUser"
            it
        }

        List<TenantRole> roles = []
        RoleList roleList = new RoleList();

        when:
        service.listUserGlobalRolesByServiceId(mockHttpHeaders, token, targetUserId, serviceId, applyRcnRoles)

        then:
        securityContext.getAndVerifyEffectiveCallerToken(token) >> callerToken
        1 * identityUserService.checkAndGetEndUserById(targetUserId) >> targetUser // Verifies target user is a provisioned user that exists
        1 * tenantService.getEffectiveGlobalRolesForUserApplyRcnRoles(targetUser, serviceId) >> roles
        1 * roleConverter.toRoleListJaxb(roles) >> roleList
        1 * openStackIdentityV2Factory.createRoles(roleList) >> Mock(JAXBElement)
    }
}
