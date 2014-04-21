package testHelpers

import com.rackspace.idm.JSONConstants
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.Base64
import org.springframework.stereotype.Component
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.JSONConstants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 6/27/13
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
class Cloud11Methods {
    @Shared WebResource resource

    @Shared String path11 = "cloud/v1.1/"

    @Shared String authUser = "auth"
    @Shared String authPassword = "auth123"

    def init(){
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")
    }

    def adminAuthenticate(credentials) {
        resource.path(path11).path(AUTH_ADMIN).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def authenticate(credentials, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE ) {
        resource.path(path11).path(AUTH).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(credentials).post(ClientResponse)
    }

    def validateToken(String token) {
        resource.path(path11).path(TOKEN).path(token).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def revokeToken(String token) {
        resource.path(path11).path(TOKEN).path(token).header("Authorization", getBasicAuth()).delete(ClientResponse)
    }

    def getUserByName(String username) {
        resource.path(path11).path(USERS).path(username).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getServiceCatalog(String username) {
        resource.path(path11).path(USERS).path(username).path(SERVICECATALOG).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createUser(user) {
        resource.path(path11).path(USERS).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def updateUser(String username, user) {
        resource.path(path11).path(USERS).path(username).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def deleteUser(String username) {
        resource.path(path11).path(USERS).path(username).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def setUserEnabled(String username, user) {
        resource.path(path11).path(USERS).path(username).path(ENABLED).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def setUserKey(String username, user) {
        resource.path(path11).path(USERS).path(username).path(KEY).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def addBaseUrl(baseUrl) {
        resource.path(path11).path(BASE_URLS).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(baseUrl).post(ClientResponse)
    }

    def addBaseUrlRefs(String username, baseUrlRefs) {
        resource.path(path11).path(USERS).path(username).path(BASE_URL_REFS).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).entity(baseUrlRefs).post(ClientResponse)
    }

    def deleteBaseUrlRefs(String username, String baseUrlRefsId) {
        resource.path(path11).path(USERS).path(username).path(BASE_URL_REFS).path(baseUrlRefsId).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def getToken(token) {
        resource.path(path11).path(TOKEN).path(token).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserEnabled(String username) {
        resource.path(path11).path(USERS).path(username).path(ENABLED).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserKey(String username) {
        resource.path(path11).path(USERS).path(username).path(KEY).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getBaseURLRefs(String username) {
        resource.path(path11).path(USERS).path(username).path(BASE_URL_REFS).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroups(String username) {
        resource.path(path11).path(USERS).path(username).path(GROUPS).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserBaseURLRef(String username, String baseUrlRefId) {
        resource.path(path11).path(USERS).path(username).path(BASE_URL_REFS).path(baseUrlRefId).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getBaseURLById(String id) {
        resource.path(path11).path(BASE_URLS).path(id).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def String getBasicAuth(username=authUser, password=authPassword) {
        "Basic " + new String(baseEncoding(username, password))
    }

    def baseEncoding(String username, String password) {
        return Base64.encode(username + ":" + password);
    }
}
