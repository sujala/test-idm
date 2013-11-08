package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.User
import org.springframework.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootIntegrationTest

/**
 * Tests the functionality of propagating roles when assigned as global roles (non-tenant specific).
 */
class GlobalPropagatingRoleIntegrationTest extends RootIntegrationTest {
    private static final String SERVICE_ADMIN_USERNAME = "authQE";
    private static final String SERVICE_ADMIN_PWD = "Auth1234"

    public static final String IDENTITY_ADMIN_USERNAME_PREFIX = "identityAdmin"
    public static final String USER_ADMIN_USERNAME_PREFIX = "userAdmin"
    public static final String DEFAULT_USER_USERNAME_PREFIX = "defaultUser"
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

    def "we can create a role with weight and propagate values"() {
        String roleName = ROLE_NAME_PREFIX + FEATURE_RANDOM

        when:
        def role = v2Factory.createRole(propagate).with {
            it.name = roleName
            return it
        }
        def response = cloud20.createRole(specificationServiceAdminToken, role)
        def createdRole = response.getEntity(Role).value

        def propagateValue = null
        propagateValue = createdRole.propagate

        then:
        propagateValue == expectedPropagate

        cleanup:
        deleteRoleQuietly(createdRole)

        where:
        propagate | expectedPropagate
        null      | false
        true      | true
        false     | false
    }

    def "service admins can add a propagating role to a user admin"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()

        //create the propagating role
        def propagatingRole = createPropagateRole()

        when: "Add propagating role to user admin"
        addRoleToUser(specificationServiceAdminToken, userAdmin, propagatingRole)

        then: "user admin has role"
        assertUserHasRole(userAdmin, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(userAdmin)
    }

    def "identity admins can add a propagating role to a user admin"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()

        //create the propagating role
        def propagatingRole = createPropagateRole()

        when: "Add propagating role to user admin"
        addRoleToUser(specificationIdentityAdminToken, userAdmin, propagatingRole)

        then: "user admin has role"
        assertUserHasRole(userAdmin, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(userAdmin)
    }

    /**
     * Current implementation supports adding a propagate role to an identity admin. This may not be desirable, but
     * original specification was not clear on whether this should be allowed. Since this is as designed, add a test demonstrating
     * the functionality.
     *
     * @return
     */
    def "can assign a propagate role to an identity admin"() {
        //create the identity admin
        def localIdentityAdmin = createIdentityAdmin()

        //create the propagating role
        def propagatingRole = createPropagateRole()

        when:
        //try to add the role to identity admin
        addRoleToUser(specificationServiceAdminToken, localIdentityAdmin, propagatingRole)

        then:
        //verify role not assigned.
        assertUserHasRole(localIdentityAdmin, propagatingRole)

        cleanup:
        deleteUserQuietly(localIdentityAdmin)
        deleteRoleQuietly(propagatingRole)
    }

    /**
     * Current implementation supports adding a propagate role to a default user. This may not be desirable, but
     * original specification was not clear on whether this should be allowed. Since this is as designed, add a test demonstrating
     * the functionality.
     *
     * @return
     */
    def "can assign a propagate role to a default user"() {
        //create the admin
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create new default user
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        when:
        //try to add the role to user
        addRoleToUser(specificationServiceAdminToken, defaultUser, propagatingRole)

        then:
        //verify role not assigned.
        assertUserHasRole(defaultUser, propagatingRole)

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(defaultUser)
        deleteRoleQuietly(propagatingRole)
    }

    def "when add a propagating role to a user admin, all existing sub users of the admin will gain that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        expect:
        //initially they don't have the role
        assertUserDoesNotHaveRole(defaultUser, propagatingRole)
        assertUserDoesNotHaveRole(userAdmin, propagatingRole)

        //add the role to user admin
        addRoleToUser(specificationServiceAdminToken, userAdmin, propagatingRole)

        //verify user admin AND sub user now have role
        assertUserHasRole(defaultUser, propagatingRole)
        assertUserHasRole(userAdmin, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when removing a propagating role from a user admin, all existing sub users of the admin will lose that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        expect:
        //add the role to user admin
        addRoleToUser(specificationServiceAdminToken, userAdmin, propagatingRole)

        //verify user admin AND sub user have role
        assertUserHasRole(defaultUser, propagatingRole)
        assertUserHasRole(userAdmin, propagatingRole)

        //remove the role from user admin
        removeRoleFromUser(specificationServiceAdminToken, userAdmin, propagatingRole)

        //verify they no longer have the role
        assertUserDoesNotHaveRole(defaultUser, propagatingRole)
        assertUserDoesNotHaveRole(userAdmin, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when add a non-propagating role to a user admin, all existing sub users of the admin will not have that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the non-propagating role
        def nonPropagatingRole = createPropagateRole(false)

        expect:
        //they don't have the role
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)
        assertUserDoesNotHaveRole(userAdmin, nonPropagatingRole)

        //add the role to user admin
        addRoleToUser(specificationServiceAdminToken, userAdmin, nonPropagatingRole)

        //verify user admin has role, but sub user does not
        assertUserHasRole(userAdmin, nonPropagatingRole)
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when removing a non-propagating role from a user admin, all existing sub users of the admin will still not have that role"() {
        //create the admin and a sub-user
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken)

