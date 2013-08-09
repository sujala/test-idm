package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools
import com.rackspace.idm.domain.entity.ScopeAccess
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.RaxAuthConstants.QNAME_PROPAGATE
import static com.rackspace.idm.RaxAuthConstants.QNAME_WEIGHT

class PropagatingRoleIntegrationTest extends RootIntegrationTest {
    private static final String SERVICE_ADMIN_USERNAME = "authQE";
    private static final String SERVICE_ADMIN_PWD = "Auth1234"

    public static final String IDENTITY_ADMIN_USERNAME = "auth"
    public static final String IDENTITY_ADMIN_PWD = "auth123"

    @Shared String SHARED_RANDOM

    @Autowired LdapConnectionPools connPools
    @Autowired Configuration config
    @Autowired DefaultCloud20Service cloud20Service

    @Shared
    def serviceAdmin
    @Shared
    def serviceAdminToken

    @Shared
    def identityAdmin
    @Shared
    def identityAdminToken

    def setupSpec() {
        SHARED_RANDOM = getNormalizedRandomString()

        def serviceAdminAuthResponse = cloud20.authenticatePassword(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PWD).getEntity(AuthenticateResponse)
        assert serviceAdminAuthResponse.value instanceof AuthenticateResponse
        serviceAdminToken = serviceAdminAuthResponse.value.token.id
        serviceAdmin = cloud20.getUserByName(serviceAdminToken, SERVICE_ADMIN_USERNAME).getEntity(User)


        identityAdmin = cloud20.getUserByName(serviceAdminToken, IDENTITY_ADMIN_USERNAME).getEntity(User)
        def identityAdminAuthResponse = cloud20.authenticatePassword(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PWD).getEntity(AuthenticateResponse)
        assert identityAdminAuthResponse.value instanceof AuthenticateResponse
        identityAdminToken = identityAdminAuthResponse.value.token.id
    }

    def setup() {
    }

    def cleanupSpec() {
    }

    def "we can create a role when specifying weight and propagate values"() {
        String SHARED_TEST_RANDOM = getNormalizedRandomString()
        String roleName = "role$SHARED_TEST_RANDOM"

        when:
        def role = v2Factory.createRole(propagate, weight).with {
            it.name = roleName
            return it
        }
        def response = cloud20.createRole(serviceAdminToken, role)
        def createdRole = response.getEntity(Role).value

        def propagateValue = null
        def weightValue = null

        if (createdRole.otherAttributes.containsKey(QNAME_PROPAGATE)) {
            propagateValue = createdRole.otherAttributes.get(QNAME_PROPAGATE).toBoolean()
        }
        if (createdRole.otherAttributes.containsKey(QNAME_WEIGHT)) {
            weightValue = createdRole.otherAttributes.get(QNAME_WEIGHT).toInteger()
        }

        then:
        propagateValue == expectedPropagate
        weightValue == expectedWeight

        cleanup:
        deleteRoleQuietly(createdRole)

        where:
        weight | propagate | expectedWeight | expectedPropagate
        null   | null      | 1000           | false
        100    | null      | 100            | false
        null   | true      | 1000           | true
        null   | false     | 1000           | false
        2000   | true      | 2000           | true
    }

    def "when add a propagating role to a user admin, all existing sub users of the admin will have that role"() {
        String SHARED_TEST_RANDOM = getNormalizedRandomString()
        String roleName = "role$SHARED_TEST_RANDOM"

        //create the admin and a sub-user
        def userAdmin = createUserAdmin("userAdmin$SHARED_TEST_RANDOM")
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken, "defaultUser$SHARED_TEST_RANDOM")

        //create the propagating role
        def propagatingRole = createPropagateRole(roleName, true, 500)

        expect:
        //initially they don't have the role
        assertUserDoesNotHaveRole(defaultUser, propagatingRole)
        assertUserDoesNotHaveRole(userAdmin, propagatingRole)

