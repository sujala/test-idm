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