        //create the propagating role
        def nonPropagatingRole = createPropagateRole(false)

        //add the role to user admin
        addRoleToUser(specificationServiceAdminToken, userAdmin, nonPropagatingRole)

        expect:
        //verify user admin has role, but sub user does not
        assertUserHasRole(userAdmin, nonPropagatingRole)
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)

        //remove the role from user admin
        removeRoleFromUser(specificationServiceAdminToken, userAdmin, nonPropagatingRole)

        //verify neither have role
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)
        assertUserDoesNotHaveRole(userAdmin, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when add a propagating role to a user admin, all new sub users of the admin will have that role"() {
        //create the admin
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create the propagating role
        def propagatingRole = createPropagateRole()

        //add the role to user admin
        addRoleToUser(specificationServiceAdminToken, userAdmin, propagatingRole)

        //create new user
        def defaultUser = createDefaultUser(userAdminToken)

        expect:
        //verify sub user has the role
        assertUserHasRole(defaultUser, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "after adding a propagating role to an identity admin, existing userAdmins and subusers will not get the propagating role"() {
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

        when: "add propagating roles to identity admin"
        //add the role to user admin
        addRoleToUser(specificationServiceAdminToken, identityAdmin, propagatingRole)
        assertUserHasRole(identityAdmin, propagatingRole)

        then: "existing users do not get propagating role"
        //verify userAdmin under identity admin does NOT have role
        assertUserDoesNotHaveRole(userAdmin, propagatingRole)
        assertUserDoesNotHaveRole(defaultUser, propagatingRole)

        cleanup:
        deleteUserQuietly(identityAdmin)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(propagatingRole)
    }

    def "when add a propagating role to an identity admin then new userAdmins and new subusers will not get the propagating role"() {
        //create identity admin
        def identityAdmin = createIdentityAdmin()
        def identityAdminToken = authenticate(identityAdmin.username)
        def propagatingRole = createPropagateRole()
        addRoleToUser(specificationServiceAdminToken, identityAdmin, propagatingRole)

        //verify identity admin has the role
        assertUserHasRole(identityAdmin, propagatingRole)

        when: "create new user admin and default user from identity admin with propagate role"
        //create the user admin
        def userAdmin = createUserAdmin(identityAdminToken)
        def userAdminToken = authenticate(userAdmin.username)

        //create new user
        def defaultUser = createDefaultUser(userAdminToken)

        then: "new users do not get propagating role from identity admin"
        //verify userAdmin under identity admin does NOT have role
        assertUserDoesNotHaveRole(userAdmin, propagatingRole)
        assertUserDoesNotHaveRole(defaultUser, propagatingRole)

        cleanup:
        deleteUserQuietly(identityAdmin)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
        deleteRoleQuietly(propagatingRole)
    }

    def "when add a non-propagating role to a user admin, all new sub users of the admin will not have that role"() {
        //create the admin
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create the nonpropagating role
        def nonPropagatingRole = createPropagateRole(false)

        //add the role to user admin
        addRoleToUser(specificationServiceAdminToken, userAdmin, nonPropagatingRole)

        //create new user
        def defaultUser = createDefaultUser(userAdminToken)

        expect: "newly created user does not have nonpropagating role"
        //verify sub user does not have the role
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
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

    def void assertUserHasRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.OK.value()
    }

    def void assertUserDoesNotHaveRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.NOT_FOUND.value()
    }

    def void addRoleToUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.addApplicationRoleToUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == HttpStatus.OK.value()
    }

    def void removeRoleFromUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.deleteApplicationRoleFromUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == HttpStatus.NO_CONTENT.value()
    }

    def createPropagateRole(boolean propagate = true, int weight = STANDARD_PROPAGATING_ROLE_WEIGHT, String roleName = ROLE_NAME_PREFIX + getNormalizedRandomString()) {
        def role = v2Factory.createRole(propagate).with {
            it.name = roleName
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

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }
}
