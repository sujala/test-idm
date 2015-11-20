package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificate;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForEntity;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthPublicCertificate extends JSONWriterForEntity<PublicCertificate> {

    @Override
    public void writeTo(PublicCertificate publicCertificate, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.PUBLIC_CERTIFICATE, JSONConstants.RAX_AUTH_PUBLIC_CERTIFICATE);
        write(publicCertificate, entityStream, prefixValues, JSONReaderForEntity.NEVER_PLURALIZE_HANDLER);
    }

}
