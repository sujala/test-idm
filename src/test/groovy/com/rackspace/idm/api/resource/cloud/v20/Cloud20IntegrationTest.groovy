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

    def randomness = UUID.randomUUID()
    static def X_AUTH_TOKEN="X-Auth-Token"

    def setupSpec() {
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml");
        serviceAdminToken = authenticate("authQE","Auth1234").getEntity(AuthenticateResponse).value.token.id
    }

    def cleanupSpec() {
    }

    def 'User CRUD'(){
        when:
        //Create user
        def randmon = ("$randomness").replace('-',"")
        def user = identityAdminUser("bob"+randmon,"test@rackspace.com", true, "Password1","ORD")
        def response = createUser(serviceAdminToken, user)
        //Get user
        def getUserResponse = getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User)
        //Update User
        def userForUpdate = userForUpdate("updatedBob"+randmon, "Bob", "test@rackspace.com", true, null, null)
        def updateUserResponse = updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = deleteUser(serviceAdminToken, userEntity.getId())
        //Hard delete user
        def hardDeleteResponses = hardDeleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponses.status == 204
    }

    def "operations on non-existent users return 'not found'" () {
        expect:
        response.status == 404

        where:
        response << [
                getUserById(serviceAdminToken, "badId"),
                updateUser(serviceAdminToken, "badId", new User()),
                deleteUser(serviceAdminToken, "badId")
        ]
    }



    def createUser(String token, user){
        resource.path(path).path('users').header(X_AUTH_TOKEN,token).entity(user).post(ClientResponse)
    }

    def getUser(String token, URI location){
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN,token).get(ClientResponse)
    }

    def getUserById(String token, String userId){
        resource.path(path).path('users').path(userId).accept(APPLICATION_XML).header(X_AUTH_TOKEN,token).get(ClientResponse)
    }

    def updateUser(String token, String userId, user){
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN,token).entity(user).post(ClientResponse)
    }

    def deleteUser(String token, String userId){
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN,token).delete(ClientResponse)
    }

    def hardDeleteUser(String token, String userId){
        resource.path(path).path('softDeleted').path('users').path(userId).header(X_AUTH_TOKEN,token).delete(ClientResponse)
    }

    def identityAdminUser(String username, String email, Boolean enabled, String password, String defaultRegion){
        new User().with {
            it.username = username
            it.email = email
            it.enabled = enabled
            it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"),password)
            it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","defaultRegion"),defaultRegion)
            return it
        }
    }

    def authenticate(username, password){
        resource.path(path + 'tokens').accept(APPLICATION_XML).entity(authenticateRequest(username,password)).post(ClientResponse)
    }

    def getCredentials(String username, String password){
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def authenticateRequest(String username, String password){
        def credentials = getCredentials(username, password)

        def objectFactory = new ObjectFactory()

        new AuthenticationRequest().with{
            it.setCredential(objectFactory.createPasswordCredentials(credentials))
            return it
        }
    }

    def userForUpdate(String username,String displayName, String email, Boolean enabled, String defaultRegion, String password ){
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

}