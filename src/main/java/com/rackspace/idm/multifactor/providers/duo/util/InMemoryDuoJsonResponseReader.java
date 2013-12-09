package com.rackspace.idm.multifactor.providers.duo.util;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoResponse;
import com.rackspace.idm.multifactor.providers.duo.domain.DuoStatus;
import com.rackspace.idm.multifactor.providers.duo.domain.FailureResult;
import com.sun.jersey.api.client.ClientResponse;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Simple converter that will load the entire response as a string before writing to the specified object. Should only be used for responses guaranteed to be smaller.
 */
public final class InMemoryDuoJsonResponseReader implements DuoJsonResponseReader {

    private Gson gson = new Gson();

    @Override
    public <T> DuoResponse<T> fromDuoResponse(ClientResponse duoResponse, Class<T> clazz) {
        String responseString = duoResponse.getEntity(String.class);
        return fromDuoResponse(responseString, clazz);
    }

    public <T> DuoResponse<T> fromDuoResponse(String duoJsonResponse, Class<T> clazz) {
        IntermediateObjectResult intermediateResult = gson.fromJson(duoJsonResponse, IntermediateObjectResult.class);

        if (intermediateResult.stat == DuoStatus.OK) {
            T response = gson.fromJson(gson.toJson(intermediateResult.response), clazz);
            return new DuoResponse<T>(intermediateResult.stat, response);
        } else {
            FailureResult fr = new FailureResult(intermediateResult.code, intermediateResult.message, intermediateResult.message_detail);
            return new DuoResponse<T>(intermediateResult.stat, fr);
        }
    }

    private class IntermediateObjectResult {
        private DuoStatus stat;
        private Object response;
        private int code;
        private String message;
        private String message_detail;
    }

}
