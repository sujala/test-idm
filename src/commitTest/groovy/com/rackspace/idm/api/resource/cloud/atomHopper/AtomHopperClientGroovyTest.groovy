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
import com.rackspace.idm.domain.service.TokenRevocationService
import com.rackspace.idm.domain.service.impl.DefaultIdentityUserService
import com.rackspace.idm.domain.service.impl.DefaultTenantService
import com.rackspace.idm.util.CryptHelper
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.protocol.HttpContext
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.w3._2005.atom.UsageEntry
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

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
    @Shared DefaultIdentityUserService identityUserService

    @Shared DefaultTenantService defaultTenantService
    @Shared Configuration config
    @Shared IdentityConfig identityConfig
    @Shared CloseableHttpClient httpClient
    @Shared org.openstack.docs.identity.api.v2.ObjectFactory objectFactory;
    @Shared CryptHelper cryptHelper
    @Shared AtomHopperHelper atomHopperHelper

    @Shared IdentityConfig.StaticConfig staticConfig
    @Shared IdentityConfig.ReloadableConfig reloadableConfig

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
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
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
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        def groupName = "MyGroup"
        def groupId = 1
        identityUserService.getGroupsForEndUser(_) >> [createGroup(groupName, groupId,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        UsageEntry entry = client.createEntryForUser(user, EventType.DELETE, false)

        def cloudIdentityType = (com.rackspace.docs.event.identity.user.CloudIdentityType) entry.content.event.any.get(0)
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
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        client.postUser(user, AtomHopperConstants.DELETED)

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
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        client.postUser(user, AtomHopperConstants.DISABLED)

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
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        client.postUser(user, AtomHopperConstants.MIGRATED)

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
        HttpEntity enty = Mock()
        User user = new User()
        user.username = "testUser"
        user.id = "1"
        user.region = "DFW"
        user.roles = [createTenantRole("someRole", "1", "desc")].asList()
        identityUserService.getGroupsForEndUser(_) >> [createGroup("group",1,"desc")].asList()
        defaultTenantService.getTenantRolesForUser(_) >> [createTenantRole("someRole", "1", "desc")].asList()
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"
        defaultTenantService.getMossoIdFromTenantRoles(_) >> "tenantId"

        when:
        client.postUser(user, AtomHopperConstants.DISABLED)

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> enty
        1 * atomHopperHelper.entityConsume(_)
    }

    def "create atom entry for token - make sure entry is consume" () {
        given:
        CloseableHttpResponse response = Mock()
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
        config.getString(_) >> "GLOBAL" >> "GLOBAL" >> "http://10.4.39.67:8888/namespace/feed"

        when:
        client.postToken(user, "revokedToken")

        then:
        1 * httpClient.execute(_) >> response
        1 * response.entity >> enty
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
        HttpEntity enty = Mock()
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
        1 * response.entity >> enty
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
        identityConfig = Mock()
        staticConfig = Mock()
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
        identityConfig = Mock()
        staticConfig = Mock()
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
        identityUserService = Mock()
        client.identityUserService = identityUserService
        defaultTenantService = Mock()
        client.defaultTenantService = defaultTenantService

        identityConfig = Mock()
        staticConfig = Mock()
        reloadableConfig = Mock()
        identityConfig.getStaticConfig() >> staticConfig
        identityConfig.getReloadableConfig() >> reloadableConfig
        client.identityConfig = identityConfig

        reloadableConfig.reuseJaxbContext() >> false

        httpClient = Mock()
        client.httpClient = httpClient
        atomHopperHelper = Mock()
        client.atomHopperHelper = atomHopperHelper

        reloadableConfig.getAtomHopperDataCenter() >> "GLOBAL"
        reloadableConfig.getAtomHopperUrl() >> "http://localhost:8888/namespace/feed"
        reloadableConfig.getAtomHopperRegion() >> "GLOBAL"

        config = Mock()
        config.getString("atom.hopper.crypto.password") >> "password"
        config.getString("atom.hopper.crypto.salt") >> "c8 99"
    }
}
