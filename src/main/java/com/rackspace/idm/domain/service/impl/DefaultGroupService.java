package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.GroupService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    @Autowired
    private UserService userService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Iterable<Group> getGroups(String marker, Integer limit) {
        return groupDao.getGroups();
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
        if(group.getGroupId().equals(config.getString("defaultGroupId"))){
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
        for (User user : defaultUserService.getUsersByGroupId(groupId)) {
            if (user.getEnabled()) {
                throw new BadRequestException("Cannot delete a group with users in it.");
            }
        }

        for (User user : defaultUserService.getUsersByGroupId(groupId)) {
            userService.deleteGroupFromUser(grpId, user.getId());
        }
        groupDao.deleteGroup(groupId);
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

    public void setConfig(Configuration config) {
        this.config = config;
    }
}
