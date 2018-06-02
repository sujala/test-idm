package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.v20.FindDelegationAgreementParams;
import com.rackspace.idm.api.resource.cloud.v20.UserGroupDelegateReference;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupRoleSearchParams;
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupSearchParams;
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria;
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.validation.Validator20;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN;
import static com.rackspace.idm.validation.Validator20.MAX_LENGTH_255;
import static com.rackspace.idm.validation.Validator20.MAX_LENGTH_64;

@Component
public class DefaultUserGroupService implements UserGroupService {
    private static final Logger log = LoggerFactory.getLogger(DefaultUserGroupService.class);

    public static final String GROUP_NOT_FOUND_ERROR_MESSAGE = "Group '%s' not found";
    public static final String USER_MUST_BELONG_TO_DOMAIN = "User must belong to domain";
    public static final String GROUP_MUST_BELONG_TO_DOMAIN = "Group must belong to domain";
    public static final String CAN_ONLY_ADD_USERS_TO_GROUPS_WITHIN_SAME_DOMAIN = "Can only add users to groups within same domain";
    public static final String CAN_ONLY_MODIFY_GROUPS_ON_PROVISIONED_USERS_VIA_API = "Can only modify groups on provisioned users via API.";

    public static final String ERROR_MESSAGE_DUPLICATE_GROUP_IN_DOMAIN = "Group already exists with this name in this domain";
    public static final String USER_GROUP_ROLE_ASSIGNMENT_FOR_ALL_TENANTS = "*";

    @Autowired
    private TenantRoleDao tenantRoleDao;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private UserGroupDao userGroupDao;

    @Autowired
    private Validator20 validator20;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private TenantAssignmentService tenantAssignmentService;

    @Autowired
    private DelegationService delegationService;

    @Override
    public UserGroup addGroup(UserGroup group) {
        Validate.notNull(group);
        Validate.notEmpty(group.getDomainId());

        Assert.isTrue(StringUtils.isNotBlank(group.getDomainId()));

        // Verify group requirements
        validateUserGroupForCreate(group);

        if (getGroupByNameForDomain(group.getName(), group.getDomainId()) != null) {
            throw new DuplicateException(ERROR_MESSAGE_DUPLICATE_GROUP_IN_DOMAIN);
        }

        // Validate there is room to create this group in the domain
        int numGroupsInDomain = userGroupDao.countGroupsInDomain(group.getDomainId());
        if (numGroupsInDomain >= identityConfig.getReloadableConfig().getMaxUsersGroupsPerDomain()) {
            throw new ForbiddenException(Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED_MSG, Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED);
        }

        userGroupDao.addGroup(group);

        return group;
    }

    private void validateUserGroupForCreate(UserGroup userGroup) {
        validateUserGroupName(userGroup);
        validateUserGroupDescription(userGroup);
    }

    private void validateUserGroupName(UserGroup userGroup) {
        validator20.validateStringNotNullWithMaxLength("name", userGroup.getName(), MAX_LENGTH_64);
    }

    private void validateUserGroupDescription(UserGroup userGroup) {
        validator20.validateStringMaxLength("description", userGroup.getDescription(), MAX_LENGTH_255);
    }

    @Override
    public UserGroup updateGroup(UserGroup group) {
        com.rackspace.idm.modules.usergroups.entity.UserGroup userGroupEntity = checkAndGetGroupByIdForDomain(group.getId(), group.getDomainId());

        // Copy over only the attributes that are provided in the request and allowed to be updated
        if (group.getName() != null) {
            validateUserGroupName(group);
            userGroupEntity.setName(group.getName());
        }

        if (group.getDescription() != null) {
            validateUserGroupDescription(group);
            userGroupEntity.setDescription(group.getDescription());
        }

        UserGroup otherGroupWithName = getGroupByNameForDomain(userGroupEntity.getName(), userGroupEntity.getDomainId());
        if (otherGroupWithName != null && !otherGroupWithName.getId().equalsIgnoreCase(group.getId())) {
            throw new DuplicateException(ERROR_MESSAGE_DUPLICATE_GROUP_IN_DOMAIN);
        }

        userGroupDao.updateGroup(userGroupEntity);

        return userGroupEntity;
    }

