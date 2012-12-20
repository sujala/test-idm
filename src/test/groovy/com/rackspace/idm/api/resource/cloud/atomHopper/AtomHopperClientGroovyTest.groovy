package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.docs.core.event.EventType
import com.rackspace.idm.domain.entity.Group
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.DefaultGroupService
import org.apache.commons.configuration.Configuration
import org.w3._2005.atom.UsageEntry
import spock.lang.Shared
import spock.lang.Specification
import org.apache.http.client.HttpClient;

import javax.ws.rs.core.MediaType

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/20/12
 * Time: 9:52 AM
 * To change this template use File | Settings | File Templates.
 */
class AtomHopperClientGroovyTest extends Specification {
    @Shared AtomHopperClient client
    @Shared DefaultGroupService defaultGroupService
    @Shared Configuration config
    @Shared HttpClient httpClient

    def setupSpec(){
        client = new AtomHopperClient();
    }


    def "create atom entry for delete user" () {
        given:
        setupMock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        defaultGroupService.getGroupsForUser(_) >> [createGroup("group",1,"desc")].asList()

        when:
        UsageEntry entry = client.createEntryForUser(user, EventType.DELETE, false)

        then:
        entry.title.value == "Identity Event"
        entry.content.type == MediaType.APPLICATION_XML
        entry.content.event.resourceName == "testUser"
    }

    def "Post deleted user" () {
        given:
        setupMock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        defaultGroupService.getGroupsForUser(_) >> [createGroup("group",1,"desc")].asList()
        config.getString(_) >> "http://10.4.39.67:8082/namespace/feed"

        when:
        client.postUser(user, "someToken", AtomHopperConstants.DELETED)

        then:
        1 * httpClient.execute(_)
    }

    def "Post disabled user" () {
        given:
        setupMock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        defaultGroupService.getGroupsForUser(_) >> [createGroup("group",1,"desc")].asList()
        config.getString(_) >> "http://10.4.39.67:8082/namespace/feed"

        when:
        client.postUser(user, "someToken", AtomHopperConstants.DISABLED)

        then:
        1 * httpClient.execute(_)
    }

    def createTenantRole(String name, String roleRsId, String description) {
        new TenantRole().with {
            it.name = name
            it.roleRsId = roleRsId
            it.description = description
            return it
        }
    }

    def createGroup(String name, Integer groupId, String description) {
        new Group().with {
            it.name  = name
            it.groupId = groupId
            it.description = description
            return it
        }
    }

    def setupMock() {
        defaultGroupService = Mock()
        client.defaultGroupService = defaultGroupService
        config = Mock()
        client.config = config
        httpClient = Mock()
        client.httpClient = httpClient
    }
}
