package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserList;

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
public class JSONReaderForUserList extends JSONReaderForArrayEntity<UserList> {

    @Getter
    @Setter
    JSONReaderForUser jsonReaderForUser = new JSONReaderForUser();

    @Override
    public UserList readFrom(Class<UserList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String outerObject = JSONConstants.USERS;
        final Class<UserList> entityType = UserList.class;

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
                UserList userList = new UserList();
                for (Object object : inner) {
                    if (object instanceof JSONObject) {
                        JSONObject obj = new JSONObject();
                        obj.put(JSONConstants.USER, object);
                        User user = jsonReaderForUser.readFrom(User.class, null, null, null, null,  IOUtils.toInputStream(obj.toJSONString()));
                        userList.getUser().add(user);
                    }
                }
                return userList;
            }
        } catch (ParseException | IOException | InstantiationException | IllegalAccessException e) {
            throw new BadRequestException("Invalid json request body");
        }
    }
}
