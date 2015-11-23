package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.AlwaysPluralizeJsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.NeverPluralizeJsonArrayTransformerHandler;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Tenants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthIdentityProviders  extends JSONReaderForArrayEntity<IdentityProviders> {

    @Getter
    @Setter
    JSONReaderForRaxAuthIdentityProvider jsonReaderForRaxAuthIdentityProvider = new JSONReaderForRaxAuthIdentityProvider();

    @Override
    public IdentityProviders readFrom(Class<IdentityProviders> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String outerObject = JSONConstants.RAX_AUTH_IDENTITY_PROVIDERS;
        final Class<IdentityProviders> entityType = IdentityProviders.class;

        try {
            String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);
            JSONArray inner = (JSONArray) outer.get(outerObject);

            if (inner == null) {
                throw new BadRequestException("Invalid json request body");
            } else if (inner.size() <= 0) {
                return entityType.newInstance();
            } else {
                IdentityProviders identityProviders = new IdentityProviders();
                for (Object object : inner) {
                    if (object instanceof JSONObject) {
                        JSONObject obj = new JSONObject();
                        obj.put(JSONConstants.RAX_AUTH_IDENTITY_PROVIDER, object);
                        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider provider = jsonReaderForRaxAuthIdentityProvider.readFrom(com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider.class, null, null, null, null,  IOUtils.toInputStream(obj.toJSONString()));
                        identityProviders.getIdentityProvider().add(provider);
                    }
                }
                return identityProviders;
            }
        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (IOException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (InstantiationException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (IllegalAccessException e) {
            throw new BadRequestException("Invalid json request body");
        }
    }
}
