package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class DisableMultifactorSecurityIntegrationTest extends RootIntegrationTest {

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification;

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    def serviceAdmin1, identityAdmin1, userAdmin1Domain1, userAdmin2Domain1, userManage1Domain1, userManage2Domain1, defaultUser1Domain1, defaultUser2Domain1
    def serviceAdmin2, identityAdmin2, userAdmin1Domain2, userAdmin2Domain2, userManage1Domain2, userManage2Domain2, defaultUser1Domain2, defaultUser2Domain2
    def serviceAdmin1Token, serviceAdmin2Token

    enum TokenType { SERVICE_ADMIN, IDENTITY_ADMIN, USER_ADMIN, USER_MANAGE, DEFAULT_USER}

    /**
     * Override the grizzly start because we want to add another context file.
     * @return
     */
    @Override
    public void doSetupSpec() {
        this.resource = startOrRestartGrizzly("classpath:app-config.xml classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml")
    }

    def setup() {
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId1 = utils.createDomain()
        def domainId2 = utils.createDomain()
        (identityAdmin1, userAdmin1Domain1, userManage1Domain1, defaultUser1Domain1) = utils.createUsers(domainId1)
        (userAdmin2Domain1, userManage2Domain1, defaultUser2Domain1) = createUsersInDomain(identityAdmin1, domainId1)
        (identityAdmin2, userAdmin1Domain2, userManage1Domain2, defaultUser1Domain2) = utils.createUsers(domainId2)
        (userAdmin2Domain2, userManage2Domain2, defaultUser2Domain2) = createUsersInDomain(identityAdmin2, domainId2)
        serviceAdmin1 = utils.getUserByName(SERVICE_ADMIN_USERNAME, utils.getServiceAdminToken())
        serviceAdmin2 = utils.getUserByName(SERVICE_ADMIN_2_USERNAME, utils.getServiceAdminToken())
        serviceAdmin1Token = utils.getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        serviceAdmin2Token = utils.getToken(SERVICE_ADMIN_2_USERNAME, SERVICE_ADMIN_2_PASSWORD)
    }

    @Override
    public void doCleanupSpec() {
        stopGrizzly();
    }

    void cleanup() {
        cleanupAndDeleteUsers(defaultUser1Domain1, defaultUser2Domain1, userManage1Domain1, userManage2Domain1, userAdmin1Domain1, userAdmin2Domain1, defaultUser1Domain2, defaultUser2Domain2, userManage1Domain2, userManage2Domain2, userAdmin1Domain2, userAdmin2Domain2, identityAdmin1, identityAdmin2)
        cleanupMfa(serviceAdmin1, serviceAdmin1Token)
        cleanupMfa(serviceAdmin2, serviceAdmin2Token)
    }

    @Unroll
    def "verify disable mfa security for #tokenType"() {
        given:
        def token
        def self
        switch (tokenType) {
            case TokenType.SERVICE_ADMIN:
                self = serviceAdmin2
                token = serviceAdmin2Token
                break
            case TokenType.IDENTITY_ADMIN:
                self = identityAdmin2
                token = utils.getToken(self.username)
                break
            case TokenType.USER_ADMIN:
                self = userAdmin1Domain2
                token = utils.getToken(self.username)
                break
            case TokenType.USER_MANAGE:
                self = userManage1Domain2
                token = utils.getToken(self.username)
                break
            case TokenType.DEFAULT_USER:
                self = defaultUser1Domain2
                token = utils.getToken(self.username)
                break
        }
        MultiFactor settings = v2Factory.createMultiFactorSettings(false)

        when: "disable mfa on service admin"
        def response = cloud20.updateMultiFactorSettings(token, serviceAdmin1.id, settings)

        then:
        response.getStatus() == serviceAdminResponse

        when: "disable mfa on identity admin"
        response = cloud20.updateMultiFactorSettings(token, identityAdmin1.id, settings)

        then:
        response.getStatus() == identityAdminResponse

        when: "disable mfa on user admin DIFFERENT DOMAIN"
        response = cloud20.updateMultiFactorSettings(token, userAdmin1Domain1.id, settings)

        then:
        response.getStatus() == userAdminDifDomainResponse

        when: "disable mfa on user admin SAME DOMAIN"
        response = cloud20.updateMultiFactorSettings(token, userAdmin2Domain2.id, settings)

        then:
        response.getStatus() == userAdminSameDomainResponse

        when: "disable mfa on user manage DIFFERENT DOMAIN"
        response = cloud20.updateMultiFactorSettings(token, userManage1Domain1.id, settings)

        then:
        response.getStatus() == userManageDifDomainResponse

        when: "disable mfa on user manage SAME DOMAIN"
        response = cloud20.updateMultiFactorSettings(token, userManage2Domain2.id, settings)

        then:
        response.getStatus() == userManageSameDomainResponse

        when: "disable mfa on default user DIFFERENT DOMAIN"
        response = cloud20.updateMultiFactorSettings(token, defaultUser1Domain1.id, settings)

        then:
        response.getStatus() == defaultUserDifDomainResponse

        when: "disable mfa on default user SAME DOMAIN"
        response = cloud20.updateMultiFactorSettings(token, defaultUser2Domain2.id, settings)

        then:
        response.getStatus() == defaultUserSameDomainResponse

        when: "disable mfa on self"
        response = cloud20.updateMultiFactorSettings(token, self.id, settings)

        then:
        response.getStatus() == selfResponse

        where:
        tokenType                | serviceAdminResponse  | identityAdminResponse | userAdminDifDomainResponse | userAdminSameDomainResponse | userManageDifDomainResponse  | userManageSameDomainResponse  | defaultUserDifDomainResponse | defaultUserSameDomainResponse | selfResponse
        TokenType.SERVICE_ADMIN  | 403                   | 204                   | 204                        | 204                         | 204                          | 204                           | 204                          | 204                           | 204
        TokenType.IDENTITY_ADMIN | 403                   | 403                   | 204                        | 204                         | 204                          | 204                           | 204                          | 204                           | 204
        TokenType.USER_ADMIN     | 403                   | 403                   | 403                        | 403                         | 403                          | 204                           | 403                          | 204                           | 204
        TokenType.USER_MANAGE    | 403                   | 403                   | 403                        | 403                         | 403                          | 403                           | 403                          | 204                           | 204
        TokenType.DEFAULT_USER   | 403                   | 403                   | 403                        | 403                         | 403                          | 403                           | 403                          | 403                           | 204
    }

    @Unroll
    def "verify enable mfa security for #tokenType"() {
        given:
        def self
        switch (tokenType) {
            case TokenType.SERVICE_ADMIN:
                self = serviceAdmin2
                break
            case TokenType.IDENTITY_ADMIN:
                self = identityAdmin2
                break
            case TokenType.USER_ADMIN:
                self = userAdmin1Domain2
                break
            case TokenType.USER_MANAGE:
                self = userManage1Domain2
                break
            case TokenType.DEFAULT_USER:
                self = defaultUser1Domain2
                break
        }
        MultiFactor settings = v2Factory.createMultiFactorSettings(false)

        when: "enable mfa on service admin"
        setupMfaForUser(serviceAdmin1)
        def response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), serviceAdmin1.id, settings)

        then:
        response.getStatus() == serviceAdminResponse

        when: "enable mfa on identity admin"
        setupMfaForUser(identityAdmin1)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), identityAdmin1.id, settings)

        then:
        response.getStatus() == identityAdminResponse

        when: "enable mfa on user admin DIFFERENT DOMAIN"
        setupMfaForUser(userAdmin1Domain1)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), userAdmin1Domain1.id, settings)

        then:
        response.getStatus() == userAdminDifDomainResponse

        when: "enable mfa on user admin SAME DOMAIN"
        setupMfaForUser(userAdmin2Domain2)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), userAdmin2Domain2.id, settings)

        then:
        response.getStatus() == userAdminSameDomainResponse

        when: "enable mfa on user manage DIFFERENT DOMAIN"
        setupMfaForUser(userManage1Domain1)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), userManage1Domain1.id, settings)

        then:
        response.getStatus() == userManageDifDomainResponse

        when: "enable mfa on user manage SAME DOMAIN"
        setupMfaForUser(userManage2Domain2)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), userManage2Domain2.id, settings)

        then:
        response.getStatus() == userManageSameDomainResponse

        when: "enable mfa on default user DIFFERENT DOMAIN"
        setupMfaForUser(defaultUser1Domain1)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), defaultUser1Domain1.id, settings)

        then:
        response.getStatus() == defaultUserDifDomainResponse

        when: "enable mfa on default user SAME DOMAIN"
        setupMfaForUser(defaultUser2Domain2)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), defaultUser2Domain2.id, settings)

        then:
        response.getStatus() == defaultUserSameDomainResponse

        when: "enable mfa on self"
        setupMfaForUser(self)
        response = cloud20.updateMultiFactorSettings(resetAndGetToken(self), self.id, settings)

        then:
        response.getStatus() == selfResponse

        where:
        tokenType                | serviceAdminResponse  | identityAdminResponse | userAdminDifDomainResponse | userAdminSameDomainResponse | userManageDifDomainResponse  | userManageSameDomainResponse  | defaultUserDifDomainResponse | defaultUserSameDomainResponse | selfResponse
        TokenType.SERVICE_ADMIN  | 403                   | 204                   | 204                        | 204                         | 204                          | 204                           | 204                          | 204                           | 204
        TokenType.IDENTITY_ADMIN | 403                   | 403                   | 204                        | 204                         | 204                          | 204                           | 204                          | 204                           | 204
        TokenType.USER_ADMIN     | 403                   | 403                   | 403                        | 403                         | 403                          | 204                           | 403                          | 204                           | 204
        TokenType.USER_MANAGE    | 403                   | 403                   | 403                        | 403                         | 403                          | 403                           | 403                          | 204                           | 204
        TokenType.DEFAULT_USER   | 403                   | 403                   | 403                        | 403                         | 403                          | 403                           | 403                          | 403                           | 204
    }

    def void cleanupAndDeleteUsers(... users) {
        for(user in users) {
            cleanupAndDeleteUser(user)
        }
    }

    def void cleanupAndDeleteUser(user) {
        def token = resetAndGetToken(user)
        cleanupMfa(user, token)
        utils.deleteUser(user)
    }

    def void setupMfaForUser(user) {
        def repoUser = userRepository.getUserById(user.id)
        if(repoUser.multiFactorMobilePhoneRsId != null) {
            def token = resetAndGetToken(user)
            token = token == null ? utils.getToken(user.username) : token
            def responsePhone = utils.addPhone(token, user.id)
            utils.sendVerificationCodeToPhone(token, user.id, responsePhone.id)
            def constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
            utils.verifyPhone(token, user.id, responsePhone.id, constantVerificationCode)
        }
    }

    def void resetTokenExpiration(token) {
        def scopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(token)
        Date now = new Date()
        Date future = new Date(now.year + 1, now.month, now.day)
        scopeAccess.setAccessTokenExp(future)
        scopeAccessRepository.updateScopeAccess(scopeAccess)
    }

    def cleanupMfa(user, token) {
        user = utils.getUserById(user.id)
        if(user.multiFactorEnabled) {
            def phoneId = utils.listDevices(user, token).mobilePhone.id[0]
            utils.deleteMultiFactor(token, user.id)
            mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(phoneId))
        }
    }
    
    def createUsersInDomain(identityAdmin, domainId) {
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def userAdmin = utils.createUser(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId)
        def userAdminToken = utils.getToken(userAdmin.username, DEFAULT_PASSWORD)

        def userManage = utils.createUser(userAdminToken, testUtils.getRandomUUID("userManage"), domainId)
        utils.addRoleToUser(userManage, USER_MANAGE_ROLE_ID)

        def defaultUser = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        return [userAdmin, userManage, defaultUser]
    }

    def resetAndGetToken(user) {
        user = userRepository.getUserById(user.id)
        def token = scopeAccessRepository.getMostRecentScopeAccessForUser(user)
        resetTokenExpiration(token.accessTokenString)
        token = scopeAccessRepository.getMostRecentScopeAccessForUser(user)
        return token.accessTokenString
    }

}
