package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForPasswordCredentials;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/6/12
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForPasswordCredentialsTest {
    String passwordCredentialsJSON = "{" +
            "   \"passwordCredentials\": {" +
            "       \"username\": \"jsmith\"," +
            "       \"password\": \"secretPassword\"" +
            "   }" +
            "}";


    @Test
    public void isReadable_withPasswordCredentials_returnsTrue() throws Exception {
        JSONReaderForPasswordCredentials jsonReaderForPasswordCredentials = new JSONReaderForPasswordCredentials();
        boolean readable = jsonReaderForPasswordCredentials.isReadable(PasswordCredentialsBase.class, PasswordCredentialsBase.class, null, null);
        assertThat("readable", readable, equalTo(true));

    }

}
