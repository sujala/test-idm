package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.IdmException;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 8/2/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForCloudAuthenticationResponseToken {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONReaderForCloudAuthenticationResponseToken.class);

    public static Token getAuthenticationResponseTokenFromJSONString(
            String jsonBody) {
        Token token = new Token();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if(outer.containsKey(JSONConstants.ACCESS)){
                JSONObject accessJson = (JSONObject) parser.parse(outer.get(JSONConstants.ACCESS).toString());

                if(accessJson.containsKey(JSONConstants.TOKEN)){

                    JSONObject tokenJson = (JSONObject) parser.parse(accessJson.get(JSONConstants.TOKEN).toString());
                    Object tokenId = tokenJson.get(JSONConstants.ID);
                    Object tokenExpiration = tokenJson.get(JSONConstants.EXPIRES);

                    if(tokenId != null){
                        token.setId(tokenId.toString());
                    }

                    if(tokenExpiration != null){
                        token.setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                                            new DateTime(tokenExpiration.toString()).toGregorianCalendar()));
                    }

                    if(tokenJson.containsKey(JSONConstants.TENANT)){
                        JSONObject tenantJson;
                        tenantJson = (JSONObject) parser.parse(tokenJson.get(JSONConstants.TENANT).toString());
                        TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
                        token.setTenant(tenant);

                        Object tenantId = tenantJson.get(JSONConstants.ID);
                        Object tenantName = tenantJson.get(JSONConstants.NAME);

                        if(tenantId != null){
                            tenant.setId(tenantId.toString());
                        }

                        if(tenantName != null){
                            tenant.setName(tenantName.toString());
                        }
                    }
                }
            }

        } catch (ParseException e) {
            LOGGER.info(e.toString());
            throw new IdmException("unable to parse Cloud AuthenticationResponse token", e);
        } catch (DatatypeConfigurationException e) {
            LOGGER.info(e.toString());
            throw new IdmException("unable to parse Cloud AuthenticationResponse token expiration", e);
        }
        return token;
    }
}
