package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootIntegrationTest

class AddRoleIntegrationTest extends RootIntegrationTest {
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

    def "Service admin can assign identity role to user without any role"() {
        //create an identity-admin from which we'll remove the identity-admin role
        def identityAdmin = createIdentityAdmin()

        /*
        get the identity admin role. Calling explicitly here rather than using DefaultAuthorizationService because in master branch the code was changed
        to no longer use static methods (required to avoid race conditions within tests). Rather than depend on something that will be changed once the code
        is merged from 2.0.0 branch to master, making the call directly. Note - once merged to master this method can safely use the DefaultAuthorizationService
        _instance_ method to retrieve the role.
         */
        //
        ClientRole cloudIdentityAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());

        removeRoleFromUser(specificationServiceAdminToken, identityAdmin, cloudIdentityAdminRole)
        assertUserDoesNotHaveRole(identityAdmin, cloudIdentityAdminRole)

        when: "Add role to user without any identity role"
        def status = addRoleToUser(specificationServiceAdminToken, identityAdmin, cloudIdentityAdminRole)
        assert status == HttpStatus.OK.value()

        then: "user admin has role"
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
        assert status == HttpStatus.OK.value()

        then: "default user now has user-manage role"
        assertUserHasRole(defaultUser, role)

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(defaultUser)
        deleteRoleQuietly(role)
    }

    def "User manager cannot assign user-manager role to default user within domain"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        def userManager = createDefaultUser(userAdminToken)
        def userManagerToken = authenticate(userManager.username)

        def defaultUser = createDefaultUser(userAdminToken)

        ClientRole userManageRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityUserManageRole());
        def status = addRoleToUser(specificationServiceAdminToken, userManager, userManageRole)
        assert status == HttpStatus.OK.value()
        assertUserHasRole(userManager, userManageRole) //verify test state

        when: "As user-manager, add user-manager role to default user within my domain"
        def forbiddenStatus = addRoleToUser(userManagerToken, defaultUser, userManageRole)

        then:
        assert forbiddenStatus == HttpStatus.FORBIDDEN.value()
        assertUserDoesNotHaveRole(defaultUser, userManageRole)

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(userManager)
        deleteUserQuietly(defaultUser)
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
            it.clientId = APPLICATION_ID
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
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.OK.value()
    }

    def void assertUserDoesNotHaveRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.NOT_FOUND.value()
    }

    def addRoleToUser(callerToken, userToAddRoleTo, roleToAdd) {
        return cloud20.addApplicationRoleToUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status
    }

    def void removeRoleFromUser(callerToken, userToRemoveRoleFrom, roleToRemove) {
        assert cloud20.deleteApplicationRoleFromUser(callerToken, roleToRemove.getId(), userToRemoveRoleFrom.getId()).status == HttpStatus.NO_CONTENT.value()
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

    def String getCloudAuthIdentityAdminRole() {
        return config.getString("cloudAuth.adminRole");
    }

    def String getCloudAuthIdentityUserManageRole() {
        return config.getString("cloudAuth.userManagedRole");
    }

    def String getCloudAuthDefaultUserRole() {
        return config.getString("cloudAuth.userRole");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }
}
