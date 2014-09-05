package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.impl.LdapDomainRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_OK

@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class AuthMfaEnforcementIntegrationTest extends RootIntegrationTest {

    def static OFF_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagOff.xml"
    def static FULL_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagFull.xml"
    def static BETA_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagBeta.xml"

    @Autowired LdapScopeAccessRepository scopeAccessRepository
    @Autowired LdapUserRepository userRepository
    @Autowired LdapDomainRepository domainRepository

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

    def "If Mulit-Factor is not enabled for a user then normal auth is allowed"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        setFlagSettings(BETA_SETTINGS_FILE)

        when:
        def response = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)

        then:
        response.status == SC_OK

        cleanup:
        setFlagSettings(FULL_SETTINGS_FILE)
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
        domainRepository.updateObjectAsIs(domain)

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
        domainRepository.updateObjectAsIs(domain)

        when:
        def response = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def setFlagSettings(String flagSettingsFile) {
        switch (flagSettingsFile) {
            case OFF_SETTINGS_FILE:
                staticIdmConfiguration.setProperty("multifactor.services.enabled", false)
                staticIdmConfiguration.setProperty("multifactor.beta.enabled", false)
                break;
            case FULL_SETTINGS_FILE:
                staticIdmConfiguration.setProperty("multifactor.services.enabled", true)
                staticIdmConfiguration.setProperty("multifactor.beta.enabled", false)
                break;
            case BETA_SETTINGS_FILE:
                staticIdmConfiguration.setProperty("multifactor.services.enabled", true)
                staticIdmConfiguration.setProperty("multifactor.beta.enabled", true)
                break;
        }
    }
}
