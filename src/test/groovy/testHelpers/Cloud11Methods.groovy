package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.Base64
import spock.lang.Shared
import spock.lang.Specification

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
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

    def init(){
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")
    }

    def adminAuthenticate(credentials) {
        resource.path(path11).path("auth-admin").header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def authenticate(credentials) {
        resource.path(path11).path("auth").accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def validateToken(String token) {
        resource.path(path11).path("token").path(token).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def revokeToken(String token) {
        resource.path(path11).path("token").path(token).header("Authorization", getBasicAuth()).delete(ClientResponse)
    }

    def getUserByName(String username) {
        resource.path(path11).path("users").path(username).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getServiceCatalog(String username) {
        resource.path(path11).path("users").path(username).path("serviceCatalog").header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createUser(user) {
        resource.path(path11).path("users").header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def updateUser(String username, user) {
        resource.path(path11).path("users").path(username).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def deleteUser(String username) {
        resource.path(path11).path("users").path(username).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def setUserEnabled(String username, user) {
        resource.path(path11).path("users").path(username).path("enabled").header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def setUserKey(String username, user) {
        resource.path(path11).path("users").path(username).path("key").header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def addBaseUrl(baseUrl) {
        resource.path(path11).path("baseURLs").header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(baseUrl).post(ClientResponse)
    }

    def addBaseUrlRefs(String username, baseUrlRefs) {
        resource.path(path11).path("users").path(username).path("baseURLRefs").header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(baseUrlRefs).post(ClientResponse)
    }

    def deleteBaseUrlRefs(String username, String baseUrlRefsId) {
        resource.path(path11).path("users").path(username).path("baseURLRefs").path(baseUrlRefsId).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def getToken(token) {
        resource.path(path11).path("token").path(token).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    private String getBasicAuth() {
        "Basic " + new String(baseEncoding(authUser, authPassword))
    }

    def baseEncoding(String username, String password) {
        return Base64.encode(username + ":" + password);
    }
}
