package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointTemplateAssignmentTypeEnum;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.service.ApplicationService;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.*;
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

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private ApplicationService applicationService;

    public ServiceCatalog toServiceCatalog(List<OpenstackEndpoint> endpoints) {
        ServiceCatalog catalog;

        if (endpoints == null || endpoints.size() == 0) {
            catalog = objFactories.getOpenStackIdentityV2Factory()
                    .createServiceCatalog();
            return catalog;
        } else {
            catalog = createNew(endpoints);
        }

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

                if (baseUrl.getRegion() != null && baseUrl.getRegion().equals(identityConfig.getReloadableConfig().getEndpointDefaultRegionId())) {
                    endpoint.setRegion(null);
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

    private EndpointTemplateAssignmentTypeEnum getAssignmentType(String baseUrlType) {
        try {
            return EndpointTemplateAssignmentTypeEnum.fromValue(baseUrlType);
        } catch (NullPointerException | IllegalArgumentException e){
            return null;
        }
    }

    public EndpointTemplate toEndpointTemplate(CloudBaseUrl baseUrl) {
        EndpointTemplate template = mapper.map(baseUrl, EndpointTemplate.class);
        template.setEnabled(baseUrl.getEnabled());
        template.setGlobal(baseUrl.getGlobal());
        template.setDefault(baseUrl.getDef());
        template.setName(baseUrl.getServiceName());
        //NOTE: ServiceId and assignmentType are now expose on every response
        template.setServiceId(baseUrl.getClientId());

        // Default endpoint template type to 'MANUAL' if null or not one of the acceptable types
        EndpointTemplateAssignmentTypeEnum typeEnum = getAssignmentType(baseUrl.getBaseUrlType());
        if (typeEnum == null) {
            template.setAssignmentType(EndpointTemplateAssignmentTypeEnum.MANUAL);
        } else {
            template.setAssignmentType(typeEnum);
        }

        if (!StringUtils.isBlank(baseUrl.getVersionId())) {
            VersionForService version = new VersionForService();
            version.setId(baseUrl.getVersionId());
            version.setInfo(baseUrl.getVersionInfo());
            version.setList(baseUrl.getVersionList());
            template.setVersion(version);
        }

        if (template.getRegion() != null && template.getRegion().equals(identityConfig.getReloadableConfig().getEndpointDefaultRegionId())) {
            template.setRegion(null);
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
        baseUrl.setServiceName(template.getName());
        baseUrl.setClientId(template.getServiceId());

        if (template.getVersion() != null) {
            baseUrl.setVersionId(template.getVersion().getId());
            baseUrl.setVersionInfo(template.getVersion().getInfo());
            baseUrl.setVersionList(template.getVersion().getList());
        }

        //do not let the user set these values, they must be set through the update endpoint template call
        baseUrl.setEnabled(false);
        baseUrl.setGlobal(false);
        baseUrl.setDef(false);

        String type = template.getType();

        /*
        Setting endpoint template baseURL type.

        1. If serviceId is provided, retrieve application by id and set both openstack and baseUrl type.
        2. Fall back to using configuration mapping to set the baseUrl Type if name and type where provided in the endpoint template creation
           when reloadable properties feature.endpoint.template.type.nast.mapping and feature.endpoint.template.type.mosso.mapping are enabled.
        3. Use hardcoded value 'object-store' to determine nast endpoint and set any other to mosso type. (Deprecate)
         */
        if (!StringUtils.isBlank(baseUrl.getClientId())) {
            Application application = applicationService.checkAndGetApplication(baseUrl.getClientId());
            baseUrl.setOpenstackType(application.getOpenStackType());
            baseUrl.setBaseUrlType(template.getAssignmentType().value());
        } else if (identityConfig.getReloadableConfig().getBaseUrlUseTypeMappingFlag()) {
            if(ignoreCaseContains(identityConfig.getReloadableConfig().getBaseUrlNastTypeMapping(), type)) {
                baseUrl.setBaseUrlType("NAST");
            } else if(ignoreCaseContains(identityConfig.getReloadableConfig().getBaseUrlMossoTypeMapping(), type)) {
                baseUrl.setBaseUrlType("MOSSO");
            } else {
                // If the service type is not in the nast/mosso mapping set baseUrl type to 'MANUAL'
                baseUrl.setBaseUrlType(EndpointTemplateAssignmentTypeEnum.MANUAL.value());
            }

        } else {
            if (type != null) {
                if(type.equalsIgnoreCase("object-store")){
                    baseUrl.setBaseUrlType("NAST");
                }else{
                    baseUrl.setBaseUrlType("MOSSO");
                }
            }
        }

        return baseUrl;
    }

    private boolean ignoreCaseContains(String[] types, String type) {
        for(String curType : types) {
            if(curType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    public IdentityConfig getIdentityConfig() {
        return identityConfig;
    }

    public void setIdentityConfig(IdentityConfig identityConfig) {
        this.identityConfig = identityConfig;
    }

    ServiceCatalog createNew(List<OpenstackEndpoint> endPoints) {
        if (endPoints == null) {
            throw new IllegalArgumentException("endPoints can not be null");
        }

        ServiceCatalog serviceCatalog = new ServiceCatalog();

        for (OpenstackEndpoint endPoint : endPoints) {
            processEndpoint(serviceCatalog, endPoint);
        }

        return serviceCatalog;
    }

    void processEndpoint(ServiceCatalog serviceCatalog,
                                OpenstackEndpoint endPoint) {

        for (CloudBaseUrl baseUrl : endPoint.getBaseUrls()) {

            ServiceForCatalog currentService = new OpenStackServiceCatalogHelper(serviceCatalog)
                    .getEndPointService(baseUrl.getServiceName(), baseUrl.getOpenstackType());

            VersionForService version = new VersionForService();
            version.setId(baseUrl.getVersionId());
            version.setInfo(baseUrl.getVersionInfo());
            version.setList(baseUrl.getVersionList());

            EndpointForService endpointItem = new EndpointForService();

            endpointItem.setAdminURL(baseUrl.getAdminUrl());
            endpointItem.setInternalURL(baseUrl.getInternalUrl());
            endpointItem.setPublicURL(baseUrl.getPublicUrl());
            endpointItem.setTenantId(endPoint.getTenantId());

            if (baseUrl.getRegion() != null && !baseUrl.getRegion().equalsIgnoreCase(identityConfig.getReloadableConfig().getEndpointDefaultRegionId())) {
                endpointItem.setRegion(baseUrl.getRegion());
            }

            if (!org.apache.commons.lang.StringUtils.isBlank(version.getId())) {
                endpointItem.setVersion(version);
            }
            if (baseUrl.getV1Default()) {
                currentService.getEndpoint().add(0, endpointItem);
            } else {
                currentService.getEndpoint().add(endpointItem);
            }
        }
    }

}
