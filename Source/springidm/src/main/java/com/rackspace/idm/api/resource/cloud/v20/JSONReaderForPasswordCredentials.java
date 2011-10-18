package com.rackspace.idm.api.resource.cloud.v20;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;

public class JSONReaderForPasswordCredentials {

    public static PasswordCredentialsRequiredUsername getPasswordCredentialsFromJSONString(String jsonBody) {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.PASSWORD_CREDENTIALS)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.PASSWORD_CREDENTIALS).toString());
                Object username = obj3.get(JSONConstants.USERNAME);
                Object password = obj3.get(JSONConstants.PASSWORD);

                if (username != null) {
                    creds.setUsername(username.toString());
                }
                if (password != null) {
                    creds.setPassword(password.toString());
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return creds;
    }
    
    public static PasswordCredentialsRequiredUsername checkAndGetPasswordCredentialsFromJSONString(String jsonBody) {
        PasswordCredentialsRequiredUsername creds = getPasswordCredentialsFromJSONString(jsonBody);
        
        if (StringUtils.isBlank(creds.getUsername())) {
            throw new BadRequestException("Expecting username");
        }
        if (StringUtils.isBlank(creds.getPassword())) {
            throw new BadRequestException("Expecting password");
        }
        
        return creds;
    }
}
