package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Unroll
import testHelpers.MultiStageTask
import testHelpers.MultiStageTaskFactory
import testHelpers.RootIntegrationTest

/**
 */
class DefaultIdentityUserServiceIntegrationTest extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass())

    @Autowired DefaultIdentityUserService identityUserService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao saRepository

    @Autowired DefaultUserService userService

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Unroll
    def "Retrieving service catalog info uses cached roles based on reloadable property feature.use.cached.client.roles.for.service.catalog: #useCachedRoles"() {
        given:
        // If either of these are 0 then cacheing is disabled altogether and this test would be pointless
        assert identityConfig.getStaticConfig().getClientRoleByIdCacheTtl().toMillis() > 0
        assert identityConfig.getStaticConfig().getClientRoleByIdCacheSize() > 0

        // Without performant catalog, doesn't matter what cache role feature is set to
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_PROP, useCachedRoles)

        // Create user to test with
        def domainId = utils.createDomain()
        def user, users1
        (user, users1) = utils.createUserAdmin(domainId)
        def userEntity = userService.getUserById(user.id)

        def originalRole = utils.createRole()
        utils.addRoleToUser(user, originalRole.id)

        when: "Retrieve catalog"
        ServiceCatalogInfo scInfo = identityUserService.getServiceCatalogInfo(userEntity)


        then: "Use has role"
        scInfo.userTenantRoles.find {it.name == originalRole.name} != null

        when: "Change role and auth again"
        ClientRole updatedRole = applicationRoleDao.getClientRole(originalRole.id)
        updatedRole.setName("update_" + RandomStringUtils.randomAlphabetic(10))
        applicationRoleDao.updateClientRole(updatedRole)
        scInfo = identityUserService.getServiceCatalogInfo(userEntity)

        then:
        if (useCachedRoles) {
            // The role name should be the old value as the client role was cached during initial auth
            assert scInfo.userTenantRoles.find {it.name == originalRole.name} != null
            assert scInfo.userTenantRoles.find {it.name == updatedRole.name} == null
        } else {
            // The role name should be the new value as the client role is always retreived from backend
            assert scInfo.userTenantRoles.find {it.name == originalRole.name} == null
            assert scInfo.userTenantRoles.find {it.name == updatedRole.name} != null
        }

        when: "switch using cached to opposite value and update client role"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_USE_CACHED_CLIENT_ROLES_FOR_SERVICE_CATALOG_PROP, !useCachedRoles)

        // Auth after making setting change to populate caches as appropriate
        scInfo = identityUserService.getServiceCatalogInfo(userEntity)

        // Change the role name for a final time
        ClientRole finalUpdatedRole = applicationRoleDao.getClientRole(originalRole.id)
        finalUpdatedRole.setName("final_" + RandomStringUtils.randomAlphabetic(10))
        applicationRoleDao.updateClientRole(finalUpdatedRole)

        // Re-auth now that the role has changed
        scInfo = identityUserService.getServiceCatalogInfo(userEntity)

        then:
        if (useCachedRoles) {
            // Original setting was to use cached roles, now we're not, so should return the final updated value
            assert scInfo.userTenantRoles.find {it.name == originalRole.name} == null
            assert scInfo.userTenantRoles.find {it.name == updatedRole.name} == null
            assert scInfo.userTenantRoles.find {it.name == finalUpdatedRole.name} != null
        } else {
            /*
             Original setting was not to use cached roles, now we are. We auth after enabling, but before updating the
             role for the final time so final auth should return the cached value after 2nd update
              */
            assert scInfo.userTenantRoles.find {it.name == originalRole.name} == null
            assert scInfo.userTenantRoles.find {it.name == updatedRole.name} != null
            assert scInfo.userTenantRoles.find {it.name == finalUpdatedRole.name} == null
        }

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteRoleQuietly(originalRole)

        where:
        useCachedRoles | _
        true | _
        false | _
    }
}
