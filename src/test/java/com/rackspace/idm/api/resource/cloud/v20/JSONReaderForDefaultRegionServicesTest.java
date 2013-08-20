package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthDefaultRegionServices;
import com.rackspace.idm.exception.BadRequestException;
import org.hamcrest.core.IsNull;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Hector
 * Date: 7/24/12
 * Time: 1:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForDefaultRegionServicesTest {

    String jsonBody = "{\n" +
            "  \"RAX-AUTH:defaultRegionServices\": [\n" +
            "    \"cloudFiles\",\n" +
            "    \"openstackNova\"\n" +
            "  ]\n" +
            "}";

    @Test
    public void getDefaultRegionServicesFromJSONString() throws Exception {
        DefaultRegionServices defaultRegionServicesFromJSONString = JSONReaderForRaxAuthDefaultRegionServices.getDefaultRegionServicesFromJSONString(jsonBody);
        assertThat("list", defaultRegionServicesFromJSONString.getServiceName(), IsNull.notNullValue());
        assertThat("list size", defaultRegionServicesFromJSONString.getServiceName().size(), equalTo(2));
        assertThat("service", defaultRegionServicesFromJSONString.getServiceName().get(0), notNullValue());
        assertThat("service", defaultRegionServicesFromJSONString.getServiceName().get(1), notNullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getDefaultRegionServicesFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForRaxAuthDefaultRegionServices.getDefaultRegionServicesFromJSONString("invalid JSON");
    }

    @Test
    public void isReadable_typeNotDefaultRegionServicesClass_returnsFalse() throws Exception {
        JSONReaderForRaxAuthDefaultRegionServices jsonReaderForDefaultRegionServices = new JSONReaderForRaxAuthDefaultRegionServices();
        boolean result = jsonReaderForDefaultRegionServices.isReadable(DefaultCloud20Service.class, null, null, null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void isReadable_typeIsDefaultRegionServicesClass_returnsTrue() throws Exception {
        JSONReaderForRaxAuthDefaultRegionServices jsonReaderForDefaultRegionServices = new JSONReaderForRaxAuthDefaultRegionServices();
        boolean result = jsonReaderForDefaultRegionServices.isReadable(DefaultRegionServices.class, null, null, null);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void readFrom_returnsObject() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(jsonBody.getBytes()));
        JSONReaderForRaxAuthDefaultRegionServices jsonReaderForDefaultRegionServices = new JSONReaderForRaxAuthDefaultRegionServices();
        DefaultRegionServices result = jsonReaderForDefaultRegionServices.readFrom(DefaultRegionServices.class, null, null, null, null, inputStream);
        assertThat("list", result.getServiceName(), IsNull.notNullValue());
        assertThat("list size", result.getServiceName().size(), equalTo(2));
        assertThat("service", result.getServiceName().get(0), notNullValue());
        assertThat("service", result.getServiceName().get(1), notNullValue());
    }
}
