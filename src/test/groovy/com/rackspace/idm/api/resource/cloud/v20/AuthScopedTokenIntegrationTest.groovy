package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ScopeEnum
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import testHelpers.V2Factory

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
import static org.apache.http.HttpStatus.SC_ACCEPTED
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static org.apache.http.HttpStatus.SC_OK

@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class AuthScopedTokenIntegrationTest extends RootIntegrationTest {

    @Autowired LdapScopeAccessRepository scopeAccessRepository
    @Autowired LdapUserRepository userRepository
    @Autowired SimulatorMobilePhoneVerification simulatorMobilePhoneVerification;

    /**
     * Override the grizzly start because we want to add another context file.
     * @return
     */
    @Override
    public void doSetupSpec(){
        this.resource = startOrRestartGrizzly("classpath:app-config.xml classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml")
    }

    @Override
    public void doCleanupSpec() {
        stopGrizzly();
    }

    def "Auth by password with scope creates ScopeAccess with scope separate from regular scopeAccess"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        def scopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(entity.token.id)

        then:
        scopeAccess != null
        scopeAccess.scope == SCOPE_SETUP_MFA
        entity.serviceCatalog == null

        when:
        def regularAuth = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)
        assert (regularAuth.status == SC_OK)
        def entity2 = regularAuth.getEntity(AuthenticateResponse).value
        assert(entity2 != null)

        then:
        entity.token.id != entity2.token.id

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth by apikey with scope creates ScopeAccess with scope"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        User initialUserAdmin = userRepository.getUserById(userAdmin.id)

        when:
        def response = cloud20.authenticateApiKeyWithScope(userAdmin.username, initialUserAdmin.apiKey, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        def scopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(entity.token.id)

        then:
        scopeAccess != null
        scopeAccess.scope == SCOPE_SETUP_MFA

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if domain attribute present"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def request = v2Factory.createPasswordAuthenticationRequestWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        request.domain = v1Factory.createDomain(null, "Domain")

        when:
        def response = cloud20.authenticate(request)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if authing with token"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def request = v2Factory.createAuthenticationRequest()
        request.token = v2Factory.createTokenForAuthenticationRequest()
        request.tenantId = null
        request.tenantName = null
        request.scope = ScopeEnum.fromValue(SCOPE_SETUP_MFA)

        when:
        def response = cloud20.authenticate(request)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if authing with passcode"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def request = v2Factory.createPasscodeAuthenticationRequest("1234")
        request.scope = ScopeEnum.fromValue(SCOPE_SETUP_MFA)

        when:
        def response = cloud20.authenticate(request)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if authing with multi-factor enabled"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def userEntity = userRepository.getUserById(userAdmin.id)
        userEntity.multifactorEnabled = true
        userRepository.updateUserAsIs(userEntity)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Validating scoped token in v2.0 throws 404"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def identityAdmin = users.get(0)
        def identityToken = utils.getToken(identityAdmin.username)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        def scopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(entity.token.id)
        def validateResponse = cloud20.validateToken(identityToken, scopeAccess.accessTokenString)

        then:
        validateResponse.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Validating scoped token in v1.1 throws 404"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        def scopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(entity.token.id)
        def validateResponse = cloud11.validateToken(scopeAccess.accessTokenString)

        then:
        validateResponse.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "A scoped token can setup MFA"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone = v2Factory.createMobilePhone();
        def constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin)

        when:
        def AddPhoneResponse = cloud20.addPhoneToUser(entity.token.id, userAdmin.id, requestMobilePhone)

        then:
        AddPhoneResponse.status == SC_CREATED

        when:
        def phoneEntity = AddPhoneResponse.getEntity(MobilePhone)
        def sendVerficationCodeResponse = cloud20.sendVerificationCode(entity.token.id, userAdmin.id, phoneEntity.id)

        then:
        sendVerficationCodeResponse.status == SC_ACCEPTED

        when:
        def verifyCodeResponse = cloud20.verifyVerificationCode(entity.token.id, userAdmin.id, phoneEntity.id, constantVerificationCode)

        then:
        verifyCodeResponse.status == SC_NO_CONTENT

        when:
        def listDevicesResponse = cloud20.listDevices(entity.token.id, userAdmin.id)

        then:
        listDevicesResponse.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "A scoped token trying to access resources other than MFA multi-factor throws 403"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)

        def nonMfaResponse = cloud20.addDomain(entity.token.id, null)

        then:
        nonMfaResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "A scoped token cannot be used to access another user's multi-factor"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)

        def mfaResponse = cloud20.listDevices(entity.token.id, "someOtherUser")

        then:
        mfaResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "A scoped token cannot be used on a user that already has multi-factor enabled"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)

        User userAdminEntity = userRepository.getUserByUsername(userAdmin.username)
        userAdminEntity.setMultifactorEnabled(true)
        userRepository.updateUserAsIs(userAdminEntity)

        def mfaResponse = cloud20.listDevices(entity.token.id, userAdminEntity.id)

        then:
        mfaResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
