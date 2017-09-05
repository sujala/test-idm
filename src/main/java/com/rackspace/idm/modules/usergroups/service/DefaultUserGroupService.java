package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserGroupService implements UserGroupService {
    @Override
    public UserGroup addGroup(UserGroup group) {
        throw new NotImplementedException("This method has not yet been implemented");
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
}
