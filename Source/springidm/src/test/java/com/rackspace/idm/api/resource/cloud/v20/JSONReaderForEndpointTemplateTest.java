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
                "       \"tenantId\":\"1\",\n" +
                "       \"region\":\"North\",\n" +
                "       \"type\":\"compute\",\n" +
                "       \"publicURL\":\"https://compute.north.public.com/v1\",\n" +
                "       \"internalURL\":\"https://compute.north.internal.com/v1\",\n" +
                "       \"adminURL\" : \"https://compute.north.admin.com/v1\",\n" +
                "       \"versionId\":\"1\",\n" +
                "       \"versionInfo\":\"https://compute.north.public.com/v1/\",\n" +
                "       \"versionList\":\"https://compute.north.public.com/versionList\"\n" +
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
    public void getEndpointTemplateFromJSONString_withValidJSON_setsEndpointId() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template id", endpointTemplateFromJSONString.getId(), equalTo(1));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsRegion() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template region", endpointTemplateFromJSONString.getRegion(), equalTo("North"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsType() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template type", endpointTemplateFromJSONString.getType(), equalTo("compute"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsPublicURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template publicURL", endpointTemplateFromJSONString.getPublicURL(), equalTo("https://compute.north.public.com/v1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsInternalURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template internalURL", endpointTemplateFromJSONString.getInternalURL(), equalTo("https://compute.north.internal.com/v1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsAdminURL() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template adminURL", endpointTemplateFromJSONString.getAdminURL(), equalTo("https://compute.north.admin.com/v1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsVersionId() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template versionId", endpointTemplateFromJSONString.getVersion().getId(), equalTo("1"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsVersionInfo() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template versionInfo", endpointTemplateFromJSONString.getVersion().getInfo(), equalTo("https://compute.north.public.com/v1/"));
    }

    @Test
    public void getEndpointTemplateFromJSONString_withValidJSON_setsVersionList() throws Exception {
        EndpointTemplate endpointTemplateFromJSONString = JSONReaderForEndpointTemplate.getEndpointTemplateFromJSONString(endpointTemplateJSON);
        assertThat("endpoint template versionList", endpointTemplateFromJSONString.getVersion().getList(), equalTo("https://compute.north.public.com/versionList"));
    }
}
