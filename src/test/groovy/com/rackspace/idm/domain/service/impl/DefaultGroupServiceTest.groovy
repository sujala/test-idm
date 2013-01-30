package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.Group
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/28/13
 * Time: 1:21 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultGroupServiceTest extends RootServiceTest {

    @Shared DefaultGroupService service

    def setupSpec(){
        service = new DefaultGroupService()
    }

    def "Deleting default group is not allowed" () {
        given:
        setupMocks()
        config.getString("defaultGroupId") >> "0"

        when:
        service.deleteGroup("0")

        then:
        thrown(BadRequestException)
    }

    def "Updating default group is not allowed" () {
        given:
        setupMocks()
        config.getString("defaultGroupId") >> "0"
        Group group = new Group().with {
            it.groupId = 0
            it.name = "someNewName"
            it.description = "newDesc"
            return it
        }

        when:
        service.updateGroup(group)

        then:
        thrown(BadRequestException)
    }

    def setupMocks(){
        config = Mock()

        service.config = config
    }
}
