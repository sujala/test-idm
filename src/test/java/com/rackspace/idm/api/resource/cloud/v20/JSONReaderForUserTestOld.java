package com.rackspace.idm.api.resource.cloud.v20;


import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForUser;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.User;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/5/12
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserTestOld {

    JSONReaderForUser jsonReaderForUser;

    @Before
    public void setUp() throws Exception {
        jsonReaderForUser = new JSONReaderForUser();
    }

    @Test
    public void isReadable_typeIsUser_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForUser.isReadable(User.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotUser_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForUser.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

//    @Test
//    public void readFrom_returnsUser() throws Exception {
//        String body = "{\n" +
//                "  \"user\": {\n" +
//                "    \"id\": \"123456\",\n" +
//                "    \"username\": \"jqsmith\",\n" +
//                "    \"display-name\": \"acme\",\n" +
//                "    \"email\": \"john.smith@example.org\",\n" +
//                "    \"enabled\": true\n" +
//                "  }\n" +
//                "}";
//        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
//        assertThat("user",jsonReaderForUser.readFrom(User.class, null, null, null, null, inputStream),instanceOf(User.class));
//    }
}
