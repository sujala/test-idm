package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Role;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
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

    String jsonBody = "{\n" +
                "  \"domain\": {\n" +
                "    \"id\": \"1\",\n" +
                "    \"name\": \"name\",\n" +
                "    \"description\": \"description...\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";

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
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(jsonBody.getBytes()));
        assertThat("domain",jsonReaderForDomain.readFrom(Domain.class, null, null, null, null, inputStream),instanceOf(Domain.class));
    }

    @Test
    public void readFrom_withValidJSON_returnsRsaCredentials() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(jsonBody.getBytes()));
        Domain domain = jsonReaderForDomain.readFrom(Domain.class, null, null, null, null, inputStream);
        assertThat("authentication request credentials", domain, not(nullValue()));
    }

    @Test(expected = BadRequestException.class)
    public void readFrom_withInvalidJSON_throwsBadRequestException() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream("invalid JSON string".getBytes()));
        jsonReaderForDomain.readFrom(Domain.class, null, null, null, null, inputStream);
    }

    @Test
    public void getRSARequestFromJSONString_withNoRSARequestKey_returnsEmptyRSARequest() throws Exception {
        Domain domainJSON = JSONReaderForDomain.getDomainFromJSONString("{ }");
        assertThat("authentication request", domainJSON.getName(), nullValue());
    }

    @Test
    public void getRSARequestFromJSONString_withValidRSARequest_returnsRSACredentials() throws Exception {
        Domain domainJSON = JSONReaderForDomain.getDomainFromJSONString(jsonBody);
        assertThat("domain", domainJSON, instanceOf(Domain.class));
    }
}
