package com.rackspace.idm.documentation.samples;

import javax.xml.bind.JAXBElement;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ObjectFactory;

@SuppressWarnings("restriction")
public class ImpersonationRequest extends AbstractSample {

	@Override
	public JAXBElement<? extends Object> getJaxbObject() {

		ObjectFactory objectFactory = new ObjectFactory();
		org.openstack.docs.identity.api.v2.ObjectFactory openstackObjectFactory 
				= new org.openstack.docs.identity.api.v2.ObjectFactory(); 
		
		org.openstack.docs.identity.api.v2.User user = openstackObjectFactory.createUser();
		user.setUsername("john.smith");

		com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest impersonationRequest = objectFactory.createImpersonationRequest();
		impersonationRequest.setUser(user);

		return objectFactory.createImpersonation(impersonationRequest);
	}

	@Override
	public boolean overwriteJSONSample() {
		return false;
	}
}
