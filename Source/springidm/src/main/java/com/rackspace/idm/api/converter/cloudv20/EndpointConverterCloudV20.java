package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.VersionForService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.List;

@Component
public class EndpointConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    private OpenStackServiceCatalogFactory sf = new OpenStackServiceCatalogFactory();

    public ServiceCatalog toServiceCatalog(List<OpenstackEndpoint> endpoints) {
        ServiceCatalog catalog = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createServiceCatalog();

        if (endpoints == null || endpoints.size() == 0) {
            return catalog;
        }

        catalog = sf.createNew(endpoints);

        return catalog;
    }

    public EndpointList toEndpointList(List<OpenstackEndpoint> endpoints) {
        EndpointList list = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createEndpointList();

        if (endpoints == null || endpoints.size() == 0) {
            return list;
        }

        for (OpenstackEndpoint point : endpoints) {
            for (CloudBaseUrl baseUrl : point.getBaseUrls()) {
                VersionForService version = new VersionForService();
                version.setId(baseUrl.getVersionId());
                version.setInfo(baseUrl.getVersionInfo());
                version.setList(baseUrl.getVersionList());

                Endpoint endpoint = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint();
                endpoint.setAdminURL(baseUrl.getAdminUrl());
                endpoint.setId(baseUrl.getBaseUrlId());     //TODO: throws null pointer of Id is not set.
                endpoint.setInternalURL(baseUrl.getInternalUrl());
                endpoint.setName(baseUrl.getServiceName());
                endpoint.setPublicURL(baseUrl.getPublicUrl());
                endpoint.setRegion(baseUrl.getRegion());
                endpoint.setType(baseUrl.getOpenstackType());
                endpoint.setTenantId(point.getTenantId());
                if (!StringUtils.isBlank(version.getId())) {
                    endpoint.setVersion(version);
                }
                list.getEndpoint().add(endpoint);
            }
        }
        return list;
    }

    public EndpointList toEndpointListFromBaseUrls(List<CloudBaseUrl> endpoints) {
        EndpointList list = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createEndpointList();

        if (endpoints == null || endpoints.size() == 0) {
            return list;
        }

        for (CloudBaseUrl baseUrl : endpoints) {
            VersionForService version = new VersionForService();
            version.setId(baseUrl.getVersionId());
            version.setInfo(baseUrl.getVersionInfo());
            version.setList(baseUrl.getVersionList());

            Endpoint endpoint = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint();
            endpoint.setAdminURL(baseUrl.getAdminUrl());
            endpoint.setId(baseUrl.getBaseUrlId());     //TODO: throws null pointer of Id is not set. Only going from Endpoints to BaseUrls throws the NPE
            endpoint.setInternalURL(baseUrl.getInternalUrl());
            endpoint.setName(baseUrl.getServiceName());
            endpoint.setPublicURL(baseUrl.getPublicUrl());
            endpoint.setRegion(baseUrl.getRegion());
            endpoint.setType(baseUrl.getOpenstackType());
            if (!StringUtils.isBlank(version.getId())) {
                endpoint.setVersion(version);
            }
            list.getEndpoint().add(endpoint);

        }
        return list;
    }

    public EndpointTemplate toEndpointTemplate(CloudBaseUrl baseUrl) {

        VersionForService version = new VersionForService();
        version.setId(baseUrl.getVersionId());
        version.setInfo(baseUrl.getVersionInfo());
        version.setList(baseUrl.getVersionList());

        EndpointTemplate template = OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory().createEndpointTemplate();
        template.setAdminURL(baseUrl.getAdminUrl());
        template.setEnabled(baseUrl.getEnabled());
        template.setGlobal(baseUrl.getGlobal());
        template.setId(baseUrl.getBaseUrlId());
        template.setInternalURL(baseUrl.getInternalUrl());
        template.setName(baseUrl.getServiceName());
        template.setPublicURL(baseUrl.getPublicUrl());
        template.setRegion(baseUrl.getRegion());
        template.setType(baseUrl.getOpenstackType());

        if (!StringUtils.isBlank(version.getId())) {
            template.setVersion(version);
        }
        return template;
    }

    public EndpointTemplateList toEndpointTemplateList(
        List<CloudBaseUrl> baseUrls) {
        EndpointTemplateList list = OBJ_FACTORIES
            .getOpenStackIdentityExtKscatalogV1Factory()
            .createEndpointTemplateList();

        if (baseUrls == null || baseUrls.size() == 0) {
            return list;
        }

        for (CloudBaseUrl baseUrl : baseUrls) {
            list.getEndpointTemplate().add(toEndpointTemplate(baseUrl));
        }

        return list;
    }

    public Endpoint toEndpoint(CloudBaseUrl baseUrl) {
        VersionForService version = new VersionForService();
        version.setId(baseUrl.getVersionId());
        version.setInfo(baseUrl.getVersionInfo());
        version.setList(baseUrl.getVersionList());

        Endpoint endpoint = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint();
        endpoint.setAdminURL(baseUrl.getAdminUrl());
        endpoint.setId(baseUrl.getBaseUrlId());
        endpoint.setInternalURL(baseUrl.getInternalUrl());
        endpoint.setName(baseUrl.getServiceName());
        endpoint.setPublicURL(baseUrl.getPublicUrl());
        endpoint.setRegion(baseUrl.getRegion());
        endpoint.setType(baseUrl.getOpenstackType());
        if (!StringUtils.isBlank(version.getId())) {
            endpoint.setVersion(version);
        }
        return endpoint;
    }
    
    public CloudBaseUrl toCloudBaseUrl(EndpointTemplate template) {
        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.setAdminUrl(template.getAdminURL());
        baseUrl.setBaseUrlId(template.getId());
        baseUrl.setEnabled(template.isEnabled());
        baseUrl.setGlobal(template.isGlobal());
        baseUrl.setInternalUrl(template.getInternalURL());
        baseUrl.setServiceName(template.getName());
        baseUrl.setOpenstackType(template.getType());
        baseUrl.setPublicUrl(template.getPublicURL());
        baseUrl.setRegion(template.getRegion());
        if (template.getVersion() != null) {
            baseUrl.setVersionId(template.getVersion().getId());
            baseUrl.setVersionInfo(template.getVersion().getInfo());
            baseUrl.setVersionList(template.getVersion().getList());
        }
        return baseUrl;
    }

    public void setOBJ_FACTORIES(JAXBObjectFactories OBJ_FACTORIES) {
        this.OBJ_FACTORIES = OBJ_FACTORIES;
    }

    public void setSf(OpenStackServiceCatalogFactory sf) {
        this.sf = sf;
    }
}
