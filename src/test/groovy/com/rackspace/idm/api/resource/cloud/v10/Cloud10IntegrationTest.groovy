package com.rackspace.idm.api.resource.cloud.v10

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest


class Cloud10IntegrationTest extends RootIntegrationTest {

    @Autowired
    Configuration config

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared String sharedRandom
    @Shared String username
    @Shared String adminToken
    @Shared User user

    def setupSpec() {
        adminToken = cloud10.authenticate("auth", "thisismykey").headers.get("X-Auth-Token")[0]

        sharedRandom = ("$sharedRandomness").replace('-',"")
        username = "auth10Test$sharedRandom"

        def response = cloud20.createUser(adminToken, v2Factory.createUserForCreate(username, "displayName", "testemail@rackspace.com", true, "ORD", "auth10Domain$sharedRandom", "Password1"))
        user = response.getEntity(User).value
        cloud20.addApiKeyToUser(adminToken, user.id, v1Factory.createApiKeyCredentials(username, "thisismykey"))
    }

    def setup() {
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")
        entropy = config.getDouble("token.entropy")
    }

    def cleanupSpec() {
        cloud20.destroyUser(adminToken, user.id)
    }

    def "authenticate using username and apiKey"() {
        when:
        def response = cloud10.authenticate(username, "thisismykey")
        then:
        response.status == 204
    }

    def "authenticate using username and invalid apiKey returns 401"() {
        when:
        def response = cloud10.authenticate(username, "invalidApiKey")
        then:
        response.status == 401
    }

    def "authenticate and verify token entropy"() {
        given:
        def response = cloud10.authenticate(username, "thisismykey")
        String token = response.headers.get("X-Auth-Token")[0]
        cloud11.revokeToken(token)

        when:
        def startTime = new DateTime()
        def expirationOne = authAndExpire(username, "thisismykey")
        def expirationTwo = authAndExpire(username, "thisismykey")
        def expirationThree = authAndExpire(username, "thisismykey")
        def endTime = new DateTime()

        def range = getRange(defaultExpirationSeconds, startTime, endTime)

        then:
        expirationOne >= range.get("min")
        expirationOne <= range.get("max")
        expirationTwo >= range.get("min")
        expirationTwo <= range.get("max")
        expirationThree >= range.get("min")
        expirationThree <= range.get("max")
    }

    @Unroll
    def "auth v1.0 includes X-Tenant-Id header, featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V10_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.resetApiKey(userAdmin)
        def apiKey = utils.getUserApiKey(userAdmin).apiKey

        when: "auth v1.0"
        def response = cloud10.authenticate(userAdmin.username, apiKey)

        then:
        response.headers.containsKey(GlobalConstants.X_TENANT_ID) == featureEnabled
        if (featureEnabled) {
            assert response.headers.get(GlobalConstants.X_TENANT_ID)[0] == domainId
        }

        cleanup:
        utils.deleteUsers(users)

        where:
        featureEnabled  | _
        true            | _
        false           | _
    }

    def authAndExpire(String username, String key) {
        def token = cloud10.authenticate(username, key).headers.get("X-Auth-Token")[0]
        def validateResponseOne = cloud20.validateToken(adminToken, token)
        cloud11.revokeToken(token)

        def expiration = validateResponseOne.getEntity(AuthenticateResponse).value.token.expires
        return expiration.toGregorianCalendar().getTime()
    }

}
