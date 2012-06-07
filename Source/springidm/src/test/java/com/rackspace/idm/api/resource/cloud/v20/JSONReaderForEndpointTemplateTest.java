package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/5/12
 * Time: 5:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForEndpointTemplateTest {

    JSONReaderForEndpointTemplate jsonReaderForEndpointTemplate;
    String endpointTemplateJSON = "{\n" +
                "   \"OS-KSCATALOG:endpointTemplate\":{\n" +
                "       \"id\":1,\n" +
                "       \"region\":\"North\",\n" +
                "       \"type\":\"compute\",\n" +
                "       \"publicURL\":\"https://compute.north.public.com/v1\",\n" +
                "       \"internalURL\":\"https://compute.north.internal.com/v1\",\n" +
                "       \"adminURL\" : \"https://compute.north.admin.com/v1\",\n" +
                "       \"global\" : \"true\",\n" +
                "       \"enabled\" : \"true\",\n" +
                "       \"versionId\":\"1\",\n" +
                "       \"versionInfo\":\"https://compute.north.public.com/v1/\",\n" +
                "       \"versionList\":\"https://compute.north.public.com/versionList\"\n" +
                "   }\n" +
                "}";

    String emptyEndpointTemplateJSON = "{\n" +
            "   \"OS-KSCATALOG:endpointTemplate\":{\n" +
            "   }\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        jsonReaderForEndpointTemplate = new JSONReaderForEndpointTemplate();
    }

    @Test
    public void isReadable_withValidType_returnsTrue() throws Exception {
        boolean readable = jsonReaderForEndpointTemplate.isReadable(EndpointTemplate.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidType_returnsFalse() throws Exception {
        boolean readable = jsonReaderForEndpointTemplate.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidInput_returnsNonNull() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(endpointTemplateJSON.getBytes()));
        EndpointTemplate endpointTemplate = jsonReaderForEndpointTemplate.readFrom(EndpointTemplate.class, null, null, null, null, inputStream);
        assertThat("endpoint template", endpointTemplate, not(nullValue()));
    }

    @Test(expected = BadRequestException.class)
    public void readFrom_withInvalidInput_throwsBadRequestException() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream("Invalid JSON String".getBytes()));
        jsonReaderForEndpointTemplate.readFrom(EndpointTemplate.class, null, null, null, null, inputStream);
    }

    @Test
    public void getEndpointTemplateFromJSONString_withEmptyJSON_returnsNewEndpointTemplate() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString("{ }");
        assertThat("endpoint template", endpointTemplateFromJSONString.getId(), equalTo(0));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsEndpointId() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template id", endpointTemplateFromJSONString.getId(), equalTo(1));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoEndpointId_setsNullEndpointID() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template id", endpointTemplateFromJSONString.getId(), equalTo(0));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsRegion() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template region", endpointTemplateFromJSONString.getRegion(), equalTo("North"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoRegion_setsNullRegion() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template region", endpointTemplateFromJSONString.getRegion(), nullValue());
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsType() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template type", endpointTemplateFromJSONString.getType(), equalTo("compute"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoType_setsNullType() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template type", endpointTemplateFromJSONString.getType(), nullValue());
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsPublicURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template publicURL", endpointTemplateFromJSONString.getPublicURL(), equalTo("https://compute.north.public.com/v1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoPublicURL_setsNullPublicURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template PublicURL", endpointTemplateFromJSONString.getPublicURL(), nullValue());
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsInternalURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template internalURL", endpointTemplateFromJSONString.getInternalURL(), equalTo("https://compute.north.internal.com/v1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoInternalURL_setsNullInternalURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template InternalURL", endpointTemplateFromJSONString.getInternalURL(), nullValue());
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsAdminURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template adminURL", endpointTemplateFromJSONString.getAdminURL(), equalTo("https://compute.north.admin.com/v1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoAdminURL_setsNullAdminURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template AdminURL", endpointTemplateFromJSONString.getAdminURL(), nullValue());
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsGlobal() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template global", endpointTemplateFromJSONString.isGlobal(), equalTo(true));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoGlobal_setsNullGlobal() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template global", endpointTemplateFromJSONString.isGlobal(), equalTo(false));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsEnabled() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template Enabled", endpointTemplateFromJSONString.isEnabled(), equalTo(true));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoEnabled_setsNullEnabled() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template Enabled", endpointTemplateFromJSONString.isEnabled(), equalTo(true));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsVersionId() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template versionId", endpointTemplateFromJSONString.getVersion().getId(), equalTo("1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoVersionId_setsNullVersion() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(emptyEndpointTemplateJSON);
        assertThat("endpoint template version", endpointTemplateFromJSONString.getVersion(), nullValue());
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsVersionInfo() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template versionInfo", endpointTemplateFromJSONString.getVersion().getInfo(), equalTo("https://compute.north.public.com/v1/"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoVersionInfo_setsNullVersionInfo() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString("{\n" +
                "   \"OS-KSCATALOG:endpointTemplate\":{\n" +
                "   \"versionId\" : \"12345\"\n" +
                "   }\n" +
                "}");
        assertThat("endpoint template versionInfo", endpointTemplateFromJSONString.getVersion().getInfo(), nullValue());
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsVersionList() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template versionList", endpointTemplateFromJSONString.getVersion().getList(), equalTo("https://compute.north.public.com/versionList"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSONAndNoVersionList_setsNullVersionList() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString("{\n" +
                "   \"OS-KSCATALOG:endpointTemplate\":{\n" +
                "   \"versionId\" : \"12345\"\n" +
                "   }\n" +
                "}");
        assertThat("endpoint template versionList", endpointTemplateFromJSONString.getVersion().getList(), nullValue());
    }
}
