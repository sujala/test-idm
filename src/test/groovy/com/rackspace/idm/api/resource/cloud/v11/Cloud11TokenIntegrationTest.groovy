package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspacecloud.docs.auth.api.v1.AuthData
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class Cloud11TokenIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessService scopeAccessService

    def "Authenticate user within a disable domain should return 403" () {
        given:
        def user = utils11.createUser()
        def domainId = String.valueOf(user.mossoId)
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }

        when:
        utils11.authenticateWithKey(user.id, user.key)
        utils.updateDomain(domainId, domain)

        def cred = v1Factory.createUserKeyCredentials(user.id, user.key)
        def response = cloud11.authenticate(cred)

        def mossoCred = v1Factory.createMossoCredentials(user.mossoId, user.key)
        def mossoResponse = cloud11.adminAuthenticate(mossoCred)

        def nastCred = v1Factory.createNastCredentials(user.nastId, user.key)
        def nastResponse = cloud11.adminAuthenticate(nastCred)

        then:
        response.status ==  SC_FORBIDDEN
        mossoResponse.status ==  SC_FORBIDDEN
        nastResponse.status ==  SC_FORBIDDEN

        cleanup:
        utils11.deleteUser(user)
        utils.deleteTenantById(String.valueOf(user.mossoId))
        utils.deleteTenantById(user.nastId)
        utils.deleteDomain(domainId)
    }

    /**
     *  Produce tokens that contain userRsId.
     *
     */
    def "auth produces token with populated userRsId"() {
        def user = utils11.createUser()
        AuthData authData = utils11.authenticateWithKey(user.id, user.key)

        when:
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(authData.token.id)

        then:
        token != null
        token.userRsId != null
    }

    @Unroll
    def "auth v1.1 returns X-Tenant-Id header - featureEnabled: #featureEnabled, accept: #accept, request: #request"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_TENANT_ID_IN_AUTH_RESPONSE_V11_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def mossoId = domainId
        def nastId = Constants.NAST_TENANT_PREFIX + domainId
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.resetApiKey(userAdmin)
        def apiKey = utils.getUserApiKey(userAdmin).apiKey

        when: "v1.1 auth w/ API key"
        def apiKeyCreds = v1Factory.createUserKeyCredentials(userAdmin.username, apiKey)
        def response = cloud11.authenticate(apiKeyCreds, request, accept)

        then:
        response.headers.containsKey(GlobalConstants.X_TENANT_ID) == featureEnabled
        if (featureEnabled) {
            assert response.headers.get(GlobalConstants.X_TENANT_ID)[0] == mossoId
        }

        when: "v1.1 auth w/ password"
        def pwCreds = v1Factory.createPasswordCredentials(userAdmin.username, Constants.DEFAULT_PASSWORD)
        response = cloud11.adminAuthenticate(pwCreds, request, accept)

        then:
        response.headers.containsKey(GlobalConstants.X_TENANT_ID) == featureEnabled
        if (featureEnabled) {
            assert response.headers.get(GlobalConstants.X_TENANT_ID)[0] == mossoId
        }

        when: "v1.1 mosso auth"
        def mossoCred = v1Factory.createMossoCredentials(Integer.parseInt(mossoId), apiKey)
        response = cloud11.adminAuthenticate(mossoCred, request, accept)

        then:
        response.headers.containsKey(GlobalConstants.X_TENANT_ID) == featureEnabled
        if (featureEnabled) {
            assert response.headers.get(GlobalConstants.X_TENANT_ID)[0] == mossoId
        }

        when: "v1.1 nast auth"
        def nastCred = v1Factory.createNastCredentials(nastId, apiKey)
        response = cloud11.adminAuthenticate(nastCred, request, accept)

        then:
        response.headers.containsKey(GlobalConstants.X_TENANT_ID) == featureEnabled
        if (featureEnabled) {
            assert response.headers.get(GlobalConstants.X_TENANT_ID)[0] == nastId
        }

        cleanup:
        utils.deleteUsers(users)

        where:
        featureEnabled  | accept                          | request
        true            | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        true            | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        true            | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        true            | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        false           | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        false           | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        false           | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        false           | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

}
