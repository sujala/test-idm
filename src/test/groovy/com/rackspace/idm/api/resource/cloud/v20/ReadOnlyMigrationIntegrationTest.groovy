package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.IdentityFault
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

class ReadOnlyMigrationIntegrationTest extends RootIntegrationTest {
    @Shared
    def specificationServiceAdminToken

    def setupSpec() {
        specificationServiceAdminToken = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MIGRATION_READ_ONLY_MODE_ENABLED_PROP, true)
    }

    @Unroll
    def "Client Role :: Can not #operation during migration"() {
        expect: "not allowed"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, IdentityFault, org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE, ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE)

        where:
        operation | response
        "add client role" | cloud20.createRole(specificationServiceAdminToken, v2Factory.createRole())
        "delete client role" | cloud20.deleteRole(specificationServiceAdminToken, "1234")
    }
}
