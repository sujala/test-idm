package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.User
import org.apache.commons.httpclient.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

class Cloud20AuthenticationIntegrationTest extends RootIntegrationTest{

    @Autowired
    UserDao userDao

    @Shared def userAdmin, users
    @Shared def domainId
    @Shared def serviceAdminToken

    def void setup() {
        domainId = utils.createDomain()
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        serviceAdminToken = utils.getServiceAdminToken()
    }

    def "Auth with token + tenant returns service catalog appropriately based on state of tenants" () {
        given:
        def token = utils.getToken(userAdmin.username)

        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        def nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with pwd + tenant returns service catalog appropriately based on state of tenants" () {
        given:
        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        def mossoAuthRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(userAdmin.username, Constants.DEFAULT_PASSWORD, mossoTenant.id)
        def nastAuthRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(userAdmin.username, Constants.DEFAULT_PASSWORD, nastTenant.id)

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        def nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with api + tenant returns service catalog appropriately based on state of tenants" () {
        given:
        User userEntity = userDao.getUserById(userAdmin.id)

        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        def mossoAuthRequest = v2Factory.createApiKeyAuthenticationRequestWithTenantId(userAdmin.username, userEntity.apiKey, mossoTenant.id)
        def nastAuthRequest = v2Factory.createApiKeyAuthenticationRequestWithTenantId(userAdmin.username, userEntity.apiKey, nastTenant.id)

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        def nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with OTP MFA returns service catalog appropriately based on state of tenants" () {
        given:
        OTPDevice device = utils.setUpAndEnableUserForMultiFactorOTP(serviceAdminToken, userAdmin)

        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        def response = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        assert wwwHeader != null

        String sessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        def otpPasscode = utils.getOTPCodeForDevice(device)

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        def nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def void assertHasServiceCatalog(response) {
        AuthenticateResponse authResponse = response.getEntity(AuthenticateResponse).value

        assert authResponse.serviceCatalog != null
        assert authResponse.serviceCatalog.service != null
        assert authResponse.serviceCatalog.service.size() > 0
    }

    def void assertDoesNotHaveServiceCatalog(response) {
        AuthenticateResponse authResponse = response.getEntity(AuthenticateResponse).value
        assert authResponse.serviceCatalog == null || authResponse.serviceCatalog.service == null ||
                authResponse.serviceCatalog.service.size() == 0
    }
}
