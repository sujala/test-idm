package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspacecloud.docs.auth.api.v1.AuthData
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault
import com.rackspacecloud.docs.auth.api.v1.User
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenant
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

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
    @Shared def validServiceName = "cloudServers"

    def setupSpec(){
        sharedRandom = ("$randomness").replace('-',"")

        serviceAdminToken = cloud20.authenticate("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = cloud20.getUserByName(serviceAdminToken, "authQE").getEntity(org.openstack.docs.identity.api.v2.User).value

        String adminUsername = "identityAdmin" + sharedRandom
        cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(adminUsername, "adminUser", "someEmail@rackspace.com", true, "ORD", null, "Password1"))
        identityAdmin = cloud20.getUserByName(serviceAdminToken, adminUsername).getEntity(org.openstack.docs.identity.api.v2.User).value
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

    def "Admin authenticate with password credentials returns 200"() {
        given:
        String username = "auth" + sharedRandom
        String password = "Password1"
        String domain = "domain" + sharedRandom
        def user = v2Factory.createUserForCreate(username, "displayName", "test@email.com", true, "DFW", domain, password)

        when:
        cloud20.createUser(identityAdminToken, user)
        def authResponse = cloud11.adminAuthenticate(v1Factory.createPasswordCredentials(username, password))

        def getUser20Response = cloud20.getUserByName(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User).value

        then:
        authResponse.status == 200

        cleanup:
        cloud11.deleteUser(username)
    }

    def "user authenticate success"() {
        given:
        def username = "userAuth11$sharedRandom"
        String domain = "userAuth11$sharedRandom"
        def key = "$sharedRandom"
        def credential = v1Factory.createApiKeyCredentials(username, key)

        def user = v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", domain, "Password1")
        def userId = cloud20.createUser(identityAdminToken, user).getEntity(org.openstack.docs.identity.api.v2.User).value.id
        cloud20.addApiKeyToUser(serviceAdminToken, userId, credential)

        when:
        def authData = utils11.authenticateWithKey(username, key)

        then:
        authData.token != null

        cleanup:
        cloud11.deleteUser(username)
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
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User).value
        def updateUserResponse = cloud11.updateUser(username, userForUpdate)


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
        updateUser.created != null
        updateUser.updated != null

        cleanup:
        cloud11.deleteUser(username)
        cloud20.deleteTenant(serviceAdminToken, String.valueOf(randomMosso))
    }

    def "Create User with existing mosso should return a 400"() {
        given:
        String username = "userExistingMosso" + sharedRandom
        User user = v1Factory.createUser(username, "1234567890", randomMosso, null, true)

        when:
        def userCreateResponse = cloud11.createUser(user)
        def getUser20Response = cloud20.getUserByName(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User).value
        user.id = "userExistingMosso2"
        def userCreateNewNameResponse = cloud11.createUser(user)



        then:
        userCreateResponse.status == 201
        User createUser = userCreateResponse.getEntity(User)
        createUser.key == "1234567890"
        createUser.mossoId == randomMosso
        createUser.enabled == true

        userCreateNewNameResponse.status == 400

        cleanup:
        cloud11.deleteUser(username)
        cloud20.deleteTenant(serviceAdminToken, String.valueOf(randomMosso))
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
        def userId = cloud20.createUser(utils.getIdentityAdminToken(), user).getEntity(org.openstack.docs.identity.api.v2.User).value.id
        cloud20.addApiKeyToUser(utils.getServiceAdminToken(), userId, credential)

        when:
        def startTime = new DateTime()
        def expOne = authAndExpire(username, key)
        def expTwo = authAndExpire(username, key)
        def expThree = authAndExpire(username, key)
        def endTime = new DateTime()
        def range = getRange(defaultExpirationSeconds, startTime, endTime)

        then:
        expOne <= range.get("max")
        expOne >= range.get("min")
        expTwo <= range.get("max")
        expTwo >= range.get("min")
        expThree <= range.get("max")
        expThree >= range.get("min")

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), userId)
    }

    def "Allow baseUrls to be assigned to negative tenants" () {
        given:
        def username = "testNegativeTenantUser$sharedRandom"
        def mossoId = -1 * getRandomNumber(1000000, 2000000);
        def baseURLId = getRandomNumber(1000000, 2000000)
        User user = v1Factory.createUser(username, "1234567890", mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", null, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)


        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def baseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        def getUser = cloud20.getUserByName(serviceAdminToken, username).getEntity(org.openstack.docs.identity.api.v2.User).value

        then:
        createdUser != null
        createdUser.mossoId == mossoId
        baseUrlResponse.status == 201
        baseUrlRefResponse.status == 201 //should be a 200

        cleanup:
        cloud20.destroyUser(serviceAdminToken, getUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
        cloud20.deleteDomain(serviceAdminToken, createdUser.mossoId.toString())
    }

    def "Add/Remove baseUrlRef from a user" () {
       given:
        def username = "addRemoveBaseUrlRefUser$sharedRandom"
        def mossoId = -1 * getRandomNumber(1000000, 2000000);
        def baseURLId = getRandomNumber(1000000, 2000000)
        User user = v1Factory.createUser(username, "1234567890", mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", null, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)


        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def addBaseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        def deleteBaseUrlRefResponse = cloud11.deleteBaseUrlRefs(username, baseUrlRef.id.toString())

        def getUser = cloud20.getUserByName(serviceAdminToken, username).getEntity(org.openstack.docs.identity.api.v2.User).value

        then:
        createdUser != null
        createdUser.mossoId == mossoId
        baseUrlResponse.status == 201
        addBaseUrlRefResponse.status == 201 //should be a 200
        deleteBaseUrlRefResponse.status == 204

        cleanup:
        cloud20.destroyUser(serviceAdminToken, getUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
        cloud20.deleteDomain(serviceAdminToken, createdUser.mossoId.toString())
    }

    @Unroll
    def "verify v1.1 add base URL feature flag works: exposeEndpoint = #exposeEndpoint" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.EXPOSE_V11_ADD_BASE_URL_PROP, exposeEndpoint)
        def baseURLId = testUtils.getRandomInteger()
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", null, null)

        when:
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)

        then:
        baseUrlResponse.status == status

        cleanup:
        if(exposeEndpoint) {
            cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
        }
        staticIdmConfiguration.reset()

        where:
        exposeEndpoint | status
        true           | 201
        false          | 404
    }

    def "auth-admin call should not display user's admin urls - password credentials" () {
        given:
        String username = "adminBaseUrl$sharedRandom"
        String password = "Password1"
        String adminUrl = "http://adminUrl"
        def baseURLId = getRandomNumber(1000000, 2000000)
        def mossoId = getRandomNumber(1000000, 2000000);
        User user = v1Factory.createUser(username, "1234567890", mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", adminUrl, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def addBaseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        def getUser = cloud20.getUserByName(serviceAdminToken, username).getEntity(org.openstack.docs.identity.api.v2.User).value
        def passwordCred = v2Factory.createPasswordCredentialsBase(username, password)
        cloud20.addCredential(serviceAdminToken, getUser.id, passwordCred)
        def cred = v1Factory.createPasswordCredentials(username, password)
        AuthData authData = cloud11.adminAuthenticate(cred).getEntity(AuthData)


        then:
        baseUrlResponse.status == 201
        addBaseUrlRefResponse.status == 201
        authData != null
        Integer index = authData.serviceCatalog.service.name.indexOf(validServiceName)
        authData.serviceCatalog.service[index].endpoint.adminURL[0] == null

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
    }

    def "auth-admin call should specify password authentication failed in error message if password is used"() {
        when:
        def createdUser = utils11.createUser()
        utils.addCredentialToUser(createdUser.id)
        def cred = v1Factory.createPasswordCredentials(createdUser.id, "invalid")
        def response = cloud11.adminAuthenticate(cred)

        then:
        response != null
        response.getStatus() == 401
        def authFault = response.getEntity(UnauthorizedFault)
        authFault != null
        !authFault.message.contains("api key")
        authFault.message.contains("credentials")

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
    }

    def "auth-admin call should specify api key authentication failed in error message if api key is used" () {
        given:
        def mossoId = testUtils.getRandomInteger()

        when:
        def createdUser = utils11.createUser()
        def cred = v1Factory.createMossoCredentials(mossoId, "invalidKey")
        def response = cloud11.adminAuthenticate(cred)

        then:
        response != null
        response.getStatus() == 401
        def authFault = response.getEntity(UnauthorizedFault)
        authFault != null
        authFault.message.contains("api key")
        !authFault.message.contains("credentials")

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
    }

    def "auth call should not display admin urls in service catalog - passwordCred" () {
        given:
        String username = "userAdminBaseUrlPwd$sharedRandom"
        String password = "Password1"
        String adminUrl = "http://adminUrl"
        def baseURLId = getRandomNumber(1000000, 2000000)
        def mossoId = getRandomNumber(1000000, 2000000);
        User user = v1Factory.createUser(username, "1234567890", mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", adminUrl, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def addBaseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        def getUser = cloud20.getUserByName(serviceAdminToken, username).getEntity(org.openstack.docs.identity.api.v2.User).value
        def passwordCred = v2Factory.createPasswordCredentialsBase(username, password)
        cloud20.addCredential(serviceAdminToken, getUser.id, passwordCred)
        def cred = v1Factory.createPasswordCredentials(username, password)
        def authResponse = cloud11.authenticate(cred)


        then:
        baseUrlResponse.status == 201
        addBaseUrlRefResponse.status == 201
        authResponse.status == 302

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
    }

    def "auth call should not display admin urls in service catalog - userKeyCredentials" () {
        given:
        String username = "userAdminBaseUrlKey$sharedRandom"
        String adminUrl = "http://adminUrl"
        def baseURLId = getRandomNumber(1000000, 2000000)
        def mossoId = getRandomNumber(1000000, 2000000);
        def key = "1234567890"
        User user = v1Factory.createUser(username, key, mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", adminUrl, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def addBaseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        def cred = v1Factory.createUserKeyCredentials(username, key)
        def authData = cloud11.authenticate(cred).getEntity(AuthData)


        then:
        baseUrlResponse.status == 201
        addBaseUrlRefResponse.status == 201
        authData != null
        Integer index = authData.serviceCatalog.service.name.indexOf(validServiceName)
        authData.serviceCatalog.service[index].endpoint.adminURL[0] == null

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
    }

    def "auth call should not display admin urls in service catalog - mossoCredentials" () {
        given:
        String username = "userAdminBaseUrlMosso$sharedRandom"
        String adminUrl = "http://adminUrl"
        def baseURLId = getRandomNumber(1000000, 2000000)
        def mossoId = getRandomNumber(1000000, 2000000);
        def key = "1234567890"
        User user = v1Factory.createUser(username, key, mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", adminUrl, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def addBaseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        def cred = v1Factory.createMossoCredentials(mossoId, key)
        def authResponse = cloud11.authenticate(cred)


        then:
        baseUrlResponse.status == 201
        addBaseUrlRefResponse.status == 201
        authResponse.status == 302

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
    }

    def "auth call should not display admin urls in service catalog - nastCredentials" () {
        given:
        String username = "userAdminBaseUrlNast$sharedRandom"
        String adminUrl = "http://adminUrl"
        def baseURLId = getRandomNumber(1000000, 2000000)
        def mossoId = getRandomNumber(1000000, 2000000);
        def key = "1234567890"
        User user = v1Factory.createUser(username, key, mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", adminUrl, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def addBaseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        def cred = v1Factory.createNastCredentials(createdUser.nastId, key)
        def authResponse = cloud11.authenticate(cred)


        then:
        baseUrlResponse.status == 201
        addBaseUrlRefResponse.status == 201
        authResponse.status == 302

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
    }

    def "auth call not should display admin urls in service catalog for admin user - userKeyCredentials" () {
        given:
        String adminUrl = "http://adminUrl"
        String key = "1234567890"
        def mossoId = -1 * getRandomNumber(1000000, 2000000);
        def baseURLId = getRandomNumber(1000000, 2000000)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", adminUrl, null)
        Tenant tenant = v2Factory.createTenant(mossoId.toString(), mossoId.toString())
        def role = v2Factory.createRole("listUsersByTenantRole$randomness").with {
            it.assignment = RoleAssignmentEnum.TENANT
            it
        }

        when:
        def addTenant = cloud20.addTenant(identityAdminToken, tenant).getEntity(Tenant).value
        def response = cloud20.createRole(identityAdminToken, role)
        def createRole = response.getEntity(Role).value
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def endpointTemplate = new EndpointTemplate().with {
            it.id = baseURLId
            it
        }
        cloud20.addEndpoint(serviceAdminToken, mossoId.toString(), endpointTemplate)
        cloud20.addRoleToUserOnTenant(serviceAdminToken, addTenant.id, identityAdmin.id, createRole.id)
        def addKey = cloud11.setUserKey(identityAdmin.username, v1Factory.createUserWithOnlyKey(key))
        def cred = v1Factory.createUserKeyCredentials(identityAdmin.username, key)
        def authData = cloud11.authenticate(cred).getEntity(AuthData)

        then:
        addKey.status == 200
        baseUrlResponse.status == 201
        authData != null
        Integer index = authData.serviceCatalog.service.name.indexOf(validServiceName)
        authData.serviceCatalog.service[index].endpoint.adminURL[0] == null

        cleanup:
        try {
            cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), addTenant.id, identityAdmin.id, createRole.id)
            cloud20.deleteTenant(serviceAdminToken, addTenant.id)
            cloud20.deleteRole(serviceAdminToken, createRole.id)
            cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
        } catch (Exception ex) {
            // Eat any exception during cleanup as it may hide
        }
    }

    def "Get service catalog for user should not display the admin urls" () {
        given:
        String username = "userServiceCatalog$sharedRandom"
        String adminUrl = "http://adminUrl"
        def baseURLId = getRandomNumber(1000000, 2000000)
        def mossoId = getRandomNumber(1000000, 2000000);
        def key = "1234567890"
        User user = v1Factory.createUser(username, key, mossoId, null, true)
        def baseUrl = v1Factory.createBaseUrl(baseURLId, validServiceName, "ORD", true, false, "http:publicUrl", adminUrl, null)
        def baseUrlRef = v1Factory.createBaseUrlRef(baseURLId, null, false)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def baseUrlResponse = cloud11.addBaseUrl(baseUrl)
        def addBaseUrlRefResponse = cloud11.addBaseUrlRefs(username, baseUrlRef)
        ServiceCatalog userServiceCatalogResponse = cloud11.getServiceCatalog(username).getEntity(ServiceCatalog)


        then:
        baseUrlResponse.status == 201
        addBaseUrlRefResponse.status == 201
        userServiceCatalogResponse != null
        for(def adminUrls : userServiceCatalogResponse.service.endpoint.adminURL){
            for(String url : adminUrls){
                url == null
            }
        }

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
        cloud20.deleteEndpointTemplate(serviceAdminToken, baseURLId.toString())
    }

    def "Creating user without apikey will generate an apikey" () {
        given:
        String username = "userNoApiKey$sharedRandom"
        def mossoId = getRandomNumber(1000000, 2000000);
        User user = v1Factory.createUser(username, null, mossoId, null, true)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def getUserResponse = cloud20.getUserByName(utils.getIdentityAdminToken(), createdUser.id)
        def userEntity = getUserResponse.getEntity(org.openstack.docs.identity.api.v2.User).value
        def listCred = cloud20.listCredentials(utils.getServiceAdminToken(), userEntity.id).getEntity(String)
        def authResponse = cloud20.authenticateApiKey(createdUser.id, createdUser.key)

        then:
        createdUser != null
        createdUser.key != null
        listCred.contains(JSONConstants.API_KEY_CREDENTIALS)
        authResponse.status == 200

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
    }

    def "Creating user with apikey will use provided apikey" () {
        given:
        String username = "userApiKeyUuid$sharedRandom"
        def mossoId = getRandomNumber(1000000, 2000000);
        def apiKey = "1234567890"
        User user = v1Factory.createUser(username, apiKey, mossoId, null, true)

        when:
        def createdUser = cloud11.createUser(user).getEntity(User)
        def getUserResponse = cloud20.getUserByName(utils.getIdentityAdminToken(), createdUser.id)
        def userEntity = getUserResponse.getEntity(org.openstack.docs.identity.api.v2.User).value
        def listCred = cloud20.listCredentials(utils.getServiceAdminToken(), userEntity.id).getEntity(String)
        def authResponse = cloud20.authenticateApiKey(createdUser.id, createdUser.key)

        then:
        createdUser != null
        listCred.contains(JSONConstants.API_KEY_CREDENTIALS)
        createdUser.key == apiKey
        authResponse.status == 200


        cleanup: "Clean up data"
        cloud20.destroyUser(serviceAdminToken, createdUser.id)
        cloud20.deleteTenant(serviceAdminToken, createdUser.mossoId.toString())
        cloud20.deleteTenant(serviceAdminToken, createdUser.nastId)
    }

    def "revoke token should disable a users token"() {
        given:
        def username = "user" + sharedRandom
        def apiKey = "1234567890"
        User user = v1Factory.createUser(username, apiKey, randomMosso, null, true)

        when:
        def userCreateResponse = cloud11.createUser(user)
        assert (userCreateResponse.status == 201)

        def authResponse = cloud11.authenticate(v1Factory.createUserKeyCredentials(username, apiKey))
        assert (authResponse.status == 200)

        def token = authResponse.getEntity(AuthData).token.id
        def validateResponse = cloud11.validateToken(token)
        assert (validateResponse.status == 200)

        def revokeResponse = cloud11.revokeToken(token)
        assert (revokeResponse.status == 204)

        def getTokenResponse = cloud11.getToken(token)
        def failedValidateResponse = cloud11.validateToken(token)

        then:
        getTokenResponse.status == 404
        failedValidateResponse.status == 404

        cleanup:
        cloud11.deleteUser(username)
    }

    def "Create user with blank nastIds" () {
        given:
        String username = "nullNastUser$sharedRandom"
        def mossoId = getRandomNumber(1000000, 2000000);
        def key = "1234567890"
        User user = v1Factory.createUser(username, key, mossoId, nastId , true)

        when:
        def createdUserResponse = cloud11.createUser(user)
        def createdUser = createdUserResponse.getEntity(User)
        def userEntity = cloud20.getUserByName(serviceAdminToken, createdUser.id).getEntity(org.openstack.docs.identity.api.v2.User).value

        then:
        createdUserResponse.status  == status
        createdUser.nastId != null
        createdUser.nastId != nastId

        cleanup:
        try { cloud20.destroyUser(serviceAdminToken, userEntity.id) } catch (Exception e) {}

        where:
        nastId | status
        null   | 201
        ""     | 201
        "  "   | 201
    }

    def "update user's nastId - blanks" () {
        given:
        String username = "spacesNastUpdateUser$sharedRandom"
        def mossoId = getRandomNumber(1000000, 2000000);
        def key = "1234567890"
        User user = v1Factory.createUser(username, key, mossoId, null , true)

        when:
        def createdUserResponse = cloud11.createUser(user)
        def createdUser = createdUserResponse.getEntity(User)
        user.nastId = nastId
        def updateUserResponse = cloud11.updateUser(username, user)
        def updateUser = updateUserResponse.getEntity(User)
        def userEntity = cloud20.getUserByName(serviceAdminToken, createdUser.id).getEntity(org.openstack.docs.identity.api.v2.User).value

        then:
        createdUserResponse.status  == 201
        createdUser.nastId != null
        updateUserResponse.status == status
        updateUser.nastId == createdUser.nastId

        cleanup:
        cloud20.destroyUser(serviceAdminToken, userEntity.id)

        where:
        nastId | status
        ""     | 200
        "   "  | 200
        null   | 200
    }

    def authAndExpire(username, key) {
        def token = cloud11.authenticate(v1Factory.createUserKeyCredentials(username, key)).getEntity(AuthData).token
        cloud11.revokeToken(token.id)
        return token.expires.toGregorianCalendar().getTime()
    }
}
