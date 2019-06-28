package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspacecloud.docs.auth.api.v1.AuthData
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault
import com.rackspacecloud.docs.auth.api.v1.User
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenant
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class Cloud11IntegrationTest extends RootIntegrationTest {

    @Autowired Configuration config
    @Autowired UserDao userDao
    @Autowired DomainDao domainDao

    @Shared def serviceAdmin
    @Shared def serviceAdminToken
    @Shared def identityAdmin
    @Shared def identityAdminToken
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def randomMosso
    @Shared def adminUser = "auth"
    @Shared def adminPassword = "auth123"
    @Shared def validServiceName = "cloudServers"

    def setupSpec(){
        sharedRandom = ("$randomness").replace('-',"")

        serviceAdminToken = cloud20.authenticate("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = cloud20.getUserByName(serviceAdminToken, "authQE").getEntity(org.openstack.docs.identity.api.v2.User).value

        String adminUsername = "identityAdmin" + sharedRandom
        def response = cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(adminUsername, "adminUser", "someEmail@rackspace.com", true, "ORD", null, "Password1"))
        def identityAdminId = response.getEntity(org.openstack.docs.identity.api.v2.User).value.id
        cloud20.addApplicationRoleToUser(serviceAdminToken, Constants.IDENTITY_RS_TENANT_ADMIN_ROLE_ID, identityAdminId)
        identityAdmin = cloud20.getUserByName(serviceAdminToken, adminUsername).getEntity(org.openstack.docs.identity.api.v2.User).value
        cloud20.addApplicationRoleToUser(serviceAdminToken, Constants.IDENTITY_RS_DOMAIN_ADMIN_ROLE_ID, identityAdmin.id)
        identityAdminToken = cloud20.authenticate(adminUsername, "Password1").getEntity(AuthenticateResponse).value.token.id
    }

    def setup(){
        cloud11.authUser = adminUser
        cloud11.authPassword = adminPassword
        Random random = new Random()
        randomMosso = 10000000 + random.nextInt(1000000)

        entropy = config.getDouble("token.entropy")
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")
    }

    def cleanupSpec() {
        cloud20.deleteUser(serviceAdminToken, identityAdmin.id)
    }

    def "Admin authenticate with password credentials returns 200"() {
        given:
        String username = "auth" + sharedRandom
        String password = "Password1"
        String domain = "domain" + sharedRandom
        def user = v2Factory.createUserForCreate(username, "displayName", "test@email.com", true, "DFW", domain, password)

        when:
        cloud20.createUser(identityAdminToken, user)
        def authResponse = cloud11.adminAuthenticate(v1Factory.createPasswordCredentials(username, password))

        def getUser20Response = cloud20.getUserByName(serviceAdminToken, username)
        def userEntity = getUser20Response.getEntity(org.openstack.docs.identity.api.v2.User).value

        then:
        authResponse.status == 200

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), userEntity.id)
    }

    def "user authenticate success"() {
        given:
        def username = "userAuth11$sharedRandom"
        String domain = "userAuth11$sharedRandom"
        def key = "$sharedRandom"
        def credential = v1Factory.createApiKeyCredentials(username, key)

        def user = v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", domain, "Password1")
        def userId = cloud20.createUser(identityAdminToken, user).getEntity(org.openstack.docs.identity.api.v2.User).value.id
        cloud20.addApiKeyToUser(serviceAdminToken, userId, credential)

        when:
        def authData = utils11.authenticateWithKey(username, key)

        then:
        authData.token != null

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), userId)
    }

    def "authenticate and verify token entropy"() {
        given:
        def username = "userTestEntroy$sharedRandom"
        def key = "$sharedRandom"
        def credential = v1Factory.createApiKeyCredentials(username, key)

        def user = v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", sharedRandom, "Password1")
        def userId = cloud20.createUser(utils.getIdentityAdminToken(), user).getEntity(org.openstack.docs.identity.api.v2.User).value.id
        cloud20.addApiKeyToUser(utils.getServiceAdminToken(), userId, credential)

        when:
        def startTime = new DateTime()
        def expOne = authAndExpire(username, key)
        def expTwo = authAndExpire(username, key)
        def expThree = authAndExpire(username, key)
        def endTime = new DateTime()
        def range = getRange(defaultExpirationSeconds, startTime, endTime)

        then:
        expOne <= range.get("max")
        expOne >= range.get("min")
        expTwo <= range.get("max")
        expTwo >= range.get("min")
        expThree <= range.get("max")
        expThree >= range.get("min")

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), userId)
    }

    def authAndExpire(username, key) {
        def token = cloud11.authenticate(v1Factory.createUserKeyCredentials(username, key)).getEntity(AuthData).token
        cloud20.revokeToken(token.id)
        return token.expires.toGregorianCalendar().getTime()
    }
}
