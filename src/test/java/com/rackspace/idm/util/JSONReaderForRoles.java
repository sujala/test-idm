package com.rackspace.idm.util;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForArrayEntity;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRole;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Wasn't provided in production code, but is useful for tests that require reading in json responses. Rather than add to production, which could affect
 * other code unexpectedly, just added to test package.
 */
public class JSONReaderForRoles extends JSONReaderForArrayEntity<RoleList> {

    JSONReaderForRole jsonReaderForRole = new JSONReaderForRole();

    @Override
    public RoleList readFrom(Class<RoleList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String outerObject = JSONConstants.ROLES;
        final Class<RoleList> entityType = RoleList.class;

        try {
            String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);
            JSONArray inner = (JSONArray) outer.get(outerObject);

            if (inner == null) {
                throw new BadRequestException("Invalid json");
            } else if (inner.size() <= 0) {
                return entityType.newInstance();
            } else {
                RoleList roles = new RoleList();
                for (Object object : inner) {
                    if (object instanceof JSONObject) {
                        JSONObject obj = new JSONObject();
                        obj.put(JSONConstants.ROLE, object);
                        Role role = jsonReaderForRole.readFrom(Role.class, null, null, null, null,  IOUtils.toInputStream(obj.toJSONString()));
                        roles.getRole().add(role);
                    }
                }
                return roles;
            }
        } catch (ParseException | IOException | IllegalAccessException | InstantiationException e) {
            throw new BadRequestException("Invalid json");
        }
    }
}
