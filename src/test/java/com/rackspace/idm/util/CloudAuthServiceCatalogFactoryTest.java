package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/3/12
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudAuthServiceCatalogFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void createNew_withNullList_throwsIllegalArgumentException() throws Exception {
            new CloudAuthServiceCatalogFactory().createNew(null);
    }


    @Test
    public void processService_withNullBaseUrls_DoesNotModifyCatalog() throws Exception {
        ServiceCatalog serviceCatalog = mock(ServiceCatalog.class);
        CloudAuthServiceCatalogFactory.processService(serviceCatalog, new OpenstackEndpoint());
        verify(serviceCatalog, never()).getService();
    }

    @Test
    public void processService_withService_callsAddEndpoint() throws Exception {
        ServiceCatalog serviceCatalog = mock(ServiceCatalog.class);
        ArrayList<Service> services = new ArrayList<Service>();
        Service service = new Service();
        service.setName("serviceName");
        services.add(service);
        when(serviceCatalog.getService()).thenReturn(services);
        OpenstackEndpoint openstackEndPoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        baseUrls.add(cloudBaseUrl);
        openstackEndPoint.setBaseUrls(baseUrls);
        CloudAuthServiceCatalogFactory.processService(serviceCatalog, openstackEndPoint);
        assertThat("services size", services.size(), equalTo(1));
    }

    @Test
    public void getServiceFromName_nameDoesNotMatch_returnsNull() throws Exception {
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        service.setName("notMatch");
        List<Service> serviceList = serviceCatalog.getService();
        serviceList.add(service);
        Service result = CloudAuthServiceCatalogFactory.getServiceFromName(serviceCatalog, "service");
        assertThat("service", result, equalTo(null));
    }
}
