package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.http.HttpStatus
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
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, IdentityFault, HttpStatus.SC_SERVICE_UNAVAILABLE, ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE)

        where:
        operation | response
        "add client role" | cloud20.createRole(specificationServiceAdminToken, v2Factory.createRole())
        "delete client role" | cloud20.deleteRole(specificationServiceAdminToken, "1234")
    }

    /**
     * Application means same thing as Service
     * @return
     */
    @Unroll
    def "Application :: Can not #operation during migration"() {
        expect: "not allowed"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, IdentityFault, HttpStatus.SC_SERVICE_UNAVAILABLE, ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE)

        where:
        operation | response
        "add application" | cloud20.createService(specificationServiceAdminToken, v1Factory.createService(null, "newService"))
        "delete application" | cloud20.deleteService(specificationServiceAdminToken, "1234")
    }

    @Unroll
    def "Capabilities :: Can not #operation during migration"() {
        expect: "not allowed"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, IdentityFault, HttpStatus.SC_SERVICE_UNAVAILABLE, ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE)

        where:
        operation | response
        "update capabilities" | cloud20.updateCapabilities(specificationServiceAdminToken, "1", "2", v1Factory.createCapabilities())
        "delete capabilities" | cloud20.deleteCapabilities(specificationServiceAdminToken, "1", "2")
    }

    @Unroll
    def "Groups :: Can not #operation during migration"() {
        expect: "not allowed"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, IdentityFault, HttpStatus.SC_SERVICE_UNAVAILABLE, ErrorCodes.ERROR_CODE_MIGRATION_READ_ONLY_ENTITY_CODE)

        where:
        operation | response
        "add group" | cloud20.createGroup(specificationServiceAdminToken, v2Factory.createGroup("blah"))
        "update group" | cloud20.updateGroup(specificationServiceAdminToken, "1", v2Factory.createGroup("blah"))
        "delete group" | cloud20.deleteGroup(specificationServiceAdminToken, "1")
    }
}
