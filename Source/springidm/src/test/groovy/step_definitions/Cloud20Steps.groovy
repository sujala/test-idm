package step_definitions

import com.sun.jersey.api.client.Client
import org.junit.Before

/**
 * User: alan.erwin
 * Date: 6/28/12
 * Time: 12:07 PM
 */
this.metaClass.mixin(cucumber.runtime.groovy.Hooks)
this.metaClass.mixin(cucumber.runtime.groovy.EN)
Client client
String jetty_host

Before() {
    client = Client.create()
    jetty_host = System.properties.get("jetty.host")
}

Given(~'^a auth (\\d+).(\\d+) endpoint$') { int version, int release ->
    // Express the Regexp above with the code you wish you had
    version_path = ""
    if (version.equals(1) && release.equals(0)) {
        version_path = "cloud/v1.0"
    } else if (version.equals(1) && release.equals(1)) {
        version_path = "cloud/v1.1"
    } else {
        version_path = "cloud/v2.0"
    }
    auth_endpoint = client.resource(jetty_host+version_path)
    auth_endpoint.get(String.class)
}


When(~'^GET version call is made$') {->
    // Express the Regexp above with the code you wish you had
  response = auth_endpoint.accept("application/xml").get(ClientResponse.class)
}

Then(~'^the response status should be (\\d+)$') { int status ->
    // Express the Regexp above with the code you wish you had
   assert response.getStatus() == status

}

