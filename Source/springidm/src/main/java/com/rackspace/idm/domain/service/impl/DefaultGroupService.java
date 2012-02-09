package com.rackspace.idm.domain.service.impl;

import com.rackspace.cloud.servers.bean.LimitGroupType;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.service.GroupService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import org.apache.commons.lang.StringUtils;
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

public class DefaultGroupService implements GroupService {

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
    public void addGroup(Group group) {
        logger.info("Adding Client Group: {}", group);
        Group exists = groupDao.getGroupByName(group.getName());
        if (exists != null) {
            String errMsg = String.format("Group with name %s already exists", group.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
        group.setGroupId(Integer.parseInt(this.groupDao.getNextGroupId()));
        groupDao.addGroup(group);
    }

    @Override
    public void updateGroup(Group group) {
        groupDao.updateGroup(group);
    }

    @Override
    public void deleteGroup(String groupId) {
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

    private List<Group> convertGroup(LimitGroupType limitGroupType) {

        List<Group> groups = new ArrayList<Group>();
        Group group = new Group();

        group.setGroupId(Integer.parseInt(limitGroupType.getName()));

        final String groupDescription = limitGroupType.getDescription();
        if (StringUtils.isNotEmpty(groupDescription)) {
            group.setDescription(limitGroupType.getDescription());
        }

        groups.add(group);
        return groups;
    }

    private GroupsList convertGroupToGroupList(LimitGroupType limitGroupType) {

        GroupsList groups = new GroupsList();
        com.rackspacecloud.docs.auth.api.v1.Group group = new com.rackspacecloud.docs.auth.api.v1.Group();

        group.setId(limitGroupType.getName());

        final String groupDescription = limitGroupType.getDescription();
        if (StringUtils.isNotEmpty(groupDescription)) {
            group.setDescription(limitGroupType.getDescription());
        }

        groups.getGroup().add(group);
        return groups;
    }

}
