package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthDomains extends JSONReaderForArrayEntity<Domains> {

    JSONReaderRaxAuthForDomain jsonReaderRaxAuthForDomain = new JSONReaderRaxAuthForDomain();

    @Override
    public Domains readFrom(Class<Domains> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String outerObject = JSONConstants.RAX_AUTH_DOMAINS;
        final Class<Domains> entityType = Domains.class;

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
                Domains domains = new Domains();
                for (Object object : inner) {
                    if (object instanceof JSONObject) {
                        JSONObject obj = new JSONObject();
                        obj.put(JSONConstants.RAX_AUTH_DOMAIN, object);
                        Domain domain = jsonReaderRaxAuthForDomain.readFrom(Domain.class, null, null, null, null,  IOUtils.toInputStream(obj.toJSONString()));
                        domains.getDomain().add(domain);
                    }
                }
                return domains;
            }
        } catch (ParseException | IOException | IllegalAccessException | InstantiationException e) {
            throw new BadRequestException("Invalid json");
        }
    }
}
