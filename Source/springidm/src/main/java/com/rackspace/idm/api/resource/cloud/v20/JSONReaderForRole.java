package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRole implements MessageBodyReader<Role> {

    private static final Logger logger = LoggerFactory.getLogger(JSONReaderForRole.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == Role.class;
    }

    @Override
    public Role readFrom(Class<Role> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        Role object = getRoleFromJSONString(jsonBody);

        return object;
    }
    
    public static Role getRoleFromJSONString(String jsonBody) {
        Role role = new Role();
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.ROLE)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.ROLE).toString());
                
                Object id = obj3.get(JSONConstants.ID);
                Object name = obj3.get(JSONConstants.NAME);
                Object description = obj3.get(JSONConstants.DESCRIPTION);
                Object tenantId = obj3.get(JSONConstants.TENANT_ID);
                Object serviceId = obj3.get(JSONConstants.SERVICE_ID);
                
                if (id != null) {
                    role.setId(id.toString());
                }
                if (name != null) {
                    role.setName(name.toString());
                }
                if (description != null) {
                    role.setDescription(description.toString());
                }
                if (tenantId != null) {
                    role.setTenantId(tenantId.toString());
                }
                if (serviceId != null) {
                    role.setServiceId(serviceId.toString());
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Bad JSON request", e);
        }
        
        return role;
    }
}
