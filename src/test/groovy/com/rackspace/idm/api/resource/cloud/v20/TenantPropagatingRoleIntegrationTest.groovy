package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.util.JSONReaderForRoles
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootIntegrationTest

/**
 * Tests propagation of roles when the role is assigned as a tenant based role as opposed to a global (non-tenant) role. See GlobalPropagatingRoleIntegrationTest
 * for tests of global role propagation.
 */
class TenantPropagatingRoleIntegrationTest extends RootIntegrationTest {
    private static final String SERVICE_ADMIN_USERNAME = "authQE";
    private static final String SERVICE_ADMIN_PWD = "Auth1234"

    public static final String IDENTITY_ADMIN_USERNAME_PREFIX = "identityAdmin"
    public static final String USER_ADMIN_USERNAME_PREFIX = "userAdmin"
    public static final String DEFAULT_USER_USERNAME_PREFIX = "defaultUser"
    public static final String DEFAULT_TENANT_NAME_PREFIX = "tenant"
    public static final String ROLE_NAME_PREFIX = "role"

    public static final String DEFAULT_PASSWORD = "Password1"

    public static final int STANDARD_PROPAGATING_ROLE_WEIGHT = 500

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
        specificationServiceAdmin = cloud20.getUserByName(specificationServiceAdminToken, SERVICE_ADMIN_USERNAME).getEntity(User)

        //create a new shared identity admin for these tests
        specificationIdentityAdmin = createIdentityAdmin(IDENTITY_ADMIN_USERNAME_PREFIX + SPECIFICATION_RANDOM)
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

    def "service admins can add a tenant propagating role to a user admin"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        when: "Add tenant propagating role to user admin"
        assertAddTenantRoleToUserReturnsStatus(specificationServiceAdminToken, tenant, userAdmin, propagatingRole)

        then: "user admin has tenant role, but not global role"
        assertUserDoesNotHaveGlobalRole(userAdmin, propagatingRole)
        assertUserHasRoleOnTenant(userAdmin, tenant, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "identity admins can add a tenant propagating role to a user admin"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        when: "Add tenant propagating role to user admin"
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, userAdmin, propagatingRole)

        then: "user admin has tenant role, but not global role"
        assertUserDoesNotHaveGlobalRole(userAdmin, propagatingRole)
        assertUserHasRoleOnTenant(userAdmin, tenant, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "can assign a tenant propagate role to a default user"() {
        //create the admin
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create new default user
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        when: "Add tenant propagating role to user admin"
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, defaultUser, propagatingRole)

        then: "user admin has tenant role, but not global role"
        assertUserDoesNotHaveGlobalRole(defaultUser, propagatingRole)
        assertUserHasRoleOnTenant(defaultUser, tenant, propagatingRole)

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(defaultUser)
        deleteRoleQuietly(propagatingRole)
        deleteTenantQuietly(tenant)
    }

