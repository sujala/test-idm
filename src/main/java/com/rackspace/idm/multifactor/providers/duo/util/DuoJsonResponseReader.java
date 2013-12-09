package com.rackspace.idm.multifactor.providers.duo.util;

import com.rackspace.idm.multifactor.providers.duo.domain.DuoResponse;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Converts the JSON responses received from Duo Security into a standard response object that makes it easy to determine whether the request succeeded
 * or failed as well as convert into the specified expected response object.
 */
public interface DuoJsonResponseReader {
    /**
     * Takes in the jersey client response and converts it to the specified java object if the response from Duo was "OK". If the response is a failure, converts to the
     * standard FailureResult object.
     *
     * @param duoResponse
     * @param clazz
     * @param <T>
     * @return
     */
    <T> DuoResponse<T> fromDuoResponse(ClientResponse duoResponse, Class<T> clazz);
}
