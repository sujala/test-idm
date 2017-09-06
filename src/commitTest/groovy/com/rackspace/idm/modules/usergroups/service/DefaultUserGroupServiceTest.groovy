package com.rackspace.idm.modules.usergroups.service

import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import testHelpers.RootServiceTest

class DefaultUserGroupServiceTest extends RootServiceTest{

    DefaultUserGroupService service
    UserGroupDao dao

    def setup() {
        service = new DefaultUserGroupService()

        mockValidator20(service)
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
        1 * validator20.validateStringNotNullWithMaxLength("description", group.description, 255) >> { throw new BadRequestException() }
        0 * dao.addGroup(_) // Not persisted
        thrown(BadRequestException)
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
        1 * dao.getGroupByDomainIdAndName(group.domainId, group.name) >> new UserGroup()
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

        when:
        service.addGroup(group)

        then:
        1 * dao.addGroup(group)
    }

}
