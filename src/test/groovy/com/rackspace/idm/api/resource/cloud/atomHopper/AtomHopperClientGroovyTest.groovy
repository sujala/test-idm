package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.docs.core.event.EventType
import com.rackspace.idm.domain.entity.Group
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.DefaultGroupService
import com.rackspace.idm.domain.service.impl.DefaultTenantService
import com.rackspace.idm.util.CryptHelper
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.util.EntityUtils
import org.mockito.Spy
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.w3._2005.atom.UsageEntry
import spock.lang.Shared
import spock.lang.Specification
import org.apache.http.client.HttpClient
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
    @Shared DefaultTenantService defaultTenantService
    @Shared Configuration config
    @Shared HttpClient httpClient
    @Shared org.openstack.docs.identity.api.v2.ObjectFactory objectFactory;
    @Shared CryptHelper cryptHelper
    @Shared AtomHopperHelper atomHopperHelper

    def setupSpec(){
        client = new AtomHopperClient();
        objectFactory = new ObjectFactory();
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
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

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
        config.getString(_) >> "DFW" >> "DFW1" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()

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
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.postUser(user, "someToken", AtomHopperConstants.DISABLED)

        then:
        1 * httpClient.execute(_)
    }

    def "Post migrated user" () {
        given:
        setupMock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        defaultGroupService.getGroupsForUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.postUser(user, "someToken", AtomHopperConstants.MIGRATED)

        then:
        1 * httpClient.execute(_)
    }

    def "Post revoked token" () {
        given:
        setupMock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >>"GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.postToken(user, "someToken", "revokedToken")

        then:
        1 * httpClient.execute(_)
    }

    def "create atom entry - make sure entry is consume" () {
        given:
        setupMock()
        HttpResponse response = Mock()
        StatusLine sl = Mock()
        response.statusLine >> sl
        sl.statusCode >> 201
        HttpEntity enty = Mock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        defaultGroupService.getGroupsForUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.postUser(user, "someToken", AtomHopperConstants.DISABLED)

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> enty
        1 * atomHopperHelper.entityConsume(_)
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
        defaultTenantService = Mock()
        client.defaultTenantService = defaultTenantService
        config = Mock()
        client.config = config
        httpClient = Mock()
        client.httpClient = httpClient
        atomHopperHelper = Mock()
        client.atomHopperHelper = atomHopperHelper

        config.getString("atom.hopper.crypto.password") >> "password"
        config.getString("atom.hopper.crypto.salt") >> "c8 99"
    }
}
