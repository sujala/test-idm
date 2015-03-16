package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultAuthorizationServiceIntegrationTest extends Specification {

    @Autowired
    DefaultAuthorizationService service

    @Autowired
    private IdentityConfig identityConfig

    def "Service loads all identity roles into name map on construction"() {
        given:
        List<String> minRoleNamesExpected = getExpectedRoles()

        expect:
        service.identityRoleNameToRoleMap.size() > 0
        for (String expectedRoleName : minRoleNamesExpected) {
            service.identityRoleNameToRoleMap.find {it.key == expectedRoleName && it.key == it.value.name} != null
        }

        //only loads "identity:" roles and the racker role
        service.identityRoleNameToRoleMap.find {!(it.key.startsWith(GlobalConstants.IDENTITY_ROLE_PREFIX)
                || it.key.equals(GlobalConstants.ROLE_NAME_RACKER))} == null
    }

    def "Service loads all identity roles into id map on construction"() {
        given:
        List<String> minRoleNamesExpected = getExpectedRoles()

        expect:
        service.identityRoleIdToRoleMap.size() > 0
        for (String expectedRoleName : minRoleNamesExpected) {
            service.identityRoleIdToRoleMap.find {it.value.name == expectedRoleName && it.key == it.value.id} != null
        }

        //only loads "identity:" roles and the racker role
        service.identityRoleNameToRoleMap.find {!(it.value.name.startsWith(GlobalConstants.IDENTITY_ROLE_PREFIX)
                || it.value.name.equals(GlobalConstants.ROLE_NAME_RACKER))} == null
    }

    def "Service loads cached role variables on construction"() {
        expect:
        service.idmSuperAdminRole != null
        service.cloudServiceAdminRole != null
        service.cloudIdentityAdminRole != null
        service.cloudUserAdminRole != null
        service.cloudUserRole != null
        service.cloudUserManagedRole != null
        service.rackerRole != null
    }

    def List<String> getExpectedRoles() {
        return Arrays.asList(identityConfig.getStaticConfig().getIdentityServiceAdminRoleName()
                , identityConfig.getStaticConfig().getIdentityIdentityAdminRoleName()
                , identityConfig.getStaticConfig().getIdentityUserAdminRoleName()
                , identityConfig.getStaticConfig().getIdentityUserManagerRoleName()
                , identityConfig.getStaticConfig().getIdentityDefaultUserRoleName()
                , identityConfig.getStaticConfig().getIdentityDefaultUserRoleName()
                , identityConfig.getStaticConfig().getMultiFactorBetaRoleName()
                , GlobalConstants.ROLE_NAME_RACKER);
    }
}