    def "when add a tenant propagating role to a user admin, all existing sub users of the admin will gain that tenant role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        when:  "add the role to user admin on tenant"
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, userAdmin, propagatingRole)

        then:
        assertUserHasRoleOnTenant(userAdmin, tenant, propagatingRole)
        assertUserHasRoleOnTenant(defaultUser, tenant, propagatingRole)
        assertUserDoesNotHaveGlobalRole(userAdmin, propagatingRole)
        assertUserDoesNotHaveGlobalRole(defaultUser, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "when remove a tenant propagating role from a user admin, all existing sub users of the admin will lose that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, userAdmin, propagatingRole)

        assertUserDoesNotHaveGlobalRole(userAdmin, propagatingRole)
        assertUserHasRoleOnTenant(userAdmin, tenant, propagatingRole)
        assertUserDoesNotHaveGlobalRole(defaultUser, propagatingRole)
        assertUserHasRoleOnTenant(defaultUser, tenant, propagatingRole)

        when: "remove propagating tenant role"
        removeTenantRoleFromUser(specificationServiceAdminToken, tenant, userAdmin, propagatingRole)

        then:
        assertUserDoesNotHaveGlobalRole(userAdmin, propagatingRole)
        assertUserDoesNotHaveRoleOnTenant(userAdmin, tenant, propagatingRole)
        assertUserDoesNotHaveGlobalRole(defaultUser, propagatingRole)
        assertUserDoesNotHaveRoleOnTenant(defaultUser, tenant, propagatingRole)

        expect: "try to remove role from sub user"
        assertRemoveTenantRoleFromUserReturnsStatus(specificationServiceAdminToken, tenant, defaultUser, propagatingRole, HttpStatus.NO_CONTENT)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "when add a tenant non-propagating role to a user admin, all existing sub users of the admin will not have that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the non-propagating role
        def nonPropagatingRole = createPropagateRole(false)

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        when: "add the role to user admin"
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, userAdmin, nonPropagatingRole)

        then: "user admin has role, but sub user does not"
        assertUserDoesNotHaveGlobalRole(userAdmin, nonPropagatingRole)
        assertUserHasRoleOnTenant(userAdmin, tenant, nonPropagatingRole)
        assertUserDoesNotHaveGlobalRole(defaultUser, nonPropagatingRole)
        assertUserDoesNotHaveRoleOnTenant(defaultUser, tenant, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "when removing a non-propagating tenant role from a user admin, all existing sub users of the admin will still not have that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the non-propagating role
        def nonPropagatingRole = createPropagateRole(false)

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, userAdmin, nonPropagatingRole)

        when: "remove role from user admin"
        removeTenantRoleFromUser(specificationIdentityAdminToken, tenant, userAdmin, nonPropagatingRole)

        then: "Neither user has role"
        assertUserDoesNotHaveGlobalRole(userAdmin, nonPropagatingRole)
        assertUserDoesNotHaveRoleOnTenant(userAdmin, tenant, nonPropagatingRole)
        assertUserDoesNotHaveGlobalRole(defaultUser, nonPropagatingRole)
        assertUserDoesNotHaveRoleOnTenant(defaultUser, tenant, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "when add a propagating tenant role to a user admin, all new sub users of the admin will have that tenant role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        //add the tenant role to user-admin
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, userAdmin, propagatingRole)

        when:  "create new user in domain"
        def defaultUser = createDefaultUser(userAdminToken)

        then:
        assertUserHasRoleOnTenant(userAdmin, tenant, propagatingRole)
        assertUserHasRoleOnTenant(defaultUser, tenant, propagatingRole)
        assertUserDoesNotHaveGlobalRole(userAdmin, propagatingRole)
        assertUserDoesNotHaveGlobalRole(defaultUser, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    def "after adding a tenant propagating role to an identity admin, existing userAdmins and subusers will not get the propagating role"() {
        //create identity admin
        def identityAdmin = createIdentityAdmin()
        def identityAdminToken = authenticate(identityAdmin.username)

        //create the user admin
        def userAdmin = createUserAdmin(identityAdminToken)
        def userAdminToken = authenticate(userAdmin.username)

        //create new user
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        when: "add propagating roles to identity admin"
        //add the tenant role to user-admin
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, identityAdmin, propagatingRole)

        then: "existing users do not get propagating role"
        assertUserHasRoleOnTenant(identityAdmin, tenant, propagatingRole)
        assertUserDoesNotHaveRoleOnTenant(userAdmin, tenant, propagatingRole)
        assertUserDoesNotHaveRoleOnTenant(defaultUser, tenant, propagatingRole)

        cleanup:
        deleteUserQuietly(identityAdmin)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(propagatingRole)
        deleteTenantQuietly(tenant)
    }

    def "when add a propagating tenant role to an identity admin then new userAdmins and new subusers will not get the propagating role"() {
        //create identity admin
        def identityAdmin = createIdentityAdmin()
        def identityAdminToken = authenticate(identityAdmin.username)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        //add the tenant role to user-admin
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, identityAdmin, propagatingRole)

        when: "create new user-admin and default user"
        def userAdmin = createUserAdmin(identityAdminToken)
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        then: "new users do not get propagating role"
        assertUserHasRoleOnTenant(identityAdmin, tenant, propagatingRole)
        assertUserDoesNotHaveRoleOnTenant(userAdmin, tenant, propagatingRole)
        assertUserDoesNotHaveRoleOnTenant(defaultUser, tenant, propagatingRole)

        cleanup:
        deleteUserQuietly(identityAdmin)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(propagatingRole)
        deleteTenantQuietly(tenant)
    }

    def "when add a non-propagating tenant role to a user admin, all new sub users of the admin will not have that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create the non-propagating role
        def nonPropagatingRole = createPropagateRole(false)

        //create a tenant
        Tenant tenant = createTenant(specificationServiceAdminToken)

        //add the tenant role to user-admin
        assertAddTenantRoleToUserReturnsStatus(specificationIdentityAdminToken, tenant, userAdmin, nonPropagatingRole)

        when:  "create new user in domain"
        def defaultUser = createDefaultUser(userAdminToken)

        then:
        assertUserHasRoleOnTenant(userAdmin, tenant, nonPropagatingRole)
        assertUserDoesNotHaveRoleOnTenant(defaultUser, tenant, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteTenantQuietly(tenant)
    }

    //Helper Methods
    def deleteRoleQuietly(role) {
        if (role != null) {
            try {
                cloud20.deleteRole(specificationServiceAdminToken, role.getId())
            } catch (all) {
                //ignore
            }
        }
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

    def deleteTenantQuietly(tenant) {
        if (tenant != null) {
            try {
                cloud20.deleteTenant(specificationServiceAdminToken, tenant.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def void assertUserHasGlobalRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.OK.value()
    }

    def void assertUserDoesNotHaveGlobalRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.NOT_FOUND.value()
    }

    def void assertUserHasRoleOnTenant(user, tenant, role) {
        assert hasRoleOnTenant(user, tenant, role)
    }

    def void assertUserDoesNotHaveRoleOnTenant(user, tenant, role) {
        assert !hasRoleOnTenant(user, tenant, role)
    }

    def boolean hasRoleOnTenant(user, tenant, role) {
        def response = cloud20.listRolesForUserOnTenant(specificationServiceAdminToken, tenant.getId(), user.getId())
        assert response.status == HttpStatus.OK.value()

        JSONReaderForRoles jsonReaderForRoles = new JSONReaderForRoles();
        InputStream is = response.getEntityInputStream();
        RoleList roleList = jsonReaderForRoles.readFrom(RoleList.class, null, null, null, null, is)
        is.close()

        for (Role assignedRole : roleList.getRole()) {
            if (role.getId().equals(assignedRole.getId()))  {
                return true
            }
        }
        return false
    }

    def void assertAddTenantRoleToUserReturnsStatus(callerToken, tenant, userToAddRoleTo, roleToAdd, HttpStatus statusResult = HttpStatus.OK) {
        assert cloud20.addRoleToUserOnTenant(callerToken, tenant.getId(), userToAddRoleTo.getId(), roleToAdd.getId()).status == statusResult.value()
    }

    def void removeTenantRoleFromUser(callerToken, tenant, user, role) {
        assert cloud20.deleteRoleFromUserOnTenant(callerToken, tenant.getId(), user.getId(), role.getId()).status == HttpStatus.NO_CONTENT.value()
    }

    def void assertRemoveTenantRoleFromUserReturnsStatus(callerToken, tenant, user, role, HttpStatus statusResult = HttpStatus.OK) {
        assert cloud20.deleteRoleFromUserOnTenant(callerToken, tenant.getId(), user.getId(), role.getId()).status == statusResult.value()
    }


    def void addGlobalRoleToUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.addApplicationRoleToUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == HttpStatus.OK.value()
    }

    def void removeGlobalRoleFromUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.deleteApplicationRoleFromUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == HttpStatus.NO_CONTENT.value()
    }

    def createPropagateRole(boolean propagate = true, String roleName = ROLE_NAME_PREFIX + getNormalizedRandomString()) {
        def role = v2Factory.createRole(propagate).with {
            it.name = roleName
            it.propagate = propagate
            it.otherAttributes = null
            return it
        }
        def responsePropagateRole = cloud20.createRole(specificationServiceAdminToken, role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        return propagatingRole
    }

    def createIdentityAdmin(String identityAdminUsername = IDENTITY_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        def createResponse = cloud20.createUser(specificationServiceAdminToken, v2Factory.createUserForCreate(identityAdminUsername, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(specificationServiceAdminToken, identityAdminUsername).getEntity(User)
        return userAdmin;
    }

    def createUserAdmin(String callerToken = specificationIdentityAdminToken, String adminUsername = USER_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        def createResponse = cloud20.createUser(callerToken, v2Factory.createUserForCreate(adminUsername, "display", "test@rackspace.com", true, null, domainId, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(callerToken, adminUsername).getEntity(User)
        return userAdmin;
    }

    def createDefaultUser(String callerToken, String userName = DEFAULT_USER_USERNAME_PREFIX + getNormalizedRandomString()) {
        def createResponse = cloud20.createUser(callerToken, v2Factory.createUserForCreate(userName, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def user = cloud20.getUserByName(callerToken, userName).getEntity(User)
        return user
    }

    def createTenant(String callerToken, String name = DEFAULT_TENANT_NAME_PREFIX + getNormalizedRandomString(), boolean enabled = true) {
        def createResponse = cloud20.addTenant(callerToken, v2Factory.createTenant(name, "displayName for $name", enabled))
        def tenant = createResponse.getEntity(Tenant).value
        return tenant
    }

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }
}
