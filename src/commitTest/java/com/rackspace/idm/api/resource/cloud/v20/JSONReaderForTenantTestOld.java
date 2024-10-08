package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForTenant;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Tenant;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/5/12
 * Time: 1:35 PM
 * To change this template use File | Settings | File Templates.
 */


public class JSONReaderForTenantTestOld {

    JSONReaderForTenant jsonReaderForTenant;

    @Before
    public void setUp() throws Exception {
        jsonReaderForTenant = new JSONReaderForTenant();
    }
    @Test
    public void isReadable_NotReadable_returnsFalse() throws Exception {
        assertThat("bool",jsonReaderForTenant.isReadable(Role.class, null, null, null),equalTo(false));
    }

    @Test
    public void isReadable_Readable_returnsTrue() throws Exception {
        assertThat("bool",jsonReaderForTenant.isReadable(Tenant.class, null, null, null),equalTo(true));
    }

    @Test
    public void readFrom_returnsTenant() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("tenant",jsonReaderForTenant.readFrom(Tenant.class, null, null, null, null, inputStream),instanceOf(Tenant.class));
    }
}
