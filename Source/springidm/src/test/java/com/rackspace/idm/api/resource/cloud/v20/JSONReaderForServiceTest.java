package com.rackspace.idm.api.resource.cloud.v20;

import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 8:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForServiceTest {
    String serviceJSON = "{" +
            "\"OS-KSADM:service\": {" +
            "   \"id\": \"serviceId\"," +
            "   \"name\": \"serviceName\"," +
            "   \"type\": \"serviceType\"," +
            "   \"description\": \"description\"" +
            "}" +
            "}";

    String emptyServiceJSON = "{" +
            "\"OS-KSADM:Service\" : {" +
            "}" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForService jsonReaderForService = new JSONReaderForService();
        boolean readable = jsonReaderForService.isReadable(Service.class, null, null, null);
        assertThat("Readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForService jsonReaderForService = new JSONReaderForService();
        boolean readable = jsonReaderForService.isReadable(Object.class, null, null, null);
        assertThat("Readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnsService() throws Exception {
        JSONReaderForService jsonReaderForService = new JSONReaderForService();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(serviceJSON.getBytes()));
        Service service = jsonReaderForService.readFrom(Service.class, null, null, null, null, inputStream);
        assertThat("service returned", service, is(Service.class));
        assertThat("service id", service.getId(), equalTo("serviceId"));
    }

    @Test
    public void getServiceFromJSONString_withValidJSON_returnsServiceWithName() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(serviceJSON);
        assertThat("service name", serviceFromJSONString.getName(),equalTo("serviceName") );
    }

    @Test
    public void getServiceFromJSONString_withValidJSON_returnsServiceWithDesc() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(serviceJSON);
        assertThat("service desc", serviceFromJSONString.getDescription(),equalTo("description") );
    }

    @Test
    public void getServiceFromJSONString_withValidJSON_returnsServiceWithId() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(serviceJSON);
        assertThat("service id", serviceFromJSONString.getId(),equalTo("serviceId") );
    }

    @Test
    public void getServiceFromJSONString_withValidJSON_returnsServiceWithType() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(serviceJSON);
        assertThat("service type", serviceFromJSONString.getType(),equalTo("serviceType") );
    }

    @Test
    public void getServiceFromJSONString_withEmptyServiceJSON_returnsServiceWithNullName() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(emptyServiceJSON);
        assertThat("service name", serviceFromJSONString.getName(), nullValue() );
    }

    @Test
    public void getServiceFromJSONString_withEmptyServiceJSON_returnsServiceWithNullDesc() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(emptyServiceJSON);
        assertThat("service desc", serviceFromJSONString.getDescription(), nullValue() );
    }

    @Test
    public void getServiceFromJSONString_withEmptyServiceJSON_returnsServiceWithNullId() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(emptyServiceJSON);
        assertThat("service id", serviceFromJSONString.getId(), nullValue() );
    }

    @Test
    public void getServiceFromJSONString_withEmptyServiceJSON_returnsServiceWithNullType() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString(emptyServiceJSON);
        assertThat("service type", serviceFromJSONString.getType(), nullValue() );
    }

    @Test
    public void getServiceFromJSONString_withEmptyJSON_returnServiceWithNullValues() throws Exception {
        Service serviceFromJSONString = JSONReaderForService.getServiceFromJSONString("{ }");
        assertThat("service id", serviceFromJSONString.getId(), nullValue());
        assertThat("service desc", serviceFromJSONString.getDescription(), nullValue());
        assertThat("service name", serviceFromJSONString.getName(), nullValue());
        assertThat("service type", serviceFromJSONString.getType(), nullValue());
    }
}
