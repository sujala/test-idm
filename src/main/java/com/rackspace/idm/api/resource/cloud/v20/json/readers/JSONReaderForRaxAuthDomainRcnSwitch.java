package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainRcnSwitch;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthDomainRcnSwitch extends JSONReaderForEntity<DomainRcnSwitch> {

    @Override
    public DomainRcnSwitch readFrom(Class<DomainRcnSwitch> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_AUTH_DOMAIN_RCN_SWITCH, JSONConstants.DOMAIN_RCN_SWITCH);
        DomainRcnSwitch rcnSwitch = read(inputStream, JSONConstants.RAX_AUTH_DOMAIN_RCN_SWITCH, prefixValues);
        return rcnSwitch;
    }
}
