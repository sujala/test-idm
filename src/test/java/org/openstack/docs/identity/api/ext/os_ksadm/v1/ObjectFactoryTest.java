package org.openstack.docs.identity.api.ext.os_ksadm.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {

    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createUserWithOnlyEnabled_returnsUserWithOnlyEnabled() throws Exception {
        assertThat("user with only enabled",objectFactory.createUserWithOnlyEnabled(),instanceOf(UserWithOnlyEnabled.class));
    }

    @Test
    public void createExtensibleCredentialsType_returnsJAXBElement() throws Exception {
        assertThat("jaxb element",objectFactory.createExtensibleCredentialsType("foo"),instanceOf(JAXBElement.class));
    }
}
