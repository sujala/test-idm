package com.rackspace.test;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.*;
import com.rackspace.idm.api.resource.cloud.ObjectMarshaller;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.ObjectFactory;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

public class Cloud20TestHelper {

    private static ObjectFactory objectFactory = new ObjectFactory();
    private org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = new org.openstack.docs.identity.api.v2.ObjectFactory();
    private com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory raxAuthObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();
    private com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory raxKsGrpObjectFactory = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory();

    public String getAuthenticationRequest(String username, String password) throws JAXBException {
        ObjectMarshaller<AuthenticationRequest> marshaller = new ObjectMarshaller<AuthenticationRequest>();

        AuthenticationRequest request = new AuthenticationRequest();
        PasswordCredentialsBase passwordCredentialsRequiredUsername = new PasswordCredentialsBase();
        passwordCredentialsRequiredUsername.setUsername(username);
        passwordCredentialsRequiredUsername.setPassword(password);

        request.setCredential(objectFactory.createCredential(passwordCredentialsRequiredUsername));

        return marshaller.marshal(objectFactory.createAuth(request), AuthenticationRequest.class);
    }

    public AuthenticateResponse getAuthenticateResponse(String response) throws JAXBException {
        ObjectMarshaller<AuthenticateResponse> unmarshaller = new ObjectMarshaller<AuthenticateResponse>();
        return unmarshaller.unmarshal(response, AuthenticateResponse.class);
    }

    public String createIdentityAdmin(String username, String password, String email) throws JAXBException {
        ObjectMarshaller<User> marshaller = new ObjectMarshaller<User>();
        User user = new User();
        user.setUsername(username);
        user.getOtherAttributes().put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password);
        user.setEmail(email);

        return marshaller.marshal(openStackIdentityV2Factory.createUser(user), User.class);
    }

    public Domains getDomainsObject(String domains) throws JAXBException {
        ObjectMarshaller<Domains> unMarshaller = new ObjectMarshaller<Domains>();
        return unMarshaller.unmarshal(domains, Domains.class);
    }

    public EndpointList getEndpointListObject(String endpoints) throws JAXBException {
        ObjectMarshaller<EndpointList> unMarshaller = new ObjectMarshaller<EndpointList>();
        return unMarshaller.unmarshal(endpoints, EndpointList.class);
    }

    public User getUser(String response) throws JAXBException {
        ObjectMarshaller<User> unmarshaller = new ObjectMarshaller<User>();
        return unmarshaller.unmarshal(response, User.class);
    }

    public String getPolicyString(String name, String blob, String type) throws JAXBException {
        Policy policy = new Policy();
        policy.setName(name);
        policy.setBlob(blob);
        policy.setType(type);

        ObjectMarshaller<Policy> marshaller = new ObjectMarshaller<Policy>();
        return marshaller.marshal(raxAuthObjectFactory.createPolicy(policy), Policy.class);
    }

    public Policy getPolicyObject(String policy) throws JAXBException {
        ObjectMarshaller<Policy> unmarshaller = new ObjectMarshaller<Policy>();
        return unmarshaller.unmarshal(policy, Policy.class);
    }

    public Policies getPolicies(String policies) throws JAXBException {
        ObjectMarshaller<Policies> unmarshaller = new ObjectMarshaller<Policies>();
        return unmarshaller.unmarshal(policies, Policies.class);
    }

    public String createUserAdmin(String name, String password, String email, String domainId) throws JAXBException {
        ObjectMarshaller<User> marshaller = new ObjectMarshaller<User>();
        User user = new User();
        user.setUsername(name);
        user.getOtherAttributes().put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password);
        user.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","domainId"), domainId);
        user.setEmail(email);

        return marshaller.marshal(openStackIdentityV2Factory.createUser(user), User.class);
    }

    public String createDefaultUser(String name, String password, String email, String domainId) throws JAXBException {
        ObjectMarshaller<User> marshaller = new ObjectMarshaller<User>();
        User user = new User();
        user.setUsername(name);
        user.getOtherAttributes().put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password);
        user.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","domainId"), domainId);
        user.setEmail(email);

        return marshaller.marshal(openStackIdentityV2Factory.createUser(user), User.class);
    }

    public String createDomain(String domainId, String domainName, boolean enabled) throws JAXBException {
        ObjectMarshaller<Domain> marshaller = new ObjectMarshaller<Domain>();
        Domain domain = new Domain();
        domain.setName(domainName);
        domain.setEnabled(enabled);
        domain.setId(domainId);

        return marshaller.marshal(raxAuthObjectFactory.createDomain(domain), Domain.class);
    }

    public Domain getDomain(String response) throws JAXBException {
        ObjectMarshaller<Domain> unmarshaller = new ObjectMarshaller<Domain>();
        return unmarshaller.unmarshal(response, Domain.class);
    }

    public String createGroup(String name, String description) throws JAXBException {
        ObjectMarshaller<Group> marshaller = new ObjectMarshaller<Group>();
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);

        return marshaller.marshal(raxKsGrpObjectFactory.createGroup(group), Group.class);
    }

    public Group getGroup(String response) throws JAXBException {
        ObjectMarshaller<Group> unmarshaller = new ObjectMarshaller<Group>();
        return unmarshaller.unmarshal(response, Group.class);
    }
}
