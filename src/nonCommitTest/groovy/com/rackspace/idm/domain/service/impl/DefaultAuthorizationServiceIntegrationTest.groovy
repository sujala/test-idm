package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.ApplicationService
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
    ApplicationService applicationService

    @Autowired
    private IdentityConfig identityConfig

    @Shared def entityFactory = new EntityFactory()

    def "Getting identity user type returns highest level role"() {
        ImmutableClientRole uaRole = applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.USER_ADMIN.roleName)
        ImmutableClientRole umRole = applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.USER_MANAGER.roleName)

        List<TenantRole> tenantRoles = [createTenantRoleForClientRole(umRole), createTenantRoleForClientRole(uaRole)]

        expect:
        service.getIdentityTypeRoleAsEnum(tenantRoles) == IdentityUserTypeEnum.USER_ADMIN
    }

    def "Getting identity user type returns null when no identity type role provided"() {
        ImmutableClientRole rackerRole = applicationService.getCachedClientRoleByName(GlobalConstants.ROLE_NAME_RACKER)

        List<TenantRole> tenantRoles = [createTenantRoleForClientRole(rackerRole)]

        expect:
        service.getIdentityTypeRoleAsEnum(tenantRoles) == null
    }

    TenantRole createTenantRoleForClientRole(ImmutableClientRole cr) {
        entityFactory.createTenantRole(cr.name, cr.roleType).with {
            it.clientId = cr.clientId
            it.roleRsId = cr.id
            it
        }
    }

}
