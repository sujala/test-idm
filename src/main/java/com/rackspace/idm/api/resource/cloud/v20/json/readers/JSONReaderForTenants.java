package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.Tenants;

import javax.ws.rs.Consumes;
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
public class JSONReaderForTenants extends JSONReaderForArrayEntity<Tenants> {

    JSONReaderForTenant jsonReaderForTenant = new JSONReaderForTenant();

    @Override
    public Tenants readFrom(Class<Tenants> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String outerObject = JSONConstants.TENANTS;
        final Class<Tenants> entityType = Tenants.class;

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
                Tenants tenants = new Tenants();
                for (Object object : inner) {
                    if (object instanceof JSONObject) {
                        JSONObject obj = new JSONObject();
                        obj.put(JSONConstants.TENANT, object);
                        Tenant tenant = jsonReaderForTenant.readFrom(Tenant.class, null, null, null, null,  IOUtils.toInputStream(obj.toJSONString()));
                        tenants.getTenant().add(tenant);
                    }
                }
                return tenants;
            }
        } catch (ParseException | IOException | IllegalAccessException | InstantiationException e) {
            throw new BadRequestException("Invalid json");
        }
    }
}
