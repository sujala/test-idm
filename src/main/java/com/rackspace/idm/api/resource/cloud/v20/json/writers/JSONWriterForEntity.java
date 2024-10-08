package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.*;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class JSONWriterForEntity <T> implements MessageBodyWriter<T> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();

    private JsonArrayTransformer arrayTransformer = new JsonArrayTransformer();

    final private Class<T> entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    public static final JsonArrayTransformerHandler ALWAYS_PLURALIZE_HANDLER = new AlwaysPluralizeJsonArrayTransformerHandler();
    public static final JsonArrayTransformerHandler NEVER_PLURALIZE_HANDLER = new NeverPluralizeJsonArrayTransformerHandler();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == entityType;
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    protected void write(T entity, OutputStream entityStream) {
        write(entity, entityStream, null);
    }

    protected void write(T entity, OutputStream entityStream, Map prefixValues) {
        write(entity, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER);
    }

    protected void write(T entity, OutputStream entityStream, Map prefixValues, JsonArrayEntryTransformer entryTransformer) {
        write(entity, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER, entryTransformer);
    }

    protected void write(T entity, OutputStream entityStream, Map prefixValues, JsonArrayTransformerHandler handler) {
        write(entity, entityStream, prefixValues, handler, null);
    }

    protected void write(T entity, OutputStream entityStream, Map prefixValues, JsonArrayTransformerHandler handler, JsonArrayEntryTransformer entryTransformer) {
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            getMarshaller().marshallToJSON(entity, outputStream);
            String jsonString = outputStream.toString();

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonString);

            JSONObject jsonObject = outer;

            if(prefixValues != null){
                jsonObject = prefixMapper.mapPrefix(jsonObject, prefixValues, entryTransformer);
            }

            arrayTransformer.transformRemoveWrapper(jsonObject, null, handler);

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
