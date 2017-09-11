package com.rackspace.idm.modules.usergroups.dao;

import com.rackspace.idm.modules.usergroups.entity.UserGroup;

public interface UserGroupDao {

    /**
     * Sets a unique id on the group and persists to backend.
     *
     * @param group
     *
     * @throws IllegalArgumentException If the provided group is null
     * @throws IllegalStateException If an error occurred persisting the group
     */
    void addGroup(UserGroup group);

    /**
     * Retrieve a group by id. Returns null if no group exists with the specified id.
     *
     * @param groupId
     *
     * @throws IllegalStateException If search resulted in error or more than one entry was found with the given id.
     * @return
     */
    UserGroup getGroupById(String groupId);

    /**
     * Update the specified group.
     *
     * @throws IllegalArgumentException If the provided object does not already exist in the directory
     *
     * @param group
     */
    void updateGroup(UserGroup group);

    /**
     * Deletes the specified group from persistent storage.
     *
     * @param group
     */
    void deleteGroup(UserGroup group);

    /**
     * Return a iterable of User Groups for the specified domain.
     *
     * @param domainId
     * @return
     */
    Iterable<UserGroup> getGroupsForDomain(String domainId);

    /**
     * Return a count of the number of user groups in a domain
     *
     * @param domainId
     * @return
     */
    int countGroupsInDomain(String domainId);

    /**
     * Return a group with the given domainId and name, or null if no such group exists.
     *
     * @param groupName
     * @param domainId
     * @return
     */
    UserGroup getGroupByNameForDomain(String groupName, String domainId);
}
