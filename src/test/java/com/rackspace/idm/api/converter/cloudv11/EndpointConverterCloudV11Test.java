package com.rackspace.idm.api.converter.cloudv11;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/13/12
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class EndpointConverterCloudV11Test {
    private EndpointConverterCloudV11 endpointConverterCloudV11;
    private Configuration config;
    private CloudBaseUrl cloudBaseUrl;
    private BaseURL baseURL;

    @Before
    public void setUp() throws Exception {
        config = mock(Configuration.class);
        endpointConverterCloudV11 = new EndpointConverterCloudV11(config);
        cloudBaseUrl = new CloudBaseUrl();
        baseURL = new BaseURL();

        // Fields
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("nast");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setUniqueId("uniqueId");

        baseURL.setAdminURL("adminUrl");
        baseURL.setDefault(true);
        baseURL.setEnabled(true);
        baseURL.setId(1);
        baseURL.setInternalURL("internalUrl");
        baseURL.setPublicURL("publicUrl");
        baseURL.setRegion("region");
        baseURL.setServiceName("serviceName");
    }

    @Test
    public void toBaseUrl_urlIsNull_returnsNull() throws Exception {
        BaseURL url = endpointConverterCloudV11.toBaseUrl(null);
        assertThat("url", url, equalTo(null));
    }

    @Test
    public void toBaseUrl_returnsBaseUrl_succeeds() throws Exception {
        BaseURL url = endpointConverterCloudV11.toBaseUrl(cloudBaseUrl);
        assertThat("id", url.getId(), equalTo(1));
        assertThat("enabled", url.isEnabled(), equalTo(true));
        assertThat("admin url", url.getAdminURL(), equalTo("adminUrl"));
        assertThat("default", url.isDefault(), equalTo(true));
        assertThat("internal url", url.getInternalURL(), equalTo("internalUrl"));
        assertThat("public url", url.getPublicURL(), equalTo("publicUrl"));
        assertThat("region", url.getRegion(),equalTo("region"));
        assertThat("service name", url.getServiceName(), equalTo("serviceName"));
        assertThat("url type", url.getUserType().value(), equalTo("NAST"));
    }

    @Test
    public void toBaseUrl_baseUrlTypeIsNull_returnsBaseUrl() throws Exception {
        cloudBaseUrl.setBaseUrlType(null);
        BaseURL url = endpointConverterCloudV11.toBaseUrl(cloudBaseUrl);
        assertThat("id", url.getId(), equalTo(1));
        assertThat("enabled", url.isEnabled(), equalTo(true));
        assertThat("admin url", url.getAdminURL(), equalTo("adminUrl"));
        assertThat("default", url.isDefault(), equalTo(true));
        assertThat("internal url", url.getInternalURL(), equalTo("internalUrl"));
        assertThat("public url", url.getPublicURL(), equalTo("publicUrl"));
        assertThat("region", url.getRegion(),equalTo("region"));
        assertThat("service name", url.getServiceName(), equalTo("serviceName"));
        assertThat("url type", url.getUserType(), equalTo(null));
    }

    @Test
    public void toBaseUrlDO_baseUrlIsNull_returnsNull() throws Exception {
        CloudBaseUrl url = endpointConverterCloudV11.toBaseUrlDO(null);
        assertThat("url", url, equalTo(null));
    }

    @Test
    public void toBaseUrlDO_returnsCloudBaseUrl_succeeds() throws Exception {
        baseURL.setUserType(Enum.valueOf(UserType.class, "NAST"));
        CloudBaseUrl url = endpointConverterCloudV11.toBaseUrlDO(baseURL);
        assertThat("id", url.getBaseUrlId(), equalTo(1));
        assertThat("default", url.getDef(), equalTo(true));
        assertThat("enabled", url.getEnabled(), equalTo(true));
        assertThat("admin url", url.getAdminUrl(), equalTo("adminUrl"));
        assertThat("internal url", url.getInternalUrl(), equalTo("internalUrl"));
        assertThat("public url", url.getPublicUrl(), equalTo("publicUrl"));
        assertThat("region", url.getRegion(),equalTo("region"));
        assertThat("service name", url.getServiceName(), equalTo("serviceName"));
        assertThat("url type", url.getBaseUrlType(), equalTo("NAST"));
    }

    @Test
    public void toBaseUrlDO_urlUserTypeIsNull_returnsUrl() throws Exception {
        CloudBaseUrl url = endpointConverterCloudV11.toBaseUrlDO(baseURL);
        assertThat("id", url.getBaseUrlId(), equalTo(1));
        assertThat("default", url.getDef(), equalTo(true));
        assertThat("enabled", url.getEnabled(), equalTo(true));
        assertThat("admin url", url.getAdminUrl(), equalTo("adminUrl"));
        assertThat("internal url", url.getInternalUrl(), equalTo("internalUrl"));
        assertThat("public url", url.getPublicUrl(), equalTo("publicUrl"));
        assertThat("region", url.getRegion(),equalTo("region"));
        assertThat("service name", url.getServiceName(), equalTo("serviceName"));
        assertThat("url type", url.getBaseUrlType(), equalTo(null));
    }

    @Test
    public void toBaseUrlRef_CloudEndpointIsNull_returnsNull() throws Exception {
        CloudEndpoint cloudEndpoint = null;
        BaseURLRef ref = endpointConverterCloudV11.toBaseUrlRef(cloudEndpoint);
        assertThat("ref", ref, equalTo(null));
    }

    @Test
    public void toBaseUrlRef_CloudEndpointBaseUrlIsNull_returnsNull() throws Exception {
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        BaseURLRef ref = endpointConverterCloudV11.toBaseUrlRef(cloudEndpoint);
        assertThat("ref", ref, equalTo(null));
    }

    @Test
    public void toBaseUrlRef_CloudEndpointBaseUrlNotNull_returnBaseUrlRef() throws Exception {
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        cloudEndpoint.setV1preferred(true);
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        when(config.getString("cloud.baseurl.ref.string")).thenReturn("https://idm.rackspace.com/idm/rest/cloud/v1.1/baseurls/%s");
        BaseURLRef ref = endpointConverterCloudV11.toBaseUrlRef(cloudEndpoint);
        assertThat("id", ref.getId(), equalTo(1));
        assertThat("v1default", ref.isV1Default(), equalTo(true));
        assertThat("reference string", ref.getHref(), equalTo("https://idm.rackspace.com/idm/rest/cloud/v1.1/baseurls/1"));
    }

    @Test
    public void toBaseUrlRefs_endpointsIsNull_returnsEmptyList() throws Exception {
        BaseURLRefList refList = endpointConverterCloudV11.toBaseUrlRefs(null);
        assertThat("list size", refList.getBaseURLRef().size(), equalTo(0));
    }

    @Test
    public void toBaseUrlRefs_endpointSizeIsZero_returnsEmptyList() throws Exception {
        List<CloudEndpoint> endpointList = new ArrayList<CloudEndpoint>();
        BaseURLRefList refList = endpointConverterCloudV11.toBaseUrlRefs(endpointList);
        assertThat("list size", refList.getBaseURLRef().size(), equalTo(0));
    }

    @Test
    public void toBaseUrlRefs_endpointSizeMoreThanZero_returnsListWithAddedReference() throws Exception {
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        cloudEndpoint.setV1preferred(true);
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        List<CloudEndpoint> endpointList = new ArrayList<CloudEndpoint>();
        endpointList.add(cloudEndpoint);
        when(config.getString("cloud.baseurl.ref.string")).thenReturn("https://idm.rackspace.com/idm/rest/cloud/v1.1/baseurls/%s");
        BaseURLRefList refList = endpointConverterCloudV11.toBaseUrlRefs(endpointList);
        assertThat("id", refList.getBaseURLRef().get(0).getId(), equalTo(1));
        assertThat("v1default", refList.getBaseURLRef().get(0).isV1Default(), equalTo(true));
        assertThat("reference string", refList.getBaseURLRef().get(0).getHref(), equalTo("https://idm.rackspace.com/idm/rest/cloud/v1.1/baseurls/1"));
    }

    @Test
    public void openstackToBaseUrlRefs_endpointListIsNull_returnsEmptyList() throws Exception {
        BaseURLRefList refList = endpointConverterCloudV11.openstackToBaseUrlRefs(null);
        assertThat("list size", refList.getBaseURLRef().size(), equalTo(0));
    }

    @Test
    public void openstackToBaseUrlRefs_endpointSizeIsZero_returnsEmptyList() throws Exception {
        List<OpenstackEndpoint> endpointList = new ArrayList<OpenstackEndpoint>();
        BaseURLRefList refList = endpointConverterCloudV11.openstackToBaseUrlRefs(endpointList);
        assertThat("list size", refList.getBaseURLRef().size(), equalTo(0));
    }

    @Test
    public void openstackToBaseUrlRefs_endpointSizeMoreThanZero_returnsListWithAddedReference() throws Exception {
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        List<CloudBaseUrl> urlList = new ArrayList<CloudBaseUrl>();
        List<OpenstackEndpoint> endpointList = new ArrayList<OpenstackEndpoint>();
        urlList.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(urlList);
        endpointList.add(openstackEndpoint);
        when(config.getString(anyString())).thenReturn("http://identity.api.rackspacecloud.com/v1.1/%s");
        BaseURLRefList refList = endpointConverterCloudV11.openstackToBaseUrlRefs(endpointList);
        assertThat("id", refList.getBaseURLRef().get(0).getId(), equalTo(1));
        assertThat("v1default", refList.getBaseURLRef().get(0).isV1Default(), equalTo(true));
        assertThat("reference string", refList.getBaseURLRef().get(0).getHref(), equalTo("http://identity.api.rackspacecloud.com/v1.1/1"));
    }

    @Test
    public void toBaseUrlRef_openStackEndpointIsNull_returnsEmptyList() throws Exception {
        OpenstackEndpoint openstackEndpoint = null;
        List<BaseURLRef> refList = endpointConverterCloudV11.toBaseUrlRef(openstackEndpoint);
        assertThat("list size", refList.size(), equalTo(0));
    }

    @Test
    public void toBaseUrlRef_openStackEndpointIsNotNull_returnsListWithAddedEndpointInfo() throws Exception {
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        List<CloudBaseUrl> urlList = new ArrayList<CloudBaseUrl>();
        urlList.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(urlList);
        when(config.getString(anyString())).thenReturn("http://identity.api.rackspacecloud.com/v1.1/%s");
        List<BaseURLRef> refList = endpointConverterCloudV11.toBaseUrlRef(openstackEndpoint);
        assertThat("id", refList.get(0).getId(), equalTo(1));
        assertThat("v1deafult", refList.get(0).isV1Default(), equalTo(true));
        assertThat("reference string", refList.get(0).getHref(), equalTo("http://identity.api.rackspacecloud.com/v1.1/1"));
    }

    @Test
    public void toBaseUrls_cloudBaseUrlListIsNull_returnsEmptyList() throws Exception {
        BaseURLList baseURLList = endpointConverterCloudV11.toBaseUrls(null);
        assertThat("list size", baseURLList.getBaseURL().size(), equalTo(0));
    }

    @Test
    public void toBaseUrls_cloudBaseUrlListSizeIsZero_returnsEmptyList() throws Exception {
        List<CloudBaseUrl> urlList = new ArrayList<CloudBaseUrl>();
        BaseURLList baseURLList = endpointConverterCloudV11.toBaseUrls(urlList);
        assertThat("list size", baseURLList.getBaseURL().size(), equalTo(0));
    }

    @Test
    public void toBaseUrls_cloudBaseUrlListSizeMoreThanZero_returnsListWithAddedUrls() throws Exception {
        List<CloudBaseUrl> urlList = new ArrayList<CloudBaseUrl>();
        urlList.add(cloudBaseUrl);
        BaseURLList url = endpointConverterCloudV11.toBaseUrls(urlList);
        assertThat("id", url.getBaseURL().get(0).getId(), equalTo(1));
        assertThat("enabled", url.getBaseURL().get(0).isEnabled(), equalTo(true));
        assertThat("admin url", url.getBaseURL().get(0).getAdminURL(), equalTo("adminUrl"));
        assertThat("default", url.getBaseURL().get(0).isDefault(), equalTo(true));
        assertThat("internal url", url.getBaseURL().get(0).getInternalURL(), equalTo("internalUrl"));
        assertThat("public url", url.getBaseURL().get(0).getPublicURL(), equalTo("publicUrl"));
        assertThat("region", url.getBaseURL().get(0).getRegion(),equalTo("region"));
        assertThat("service name", url.getBaseURL().get(0).getServiceName(), equalTo("serviceName"));
        assertThat("url type", url.getBaseURL().get(0).getUserType().value(), equalTo("NAST"));
    }

    @Test
    public void toServiceCatalog_cloudEndpointListIsNull_returnsEmptyServiceCatalogList() throws Exception {
        ServiceCatalog catalog = endpointConverterCloudV11.toServiceCatalog(null);
        assertThat("list size",catalog.getService().size(), equalTo(0));
    }

    @Test
    public void toServiceCatalog_cloudEndpointListSizeIsZero_returnsEmptyServiceCatalogList() throws Exception {
        List<OpenstackEndpoint> urlList = new ArrayList<OpenstackEndpoint>();
        ServiceCatalog catalog = endpointConverterCloudV11.toServiceCatalog(urlList);
        assertThat("list size",catalog.getService().size(), equalTo(0));
    }

    @Test
    public void toServiceCatalog_cloudEndpointListSizeMoreThanZero_returnsCatalogWithAddedEndpoint() throws Exception {
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setTenantId("nastId");
        openstackEndpoint.setTenantName("nastId");
        List<CloudBaseUrl> baseURLs = new ArrayList<CloudBaseUrl>();
        baseURLs.add(cloudBaseUrl);
        cloudBaseUrl.setAdminUrl(cloudBaseUrl.getAdminUrl()+"/nastId");
        cloudBaseUrl.setPublicUrl(cloudBaseUrl.getPublicUrl()+"/nastId");
        cloudBaseUrl.setInternalUrl(cloudBaseUrl.getInternalUrl()+"/nastId");
        openstackEndpoint.setBaseUrls(baseURLs);
        List<OpenstackEndpoint> urlList = new ArrayList<OpenstackEndpoint>();
        urlList.add(openstackEndpoint);
        ServiceCatalog catalog = endpointConverterCloudV11.toServiceCatalog(urlList);
        assertThat("admin url", catalog.getService().get(0).getEndpoint().get(0).getAdminURL(), equalTo("adminUrl/nastId"));
        assertThat("v1default", catalog.getService().get(0).getEndpoint().get(0).isV1Default(), equalTo(true));
        assertThat("internal url", catalog.getService().get(0).getEndpoint().get(0).getInternalURL(), equalTo("internalUrl/nastId"));
        assertThat("public url", catalog.getService().get(0).getEndpoint().get(0).getPublicURL(), equalTo("publicUrl/nastId"));
        assertThat("region", catalog.getService().get(0).getEndpoint().get(0).getRegion(), equalTo("region"));
        assertThat("service name", catalog.getService().get(0).getName(), equalTo("serviceName"));
    }
}
