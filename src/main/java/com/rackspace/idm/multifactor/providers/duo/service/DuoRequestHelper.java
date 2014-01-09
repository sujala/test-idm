package com.rackspace.idm.multifactor.providers.duo.service;

import com.sun.jersey.api.client.WebResource;

import java.util.Map;

/**
 */
public interface DuoRequestHelper {
    WebResource createWebResource(String additionalPath);

    <T> T makeDeleteRequest(WebResource baseResource, Map<String, String> params, Class<T> responseClass);

    WebResource.Builder buildDeleteWebResource(WebResource baseResource, Map<String, String> params);

    <T> T makeGetRequest(WebResource baseResource, Map<String, String> params, Class<T> responseClass);

    WebResource.Builder buildGetWebResource(WebResource baseResource, Map<String, String> params);

    WebResource.Builder buildPostWebResource(WebResource baseResource, Map<String, String> params);

    <T> T makePostRequest(WebResource baseResource, Map<String, String> params, Class<T> responseClass);
}
