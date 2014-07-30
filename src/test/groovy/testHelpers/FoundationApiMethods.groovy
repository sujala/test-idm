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

    private volatile boolean initialized = false;

    def initOnUse(){
        if (!initialized) {
            synchronized (Cloud10Methods.class) {
                if (!initialized) {
                    this.resource = ensureGrizzlyStarted("classpath:app-config.xml")
                    initialized = true;
                }
            }
        }
    }


    def authenticate(String clientId, String clientSecret) {
        initOnUse()
        def cred = factory.createAuthCredentials(clientId, clientSecret)
        resource.path(path).path(TOKENS).type(APPLICATION_JSON).accept(APPLICATION_XML).entity(cred).post(ClientResponse)
    }

    def validateToken(String adminToken, String token) {
        initOnUse()
        resource.path(path).path(TOKENS).path(token).header(X_AUTH_TOKEN, adminToken).type(APPLICATION_JSON).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createUser(String token, user) {
        initOnUse()
        resource.path(path).path(USERS).header(X_AUTH_TOKEN, token).type(APPLICATION_JSON).accept(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def getUser(String token, String userId) {
        initOnUse()
        resource.path(path).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createTenant(String token, tenant) {
        initOnUse()
        resource.path(path).path(TENANTS).header(X_AUTH_TOKEN, token).type(APPLICATION_JSON).accept(APPLICATION_XML).entity(tenant).post(ClientResponse)
    }

    def getTenant(String token, String tenantId) {
        initOnUse()
        resource.path(path).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserPasswordCredentials(String token, String userId) {
        initOnUse()
        resource.path(path).path(USERS).path(userId).path("passwordcredentials").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def addUserTenantRole(String token, String userId, String roleId, String tenantId) {
        initOnUse()
        resource.path(path).path(USERS).path(userId).path(ROLES).path(roleId).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).put(ClientResponse)
    }

    def authenticateUser(String clientId, String clientSecret, String username, String password) {
        initOnUse()
        def cred = factory.createUserAuthCredentials(clientId, clientSecret, username, password)
        resource.path(path).path(TOKENS).type(APPLICATION_JSON).accept(APPLICATION_XML).entity(cred).post(ClientResponse)
    }

    def authenticateRacker(String clientId, String clientSecret, String username, String password) {
        initOnUse()
        def cred = factory.createRackerAuthCredentials(clientId, clientSecret, username, password)
        resource.path(path).path(TOKENS).type(APPLICATION_JSON).accept(APPLICATION_XML).entity(cred).post(ClientResponse)
    }

    def invalidFoundationCall(){
        initOnUse()
        resource.path(path).path("invalid").type(APPLICATION_XML).accept(APPLICATION_XML).get(ClientResponse)
    }

    def invalidIdmCall(){
        initOnUse()
        resource.path("invalid").path("invalid").type(APPLICATION_XML).accept(APPLICATION_XML).get(ClientResponse)
    }

    def invalidCall(){
        initOnUse()
        resource.path("invalid").type(APPLICATION_XML).accept(APPLICATION_XML).get(ClientResponse)
    }
}
