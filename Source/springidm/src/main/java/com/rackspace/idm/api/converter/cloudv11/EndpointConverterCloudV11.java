package com.rackspace.idm.api.converter.cloudv11;

import java.util.List;

import org.apache.commons.configuration.Configuration;

import com.rackspace.idm.cloudv11.jaxb.BaseURL;
import com.rackspace.idm.cloudv11.jaxb.BaseURLList;
import com.rackspace.idm.cloudv11.jaxb.BaseURLRef;
import com.rackspace.idm.cloudv11.jaxb.BaseURLRefList;
import com.rackspace.idm.cloudv11.jaxb.ObjectFactory;
import com.rackspace.idm.cloudv11.jaxb.ServiceCatalog;
import com.rackspace.idm.cloudv11.jaxb.UserType;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.util.CloudAuthServiceCatalogFactory;

public class EndpointConverterCloudV11 {
    private final Configuration config;

    private final ObjectFactory of = new ObjectFactory();
    private final CloudAuthServiceCatalogFactory sf = new CloudAuthServiceCatalogFactory();

    public EndpointConverterCloudV11(Configuration config) {
        this.config = config;
    }

    public BaseURL toBaseUrl(CloudBaseUrl url) {
        if (url == null) {
            return null;
        }
        BaseURL baseUrl = of.createBaseURL();
        baseUrl.setEnabled(url.getEnabled());
        baseUrl.setAdminURL(url.getAdminUrl());
        baseUrl.setDefault(url.getDef());
        baseUrl.setId(url.getBaseUrlId());
        baseUrl.setInternalURL(url.getInternalUrl());
        baseUrl.setPublicURL(url.getPublicUrl());
        baseUrl.setRegion(url.getRegion());
        baseUrl.setServiceName(url.getService());
        if (url.getBaseUrlType() != null) {
            baseUrl.setUserType(Enum.valueOf(UserType.class, url
                .getBaseUrlType().toUpperCase()));
        }
        return baseUrl;
    }

    public CloudBaseUrl toBaseUrlDO(BaseURL baseURL) {
        if (baseURL == null) {
            return null;
        }
        CloudBaseUrl url = new CloudBaseUrl();
        url.setAdminUrl(baseURL.getAdminURL());
        url.setBaseUrlId(baseURL.getId());
        if (baseURL.getUserType() != null) {
            url.setBaseUrlType(baseURL.getUserType().toString());
        }
        url.setDef(baseURL.isDefault());
        url.setInternalUrl(baseURL.getInternalURL());
        url.setPublicUrl(baseURL.getPublicURL());
        url.setRegion(baseURL.getRegion());
        url.setService(baseURL.getServiceName());
        return url;
    }

    public BaseURLRef toBaseUrlRef(CloudEndpoint endpoint) {
        if (endpoint == null || endpoint.getBaseUrl() == null) {
            return null;
        }
        BaseURLRef baseUrlRef = of.createBaseURLRef();
        baseUrlRef.setId(endpoint.getBaseUrl().getBaseUrlId());
        baseUrlRef.setV1Default(endpoint.isV1preferred());
        baseUrlRef.setHref(String.format(getBaseUrlReferenceString(),
            endpoint.getBaseUrl().getBaseUrlId()));
        
        return baseUrlRef;
    }

    public BaseURLRefList toBaseUrlRefs(List<CloudEndpoint> endpoints) {
        BaseURLRefList refs = of.createBaseURLRefList();

        if (endpoints == null || endpoints.size() == 0) {
            return refs;
        }

        for (CloudEndpoint e : endpoints) {
            refs.getBaseURLRef().add(toBaseUrlRef(e));
        }
        return refs;
    }

    public BaseURLList toBaseUrls(List<CloudBaseUrl> urls) {
        BaseURLList baseUrls = of.createBaseURLList();
        if (urls == null || urls.size() == 0) {
            return baseUrls;
        }
        for (CloudBaseUrl url : urls) {
            baseUrls.getBaseURL().add(toBaseUrl(url));
        }
        return baseUrls;
    }

    public ServiceCatalog toServiceCatalog(List<CloudEndpoint> endpoints) {
        ServiceCatalog catalog = of.createServiceCatalog();

        if (endpoints == null || endpoints.size() == 0) {
            return catalog;
        }

        catalog = sf.createNew(endpoints);

        return catalog;
    }

    private String getBaseUrlReferenceString() {
        return config.getString("cloud.baseurl.ref.string");
    }
}
