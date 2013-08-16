package com.rackspace.idm.api.resource.cloud.v20.JSONReaders;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JSONReaderForEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxKsKeyApiKeyCredentials extends JSONReaderForEntity<ApiKeyCredentials> implements MessageBodyReader<ApiKeyCredentials> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ApiKeyCredentials.class;
    }

    @Override
    public ApiKeyCredentials readFrom(Class<ApiKeyCredentials> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS, JSONConstants.API_KEY_CREDENTIALS);

        return read(inputStream, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS, prefixValues);
    }
}
