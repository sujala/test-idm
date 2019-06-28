package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.Base64
import org.springframework.stereotype.Component
import spock.lang.Shared

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.JSONConstants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE

@Component
class Cloud11Methods {
    @Shared WebResource resource

    @Shared String path11 = "cloud/v1.1/"

    @Shared String authUser = "auth"
    @Shared String authPassword = "auth123"

    static def SERVICE_PATH_NAST = "nast"
    static def SERVICE_PATH_MOSSO = "mosso"

    private def initOnUse(){
        resource = ensureGrizzlyStarted("classpath:app-config.xml");
    }

    def adminAuthenticate(credentials, MediaType requestContentMediaType = APPLICATION_XML_TYPE, MediaType acceptMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path11).path(AUTH_ADMIN).header("Authorization", getBasicAuth()).accept(acceptMediaType).type(requestContentMediaType).entity(credentials).post(ClientResponse)
    }

    def authenticate(credentials, MediaType requestContentMediaType = APPLICATION_XML_TYPE, MediaType acceptMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path11).path(AUTH).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(credentials).post(ClientResponse)
    }

    def validateToken(String token) {
        initOnUse()
        resource.path(path11).path(TOKEN).path(token).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserByName(String username) {
        initOnUse()
        resource.path(path11).path(USERS).path(username).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserFromNastId(String nastId, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path11).path(SERVICE_PATH_NAST).path(nastId).header("Authorization", getBasicAuth()).accept(mediaType).get(ClientResponse)
    }

    def getUserFromMossoId(String mossoId, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path11).path(SERVICE_PATH_MOSSO).path(mossoId).header("Authorization", getBasicAuth()).accept(mediaType).get(ClientResponse)
    }

    def getToken(token) {
        initOnUse()
        resource.path(path11).path(TOKEN).path(token).header("Authorization", getBasicAuth()).accept(APPLICATION_XML).get(ClientResponse)
    }

    def String getBasicAuth(username=authUser, password=authPassword) {
        "Basic " + new String(baseEncoding(username, password))
    }

    def getVersion(MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path11).accept(requestContentMediaType).get(ClientResponse)
    }

    def baseEncoding(String username, String password) {
        return Base64.encode(username + ":" + password);
    }
}
