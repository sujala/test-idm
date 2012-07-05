package step_definitions.cloud

import com.sun.jersey.api.client.Client

this.metaClass.mixin(cucumber.runtime.groovy.Hooks)
this.metaClass.mixin(cucumber.runtime.groovy.EN)


class Cloud20Client{
    Client client;
    String endpoint = "cloud/v2.0/"

}

