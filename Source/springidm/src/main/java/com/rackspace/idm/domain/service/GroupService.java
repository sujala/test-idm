package com.rackspace.idm.domain.service;


import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.Users;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */
public interface GroupService {
    List<Group> getGroups(String marker, Integer limit);
    List<Group> getGroupsByMossoId(Integer mossoAccountId);
    List<Group> getGroupListByMossoId(Integer mossoAccountId);
    Group getGroupById(Integer groupId);
    Group getGroupByName(String groupName);
    void addGroup(Group group);
    void updateGroup(Group group);
    void deleteGroup(String groupId);
    void addGroupToUser(int groupId, String userId);
    void deleteGroupFromUser(int groupId, String userId);
    List<Group> getGroupsForUser(String userId);
    Users getAllEnabledUsers(FilterParam[] filters, String offset, int limit);
}
