package com.rackspace.idm.api.resource.cloud.devops;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionRequest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

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
     * Remove obsolete TRRs. Must have "identity:purge-trr" role
     *
     * @param authToken
     *
     * @return properties map.
     */
    Response.ResponseBuilder purgeObsoleteTrrs(String authToken, TokenRevocationRecordDeletionRequest request);

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

    /**
     * Remove SMS MFA from the specified user. Will error if user has OTP MFA enabled.
     *
     * Caller must have the role "identity:mfa-admin"
     *
     * @param authToken
     * @param userId
     * @return
     */
    Response.ResponseBuilder removeMfaFromUser(String authToken, String userId);

    /**
     * Searches and returns all IdentityProperty entries found that match the identity property versions
     * and property name. The search by name and IDM version are case-insensitive. Both name and idmVersions
     * params are optional. If both are provided the property must match both name AND idmVersions. The idmVersions
     * are OR'd in the query if multiple versions are provided.
     *
     * NOTE: this does not return IdentityProperty objects if they are set to reloadable = false
     *
     * @param authToken
     * @param name
     * @param versions
     * @return
     */
    Response.ResponseBuilder getIdmPropsByQuery(String authToken, final List<String> versions, String name);

    /**
     * Creates the Identity property with the data provided in the identityProperty request property
     *
     * @param authToken
     * @param identityProperty
     * @return
     */
    Response.ResponseBuilder createIdmProperty(String authToken, IdentityProperty identityProperty);

    /**
     * Updates the Identity property with the data provided in the identityProperty request object
     *
     * @param authToken
     * @param idmPropertyId
     * @param identityProperty
     * @return
     */
    Response.ResponseBuilder updateIdmProperty(String authToken, String idmPropertyId, IdentityProperty identityProperty);

    /**
     * Deletes the Identity property specified by the given identity property ID
     *
     * @param authToken
     * @param idmPropertyId
     * @return
     */
    Response.ResponseBuilder deleteIdmProperty(String authToken, String idmPropertyId);

}
