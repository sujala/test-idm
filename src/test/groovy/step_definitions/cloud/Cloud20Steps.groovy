import com.sun.jersey.api.client.Client
import cucumber.runtime.PendingException
import cucumber.table.DataTable
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername
import com.sun.jersey.api.client.ClientResponse


this.metaClass.mixin(cucumber.runtime.groovy.Hooks)
this.metaClass.mixin(cucumber.runtime.groovy.EN)


Given(~'^a valid Identity-Admin X-Auth-Token$') { ->
    response = auth_endpoint.path("/tokens")
                            .header("Content-Type", "application/xml")
                            .header("Accept", "application/xml")
                            .entity("<auth xmlns=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><passwordCredentials password=\"Auth1234\" username=\"auth\"/></auth>")
                            .post(ClientResponse.class)

    authentication_response = response.getEntity(AuthenticateResponse.class).getValue()
    identity_admin_token = authentication_response.getToken().getId()
}

Given(~'^a valid Service-Admin X-Auth-Token$') { ->
    // Express the Regexp above with the code you wish you had
    BaseFoundationSteps.Given(~'^a foundation endpoint$');
    create_response = foundation_endpoint.path("/users")
                      .header("Content-Type", "application/xml")
                      .header("Accept", "application/xml")
                      .header("X-Auth-Token", identity_admin_token)
                      .entity("<user customerId=\"RCN-000-111-000\" displayName=\"test user\" email=\"test.user@example.org\" firstName=\"SAtest\" lastName=\"Smith\" middleName=\"Lee\" personId=\"RPN-111-111-111\" prefLanguage=\"US_en\" region=\"SAT\" timeZone=\"America/Chicago\" username=\"cloudfiles\" xmlns=\"http://idm.api.rackspace.com/v1.0\"><roles id=\"4\"/><secret secretAnswer=\"Buddy\" secretQuestion=\"What is your dogs name?\"/><passwordCredentials><currentPassword password=\" Ud2ack8hibJ5bje2lks\"/></passwordCredentials></user>")
                      .post(ClientResponse.class)



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