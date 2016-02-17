package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import org.springframework.stereotype.Component
import spock.lang.Shared

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_JSON

@Component
class DevOpsMethods {

    @Shared WebResource resource

    def initOnUse(){
        resource = ensureGrizzlyStarted("classpath:app-config.xml");
    }

    def getInfo(token) {
        initOnUse()
        resource.path("devops/keystore/meta").accept(APPLICATION_JSON).type(APPLICATION_JSON).header("X-Auth-Token", token).get(ClientResponse)
    }

    def forceUpdateInfo(token) {
        initOnUse()
        resource.path("devops/keystore/meta").accept(APPLICATION_JSON).type(APPLICATION_JSON).header("X-Auth-Token", token).put(ClientResponse)
    }

    def getIdmProps(token) {
        initOnUse()
        resource.path("devops/props").accept(APPLICATION_JSON).type(APPLICATION_JSON).header("X-Auth-Token", token).get(ClientResponse)
    }

    def getFederationDeletion(token, request = "{\"federatedUsersDeletionRequest\":{\"max\": 1000}}", accept = APPLICATION_JSON, type = APPLICATION_JSON) {
        initOnUse()
        resource.path("devops/federation/deletion").accept(accept).type(type).header("X-Auth-Token", token).entity(request).post(ClientResponse)
    }

    def migrateSmsMfaOnUser(token, String userId, MobilePhone phone, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path("devops/users").path(userId).path("multi-factor/setupsms").accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).header("X-Auth-Token", token).entity(phone).post(ClientResponse)
    }

    def removeSmsMfaFromUser(token, String userId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path("devops/users").path(userId).path("multi-factor").accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).header("X-Auth-Token", token).delete(ClientResponse)
    }

}
