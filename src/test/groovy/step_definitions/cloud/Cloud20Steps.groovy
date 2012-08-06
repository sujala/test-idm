package step_definitions.cloud

import com.sun.jersey.api.client.Client
import cucumber.runtime.PendingException
import cucumber.table.DataTable
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername

this.metaClass.mixin(cucumber.runtime.groovy.Hooks)
this.metaClass.mixin(cucumber.runtime.groovy.EN)


class Cloud20Client{
    Client client;
    String endpoint = "cloud/v2.0/"

}

When(~'^POST token call is made with the following data:$') { DataTable creds ->
    request_body = new AuthenticationRequest()
    password_creds = new PasswordCredentialsRequiredUsername()
    password_creds.username = creds.getProperties().get("Username")
    password_creds.password = creds.getProperties().getAt("Password")
    request_body.credential = password_creds
    auth_endpoint.entity(request_body)

}

Then(~'^the body should be an access with:$') { DataTable arg1 ->
    // Express the Regexp above with the code you wish you had
    throw new PendingException()
}

Then(~'^the user element should be:$') { DataTable arg1 ->
    // Express the Regexp above with the code you wish you had
    throw new PendingException()
}

Then(~'^the user roles should be:$') { DataTable arg1 ->
    // Express the Regexp above with the code you wish you had
    throw new PendingException()
}