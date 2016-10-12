package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Tenant;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForTenant extends JSONReaderForEntity<Tenant> {

    @Override
    public Tenant readFrom(Class<Tenant> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(TENANT_RAX_AUTH_DOMAIN_ID_PATH, DOMAIN_ID);
        prefixValues.put(TENANT_RAX_AUTH_TYPES_PATH, TYPES);

        String json = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes());
        Tenant tenant = read(arrayInputStream, JSONConstants.TENANT, prefixValues);

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(json);
            JSONObject inner = (JSONObject) outer.get(JSONConstants.TENANT);

            if (inner.containsKey(JSONConstants.RAX_AUTH_TYPES)) {
                Object typesObject = inner.get(JSONConstants.RAX_AUTH_TYPES);
                if (typesObject == null) {
                    Types types = new Types();
                    tenant.setTypes(types);
                }
            }

        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body");
        }

        return tenant;
    }
}
