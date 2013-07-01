package testHelpers

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge|jacob
 * Date: 6/27/13
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */

class Cloud10Methods {

    @Shared WebResource resource

    @Shared String path10 = "cloud/v1.0/"
    @Shared String X_AUTH_USER = "X-Auth-User"
    @Shared String X_AUTH_KEY = "X-Auth-Key"


    def authenticate(String username, String apiKey) {
        resource.path(path10).header(X_AUTH_USER, username).header(X_AUTH_KEY, apiKey).get(ClientResponse)
    }
}
