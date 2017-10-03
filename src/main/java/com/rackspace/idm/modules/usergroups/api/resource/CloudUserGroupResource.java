package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignment;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import static com.rackspace.idm.api.resource.cloud.v20.Cloud20VersionResource.*;

/**
 * The top level Resource defining the API endpoints for managing user groups. This really just serves as an forwarder/adapter
 * to the underlying UserGroupCloudService.
 *
 * Path to get here is 'RAX-AUTH/domains/{domainId}/groups'
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CloudUserGroupResource {

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private UserGroupCloudService userGroupCloudService;

    @POST
    public Response addGroup(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            UserGroup group) {
        group.setDomainId(domainId); //overwrite any domainId value specified in request w/ path param
        group.setId(null);
        return userGroupCloudService.addGroup(uriInfo, authToken, group);
    }

    @GET
    public Response getGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @QueryParam("name") String name,
            @QueryParam("userId") String userId) {
        return userGroupCloudService.listGroupsForDomain(authToken, domainId, new UserGroupSearchParams(name, userId));
    }

    @GET
    @Path("/{groupId}")
    public Response getGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId) {
        return userGroupCloudService.getGroupByIdForDomain(authToken, groupId, domainId);
    }

    @PUT
    @Path("/{groupId}")
    public Response updateGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            UserGroup group) {
        group.setDomainId(domainId); //overwrite any domainId value specified in request w/ path param
        group.setId(groupId); //overwrite any groupId value specified in request w/ path param
        return userGroupCloudService.updateGroup(authToken, group);
    }

    @DELETE
    @Path("/{groupId}")
    public Response deleteGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId) {
        return userGroupCloudService.deleteGroup(authToken, domainId, groupId);
    }

    @GET
    @Path("/{groupId}/users")
    public Response getUsersInGroup(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return userGroupCloudService.getUsersInGroup(uriInfo, authToken, domainId, groupId, new UserSearchCriteria(new PaginationParams(validateMarker(marker), validateLimit(limit))));
    }

    @PUT
    @Path("/{groupId}/users/{userId}")
    public Response addUserToGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam(GROUP_ID_PATH_PARAM_NAME) String groupId,
            @PathParam(USER_ID_PATH_PARAM_NAME) String userId) {
        return userGroupCloudService.addUserToGroup(authToken, domainId, groupId, userId);
    }

    @DELETE
    @Path("/{groupId}/users/{userId}")
    public Response removeUserFromGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam(GROUP_ID_PATH_PARAM_NAME) String groupId,
            @PathParam(USER_ID_PATH_PARAM_NAME) String userId) {
        return userGroupCloudService.removeUserFromGroup(authToken, domainId, groupId, userId);
    }

    @PUT
    @Path("/{groupId}/roles/{roleId}")
    public Response grantRoleToGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            @PathParam("roleId") String roleId,
            RoleAssignment roleAssignment) {
        // A null assignment (no request body) is shorthand for a domain (aka globally) assigned role. Convert to that.
        if (roleAssignment == null) {
            roleAssignment = new TenantAssignment();
        }
        roleAssignment.setOnRole(roleId); // Always set the roleId based on the path
        return userGroupCloudService.grantRoleToGroup(authToken, domainId, groupId, roleAssignment);
    }

    @PUT
    @Path("/{groupId}/roles")
    public Response grantRolesToGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            RoleAssignments roleAssignments) {
        return userGroupCloudService.grantRolesToGroup(authToken, domainId, groupId, roleAssignments);
    }

    @PUT
    @Path("/{groupId}/roles/{roleId}/tenants/{tenantId}")
    public Response grantRoleOnTenantOnGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            @PathParam("roleId") String roleId,
            @PathParam("tenantId") String tenantId) {
        return userGroupCloudService.grantRoleOnTenantToGroup(authToken, domainId, groupId, roleId, tenantId);
    }

    @DELETE
    @Path("/{groupId}/roles/{roleId}")
    public Response revokeRoleFromGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            @PathParam("roleId") String roleId) {
        return userGroupCloudService.revokeRoleFromGroup(authToken, domainId, groupId, roleId);
    }

    @GET
    @Path("/{groupId}/roles")
    public Response getRolesOnGroup(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return userGroupCloudService.listRoleAssignmentsOnGroup(uriInfo, authToken, domainId, groupId, new UserGroupRoleSearchParams(new PaginationParams(validateMarker(marker), validateLimit(limit))));
    }

    @GET
    @Path("/{groupId}/roles/{roleId}")
    public Response getRoleOnGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam(DOMAIN_ID_PATH_PARAM_NAME) String domainId,
            @PathParam("groupId") String groupId,
            @PathParam("roleId") String roleId) {
        return userGroupCloudService.getRoleOnGroup(authToken, domainId, groupId, roleId);
    }

    protected int validateMarker(Integer offset) {
        if (offset == null) {
            return 0;
        }
        if (offset < 0) {
            throw new BadRequestException("Marker must be non negative");
        }
        return offset;
    }

    protected int validateLimit(Integer limit) {
        if (limit == null) {
            return identityConfig.getStaticConfig().getLdapPagingDefault();
        }
        if (limit < 0) {
            throw new BadRequestException("Limit must be non negative");
        } else if (limit == 0) {
            return identityConfig.getStaticConfig().getLdapPagingDefault();
        } else if (limit > identityConfig.getStaticConfig().getLdapPagingMaximum()) {
            return identityConfig.getStaticConfig().getLdapPagingMaximum();
        } else {
            return limit;
        }
    }
}
