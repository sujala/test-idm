package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import org.springframework.stereotype.Component
import spock.lang.Shared

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE

@Component
class CloudMethods {

    @Shared WebResource resource

    @Shared String path10 = "cloud/"

    def initOnUse(){
        resource = ensureGrizzlyStarted("classpath:app-config.xml");
    }

    def getVersions(MediaType accept = APPLICATION_XML_TYPE) {
        initOnUse();
        resource.path(path10).accept(accept).get(ClientResponse)
    }
}
