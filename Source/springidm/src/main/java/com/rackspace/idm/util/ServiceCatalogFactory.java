package com.rackspace.idm.util;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import com.rackspace.idm.entities.CloudEndpoint;
import com.rackspace.idm.jaxb.Endpoint;
import com.rackspace.idm.jaxb.Service;
import com.rackspace.idm.jaxb.ServiceCatalog;
import com.rackspace.idm.jaxb.UserType;

public class ServiceCatalogFactory {
    public ServiceCatalog createNew(List<CloudEndpoint> endPoints) {
        if (endPoints == null) {
            throw new IllegalArgumentException("endPoints can not be null");
        }

        ServiceCatalog serviceCatalog = new ServiceCatalog();

        for (CloudEndpoint endPoint : endPoints) {
            processEndpoint(serviceCatalog, endPoint);
        }

        return serviceCatalog;
    }

    static void processEndpoint(ServiceCatalog serviceCatalog,
        CloudEndpoint endPoint) {
        Service currentService = new ServiceCatalogHelper(serviceCatalog)
            .getEndPointService(endPoint);

        String accountId = getAccountIdForUrl(endPoint.getBaseUrl()
            .getBaseUrlType(), endPoint.getUsername(), endPoint.getNastId(),
            endPoint.getMossoId());

        Endpoint endpoint = new Endpoint();
        endpoint.setAdminURL(endPoint.getBaseUrl().getAdminUrl());
        endpoint.setInternalURL(endPoint.getBaseUrl().getInternalUrl());
        endpoint.setPublicURL(endPoint.getBaseUrl().getPublicUrl());
        endpoint.setRegion(endPoint.getBaseUrl().getRegion());
        endpoint.setV1Default(endPoint.isV1preferred());

        setEndpointUrls(endpoint, accountId);

        currentService.getEndpoints().add(endpoint);
    }

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
                accountId = String.valueOf(mossoId);
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