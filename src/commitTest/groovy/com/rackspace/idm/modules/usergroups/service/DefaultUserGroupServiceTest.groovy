package com.rackspace.idm.modules.usergroups.service

import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.modules.usergroups.Constants
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class DefaultUserGroupServiceTest extends RootServiceTest{

    DefaultUserGroupService service
    UserGroupDao dao

    def setup() {
        service = new DefaultUserGroupService()

        mockValidator20(service)
        mockIdentityConfig(service)
        dao = Mock()
        service.userGroupDao = dao
    }

    def "addGroup: validates group name and length through validator standard calls"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", group.name, 64)
        1 * validator20.validateStringMaxLength("description", group.description, 255) >> { throw new BadRequestException() }
        0 * dao.addGroup(_) // Not persisted
        thrown(BadRequestException)
    }

    @Unroll
    def "addGroup: Forbidden when existing number of groups equals or exceeds max group allowed per domain threshold: maxInDomain: #maxInDomain; numExistingGroups:#numExistingGroups"() {
        def domainId = "donmainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * reloadableConfig.getMaxUsersGroupsPerDomain() >> maxInDomain
        1 * dao.countGroupsInDomain(domainId) >> numExistingGroups
        0 * dao.addGroup(_) // Not persisted
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED, Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED_MSG )

        where:
        maxInDomain | numExistingGroups
        4 | 5
        5 | 5
    }

    def "addGroup: Allowed when existing groups less than max group allowed per domain threshold"() {
        def domainId = "donmainId"
        UserGroup group = new UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * reloadableConfig.getMaxUsersGroupsPerDomain() >> 6
        1 * dao.countGroupsInDomain(domainId) >> 5
        1 * dao.addGroup(_) // Not persisted
        notThrown(Exception)
    }

    def "addGroup: Throws DuplicateException if group with name already exists in domainId without persisting"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.addGroup(group)

        then:
        1 * dao.getGroupByNameForDomain(group.name, group.domainId) >> new UserGroup()
        0 * dao.addGroup(_) // Not persisted
        thrown(DuplicateException)
    }

    def "addGroup: Persists object via dao when validation succeeds"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }
        reloadableConfig.getMaxUsersGroupsPerDomain() >> 5 // Set > 0

        when:
        service.addGroup(group)

        then:
        1 * dao.addGroup(group)
    }

    def "deleteGroup: removes object via dao"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        service.deleteGroup(group)

        then:
        1 * dao.deleteGroup(group)
    }

    def "listGroups: gets objects via dao"() {
        given:
        def domainId = "domainId"

        when:
        service.getGroupsForDomain(domainId)

        then:
        1 * dao.getGroupsForDomain(domainId)
    }

}
