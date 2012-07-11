package step_definitions

import com.sun.jersey.api.client.Client
import cucumber.table.DataTable

this.metaClass.mixin(cucumber.runtime.groovy.Hooks)
this.metaClass.mixin(cucumber.runtime.groovy.EN)
Client client;

//class HelloWorld {
//}

Given(~'^new user data:$') { DataTable arg1 ->
    // Express the Regexp above with the code you wish you had
//    throw new PendingException()
}

Given(~'^a valid X-Auth-Token$') { ->
    // Express the Regexp above with the code you wish you had
//    throw new PendingException()
}

When(~'^create user call is made$') { ->
    // Express the Regexp above with the code you wish you had
//    throw new PendingException()
}

Then(~'^the response should have the following fields:$') { DataTable arg1 ->
    // Express the Regexp above with the code you wish you had
//    throw new PendingException()
}


