package com.rackspace.idm.domain.service.impl;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.GroupService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */
@Component
public class DefaultGroupService implements GroupService {

    public static final String GROUP_CANNOT_BE_NULL = "Group cannot be null";

    @Autowired
    private UserService defaultUserService;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private Configuration config;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public List<Group> getGroups(String marker, Integer limit) {
        return groupDao.getGroups(marker, limit);
    }

    @Override
    public List<Group> getGroupsByMossoId(Integer mossoAccountId) {
        return null;
    }

    @Override
    public List<Group> getGroupListByMossoId(Integer mossoAccountId) {
        return null;
    }

    @Override
    public Group getGroupById(String groupId) {
        return groupDao.getGroupById(groupId);
    }

    @Override
    public Group getGroupByName(String groupName) {
        Group group = groupDao.getGroupByName(groupName);
        if (group == null) {
            String errMsg = String.format("Group %s not found", groupName);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return group;
    }

    @Override
    public void addGroup(Group group) {

        if(group == null){
            throw new IllegalArgumentException(GROUP_CANNOT_BE_NULL);
        }

        logger.info("Adding Client Group: {}", group);
        verifyDuplicateGroup(group);
        group.setGroupId(this.groupDao.getNextGroupId());
        groupDao.addGroup(group);
    }

    @Override
    public void insertGroup(Group group) {
        logger.info("Inserting Client Group: {}", group);
        groupDao.addGroup(group);
    }

    @Override
    public void updateGroup(Group group) {
        logger.info("Updating Client Group: {}", group);
        if(group == null){
            throw new IllegalArgumentException(GROUP_CANNOT_BE_NULL);
        }
        if(group.getGroupId() == null){
            throw new IllegalArgumentException("GroupId cannot be null");
        }
        if(String.valueOf(group.getGroupId()).equals(config.getString("defaultGroupId"))){
            throw new BadRequestException("Default Group can not be updated.");
        }
        String groupId = group.getGroupId();
        Group groupDo = groupDao.getGroupById(groupId);

        if(groupDo.getName() == null){
            throw new BadRequestException();
        }

        if (!groupDo.getName().equals(group.getName())) {
            verifyDuplicateGroup(group);
        }
        groupDao.updateGroup(group);
    }

    void verifyDuplicateGroup(Group group) {
        if(group == null){
            throw new IllegalArgumentException(GROUP_CANNOT_BE_NULL);
        }
        Group exists = groupDao.getGroupByName(group.getName());
        if (exists != null) {
            String errMsg = String.format("Group with name %s already exists", group.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
    }

    @Override
    public void deleteGroup(String groupId) {
        if(groupId == null){
            throw new IllegalArgumentException(GROUP_CANNOT_BE_NULL);
        }
        if(groupId.equals(config.getString("defaultGroupId"))){
            throw new BadRequestException("Default Group can not be deleted");
        }
        String grpId = groupId;
        Group exists = groupDao.getGroupById(grpId);
        if (exists == null) {
            String errMsg = String.format("Group %s not found", groupId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.GROUP_ID, groupId)};
        //index and offset and will need to change when pagination is done.
        Users users = defaultUserService.getAllUsers(filters);
        if (users.getUsers().size() != 0) {
            for (User user : users.getUsers()) {
                if (user.isEnabled()) {
                    throw new BadRequestException("Cannot delete a group with users in it.");
                }
            }

            for (User user : users.getUsers()) {
                deleteGroupFromUser(grpId,user.getId());
            }
        }
        groupDao.deleteGroup(groupId);
    }

    @Override
    public void addGroupToUser(String groupId, String userId) {
        groupDao.addGroupToUser(groupId, userId);
    }

    @Override
    public void deleteGroupFromUser(String groupId, String userId) {
        groupDao.deleteGroupFromUser(groupId, userId);
    }

    @Override
    public List<Group> getGroupsForUser(String userId) {
        return groupDao.getGroupsForUser(userId);
    }

    @Override
    public Users getAllEnabledUsers(FilterParam[] filters, String offset, int limit) {
        logger.debug("Getting Enabled All Users");

        Users users = defaultUserService.getAllUsers(filters, Integer.parseInt(offset), limit);
        logger.debug("Got All Users {}", filters);
        List<User> enabledUsers = new ArrayList<User>();
        for (User user : users.getUsers()) {
            if (user.isEnabled()) {
                enabledUsers.add(user);
            }
        }

        Users enabled = new Users();
        enabled.setUsers(enabledUsers);
        return enabled;
    }

    public void setDefaultUserService(DefaultUserService defaultUserService) {
        this.defaultUserService = defaultUserService;
    }

	@Override
	public Group checkAndGetGroupById(String groupId) {
		Group group = getGroupById(groupId);
        
        if (group == null) {
            String errorMsg = String.format("Group %s not found", groupId);
            throw new NotFoundException(errorMsg);
        } 
        
        return group;
	}

	@Override
	public boolean isUserInGroup(String userId, String groupId) {
		List<Group> groups = getGroupsForUser(userId);
		
        for (Group currentGroup : groups) {
            if (currentGroup.getGroupId().equals(groupId)) {
                return true;
            }
        }
        return false;
	}

    public void setConfig(Configuration config) {
        this.config = config;
    }
}
