package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.docs.core.event.EventType
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.api.resource.cloud.v20.JSONReaderForCloudAuthenticationResponseToken
import com.rackspace.idm.domain.entity.Group
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.DefaultGroupService
import com.rackspace.idm.domain.service.impl.DefaultTenantService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.openstack.docs.identity.api.v2.Token
import org.w3._2005.atom.UsageEntry
import spock.lang.Shared
import spock.lang.Specification
import org.apache.http.client.HttpClient;

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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
    @Shared DefaultCloud20Service defaultCloud20Service

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
        config.getString(_) >> "GLOBAL" >> "http://10.4.39.67:8082/namespace/feed"

        when:
        UsageEntry entry = client.createEntryForUser(user, EventType.DELETE, false)

        then:
        entry.title.value == "Identity Event"
        entry.content.type == MediaType.APPLICATION_ATOM_XML
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
        config.getString(_) >> "DFW1" >> "http://10.4.39.67:8082/namespace/feed"
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
        config.getString(_) >> "GLOBAL" >> "http://10.4.39.67:8082/namespace/feed"

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
        config.getString(_) >> "GLOBAL" >> "http://10.4.39.67:8082/namespace/feed"

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
        config.getString(_) >> "GLOBAL" >> "http://10.4.39.67:8082/namespace/feed"

        when:
        client.postToken(user, "someToken", "revokedToken")

        then:
        1 * httpClient.execute(_)
    }

    def "Get Auth Token" (){
        given:
        setupMock()
        config.getString(_) >> "https://d-api1.cidm.iad2.corp.rackspace.com/" >> "auth" >> "auth123"
        AuthenticateResponse response1 = objectFactory.createAuthenticateResponse()
        Token token1 = new Token()
        token1.id = "1"
        response1.token = token1

        Response.ResponseBuilder response = Response.ok(response1)
        defaultCloud20Service.authenticate(_,_) >> response

        when:
        String token = client.getAuthToken()

        then:
        token != null;
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
        defaultCloud20Service = Mock()
        client.defaultCloud20Service = defaultCloud20Service
        client.objectFactory = objectFactory;
    }
}
