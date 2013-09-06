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
import org.openstack.docs.identity.api.v2.Endpoint;

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

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForEndpoint implements MessageBodyWriter<Endpoint> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Endpoint.class;
    }

    @Override
    public long getSize(Endpoint endpoint, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Endpoint endpoint, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put("endpoint.link", ENDPOINT_LINKS);
        write(endpoint, entityStream, prefixValues);
    }

    protected void write(Endpoint endpoint, OutputStream entityStream, HashMap prefixValues) {
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            getMarshaller().marshallToJSON(endpoint, outputStream);
            String jsonString = outputStream.toString();

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonString);

            JSONObject jsonObject;

            if(prefixValues != null){
                jsonObject = prefixMapper.addPrefix(outer, prefixValues);
            }else{
                jsonObject = outer;
            }

            JSONObject version = (JSONObject) ((JSONObject)outer.get(ENDPOINT)).get(VERSION);

            if(version != null){
                Object id = version.get(ID);
                if(id != null){
                    ((JSONObject)outer.get(ENDPOINT)).put(VERSION_ID, id);
                }
                Object info = version.get(INFO);
                if(info != null){
                    ((JSONObject)outer.get(ENDPOINT)).put(VERSION_INFO, info);
                }
                Object list = version.get(LIST);
                if(list != null){
                    ((JSONObject)outer.get(ENDPOINT)).put(VERSION_LIST, list);
                }
                ((JSONObject)outer.get(ENDPOINT)).remove(VERSION);
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
