package com.rackspace.idm.api.converter.cloudv20;

import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.ServiceForCatalog;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/15/12
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpenStackServiceCatalogHelperTest {
    ServiceCatalog serviceCatalog;
    OpenStackServiceCatalogHelper openStackServiceCatalogHelper;
    OpenStackServiceCatalogHelper spy;

    @Before
    public void setUp() throws Exception {
        serviceCatalog = mock(ServiceCatalog.class);
        openStackServiceCatalogHelper = new OpenStackServiceCatalogHelper(serviceCatalog);

        spy = spy(openStackServiceCatalogHelper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void openStackServiceCatalogHelper_withNullServiceCatalog_throwsIllegalArgumentException() throws Exception {
        new OpenStackServiceCatalogHelper(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getService_withNullServiceName_throwsIllegalArgumentException() throws Exception {
        new OpenStackServiceCatalogHelper(serviceCatalog).getService(null);
    }

    @Test
    public void getService_withServiceInServiceCatalog_returnsCorrectService() throws Exception {
        ArrayList<ServiceForCatalog> serviceForCatalogs = new ArrayList<ServiceForCatalog>();
        ServiceForCatalog serviceForCatalog = new ServiceForCatalog();
        serviceForCatalog.setName("serviceName");
        serviceForCatalogs.add(new ServiceForCatalog());
        serviceForCatalogs.add(new ServiceForCatalog());
        serviceForCatalogs.add(serviceForCatalog);
        serviceForCatalogs.add(new ServiceForCatalog());
        when(serviceCatalog.getService()).thenReturn(serviceForCatalogs);

        ServiceForCatalog serviceFromCatalog = new OpenStackServiceCatalogHelper(serviceCatalog).getService("serviceName");
        assertThat("service returned", serviceFromCatalog, equalTo(serviceForCatalog));
    }

    @Test
    public void getService_withServiceNotInCatalog_returnsNull() throws Exception {
        when(serviceCatalog.getService()).thenReturn(new ArrayList<ServiceForCatalog>());
        ServiceForCatalog serviceFromCatalog = new OpenStackServiceCatalogHelper(serviceCatalog).getService("serviceName");
        assertThat("service returned", serviceFromCatalog, nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getEndPointService_withNullEndpointName_throwsIllegalArgumentException() throws Exception {
        new OpenStackServiceCatalogHelper(serviceCatalog).getEndPointService("ServiceName", null);
    }

    @Test
    public void getEndPointService_withServiceNotInCatalog_returnsNewService() throws Exception {
        when(serviceCatalog.getService()).thenReturn(new ArrayList<ServiceForCatalog>());
        ServiceForCatalog endPointService = new OpenStackServiceCatalogHelper(serviceCatalog).getEndPointService("ServiceName", "endpointService");
        assertThat("endpoint service", endPointService, not(nullValue()));
    }

    @Test
    public void getEndPointService_withServiceNotInCatalog_setsServicesFields() throws Exception {
        when(serviceCatalog.getService()).thenReturn(new ArrayList<ServiceForCatalog>());
        ServiceForCatalog endPointService = new OpenStackServiceCatalogHelper(serviceCatalog).getEndPointService("serviceName", "serviceType");
        assertThat("endpoint service name", endPointService.getName(), equalTo("serviceName"));
        assertThat("endpoint service type", endPointService.getType(), equalTo("serviceType"));
    }

    @Test
    public void getEndPointService_withServiceNotInCatalog_addsServiceToCatalog() throws Exception {
        ArrayList<ServiceForCatalog> serviceForCatalogs = new ArrayList<ServiceForCatalog>();
        when(serviceCatalog.getService()).thenReturn(serviceForCatalogs);

        ServiceForCatalog endPointService = new OpenStackServiceCatalogHelper(serviceCatalog).getEndPointService("serviceName", "serviceType");
        assertThat("service catalog size grew", serviceForCatalogs.size(), equalTo(1));
        assertThat("service catalog contains endpoint", serviceForCatalogs.get(0), equalTo(endPointService));
    }

    @Test
    public void getEndPointService_withServiceInCatalog_returnsExistingService() throws Exception {
        ArrayList<ServiceForCatalog> serviceForCatalogs = new ArrayList<ServiceForCatalog>();
        ServiceForCatalog serviceForCatalog = new ServiceForCatalog();
        serviceForCatalog.setName("serviceName");
        serviceForCatalogs.add(serviceForCatalog);
        when(serviceCatalog.getService()).thenReturn(serviceForCatalogs);

        ServiceForCatalog endPointService = new OpenStackServiceCatalogHelper(serviceCatalog).getEndPointService("serviceName", "serviceType");
        assertThat("service", endPointService, equalTo(serviceForCatalog));
    }

    @Test
    public void contains_returnsFalse() throws Exception {
        Boolean result = openStackServiceCatalogHelper.contains("test");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void contains_returnsTrue() throws Exception {
        doReturn(new ServiceForCatalog()).when(spy).getService("test");
        Boolean result = spy.contains("test");
        assertThat("boolean", result, equalTo(true));
    }
}
