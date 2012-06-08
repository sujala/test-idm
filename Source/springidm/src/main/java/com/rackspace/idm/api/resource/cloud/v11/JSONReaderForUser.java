package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
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
public class JSONReaderForUser implements MessageBodyReader<User> {

    private static Logger logger = LoggerFactory.getLogger(JSONReaderForUser.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == User.class;
    }

    @Override
    public User readFrom(Class<User> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        User object = getUserFromJSONString(jsonBody);

        return object;
    }

    public static User getUserFromJSONString(String jsonBody) {
        User user = new User();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.USER)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(JSONConstants.USER)
                    .toString());

                Object id = obj3.get(JSONConstants.ID);
                Object mossoId = obj3.get(JSONConstants.MOSSO_ID);
                Object nastId = obj3.get(JSONConstants.NAST_ID);
                Object key = obj3.get(JSONConstants.KEY);
                Object enabled = obj3.get(JSONConstants.ENABLED);

                Object baseUrlRefs = obj3.get(JSONConstants.BASE_URL_REFS);

                if (id != null) {
                    user.setId(id.toString());
                }
                if (mossoId != null) {
                    user.setMossoId(Integer.parseInt(mossoId.toString()));
                }
                if (nastId != null) {
                    user.setNastId(nastId.toString());
                }
                if (key != null) {
                    user.setKey(key.toString());
                }
                if (enabled != null) {
                    user.setEnabled(Boolean.valueOf(enabled.toString()));
                }

                if (baseUrlRefs != null) {
                    user.setBaseURLRefs(JSONReaderForBaseURLRefList.getBaseURLRefFromJSONString(obj3.toString()));
                }

            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON");
        }

        return user;
    }
}
