package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainAdministratorChange;
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
public class JSONReaderForRaxAuthDomainAdministratorChange extends JSONReaderForEntity<DomainAdministratorChange> {

    @Override
    public DomainAdministratorChange readFrom(Class<DomainAdministratorChange> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_AUTH_DOMAIN_ADMINISTRATOR_CHANGE, JSONConstants.DOMAIN_ADMINISTRATOR_CHANGE);
        DomainAdministratorChange domainAdministratorChange = read(inputStream, JSONConstants.RAX_AUTH_DOMAIN_ADMINISTRATOR_CHANGE, prefixValues);
        return domainAdministratorChange;
    }
}
