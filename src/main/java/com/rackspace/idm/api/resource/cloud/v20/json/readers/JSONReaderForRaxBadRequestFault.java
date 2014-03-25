package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.api.common.fault.v1.BadRequestFault;
import com.rackspace.idm.JSONConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxBadRequestFault extends JSONReaderForEntity<BadRequestFault> {

    @Override
    public BadRequestFault readFrom(Class<BadRequestFault> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.OPENSTACK_BAD_REQUEST, JSONConstants.RAX_COMMON_BAD_REQUEST);

        return read(entityStream, JSONConstants.OPENSTACK_BAD_REQUEST, prefixValues);
    }
}
