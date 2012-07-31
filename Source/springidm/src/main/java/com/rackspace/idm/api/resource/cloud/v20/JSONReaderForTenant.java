package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Tenant;

import com.rackspace.idm.JSONConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForTenant implements MessageBodyReader<Tenant> {
    private static final Logger logger = LoggerFactory.getLogger(JSONReaderForTenant.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == Tenant.class;
    }

    @Override
    public Tenant readFrom(Class<Tenant> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        Tenant object = getTenantFromJSONString(jsonBody);

        return object;
    }
    
    public static Tenant getTenantFromJSONString(String jsonBody) {
        Tenant tenant = new Tenant();
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.TENANT)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.TENANT).toString());
                
                Object id = obj3.get(JSONConstants.ID);
                Object name = obj3.get(JSONConstants.NAME);
                Object enabled = obj3.get(JSONConstants.ENABLED);
                Object description = obj3.get(JSONConstants.DESCRIPTION);
                Object displayname = obj3.get(JSONConstants.DISPLAY_NAME_CLOUD);
                
                if (id != null) {
                    tenant.setId(id.toString());
                }
                if (enabled != null) {
                    tenant.setEnabled(Boolean.valueOf(enabled.toString()));
                }
                if (name != null) {
                    tenant.setName(name.toString());
                }
                if (description != null) {
                    tenant.setDescription(description.toString());
                }
                if (displayname != null) {
                    tenant.setDisplayName(displayname.toString());
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Bad JSON request", e);
        }
        
        return tenant;
    }
}
