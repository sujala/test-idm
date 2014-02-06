package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import org.springframework.stereotype.Component
import spock.lang.Shared
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static com.rackspace.idm.JSONConstants.*
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.MediaType.APPLICATION_XML

@Component
class FoundationApiMethods {

    @Shared WebResource resource
    @Shared def factory = new FoundationFactory()
    @Shared String path = "v1/"

    //Constants
    static def X_AUTH_TOKEN = "X-Auth-Token"

    def init() {
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")
    }

    def authenticate(String clientId, String clientSecret) {
        def cred = factory.createAuthCredentials(clientId, clientSecret)

        resource.path(path).path(TOKENS).type(APPLICATION_JSON).accept(APPLICATION_XML).entity(cred).post(ClientResponse)
    }

    def validateToken(String adminToken, String token) {
        resource.path(path).path(TOKENS).path(token).header(X_AUTH_TOKEN, adminToken).type(APPLICATION_JSON).accept(APPLICATION_XML).get(ClientResponse)
    }
}
