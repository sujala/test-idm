package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.CredentialType;

public final class JSONReaderForCredentialType {

    private JSONReaderForCredentialType(){};

    public static CredentialType checkAndGetCredentialsFromJSONString(String jsonBody) {
        JSONParser parser = new JSONParser();
        CredentialType creds = null;

        try {
            JSONObject obj = (JSONObject) parser.parse(jsonBody);

            if (obj.containsKey(JSONConstants.PASSWORD_CREDENTIALS)) {

                creds = JSONReaderForPasswordCredentials
                    .checkAndGetPasswordCredentialsFromJSONString(jsonBody);

            } else if (obj.containsKey(JSONConstants.APIKEY_CREDENTIALS)) {

                creds = JSONReaderForApiKeyCredentials
                    .checkAndGetApiKeyCredentialsFromJSONString(jsonBody);

            } else {
                throw new BadRequestException("Unsupported credential type");
            }
        } catch (ParseException e) {
            throw new BadRequestException("malformed JSON", e);
        }
        return creds;
    }
}
