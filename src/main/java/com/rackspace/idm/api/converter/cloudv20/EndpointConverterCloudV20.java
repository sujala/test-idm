package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import org.dozer.Mapper;
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
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    private OpenStackServiceCatalogFactory sf = new OpenStackServiceCatalogFactory();

    public ServiceCatalog toServiceCatalog(List<OpenstackEndpoint> endpoints) {
        ServiceCatalog catalog = objFactories.getOpenStackIdentityV2Factory()
            .createServiceCatalog();

        if (endpoints == null || endpoints.size() == 0) {
            return catalog;
        }

        catalog = sf.createNew(endpoints);

        return catalog;
    }

    public EndpointList toEndpointList(List<OpenstackEndpoint> endpoints) {
        EndpointList list = objFactories.getOpenStackIdentityV2Factory()
            .createEndpointList();

        if (endpoints == null || endpoints.size() == 0) {
            return list;
        }

        for (OpenstackEndpoint point : endpoints) {
            for (CloudBaseUrl baseUrl : point.getBaseUrls()) {
                Endpoint endpoint = mapper.map(baseUrl, Endpoint.class);
                endpoint.setTenantId(point.getTenantId());
                endpoint.setName(baseUrl.getServiceName());
                if (!StringUtils.isBlank(baseUrl.getVersionId())) {
                    VersionForService version = new VersionForService();
                    version.setId(baseUrl.getVersionId());
                    version.setInfo(baseUrl.getVersionInfo());
                    version.setList(baseUrl.getVersionList());
                    endpoint.setVersion(version);
                }

                list.getEndpoint().add(endpoint);
            }
        }
        return list;
    }

    public EndpointList toEndpointListFromBaseUrls(List<CloudBaseUrl> endpoints) {
        EndpointList list = objFactories.getOpenStackIdentityV2Factory()
            .createEndpointList();

        if (endpoints == null || endpoints.size() == 0) {
            return list;
        }

        for (CloudBaseUrl baseUrl : endpoints) {
            list.getEndpoint().add(toEndpoint(baseUrl));
        }
        return list;
    }

    public EndpointTemplate toEndpointTemplate(CloudBaseUrl baseUrl) {
        EndpointTemplate template = mapper.map(baseUrl, EndpointTemplate.class);
        template.setEnabled(baseUrl.getEnabled());
        template.setGlobal(baseUrl.getGlobal());
        template.setDefault(baseUrl.getDef());
        template.setName(baseUrl.getServiceName());

        if (!StringUtils.isBlank(baseUrl.getVersionId())) {
            VersionForService version = new VersionForService();
            version.setId(baseUrl.getVersionId());
            version.setInfo(baseUrl.getVersionInfo());
            version.setList(baseUrl.getVersionList());
            template.setVersion(version);
        }
        return template;
    }

    public EndpointTemplateList toEndpointTemplateList(
        List<CloudBaseUrl> baseUrls) {
        EndpointTemplateList list = objFactories
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
        Endpoint endpoint = mapper.map(baseUrl, Endpoint.class);
        endpoint.setName(baseUrl.getServiceName());

        if (!StringUtils.isBlank(baseUrl.getVersionId())) {
            VersionForService version = new VersionForService();
            version.setId(baseUrl.getVersionId());
            version.setInfo(baseUrl.getVersionInfo());
            version.setList(baseUrl.getVersionList());
            endpoint.setVersion(version);
        }
        return endpoint;
    }
    
    public CloudBaseUrl toCloudBaseUrl(EndpointTemplate template) {
        CloudBaseUrl baseUrl = mapper.map(template, CloudBaseUrl.class);
        baseUrl.setEnabled(template.isEnabled());
        baseUrl.setGlobal(template.isGlobal());
        baseUrl.setServiceName(template.getName());

        if (template.getVersion() != null) {
            baseUrl.setVersionId(template.getVersion().getId());
            baseUrl.setVersionInfo(template.getVersion().getInfo());
            baseUrl.setVersionList(template.getVersion().getList());
        }
        String type = template.getType();

        if (type != null) {
            if(type.equalsIgnoreCase("object-store")){
                baseUrl.setBaseUrlType("NAST");
            }else{
                baseUrl.setBaseUrlType("MOSSO");
            }
        }

        return baseUrl;
    }
}
