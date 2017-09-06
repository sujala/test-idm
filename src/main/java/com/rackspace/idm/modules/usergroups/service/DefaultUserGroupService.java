package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.idm.domain.service.impl.DefaultFederatedIdentityService;
import com.rackspace.idm.exception.DuplicateException;
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
    UserGroupDao userGroupDao;

    @Autowired
    Validator20 validator20;

    @Override
    public UserGroup addGroup(UserGroup group) {
        Validate.notNull(group);
        Validate.notEmpty(group.getDomainId());

        Assert.isTrue(StringUtils.isNotBlank(group.getDomainId()));

        //verify group requirements
        validateUserGroupForCreateAndUpdate(group);

        if (getGroupByDomainIdAndName(group.getDomainId(), group.getName()) != null) {
            throw new DuplicateException("Group already exists with this name in this domain");
        }

        userGroupDao.addGroup(group);

        return group;
    }

    public void validateUserGroupForCreateAndUpdate(UserGroup userGroup) {
        validator20.validateStringNotNullWithMaxLength("name", userGroup.getName(), MAX_LENGTH_64);
        validator20.validateStringNotNullWithMaxLength("description", userGroup.getDescription(), MAX_LENGTH_255);
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
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public UserGroup checkAndGetGroupById(String groupId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public UserGroup getGroupByIdForDomain(String domainId, String groupId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public UserGroup checkAndGetGroupByIdForDomain(String domainId, String groupId) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public UserGroup getGroupByDomainIdAndName(String domainId, String groupName) {
        Validate.notEmpty(domainId);
        Validate.notEmpty(groupName);

        return userGroupDao.getGroupByDomainIdAndName(domainId, groupName);
    }
}
