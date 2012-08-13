package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Role;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/13/12
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForDomainTest {
    JSONReaderForDomain jsonReaderForDomain;
    JSONReaderForDomain spy;

    @Before
    public void setUp() throws Exception {
        jsonReaderForDomain = new JSONReaderForDomain();
        spy = spy(jsonReaderForDomain);
    }
    @Test
    public void isReadable_NotReadable_returnsFalse() throws Exception {
        assertThat("bool",jsonReaderForDomain.isReadable(Role.class, null, null, null),equalTo(false));
    }
    @Test
    public void isReadable_NotReadable_returnsTrue() throws Exception {
        assertThat("bool",jsonReaderForDomain.isReadable(Domain.class, null, null, null),equalTo(true));
    }
    @Test
    public void readFrom_returnsTenant() throws Exception {
        String body = "{\n" +
                "  \"domain\": {\n" +
                "    \"id\": \"1\",\n" +
                "    \"name\": \"name\",\n" +
                "    \"description\": \"description...\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("domain",jsonReaderForDomain.readFrom(Domain.class, null, null, null, null, inputStream),instanceOf(Domain.class));
    }
}
