package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;

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

        String jsonBody = IOUtils.toString(inputStream, "UTF-8");

        SecretQA secrets = new SecretQA();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey("RAX-KSQA:secretQA")) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    "RAX-KSQA:secretQA").toString());
                
                Object answer = obj3.get("answer");
                Object question = obj3.get("question");

                if (answer != null) {
                    secrets.setAnswer(answer.toString());
                }
                if (question != null) {
                    secrets.setQuestion(question.toString());
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return secrets;
    }
}
