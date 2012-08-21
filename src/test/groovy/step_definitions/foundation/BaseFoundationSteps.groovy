import cucumber.runtime.PendingException
import org.junit.Before
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse

this.metaClass.mixin(cucumber.runtime.groovy.Hooks)
this.metaClass.mixin(cucumber.runtime.groovy.EN)

Before() {
    client = Client.create()
//    jetty_host = System.properties.get("jetty.host")
    jetty_host = "http://localhost:8083/idm/"
}

//Version steps
Given(~'^a foundation endpoint$') { ->
    // Express the Regexp above with the code you wish you had
    foundation_path = "v1"
    foundation_endpoint = client.resource(jetty_host+foundation_path)
}

//X-Auth-Token
Given(~'^a valid Foundation-Api X-Auth-Token$') { ->
    // Express the Regexp above with the code you wish you had
    foundation_response_token = foundation_endpoint.path("/tokens")
                                                   .header("Content-Type", "application/xml")
                                                   .header("Accept", "application/xml")
                                                   .entity("<authCredentials client_id=\"18e7a7032733486cd32f472d7bd58f709ac0d221\" client_secret=\"password1\" grant_type=\"CLIENT_CREDENTIALS\" xmlns=\"http://idm.api.rackspace.com/v1.0\"/>")
                                                   .post(ClientResponse.class)
}