        //add the role to user admin
        addRoleToUser(serviceAdminToken, userAdmin, propagatingRole)

        //verify user admin AND sub user now have role
        assertUserHasRole(defaultUser, propagatingRole)
        assertUserHasRole(userAdmin, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when removing a propagating role from a user admin, all existing sub users of the admin will lose that role"() {
        String SHARED_TEST_RANDOM = getNormalizedRandomString()
        String roleName = "role$SHARED_TEST_RANDOM"

        //create the admin and a sub-user
        def userAdmin = createUserAdmin("userAdmin$SHARED_TEST_RANDOM")
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken, "defaultUser$SHARED_TEST_RANDOM")

        //create the propagating role
        def propagatingRole = createPropagateRole(roleName, true, 500)

        expect:
        //add the role to user admin
        addRoleToUser(serviceAdminToken, userAdmin, propagatingRole)

        //verify user admin AND sub user have role
        assertUserHasRole(defaultUser, propagatingRole)
        assertUserHasRole(userAdmin, propagatingRole)

        //remove the role from user admin
        removeRoleFromUser(serviceAdminToken, userAdmin, propagatingRole)

        //verify they no longer have the role
        assertUserDoesNotHaveRole(defaultUser, propagatingRole)
        assertUserDoesNotHaveRole(userAdmin, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when add a non-propagating role to a user admin, all existing sub users of the admin will not have that role"() {
        String SHARED_TEST_RANDOM = getNormalizedRandomString()
        String roleName = "role$SHARED_TEST_RANDOM"

        //create the admin and a sub-user
        def userAdmin = createUserAdmin("userAdmin$SHARED_TEST_RANDOM")
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken, "defaultUser$SHARED_TEST_RANDOM")

        //create the non-propagating role
        def nonPropagatingRole = createPropagateRole(roleName, false, 500)

