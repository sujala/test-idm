package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import spock.lang.Ignore
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import com.sun.jersey.api.client.ClientResponse

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForArrayEntity
import org.openstack.docs.identity.api.v2.RoleList
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

class ListUserRoleIntegrationTest extends RootIntegrationTest {
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

    @Autowired
    DefaultAuthorizationService defaultAuthorizationService

    @Autowired
    ApplicationService applicationService

    @Autowired
    Configuration config;

    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    AuthorizationService authorizationService

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

    def "acl test: user managers can list all roles for default users within domain"() {
        //create a user admin and 2 sub-users
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create 2 new default users
        def userManager = createDefaultUser(userAdminToken)
        def userManagerToken = authenticate(userManager.username)

        def defaultUser = createDefaultUser(userAdminToken)

        //create 2 roles to attach to default user
        def roleName1 = getNormalizedRandomString()
        def roleName2 = getNormalizedRandomString()
        Role role1 = createPropagateRole(false, 500, roleName1)
        Role role2 = createPropagateRole(false, 1000, roleName2)

        ClientRole cloudIdentityUserManageRole = getUserManageRole()

        //add user-manage role to user-manager
        addRoleToUser(userAdminToken, userManager, cloudIdentityUserManageRole)

        //add 2 roles to default user
        addRoleToUser(specificationIdentityAdminToken, defaultUser, role1)  //identity admin must assign 500 weight role
        addRoleToUser(userAdminToken, defaultUser, role2)

        when: "When list roles"
        def response = cloud20.listUserGlobalRoles(userManagerToken, defaultUser.getId())

        then:
        response.status == HttpStatus.OK.value
        RoleList roleList = response.getEntity(RoleList).value

        //should contain the two added roles + default user role
        roleList.role.id.contains(getUserDefaultRole().id)
        roleList.role.id.contains(role1.id)
        roleList.role.id.contains(role2.id)

        cleanup:
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userManager)
        deleteRoleQuietly(role1)
        deleteRoleQuietly(role2)
        deleteUserQuietly(userAdmin)
    }

    def "acl test: user managers can not list roles for other user-managers within domain"() {
        //create a user admin and 2 sub-users
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create 2 new default users
        def userManager = createDefaultUser(userAdminToken)
        def userManagerToken = authenticate(userManager.username)

        def userManager2 = createDefaultUser(userAdminToken)

        //create 2 roles
        def roleName1 = getNormalizedRandomString()
        def roleName2 = getNormalizedRandomString()
        Role role1 = createPropagateRole(false, 500, roleName1)
        Role role2 = createPropagateRole(false, 1000, roleName2)

        ClientRole cloudIdentityUserManageRole = getUserManageRole()

        //add user-manage role to user-manager
        addRoleToUser(userAdminToken, userManager, cloudIdentityUserManageRole)
        addRoleToUser(userAdminToken, userManager2, cloudIdentityUserManageRole)

        //add 2 roles to 2nd user manager
        addRoleToUser(specificationIdentityAdminToken, userManager2, role1)  //identity admin must assign 500 weight role
        addRoleToUser(userAdminToken, userManager2, role2)

        when: "When list roles"
        def response = cloud20.listUserGlobalRoles(userManagerToken, userManager2.getId())

        then:
        response.status == HttpStatus.FORBIDDEN.value

        cleanup:
        deleteUserQuietly(userManager2)
        deleteUserQuietly(userManager)
        deleteRoleQuietly(role1)
        deleteRoleQuietly(role2)
        deleteUserQuietly(userAdmin)
    }

    def "acl test: user managers can not list roles for user-admin users within domain"() {
        //create a user admin and 2 sub-users
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        //create user manager
        def userManager = createDefaultUser(userAdminToken)
        def userManagerToken = authenticate(userManager.username)

        //add user-manage role to user-manager
        ClientRole cloudIdentityUserManageRole = getUserManageRole()
        addRoleToUser(userAdminToken, userManager, cloudIdentityUserManageRole)

        when: "When list roles"
        def response = cloud20.listUserGlobalRoles(userManagerToken, userAdmin.getId())

        then:
        response.status == HttpStatus.FORBIDDEN.value

        cleanup:
        deleteUserQuietly(userManager)
        deleteUserQuietly(userAdmin)
    }

    def "identity:get-user-roles-global can be used to retrieve roles for users"() {
        //start with role enabled
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GET_USER_ROLES_GLOBAL_ROLE_PROP, true)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)
        String uaToken = token.accessTokenString
        String iaToken = utils.getToken(users[0].username)

        def userRole = authorizationService.getCachedIdentityRoleByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName())

        when: "user admin tries to load identity admin roles"
        def uaResponse = cloud20.listUserGlobalRoles(uaToken, users[0].id)

        then:
        uaResponse.status == org.apache.http.HttpStatus.SC_FORBIDDEN

        when: "give user global roles role"
        utils.addRoleToUser(userAdmin, userRole.id)
        uaResponse = cloud20.listUserGlobalRoles(uaToken, users[0].id)

        then: "user admin can now access roles"
        uaResponse != null
        uaResponse.status == org.apache.http.HttpStatus.SC_OK

        when: "identity admin tries to load roles of user admin"
        def iaResponse = cloud20.listUserGlobalRoles(iaToken, userAdmin.id)

        then: "allowed"
        iaResponse.status == org.apache.http.HttpStatus.SC_OK

        when: "disable endpoint global role feature"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GET_USER_ROLES_GLOBAL_ROLE_PROP, false)
        uaResponse = cloud20.listUserGlobalRoles(uaToken, users[0].id)
        iaResponse = cloud20.listUserGlobalRoles(iaToken, userAdmin.id)

        then: "user admin can no longer access token endpoints"
        uaResponse.status == org.apache.http.HttpStatus.SC_FORBIDDEN
        iaResponse.status == org.apache.http.HttpStatus.SC_OK

        cleanup:
        reloadableConfiguration.reset()
        try {
            utils.deleteUsers(users)
        } catch (Exception ex) {/*ignore*/
        }
    }

    private ClientRole getUserManageRole() {
/*
        get the identity admin role. Calling explicitly here rather than using DefaultAuthorizationService because in master branch the code was changed
        to no longer use static methods (required to avoid race conditions within tests). Rather than depend on something that will be changed once the code
        is merged from 2.0.0 branch to master, making the call directly. Note - once merged to master this method can safely use the DefaultAuthorizationService
        _instance_ method to retrieve the role.
         */
        ClientRole role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserManageRole());
        role
    }

    private ClientRole getUserDefaultRole() {
/*
        get the identity admin role. Calling explicitly here rather than using DefaultAuthorizationService because in master branch the code was changed
        to no longer use static methods (required to avoid race conditions within tests). Rather than depend on something that will be changed once the code
        is merged from 2.0.0 branch to master, making the call directly. Note - once merged to master this method can safely use the DefaultAuthorizationService
        _instance_ method to retrieve the role.
         */
        ClientRole role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthDefaultUserRole());
        role
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
            it.propagate = propagate
            it.otherAttributes = null
            return it
        }
        def responsePropagateRole = cloud20.createRole(specificationServiceAdminToken, role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        return propagatingRole
    }

    def createIdentityAdmin(String identityAdminUsername = IDENTITY_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(specificationServiceAdminToken, v2Factory.createUserForCreate(identityAdminUsername, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(specificationServiceAdminToken, identityAdminUsername).getEntity(User).value
        return userAdmin;
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

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }

    def String getCloudAuthUserManageRole() {
        return config.getString("cloudAuth.userManagedRole");
    }

    def String getCloudAuthDefaultUserRole() {
        return config.getString("cloudAuth.userRole");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private def listRolesForUserOnTenant(String token, String tenantId, String userId) {
        cloud20.resource.path(cloud20.path20).path("tenants").path(tenantId).path("users").path(userId).path("roles")
                .header(cloud20.X_AUTH_TOKEN, token).get(ClientResponse)
    }

    public class JSONReaderForRoles extends JSONReaderForArrayEntity<RoleList> {
        @Override
        public RoleList readFrom(Class<RoleList> type,
                                 Type genericType, Annotation[] annotations, MediaType mediaType,
                                 MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

            return read(JSONConstants.ROLES, JSONConstants.ROLE, inputStream);
        }
    }
}
