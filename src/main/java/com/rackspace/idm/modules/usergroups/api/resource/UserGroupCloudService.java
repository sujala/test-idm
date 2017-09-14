package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignment;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Provides services to adapt REST based service requests into backend business services. Each service is expected to:
 * <ol>
 *     <li>Perform the necessary authorization of the REST caller and populate the RequestContext with the appropriate security
 * context as necessary.</li>
 * <li>Perform some degree of validation that the REST request is valid. This will vary depending on level of validation performed
 * by the underlying business service</li>
 * <li>Translate result to appropriate REST based response for the caller.</li>
 * <li>Translate any error/exception received from backend services to the appropriate REST based error response. None of
 * these services should cause an exception to escape (e.g. get bubbled up to caller of these methods).</li>
 * </ol>
 */
public interface UserGroupCloudService {

    /**
     * Add a new user group to the specified domain. The name of the group must be unique across all groups
     * within the domain.
     *
     * On success returns:
     * <ol>
     *     <li>A 201 response</li>
     *     <li>A location header w/ value set to the GET REST call to retrieve the newly created user group</li>
     *     <li>The created user group in the response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain does not exist</li>
     *     <li>400 - If the specified group does not meet validation requirements for creating a new group</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param uriInfo
     * @param authToken
     * @param group
     *
     * @return
     */
    Response addGroup(UriInfo uriInfo, String authToken, UserGroup group);

    /**
     * Returns the specified group that exists within the specified domain.
     *
     * On success returns:
     * <ol>
     *     <li>200 response</li>
     *     <li>The specified user group in the response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group doesn't exist
     *     <li>403 - If the group w/ the specified ID exists in a different domain</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param groupId
     * @param domainId
     * @return
     */
    Response getGroupByIdForDomain(String authToken, String groupId, String domainId);

    /**
     * Update the name and/or description of the user group. Can not be used to switch a group's id or domain. An update of
     * an attribute will only occur if the value is not blank (e.g not whitespace only or null).
     *
     * On success returns:
     * <ol>
     *     <li>200 response</li>
     *     <li>The updated user group in the response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group does not exist</li>
     *     <li>403 - If the group w/ the specified ID exists in a different domain</li>
     *     <li>400 - If the specified group does not meet validation requirements for updating the group</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param group
     * @return
     */
    Response updateGroup(String authToken, UserGroup group);

    /**
     * Deletes the specified group from the specified domain
     *
     * On success returns:
     * <ol>
     *     <li>204 response</li>
     *     <li>No response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group does not exist</li>
     *     <li>403 - If the group w/ the specified ID exists in a different domain</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @return
     */
    Response deleteGroup(String authToken, String domainId, String groupId);

    /**
     * Returns an array of groups within a domain, filtered by the specified criteria.
     *
     * On success returns:
     * <ol>
     *     <li>200 response</li>
     *     <li>An array of group entries. If no groups exist that match
     * the criteria, an empty array will be returned.</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>400 - If the search criteria are invalid</li>
     *     <li>404 - If the domain does not exist</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param searchCriteria
     * @return
     */
    Response listGroupsForDomain(String authToken, String domainId, UserGroupSearchParams searchCriteria);

    /**
     * Returns an array of users within a group, filtered by the specified criteria.
     *
     * On success returns:
     * <ol>
     *     <li>200 response</li>
     *     <li>An array of user entries. If no user exist that match
     * the criteria, an empty array will be returned.</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group does not exist</li>
     *     <li>403 - If the group w/ the specified ID exists in a different domain</li>
     *     <li>400 - If the search criteria are invalid</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param userSearchCriteria
     * @return
     */
    Response getUsersInGroup(String authToken, String domainId, String groupId, UserSearchCriteria userSearchCriteria);

    /**
     * Add the specified user to the specified user group. If the user is already a member of the group, this results
     * in a no-op.
     *
     * On success returns:
     * <ol>
     *     <li>204 response</li>
     *     <li>No response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain, user, or group does not exist</li>
     *     <li>403 - If the group or user exists in a different domain</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param userId
     * @return
     */
    Response addUserToGroup(String authToken, String domainId, String groupId, String userId);

    /**
     * Remove the specified user from the specified user group. If the user is not a member of the group (regardless of
     * whether the user actually exists), this results in a no-op with a success result (as the goal of the user not
     * belong to the group is "achieved").
     *
     * On success returns:
     * <ol>
     *     <li>204 response</li>
     *     <li>No response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group does not exist</li>
     *     <li>403 - If the group exists in a different domain</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param userId
     * @return
     */
    Response removeUserFromGroup(String authToken, String domainId, String groupId, String userId);

