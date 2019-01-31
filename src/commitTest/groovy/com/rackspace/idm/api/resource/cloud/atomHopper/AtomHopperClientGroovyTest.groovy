package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.event.identity.trr.user.CloudIdentityType
import com.rackspace.docs.event.identity.trr.user.ValuesEnum
import com.rackspace.docs.event.identity.user.CloudIdentity1Type
import com.rackspace.docs.event.identity.user.CloudIdentity3Type
import com.rackspace.docs.event.identity.user.ResourceTypes1
import com.rackspace.docs.event.identity.user.ResourceTypes3
import com.rackspace.docs.event.identity.user.UpdatedAttributes3Enum
import com.rackspace.idm.domain.dao.impl.MemoryTokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.TokenRevocationService
import org.apache.commons.collections4.CollectionUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.protocol.HttpContext
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.w3._2005.atom.UsageEntry
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.core.MediaType

class AtomHopperClientGroovyTest extends RootServiceTest {
    @Shared AtomHopperClient client
    @Shared AtomHopperLogger httpClient
    @Shared org.openstack.docs.identity.api.v2.ObjectFactory objectFactory;
    @Shared AtomHopperHelper atomHopperHelper

    def setupSpec(){
        client = new AtomHopperClient();
        objectFactory = new ObjectFactory();
    }

    def setup() {
        setupMock(client)
    }

    def cleanup() {
        client.destroy()
    }

