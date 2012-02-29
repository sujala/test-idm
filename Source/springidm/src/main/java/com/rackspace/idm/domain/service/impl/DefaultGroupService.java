package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.GroupService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */

public class DefaultGroupService implements GroupService {

    @Autowired
    private DefaultUserService defaultUserService;

    private final GroupDao groupDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultGroupService(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    @Override
    public List<Group> getGroups(String marker, Integer limit) {
        return groupDao.getGroups(marker, limit);
    }

    @Override
    public List<Group> getGroupsByMossoId(Integer mossoAccountId) {
        List<Group> groups = null;
        return groups;
    }

    @Override
    public List<Group> getGroupListByMossoId(Integer mossoAccountId) {
        List<Group> groups = null;
        return groups;
    }

    @Override
    public Group getGroupById(Integer groupId) {
        Group group = groupDao.getGroupById(groupId);
        return group;
    }

    @Override
    public Group getGroupByName(String groupName){
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
        logger.info("Adding Client Group: {}", group);
        verifyDuplicateGroup(group);
        group.setGroupId(Integer.parseInt(this.groupDao.getNextGroupId()));
        groupDao.addGroup(group);
    }

    @Override
    public void updateGroup(Group group) {
        logger.info("Updating Client Group: {}", group);
        Group groupDo = groupDao.getGroupById(group.getGroupId());
        if(!groupDo.getName().equals(group.getName())){
            verifyDuplicateGroup(group);
        }
        groupDao.updateGroup(group);
    }

    private void verifyDuplicateGroup(Group group) {
        Group exists = groupDao.getGroupByName(group.getName());
        if (exists != null) {
            String errMsg = String.format("Group with name %s already exists", group.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
    }

    @Override
    public void deleteGroup(String groupId) {
        Group exists = groupDao.getGroupById(Integer.parseInt(groupId));
        if (exists == null) {
            String errMsg = String.format("Group %s not found", groupId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        groupDao.deleteGroup(Integer.parseInt(groupId));
    }

    @Override
    public void addGroupToUser(int groupId, String userId) {
        groupDao.addGroupToUser(groupId, userId);
    }

    @Override
    public void deleteGroupFromUser(int groupId, String userId) {
        groupDao.deleteGroupFromUser(groupId, userId);
    }

    @Override
    public List<Group> getGroupsForUser(String userId) {
        return groupDao.getGroupsForUser(userId);
    }

    @Override
    public Users getAllEnabledUsers(FilterParam[] filters, String offset, int limit){
        logger.debug("Getting All Users");

        Users users = defaultUserService.getAllUsers(filters, Integer.parseInt(offset), limit);
        logger.debug("Got All Users {}", filters);
        List<User> enabledUsers = new ArrayList<User>();
        for(User user : users.getUsers()){
            if(user.isEnabled()){
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
}
