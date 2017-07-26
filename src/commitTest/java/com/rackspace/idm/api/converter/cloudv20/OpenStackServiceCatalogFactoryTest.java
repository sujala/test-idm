package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.EndpointForService;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.ServiceForCatalog;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

//TODO: Refactor this test class as the functionality of openstackservicecatalog was combined with EndpointConvertor.
// Need to refactor how endpoints are converted to responses
public class OpenStackServiceCatalogFactoryTest {

    EndpointConverterCloudV20 openStackServiceCatalogFactory;
    ServiceCatalog serviceCatalog;

    private Configuration reloadableConfiguration;
    private Configuration staticConfiguration;
    private IdentityConfig identityConfig;

    @Before
    public void setUp() throws Exception {
        reloadableConfiguration = new PropertiesConfiguration();
        staticConfiguration = new PropertiesConfiguration();
        identityConfig = new IdentityConfig(staticConfiguration, reloadableConfiguration);

        openStackServiceCatalogFactory = new EndpointConverterCloudV20();
        openStackServiceCatalogFactory.setIdentityConfig(identityConfig);

        reloadableConfiguration.setProperty(IdentityConfig.ENDPOINT_REGIONID_DEFAULT, "DEFAULT");

        serviceCatalog = mock(ServiceCatalog.class);
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
        cloudBaseUrl.setV1Default(true);
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setOpenstackType("endpointType");
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        openstackEndpoint.setTenantName("tenantName");
        endpointList.add(openstackEndpoint);
        endpointList.add(openstackEndpoint);

        ServiceCatalog serviceCatalog = openStackServiceCatalogFactory.createNew(endpointList);
        assertThat("service catalog", serviceCatalog.getService().get(0).getEndpoint().size(), equalTo(2));
    }

    @Test
    public void processEndpoint_withEndpoint_WithNoBaseUrls_doesChangeAnyService() throws Exception {
        OpenstackEndpoint endPoint = new OpenstackEndpoint();
        endPoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        ServiceCatalog serviceCatalog = mock(ServiceCatalog.class);
        openStackServiceCatalogFactory.processEndpoint(serviceCatalog, endPoint);
        verify(serviceCatalog, never()).getService();
    }

    @Test
    public void processEndpoint_withEndpoint_AddsEndpointsToService() throws Exception {
        OpenstackEndpoint endPoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setV1Default(false);
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setOpenstackType("endpointType");
        baseUrls.add(cloudBaseUrl);
        baseUrls.add(cloudBaseUrl);
        endPoint.setBaseUrls(baseUrls);

        when(serviceCatalog.getService()).thenReturn(new ArrayList<ServiceForCatalog>());

        openStackServiceCatalogFactory.processEndpoint(serviceCatalog, endPoint);
        assertThat("service name", serviceCatalog.getService().get(0).getName(), equalTo("serviceName"));
        assertThat("endpoint list size", serviceCatalog.getService().get(0).getEndpoint().size(), equalTo(2));
    }

    @Test
    public void processEndpoint_withEndpoint_setsFields() throws Exception {
        OpenstackEndpoint endPoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setOpenstackType("endpointType");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setV1Default(true);
        baseUrls.add(cloudBaseUrl);
        endPoint.setBaseUrls(baseUrls);
        endPoint.setTenantId("tenantId");

        when(serviceCatalog.getService()).thenReturn(new ArrayList<ServiceForCatalog>());
        openStackServiceCatalogFactory.processEndpoint(serviceCatalog, endPoint);
        EndpointForService endpointForService = serviceCatalog.getService().get(0).getEndpoint().get(0);
        assertThat("version id", endpointForService.getVersion().getId(), equalTo("versionId"));
        assertThat("version list", endpointForService.getVersion().getList(), equalTo("versionList"));
        assertThat("version info", endpointForService.getVersion().getInfo(), equalTo("versionInfo"));
        assertThat("admin url", endpointForService.getAdminURL(), equalTo("adminUrl"));
        assertThat("internal url", endpointForService.getInternalURL(), equalTo("internalUrl"));
        assertThat("public url", endpointForService.getPublicURL(), equalTo("publicUrl"));
        assertThat("region", endpointForService.getRegion(), equalTo("region"));
        assertThat("tenant id", endpointForService.getTenantId(), equalTo("tenantId"));
    }


}
