package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Group;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 1/31/12
 * Time: 12:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface GroupDao {

    String getNextGroupId();
    Iterable<Group> getGroups();
    Group getGroupById(String groupId);
    Group getGroupByName(String groupName);
    void deleteGroup(String groupId);
    void addGroup(Group group);
    void updateGroup(Group group);
}
