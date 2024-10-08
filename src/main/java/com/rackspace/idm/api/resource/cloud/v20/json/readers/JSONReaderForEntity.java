package com.rackspace.idm.api.resource.cloud.v20.json.readers;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.rackspace.idm.api.resource.cloud.AlwaysPluralizeJsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformer;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.JsonPrefixMapper;
import com.rackspace.idm.api.resource.cloud.NeverPluralizeJsonArrayTransformerHandler;
import com.rackspace.idm.exception.BadRequestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class JSONReaderForEntity<T> implements MessageBodyReader<T> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();
    private JsonArrayTransformer arrayTransformer = new JsonArrayTransformer();

    final private Class<T> entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    public static final JsonArrayTransformerHandler ALWAYS_PLURALIZE_HANDLER = new AlwaysPluralizeJsonArrayTransformerHandler();
    public static final JsonArrayTransformerHandler NEVER_PLURALIZE_HANDLER = new NeverPluralizeJsonArrayTransformerHandler();

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {
        return type == entityType;
    }

    protected T read(InputStream entityStream, String rootValue) {
        return read(entityStream, rootValue, null);
    }


    protected T read(InputStream entityStream, String rootValue, Map prefixValues) {
        return read(entityStream, rootValue, prefixValues, true);
    }

    protected T read(InputStream entityStream, String rootValue, Map prefixValues, boolean pluralizeArrays) {
        if (pluralizeArrays) {
            return read(entityStream, rootValue, prefixValues, ALWAYS_PLURALIZE_HANDLER);
        } else {
            return read(entityStream, rootValue, prefixValues, NEVER_PLURALIZE_HANDLER);
        }
    }

    protected T read(InputStream entityStream, String rootValue, Map prefixValues, JsonArrayTransformerHandler arrayTransformerHandler) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(new InputStreamReader(entityStream, StandardCharsets.UTF_8));

            if (outer == null || outer.keySet().size() < 1) {
                throw new BadRequestException("Invalid json request body");
            }

            String rootElement = outer.keySet().iterator().next().toString();
            if(!rootElement.equals(rootValue)){
                throw new BadRequestException("Invalid json request body");
            }

            /* When a new entity is created in our tests and no attributes are added, our parser converts the new entity
             * as: "{"rootElement" : null}". This prevents us from testing empty request like "{"rootElement" : {}}".
             * This check will ensure that a new instance of the entity is returned if the root element's value is set
             * null. If a failure occurs when creating new instance, a bad request error will be returned.
             */
            if (outer.get(rootElement) == null) {
                return entityType.newInstance();
            }

            JSONObject jsonObject;

            if(prefixValues != null){
                jsonObject = prefixMapper.mapPrefix(outer, prefixValues);
            }else {
                jsonObject = outer;
            }

            arrayTransformer.transformIncludeWrapper(jsonObject, arrayTransformerHandler);

            String jsonString = jsonObject.toString();
            ObjectMapper om = new ObjectMapper();
            om.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
            om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
            return om.readValue(jsonString.getBytes(), entityType);

        } catch (ParseException | IllegalAccessException | InstantiationException e) {
            throw new BadRequestException("Invalid json request body", e);
        } catch (InvalidFormatException e) {
            throw new BadRequestException(String.format("Invalid json request body for value '%s'", e.getValue()), e);
        } catch (IOException e) {
            throw new BadRequestException("Invalid json request body", e);
        }
    }
}
