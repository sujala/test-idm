package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import org.springframework.stereotype.Component
import spock.lang.Shared

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.JSONConstants.TOKENS
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
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
}
