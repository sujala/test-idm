package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.event.identity.trr.user.CloudIdentityType
import com.rackspace.docs.event.identity.trr.user.ValuesEnum
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.MemoryTokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.Group
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.TokenRevocationRecord
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.TokenRevocationService
import com.rackspace.idm.domain.service.impl.DefaultIdentityUserService
import com.rackspace.idm.domain.service.impl.DefaultTenantService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.util.CryptHelper
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
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
    @Shared DefaultUserService defaultUserService
    @Shared DefaultIdentityUserService identityUserService

    @Shared DefaultTenantService defaultTenantService
    @Shared HttpClient httpClient
    @Shared org.openstack.docs.identity.api.v2.ObjectFactory objectFactory;
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
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        UsageEntry entry = client.createEntryForUser(user, EventType.DELETE, false)

        then:
        entry.title.value.equals("Identity Event")
        entry.content.type.equals(MediaType.APPLICATION_XML)
        entry.content.event.resourceName.equals("testUser")
        entry.content.event.tenantId.equals("tenantId")
    }

    def "atom entry posts Groud Id not Group Name " () {
        given:
        setupMock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        def groupName = "MyGroup"
        def groupId = 1
        identityUserService.getGroupsForEndUser(_) >> [createGroup(groupName, groupId,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        UsageEntry entry = client.createEntryForUser(user, EventType.DELETE, false)

        def cloudIdentityType = (com.rackspace.docs.event.identity.user.CloudIdentityType) entry.content.event.any.get(0)
        def groupInfo = cloudIdentityType.getGroups().get(0)
        then:
        !groupInfo.equals(groupName)
        Integer.parseInt(groupInfo) == groupId
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
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        client.postUser(user, "someToken", AtomHopperConstants.DISABLED)

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> enty
        1 * atomHopperHelper.entityConsume(_)
    }

    def "create atom entry for token - make sure entry is consume" () {
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
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()

        when:
        client.postToken(user, "someToken", "revokedToken")

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> enty
        1 * atomHopperHelper.entityConsume(_)
    }

    def "create feed event for user TRR" () {
        given:
        setupMock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"

        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()

        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        TokenRevocationRecord trr = new MemoryTokenRevocationRecordPersistenceStrategy.BasicTokenRevocationRecord()
        trr.setTargetCreatedBefore(new Date())
        trr.setTargetIssuedToId(user.id)

        when:
        trr.setTargetAuthenticatedByMethodGroups(TokenRevocationService.AUTH_BY_LIST_ALL_TOKENS)
        UsageEntry entry = client.createUserTrrEntry(user, trr)

        then:
        entry.getContent().getEvent().getAny().get(0) instanceof CloudIdentityType
        CloudIdentityType trrEntry = (CloudIdentityType) entry.getContent().getEvent().getAny().get(0);
        CollectionUtils.isEmpty(trrEntry.tokenAuthenticatedBy)

        when: "have single auth by in single set"
        trr.setTargetAuthenticatedByMethodGroups(TokenRevocationService.AUTH_BY_LIST_API_TOKENS)
        entry = client.createUserTrrEntry(user, trr)

        then:
        entry.getContent().getEvent().getAny().get(0) instanceof CloudIdentityType
        CloudIdentityType trrEntry2 = (CloudIdentityType) entry.getContent().getEvent().getAny().get(0);
        trrEntry2.tokenAuthenticatedBy.get(0).values.contains(ValuesEnum.APIKEY)

        when: "have multiple auth by in single set"
        trr.setTargetAuthenticatedByMethodGroups(Arrays.asList(AuthenticatedByMethodGroup.getGroup(AuthenticatedByMethodEnum.PASSWORD, AuthenticatedByMethodEnum.PASSCODE)))
        entry = client.createUserTrrEntry(user, trr)

        then:
        entry.getContent().getEvent().getAny().get(0) instanceof CloudIdentityType
        CloudIdentityType trrEntry3 = (CloudIdentityType) entry.getContent().getEvent().getAny().get(0);
        CollectionUtils.isEqualCollection(trrEntry3.tokenAuthenticatedBy.get(0).values, Arrays.asList(ValuesEnum.PASSWORD, ValuesEnum.PASSCODE))

        when: "have single auth by in multiple sets"
        trr.setTargetAuthenticatedByMethodGroups(Arrays.asList(AuthenticatedByMethodGroup.PASSWORD, AuthenticatedByMethodGroup.APIKEY))
        entry = client.createUserTrrEntry(user, trr)

        then:
        entry.getContent().getEvent().getAny().get(0) instanceof CloudIdentityType
        CloudIdentityType trrEntry4 = (CloudIdentityType) entry.getContent().getEvent().getAny().get(0);
        CollectionUtils.isEqualCollection(trrEntry4.tokenAuthenticatedBy.get(0).values, Arrays.asList(ValuesEnum.PASSWORD))
        CollectionUtils.isEqualCollection(trrEntry4.tokenAuthenticatedBy.get(1).values, Arrays.asList(ValuesEnum.APIKEY))
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
        defaultUserService = Mock()
        client.userService = defaultUserService
        identityUserService = Mock()
        client.identityUserService = identityUserService
        defaultTenantService = Mock()
        client.defaultTenantService = defaultTenantService
        client.identityConfig = Mock(IdentityConfig)
        client.identityConfig.getReloadableConfig() >> Mock(IdentityConfig.ReloadableConfig)
        client.identityConfig.getReloadableConfig().getAtomHopperUrl() >> "http://localhost:8888/namespace/feed"
        client.identityConfig.getReloadableConfig().getAtomHopperDataCenter() >> "GLOBAL"
        client.identityConfig.getReloadableConfig().getAtomHopperRegion() >> "GLOBAL"
        httpClient = Mock()
        client.httpClient = httpClient
        atomHopperHelper = Mock()
        client.atomHopperHelper = atomHopperHelper
    }
}