    def "create atom entry for delete user" () {
        given:
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUserPerformant(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"
        reloadableConfig.getFeedsUserProductSchemaVersion() >> 1

        when:
        UsageEntry entry = client.createEntryForUser(user, EventType.DELETE, false, FeedsUserStatusEnum.DELETED, "requestId")

        then:
        entry.title.value.equals("Identity Event")
        entry.content.type.equals(MediaType.APPLICATION_XML)
        entry.content.event.resourceName.equals("testUser")
        entry.content.event.tenantId.equals("tenantId")
    }

    def "atom entry posts GroupId not Group Name " () {
        given:
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        def groupName = "MyGroup"
        def groupId = 1
        identityUserService.getGroupsForEndUser(_) >> [createGroup(groupName, groupId,"desc")].asList()
        defaultTenantService.getTenantRolesForUserPerformant(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"
        reloadableConfig.getFeedsUserProductSchemaVersion() >> 1

        when:
        UsageEntry entry = client.createEntryForUser(user, EventType.DELETE, false, FeedsUserStatusEnum.GROUP, "requestId")

        def cloudIdentityType = (CloudIdentity1Type) entry.content.event.any.get(0)
        def groupInfo = cloudIdentityType.getGroups().get(0)
        then:
        !groupInfo.equals(groupName)
        Integer.parseInt(groupInfo) == groupId
    }

    def "Post deleted user" () {
        given:
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        config.getString(_) >> "DFW" >> "DFW1" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getTenantRolesForUserPerformant(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"
        reloadableConfig.getFeedsUserProductSchemaVersion() >> 1

        when:
        client.postUser(user, FeedsUserStatusEnum.DELETED, "requestId")

        then:
        1 * httpClient.execute(_)
    }

    def "Post disabled user" () {
        given:
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUserPerformant(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"
        reloadableConfig.getFeedsUserProductSchemaVersion() >> 1

        when:
        client.postUser(user, FeedsUserStatusEnum.DISABLED, "requestId")

        then:
        1 * httpClient.execute(_)
    }

    def "Post migrated user" () {
        given:
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUserPerformant(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"
        reloadableConfig.getFeedsUserProductSchemaVersion() >> 1

        when:
        client.postUser(user, FeedsUserStatusEnum.MIGRATED, "requestId")

        then:
        1 * httpClient.execute(_)
    }

    def "Post revoked token" () {
        given:
        CloseableHttpResponse response = Mock()
        StatusLine sl = Mock()
        response.statusLine >> sl
        sl.statusCode >> 201
        HttpEntity entity = Mock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >>"GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.postToken(user, "revokedToken")

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> entity
        1 * atomHopperHelper.entityConsume(_)
    }

    def "create atom entry - make sure entry is consume" () {
        given:
        CloseableHttpResponse response = Mock()
        StatusLine sl = Mock()
        response.statusLine >> sl
        sl.statusCode >> 201
        HttpEntity entity = Mock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUserPerformant(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"
        reloadableConfig.getFeedsUserProductSchemaVersion() >> 1

        when:
        client.postUser(user, FeedsUserStatusEnum.DISABLED, "requestId")

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> entity
        1 * atomHopperHelper.entityConsume(_)
    }

    def "create atom entry for token - make sure entry is consume" () {
        given:
        CloseableHttpResponse response = Mock()
        StatusLine sl = Mock()
        response.statusLine >> sl
        sl.statusCode >> 201
        HttpEntity entity = Mock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.postToken(user, "revokedToken")

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> entity
        1 * atomHopperHelper.entityConsume(_)
    }

    @Unroll
    def "Entity consumed regardless of feature flag controls reusing jaxbcontext. When #setting" () {
        given:
        setupMock(client)
        CloseableHttpResponse response = Mock()
        StatusLine sl = Mock()
        response.statusLine >> sl
        sl.statusCode >> 201
        HttpEntity entity = Mock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.identityConfig.getReloadableConfig().reuseJaxbContext() >> true
        client.postToken(user, "revokedToken")

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> entity
        1 * atomHopperHelper.entityConsume(_)

        where:
        setting | _
        true | _
        false | _
    }

    def "create feed event for user TRR" () {
        given:
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"

        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString("atom.hopper.region") >> "GLOBAL"
        config.getString("atom.hopper.dataCenter") >> "GLOBAL"

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

    def "When feeds deamon is enabled, idle connection monitor thread is created"() {
        given:
        staticConfig.getFeedsDeamonEnabled() >> true
        staticConfig.getFeedsMaxConnectionsPerRoute() >> 100
        staticConfig.getFeedsMaxTotalConnections() >> 200

        when:
        client.init()
        sleep(1000)

        then:
        client.idleConnectionMonitorThread != null
        client.idleConnectionMonitorThread.isAlive() == true
        client.idleConnectionMonitorThread.isDaemon() == true
        (1.._) * reloadableConfig.getFeedsDaemonEvictionFrequency() >> 100
        (1.._) * reloadableConfig.getFeedsDaemonEvictionCloseIdleConnectionsAfter() >> 100
    }

    def "When feeds deamon is not enabled, idle connection monitor thread is not created"() {
        given:
        def client = new AtomHopperClient()
        setupMock(client)
        staticConfig.getFeedsDeamonEnabled() >> false
        staticConfig.getFeedsMaxConnectionsPerRoute() >> 100
        staticConfig.getFeedsMaxTotalConnections() >> 200

        when:
        client.init()
        sleep(1000)

        then:
        client.idleConnectionMonitorThread == null
        0 * reloadableConfig.getFeedsDaemonEvictionFrequency() >> 100
        0 * reloadableConfig.getFeedsDaemonEvictionCloseIdleConnectionsAfter() >> 100
    }

    def "idleConnectionMonitorThread closes connections"() {
        given:
        def connMgr = Mock(HttpClientConnectionManager)
        def idleConnectionMonitorThread = new AtomHopperClient.IdleConnectionMonitorThread(connMgr, identityConfig);
        reloadableConfig.getFeedsDaemonEvictionFrequency() >> 100
        reloadableConfig.getFeedsDaemonEvictionCloseIdleConnectionsAfter() >> 100

        when:
        idleConnectionMonitorThread.setDaemon(true)
        idleConnectionMonitorThread.start()
        sleep(1000)

        then:
        (1.._) * connMgr.closeExpiredConnections()
        (1.._) * connMgr.closeIdleConnections(_, _)
    }

    def "idleConnectionMonitorThread does not close idle connections when idle threshold property set to 0"() {
        given:
        def connMgr = Mock(HttpClientConnectionManager)
        def idleConnectionMonitorThread = new AtomHopperClient.IdleConnectionMonitorThread(connMgr, identityConfig);
        reloadableConfig.getFeedsDaemonEvictionFrequency() >> 10000
        reloadableConfig.getFeedsDaemonEvictionCloseIdleConnectionsAfter() >> 0

        when:
        idleConnectionMonitorThread.setDaemon(true)
        idleConnectionMonitorThread.start()
        sleep(1000)

        then:
        0 * connMgr.closeExpiredConnections()
        0 * connMgr.closeIdleConnections(_, _)
    }

    def "keep-alive stategy uses duration config value when no default duration is specified and keepAlive is enabled"() {
        given:
        identityConfig.getReloadableConfig() >> reloadableConfig
        reloadableConfig.getFeedsAllowConnectionKeepAlive() >> true
        reloadableConfig.getFeedsConnectionKeepAliveDuration() >> 1000

        def connectionKeepAliveStrategy = new AtomHopperConnectionKeepAliveStrategy(identityConfig);
        DefaultConnectionKeepAliveStrategy internalConnectionKeepAliveStrategy = Mock()
        internalConnectionKeepAliveStrategy.getKeepAliveDuration(_,_) >> -1
        connectionKeepAliveStrategy.connectionKeepAliveStrategy = internalConnectionKeepAliveStrategy
        HttpResponse response = Mock()
        HttpContext context = Mock()

        when:
        def duration = connectionKeepAliveStrategy.getKeepAliveDuration(response, context)

        then:
        duration == 1000
    }

    @Unroll
    def "keep-alive strategy uses default duration when specified with keepAlive value: #keepAlive"() {
        given:
        identityConfig.getReloadableConfig() >> reloadableConfig
        reloadableConfig.getFeedsAllowConnectionKeepAlive() >> keepAlive
        reloadableConfig.getFeedsConnectionKeepAliveDuration() >> 1000

        def connectionKeepAliveStrategy = new AtomHopperConnectionKeepAliveStrategy(identityConfig);
        DefaultConnectionKeepAliveStrategy internalConnectionKeepAliveStrategy = Mock()
        internalConnectionKeepAliveStrategy.getKeepAliveDuration(_,_) >> 500
        connectionKeepAliveStrategy.connectionKeepAliveStrategy = internalConnectionKeepAliveStrategy
        HttpResponse response = Mock()
        HttpContext context = Mock()

        when:
        def duration = connectionKeepAliveStrategy.getKeepAliveDuration(response, context)

        then:
        duration == 500

        where:
        keepAlive | _
        true      | _
        false     | _
    }

    @Unroll
    def "createCloudIdentity1Type: builds correct v1 cloud identity type - FeedUserStatus = #feedUserStatus"() {
        given:
        def user = entityFactory.createUser().with {
            it.contactId = "contactId"
            it
        }
        def group = entityFactory.createGroup("id", "groupName", "description")
        def groupList = [group]
        def tenantRole = entityFactory.createTenantRole()
        def tenantRoles = [entityFactory.createTenantRole()]

        when:
        CloudIdentity1Type cloudIdentity1Type = client.createCloudIdentity1Type(user, user.username, groupList, tenantRoles, false)

        then:
        cloudIdentity1Type != null
        cloudIdentity1Type.getServiceCode() == AtomHopperConstants.CLOUD_IDENTITY
        cloudIdentity1Type.getVersion() == String.valueOf(AtomHopperConstants.V1_USER_PRODUCT_SCHEMA_VERSION)
        cloudIdentity1Type.getResourceType() == ResourceTypes1.USER
        cloudIdentity1Type.getDisplayName() == user.username
        !cloudIdentity1Type.isMultiFactorEnabled()
        cloudIdentity1Type.getGroups().size() == 1
        cloudIdentity1Type.getGroups().get(0) == group.groupId
        cloudIdentity1Type.getRoles().size() == 1
        cloudIdentity1Type.getRoles().get(0) == tenantRole.name
        !cloudIdentity1Type.isMigrated()

        where:
        feedUserStatus << FeedsUserStatusEnum.values()
    }

    @Unroll
    def "createCloudIdentity3Type: builds correct v3 cloud identity type - FeedUserStatus = #feedUserStatus"() {
        given:
        def user = entityFactory.createUser().with {
            it.contactId = "contactId"
            it
        }
        def group = entityFactory.createGroup("id", "groupName", "description")
        def groupList = [group]
        def tenantRole = entityFactory.createTenantRole()
        def tenantRoles = [entityFactory.createTenantRole()]
        def requestId = "requestId"

        when:
        CloudIdentity3Type cloudIdentity3Type = client.createCloudIdentity3Type(user, user.username, groupList, tenantRoles, false, requestId, feedUserStatus as FeedsUserStatusEnum)

        then:
        cloudIdentity3Type != null
        cloudIdentity3Type.getServiceCode() == AtomHopperConstants.CLOUD_IDENTITY
        cloudIdentity3Type.getVersion() == String.valueOf(AtomHopperConstants.V3_USER_PRODUCT_SCHEMA_VERSION)
        cloudIdentity3Type.getResourceType() == ResourceTypes3.USER
        cloudIdentity3Type.getDisplayName() == user.username
        cloudIdentity3Type.getRequestId() == requestId
        !cloudIdentity3Type.isMultiFactorEnabled()
        cloudIdentity3Type.getContactId() == user.contactId
        cloudIdentity3Type.getGroups().size() == 1
        cloudIdentity3Type.getGroups().get(0) == group.groupId
        cloudIdentity3Type.getRoles().size() == 1
        cloudIdentity3Type.getRoles().get(0) == tenantRole.name
        !cloudIdentity3Type.isMigrated()

        if (((FeedsUserStatusEnum) feedUserStatus).isUpdateEvent()) {
            assert cloudIdentity3Type.getUpdatedAttributes() != null
            if (feedUserStatus == FeedsUserStatusEnum.USER_GROUP) {
                assert cloudIdentity3Type.getUpdatedAttributes().find {it == UpdatedAttributes3Enum.ROLES}
            }
            if (feedUserStatus == FeedsUserStatusEnum.ROLE) {
                assert cloudIdentity3Type.getUpdatedAttributes().find {it == UpdatedAttributes3Enum.ROLES}
            }

            if (cloudIdentity3Type.getUpdatedAttributes().isEmpty()) {
                assert cloudIdentity3Type.getUpdatedAttributes().find {it == UpdatedAttributes3Enum.USER}
            }
        } else {
            assert cloudIdentity3Type.getUpdatedAttributes().isEmpty()
        }

        where:
        feedUserStatus << FeedsUserStatusEnum.values()
    }

    def "createEntryForUser: calls correct services"(){
        given:
        def user = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole()
        def tenantRoles = [tenantRole]
        def group = entityFactory.createGroup("groupId", "groupName", "description")

        when:
        UsageEntry usageEntry = client.createEntryForUser(user, EventType.UPDATE, false, FeedsUserStatusEnum.UPDATE, "requestId")

        then:
        usageEntry != null

        1 * reloadableConfig.getFeedsUserProductSchemaVersion() >> 1
        1 * defaultTenantService.getTenantRolesForUserPerformant(user) >> tenantRoles
        1 * identityUserService.getGroupsForEndUser(user.id) >> [group]
        1 * defaultTenantService.getMossoIdFromTenantRoles(tenantRoles) >> "mossoId"
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

    def setupMock(client) {
        mockIdentityUserService(client)
        mockDefaultTenantService(client)
        mockIdentityConfig(client)

        reloadableConfig.reuseJaxbContext() >> false

        httpClient = Mock()
        client.httpClient = httpClient
        atomHopperHelper = Mock()
        client.atomHopperHelper = atomHopperHelper

        reloadableConfig.getFeedsDataCenter() >> "GLOBAL"
        reloadableConfig.getFeedsUrl() >> "http://localhost:8888/namespace/feed"
        reloadableConfig.getFeedsRegion() >> "GLOBAL"

        config.getString("atom.hopper.crypto.password") >> "password"
        config.getString("atom.hopper.crypto.salt") >> "c8 99"
    }
}
