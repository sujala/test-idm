package com.rackspace.idm.modules.usergroups.service

import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.Constants
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class GetUserGroupServiceTest extends RootServiceTest{

    DefaultUserGroupService service
    UserGroupDao dao

    def setup() {
        service = new DefaultUserGroupService()

        mockValidator20(service)
        mockIdentityConfig(service)
        dao = Mock()
        service.userGroupDao = dao
    }

    @Unroll
    def "getGroupById: Calls dao and returns response"() {
        def groupId = "groupId"
        when:
        UserGroup result = service.getGroupById(groupId)

        then:
        1 * dao.getGroupById(groupId) >> expected
        result == expected // Seems stupid, but verifies method actually returns the result of the dao search

        where:
        expected << [null, new UserGroup()]
    }

    def "checkAndGetGroupById: Calls dao and returns response when not null"() {
        def groupId = "groupId"
        when:
        UserGroup result = service.checkAndGetGroupById(groupId)

        then:
        1 * dao.getGroupById(groupId) >> new UserGroup()
    }

    def "checkAndGetGroupById: Throws NotFoundException when group not found"() {
        def groupId = "groupId"
        when:
        UserGroup result = service.checkAndGetGroupById(groupId)

        then:
        1 * dao.getGroupById(groupId) >> null
        thrown(NotFoundException)
    }

    def "getGroupById: Returns group if in domain"() {
        def groupId = "groupId"
        def domainId = "domainId"

        def matchedGroup = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it
        }

        def unMatchedGroup = new UserGroup().with {
            it.id = groupId
            it.domainId = "otherDomain"
            it
        }

        when:
        UserGroup result = service.getGroupByIdForDomain(groupId, domainId)

        then:
        1 * dao.getGroupById(groupId) >> matchedGroup
        result == matchedGroup

        when:
        result = service.getGroupByIdForDomain(groupId, domainId)

        then:
        1 * dao.getGroupById(groupId) >> unMatchedGroup
        result == null

        when:
        result = service.getGroupByIdForDomain(groupId, domainId)

        then:
        1 * dao.getGroupById(groupId) >> null
        result == null
    }

    def "checkAndGetGroupByIdForDomain: Returns group if in domain, NotFoundException if no group exists or different domain"() {
        def groupId = "groupId"
        def domainId = "domainId"

        def matchedGroup = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it
        }

        def unMatchedGroup = new UserGroup().with {
            it.id = groupId
            it.domainId = "otherDomain"
            it
        }

        when:
        UserGroup result = service.checkAndGetGroupByIdForDomain(groupId, domainId)

        then:
        1 * dao.getGroupById(groupId) >> matchedGroup
        result == matchedGroup

        when:
        service.checkAndGetGroupByIdForDomain(groupId, domainId)

        then:
        1 * dao.getGroupById(groupId) >> unMatchedGroup
        thrown(NotFoundException)

        when:
        service.checkAndGetGroupByIdForDomain(groupId, domainId)

        then:
        1 * dao.getGroupById(groupId) >> null
        thrown(NotFoundException)
    }

    def "getGroupByNameForDomain: Returns group as found"() {
        def groupName = "groupName"
        def domainId = "domainId"

        def matchedGroup = new UserGroup().with {
            it.id = groupName
            it.domainId = domainId
            it
        }

        when:
        UserGroup result = service.getGroupByNameForDomain(groupName, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> matchedGroup
        result == matchedGroup

        when:
        result = service.getGroupByNameForDomain(groupName, domainId)

        then:
        1 * dao.getGroupByNameForDomain(groupName, domainId) >> null
        result == null
    }

    @Unroll
    def "getGroupById: Throws IllegalArgumentException if groupId is empty: val: #groupId"() {
        when:
        service.getGroupById(groupId)

        then:
        thrown(IllegalArgumentException)

        where:
        groupId << [null, ""]
    }

    @Unroll
    def "getGroupByNameForDomain: Throws IllegalArgumentException if groupId or domainId is empty: groupId: #groupId; domainName: #domainName"() {
        when:
        service.getGroupByNameForDomain(groupId, domainName)

        then:
        thrown(IllegalArgumentException)

        where:
        groupId | domainName
        null    | null
        null    | ""
        null    | "value"
        ""      | null
        "value" | null
        ""      | ""
    }

    @Unroll
    def "getGroupByIdForDomain: Throws IllegalArgumentException if groupId or domainId is empty: groupId: #groupId; domainId: #domainId"() {
        when:
        service.getGroupByIdForDomain(groupId, domainId)

        then:
        thrown(IllegalArgumentException)

        where:
        groupId | domainId
        null    | null
        null    | ""
        null    | "value"
        ""      | null
        "value" | null
        ""      | ""
    }
}
