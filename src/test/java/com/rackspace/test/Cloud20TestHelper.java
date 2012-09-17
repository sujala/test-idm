package com.rackspace.test;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.idm.api.resource.cloud.ObjectMarshaller;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

public class Cloud20TestHelper {


    private ObjectFactory objectFactory = new ObjectFactory();
    private org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory catalogObjectFactory = new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory();
    private org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = new org.openstack.docs.identity.api.v2.ObjectFactory();
    private com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory policyObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

    public String getAuthenticationRequest(String username, String password) throws JAXBException {
        ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();

        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);

        request.setCredential(objectFactory.createPasswordCredentials(passwordCredentialsRequiredUsername));

        return marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);
    }

    public AuthenticateResponse getAuthenticateResponse(String response) throws JAXBException {
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
    }

    public String getEndpointTemplateString(String endpointTemplateId) throws JAXBException {
        ObjectMarshaller<EndpointTemplate> marshaller = new ObjectMarshaller<EndpointTemplate>();

        EndpointTemplate endpoint = new EndpointTemplate();
        endpoint.setId(Integer.valueOf(endpointTemplateId));
        endpoint.setType("type");
        endpoint.setPublicURL("http://public.url");

        return marshaller.marshal(catalogObjectFactory.createEndpointTemplate(endpoint), EndpointTemplate.class);
    }

    public String createServiceAdmin(String username, String password, String email) throws JAXBException {
        ObjectMarshaller<User> marshaller = new ObjectMarshaller<User>();
        User user = new User();
        user.setUsername(username);
        user.getOtherAttributes().put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"),password);
        user.setEmail(email);

        return marshaller.marshal(openStackIdentityV2Factory.createUser(user), User.class);
    }

    public EndpointTemplate getEndpointTemplateObject(String endpointTemplate) throws JAXBException {
        ObjectMarshaller<EndpointTemplate> unmarshaller = new ObjectMarshaller<EndpointTemplate>();
        return unmarshaller.unmarshal(endpointTemplate, EndpointTemplate.class);
    }

    public User getUser(String response) throws JAXBException {
        ObjectMarshaller<User> unmarshaller = new ObjectMarshaller<User>();
        return unmarshaller.unmarshal(response, User.class);
    }

    public String getPolicyString() throws JAXBException {
        Policy policy = new Policy();
        policy.setName("name");
        policy.setBlob("blob");
        policy.setType("type");

        ObjectMarshaller<Policy> marshaller = new ObjectMarshaller<Policy>();
        return marshaller.marshal(policyObjectFactory.createPolicy(policy), Policy.class);
    }

    public Policy getPolicyObject(String policy) throws JAXBException {
        ObjectMarshaller<Policy> unmarshaller = new ObjectMarshaller<Policy>();
        return unmarshaller.unmarshal(policy, Policy.class);
    }
}
