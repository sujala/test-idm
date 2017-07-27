package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultAuthorizationServiceIntegrationTest extends Specification {

    @Autowired
    DefaultAuthorizationService service

    @Autowired
    private IdentityConfig identityConfig

    @Shared def entityFactory = new EntityFactory()

    def "Service loads all identity roles into name map on construction"() {
        given:
        List<String> minRoleNamesExpected = getExpectedRoles()

        expect:
        service.identityRoleNameToRoleMap.size() > 0
        for (String expectedRoleName : minRoleNamesExpected) {
            service.identityRoleNameToRoleMap.find {it.key == expectedRoleName && it.key == it.value.name} != null
        }

        //only loads "identity:" roles, rcn:admin and the racker role
        service.identityRoleNameToRoleMap.find {!(it.key.startsWith(GlobalConstants.IDENTITY_ROLE_PREFIX)
                || it.key.equals(GlobalConstants.ROLE_NAME_RACKER)
                || it.key.equals(IdentityRole.RCN_ADMIN.roleName))} == null
    }

    def "Service loads all identity roles into id map on construction"() {
        given:
        List<String> minRoleNamesExpected = getExpectedRoles()

        expect:
        service.identityRoleIdToRoleMap.size() > 0
        for (String expectedRoleName : minRoleNamesExpected) {
            service.identityRoleIdToRoleMap.find {it.value.name == expectedRoleName && it.key == it.value.id} != null
        }

        //only loads "identity:" roles, rcn:admin and the racker role
        service.identityRoleNameToRoleMap.find {!(it.value.name.startsWith(GlobalConstants.IDENTITY_ROLE_PREFIX)
                || it.value.name.equals(GlobalConstants.ROLE_NAME_RACKER)
                || it.key.equals(IdentityRole.RCN_ADMIN.roleName))} == null
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

    def "Getting identity user type only recognizes user type roles"() {
        ImmutableClientRole uaRole = service.identityRoleNameToRoleMap.get(identityConfig.getStaticConfig().getIdentityUserAdminRoleName())
        ImmutableClientRole umRole = service.identityRoleNameToRoleMap.get(identityConfig.getStaticConfig().getIdentityUserManagerRoleName())

        List<TenantRole> tenantRoles = [createTenantRoleForClientRole(umRole), createTenantRoleForClientRole(uaRole)]

        expect:
        service.getIdentityTypeRoleAsEnum(tenantRoles) == IdentityUserTypeEnum.USER_ADMIN
    }

    def "Getting identity user type returns null when no identity type role provided"() {
        ImmutableClientRole rackerRole = service.identityRoleNameToRoleMap.get(GlobalConstants.ROLE_NAME_RACKER)

        List<TenantRole> tenantRoles = [createTenantRoleForClientRole(rackerRole)]

        expect:
        service.getIdentityTypeRoleAsEnum(tenantRoles) == null
    }


    def List<String> getExpectedRoles() {
        return Arrays.asList(identityConfig.getStaticConfig().getIdentityServiceAdminRoleName()
                , identityConfig.getStaticConfig().getIdentityIdentityAdminRoleName()
                , identityConfig.getStaticConfig().getIdentityUserAdminRoleName()
                , identityConfig.getStaticConfig().getIdentityUserManagerRoleName()
                , identityConfig.getStaticConfig().getIdentityDefaultUserRoleName()
                , GlobalConstants.ROLE_NAME_RACKER);
    }

    def TenantRole createTenantRoleForClientRole(ImmutableClientRole cr) {
        entityFactory.createTenantRole(cr.name, cr.roleType).with {
            it.clientId = cr.clientId
            it.roleRsId = cr.id
            it
        }
    }

}
