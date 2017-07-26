package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForOsKsCatalogEndpointTemplate;
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

    JSONReaderForOsKsCatalogEndpointTemplate jsonReaderForEndpointTemplate;
    String endpointTemplateJSON = "{\n" +
                "   \"OS-KSCATALOG:endpointTemplate\":{\n" +
                "       \"id\":1,\n" +
                "       \"region\":\"North\",\n" +
                "       \"type\":\"compute\",\n" +
                "       \"name\": \"name\",\n" +
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
        jsonReaderForEndpointTemplate = new JSONReaderForOsKsCatalogEndpointTemplate();
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

}
