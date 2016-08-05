package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import org.json.simple.JSONObject;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForOsKsCatalogEndpointTemplates extends JSONWriterForArrayEntity<EndpointTemplateList> {

    @Override
    public void writeTo(EndpointTemplateList endpointTemplateList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(endpointTemplateList, JSONConstants.ENDPOINT_TEMPLATES, JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATES, entityStream, new EndpointTemplateEntryTransformer());
    }

    private static class EndpointTemplateEntryTransformer implements JsonArrayEntryTransformer {

        @Override
        public void transform(JSONObject arrayEntry) {
            if (arrayEntry.containsKey(ASSIGNMENT_TYPE)) {
                Object assignmentType = arrayEntry.get(ASSIGNMENT_TYPE);
                arrayEntry.remove(ASSIGNMENT_TYPE);
                arrayEntry.put(RAX_AUTH_ASSIGNMENT_TYPE, assignmentType);
            }
            if (arrayEntry.containsKey(VERSION)) {
                JSONObject version = (JSONObject)arrayEntry.get(VERSION);
                if(version != null){
                    Object id = version.get(ID);
                    if(id != null){
                        arrayEntry.put(VERSION_ID, id);
                    }
                    Object info = version.get(INFO);
                    if(info != null){
                        arrayEntry.put(VERSION_INFO, info);
                    }
                    Object list = version.get(LIST);
                    if(list != null){
                        arrayEntry.put(VERSION_LIST, list);
                    }
                    arrayEntry.remove(JSONConstants.VERSION);
                }
            }
        }
    }
}