    @Override
    public void deleteGroup(UserGroup group) {

        delegationService.removeConsumerFromExplicitDelegationAgreementAssignments(group);

        // Remove user group membership from users
        for (EndUser user : getUsersInGroup(group)) {
            // TODO: Remove user group from user should be changed to support removing EndUser(provision and federated)
            // from a user group once adding federated user to a user group is implemented.
            if (user instanceof  User) {
                identityUserService.removeUserGroupFromUser(group, (User) user);
            }
        }

        userGroupDao.deleteGroup(group);
    }

    @Override
    public UserGroup getGroupById(String groupId) {
        Validate.notEmpty(groupId);

        return userGroupDao.getGroupById(groupId);
    }

    @Override
    public UserGroup checkAndGetGroupById(String groupId) {
        Validate.notEmpty(groupId);

        UserGroup group = getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException(String.format(GROUP_NOT_FOUND_ERROR_MESSAGE, groupId));
        }
        return group;
    }

    @Override
    public UserGroup getGroupByIdForDomain(String groupId, String domainId) {
        Validate.notEmpty(domainId);
        Validate.notEmpty(groupId);

        UserGroup group = getGroupById(groupId);
        if (group != null && !domainId.equalsIgnoreCase(group.getDomainId())) {
            return null; // If group exists, but doesn't belong to domain, pretend it doesn't exist
        }
        return group;
    }

    @Override
    public UserGroup checkAndGetGroupByIdForDomain(String groupId, String domainId) {
        Validate.notEmpty(groupId);
        Validate.notEmpty(domainId);

        UserGroup group = getGroupByIdForDomain(groupId, domainId);
        if (group == null) {
            /*
             While technically the group may exist, just not in the specified domain, want the error message to be
             the same in both cases.
             */
            throw new NotFoundException(String.format(GROUP_NOT_FOUND_ERROR_MESSAGE, groupId));
        }
        return group;
    }

    @Override
    public UserGroup getGroupByNameForDomain(String groupName, String domainId) {
        Validate.notEmpty(groupName);
        Validate.notEmpty(domainId);

        return userGroupDao.getGroupByNameForDomain(groupName, domainId);
    }

    @Override
    public List<UserGroup> getGroupsBySearchParamsInDomain(UserGroupSearchParams userGroupSearchParams, String domainId) {
        Validate.notEmpty(domainId);

        String name = userGroupSearchParams.getName();
        String userId = userGroupSearchParams.getUserId();

        List<UserGroup> userGroups = new ArrayList<>();
        UserGroup group;

        if (name != null && userId != null) {
            group = getGroupByNameForUserInDomain(name, userId, domainId);
            if (group != null) {
                userGroups.add(group);
            }
        } else if (userGroupSearchParams.getName() != null) {
            group = getGroupByNameForDomain(name, domainId);
            if (group != null) {
                userGroups.add(group);
            }
        } else if (userId != null) {
            EndUser user = identityUserService.getEndUserById(userId);

            // Return an empty list if the user was not found or does not belong to the same domain specified on the request.
            if (user != null && user.getDomainId().equals(domainId)) {
                for (String userGroupId : user.getUserGroupIds()){
                    group = getGroupById(userGroupId);
                    // Only add existing user groups.
                    if (group != null) {
                        userGroups.add(group);
                    }
                }
            }
        }

        return userGroups;
    }

    @Override
    public void addRoleAssignmentOnGroup(UserGroup userGroup, String roleId, String tenantId) {
        Validate.notNull(userGroup);
        Validate.notNull(roleId);
        Validate.notNull(tenantId);

        // Verify the role exist
        ClientRole clientRole = applicationService.getClientRoleById(roleId);
        if (clientRole == null) {
            String errMsg = String.format("Role '%s' does not exist.", roleId);
            throw new NotFoundException(errMsg);
        }

        // Verify the role's administrator is user-manager
        if (clientRole.getRsWeight() != RoleLevelEnum.LEVEL_1000.getLevelAsInt()) {
            throw new ForbiddenException("Not authorized to create role assignment.");
        }

        // Verify the tenant exist
        tenantService.checkAndGetTenant(tenantId);

        TenantRole tenantRole = getRoleAssignmentOnGroup(userGroup, roleId);
        if (tenantRole != null ) {
            // Role assignment already exist if the tenantRole's tenantIds is empty, set to "*", or contains
            // the provided tenant id.
            if (tenantRole.getTenantIds().isEmpty() ||
                    tenantRole.getTenantIds().contains(USER_GROUP_ROLE_ASSIGNMENT_FOR_ALL_TENANTS) ||
                    tenantRole.getTenantIds().contains(tenantId)) {
                throw new DuplicateException("Role assignment already exist on user group.");
            }
            tenantRole.getTenantIds().add(tenantId);

            tenantRoleDao.updateRoleAssignmentOnGroup(userGroup, tenantRole);
        } else {
            tenantRole = new TenantRole();
            tenantRole.setClientId(clientRole.getClientId());
            tenantRole.setRoleRsId(roleId);
            tenantRole.getTenantIds().add(tenantId);

            tenantRoleDao.addRoleAssignmentOnGroup(userGroup, tenantRole);
        }
    }

    @Override
    public void revokeRoleAssignmentOnGroup(UserGroup userGroup, String roleId, String tenantId) {
        Validate.notNull(userGroup);
        Validate.notNull(roleId);
        Validate.notNull(tenantId);

        // Verify the role exist
        ClientRole clientRole = applicationService.getClientRoleById(roleId);
        if (clientRole == null) {
            String errMsg = String.format("Role '%s' does not exist.", roleId);
            throw new NotFoundException(errMsg);
        }

        // Verify the role's administrator is user-manager
        if (clientRole.getRsWeight() != RoleLevelEnum.LEVEL_1000.getLevelAsInt()) {
            throw new ForbiddenException("Not authorized to create role assignment.");
        }

        // Verify the tenant exist
        tenantService.checkAndGetTenant(tenantId);

        TenantRole tenantRole = getRoleAssignmentOnGroup(userGroup, roleId);
        if (tenantRole == null ||
                tenantRole.getTenantIds().isEmpty() ||
                !tenantRole.getTenantIds().contains(tenantId) && !tenantRole.getTenantIds().contains(USER_GROUP_ROLE_ASSIGNMENT_FOR_ALL_TENANTS)) {
            String errMsg = String.format("Role assignment does not exist.", roleId);
            throw new NotFoundException(errMsg);
        } else if (tenantRole.getTenantIds().contains(USER_GROUP_ROLE_ASSIGNMENT_FOR_ALL_TENANTS)) {
            String errMsg = "Role assignment exist as global role.";
            throw new BadRequestException(errMsg);
        }

        tenantRole.getTenantIds().clear();
        tenantRole.getTenantIds().add(tenantId);
        tenantRoleDao.deleteOrUpdateRoleAssignmentOnGroup(userGroup, tenantRole);
    }

    private UserGroup getGroupByNameForUserInDomain(String groupName, String userId, String domainId) {
        Validate.notEmpty(groupName);
        Validate.notEmpty(userId);
        Validate.notEmpty(domainId);

        UserGroup userGroup = userGroupDao.getGroupByNameForDomain(groupName, domainId);
        if (userGroup == null) {
            return null;
        }

        EndUser user = identityUserService.getEndUserById(userId);
        if (user == null) {
            return null;
        }

        DN groupDn;
        try {
            groupDn = new DN(userGroup.getUniqueId());
        } catch (LDAPException e) {
            String errMsg = "User group DN could not be parsed";
            log.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        return user.getUserGroupDNs().contains(groupDn) ? userGroup : null;
    }

    @Override
    public List<TenantRole> getRoleAssignmentsOnGroup(String userGroupId) {
        Assert.notNull(userGroupId);

        UserGroup group = getGroupById(userGroupId);

        List<TenantRole> assignedRoles;
        if (group != null) {
            Iterable<TenantRole> rolesIt = tenantRoleDao.getRoleAssignmentsOnGroup(group);
            assignedRoles = IteratorUtils.toList(rolesIt.iterator());
        } else {
            throw new NotFoundException(String.format(GROUP_NOT_FOUND_ERROR_MESSAGE, userGroupId));
        }
        return assignedRoles;
    }

    @Override
    public PaginatorContext<TenantRole> getRoleAssignmentsOnGroup(UserGroup userGroup, UserGroupRoleSearchParams userGroupRoleSearchParams) {
        Validate.notNull(userGroup);
        Validate.isTrue(StringUtils.isNotBlank(userGroup.getUniqueId()));
        Validate.notNull(userGroupRoleSearchParams);

        return tenantRoleDao.getRoleAssignmentsOnGroup(userGroup, userGroupRoleSearchParams);
    }

    @Override
    public TenantRole getRoleAssignmentOnGroup(UserGroup userGroup, String roleId) {
        Assert.notNull(userGroup);

        return tenantRoleDao.getRoleAssignmentOnGroup(userGroup, roleId);
    }

    @Override
    public List<TenantRole> replaceRoleAssignmentsOnGroup(UserGroup userGroup, RoleAssignments roleAssignments) {
        Validate.notNull(userGroup);
        Validate.notNull(userGroup.getUniqueId());
        Validate.notNull(roleAssignments);

        if (roleAssignments.getTenantAssignments() == null || CollectionUtils.isEmpty(roleAssignments.getTenantAssignments().getTenantAssignment())) {
            return Collections.emptyList();
        }

        return tenantAssignmentService.replaceTenantAssignmentsOnUserGroup(
                userGroup, roleAssignments.getTenantAssignments().getTenantAssignment() );
    }

    @Override
    public void revokeRoleAssignmentOnGroup(UserGroup userGroup, String roleId) {
        Validate.notNull(userGroup);
        Validate.notNull(userGroup.getUniqueId());
        Validate.notEmpty(roleId);

        TenantRole assignedRole = getRoleAssignmentOnGroup(userGroup, roleId);

        if (assignedRole == null) {
            throw new NotFoundException(String.format(ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN, roleId), ErrorCodes.ERROR_CODE_NOT_FOUND);
        }

        tenantRoleDao.deleteTenantRole(assignedRole);
    }

    @Override
    public Iterable<UserGroup> getGroupsForDomain(String domainId) {
        Validate.notEmpty(domainId);

        return userGroupDao.getGroupsForDomain(domainId);
    }

    @Override
    public void addUserToGroup(String userId, UserGroup group) {
        Assert.notNull(userId);
        Assert.notNull(group);

        User targetUser = verifyAndGetUserForGroup(userId);

        if (StringUtils.isBlank(targetUser.getDomainId())) {
            throw new BadRequestException(USER_MUST_BELONG_TO_DOMAIN);
        } else if (StringUtils.isBlank(group.getDomainId())) {

            throw new BadRequestException(GROUP_MUST_BELONG_TO_DOMAIN);
        } else if (!targetUser.getDomainId().equalsIgnoreCase(group.getDomainId())) {
            throw new BadRequestException(CAN_ONLY_ADD_USERS_TO_GROUPS_WITHIN_SAME_DOMAIN);
        }

        identityUserService.addUserGroupToUser(group, targetUser);
    }

    @Override
    public void removeUserFromGroup(String userId, UserGroup group) {
        Assert.notNull(userId);
        Assert.notNull(group);

        User targetUser = verifyAndGetUserForGroup(userId);

        identityUserService.removeUserGroupFromUser(group, targetUser);
    }

    @Override
    public PaginatorContext<EndUser> getUsersInGroupPaged(UserGroup group, UserSearchCriteria userSearchCriteria) {
        Assert.notNull(group);
        Assert.notNull(userSearchCriteria);
        Assert.notNull(userSearchCriteria.getPaginationRequest());

        return identityUserService.getEndUsersInUserGroupPaged(group, userSearchCriteria);
    }

    @Override
    public Iterable<EndUser> getUsersInGroup(UserGroup group) {
        Assert.notNull(group);

        return identityUserService.getEndUsersInUserGroup(group);
    }


    private User verifyAndGetUserForGroup(String userId) {
        com.rackspace.idm.domain.entity.EndUser user = identityUserService.checkAndGetUserById(userId);

        User targetUser = null;
        if (user instanceof User) {
            targetUser = (User) user;
        } else {
            throw new ForbiddenException(CAN_ONLY_MODIFY_GROUPS_ON_PROVISIONED_USERS_VIA_API);
        }

        return targetUser;
    }

}
