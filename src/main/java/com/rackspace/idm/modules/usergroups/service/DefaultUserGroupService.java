package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.service.impl.DefaultFederatedIdentityService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static com.rackspace.idm.validation.Validator20.MAX_LENGTH_255;
import static com.rackspace.idm.validation.Validator20.MAX_LENGTH_64;

@Component
public class DefaultUserGroupService implements UserGroupService {
    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);

    @Autowired
    private UserGroupDao userGroupDao;

    @Autowired
    private Validator20 validator20;

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public UserGroup addGroup(UserGroup group) {
        Validate.notNull(group);
        Validate.notEmpty(group.getDomainId());

        Assert.isTrue(StringUtils.isNotBlank(group.getDomainId()));

        // Verify group requirements
        validateUserGroupForCreateAndUpdate(group);

        if (getGroupByNameForDomain(group.getName(), group.getDomainId()) != null) {
            throw new DuplicateException("Group already exists with this name in this domain");
        }

        // Validate there is room to create this group in the domain
        int numGroupsInDomain = userGroupDao.countGroupsInDomain(group.getDomainId());
        if (numGroupsInDomain >= identityConfig.getReloadableConfig().getMaxUsersGroupsPerDomain()) {
            throw new ForbiddenException(Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED_MSG, Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED);
        }

        userGroupDao.addGroup(group);

        return group;
    }

    public void validateUserGroupForCreateAndUpdate(UserGroup userGroup) {
        validator20.validateStringNotNullWithMaxLength("name", userGroup.getName(), MAX_LENGTH_64);
        validator20.validateStringMaxLength("description", userGroup.getDescription(), MAX_LENGTH_255);
    }

    @Override
    public UserGroup updateGroup(UserGroup group) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public void deleteGroup(UserGroup group) {
        throw new NotImplementedException("This method has not yet been implemented");
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
            throw new NotFoundException(String.format("Group '%s' not found", groupId));
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
    public UserGroup checkAndGetGroupByIdForDomain(String domainId, String groupId) {
        Validate.notEmpty(domainId);
        Validate.notEmpty(groupId);

        UserGroup group = getGroupByIdForDomain(domainId, groupId);
        if (group == null) {
            /*
             While technically the group may exist, just not in the specified domain, want the error message to be
             the same in both cases.
             */
            throw new NotFoundException(String.format("Group '%s' not found", groupId));
        }
        return group;
    }

    @Override
    public UserGroup getGroupByNameForDomain(String groupName, String domainId) {
        Validate.notEmpty(groupName);
        Validate.notEmpty(domainId);

        return userGroupDao.getGroupByNameForDomain(groupName, domainId);
    }
}
