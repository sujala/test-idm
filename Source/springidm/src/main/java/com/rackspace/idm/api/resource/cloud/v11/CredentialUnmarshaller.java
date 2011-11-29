package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 11/22/11
 * Time: 12:20 AM
 */
@Component
public class CredentialUnmarshaller {

    private static final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public JAXBElement<? extends Credentials> unmarshallCredentialsFromJSON(String jsonBody) {

        JSONParser parser = new JSONParser();
        JAXBElement<? extends Credentials> creds = null;

        try {
            JSONObject obj = (JSONObject) parser.parse(jsonBody);
            if (obj.containsKey(JSONConstants.CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse((String) (obj.get(JSONConstants.CREDENTIALS)));
                UserCredentials userCreds = new UserCredentials();
                userCreds.setKey((String) (obj3.get(JSONConstants.KEY)));
                userCreds.setUsername((String) (obj3.get(JSONConstants.USERNAME)));
                creds = OBJ_FACTORY.createCredentials(userCreds);

            } else if (obj.containsKey(JSONConstants.MOSSO_CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(JSONConstants.MOSSO_CREDENTIALS).toString());
                MossoCredentials mossoCreds = new MossoCredentials();
                mossoCreds.setKey((String) (obj3.get(JSONConstants.KEY)));
                Object mossoId = obj3.get(JSONConstants.MOSSO_ID);
                if (mossoId != null) {
                    mossoCreds.setMossoId(Integer.parseInt(mossoId.toString()));
                }
                creds = OBJ_FACTORY.createMossoCredentials(mossoCreds);

            } else if (obj.containsKey(JSONConstants.NAST_CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse((String) (obj.get(JSONConstants.NAST_CREDENTIALS)));
                NastCredentials nastCreds = new NastCredentials();
                nastCreds.setKey((String) (obj3.get(JSONConstants.KEY)));
                nastCreds.setNastId((String) (obj3.get(JSONConstants.NAST_ID)));
                creds = OBJ_FACTORY.createNastCredentials(nastCreds);

            } else if (obj.containsKey(JSONConstants.PASSWORD_CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse((String) (obj.get(JSONConstants.PASSWORD_CREDENTIALS)));
                PasswordCredentials passwordCreds = new PasswordCredentials();
                passwordCreds.setUsername((String) (obj3.get(JSONConstants.USERNAME)));
                passwordCreds.setPassword((String) (obj3.get(JSONConstants.PASSWORD)));
                creds = OBJ_FACTORY.createPasswordCredentials(passwordCreds);
            }
        } catch (ParseException e) {
            throw new BadRequestException("malformed JSON");
        }
        return creds;
    }

}
