package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForImpersonationRequest extends JSONWriterForEntity<ImpersonationRequest> implements MessageBodyWriter<ImpersonationRequest> {

    @Override
    public void writeTo(ImpersonationRequest impersonationRequest, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(RAX_AUTH_IMPERSONATION_FEDERATED_IDP_PATH, RAX_AUTH_FEDERATED_IDP);
        prefixValues.put(RAX_AUTH_IMPERSONATION_PATH, RAX_AUTH_IMPERSONATION);
        write(impersonationRequest, entityStream, prefixValues);
    }

}