    /**
     * Grant or update the role assignment to the specified user group. If the role is currently assigned to the group
     * it is replaced with this one.
     *
     * On success returns:
     * <ol>
     *     <li>A 200 response</li>
     *     <li>The created/updated role assignment in the response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain, group, or role does not exist</li>
     *     <li>403 - If the group exists in a different domain or the role can not be assigned to a user group</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param roleAssignment
     * @return
     */
    Response grantRoleToGroup(String authToken, String domainId, String groupId, RoleAssignment roleAssignment);

    /**
     * Grant or update the role assignments to the specified user group. If any role is currently assigned to the group
     * it is replaced with the provided one. A given role can only appear once in the list of roles to assign. The same
     * constraints apply for each individual role assignment specified as if they were being assigned individually. The
     * entire request will be validated prior to assigning any roles.
     *
     * If the request is deemed valid, the assignments are iterated over to apply. If an error is encountered, processing will stop
     * on the current assignment, but no efforts will be made to rollback previously successful assignments. Upon receiving
     * an error the caller should verify the state of the user groups roles and take corrective action as necessary.
     *
     * On success returns:
     * <ol>
     *     <li>A 200 response</li>
     *     <li>The response body is the final role assignments associated with the user group after applying the updates. It will
     *     include all the roles assigned to the group regardless of when or how it was assigned.</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group does not exist, the group exists in a different domain, or specified role does not exist</li>
     *     <li>400 - If the request does not meet validation requirements.</li>
     *     <li>403 - If the group exists in a different domain or a role can not be assigned to the user group</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param roleAssignments
     * @return
     */
    Response grantRolesToGroup(String authToken, String domainId, String groupId, RoleAssignments roleAssignments);

    /**
     * Grant or update the tenant role assignment to the specified user group. If the group has already been assigned
     * this role, the assignment must be a tenant based assignment or an error will be returned. If the group already
     * has the role assigned on the tenant, this is a no-op.
     *
     * The tenant must belong to the same domain as the group.
     *
     * On success returns:
     * <ol>
     *     <li>A 204 response</li>
     *     <li>No response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain, tenant, role, or group does not exist
     *     <li>403 - If group/tenant exists in a different domain or the role can not be assigned to the user group</li>
     *     <li>409 - If the group is already assigned the role as a domain (global) role</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param tenantId
     * @return
     */
    Response grantRoleOnTenantToGroup(String authToken, String domainId, String groupId, String roleId, String tenantId);

    /**
     * Remove the assignment of the specified role from the group. If the role is not currently assigned to the group (regardless
     * of whether the role actually exists), the call returns successful, but performs no actions.
     *
     * On success returns:
     * <ol>
     *     <li>A 204 response</li>
     *     <li>No response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group does not exist</li>
     *     <li>403 - If the group exists in a different domain</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param roleId
     * @return
     */
    Response revokeRoleFromGroup(String authToken, String domainId, String groupId, String roleId);

    /**
     * Removes a tenant role assignment to the specified user group. If the group has already been assigned
     * this role, the assignment must be a tenant based assignment or an error will be returned. If the group does
     * not have the role assigned on the tenant, this is a no-op.
     *
     * The tenant must belong to the same domain as the group.
     *
     * On success returns:
     * <ol>
     *     <li>A 204 response</li>
     *     <li>No response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain, tenant, role, or group does not exist
     *     <li>403 - If group/tenant exists in a different domain or the role can not be assigned to the user group</li>
     *     <li>409 - If the group is already assigned the role as a domain (global) role</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param tenantId
     * @return
     */
    Response revokeRoleOnTenantToGroup(String authToken, String domainId, String groupId, String roleId, String tenantId);

    /**
     * Returns an array of RoleAssignments on the specified group that meet the specified search criteria.
     *
     * On success returns:
     * <ol>
     *     <li>A 204 response</li>
     *     <li>An array of RoleAssignment entries. If no assignments exist that match
     * the criteria, an empty array is returned.</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain or group does not exist</li>
     *     <li>403 - If the group exists in a different domain</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param userGroupRoleSearchParams
     * @return
     */
    Response getRolesOnGroup(String authToken, String domainId, String groupId, UserGroupRoleSearchParams userGroupRoleSearchParams);

    /**
     * Retrieve the role assignment on the group for the specified role.
     *
     * On success returns:
     * <ol>
     *     <li>A 200 response</li>
     *     <li>The role assignment in the response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the domain, group, or role does not exist or the role is not assigned to the group</li>
     *     <li>403 - If the group exists in a different domain</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param domainId
     * @param groupId
     * @param roleId
     * @return
     */
    Response getRoleOnGroup(String authToken, String domainId, String groupId, String roleId);
}
