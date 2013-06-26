package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.api.idm.v1.AuthData
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspacecloud.docs.auth.api.v1.User
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import javax.mail.search.DateTerm

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/25/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class Cloud11IntegrationTest extends RootIntegrationTest {

    @Shared def serviceAdmin
    @Shared def serviceAdminToken
    @Shared def identityAdmin
    @Shared def identityAdminToken
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def randomMosso

    def setupSpec(){
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml");

        sharedRandom = ("$randomness").replace('-',"")

        serviceAdminToken = authenticate20("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = getUserByName20(serviceAdminToken, "authQE").getEntity(org.openstack.docs.identity.api.v2.User)

        String adminUsername = "identityAdmin" + sharedRandom
        createUser20(serviceAdminToken, v2Factory.createUserForCreate(adminUsername, "adminUser", "someEmail@rackspace.com", true, "ORD", null, "Password1"))
        identityAdmin = getUserByName20(serviceAdminToken, adminUsername).getEntity(org.openstack.docs.identity.api.v2.User)
        identityAdminToken = authenticate20(adminUsername, "Password1").getEntity(AuthenticateResponse).value.token.id
    }

    def cleanupSpec() {
        deleteUser11(identityAdmin.username)
    }

    def setup(){
        Random random = new Random()
        randomMosso = 10000000 + random.nextInt(1000000)
        authUser11 = "auth"
        authUserPwd11 = "auth123"
    }

    def "Authenticate with password credentials returns 200"() {
        given:
        String username = "auth" + sharedRandom
        String password = "Password1"
        String domain = "domain" + sharedRandom
        def user = v2Factory.createUserForCreate(username, "displayName", "test@email.com", true, "DFW", domain, password)

        when:
        createUser20(identityAdminToken, user)
        def authResponse = authenticate11(v1Factory.createPasswordCredentials(username, password))

        def getUser20Response = getUserByName20(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User)
        deleteUser11(username)
        hardDeleteUser(serviceAdminToken, userEntity.id)

        then:
        authResponse.status == 200
    }

    def "CRUD user v1.1"() {
        given:
        String username = "userIT" + sharedRandom
        User user = v1Factory.createUser(username, "1234567890", randomMosso, null, true)
        User userForUpdate = v1Factory.createUser(username, null, randomMosso+1, "someNastId1", true)

        when:
        def userCreateResponse = createUser11(user)
        def getUserResponse = getUserByName11(username)
        def getUser20Response = getUserByName20(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User)

        def updateUserResponse = updateUser11(username, userForUpdate)
        def deleteUserResponse = deleteUser11(username)
        def hardDeleteUserResponse = hardDeleteUser(serviceAdminToken, userEntity.id)
        def deleteTenantResponse = deleteTenant20(serviceAdminToken, String.valueOf(randomMosso))

        then:
        userCreateResponse.status == 201
        User createUser = userCreateResponse.getEntity(User)
        createUser.key == "1234567890"
        createUser.mossoId == randomMosso
        createUser.enabled == true

        getUserResponse.status == 200
        User getUser = getUserResponse.getEntity(User)
        getUser.enabled == true
        getUser.nastId == createUser.nastId
        getUser.mossoId == randomMosso
        getUser.id == username

        updateUserResponse.status == 200
        User updateUser = updateUserResponse.getEntity(User)
        updateUser.id == username
        updateUser.nastId == "someNastId1"
        updateUser.mossoId == randomMosso+1
        updateUser.enabled == true

        deleteUserResponse.status == 204
        hardDeleteUserResponse.status == 204
        deleteTenantResponse.status == 204
    }

    def "Create User with existing mosso should return a 400"() {
        given:
        String username = "userExistingMosso" + sharedRandom
        User user = v1Factory.createUser(username, "1234567890", randomMosso, null, true)

        when:
        def userCreateResponse = createUser11(user)
        def getUser20Response = getUserByName20(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User)
        user.id = "userExistingMosso2"
        def userCreateNewNameResponse = createUser11(user)

        def deleteUserResponse = deleteUser11(username)
        def hardDeleteUserResponse = hardDeleteUser(serviceAdminToken, userEntity.id)
        def deleteTenantResponse = deleteTenant20(serviceAdminToken, String.valueOf(randomMosso))

        then:
        userCreateResponse.status == 201
        User createUser = userCreateResponse.getEntity(User)
        createUser.key == "1234567890"
        createUser.mossoId == randomMosso
        createUser.enabled == true

        userCreateNewNameResponse.status == 400

        deleteUserResponse.status == 204
        hardDeleteUserResponse.status == 204
        deleteTenantResponse.status == 204
    }

    def "Don't allow disabled identity/service admin to get/create user"() {
        given:
        User userForUpdate = v1Factory.createUser(identityAdmin.username, null, null, null, false)
        String username = "userAdmin" + sharedRandom
        User user = v1Factory.createUser(username, "1234567890", randomMosso, null, true)
        String username2 = "userAdmin2" + sharedRandom
        User userForCreate = v1Factory.createUser(username2, "1234567890", randomMosso+1, null, true)

        when:
        def updateUser = updateUser11(identityAdmin.username, userForUpdate)
        createUser11(user)
        authUser11 = identityAdmin.username
        authUserPwd11 = "Password1"
        def getUser = getUserByName11(user.id)
        def createUser = createUser11(userForCreate)

        then:
        updateUser.status == 200
        getUser.status == 403
        createUser.status == 403
    }

    @Ignore
    def "Identity admin should not be allowed to delete himself"() {
        given:
        User userForUpdate = v1Factory.createUser(identityAdmin.username, null, null, null, true)

        when:
        def updateUser = updateUser11(identityAdmin.username, userForUpdate)
        authUser11 = identityAdmin.username
        authUserPwd11 = "Password1"
        def deleteAdminUser = deleteUser11(identityAdmin.username)

        then:
        updateUser.status == 200
        deleteAdminUser.status == 403
    }

    def "authenticate and verify token entropy"() {
        given:
        def username = "user$sharedRandom"
        def key = "$sharedRandom"
        def credential = v1Factory.createApiKeyCredentials(username, key)

        def user = v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", sharedRandom, "Password1")
        def userId = createUser20(identityAdminToken, user).getEntity(org.openstack.docs.identity.api.v2.User).id
        addApiKeyToUser(serviceAdminToken, userId, credential)

        when:
        def startTime = new DateTime()
        def expOne = authAndExpire(username, key)
        def expTwo = authAndExpire(username, key)
        def expThree = authAndExpire(username, key)
        def endTime = new DateTime()

        def range = getRange(defaultExpirationSeconds, startTime, endTime)
        hardDeleteUser(serviceAdminToken, userId)

        then:
        expOne <= range.get("max")
        expOne >= range.get("min")
        expTwo <= range.get("max")
        expTwo <= range.get("min")
        expThree >= range.get("max")
        expThree >= range.get("min")
    }

    def authAndExpire(username, key) {
        def token = authenticate11(v1Factory.createUserKeyCredentials(username, key)).getEntity(AuthData).accessToken
        revokeToken11(authUser11, authUserPwd11, token.id)
        return token.id
    }
}
