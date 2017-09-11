package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class DefaultUserGroupCloudServiceRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

    void doSetupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserAdmin.domainId).getEntity(Tenants).value
        sharedUserAdminCloudTenant = tenants.tenant.find {
            it.id == sharedUserAdmin.domainId
        }
        sharedUserAdminFilesTenant = tenants.tenant.find() {
            it.id != sharedUserAdmin.domainId
        }
    }

    @Unroll
    def "add/get group; mediaType = #mediaType"() {
        when:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def response = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType)

        then:
        response.status == HttpStatus.SC_CREATED
        UserGroup created = response.getEntity(UserGroup)

        and:
        created.domainId == group.domainId
        created.id != null
        created.description == group.description
        created.name == group.name

        when:
        def getResponse = cloud20.getUserGroup(sharedIdentityAdminToken, created, mediaType)

        then:
        getResponse.status == HttpStatus.SC_OK
        UserGroup retrievedEntity = getResponse.getEntity(UserGroup)

        and:
        retrievedEntity.domainId == group.domainId
        retrievedEntity.id == created.id
        retrievedEntity.description == group.description
        retrievedEntity.name == group.name

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

}
