package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/20/12
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class JAXBObjectFactoriesTest {

    JAXBObjectFactories jaxbObjectFactories;

    @Before
    public void setUp() throws Exception {
        jaxbObjectFactories = new JAXBObjectFactories();
    }

    @Test
    public void getRackspaceIdentityExtKsqaV1Factory_succeeds() throws Exception {
        assertThat("rackspace identity ext ksqa v1 factory",jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory(),instanceOf(ObjectFactory.class));
    }

    @Test
    public void getOpenStackCommonV1Factory_succeeds() throws Exception {
        assertThat("open stack common v1 factory",jaxbObjectFactories.getOpenStackCommonV1Factory(),instanceOf(org.openstack.docs.common.api.v1.ObjectFactory.class));
    }

    @Test
    public void getOpenStackComputeV1Factory_succeeds() throws Exception {
        assertThat("open stack compute v1 factory",jaxbObjectFactories.getOpenStackComputeV1Factory(),instanceOf(org.openstack.docs.compute.api.v1.ObjectFactory.class));
    }

    @Test
    public void getOpenStackIdentityExtKsec2V1Factory_succeeds() throws Exception {
        assertThat("open stack identity ext ksec2 v1 factory",jaxbObjectFactories.getOpenStackIdentityExtKsec2V1Factory(),instanceOf(org.openstack.docs.identity.api.ext.os_ksec2.v1.ObjectFactory.class));
    }

    @Test
    public void getRackspaceIdentityExtKsgrpV1Factory_succeeds() throws Exception {
        assertThat("rackspace identity ext ksgrp v1 factory",jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory(),instanceOf(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory.class));
    }

    @Test
    public void getAtomObjectFactory_succeeds() throws Exception {
        assertThat("atom object factory",jaxbObjectFactories.getAtomObjectFactory(),instanceOf(org.w3._2005.atom.ObjectFactory.class));
    }

    @Test
    public void getRackspaceCloudV1ObjectFactory_succeeds() throws Exception {
        assertThat("rackspace cloud v1 object factory",jaxbObjectFactories.getRackspaceCloudV1ObjectFactory(),instanceOf(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class));
    }
}
