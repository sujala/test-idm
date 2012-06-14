package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/13/12
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudAuthServiceCatalogHelperTest {
    private CloudAuthServiceCatalogHelper cloudAuthServiceCatalogHelper;
    private ServiceCatalog serviceCatalog;

    @Before
    public void setUp() throws Exception {
        serviceCatalog = new ServiceCatalog();
        List<Service> serviceList = serviceCatalog.getService();
        Service service = new Service();
        service.setName("serviceName");
        serviceList.add(service);
        cloudAuthServiceCatalogHelper = new CloudAuthServiceCatalogHelper(serviceCatalog);
    }

    @Test (expected = IllegalArgumentException.class)
    public void cloudAuthServiceCatalogHelper_serviceCatalogIsNull_throwsIllegalArgument() throws Exception {
        cloudAuthServiceCatalogHelper = new CloudAuthServiceCatalogHelper(null);
    }

    @Test
    public void contains_serviceNameNotMatch_returnsFalse() throws Exception {
        Boolean contains = cloudAuthServiceCatalogHelper.contains("something");
        assertThat("boolean", contains, equalTo(false));
    }

    @Test
    public void contains_serviceNameMatches_returnsTrue() throws Exception {
        Boolean contains = cloudAuthServiceCatalogHelper.contains("serviceName");
        assertThat("boolean", contains, equalTo(true));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getService_serviceNameIsNull_throwsIllegalArgument() throws Exception {
        cloudAuthServiceCatalogHelper.getService(null);
    }

    @Test
    public void getService_serviceNameMatches_returnsService() throws Exception {
        Service serviceName = cloudAuthServiceCatalogHelper.getService("serviceName");
        assertThat("service name", serviceName.getName(), equalTo("serviceName"));
    }

    @Test
    public void getService_serviceNameNotMatch_returnsServiceWithNullValue() throws Exception {
        Service serviceName = cloudAuthServiceCatalogHelper.getService("");
        assertThat("service", serviceName, equalTo(null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getEndPointService_endPointIsNull_throwsIllegalArgument() throws Exception {
        cloudAuthServiceCatalogHelper.getEndPointService(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getEndPointService_endPointBaseUrlIsNull_throwsIllegalArgument() throws Exception {
        CloudEndpoint endpoint = new CloudEndpoint();
        cloudAuthServiceCatalogHelper.getEndPointService(endpoint);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getEndPointService_serviceNameIsNull_throwsIllegalArgument() throws Exception {
        CloudEndpoint endpoint = new CloudEndpoint();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        endpoint.setBaseUrl(cloudBaseUrl);
        cloudAuthServiceCatalogHelper.getEndPointService(endpoint);
    }

    @Test
    public void getEndPointService_serviceNameMatches_returnsService() throws Exception {
        CloudEndpoint endpoint = new CloudEndpoint();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        endpoint.setBaseUrl(cloudBaseUrl);
        Service endPointService = cloudAuthServiceCatalogHelper.getEndPointService(endpoint);
        assertThat("service name", endPointService.getName(), equalTo("serviceName"));
    }

    @Test
    public void getEndPointService_serviceNameDoesNotMatch_returnsServiceWithNewServiceName() throws Exception {
        CloudEndpoint endpoint = new CloudEndpoint();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("notMatch");
        endpoint.setBaseUrl(cloudBaseUrl);
        Service endPointService = cloudAuthServiceCatalogHelper.getEndPointService(endpoint);
        assertThat("service name", endPointService.getName(), equalTo("notMatch"));
    }
}
