package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.Base64
import org.joda.time.DateTime
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import static javax.ws.rs.core.MediaType.APPLICATION_XML

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 6/26/13
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class RootIntegrationTest extends Specification {

    static String X_AUTH_TOKEN = "X-Auth-Token"
    @Shared String path10 = "cloud/v1.0/"
    @Shared String path11 = "cloud/v1.1/"
    @Shared String path20 = "cloud/v2.0/"

    @Shared double entropy
    @Shared int defaultExpirationSeconds

    @Shared def authUser11
    @Shared def authUserPwd11

    @Shared WebResource resource

    @Shared def v1Factory = new V1Factory()
    @Shared def v2Factory = new V2Factory()
    @Shared def entityFactory = new EntityFactory()

    def getRange(seconds) {
        HashMap<String, Date> range = new HashMap<>()
        def min = new DateTime().plusSeconds((int)Math.floor(seconds * (1 - entropy))).toDate()
        def max = new DateTime().plusSeconds((int)Math.ceil(seconds * (1 + entropy))).toDate()
        range.put("min", min)
        range.put("max", max)
        return range
    }

    def getRange(seconds, start, end) {
        HashMap<String, Date> range = new HashMap<>()
        def min = start.plusSeconds((int)Math.floor(seconds * (1 - entropy))).toDate()
        def max = end.plusSeconds((int)Math.ceil(seconds * (1 + entropy))).toDate()
        range.put("min", min)
        range.put("max", max)
        return range
    }

    def baseEncoding(String username, String password) {
        return Base64.encode(username + ":" + password);
    }

    /* Resource Methods */

    def authenticate10(String username, String apiKey) {
        resource.path(path10).header("X-Auth-User", username).header("X-Auth-Key", apiKey).get(ClientResponse)
    }

    def authenticate11(credentials) {
        resource.path(path11).path("auth").header("Authorization", "Basic " + new String(baseEncoding(authUser11,authUserPwd11))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def authenticate20(username, password) {
        def credentials = v2Factory.createAuthenticationRequest(username, password)
        resource.path(path20).path("tokens").accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def revokeToken11(auth, authPwd, String token) {
        resource.path(path11).path("token").path(token).header("Authorization", "Basic " + new String(baseEncoding(auth,authPwd))).delete(ClientResponse)
    }

    def validateToken20(authToken, token) {
        resource.path(path20).path("tokens").path(token).header("X-Auth-Token", authToken).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserByName11(String username) {
        resource.path(path11).path("users").path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser11,authUserPwd11))).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserByName20(String token, String name) {
        resource.path(path20).path("users").queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def createUser11(user) {
        resource.path(path11).path("users").header("Authorization", "Basic " + new String(baseEncoding(authUser11,authUserPwd11))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def createUser20(String token, user) {
        resource.path(path20).path("users").accept(APPLICATION_XML).type(APPLICATION_XML).header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def updateUser11(String username, user) {
        resource.path(path11).path("users").path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser11,authUserPwd11))).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).put(ClientResponse)
    }

    def getUserApiKey(String userId, String token) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def addApiKeyToUser(String token, String userId, credential) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credential).post(ClientResponse)
    }

    def deleteUser11(String username) {
        resource.path(path11).path("users").path(username).header("Authorization", "Basic " + new String(baseEncoding(authUser11,authUserPwd11))).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def hardDeleteUser(String token, String userId) {
        resource.path(path20).path("softDeleted").path("users").path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def deleteTenant20(String token, String tenantId) {
        resource.path(path20).path("tenants").path(tenantId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }
}
