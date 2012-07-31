package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
public class JSONReaderForSecretQA implements
MessageBodyReader<SecretQA> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == SecretQA.class;
    }

    @Override
    public SecretQA readFrom(Class<SecretQA> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        SecretQA secrets = getSecretQAFromJSONString(jsonBody);

        return secrets;
    }
    
    public static SecretQA getSecretQAFromJSONString(String jsonBody) {
        SecretQA secrets = new SecretQA();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.SECRET_QA)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.SECRET_QA).toString());
                
                Object answer = obj3.get(JSONConstants.ANSWER);
                Object question = obj3.get(JSONConstants.QUESTION);

                if (answer != null) {
                    secrets.setAnswer(answer.toString());
                }
                if (question != null) {
                    secrets.setQuestion(question.toString());
                }
            }
        } catch (ParseException e) {
            throw new BadRequestException("Unable to parse data in request body. Please review JSON formatting", e);
        }

        return secrets;
    }
}
