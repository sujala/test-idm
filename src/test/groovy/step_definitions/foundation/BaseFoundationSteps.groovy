import cucumber.runtime.PendingException
import org.junit.Before
import com.sun.jersey.api.client.Client

this.metaClass.mixin(cucumber.runtime.groovy.Hooks)
this.metaClass.mixin(cucumber.runtime.groovy.EN)

Before() {
    client = Client.create()
//    jetty_host = System.properties.get("jetty.host")
    jetty_host = "http://localhost:8083/idm/"
}
