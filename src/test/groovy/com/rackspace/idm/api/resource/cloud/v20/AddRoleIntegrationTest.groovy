package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.rackspace.idm.validation.Validator20
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

/**
 * Tests adding various types of roles
 *
 * {@see TenantPropagatingRoleIntegrationTest}
 * {@see GlobalPropagatingRoleIntegrationTest}
 */
class AddRoleIntegrationTest extends RootIntegrationTest {
    public static final String IDENTITY_ADMIN_USERNAME_PREFIX = "identityAdmin"
    public static final String USER_ADMIN_USERNAME_PREFIX = "userAdmin"
    public static final String DEFAULT_USER_USERNAME_PREFIX = "defaultUser"
    public static final String ROLE_NAME_PREFIX = "role"

    /**
     * Random string generated for entire test class. Same for all feature methods.
     */
    @Shared String SPECIFICATION_RANDOM

    @Shared
    def specificationServiceAdmin
    @Shared
    def specificationServiceAdminToken

    @Shared
    def specificationIdentityAdmin

    @Shared
    def specificationIdentityAdminToken

    @Autowired
    DefaultAuthorizationService defaultAuthorizationService

    @Autowired
    ApplicationService applicationService

    @Autowired
    Configuration config;

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Autowired
    TenantService tenantService

    @Autowired
    UserService userService

    /**
     * Random string that is unique for each feature method
     */
    @Shared String FEATURE_RANDOM

    /**
     * Like most other tests, this test class depends on a pre-existing service admin (authQE)
     *
     * @return
     */
    def setupSpec() {
        SPECIFICATION_RANDOM = getNormalizedRandomString()

        //login via the already existing service admin user
        def serviceAdminAuthResponse = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD).getEntity(AuthenticateResponse)
        //verify the authentication worked before retrieving the token
        assert serviceAdminAuthResponse.value instanceof AuthenticateResponse
        specificationServiceAdminToken = serviceAdminAuthResponse.value.token.id
        specificationServiceAdmin = cloud20.getUserByName(specificationServiceAdminToken, Constants.SERVICE_ADMIN_USERNAME).getEntity(User).value

