package com.rackspace.idm.api.resource.cloud.JSONReaders;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonPrefixMapper;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.VersionForService;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;
import static com.rackspace.idm.JSONConstants.ENDPOINT;
import static com.rackspace.idm.JSONConstants.VERSION;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForEndpoint implements MessageBodyReader<Endpoint> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == Endpoint.class;
    }

    @Override
    public Endpoint readFrom(Class<Endpoint> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put("endpoint.endpoint_links", JSONConstants.LINK);

        return read(inputStream, JSONConstants.ENDPOINT, prefixValues);
    }

    protected Endpoint read(InputStream entityStream, String rootValue, HashMap prefixValues) {
        try {

            String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);


            if (outer == null || outer.keySet().size() < 1) {
                throw new BadRequestException("Invalid json request body");
            }

            VersionForService versionForService = new VersionForService();
            String id = (String)((JSONObject)outer.get(ENDPOINT)).get(VERSION_ID);
            if(id != null){
                versionForService.setId(id);
            }
            String info = (String)((JSONObject)outer.get(ENDPOINT)).get(VERSION_INFO);
            if(info != null){
                versionForService.setInfo(info);
            }
            String list = (String)((JSONObject)outer.get(ENDPOINT)).get(VERSION_LIST);
            if(list != null){
                versionForService.setList(list);
            }

            ((JSONObject)outer.get(ENDPOINT)).remove(VERSION_ID);
            ((JSONObject)outer.get(ENDPOINT)).remove(VERSION_INFO);
            ((JSONObject)outer.get(ENDPOINT)).remove(VERSION_LIST);

            String rootElement = outer.keySet().iterator().next().toString();
            if(!rootElement.equals(rootValue)){
                throw new BadRequestException("Invalid json request body");
            }

            JSONObject jsonObject;

            if(prefixValues != null){
                jsonObject = prefixMapper.addPrefix(outer, prefixValues);
            }else {
                jsonObject = outer;
            }

            String jsonString = jsonObject.toString();
            ObjectMapper om = new ObjectMapper();
            om.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
            om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
            Endpoint endpoint = om.readValue(jsonString.getBytes(), Endpoint.class);
            endpoint.setVersion(versionForService);
            return endpoint;

        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (IOException e) {
            throw new BadRequestException("Invalid json request body");
        }
    }
    
}
