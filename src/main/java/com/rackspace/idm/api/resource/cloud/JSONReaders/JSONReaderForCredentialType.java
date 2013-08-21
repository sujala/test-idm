package com.rackspace.idm.api.resource.cloud.JSONReaders;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.rackspace.idm.JSONConstants.*;

public final class JSONReaderForCredentialType {

    private static JSONReaderForPasswordCredentials readerForPasswordCredentials = new JSONReaderForPasswordCredentials();
    private static JSONReaderForRaxKsKeyApiKeyCredentials readerForRaxKsKeyApiKeyCredentials = new JSONReaderForRaxKsKeyApiKeyCredentials();

    private JSONReaderForCredentialType(){};

    public static CredentialType checkAndGetCredentialsFromJSONString(String jsonBody) {
        JSONParser parser = new JSONParser();
        CredentialType creds = null;

        try {
            JSONObject obj = (JSONObject) parser.parse(jsonBody);

            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(jsonBody.getBytes());

            if (obj.containsKey(PASSWORD_CREDENTIALS)) {
                creds = readerForPasswordCredentials.readFrom(PasswordCredentialsBase.class, null, null, null, null, arrayInputStream);
            }  else if (obj.containsKey(RAX_KSKEY_API_KEY_CREDENTIALS)) {
                creds = readerForRaxKsKeyApiKeyCredentials.readFrom(ApiKeyCredentials.class, null , null, null, null, arrayInputStream);
            } else {
                throw new BadRequestException("Unsupported credential type");
            }
        } catch (ParseException e) {
            throw new BadRequestException("malformed JSON", e);
        } catch (IOException e) {
            throw new BadRequestException("malformed JSON", e);
        }
        return creds;
    }
}