        //create a new shared identity admin for these tests
        specificationIdentityAdmin = createIdentityAdmin(IDENTITY_ADMIN_USERNAME_PREFIX + SPECIFICATION_RANDOM)
        def identityAdminAuthResponse = cloud20.authenticatePassword(specificationIdentityAdmin.getUsername(), Constants.DEFAULT_PASSWORD).getEntity(AuthenticateResponse)
        //verify the authentication worked before retrieving the token
        assert identityAdminAuthResponse.value instanceof AuthenticateResponse
        specificationIdentityAdminToken = identityAdminAuthResponse.value.token.id
    }

    def setup() {
        FEATURE_RANDOM = getNormalizedRandomString()
    }

    def cleanupSpec() {
        deleteUserQuietly(specificationIdentityAdmin)
    }

    @Unroll
    def "Create Role: Role with weight #expectedRoleWeight created when administratorRole set to #administratorRole and called by #callerUserType using #requestType"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        Role newRole = v2Factory.createRole(roleName).with {
            if (administratorRole != null) {
                it.administratorRole = administratorRole
            }
            it
        }

        when:
        def response = cloud20.createRole(callerToken, newRole, requestType, responseType)
        def createdRole = responseType == MediaType.APPLICATION_XML_TYPE ? response.getEntity(Role).value : response.getEntity(Role)
        def retrievedRole = applicationRoleDao.getClientRole(createdRole.id)

        then:
        response.status == 201
        retrievedRole != null
        retrievedRole.rsWeight == expectedRoleWeight

        cleanup:
        if (createdRole != null) {
            utils.deleteRoleQuietly(createdRole)
        }

        where:
        callerUserType | administratorRole | expectedRoleWeight | callerToken | requestType | responseType
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | null | 50 | specificationServiceAdminToken | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | IdentityUserTypeEnum.SERVICE_ADMIN.roleName | 50 | specificationServiceAdminToken | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | 500 | specificationServiceAdminToken | MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | IdentityUserTypeEnum.USER_MANAGER.roleName | 1000 | specificationServiceAdminToken | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | null | 500 | specificationIdentityAdminToken | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | 500 | specificationIdentityAdminToken | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | IdentityUserTypeEnum.USER_MANAGER.roleName | 1000 | specificationIdentityAdminToken | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Create Role: Verify #callerUserType can not create role with administratorRole #administratorRole"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        Role newRole = v2Factory.createRole(roleName).with {
            if (administratorRole != null) {
                it.administratorRole = administratorRole
            }
            it
        }

        when:
        def response = cloud20.createRole(callerToken, newRole)

        then:
        response.status == expectedStatus

        where:
        callerUserType | administratorRole | expectedStatus | callerToken
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | IdentityUserTypeEnum.USER_ADMIN.roleName | HttpStatus.SC_BAD_REQUEST | specificationServiceAdminToken
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | IdentityUserTypeEnum.DEFAULT_USER.roleName | HttpStatus.SC_BAD_REQUEST | specificationServiceAdminToken
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | "nonExistantRoleName" | HttpStatus.SC_BAD_REQUEST | specificationServiceAdminToken
        IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | IdentityUserTypeEnum.SERVICE_ADMIN.roleName | HttpStatus.SC_FORBIDDEN | specificationIdentityAdminToken
        IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | IdentityUserTypeEnum.USER_ADMIN.roleName | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken
        IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | IdentityUserTypeEnum.DEFAULT_USER.roleName | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken
    }

    def "Create Role: User admin/managers/default users can not create new roles"() {
        given:
        Role newRole = v2Factory.createRole(testUtils.getRandomUUID("role"))

        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        def userManager = createDefaultUser(userAdminToken)
        def userManagerToken = authenticate(userManager.username)
        ClientRole userManageRole = defaultAuthorizationService.getCloudUserManagedRole()
        def status = addRoleToUser(specificationServiceAdminToken, userManager, userManageRole)
        assert status == HttpStatus.SC_OK.intValue()
        assertUserHasRole(userManager, userManageRole) //verify test state

        def defaultUser = createDefaultUser(userAdminToken)
        def defaultUserToken = authenticate(defaultUser.username)

        when: "create role as user-admin"
        def forbiddenStatus = cloud20.createRole(userAdminToken, newRole)

        then: "forbidden"
        assert forbiddenStatus.status == HttpStatus.SC_FORBIDDEN

        when: "create role as user-manager"
        forbiddenStatus = cloud20.createRole(userManagerToken, newRole)

        then: "forbidden"
        assert forbiddenStatus.status == HttpStatus.SC_FORBIDDEN

        when: "create role as default user"
        forbiddenStatus = cloud20.createRole(defaultUserToken, newRole)

        then: "forbidden"
        assert forbiddenStatus.status == HttpStatus.SC_FORBIDDEN

        cleanup:
        deleteUserQuietly(userManager)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    @Unroll
    def "Create Role: Verify naming convention enforcement. Attempt to create role with role name #roleName returns status #expectedStatus"() {
        given:
        Role newRole = v2Factory.createRole(roleName).with {
            if (administratorRole != null) {
                it.administratorRole = administratorRole
            }
            it
        }

        when:
        def response = cloud20.createRole(callerToken, newRole)

        then:
        response.status == expectedStatus

        cleanup:
        if (response.status == HttpStatus.SC_CREATED) {
            def createdRole = response.getEntity(Role).value
            utils.deleteRoleQuietly(createdRole)
        }

        where:
        roleName | expectedStatus | callerToken
        "identity:" + RandomStringUtils.randomAlphanumeric(10) | HttpStatus.SC_CREATED | specificationIdentityAdminToken //valid product role
        RandomStringUtils.randomAlphanumeric(10) | HttpStatus.SC_CREATED | specificationIdentityAdminToken //valid global role
        "identity:" + RandomStringUtils.randomAlphanumeric(10) + ":" | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken //trailing colon
        RandomStringUtils.randomAlphanumeric(10) + "+" | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken //invalid character
        "inva:li:d" + RandomStringUtils.randomAlphanumeric(10) | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken //multiple colons
        ":invalid" + RandomStringUtils.randomAlphanumeric(10) | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken //leading colon
        "invalid:-String" + RandomStringUtils.randomAlphanumeric(10) | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken //name not start w/ alphanum
        "_invalid:String" + RandomStringUtils.randomAlphanumeric(10) | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken //prefix not start w/ alphanum
        RandomStringUtils.randomAlphanumeric(Validator20.MAX_ROLE_NAME + 1) | HttpStatus.SC_BAD_REQUEST | specificationIdentityAdminToken //too big
    }

    def "Service admin can assign identity role to user that does not have an identity role"() {
        //create an identity-admin from which we'll remove the identity-admin role
        def identityAdmin = createIdentityAdmin()

        ClientRole cloudIdentityAdminRole = defaultAuthorizationService.getCloudIdentityAdminRole();

        def identityAdminEntity = userService.getUserById(identityAdmin.id)
        tenantService.deleteTenantRoleForUser(identityAdminEntity, tenantService.getTenantRoleForUserById(identityAdminEntity, Constants.IDENTITY_ADMIN_ROLE_ID))
        assertUserDoesNotHaveRole(identityAdmin, cloudIdentityAdminRole)

        when: "Add role to user without any identity role"
        def status = addRoleToUser(specificationServiceAdminToken, identityAdmin, cloudIdentityAdminRole)
        assert status == HttpStatus.SC_OK

        then: "user now has role"
        assertUserHasRole(identityAdmin, cloudIdentityAdminRole)

        cleanup:
        deleteUserQuietly(identityAdmin)
    }

    def "User admin can assign 1000 weight role to default user within domain"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        Role role = createPropagateRole(false, 1000)

        when: "As user-admin, add 1000 weight role to default user within my domain"
        def status = addRoleToUser(userAdminToken, defaultUser, role)
        assert status == HttpStatus.SC_OK

        then: "default user now has user-manage role"
        assertUserHasRole(defaultUser, role)

        cleanup:
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(role)
    }

    def "User manager cannot assign user-manager role to default user within domain"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        def userManager = createDefaultUser(userAdminToken)
        def userManagerToken = authenticate(userManager.username)

        def defaultUser = createDefaultUser(userAdminToken)

        ClientRole userManageRole = defaultAuthorizationService.getCloudUserManagedRole()
        def status = addRoleToUser(specificationServiceAdminToken, userManager, userManageRole)
        assert status == HttpStatus.SC_OK
        assertUserHasRole(userManager, userManageRole) //verify test state

        when: "As user-manager, add user-manager role to default user within my domain"
        def forbiddenStatus = addRoleToUser(userManagerToken, defaultUser, userManageRole)

        then:
        assert forbiddenStatus == HttpStatus.SC_FORBIDDEN
        assertUserDoesNotHaveRole(defaultUser, userManageRole)

        cleanup:
        deleteUserQuietly(userManager)
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(defaultUser)
    }

    def "Cannot create two Client roles with the same name under the same application"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.name = roleName
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it
        }

        when:
        def response = cloud20.createRole(utils.getServiceAdminToken(), createRole)
        def createdRole = response.getEntity(Role).value

        then:
        response.status == 201

        when:
        response = cloud20.createRole(utils.getServiceAdminToken(), createRole)

        then:
        response.status == 400

        cleanup:
        utils.deleteRole(createdRole)
    }

    def "Cannot create two Client roles with the same name under two different applications"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        def service = utils.createService()
        Role createRole1 = v2Factory.createRole().with {
            it.name = roleName
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it
        }
        Role createRole2 = v2Factory.createRole().with {
            it.name = roleName
            it.serviceId = service.id
            it
        }

        when:
        def response = cloud20.createRole(utils.getServiceAdminToken(), createRole1)
        def createdRole = response.getEntity(Role).value

        then:
        response.status == 201

        when:
        response = cloud20.createRole(utils.getServiceAdminToken(), createRole2)

        then:
        response.status == 400

        cleanup:
        utils.deleteService(service)
        utils.deleteRole(createdRole)
    }

    /**
     * The determination of whether a new role is a propagating role is the roleType. The propagating attribute is
     * ignored. However, for backwards compatibility the propagating attribute is still returned in responses. It is
     * set based on the roleType.
     * @return
     */
    @Unroll
    def "Creating a propagating role ignores provided propagating attribute and uses roleType: roleType: #roleType; propagate: #propagate"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.name = roleName
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it.propagate = propagate
            it.roleType = roleType
            it
        }

        when:
        def response = cloud20.createRole(utils.getServiceAdminToken(), createRole)
        Role createdRole = response.getEntity(Role).value

        then:
        response.status == 201
        createdRole.propagate
        createdRole.roleType == roleType

        and: "Backend stores roleType as STANDARD, propagating attribute as true"
        ClientRole ldapRole = applicationRoleDao.getClientRole(createdRole.id)
        ldapRole.getRawRoleType() == RoleTypeEnum.STANDARD.name()
        ldapRole.propagate

        and: "Get role by Id also correctly returns prop attributes"
        Role getRole = utils.getRole(createdRole.id)
        getRole.roleType == roleType
        getRole.propagate

        cleanup:
        utils.deleteRoleQuietly(createdRole)

        where:
        propagate | roleType
        true      | RoleTypeEnum.PROPAGATE
        false     | RoleTypeEnum.PROPAGATE
        null      | RoleTypeEnum.PROPAGATE
    }

    /**
     * The determination of whether a new role is a propagating role is the roleType. The propagating attribute provided is
     * ignored. However, for backwards compatibility the propagating attribute is still returned in responses. It is
     * set based on the roleType.
     * @return
     */
    @Unroll
    def "Creating a non-propagating role appropriately sets propagating attribute to false: roleType: #roleType; propagate: #propagate"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        Role createRole = v2Factory.createRole(roleName).with {
            it.propagate = propagate
            it.roleType = roleType
            if (roleType == RoleTypeEnum.RCN) {
                it.types = new Types().with {
                    it.type = ["cloud"]
                    it
                }
            }
            it
        }

        when:
        def response = cloud20.createRole(utils.getServiceAdminToken(), createRole)
        Role createdRole = response.getEntity(Role).value
        def expectedRoleType = roleType == null ? RoleTypeEnum.STANDARD : roleType

        then:
        response.status == 201
        !createdRole.propagate
        createdRole.roleType == expectedRoleType

        cleanup:
        utils.deleteRoleQuietly(createdRole)

        where:
        propagate | roleType
        true      | null
        false     | null
        true      | RoleTypeEnum.STANDARD
        false     | RoleTypeEnum.STANDARD
        null      | RoleTypeEnum.STANDARD
        true      | RoleTypeEnum.RCN
        false     | RoleTypeEnum.RCN
        null      | RoleTypeEnum.RCN
    }

    @Unroll
    def "Can create a propagating role when administratorRole set to identity:admin or identity:service-admin: administratorRole: #administratorRole"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        Role createRole = v2Factory.createRole(roleName).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it.administratorRole = administratorRole
            it
        }

        when:
        def response = cloud20.createRole(utils.getServiceAdminToken(), createRole)
        Role createdRole = response.getEntity(Role).value
        def expectedAdminRole = administratorRole != null ? administratorRole : "identity:service-admin"

        then:
        response.status == 201
        createdRole.administratorRole == expectedAdminRole

        cleanup:
        utils.deleteRoleQuietly(createdRole)

        where:
        administratorRole | _
        "identity:admin" | _
        "identity:service-admin" | _
        null | _
    }

    @Unroll
    def "Can not create a propagating role when administratorRole set something other than identity:admin or identity:service-admin: administratorRole: #administratorRole"() {
        given:
        def roleName = testUtils.getRandomUUID("role")
        Role createRole = v2Factory.createRole(roleName).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it.administratorRole = administratorRole
            it
        }

        when:
        def response = cloud20.createRole(utils.getServiceAdminToken(), createRole)

        then:
        response.status == 400

        where:
        administratorRole | _
        "identity:user-manage" | _
        "timbuktoo" | _
    }

    @Unroll
    def "Any tenant types specified in 'Create Role' service must match an existing tenant type: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Types types = new Types().with {
            it.type = ["cloud", "doesnotexist"]
            it
        }
        def role = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        when: "If any provided tenant type does not match"
        def response = cloud20.createRole(utils.getServiceAdminToken(), role, acceptContentType, requestContentType)

        then: "a 400 must be returned"
        String errMsg = String.format(Validator20.ERROR_TENANT_TYPE_WAS_NOT_FOUND, "doesnotexist");
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, errMsg)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def deleteUserQuietly(user) {
        if (user != null) {
            try {
                cloud20.destroyUser(specificationServiceAdminToken, user.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def deleteRoleQuietly(role) {
        if (role != null) {
            try {
                cloud20.deleteRole(specificationServiceAdminToken, role.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def createPropagateRole(boolean propagate = true, int weight = STANDARD_PROPAGATING_ROLE_WEIGHT, String roleName = ROLE_NAME_PREFIX + getNormalizedRandomString()) {
        def role = entityFactory.createClientRole().with {
            it.id = getNormalizedRandomString()
            it.name = roleName
            it.propagate = propagate
            it.rsWeight = weight
            it.clientId = Constants.IDENTITY_SERVICE_ID
            it
        }

        applicationService.addClientRole(role)

        return v2Factory.createRole().with {
            it.id = role.id
            it.name = role.name
            it.propagate = role.propagate
            it
        }
    }

    def void assertUserHasRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.SC_OK
    }

    def void assertUserDoesNotHaveRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.SC_NOT_FOUND
    }

    def addRoleToUser(callerToken, userToAddRoleTo, roleToAdd) {
        return cloud20.addApplicationRoleToUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status
    }

    def createIdentityAdmin(String identityAdminUsername = IDENTITY_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(specificationServiceAdminToken, v2Factory.createUserForCreate(identityAdminUsername, "display", "test@rackspace.com", true, null, null, Constants.DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(specificationServiceAdminToken, identityAdminUsername).getEntity(User).value
        return userAdmin;
    }

    def createUserAdmin(String callerToken = specificationIdentityAdminToken, String adminUsername = USER_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(adminUsername, "display", "test@rackspace.com", true, null, domainId, Constants.DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(callerToken, adminUsername).getEntity(User).value
        return userAdmin;
    }

    def createDefaultUser(String callerToken, String userName = DEFAULT_USER_USERNAME_PREFIX + getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(userName, "display", "test@rackspace.com", true, null, null, Constants.DEFAULT_PASSWORD))
        def user = cloud20.getUserByName(callerToken, userName).getEntity(User).value
        return user
    }

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, Constants.DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }
}
