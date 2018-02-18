package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.IdentityUserService
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class DefaultTenantServiceIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    DefaultTenantService tenantService

    def "getEffectiveGlobalRolesForUser: Returns fed user's global roles"() {
        def userAdmin = utils.createCloudAccount()
        def fedUser = utils.createFederatedUser(userAdmin.domainId)

        FederatedUser fedEntity = identityUserService.getFederatedUserById(fedUser.id)
        assert fedEntity != null

        when: "get roles for fed user"
        List<TenantRole> fedGlobalRoles = tenantService.getEffectiveGlobalRolesForUserIncludeRcnRoles(fedEntity)

        then: "has default role"
        fedGlobalRoles.size() == 1
        fedGlobalRoles.find {it.roleRsId == Constants.DEFAULT_USER_ROLE_ID}
    }
}