        expect:
        //they don't have the role
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)
        assertUserDoesNotHaveRole(userAdmin, nonPropagatingRole)

        //add the role to user admin
        addRoleToUser(serviceAdminToken, userAdmin, nonPropagatingRole)

        //verify user admin has role, but sub user does not
        assertUserHasRole(userAdmin, nonPropagatingRole)
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when removing a non-propagating role from a user admin, all existing sub users of the admin will still not have that role"() {
        String SHARED_TEST_RANDOM = getNormalizedRandomString()
        String roleName = "role$SHARED_TEST_RANDOM"

        //create the admin and a sub-user
        def userAdmin = createUserAdmin("userAdmin$SHARED_TEST_RANDOM")
        def userAdminToken = authenticate(userAdmin.username)
        def defaultUser = createDefaultUser(userAdminToken, "defaultUser$SHARED_TEST_RANDOM")

        //create the propagating role
        def nonPropagatingRole = createPropagateRole(roleName, false, 500)

        //add the role to user admin
        addRoleToUser(serviceAdminToken, userAdmin, nonPropagatingRole)

        expect:
        //verify user admin has role, but sub user does not
        assertUserHasRole(userAdmin, nonPropagatingRole)
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)

        //remove the role from user admin
        removeRoleFromUser(serviceAdminToken, userAdmin, nonPropagatingRole)

        //verify neither have role
        assertUserDoesNotHaveRole(defaultUser, nonPropagatingRole)
        assertUserDoesNotHaveRole(userAdmin, nonPropagatingRole)

        cleanup:
        deleteRoleQuietly(nonPropagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "when add a propagating role to a user admin, all new sub users of the admin will have that role"() {
        String SHARED_TEST_RANDOM = getNormalizedRandomString()
        String roleName = "role$SHARED_TEST_RANDOM"

        //create the admin
        def userAdmin = createUserAdmin("userAdmin$SHARED_TEST_RANDOM")
        def userAdminToken = authenticate(userAdmin.username)

        //create the propagating role
        def propagatingRole = createPropagateRole(roleName, true, 500)

        //add the role to user admin
        addRoleToUser(serviceAdminToken, userAdmin, propagatingRole)

        //create new user
        def defaultUser = createDefaultUser(userAdminToken, "defaultUser$SHARED_TEST_RANDOM")

        expect:
        //verify sub user has the role
        assertUserHasRole(defaultUser, propagatingRole)

        cleanup:
        deleteRoleQuietly(propagatingRole)
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    @Ignore("Demonstrating defect D-13974. Once defect is fixed this test must not be ignored.")
    def "when add a non-propagating role to a user admin, all new sub users of the admin will not have that role"() {
        String SHARED_TEST_RANDOM = getNormalizedRandomString()
        String roleName = "role$SHARED_TEST_RANDOM"

        //create the admin
        def userAdmin = createUserAdmin("userAdmin$SHARED_TEST_RANDOM")
        def userAdminToken = authenticate(userAdmin.username)

        //create the nonpropagating role
        def nonPropagatingRole = createPropagateRole(roleName, false, 500)

        //add the role to user admin
        addRoleToUser(serviceAdminToken, userAdmin, nonPropagatingRole)

        //create new user
        def defaultUser = createDefaultUser(userAdminToken, "defaultUser$SHARED_TEST_RANDOM")

        expect: "newly created user does not have nonpropagating role"
        //verify sub user has the role
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
                cloud20.deleteRole(serviceAdminToken, role.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def deleteUserQuietly(user) {
        if (user != null) {
            try {
                cloud20.destroyUser(serviceAdminToken, user.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def setConfigValues() {
        REFRESH_WINDOW_HOURS = config.getInt("token.refreshWindowHours")
        CLOUD_CLIENT_ID = config.getString("cloudAuth.clientId")
    }

    def void assertUserHasRole(user, role) {
        assert cloud20.getUserApplicationRole(serviceAdminToken, role.getId(), user.getId()).status == 200
    }

    def void assertUserDoesNotHaveRole(user, role) {
        assert cloud20.getUserApplicationRole(serviceAdminToken, role.getId(), user.getId()).status == 404
    }

    def void addRoleToUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.addApplicationRoleToUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == 200
    }

    def void removeRoleFromUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.deleteApplicationRoleFromUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == 204
    }

    def expireTokens(String uid, int hoursOffset) {
        def resultCloudAuthScopeAccess = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=$USER_FOR_AUTH))", "*")
        for (SearchResultEntry entry in resultCloudAuthScopeAccess.getSearchEntries()) {
            def entity = LDAPPersister.getInstance(ScopeAccess.class) decode(entry)
            if (!entity.isAccessTokenExpired(new DateTime())) {
                entity.accessTokenExp = new DateTime().minusHours(hoursOffset).toDate()
                List<Modification> mods = LDAPPersister.getInstance(ScopeAccess.class).getModifications(entity, true)
                connPools.getAppConnPool().modify(entity.getUniqueId(), mods)
            }
        }
    }

    def createPropagateRole(String roleName = getNormalizedRandomString(), boolean propagate = false, int weight = null) {
        def role = v2Factory.createRole(propagate, weight).with {
            it.name = roleName
            return it
        }
        def responsePropagateRole = cloud20.createRole(serviceAdminToken, role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        return propagatingRole
    }

    def createUserAdmin(String adminUsername = "userAdmin" + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        def createResponse = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(adminUsername, "display", "test@rackspace.com", true, null, domainId, "Password1"))
        def userAdmin = cloud20.getUserByName(identityAdminToken, adminUsername).getEntity(User)
        return userAdmin;
    }

    def createDefaultUser(String userAdminToken, String userName = "defaultUser" + getNormalizedRandomString()) {
        def createResponse = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(userName, "display", "test@rackspace.com", true, null, null, "Password1"))
        def user = cloud20.getUserByName(userAdminToken, userName).getEntity(User)
        return user
    }

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, "Password1").getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }
}
