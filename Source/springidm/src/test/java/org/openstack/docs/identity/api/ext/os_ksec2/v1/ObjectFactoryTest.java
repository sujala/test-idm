package org.openstack.docs.identity.api.ext.os_ksec2.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {

    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createEc2CredentialsType_returnsEc2CredentialsType() throws Exception {
        assertThat("ec2 credentials type", objectFactory.createEc2CredentialsType(), instanceOf(Ec2CredentialsType.class));
    }

    @Test
    public void createEc2Credentials_returnsJAXBElement() throws Exception {
        Ec2CredentialsType ec2CredentialsType = new Ec2CredentialsType();
        assertThat("JAXBElement",objectFactory.createEc2Credentials(ec2CredentialsType),instanceOf(JAXBElement.class));
    }
}
