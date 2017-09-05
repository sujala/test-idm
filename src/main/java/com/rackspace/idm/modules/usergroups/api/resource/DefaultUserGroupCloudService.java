package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignment;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Component
public class DefaultUserGroupCloudService implements UserGroupCloudService {
    @Override
    public Response addGroup(UriInfo uriInfo, String authToken, UserGroup group) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response getGroupByDomainIdAndId(String authToken, String domainId, String groupId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response updateGroup(String authToken, UserGroup group) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response deleteGroup(String authToken, String domainId, String groupId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response listGroupsForDomain(String authToken, String domainId, UserGroupSearchParams searchCriteria) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response getUsersInGroup(String authToken, String domainId, String groupId, UserSearchCriteria userSearchCriteria) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response addUserToGroup(String authToken, String domainId, String groupId, String userId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response removeUserFromGroup(String authToken, String domainId, String groupId, String userId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response grantRoleToGroup(String authToken, String domainId, String groupId, RoleAssignment roleAssignment) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response grantRoleOnTenantToGroup(String authToken, String domainId, String groupId, String roleId, String tenantId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response grantRolesToGroup(String authToken, String domainId, String groupId, RoleAssignments roleAssignments) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response revokeRoleFromGroup(String authToken, String domainId, String groupId, String roleId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response revokeRoleOnTenantToGroup(String authToken, String domainId, String groupId, String roleId, String tenantId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response getRolesOnGroup(String authToken, String domainId, String groupId, UserGroupRoleSearchParams userGroupRoleSearchParams) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response getRoleOnGroup(String authToken, String domainId, String groupId, String roleId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }
}