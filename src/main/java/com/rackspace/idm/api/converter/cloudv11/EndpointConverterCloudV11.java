package com.rackspace.idm.api.converter.cloudv11;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.util.CloudAuthServiceCatalogFactory;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class EndpointConverterCloudV11 {
    @Autowired
    private Configuration config;

    private final ObjectFactory of = new ObjectFactory();
    private final CloudAuthServiceCatalogFactory sf = new CloudAuthServiceCatalogFactory();

    public BaseURL toBaseUrl(CloudBaseUrl url) {
        if (url == null) {
            return null;
        }
        BaseURL baseUrl = of.createBaseURL();
        baseUrl.setEnabled(url.getEnabled());
        if (url.getAdminUrl() != null) {
            baseUrl.setAdminURL(url.getAdminUrl().trim());
        }
        baseUrl.setDefault(url.getDef());
        baseUrl.setId(Integer.parseInt(url.getBaseUrlId()));
        if (url.getInternalUrl() != null) {
            baseUrl.setInternalURL(url.getInternalUrl().trim());
        }
        if (url.getPublicUrl() != null) {
            baseUrl.setPublicURL(url.getPublicUrl().trim());
        }
        baseUrl.setRegion(url.getRegion());
        baseUrl.setServiceName(url.getServiceName());
        if (url.getBaseUrlType() != null) {
            UserType userType;
            try {
                userType = UserType.valueOf(url.getBaseUrlType().toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                userType = UserType.UNKNOWN;
            }
            baseUrl.setUserType(userType);
        }

        return baseUrl;
    }

    public CloudBaseUrl toBaseUrlDO(BaseURL baseURL) {
        if (baseURL == null) {
            return null;
        }
        CloudBaseUrl url = new CloudBaseUrl();
        url.setAdminUrl(baseURL.getAdminURL());
        url.setBaseUrlId(String.valueOf(baseURL.getId()));
        if (baseURL.getUserType() != null) {
            url.setBaseUrlType(baseURL.getUserType().toString());
        }
        url.setDef(baseURL.isDefault());
        url.setInternalUrl(baseURL.getInternalURL());
        url.setPublicUrl(baseURL.getPublicURL());
        url.setRegion(baseURL.getRegion());
        url.setServiceName(baseURL.getServiceName());
        url.setEnabled(baseURL.isEnabled());
        if(String.valueOf(baseURL.getUserType()).equals("NAST")){
            url.setOpenstackType("object-store");
        }else{
            url.setOpenstackType("compute");
        }
        return url;
    }

    public BaseURLRef toBaseUrlRef(CloudEndpoint endpoint) {
        if (endpoint == null || endpoint.getBaseUrl() == null) {
            return null;
        }
        BaseURLRef baseUrlRef = of.createBaseURLRef();
        baseUrlRef.setId(Integer.parseInt(endpoint.getBaseUrl().getBaseUrlId()));
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

	public BaseURLRefList openstackToBaseUrlRefs(List<OpenstackEndpoint> endpoints) {
        BaseURLRefList refs = of.createBaseURLRefList();

        if (endpoints == null || endpoints.size() == 0) {
            return refs;
        }

        for (OpenstackEndpoint e : endpoints) {
            refs.getBaseURLRef().addAll(toBaseUrlRef(e));
        }

        return removeDuplicates(refs);
	}

    protected BaseURLRefList removeDuplicates(BaseURLRefList refs) {
        HashMap<Integer, BaseURLRef> refHashMap = new HashMap<Integer, BaseURLRef>();

        for (BaseURLRef baseURLRef : refs.getBaseURLRef()) {
            if (!refHashMap.containsKey(baseURLRef.getId())) {
                refHashMap.put(baseURLRef.getId(), baseURLRef);
            }
        }
        refs.getBaseURLRef().clear();
        refs.getBaseURLRef().addAll(new ArrayList<BaseURLRef>(refHashMap.values()));
        return refs;
    }

    List<BaseURLRef> toBaseUrlRef(OpenstackEndpoint endpoint) {
    	List<BaseURLRef> result = new ArrayList<BaseURLRef>();

    	if (endpoint == null) {
            return result;
        }

    	for (CloudBaseUrl baseUrl : endpoint.getBaseUrls()) {
            BaseURLRef baseUrlRef = of.createBaseURLRef();
            baseUrlRef.setId(Integer.parseInt(baseUrl.getBaseUrlId()));
            baseUrlRef.setV1Default(baseUrl.getV1Default());
            baseUrlRef.setHref(String.format(getBaseUrlReferenceString(), baseUrl.getBaseUrlId()));
            result.add(baseUrlRef);
    	}

        return result;
	}

	public BaseURLList toBaseUrls(Iterable<CloudBaseUrl> urls) {
        BaseURLList baseUrls = of.createBaseURLList();
        if (!urls.iterator().hasNext()) {
            return baseUrls;
        }
        for (CloudBaseUrl url : urls) {
            baseUrls.getBaseURL().add(toBaseUrl(url));
        }
        return baseUrls;
    }

    public ServiceCatalog  toServiceCatalog(List<OpenstackEndpoint> endpoints) {
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
