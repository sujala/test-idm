package com.rackspace.idm.api.resource.cloud.v20.json.readers;

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
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
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

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForOsKsCatalogEndpointTemplate implements MessageBodyReader<EndpointTemplate> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == EndpointTemplate.class;
    }

    @Override
    public EndpointTemplate readFrom(Class<EndpointTemplate> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(OS_KSCATALOG_ENDPOINT_TEMPLATE, ENDPOINT_TEMPLATE);
        prefixValues.put(ENDPOINT_TEMPLATE_RAX_AUTH_TENANT_ALIAS_PATH, TENANT_ALIAS);

        return read(inputStream, OS_KSCATALOG_ENDPOINT_TEMPLATE, prefixValues);
    }

    protected EndpointTemplate read(InputStream entityStream, String rootValue, HashMap prefixValues) {
        try {

            String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);


            if (outer == null || outer.keySet().size() < 1) {
                throw new BadRequestException("Invalid json request body");
            }

            String rootElement = outer.keySet().iterator().next().toString();
            if(!rootElement.equals(rootValue)){
                throw new BadRequestException("Invalid json request body");
            }

            JSONObject jsonObject;

            if(prefixValues != null){
                jsonObject = prefixMapper.mapPrefix(outer, prefixValues);
            }else {
                jsonObject = outer;
            }

            VersionForService versionForService = new VersionForService();
            String id = (String)((JSONObject)outer.get(ENDPOINT_TEMPLATE)).get(VERSION_ID);
            if(id != null){
                versionForService.setId(id);
            }
            String info = (String)((JSONObject)outer.get(ENDPOINT_TEMPLATE)).get(VERSION_INFO);
            if(info != null){
                versionForService.setInfo(info);
            }
            String list = (String)((JSONObject)outer.get(ENDPOINT_TEMPLATE)).get(VERSION_LIST);
            if(list != null){
                versionForService.setList(list);
            }

            ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).remove(VERSION_ID);
            ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).remove(VERSION_INFO);
            ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).remove(VERSION_LIST);

            String jsonString = jsonObject.toString();
            ObjectMapper om = new ObjectMapper();
            om.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
            om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
            EndpointTemplate endpointTemplate = om.readValue(jsonString.getBytes(), EndpointTemplate.class);
            endpointTemplate.setVersion(versionForService);
            return endpointTemplate;

        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (IOException e) {
            throw new BadRequestException("Invalid json request body");
        }
    }
}
