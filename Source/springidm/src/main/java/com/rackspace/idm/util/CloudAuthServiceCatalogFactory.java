package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspacecloud.docs.auth.api.v1.Endpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import com.rackspacecloud.docs.auth.api.v1.UserType;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class CloudAuthServiceCatalogFactory {
    public ServiceCatalog createNew(List<OpenstackEndpoint> endPoints) {
        if (endPoints == null) {
            throw new IllegalArgumentException("endPoints can not be null");
        }

        ServiceCatalog serviceCatalog = new ServiceCatalog();

        for (OpenstackEndpoint endPoint : endPoints) {
            processService(serviceCatalog, endPoint);
        }

        return serviceCatalog;
    }

    static void processService(ServiceCatalog serviceCatalog, OpenstackEndpoint openstackEndPoint) {
        if(openstackEndPoint.getBaseUrls() != null) {
            for (CloudBaseUrl baseUrl : openstackEndPoint.getBaseUrls()) {
                Service service = new Service();
                service.setName(baseUrl.getServiceName());
                Endpoint endpoint = new Endpoint();
                endpoint.setAdminURL(baseUrl.getAdminUrl());
                endpoint.setInternalURL(baseUrl.getInternalUrl());
                endpoint.setPublicURL(baseUrl.getPublicUrl());
                endpoint.setRegion(baseUrl.getRegion());
                endpoint.setV1Default(baseUrl.getDef());
                service.getEndpoint().add(endpoint);
                serviceCatalog.getService().add(service);
            }
        }
    }

    /*
    static void processEndpoint(ServiceCatalog serviceCatalog, OpenstackEndpoint endPoint) {
        Service currentService = new CloudAuthServiceCatalogHelper(serviceCatalog).getEndPointService(endPoint);

        String accountId = getAccountIdForUrl(endPoint.getBaseUrl()
            .getBaseUrlType(), endPoint.getUsername(), endPoint.getNastId(),
            endPoint.getMossoId());
        
        if (accountId == null) {
            return;
        }

        Endpoint endpoint = new Endpoint();
        endpoint.setAdminURL(endPoint.getBaseUrl().getAdminUrl());
        endpoint.setInternalURL(endPoint.getBaseUrl().getInternalUrl());
        endpoint.setPublicURL(endPoint.getBaseUrl().getPublicUrl());
        endpoint.setRegion(endPoint.getBaseUrl().getRegion());
        endpoint.setV1Default(endPoint.isV1preferred());

        setEndpointUrls(endpoint, accountId);

        currentService.getEndpoint().add(endpoint);
    }
    */
    
    static void setEndpointUrls(Endpoint endpoint, String accountId) {
        endpoint.setAdminURL(createUrl(endpoint.getAdminURL(), accountId));
        endpoint
            .setInternalURL(createUrl(endpoint.getInternalURL(), accountId));
        endpoint.setPublicURL(createUrl(endpoint.getPublicURL(), accountId));
    }

    static String createUrl(String urlBase, String accountId) {
        if (StringUtils.isBlank(accountId)) {
            throw new IllegalArgumentException(
                "accountId can not be null or empty");
        }

        String url = null;

        if (!StringUtils.isBlank(urlBase)) {
            url = urlBase + getUrlDelimiter(urlBase) + accountId;
        }

        return url;
    }

    static String getUrlDelimiter(String urlBase) {
        return urlBase.charAt(urlBase.length() - 1) == '/' ? "" : "/";
    }

    static String getAccountIdForUrl(String urlBaseType, String username,
        String nastId, int mossoId) {

        String accountId = null;

        UserType type = Enum.valueOf(UserType.class, urlBaseType.toUpperCase());

        switch (type) {
            case MOSSO:
                accountId = mossoId > 0 ? String.valueOf(mossoId) : null;
                break;
            case CLOUD:
                accountId = username;
                break;
            case NAST:
                accountId = nastId;
                break;
        }

        return accountId;
    }
}
