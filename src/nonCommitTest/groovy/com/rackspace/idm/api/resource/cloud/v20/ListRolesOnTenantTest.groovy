package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootIntegrationTest


class ListRolesOnTenantTest extends RootIntegrationTest {
    private static final String SERVICE_ADMIN_USERNAME = "authQE";
    private static final String SERVICE_ADMIN_PWD = "Auth1234"

    public static final String IDENTITY_ADMIN_USERNAME_PREFIX = "identityAdmin"
    public static final String USER_ADMIN_USERNAME_PREFIX = "userAdmin"
    public static final String DEFAULT_USER_USERNAME_PREFIX = "defaultUser"
    public static final String ROLE_NAME_PREFIX = "role"

    public static final String DEFAULT_PASSWORD = "Password1"
    public static final String APPLICATION_ID = "bde1268ebabeeabb70a0e702a4626977c331d5c4"

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
        def serviceAdminAuthResponse = cloud20.authenticatePassword(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PWD).getEntity(AuthenticateResponse)
        //verify the authentication worked before retrieving the token
        assert serviceAdminAuthResponse.value instanceof AuthenticateResponse
        specificationServiceAdminToken = serviceAdminAuthResponse.value.token.id
        specificationServiceAdmin = cloud20.getUserByName(specificationServiceAdminToken, SERVICE_ADMIN_USERNAME).getEntity(User).value

        //create a new shared identity admin for these tests
        specificationIdentityAdmin = createIdentityAdmin(IDENTITY_ADMIN_USERNAME_PREFIX + SPECIFICATION_RANDOM)
        cloud20.addApplicationRoleToUser(specificationServiceAdminToken, Constants.IDENTITY_RS_DOMAIN_ADMIN_ROLE_ID, specificationIdentityAdmin.id)
        def identityAdminAuthResponse = cloud20.authenticatePassword(specificationIdentityAdmin.getUsername(), DEFAULT_PASSWORD).getEntity(AuthenticateResponse)
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

