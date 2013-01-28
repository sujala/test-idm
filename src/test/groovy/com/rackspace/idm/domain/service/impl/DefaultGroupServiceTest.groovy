package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.Group
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/28/13
 * Time: 1:21 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultGroupServiceTest extends RootServiceTest{

    @Shared DefaultGroupService defaultGroupService
    @Shared Configuration config

    def setupSpec(){
        defaultGroupService = new DefaultGroupService()
    }

    def "Deleting default group is not allowed" () {
        given:
        setupMocks()
        config.getString("defaultGroupId") >> "0"

        when:
        defaultGroupService.deleteGroup("0")

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
        defaultGroupService.updateGroup(group)

        then:
        thrown(BadRequestException)
    }

    def setupMocks(){
        config = Mock()

        defaultGroupService.config = config
    }
}
