package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignment;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmExceptionHandler;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter;
import com.rackspace.idm.modules.usergroups.api.resource.converter.UserGroupConverter;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private TenantService tenantService;

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
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(group.getDomainId());

            domainService.checkAndGetDomain(group.getDomainId());

            // Convert to entity object
            com.rackspace.idm.modules.usergroups.entity.UserGroup userGroupEntity = userGroupConverter.fromUserGroupWeb(group);

            com.rackspace.idm.modules.usergroups.entity.UserGroup updatedGroup = userGroupService.updateGroup(userGroupEntity);

            return Response.ok(userGroupConverter.toUserGroupWeb(updatedGroup)).build();
        } catch (Exception ex) {
            LOG.error(String.format("Error updating user group for domain '%s' and groupId '%s'", group.getDomainId(), group.getId()), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
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

            /*
            A non user-admin/user-manage/rcn-admin end user (federated or provisioned) can only make this request if the
            userId query param is set to their own userId.
             */
            if (!requestContextHolder.getRequestContext().getEffectiveCaller().getId().equals(searchCriteria.getUserId())) {
                // Verify caller can manage specified domain's user groups
                userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);
            }

            List<com.rackspace.idm.modules.usergroups.entity.UserGroup> userGroups = new ArrayList<>();
            if (searchCriteria.hasSearchParams()) {
                userGroups.addAll(userGroupService.getGroupsBySearchParamsInDomain(searchCriteria, domainId));
            } else {
                for (com.rackspace.idm.modules.usergroups.entity.UserGroup group : userGroupService.getGroupsForDomain(domainId)) {
                    userGroups.add(group);
                }
            }

            return Response.ok(userGroupConverter.toUserGroupsWeb(userGroups)).build();
        } catch (Exception ex) {
            LOG.info(String.format("Error retrieving user groups for domain '%s'", domainId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response getUsersInGroup(UriInfo uriInfo, String authToken, String domainId, String groupId, UserSearchCriteria userSearchCriteria) {
        try {
            Assert.notNull(userSearchCriteria);
            Assert.notNull(userSearchCriteria.getPaginationRequest());

            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            // Verify userGroup exists for domain
            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            PaginatorContext<EndUser> paginatorContext = userGroupService.getUsersInGroupPaged(group, userSearchCriteria);

            String linkHeader = idmPathUtils.createLinkHeader(uriInfo, paginatorContext);

            return Response.status(HttpStatus.OK.value())
                    .header(HttpHeaders.LINK, linkHeader)
                    .entity(objFactories.getOpenStackIdentityV2Factory().createUsers(
                            this.userConverterCloudV20.toUserList(paginatorContext.getValueList())).getValue()).build();
        } catch (Exception ex) {
            LOG.info(String.format("Error retrieving users in group '%s' for domain '%s'", groupId, domainId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response addUserToGroup(String authToken, String domainId, String groupId, String userId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify userGroup exists for domain
            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(group.getDomainId());

            userGroupService.addUserToGroup(userId, group);

            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.error(String.format("Error adding user '%s' to group '%s' on domain '%s'", userId, groupId, domainId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }

    }

    @Override
    public Response removeUserFromGroup(String authToken, String domainId, String groupId, String userId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify userGroup exists for domain
            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(group.getDomainId());

            userGroupService.removeUserFromGroup(userId, group);

            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.error(String.format("Error removing user '%s' from group '%s' on domain '%s'", userId, groupId, domainId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response grantRoleToGroup(String authToken, String domainId, String groupId, RoleAssignment roleAssignment) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public Response grantRoleOnTenantToGroup(String authToken, String domainId, String groupId, String roleId, String tenantId) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            com.rackspace.idm.modules.usergroups.entity.UserGroup userGroup = userGroupService.checkAndGetGroupById(groupId);

            userGroupService.addRoleAssignmentOnGroup(userGroup, roleId, tenantId);

            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error granting role '%s' on tenant '%s' to user group for domain '%s' and groupId '%s'", roleId, tenantId, domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
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
            LOG.debug(String.format("Error granting roles to user group for domain '%s' and groupid '%s'", domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response revokeRoleFromGroup(String authToken, String domainId, String groupId, String roleId) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            userGroupService.revokeRoleAssignmentOnGroup(group, roleId);

            Response.ResponseBuilder response = Response.noContent();
            return response.build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error revoking role '%s' from user group for domain '%s' and groupId '%s'", roleId, domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response revokeRoleOnTenantToGroup(String authToken, String domainId, String groupId, String roleId, String tenantId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);

            // Verify userGroup exists for domain
            com.rackspace.idm.modules.usergroups.entity.UserGroup userGroup = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            userGroupService.revokeRoleAssignmentOnGroup(userGroup, roleId, tenantId);

            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error revoking role '%s' on tenant '%s' to user group for domain '%s' and groupId '%s'", roleId, tenantId, domainId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response listRoleAssignmentsOnGroup(UriInfo uriInfo, String authToken, String domainId, String groupId, UserGroupRoleSearchParams userGroupRoleSearchParams) {
        try {
            Validate.notNull(uriInfo);
            Validate.isTrue(StringUtils.isNotBlank(domainId), "Domain id must be supplied");
            Validate.isTrue(StringUtils.isNotBlank(groupId), "Group id must be supplied");
            Validate.notNull(userGroupRoleSearchParams);

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
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller can manage specified domain's user groups
            userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId);
            com.rackspace.idm.modules.usergroups.entity.UserGroup group = userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId);

            return Response.ok(roleAssignmentConverter.toRoleAssignmentWeb(getTenantRoleOnGroup(group, roleId))).build();
        } catch (Exception ex) {
            LOG.info(String.format("Error retrieving role '%s' for user group with groupId '%s'", roleId, groupId), ex);
            return idmExceptionHandler.exceptionResponse(ex).build();
        }
    }

    private TenantRole getTenantRoleOnGroup(com.rackspace.idm.modules.usergroups.entity.UserGroup group, String roleId) {

        TenantRole tenantRole = userGroupService.getRoleAssignmentOnGroup(group, roleId);

        if(tenantRole == null) {
            throw new NotFoundException(String.format("Role with ID %s not found on the user group with ID %s.", roleId, group.getId()));
        }
        return tenantRole;
    }
}
