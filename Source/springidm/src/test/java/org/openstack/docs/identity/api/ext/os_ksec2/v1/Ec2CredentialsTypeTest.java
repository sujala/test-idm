package org.openstack.docs.identity.api.ext.os_ksec2.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Ec2CredentialsTypeTest {

    Ec2CredentialsType ec2CredentialsType;

    @Before
    public void setUp() throws Exception {
        ec2CredentialsType = new Ec2CredentialsType();
    }

    @Test
    public void getUsername_setUsername_behavesCorrectly() throws Exception {
        assertThat("null value",ec2CredentialsType.getUsername(),nullValue());
        ec2CredentialsType.setUsername("value");
        assertThat("string",ec2CredentialsType.getUsername(),equalTo("value"));
    }

    @Test
    public void getKey_setKey_behavesCorrectly() throws Exception {
        assertThat("null value",ec2CredentialsType.getKey(),nullValue());
        ec2CredentialsType.setKey("value");
        assertThat("string",ec2CredentialsType.getKey(),equalTo("value"));
    }

    @Test
    public void getSignature_setSignature_behavesCorrectly() throws Exception {
        assertThat("null value",ec2CredentialsType.getSignature(),nullValue());
        ec2CredentialsType.setSignature("value");
        assertThat("string",ec2CredentialsType.getSignature(),equalTo("value"));
    }
}
