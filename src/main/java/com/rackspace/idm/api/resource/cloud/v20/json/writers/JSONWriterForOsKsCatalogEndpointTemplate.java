package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonPrefixMapper;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForOsKsCatalogEndpointTemplate extends JSONWriterForEntity<EndpointTemplate> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == EndpointTemplate.class;
    }

    @Override
    public long getSize(EndpointTemplate endpointTemplate, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(EndpointTemplate endpointTemplate, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(ENDPOINT_TEMPLATE, OS_KSCATALOG_ENDPOINT_TEMPLATE);

        write(endpointTemplate, entityStream, prefixValues);
    }

    protected void write(EndpointTemplate entity, OutputStream entityStream, HashMap prefixValues) {
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            getMarshaller().marshallToJSON(entity, outputStream);
            String jsonString = outputStream.toString();

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonString);

            JSONObject jsonObject;

            JSONObject version = (JSONObject) ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).get(VERSION);

            if(version != null){
                Object id = version.get(ID);
                if(id != null){
                    ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).put(VERSION_ID, id);
                }
                Object info = version.get(INFO);
                if(info != null){
                    ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).put(VERSION_INFO, info);
                }
                Object list = version.get(LIST);
                if(list != null){
                    ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).put(VERSION_LIST, list);
                }
                ((JSONObject)outer.get(ENDPOINT_TEMPLATE)).remove(VERSION);
            }

            if(prefixValues != null){
                jsonObject = prefixMapper.addPrefix(outer, prefixValues);
            }else{
                jsonObject = outer;
            }

            String newJsonString = jsonObject.toString();
            entityStream.write(newJsonString.getBytes(JSONConstants.UTF_8));

        } catch (JAXBException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (ParseException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (UnsupportedEncodingException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (IOException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        }
    }

    JSONMarshaller getMarshaller() throws JAXBException {
        return ((JSONJAXBContext) JAXBContextResolver.get()).createJSONMarshaller();
    }
}