    def "Service Admin CAN list user roles on tenants"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)
        def tenant = v2Factory.createTenant("tenant" + getNormalizedRandomString(), "tenantName" + getNormalizedRandomString())
        def addTenantResponse = addTenant(specificationServiceAdminToken, tenant)
        def addedTenant = addTenantResponse.getEntity(Tenant.class).value
        def role = createRole()
        cloud20.addRoleToUserOnTenant(specificationIdentityAdminToken, addedTenant.id, defaultUser.id, role.id)

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationServiceAdminToken, addedTenant.id, defaultUser.id)

        then:
        listUserRoleOnTenant.status == HttpStatus.OK.value()

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(defaultUser)
        deleteTenantQuietly(tenant)
        deleteRoleQuietly(role)

    }

    def "Service Admin and Identity Admin CAN list roles on tenants for self"() {

        def tenant = v2Factory.createTenant("tenant" + getNormalizedRandomString(), "tenantName" + getNormalizedRandomString())
        def addTenantResponse = addTenant(specificationServiceAdminToken, tenant)
        def addedTenant = addTenantResponse.getEntity(Tenant.class).value

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationServiceAdminToken, addedTenant.id, specificationServiceAdmin.id)
        def listUserRoleOnTenant2 = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, addedTenant.id, specificationIdentityAdmin.id)

        then:
        listUserRoleOnTenant.status == HttpStatus.OK.value()
        listUserRoleOnTenant2.status == HttpStatus.OK.value()

        cleanup:
        deleteTenantQuietly(tenant)
    }

    def "Identity Admin CAN list user roles on tenants (except for Service Admins)"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)
        def tenant = v2Factory.createTenant("tenant" + getNormalizedRandomString(), "tenantName" + getNormalizedRandomString())
        def addTenantResponse = addTenant(specificationServiceAdminToken, tenant)
        def addedTenant = addTenantResponse.getEntity(Tenant.class).value
        def role = createRole()
        cloud20.addRoleToUserOnTenant(specificationIdentityAdminToken, addedTenant.id, defaultUser.id, role.id)

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, addedTenant.id, defaultUser.id)
        def listServiceAdminRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, addedTenant.id, specificationServiceAdmin.id)

        then:
        listServiceAdminRoleOnTenant.status == HttpStatus.FORBIDDEN.value()
        listUserRoleOnTenant.status == HttpStatus.OK.value()

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(defaultUser)
        deleteTenantQuietly(tenant)
        deleteRoleQuietly(role)
    }

    def "User Admin CANNOT list user roles on tenants"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        when:
        def forbiddenResponse = cloud20.listRolesForUserOnTenant(userAdminToken, "faketenant", userAdmin.id)

        then:
        forbiddenResponse.status == HttpStatus.FORBIDDEN.value()

        cleanup:
        deleteUserQuietly(userAdmin)
    }

    def "User Manage CANNOT list user roles on tenants"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        def userManager = createDefaultUser(userAdminToken)
        def userManagerToken = authenticate(userManager.username)

        ClientRole userManageRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityUserManageRole());
        def status = addRoleToUser(specificationServiceAdminToken, userManager, userManageRole)
        assert status == HttpStatus.OK.value()
        assertUserHasRole(userManager, userManageRole) //verify test state

        when:
        def forbiddenResponse = cloud20.listRolesForUserOnTenant(userManagerToken, "faketenant", userManager.id)

        then:
        forbiddenResponse.status == HttpStatus.FORBIDDEN.value()

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(userManager)
    }

    def "Default User CANNOT list user roles on tenants"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)
        def defaultUserToken = authenticate(defaultUser.username)

        when:
        def forbiddenResponse = cloud20.listRolesForUserOnTenant(defaultUserToken, "faketenant", defaultUser.id)

        then:
        forbiddenResponse.status == HttpStatus.FORBIDDEN.value()

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(defaultUser)
    }

    def "List Roles on Tenant returns auto-assigned roles for a user"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def tenant = v2Factory.createTenant("tenant" + getNormalizedRandomString(), "tenantName" + getNormalizedRandomString()).with {
            it.domainId = userAdmin.domainId
            it
        }
        def addedTenant = utils.createTenant(tenant)

        when: "implicit tenant role assignment"
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, addedTenant.id, userAdmin.id).getEntity(RoleList).value

        then:
        listUserRoleOnTenant != null
        listUserRoleOnTenant.role.size() == 1
        listUserRoleOnTenant.role[0].id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "If the tenant lives within the user's domain, the user will receive each non-RCN globally assigned role on that tenant"() {
        given:
        def userAdmin = createUserAdmin()
        def domainId = userAdmin.domainId
        def tenant = v2Factory.createTenant().with {
            it.name = testUtils.getRandomUUID("tenant")
            it.domainId = domainId
            it
        }
        def createdTenant = utils.createTenant(tenant)

        def roleName = getRandomUUID("role")
        def role = v2Factory.createRole().with {
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it.name = roleName
            it.roleType = RoleTypeEnum.STANDARD
            it.assignment = RoleAssignmentEnum.GLOBAL
            it
        }

        def createdRole = utils.createRole(role)
        utils.addRoleToUser(userAdmin, createdRole.id)

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, createdTenant.id, userAdmin.id, true).getEntity(RoleList).value

        then:
        listUserRoleOnTenant != null
        def globalNonRcnRole = listUserRoleOnTenant.role.find { it.id == createdRole.id }
        globalNonRcnRole.tenantId == createdTenant.id

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(createdRole)
        deleteTenantQuietly(createdTenant)
    }

    def "Each RCN role the user is assigned that applies to the tenant."() {
        given:
        def tenantType = "cloud"
        Types types = new Types().with {
            it.type = [tenantType]
            it
        }
        def userAdmin = createUserAdmin()
        def domainId = userAdmin.domainId
        def tenant = v2Factory.createTenant().with {
            it.name = testUtils.getRandomUUID("tenant")
            it.domainId = domainId
            it.types = types
            it
        }
        def createdTenant = utils.createTenant(tenant)

        def roleName = getRandomUUID("role")
        def role = v2Factory.createRole().with {
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it.name = roleName
            it.roleType = RoleTypeEnum.RCN
            it.assignment = RoleAssignmentEnum.GLOBAL
            it.types = types
            it
        }

        def createdRole = utils.createRole(role)
        utils.addRoleToUser(userAdmin, createdRole.id)

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, createdTenant.id, userAdmin.id, true).getEntity(RoleList).value

        then:
        listUserRoleOnTenant != null
        def globalNonRcnRole = listUserRoleOnTenant.role.find { it.id == createdRole.id }
        globalNonRcnRole.tenantId == createdTenant.id

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(createdRole)
        deleteTenantQuietly(createdTenant)
    }

    def "The RCN role is not returned for a tenant within the user's domain if the tenant doesn't 'match' the RCN role."() {
        given:
        def tenantType = "cloud"
        Types types = new Types().with {
            it.type = [tenantType]
            it
        }
        def userAdmin = createUserAdmin()
        def domainId = userAdmin.domainId
        def tenant = v2Factory.createTenant().with {
            it.name = testUtils.getRandomUUID("tenant")
            it.domainId = domainId
            it
        }
        def createdTenant = utils.createTenant(tenant)

        def roleName = getRandomUUID("role")
        def role = v2Factory.createRole().with {
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it.name = roleName
            it.roleType = RoleTypeEnum.RCN
            it.assignment = RoleAssignmentEnum.GLOBAL
            it.types = types
            it
        }

        def createdRole = utils.createRole(role)
        utils.addRoleToUser(userAdmin, createdRole.id)

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, createdTenant.id, userAdmin.id, true).getEntity(RoleList).value

        then:
        listUserRoleOnTenant != null
        def globalNonRcnRole = listUserRoleOnTenant.role.find { it.id == createdRole.id }
        globalNonRcnRole == null

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(createdRole)
        deleteTenantQuietly(createdTenant)
    }

    def "The RCN role would be returned for a matching tenant outside the user's domain, as long as that external tenant is within the same RCN"() {
        given:
        def tenantType = "cloud"
        Types types = new Types().with {
            it.type = [tenantType]
            it
        }
        def userAdmin = createUserAdmin()
        def otherUserAdmin = createUserAdmin()

        def rcn = testUtils.getRandomUUID()
        utils.domainRcnSwitch(userAdmin.domainId, rcn)
        utils.domainRcnSwitch(otherUserAdmin.domainId, rcn)

        def tenant = v2Factory.createTenant().with {
            it.name = testUtils.getRandomUUID("tenant")
            it.domainId = otherUserAdmin.domainId
            it.types = types
            it
        }
        def createdTenant = utils.createTenant(tenant)

        def roleName = getRandomUUID("role")
        def role = v2Factory.createRole().with {
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it.name = roleName
            it.roleType = RoleTypeEnum.RCN
            it.assignment = RoleAssignmentEnum.GLOBAL
            it.types = types
            it
        }

        def createdRole = utils.createRole(role)
        utils.addRoleToUser(userAdmin, createdRole.id)

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, createdTenant.id, userAdmin.id, true).getEntity(RoleList).value

        then:
        listUserRoleOnTenant != null
        def globalNonRcnRole = listUserRoleOnTenant.role.find { it.id == createdRole.id }
        globalNonRcnRole.tenantId == createdTenant.id

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(otherUserAdmin)
        deleteRoleQuietly(createdRole)
        deleteTenantQuietly(createdTenant)
    }

    def "A globally assigned non-RCN role is not returned for a tenant outside the user's domain, but within the same RCN"() {
        given:
        def tenantType = "cloud"
        Types types = new Types().with {
            it.type = [tenantType]
            it
        }
        def userAdmin = createUserAdmin()
        def otherUserAdmin = createUserAdmin()

        def rcn = testUtils.getRandomUUID()
        utils.updateDomain(userAdmin.domainId, v2Factory.createDomain().with {it.rackspaceCustomerNumber = rcn; it})
        utils.updateDomain(otherUserAdmin.domainId, v2Factory.createDomain().with {it.rackspaceCustomerNumber = rcn; it})

        def tenant = v2Factory.createTenant().with {
            it.name = testUtils.getRandomUUID("tenant")
            it.domainId = otherUserAdmin.domainId
            it.types = types
            it
        }
        def createdTenant = utils.createTenant(tenant)

        def roleName = getRandomUUID("role")
        def role = v2Factory.createRole().with {
            it.serviceId = Constants.IDENTITY_SERVICE_ID
            it.name = roleName
            it.roleType = RoleTypeEnum.STANDARD
            it.assignment = RoleAssignmentEnum.GLOBAL
            it
        }

        def createdRole = utils.createRole(role)
        utils.addRoleToUser(userAdmin, createdRole.id)

        when:
        def listUserRoleOnTenant = cloud20.listRolesForUserOnTenant(specificationIdentityAdminToken, createdTenant.id, userAdmin.id, true).getEntity(RoleList).value

        then:
        listUserRoleOnTenant != null
        def globalNonRcnRole = listUserRoleOnTenant.role.find { it.id == createdRole.id }
        globalNonRcnRole == null

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(otherUserAdmin)
        deleteRoleQuietly(createdRole)
        deleteTenantQuietly(createdTenant)
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

    def deleteTenantQuietly(tenant) {
        if (tenant != null) {
            try {
                cloud20.deleteTenant(specificationServiceAdminToken, tenant.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def createRole(String roleName = ROLE_NAME_PREFIX + getNormalizedRandomString()) {
        def role = entityFactory.createClientRole().with {
            it.id = getNormalizedRandomString()
            it.name = roleName
            it.rsWeight = 1000
            it.clientId = APPLICATION_ID
            it
        }

        applicationService.addClientRole(role)

        return v2Factory.createRole().with {
            it.id = role.id
            it.name = role.name
            it
        }
    }

    def void assertUserHasRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.OK.value()
    }

    def addRoleToUser(callerToken, userToAddRoleTo, roleToAdd) {
        return cloud20.addApplicationRoleToUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status
    }

    def addTenant(callerToken, tenantToAdd) {
        return cloud20.addTenant(callerToken, tenantToAdd)
    }

    def addRoleToUserOnTenant(callerToken, tenantId, userId, roleId) {
        return cloud20.addRoleToUserOnTenant(callerToken, tenantId, userId, roleId).status
    }

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def createIdentityAdmin(String identityAdminUsername = IDENTITY_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(specificationServiceAdminToken, v2Factory.createUserForCreate(identityAdminUsername, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def identityAdmin = cloud20.getUserByName(specificationServiceAdminToken, identityAdminUsername).getEntity(User).value
        cloud20.addApplicationRoleToUser(specificationServiceAdminToken, Constants.IDENTITY_RS_DOMAIN_ADMIN_ROLE_ID, identityAdmin.id)
        return identityAdmin;
    }

    def createUserAdmin(String callerToken = specificationIdentityAdminToken, String adminUsername = USER_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(adminUsername, "display", "test@rackspace.com", true, null, domainId, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(callerToken, adminUsername).getEntity(User).value
        return userAdmin;
    }

    def createDefaultUser(String callerToken, String userName = DEFAULT_USER_USERNAME_PREFIX + getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(userName, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def user = cloud20.getUserByName(callerToken, userName).getEntity(User).value
        return user
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }

    def String getCloudAuthIdentityAdminRole() {
        return IdentityUserTypeEnum.IDENTITY_ADMIN.getRoleName()
    }

    def String getCloudAuthIdentityUserManageRole() {
        return IdentityUserTypeEnum.USER_MANAGER.getRoleName()
    }

    def String getCloudAuthDefaultUserRole() {
        return IdentityUserTypeEnum.DEFAULT_USER.getRoleName()
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }
}
