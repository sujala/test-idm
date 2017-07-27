package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_OK

class AuthMfaEnforcementIntegrationTest extends RootIntegrationTest {

    def static OFF_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagOff.xml"
    def static FULL_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagFull.xml"
    def static BETA_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagBeta.xml"

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired UserDao userRepository
    @Autowired DomainDao domainRepository

    def "If Mulit-Factor is not enabled for a user then normal auth is allowed"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        def response = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)

        then:
        response.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "If a user's mfa enforcement flag is OPTIONAL then normal auth is allowed"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        User initialUserAdmin = userRepository.getUserById(userAdmin.id)
        initialUserAdmin.userMultiFactorEnforcementLevel = GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL
        userRepository.updateUserAsIs(initialUserAdmin)

        when:
        def response = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)

        then:
        response.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "If a user's mfa enforcement flag is DEFAULT and the user's domain mfa enforcment flag is OPTIONAL then normal auth is allowed"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        User initialUserAdmin = userRepository.getUserById(userAdmin.id)
        initialUserAdmin.userMultiFactorEnforcementLevel = GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT
        userRepository.updateUserAsIs(initialUserAdmin)
        Domain domain = domainRepository.getDomain(domainId)
        domain.domainMultiFactorEnforcementLevel = GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL
        domainRepository.updateDomain(domain)

        when:
        def response = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)

        then:
        response.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "If a user's mfa enforcement flag is REQUIRED then normal auth is forbidden"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        User initialUserAdmin = userRepository.getUserById(userAdmin.id)
        initialUserAdmin.userMultiFactorEnforcementLevel = GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED
        userRepository.updateUserAsIs(initialUserAdmin)

        when:
        def response = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "If a user's mfa enforcement flag is DEFAULT and the user's domain mfa enforcment flag is REQUIRED then normal forbidden"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        User initialUserAdmin = userRepository.getUserById(userAdmin.id)
        initialUserAdmin.userMultiFactorEnforcementLevel = GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT
        userRepository.updateUserAsIs(initialUserAdmin)
        Domain domain = domainRepository.getDomain(domainId)
        domain.domainMultiFactorEnforcementLevel = GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED
        domainRepository.updateDomain(domain)

        when:
        def response = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

}
