package com.rackspace.idm.multifactor.providers.duo.service;

import com.rackspace.idm.multifactor.providers.ProviderAvailability;
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

import java.util.Collections;

/**
 * Duo Security ProviderAvailability implementation
 */
@Component
public class DuoAvailability implements ProviderAvailability {

    @Autowired(required = false)
    private DuoJsonResponseReader duoJsonResponseReader = new InMemoryDuoJsonResponseReader();

    private Configuration globalConfig;

    private DuoRequestHelper duoRequestHelper;

    private final String AUTH_ENDPOINT_BASE_PATH = "/auth/v2";
    private final WebResource AUTH_ENDPOINT_BASE_RESOURCE;

    private final WebResource PING_ENDPOINT_RESOURCE;

    @Autowired
    public DuoAvailability(Configuration globalConfig) {
        this(new DuoRequestHelper(new AuthApiConfig(globalConfig)), globalConfig);
    }

    public DuoAvailability(DuoRequestHelper duoRequestHelper, Configuration globalConfig) {
        this.duoRequestHelper = duoRequestHelper;
        this.globalConfig = globalConfig;

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
