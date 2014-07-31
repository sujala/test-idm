package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import org.springframework.stereotype.Component
import spock.lang.Shared
import spock.lang.Specification

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted

/**
 * Created with IntelliJ IDEA.
 * User: jorge|jacob
 * Date: 6/27/13
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
class Cloud10Methods {

    @Shared WebResource resource

    @Shared String path10 = "cloud/v1.0/"
    @Shared String X_AUTH_USER = "X-Auth-User"
    @Shared String X_AUTH_KEY = "X-Auth-Key"

    def initOnUse(){
        resource = ensureGrizzlyStarted("classpath:app-config.xml");
    }

    def authenticate(String username, String apiKey) {
        initOnUse();
        resource.path(path10).header(X_AUTH_USER, username).header(X_AUTH_KEY, apiKey).get(ClientResponse)
    }
}
