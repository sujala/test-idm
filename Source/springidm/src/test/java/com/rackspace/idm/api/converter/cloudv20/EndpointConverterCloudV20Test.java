package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.ServiceCatalog;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/13/12
 * Time: 1:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class EndpointConverterCloudV20Test {
    EndpointConverterCloudV20 endpointConverterCloudV20;
    OpenStackServiceCatalogFactory openStackServiceCatalogFactory;

    @Before
    public void setUp() throws Exception {
        openStackServiceCatalogFactory = mock(OpenStackServiceCatalogFactory.class);
        endpointConverterCloudV20 = new EndpointConverterCloudV20();
        endpointConverterCloudV20.setOBJ_FACTORIES(new JAXBObjectFactories());
        endpointConverterCloudV20.setSf(openStackServiceCatalogFactory);
    }

    @Test
    public void toServiceCatalog_withNullList_returnsEmptyServiceCatalog() throws Exception {
        ServiceCatalog serviceCatalog = endpointConverterCloudV20.toServiceCatalog(null);
        assertThat("service catalog", serviceCatalog.getAny().size(), equalTo(0));
    }

    @Test
    public void toServiceCatalog_withEmptylList_returnsEmptyServiceCatalog() throws Exception {
        ServiceCatalog serviceCatalog = endpointConverterCloudV20.toServiceCatalog(new ArrayList<OpenstackEndpoint>());
        assertThat("service catalog", serviceCatalog.getAny().size(), equalTo(0));
    }

    @Test
    public void toServiceCatalog_withEndpointList_callsOpenStackServiceCatalogFactory_createNew() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        endpoints.add(new OpenstackEndpoint());
        endpoints.add(new OpenstackEndpoint());
        endpointConverterCloudV20.toServiceCatalog(endpoints);
        verify(openStackServiceCatalogFactory).createNew(endpoints);
    }

    @Test
    public void toEndpointList_withNull_returnsEmptyEndpointList() throws Exception {
        EndpointList endpointList = endpointConverterCloudV20.toEndpointList(null);
        assertThat("endpoing list", endpointList.getAny().size(), equalTo(0));
    }

    @Test
    public void toEndpointList_withEmptyList_returnsEmptyEndpointList() throws Exception {
        EndpointList endpointList = endpointConverterCloudV20.toEndpointList(new ArrayList<OpenstackEndpoint>());
        assertThat("endpoing list", endpointList.getAny().size(), equalTo(0));
    }

    @Test
    public void toEndpointList_withList_returnsEndpointList_withCorrectSize() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(123456);
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        endpoints.add(openstackEndpoint);
        endpoints.add(openstackEndpoint);
        EndpointList endpointList = endpointConverterCloudV20.toEndpointList(endpoints);
        assertThat("endpoint list", endpointList.getEndpoint().size(), equalTo(2));
    }

    @Test
    public void toEndpointList_withListOfEndpoints_withNoBaseUrls_returnsEmptyList() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        endpoints.add(openstackEndpoint);
        endpoints.add(openstackEndpoint);
        EndpointList endpointList = endpointConverterCloudV20.toEndpointList(endpoints);
        assertThat("endpoint list", endpointList.getEndpoint().size(), equalTo(0));
    }

    @Test
    public void toEndpointList_withList_setsEndpointFields() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123456);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setOpenstackType("openStackType");
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        openstackEndpoint.setTenantId("tenantId");
        endpoints.add(openstackEndpoint);

        EndpointList endpointList = endpointConverterCloudV20.toEndpointList(endpoints);

        Endpoint endpoint = endpointList.getEndpoint().get(0);
        assertThat("endpoint version id", endpoint.getVersion().getId(), equalTo("versionId"));
        assertThat("endpoint version info", endpoint.getVersion().getInfo(), equalTo("versionInfo"));
        assertThat("endpoint version list", endpoint.getVersion().getList(), equalTo("versionList"));
        assertThat("endpoint admin url", endpoint.getAdminURL(), equalTo("adminUrl"));
        assertThat("endpoint admin url", endpoint.getId(), equalTo(123456));
        assertThat("endpoint admin url", endpoint.getInternalURL(), equalTo("internalUrl"));
        assertThat("endpoint admin url", endpoint.getName(), equalTo("serviceName"));
        assertThat("endpoint admin url", endpoint.getPublicURL(), equalTo("publicUrl"));
        assertThat("endpoint admin url", endpoint.getRegion(), equalTo("region"));
        assertThat("endpoint admin url", endpoint.getType(), equalTo("openStackType"));
        assertThat("endpoint admin url", endpoint.getTenantId(), equalTo("tenantId"));
    }

    @Test
    public void toEndpointList_withList_withNoVersionId_doesNotSetVersionFields() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setBaseUrlId(123456);
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        endpoints.add(openstackEndpoint);

        EndpointList endpointList = endpointConverterCloudV20.toEndpointList(endpoints);

        Endpoint endpoint = endpointList.getEndpoint().get(0);
        assertThat("endpoint version", endpoint.getVersion(), nullValue());
    }

}
