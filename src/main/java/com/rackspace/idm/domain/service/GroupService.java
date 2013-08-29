package com.rackspace.idm.domain.service;


import com.rackspace.idm.domain.entity.Group;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */
public interface GroupService {
    Iterable<Group> getGroups(String marker, Integer limit);
    List<Group> getGroupsByMossoId(Integer mossoAccountId);
    List<Group> getGroupListByMossoId(Integer mossoAccountId);
    Group getGroupById(String groupId);
    Group checkAndGetGroupById(String groupId);
    Group getGroupByName(String groupName);
    void addGroup(Group group);
    void insertGroup(Group group);
    void updateGroup(Group group);
    void deleteGroup(String groupId);
}
