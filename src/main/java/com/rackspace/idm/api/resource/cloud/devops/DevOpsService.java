package com.rackspace.idm.api.resource.cloud.devops;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/26/13
 * Time: 1:49 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DevOpsService {
    void encryptUsers(String authToken);

    /**
     * Retrieves a log of ldap calls made while processing a previous request where the X-LOG-LDAP header (with a value of true) to the request
     *
     * Only callable by service admins and when the configuration property "allow.ldap.logging" is set to true in the configuration files
     *
     * @param uriInfo
     * @param authToken
     * @param logName
     * @return
     */
    Response.ResponseBuilder getLdapLog(UriInfo uriInfo, String authToken, String logName);

    /**
     * Retrieves the current node state for KeyCzar cached keys.
     *
     * @param authToken
     *
     * @return metadata map.
     */
    Response.ResponseBuilder getKeyMetadata(String authToken);

    /**
     * Reset the KeyCzar cache.
     *
     * @param authToken
     *
     * @return metadata map.
     */
    Response.ResponseBuilder resetKeyMetadata(String authToken);

    /**
     * Retrieves IDM properties.
     *
     * @param authToken
     *
     * @return properties map.
     */
    Response.ResponseBuilder getIdmProps(String authToken);

    /**
     * Remove Federated Users eligible for deletion
     *
     * @param authToken
     *
     * @return properties map.
     */
    Response.ResponseBuilder expiredFederatedUsersDeletion(String authToken, FederatedUsersDeletionRequest request);

    /**
     * Setup SMS MFA on the specified user. Must specify the phone number. Will link user to phone and mark the phone
     * as verified. May optionally enable MFA for the user as well.
     *
     * Caller must have the role "identity:mfa-admin"
     *
     * @param authToken
     * @param userId
     * @param mobilePhone
     * @return
     */
    Response.ResponseBuilder setupSmsMfaOnUser(String authToken, String userId, MobilePhone mobilePhone);
}
