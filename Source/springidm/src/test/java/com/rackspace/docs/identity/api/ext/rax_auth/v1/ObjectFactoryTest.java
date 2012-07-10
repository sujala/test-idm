package com.rackspace.docs.identity.api.ext.rax_auth.v1;

import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/9/12
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createImpersonationRequest_returnsNewCreatedObject() throws Exception {
        ImpersonationRequest result = objectFactory.createImpersonationRequest();
        assertThat("user", result.user, equalTo(null));
    }

    @Test
    public void createImpersonator_returnsNewCreatedObject() throws Exception {
        JAXBElement<UserForAuthenticateResponse> result = objectFactory.createImpersonator(new UserForAuthenticateResponse());
        assertThat("id", result.getValue().getId(), equalTo(null));
    }

    @Test
    public void createImpersonation_returnsNewCreatedObject() throws Exception {
        JAXBElement<ImpersonationRequest> result = objectFactory.createImpersonation(new ImpersonationRequest());
        assertThat("user", result.getValue().user, equalTo(null));
    }
}
