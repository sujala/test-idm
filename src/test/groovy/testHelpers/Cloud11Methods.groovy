package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.Base64
import spock.lang.Shared
import spock.lang.Specification

import static javax.ws.rs.core.MediaType.APPLICATION_XML

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 6/27/13
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
class Cloud11Methods {
    @Shared WebResource resource

    @Shared String path11 = "cloud/v1.1/"

    @Shared String authUser = "auth"
    @Shared String authPassword = "auth123"

    def adminAuthenticate(credentials) {
        resource.path(path11).path("auth-admin").header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def authenticate(credentials) {
        resource.path(path11).path("auth").header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def revokeToken(String token) {
        resource.path(path11).path("token").path(token).header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).delete(ClientResponse)
    }

    def getUserByName(String username) {
        resource.path(path11).path("users").path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createUser(user) {
        resource.path(path11).path("users").header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def updateUser(String username, user) {
        resource.path(path11).path("users").path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def deleteUser(String username) {
        resource.path(path11).path("users").path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def setUserEnabled(String username, user) {
        resource.path(path11).path("users").path(username).path("enabled").header("Authorization", "Basic " + new String(baseEncoding(authUser,authPassword))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def baseEncoding(String username, String password) {
        return Base64.encode(username + ":" + password);
    }
}
