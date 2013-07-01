package com.rackspace.idm.api.resource.cloud.v11

import com.rackspacecloud.docs.auth.api.v1.AuthData;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspacecloud.docs.auth.api.v1.User
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import spock.lang.Shared
import testHelpers.Cloud11Methods
import testHelpers.Cloud20Methods
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
class Cloud11IntegrationTest extends RootIntegrationTest {

    @Autowired Configuration config

    @Shared def serviceAdmin
    @Shared def serviceAdminToken
    @Shared def identityAdmin
    @Shared def identityAdminToken
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def randomMosso
    @Shared def adminUser = "auth"
    @Shared def adminPassword = "auth123"

    def setupSpec(){
        sharedRandom = ("$randomness").replace('-',"")

        serviceAdminToken = cloud20.authenticate("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = cloud20.getUserByName(serviceAdminToken, "authQE").getEntity(org.openstack.docs.identity.api.v2.User)

        String adminUsername = "identityAdmin" + sharedRandom
        cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(adminUsername, "adminUser", "someEmail@rackspace.com", true, "ORD", null, "Password1"))
        identityAdmin = cloud20.getUserByName(serviceAdminToken, adminUsername).getEntity(org.openstack.docs.identity.api.v2.User)
        identityAdminToken = cloud20.authenticate(adminUsername, "Password1").getEntity(AuthenticateResponse).value.token.id
    }

    def setup(){
        cloud11.authUser = adminUser
        cloud11.authPassword = adminPassword
        Random random = new Random()
        randomMosso = 10000000 + random.nextInt(1000000)

        entropy = config.getDouble("token.entropy")
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")

    }

    def cleanupSpec() {
        cloud11.deleteUser(identityAdmin.username)
    }



    def "Authenticate with password credentials returns 200"() {
        given:
        String username = "auth" + sharedRandom
        String password = "Password1"
        String domain = "domain" + sharedRandom
        def user = v2Factory.createUserForCreate(username, "displayName", "test@email.com", true, "DFW", domain, password)

        when:
        cloud20.createUser(identityAdminToken, user)
        def authResponse = cloud11.adminAuthenticate(v1Factory.createPasswordCredentials(username, password))

        def getUser20Response = cloud20.getUserByName(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User)
        cloud11.deleteUser(username)
        cloud20.hardDeleteUser(serviceAdminToken, userEntity.id)

        then:
        authResponse.status == 200
    }

    def "CRUD user v1.1"() {
        given:
        String username = "userIT" + sharedRandom
        User user = v1Factory.createUser(username, "1234567890", randomMosso, null, true)
        User userForUpdate = v1Factory.createUser(username, null, randomMosso+1, "someNastId1", true)

        when:
        def userCreateResponse = cloud11.createUser(user)
        def getUserResponse = cloud11.getUserByName(username)
        def getUser20Response = cloud20.getUserByName(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User)

        def updateUserResponse = cloud11.updateUser(username, userForUpdate)
        def deleteUserResponse = cloud11.deleteUser(username)
        def hardDeleteUserResponse = cloud20.hardDeleteUser(serviceAdminToken, userEntity.id)
        def deleteTenantResponse = cloud20.deleteTenant(serviceAdminToken, String.valueOf(randomMosso))

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
        def userCreateResponse = cloud11.createUser(user)
        def getUser20Response = cloud20.getUserByName(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User)
        user.id = "userExistingMosso2"
        def userCreateNewNameResponse = cloud11.createUser(user)

        def deleteUserResponse = cloud11.deleteUser(username)
        def hardDeleteUserResponse = cloud20.hardDeleteUser(serviceAdminToken, userEntity.id)
        def deleteTenantResponse = cloud20.deleteTenant(serviceAdminToken, String.valueOf(randomMosso))

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
        def updateUser = cloud11.updateUser(identityAdmin.username, userForUpdate)
        cloud11.createUser(user)
        cloud11.authUser = identityAdmin.username
        cloud11.authPassword = "Password1"
        def getUser = cloud11.getUserByName(user.id)
        def createUser = cloud11.createUser(userForCreate)
        cloud11.authUser = adminUser
        cloud11.authPassword = adminPassword
        cloud11.setUserEnabled(identityAdmin.username, v1Factory.createUserWithOnlyEnabled(true))
        identityAdminToken = cloud20.authenticate(identityAdmin.username, "Password1").getEntity(AuthenticateResponse).value.token.id

        then:
        updateUser.status == 200
        getUser.status == 403
        createUser.status == 403
    }

    def "authenticate and verify token entropy"() {
        given:
        def username = "userTestEntroy$sharedRandom"
        def key = "$sharedRandom"
        def credential = v1Factory.createApiKeyCredentials(username, key)

        def user = v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", sharedRandom, "Password1")
        def userId = cloud20.createUser(identityAdminToken, user).getEntity(org.openstack.docs.identity.api.v2.User).id
        cloud20.addApiKeyToUser(serviceAdminToken, userId, credential)

        when:
        def startTime = new DateTime()
        def expOne = authAndExpire(username, key)
        def expTwo = authAndExpire(username, key)
        def expThree = authAndExpire(username, key)
        def endTime = new DateTime()

        def range = getRange(defaultExpirationSeconds, startTime, endTime)
        cloud20.hardDeleteUser(serviceAdminToken, userId)

        then:
        expOne <= range.get("max")
        expOne >= range.get("min")
        expTwo <= range.get("max")
        expTwo >= range.get("min")
        expThree <= range.get("max")
        expThree >= range.get("min")
    }

    def authAndExpire(username, key) {
        def token = cloud11.authenticate(v1Factory.createUserKeyCredentials(username, key)).getEntity(AuthData).token
        cloud11.revokeToken(token.id)
        return token.expires.toGregorianCalendar().getTime()
    }
}
