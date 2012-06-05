package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
import com.sun.imageio.plugins.common.InputStreamAdapter;
import org.bouncycastle.asn1.ASN1InputStream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.HttpParser;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Tenant;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/5/12
 * Time: 1:35 PM
 * To change this template use File | Settings | File Templates.
 */


public class JSONReaderForTenantTest {

    JSONReaderForTenant jsonReaderForTenant;
    JSONReaderForTenant spy;

    @Before
    public void setUp() throws Exception {
        jsonReaderForTenant = new JSONReaderForTenant();
        spy = spy(jsonReaderForTenant);
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

    @Test
    public void getTenantFromJSONString_validJsonBody_returnsTenantCorrectId() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": true,\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getId(), equalTo("1234"));
    }

    @Test
    public void getTenantFromJSONString_validJsonBody_returnsTenantCorrectName() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": true,\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getName(), equalTo("ACME corp"));
    }

    @Test
    public void getTenantFromJSONString_validJsonBody_returnsTenantCorrectDisplayName() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": true,\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getDisplayName(), equalTo("acme"));
    }

    @Test
    public void getTenantFromJSONString_validJsonBody_returnsTenantCorrectDescription() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": true,\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getDescription(), equalTo("A description ..."));
    }

    @Test
    public void getTenantFromJSONString_validJsonBody_returnsTenantNotEnabled() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).isEnabled(), equalTo(false));
    }

    @Test
    public void getTenantFromJSONString_passwordCredentialInput_returnsEmptyTenant() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getId(), nullValue());
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getName(), nullValue());
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getDisplayName(), nullValue());
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getDescription(), nullValue());
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).isEnabled(), equalTo(true));
    }

    @Test(expected = BadRequestException.class)
    public void getTenantFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForTenant.getTenantFromJSONString(body);
    }

    @Test
    public void getTenantFromJSONString_nullID_returnsTenantNullID() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getId(), nullValue());
    }

    @Test
    public void getTenantFromJSONString_nullName_returnsTenantNullName() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getName(), nullValue());
    }

    @Test
    public void getTenantFromJSONString_nullDisplayName_returnsTenantNullDisplayName() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getDisplayName(), nullValue());
    }

    @Test
    public void getTenantFromJSONString_nullDescription_returnsTenantNullDescription() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).getDescription(), nullValue());
    }

    @Test
    public void getTenantFromJSONString_nullEnable_returnsTenantEnableTrue() throws Exception {
        String body = "{\n" +
                "  \"tenant\": {\n" +
                "    \"id\": \"1234\",\n" +
                "    \"name\": \"ACME corp\",\n" +
                "    \"description\": \"A description ...\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "  }\n" +
                "}";
        assertThat("tenant", JSONReaderForTenant.getTenantFromJSONString(body).isEnabled(), equalTo(true));
    }


}
