package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.IdmException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JSONReaderForCloudAuthenticationResponseUser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONReaderForCloudAuthenticationResponseUser.class);

    public static UserForAuthenticateResponse getAuthenticationResponseUserFromJSONString(
            String jsonBody) {
        UserForAuthenticateResponse user = new UserForAuthenticateResponse();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if(outer.containsKey(JSONConstants.ACCESS)){
                JSONObject accessJson = (JSONObject) parser.parse(outer.get(JSONConstants.ACCESS).toString());

                if(accessJson.containsKey(JSONConstants.USER)){

                    JSONObject userJson = (JSONObject) parser.parse(accessJson.get(JSONConstants.USER).toString());
                    Object userId = userJson.get(JSONConstants.ID);
                    Object userName = userJson.get(JSONConstants.NAME);
                    Object userRegion = userJson.get(JSONConstants.RAX_AUTH_DEFAULT_REGION);
                    Object fedIdp = userJson.get(JSONConstants.RAX_AUTH_FEDERATED_IDP);

                    JSONArray userRolesArray = (JSONArray) userJson.get(JSONConstants.ROLES);

                    if(userId != null){
                        user.setId(userId.toString());
                    }

                    if(userName != null){
                        user.setName(userName.toString());
                    }

                    if(userRegion != null){
                        user.setDefaultRegion(userRegion.toString());
                    }

                    if(fedIdp != null){
                        user.setFederatedIdp(fedIdp.toString());
                    }


                    if (userRolesArray != null) {
                        RoleList roles = new RoleList();
                        for (Object role : userRolesArray) {
                            Object roleId = ((JSONObject)role).get(JSONConstants.ID);
                            Object serviceId = ((JSONObject)role).get(JSONConstants.SERVICE_ID);
                            Object description = ((JSONObject)role).get(JSONConstants.DESCRIPTION);
                            Object name = ((JSONObject)role).get(JSONConstants.NAME);

                            Role newRole = new Role();
                            if (roleId != null) {
                                newRole.setId(roleId.toString());
                            }
                            if (name != null) {
                                newRole.setName(name.toString());
                            }
                            if (description != null) {
                                newRole.setDescription(description.toString());
                            }
                            if (serviceId != null) {
                                newRole.setServiceId(serviceId.toString());
                            }

                            roles.getRole().add(newRole);
                        }
                        if (roles.getRole().size() > 0) {
                            user.setRoles(roles);
                        }
                    }
                }
            }

        } catch (ParseException e) {
            LOGGER.info(e.toString());
            throw new IdmException("unable to parse Cloud AuthenticationResponse token", e);
        }
        return user;
    }
}
