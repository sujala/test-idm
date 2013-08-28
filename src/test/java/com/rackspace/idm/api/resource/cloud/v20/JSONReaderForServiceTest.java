package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForOsKsAdmService;
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
            "\"OS-KSADM:service\" : {" +
            "}" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForOsKsAdmService jsonReaderForService = new JSONReaderForOsKsAdmService();
        boolean readable = jsonReaderForService.isReadable(Service.class, null, null, null);
        assertThat("Readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForOsKsAdmService jsonReaderForService = new JSONReaderForOsKsAdmService();
        boolean readable = jsonReaderForService.isReadable(Object.class, null, null, null);
        assertThat("Readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnsService() throws Exception {
        JSONReaderForOsKsAdmService jsonReaderForService = new JSONReaderForOsKsAdmService();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(serviceJSON.getBytes()));
        Service service = jsonReaderForService.readFrom(Service.class, null, null, null, null, inputStream);
        assertThat("service returned", service, is(Service.class));
        assertThat("service id", service.getId(), equalTo("serviceId"));
    }
}
