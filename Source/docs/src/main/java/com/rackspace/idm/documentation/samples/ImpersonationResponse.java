package com.rackspace.idm.documentation.samples;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ObjectFactory;

@SuppressWarnings("restriction")
public class ImpersonationResponse extends AbstractSample {

	@Override
	public JAXBElement<? extends Object> getJaxbObject() {

		ObjectFactory objectFactory = new ObjectFactory();
		org.openstack.docs.identity.api.v2.ObjectFactory openstackObjectFactory 
				= new org.openstack.docs.identity.api.v2.ObjectFactory(); 
		
		org.openstack.docs.identity.api.v2.Token token = openstackObjectFactory.createToken();
		token.setId("xxx-yyy-zzz-kwejKId893KJDKJKSKJS");
		token.setExpires(getTime(20L));

		com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationResponse impersonationResponse 
			= objectFactory.createImpersonationResponse();
		impersonationResponse.setToken(token);

		return objectFactory.createAccess(impersonationResponse);
	}
	
	@Override
	public boolean overwriteJSONSample() {
		return false;
	}
	
	XMLGregorianCalendar getTime(Long date) {
		try {
			GregorianCalendar c = new GregorianCalendar();
			c.setTime(new Date(date));
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}
}
