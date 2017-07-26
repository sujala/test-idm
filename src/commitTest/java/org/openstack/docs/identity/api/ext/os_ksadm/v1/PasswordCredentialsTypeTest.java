package org.openstack.docs.identity.api.ext.os_ksadm.v1;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class PasswordCredentialsTypeTest {

    PasswordCredentialsType passwordCredentialsType = PasswordCredentialsType.PASSWORD ;


    @Test
    public void value_returnsString() throws Exception {
        assertThat("string", passwordCredentialsType.value(), equalTo("password"));
    }

    @Test
    public void fromValue_valueIsSupported_returnsEnum() throws Exception {
        assertThat("enum",PasswordCredentialsType.fromValue("password"),equalTo(PasswordCredentialsType.PASSWORD));
    }

    @Test
    public void fromValue_valueIsNotSupported_throwsIllegalArgumentException() throws Exception {
        try{
            PasswordCredentialsType.fromValue("foo");
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message",ex.getMessage(),equalTo("foo"));
        }
    }
}
