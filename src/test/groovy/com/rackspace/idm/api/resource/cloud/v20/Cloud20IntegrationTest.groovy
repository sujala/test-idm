package com.rackspace.idm.api.resource.cloud.v20;


import com.sun.jersey.api.client.ClientResponse
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.namespace.QName

import org.openstack.docs.identity.api.v2.*

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import com.sun.jersey.api.client.WebResource

class Cloud20IntegrationTest extends Specification {
    @Shared WebResource resource

    @Shared def path = "cloud/v2.0/"
    @Shared def serviceAdminToken
    @Shared def identityAdminToken
    @Shared def userAdminToken
    @Shared def defaultUserToken

    @Shared def userAdmin
    @Shared def defaultUser

    def randomness = UUID.randomUUID()
    static def X_AUTH_TOKEN = "X-Auth-Token"

    def setupSpec() {
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml");
        serviceAdminToken = authenticate("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        identityAdminToken = authenticate("auth", "auth123").getEntity(AuthenticateResponse).value.token.id
//        userAdmin = getUserByName(identityAdminToken, "testUserAdmin_doNotDeleteMe").getEntity(User)
//        if (userAdmin == null) {
//            userAdmin = createUser(identityAdminToken, userForCreate("testUserAdmin_doNotDeleteMe", "display", "test@rackspace.com", true, "ORD", "domainId", "Password1"))
//        }
//        userAdminToken = authenticate("auth", "auth123").getEntity(AuthenticateResponse).value.token.id
//        defaultUser = getUserByName(userAdminToken, "testDefaultUser_doNotDeleteMe").getEntity(User)
//        if( defaultUser == null){
//            defaultUser = createUser(userAdminToken,  userForCreate("testDefaultUser_doNotDeleteMe", "display", "test@rackspace.com", true, null, null, "Password1"))
//        }
    }

    def cleanupSpec() {
    }

    def 'User CRUD'() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = userForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = createUser(serviceAdminToken, user)
        //Get user
        def getUserResponse = getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User)
        //Get user by id
        def getUserByIdResponse = getUserById(serviceAdminToken, userEntity.getId())
        def getUserByNameResponse = getUserByName(serviceAdminToken, userEntity.getUsername())
        //Update User
        def userForUpdate = userForUpdate("updatedBob" + random, "Bob", "test@rackspace.com", true, null, null)
        def updateUserResponse = updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = deleteUser(serviceAdminToken, userEntity.getId())
        //Hard delete user
        def hardDeleteResponses = hardDeleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        getUserByIdResponse.status == 200
        getUserByNameResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponses.status == 204
    }

    def "operations on non-existent users return 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                getUserById(serviceAdminToken, "badId"),
                getUserByName(serviceAdminToken, "badName"),
                updateUser(serviceAdminToken, "badId", new User()),
                deleteUser(serviceAdminToken, "badId")
        ]
    }

    def "invalid operations on create user returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createUser(serviceAdminToken, userForCreate("!@#What", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("1one", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("one name", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate(null, "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "junk!@#", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Pop1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "longpassword1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Longpassword")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", "someId", "Longpassword")),
                createUser(identityAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Longpassword1"))
        ]
    }

    def 'operations with invalid tokens'(){
        expect:
        response.status == 401

        where:
        response << [
                createUser("invalidToken", userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(null, userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                getUserById("invalidToken", "badId"),
                getUserById(null, "badId"),
                getUserByName("invalidToken", "badId"),
                getUserByName(null, "badId"),
                updateUser("invalidToken", "badId", new User()),
                updateUser(null, "badId", new User()),
                deleteUser("invalidToken", "badId"),
                deleteUser(null, "badId"),
                listUser("invalidToken"),
                listUser(null)
        ]

    }

    def 'valid operations on list users'(){
        expect:
        response.status == 200

        where:
        response << [
                listUser(serviceAdminToken),
                listUser(identityAdminToken)
        ]

    }



    def createUser(String token, user) {
        resource.path(path).path('users').header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def getUser(String token, URI location) {
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listUser(String token){
        resource.path(path).path('users').accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUserById(String token, String userId) {
        resource.path(path).path('users').path(userId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUserByName(String token, String name) {
        resource.path(path).path('users').queryParam("name",name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateUser(String token, String userId, user) {
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def deleteUser(String token, String userId) {
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def hardDeleteUser(String token, String userId) {
        resource.path(path).path('softDeleted').path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def authenticate(username, password) {
        resource.path(path + 'tokens').accept(APPLICATION_XML).entity(authenticateRequest(username, password)).post(ClientResponse)
    }

    def getCredentials(String username, String password) {
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def authenticateRequest(String username, String password) {
        def credentials = getCredentials(username, password)

        def objectFactory = new ObjectFactory()

        new AuthenticationRequest().with {
            it.setCredential(objectFactory.createPasswordCredentials(credentials))
            return it
        }
    }

    def userForUpdate(String username, String displayName, String email, Boolean enabled, String defaultRegion, String password) {
        new User().with {
            it.username = username
            it.email = email
            it.enabled = enabled
            it.displayName = displayName
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
            if (defaultRegion != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), defaultRegion)
            }
            return it
        }
    }

    def userForCreate(String username, String displayName, String email, Boolean enabled, String defaultRegion, String domainId, String password) {
        new User().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            if (defaultRegion != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), defaultRegion)
            }
            if (domainId != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","domainId"), domainId)
            }
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
            return it
        }
    }

}