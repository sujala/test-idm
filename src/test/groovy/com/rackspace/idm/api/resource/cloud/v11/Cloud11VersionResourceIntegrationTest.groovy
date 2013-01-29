package com.rackspace.idm.api.resource.cloud.v11

import com.rackspacecloud.docs.auth.api.v1.User
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.Base64
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.namespace.QName

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/25/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class Cloud11VersionResourceIntegrationTest extends Specification{

    @Shared WebResource resource
    @Shared def path11 = "cloud/v1.1/"
    @Shared def path20 = "cloud/v2.0/"
    @Shared def serviceAdmin
    @Shared def serviceAdminToken
    @Shared org.openstack.docs.identity.api.v2.User identityAdmin
    @Shared def identityAdminToken
    @Shared authUser
    @Shared authPassword
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared Integer randomMosso

    static def X_AUTH_TOKEN = "X-Auth-Token"

    def setupSpec(){
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml");

        sharedRandom = ("$randomness").replace('-',"")

        serviceAdminToken = authenticate20XML("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = getUserByName20XML(serviceAdminToken, "authQE").getEntity(org.openstack.docs.identity.api.v2.User)

        String adminUsername = "identityAdmin" + sharedRandom
        createUser20XML(serviceAdminToken, userForCreate20(adminUsername, "adminUser", "someEmail@rackspace.com", true, "ORD", null, "Password1"))
        identityAdmin = getUserByName20XML(serviceAdminToken, adminUsername).getEntity(org.openstack.docs.identity.api.v2.User)
        identityAdminToken = authenticate20XML(adminUsername, "Password1").getEntity(AuthenticateResponse).value.token.id
    }

    def cleanupSpec() {
        deleteUserXML(identityAdmin.username)
    }

    def setup(){
        Random random = new Random()
        randomMosso = 100000 + random.nextInt(1000)
        authUser = "auth"
        authPassword = "auth123"
    }

    def "CRUD user v1.1" () {
        given:
        String username = "userIT" + sharedRandom
        User user = createUser(username, "1234567890", randomMosso, null, true)
        User userForUpdate = createUser(username, null, randomMosso+1, "someNastId1", true)

        when:
        def userCreateResponse = createUserXML(user)
        def getUserResponse = getUserXML(username)
        def updateUserResponse = updateUserXML(username, userForUpdate)
        def deleteUserResponse = deleteUserXML(username)

        then:
        userCreateResponse.status == 201
        User createUser = userCreateResponse.getEntity(User)
        createUser.key == "1234567890"
        createUser.nastId != null
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
    }

    def "Don't allow disabled identity/service admin to get/create user"(){
        given:
        User userForUpdate = createUser(identityAdmin.username, null, null, null, false)
        String username = "userAdmin" + sharedRandom
        User user = createUser(username, "1234567890", randomMosso, null, true)
        String username2 = "userAdmin2" + sharedRandom
        User userForCreate = createUser(username2, "1234567890", randomMosso+1, null, true)

        when:
        def updateUser = updateUserXML(identityAdmin.username, userForUpdate)
        createUserXML(user)
        authUser = identityAdmin.username
        authPassword = "Password1"
        def getUser = getUserXML(user.id)
        def createUser = createUserXML(userForCreate)

        then:
        updateUser.status == 200
        getUser.status == 403
        createUser.status == 403
    }

    @Ignore
    def "Identity admin should not be allowed to delete himself" () {
        given:
        User userForUpdate = createUser(identityAdmin.username, null, null, null, true)

        when:
        def updateUser = updateUserXML(identityAdmin.username, userForUpdate)
        authUser = identityAdmin.username
        authPassword = "Password1"
        def deleteAdminUser = deleteUserXML(identityAdmin.username)

        then:
        updateUser.status == 200
        deleteAdminUser.status == 403
    }

    def createUser(String id, String key, Integer mossoId, String nastId, Boolean enabled) {
        new User().with {
            it.id = id
            it.key = key
            it.mossoId = mossoId
            it.nastId = nastId
            it.enabled = enabled
            return it
        }
    }

    //Resource Calls v1.1
    def createUserXML(user) {
        resource.path(path11).path('users').header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def getUserXML(String username) {
        resource.path(path11).path('users').path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateUserXML(String username, user) {
        resource.path(path11).path('users').path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def deleteUserXML(String username) {
        resource.path(path11).path('users').path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).delete(ClientResponse)
    }

    //Resource Calls v2.0

    def authenticate20XML(username, password) {
        resource.path(path20 + 'tokens').accept(APPLICATION_XML).entity(authenticateRequest(username, password)).post(ClientResponse)
    }

    def getUserByName20XML(String token, String name) {
        resource.path(path20).path('users').queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def createUser20XML(String token, user) {
        resource.path(path20).path('users').header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def userForCreate20(String username, String displayName, String email, Boolean enabled, String defaultRegion, String domainId, String password) {
        new org.openstack.docs.identity.api.v2.User().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            if (defaultRegion != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), defaultRegion)
            }
            if (domainId != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "domainId"), domainId)
            }
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
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

    def getCredentials(String username, String password) {
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def baseEncoding(String username, String password){
        return Base64.encode(username + ":" + password);
    }

}
