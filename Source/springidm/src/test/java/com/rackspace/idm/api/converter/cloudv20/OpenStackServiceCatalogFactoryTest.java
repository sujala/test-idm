package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.ServiceForCatalog;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/14/12
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpenStackServiceCatalogFactoryTest {

    OpenStackServiceCatalogFactory openStackServiceCatalogFactory;

    @Before
    public void setUp() throws Exception {
        openStackServiceCatalogFactory = new OpenStackServiceCatalogFactory();
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNew_withNullList_throwsIllegalArgumentException() throws Exception {
        openStackServiceCatalogFactory.createNew(null);
    }

    @Test
    public void createNew_withList_ReturnsServiceCatalog_withRightSize() throws Exception {
        List<OpenstackEndpoint> endpointList = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        openstackEndpoint.setTenantName("tenantName");
        endpointList.add(openstackEndpoint);
        endpointList.add(openstackEndpoint);

        ServiceCatalog serviceCatalog = openStackServiceCatalogFactory.createNew(endpointList);
        assertThat("service catalog", serviceCatalog.getService().get(0).getEndpoint().size(), equalTo(2));
    }

    @Test
    public void processEndpoint_withEndpoint_WithNoBaseUrls_doesNotGetService() throws Exception {
        OpenstackEndpoint endPoint = new OpenstackEndpoint();
        endPoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        ServiceCatalog serviceCatalog = mock(ServiceCatalog.class);
        OpenStackServiceCatalogFactory.processEndpoint(serviceCatalog, endPoint);
        verify(serviceCatalog, never()).getService();
    }

    @Test
    public void processEndpoint_withEndpoint_AddsEndpointToService() throws Exception {
        OpenstackEndpoint endPoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        baseUrls.add(cloudBaseUrl);
        endPoint.setBaseUrls(baseUrls);

        ServiceCatalog serviceCatalog = mock(ServiceCatalog.class);
        when(serviceCatalog.getService()).thenReturn(new ArrayList<ServiceForCatalog>());

        OpenStackServiceCatalogFactory.processEndpoint(serviceCatalog, endPoint);
        assertThat("endpoint", serviceCatalog.getService().size(), equalTo(1));
    }
}
