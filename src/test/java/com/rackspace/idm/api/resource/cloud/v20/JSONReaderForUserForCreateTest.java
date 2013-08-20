package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForUserForCreate;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.Tenant;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/6/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserForCreateTest {


    JSONReaderForUserForCreate jsonReaderForUserForCreate;

    @Before
    public void setUp() throws Exception {
        jsonReaderForUserForCreate = new JSONReaderForUserForCreate();
    }

    @Test
    public void isReadable_typeIsUserForCreate_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForUserForCreate.isReadable(UserForCreate.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotUserForCreate_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForUserForCreate.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsUserForCreate() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",\n" +
                "            \"username\": \"cmarin1subX2\",\n" +
                "            \"display-name\": \"marin\",\n" +
                "            \"email\": \"cmarin1-sub@example.com\",\n" +
                "            \"enabled\": false,\n" +
                "            \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("userForCreate",jsonReaderForUserForCreate.readFrom(UserForCreate.class, null, null, null, null, inputStream),instanceOf(UserForCreate.class));
    }

}
