package com.rackspace.idm.api.converter.cloudv20;

import java.util.List;

import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.VersionForService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;

@Component
public class EndpointConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    private final OpenStackServiceCatalogFactory sf = new OpenStackServiceCatalogFactory();

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

                Endpoint endpoint = OBJ_FACTORIES
                    .getOpenStackIdentityV2Factory().createEndpoint();
                endpoint.setAdminURL(baseUrl.getAdminUrl());
                endpoint.setId(baseUrl.getBaseUrlId());
                endpoint.setInternalURL(baseUrl.getInternalUrl());
                endpoint.setName(baseUrl.getName());
                endpoint.setPublicURL(baseUrl.getPublicUrl());
                endpoint.setRegion(baseUrl.getRegion());
                endpoint.setType(baseUrl.getOpenstackType());
                if (!StringUtils.isBlank(version.getId())) {
                    endpoint.setVersion(version);
                }
                list.getEndpoint().add(endpoint);
            }
        }
        return list;
    }

    public EndpointList toEndpointListFromBaseUrls(List<CloudBaseUrl> baseUrls) {
        EndpointList list = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createEndpointList();

        if (baseUrls == null || baseUrls.size() == 0) {
            return list;
        }

        for (CloudBaseUrl baseUrl : baseUrls) {
            VersionForService version = new VersionForService();
            version.setId(baseUrl.getVersionId());
            version.setInfo(baseUrl.getVersionInfo());
            version.setList(baseUrl.getVersionList());

            Endpoint endpoint = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                .createEndpoint();
            endpoint.setAdminURL(baseUrl.getAdminUrl());
            endpoint.setId(baseUrl.getBaseUrlId());
            endpoint.setInternalURL(baseUrl.getInternalUrl());
            endpoint.setName(baseUrl.getName());
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
}
