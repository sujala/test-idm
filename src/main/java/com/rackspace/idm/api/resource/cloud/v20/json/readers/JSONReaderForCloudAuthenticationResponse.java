package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.IdmException;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;


public class JSONReaderForCloudAuthenticationResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONReaderForCloudAuthenticationResponse.class);

    public static AuthenticateResponse getAuthenticationResponseFromJSONString(
            String jsonBody) {
        AuthenticateResponse response = new AuthenticateResponse();

        Token token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(jsonBody);
        response.setToken(token);

        UserForAuthenticateResponse user = JSONReaderForCloudAuthenticationResponseUser.getAuthenticationResponseUserFromJSONString(jsonBody);
        response.setUser(user);

        ServiceCatalog serviceCatalog = JSONReaderForCloudAuthenticationResponseServiceCatalog.parse(jsonBody);
        response.setServiceCatalog(serviceCatalog);

        return response;
    }
}
