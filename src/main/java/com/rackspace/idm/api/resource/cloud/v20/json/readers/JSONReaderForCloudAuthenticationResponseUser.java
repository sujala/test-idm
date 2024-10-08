package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.IdmException;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Duration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;


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
                    Object userDomain = userJson.get(JSONConstants.RAX_AUTH_DOMAIN_ID);
                    Object fedIdp = userJson.get(JSONConstants.RAX_AUTH_FEDERATED_IDP);
                    Object contactId = userJson.get(JSONConstants.RAX_AUTH_CONTACT_ID);
                    Object sessionInactivityTimeout = userJson.get(JSONConstants.RAX_AUTH_SESSION_INACTIVITY_TIMEOUT);
                    Object delegationAgreementId = userJson.get(JSONConstants.RAX_AUTH_DELEGATION_AGREEMENT_ID);
                    Object phonePin = userJson.get(JSONConstants.RAX_AUTH_PHONE_PIN);
                    Object phonePinState = userJson.get(JSONConstants.RAX_AUTH_PHONE_PIN_STATE);

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

                    if(userDomain != null){
                        user.setDomainId(userDomain.toString());
                    }

                    if(fedIdp != null){
                        user.setFederatedIdp(fedIdp.toString());
                    }

                    if (contactId != null) {
                        user.setContactId(contactId.toString());
                    }

                    if (phonePin != null) {
                        user.setPhonePin(phonePin.toString());
                    }

                    if (phonePinState != null) {
                        user.setPhonePinState(PhonePinStateEnum.fromValue(phonePinState.toString()));
                    }

                    if (sessionInactivityTimeout != null && StringUtils.isNotBlank(sessionInactivityTimeout.toString())) {
                        try {
                            user.setSessionInactivityTimeout(DatatypeFactory.newInstance().newDuration(sessionInactivityTimeout.toString()));
                        } catch (Exception e) {
                            LOGGER.error("Error converting session inactivity timeout to duration");
                        }
                    }

                    if (delegationAgreementId != null) {
                        user.setDelegationAgreementId(delegationAgreementId.toString());
                    }

                    if (userRolesArray != null) {
                        RoleList roles = new RoleList();
                        for (Object role : userRolesArray) {
                            Object roleId = ((JSONObject)role).get(JSONConstants.ID);
                            Object serviceId = ((JSONObject)role).get(JSONConstants.SERVICE_ID);
                            Object description = ((JSONObject)role).get(JSONConstants.DESCRIPTION);
                            Object name = ((JSONObject)role).get(JSONConstants.NAME);
                            Object tenantId = ((JSONObject)role).get(JSONConstants.TENANT_ID);

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
                            if (tenantId != null) {
                                newRole.setTenantId(tenantId.toString());
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
