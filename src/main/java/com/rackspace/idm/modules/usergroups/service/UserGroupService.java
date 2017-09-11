package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.idm.modules.usergroups.entity.UserGroup;

/**
 * Business service to manage user groups
 */
public interface UserGroupService {

    /**
     * Adds the specified user group
     *
     * @param group
     *
     * @throws com.rackspace.idm.exception.NotFoundException If the specified domain for the group doesn't exist
     * @throws com.rackspace.idm.exception.BadRequestException If the domain specified for the group does not exist
     * or request does not meet validation requirements
     * @return
     */
    UserGroup addGroup(UserGroup group);

    /**
     * Update the existing user group with the provided information. Only non-null fields other than the id and domainId
     * will be updated.
     *
     * @param group
     * @throws com.rackspace.idm.exception.DuplicateException If updating the group name and a different group already
     * exists with this name
     * @throws com.rackspace.idm.exception.NotFoundException If the specified group doesn't exist
     * @throws com.rackspace.idm.exception.BadRequestException If the group changes do not meet validation requirements
     * @return
     */
    UserGroup updateGroup(UserGroup group);

    /**
     * Delete the user group
     *
     * @param group
     */
    void deleteGroup(UserGroup group);

    /**
     * Retrieve the specified group by id regardless of what domain it exists within. Return null if the group does
     * not exist.
     *
     * @param groupId
     * @return
     */
    UserGroup getGroupById(String groupId);

    /**
     * Returns the user group with the specified id. If the group doesn't exist, throws a NotFoundException.
     *
     * @param groupId
     * @throws com.rackspace.idm.exception.NotFoundException If the specified group doesn't exist
     * @throws IllegalArgumentException If supplied groupId is null or empty string
     *
     * @return
     */
    UserGroup checkAndGetGroupById(String groupId);

    /**
     * Retrieves the group with the specified groupId under the specified domain. If a group with the specified groupId
     * exists, but lives in a different domain than the one specified, will return null.
     *
     * @param domainId
     * @param groupId
     * @throws IllegalArgumentException If supplied domainId or groupId is null or empty string
     *
     * @return
     */
    UserGroup getGroupByIdForDomain(String groupId, String domainId);

    /**
     * Retrieves the group with the specified groupId under the specified domain. If no such group exists, throws
     * NotFoundException.
     *
     * @param domainId
     * @param groupId
     *
     * @throws com.rackspace.idm.exception.NotFoundException If the specified group doesn't exist in the given domain
     * @throws IllegalArgumentException If supplied domainId or groupId is null or empty string
     * @return
     */
    UserGroup checkAndGetGroupByIdForDomain(String groupId, String domainId);

    /**
     * Retrieves the group with the specified group name under the specified domain. If no such group exists, returns null
     *
     * @param domainId
     * @param groupName
     * @throws IllegalArgumentException If supplied domainId or groupName is null or empty string
     *
     * @return
     */
    UserGroup getGroupByNameForDomain(String groupName, String domainId);
}
