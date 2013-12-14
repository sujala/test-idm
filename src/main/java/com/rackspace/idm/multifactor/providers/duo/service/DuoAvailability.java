package com.rackspace.idm.multifactor.providers.duo.service;

import com.rackspace.idm.multifactor.providers.ProviderAvailability;
import com.rackspace.idm.multifactor.providers.duo.config.ApacheConfigAuthApiConfig;
import com.rackspace.idm.multifactor.providers.duo.config.AuthApiConfig;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoPing;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoResponse;
import com.rackspace.idm.multifactor.providers.duo.util.DuoJsonResponseReader;
import com.rackspace.idm.multifactor.providers.duo.util.InMemoryDuoJsonResponseReader;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import lombok.Getter;
import lombok.Setter;

/**
 * Duo Security ProviderAvailability implementation
 */
@Component
public class DuoAvailability implements ProviderAvailability {
    private static final String AUTH_ENDPOINT_BASE_PATH = "/auth/v2";

    @Autowired(required = false)
    private DuoJsonResponseReader duoJsonResponseReader = new InMemoryDuoJsonResponseReader();

    @Autowired
    @Getter
    @Setter
    private AuthApiConfig authApiConfig;

    @Autowired(required = false)
    DuoRequestHelperFactory duoRequestHelperFactory = new SingletonDuoRequestHelperFactory();

    private DuoRequestHelper duoRequestHelper;

    private WebResource AUTH_ENDPOINT_BASE_RESOURCE;
    private WebResource PING_ENDPOINT_RESOURCE;


    /**
     * Default constructor to allow setter dependency injection. Must call init() once completed.
     */
    public DuoAvailability() {}

    /**
     * Regular constructor for non-injection based construction. Must call init() before use.
     *
     * @param authApiConfig
     */
    public DuoAvailability(AuthApiConfig authApiConfig) {
        this.authApiConfig = authApiConfig;
    }


    /**
     * Must be called after dependencies are injected and before the services are used.
     */
    @PostConstruct
    protected synchronized void init() {
        this.duoRequestHelper = duoRequestHelperFactory.getInstance(authApiConfig);
        AUTH_ENDPOINT_BASE_RESOURCE = duoRequestHelper.createWebResource(AUTH_ENDPOINT_BASE_PATH);
        PING_ENDPOINT_RESOURCE = AUTH_ENDPOINT_BASE_RESOURCE.path("ping");
    }

    @Override
    public boolean available() {
        ClientResponse clientResponse = duoRequestHelper.makeGetRequest(PING_ENDPOINT_RESOURCE, Collections.EMPTY_MAP, ClientResponse.class);
        DuoResponse<DuoPing> response = duoJsonResponseReader.fromDuoResponse(clientResponse, DuoPing.class);
        return response.isSuccess();
    }
}