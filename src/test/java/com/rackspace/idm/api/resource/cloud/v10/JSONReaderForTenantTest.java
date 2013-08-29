package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.api.idm.v1.Tenant;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForTenantTest {

    JSONReaderForTenant jsonReaderForTenant;
    String tenantJSON;
    String emptyTenantJSON;

    @Before
    public void setUp() throws Exception {
        jsonReaderForTenant = new JSONReaderForTenant();
        tenantJSON = "{" +
                "   \"tenant\" : {" +
                "       \"id\" : \"tenantId\"," +
                "       \"name\" : \"tenantName\"," +
                "       \"description\" : \"tenantDescription\"," +
                "       \"enabled\" : false," +
                "       \"display-name\" : \"tenantDisplayName\"" +
                "   }" +
                "}";

        emptyTenantJSON = "{" +
                "   \"tenant\" : {" +
                "   }" +
                "}";
    }

    @Test
    public void isReadable_forValidClass_returnsTrue() throws Exception {
        JSONReaderForTenant jsonReaderForTenant = new JSONReaderForTenant();
        boolean readable = jsonReaderForTenant.isReadable(Tenant.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_forInvalidClass_returnsFalse() throws Exception {
        JSONReaderForTenant jsonReaderForTenant = new JSONReaderForTenant();
        boolean readable = jsonReaderForTenant.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnsTenantObject() throws Exception {
        JSONReaderForTenant jsonReaderForTenant = new JSONReaderForTenant();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(tenantJSON.getBytes()));
        jsonReaderForTenant.readFrom(Tenant.class, null, null, null, null, inputStream);
    }

    @Test
    public void getTenantFromJSONString_withValidJSON_returnsTenantWithId() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(tenantJSON);
        assertThat("tenant id", tenantFromJSONString.getId(), equalTo("tenantId"));
    }

    @Test
    public void getTenantFromJSONString_withValidJSON_returnsTenantWithName() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(tenantJSON);
        assertThat("tenant name", tenantFromJSONString.getName(), equalTo("tenantName"));
    }

    @Test
    public void getTenantFromJSONString_withValidJSON_returnsTenantWithDescription() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(tenantJSON);
        assertThat("tenant description", tenantFromJSONString.getDescription(), equalTo("tenantDescription"));
    }

    @Test
    public void getTenantFromJSONString_withValidJSON_returnsTenantWithEnabled() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(tenantJSON);
        assertThat("tenant enabled", tenantFromJSONString.isEnabled(), equalTo(false));
    }

    @Test
    public void getTenantFromJSONString_withValidJSON_returnsTenantWithDisplayName() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(tenantJSON);
        assertThat("tenant display-name", tenantFromJSONString.getDisplayName(), equalTo("tenantDisplayName"));
    }

    @Test
    public void getTenantFromJSONString_withValidEmptyTenantJSON_returnsTenantWithNullId() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(emptyTenantJSON);
        assertThat("tenant id", tenantFromJSONString.getId(), nullValue());
    }

    @Test
    public void getTenantFromJSONString_withValidEmptyTenantJSON_returnsTenantWithNullName() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(emptyTenantJSON);
        assertThat("tenant name", tenantFromJSONString.getName(), nullValue());
    }

    @Test
    public void getTenantFromJSONString_withValidEmptyTenantJSON_returnsTenantWithNullDescription() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(emptyTenantJSON);
        assertThat("tenant description", tenantFromJSONString.getDescription(), nullValue());
    }

    @Test
    public void getTenantFromJSONString_withValidEmptyTenantJSON_returnsTenantWithEnabledTrue() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(emptyTenantJSON);
        assertThat("tenant enabled", tenantFromJSONString.isEnabled(), equalTo(true));
    }

    @Test
    public void getTenantFromJSONString_withValidEmptyTenantJSON_returnsTenantWithNullDisplayName() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString(emptyTenantJSON);
        assertThat("tenant display-name", tenantFromJSONString.getDisplayName(), nullValue());
    }

    @Test
    public void getTenantFromJSON_WithEmptyJSON_returnsNewTenantObject() throws Exception {
        Tenant tenantFromJSONString = JSONReaderForTenant.getTenantFromJSONString("{ }");
        assertThat("tenant id", tenantFromJSONString.getId(), nullValue());
        assertThat("tenant name", tenantFromJSONString.getName(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getTenantFromJSON_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForTenant.getTenantFromJSONString("Invalid JSON");
    }
}
