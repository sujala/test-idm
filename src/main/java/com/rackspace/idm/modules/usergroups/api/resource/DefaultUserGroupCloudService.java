package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignment;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;
import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.IdmExceptionHandler;
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter;
import com.rackspace.idm.modules.usergroups.api.resource.converter.UserGroupConverter;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultUserGroupCloudService implements UserGroupCloudService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserGroupCloudService.class);

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private UserGroupAuthorizationService userGroupAuthorizationService;

    @Autowired
    private RoleAssignmentConverter roleAssignmentConverter;

    @Autowired
    private DomainService domainService;

    @Autowired
    private UserGroupConverter userGroupConverter;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private IdmExceptionHandler idmExceptionHandler;

    @Autowired
    private IdmPathUtils idmPathUtils;

    @Override
    public Response addGroup(UriInfo uriInfo, String authToken, UserGroup group) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(group.getDomainId());

            // Convert to entity object
            com.rackspace.idm.modules.usergroups.entity.UserGroup userGroupEntity = userGroupConverter.fromUserGroupWeb(group);

            userGroupService.addGroup(userGroupEntity);

            URI location = idmPathUtils.createLocationHeaderValue(uriInfo, userGroupEntity.getId());
            Response.ResponseBuilder response = Response.created(location);
            response.entity(userGroupConverter.toUserGroupWeb(userGroupEntity));
            return response.build();
        } catch (Exception ex) {
            LOG.error(String.format("Error creating user group for domain %s", group.getDomainId()), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response getGroupByIdForDomain(String authToken, String groupId, String domainId) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            Response.ResponseBuilder response = Response.ok(userGroupConverter.toUserGroupWeb(group));
            return response.build();
        } catch (Exception ex) {
            LOG.error(String.format("Error retrieving user group for domain '%s' and groupId '%s'", domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response updateGroup(String authToken, UserGroup group) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response deleteGroup(String authToken, String domainId, String groupId) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify userGroup exists for domain
            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(group.getDomainId());

            userGroupService.deleteGroup(group);

            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.error(String.format("Error deleting user group for domain '%s' and groupId '%s'", domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response listGroupsForDomain(String authToken, String domainId, UserGroupSearchParams searchCriteria) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            List<com.rackspace.idm.modules.usergroups.entity.UserGroup> userGroups = new ArrayList<>();
            if (searchCriteria.getName() != null) {
                com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.getGroupByNameForDomain(searchCriteria.getName(), domainId);
                if (group != null) {
                    userGroups.add(group);
                }
            } else {
                for (com.rackspace.idm.modules.usergroups.entity.UserGroup group : userGroupService.getGroupsForDomain(domainId)) {
                    userGroups.add(group);
                }
            }

            return Response.ok(userGroupConverter.toUserGroupsWeb(userGroups)).build();
        } catch (Exception ex) {
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
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
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            if (roleAssignments == null) {
                throw new BadRequestException("Must supply a set of assignments");
            }

            userGroupService.replaceRoleAssignmentsOnGroup(group, roleAssignments);

            // Retrieve the first 1000 assigned roles on the group
            PaginatorContext<TenantRole> tenantRolePage = userGroupService.getRoleAssignmentsOnGroup(group, new UserGroupRoleSearchParams(new PaginationParams(0, 1000)));

            return Response.ok(roleAssignmentConverter.toRoleAssignmentsWeb(tenantRolePage.getValueList())).build();
        } catch (Exception ex) {
            LOG.error(String.format("Error granting roles to user group for domain '%s' and groupid '%s'", domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
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
    public Response listRoleAssignmentsOnGroup(UriInfo uriInfo, String authToken, String domainId, String groupId, UserGroupRoleSearchParams userGroupRoleSearchParams) {
        try {
            Validate.notNull(userGroupRoleSearchParams);
            Validate.notNull(userGroupRoleSearchParams.getPaginationRequest());

            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            PaginatorContext<TenantRole> tenantRolePage = userGroupService.getRoleAssignmentsOnGroup(group, userGroupRoleSearchParams);

            String linkHeader = idmPathUtils.createLinkHeader(uriInfo, tenantRolePage);

            return Response.status(200)
                    .header(HttpHeaders.LINK, linkHeader)
                    .entity(roleAssignmentConverter.toRoleAssignmentsWeb(tenantRolePage.getValueList())).build();
        } catch (Exception ex) {
            LOG.error(String.format("Error listing role assignments for user group for domain '%s' and groupid '%s'", domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response getRoleOnGroup(String authToken, String domainId, String groupId, String roleId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }
}
