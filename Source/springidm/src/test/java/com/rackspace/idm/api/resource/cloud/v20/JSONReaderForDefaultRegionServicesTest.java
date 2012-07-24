package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import org.hamcrest.core.IsNull;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;
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
        DefaultRegionServices defaultRegionServicesFromJSONString = JSONReaderForDefaultRegionServices.getDefaultRegionServicesFromJSONString(jsonBody);
        assertThat("list", defaultRegionServicesFromJSONString.getServiceName(), IsNull.notNullValue());
        assertThat("list size", defaultRegionServicesFromJSONString.getServiceName().size(), equalTo(2));
        assertThat("service", defaultRegionServicesFromJSONString.getServiceName().get(0), notNullValue());
        assertThat("service", defaultRegionServicesFromJSONString.getServiceName().get(1), notNullValue());
    }

}
