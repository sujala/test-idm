package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.exception.ForbiddenException
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.IdentityFault
import org.openstack.docs.identity.api.v2.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import spock.lang.Ignore
import spock.lang.Shared
import testHelpers.ConcurrentStageTaskRunner
import testHelpers.MultiStageTaskFactory
import testHelpers.RootIntegrationTest

/**
 * Verify the operations that an Identity Admin can perform
 */
class UserUpdateAuthorizationIntegrationTests extends RootConcurrentIntegrationTest {

    @Autowired DefaultAuthorizationService authorizationService

    def "identity admin can retrieve service admin user"() {
        when:
        def final serviceAdminUser = cloud20.getUserByName(specificationIdentityAdminToken, SERVICE_ADMIN_USERNAME).getEntity(org.openstack.docs.identity.api.v2.User)

        then:
        serviceAdminUser instanceof User
    }

    /**
     * This test authenticates as an identity admin and attempts to update the email address of a service admin. This should not be allowed.
     * @return
     */
    def "identity admin can not update service admin information"() {
        setup:
        User serviceAdminUser = cloud20.getUserByName(specificationIdentityAdminToken, SERVICE_ADMIN_USERNAME).getEntity(org.openstack.docs.identity.api.v2.User)
        def originalName = serviceAdminUser.getEmail()

        serviceAdminUser.setEmail("$FEATURE_RANDOM@test.com")

        when:
        def updateResult = cloud20.updateUser(specificationIdentityAdminToken, serviceAdminUser.getId(), serviceAdminUser)

        then:
        updateResult.status == HttpStatus.FORBIDDEN.value()

        cleanup:
        if (updateResult.status != HttpStatus.FORBIDDEN.value()) {
            serviceAdminUser.setEmail(originalName)
            cloud20.updateUser(specificationIdentityAdminToken, serviceAdminUser.getId(), serviceAdminUser)
        }
    }
}
