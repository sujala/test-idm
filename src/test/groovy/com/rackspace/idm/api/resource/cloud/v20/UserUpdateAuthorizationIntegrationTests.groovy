package com.rackspace.idm.api.resource.cloud.v20
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
/**
 * Verify the operations that users can perform to update users (own account + others). Other test classes, in particular Cloud20IntegrationTest, perform some of these operations as well.
 * The goal is to migrate all integration tests regarding updating a user into this class.
 */
class UserUpdateAuthorizationIntegrationTests extends RootConcurrentIntegrationTest {

    @Autowired DefaultAuthorizationService authorizationService

    def "identity admin can retrieve service admin user"() {
        when:
        def final serviceAdminUser = cloud20.getUserByName(specificationIdentityAdminToken, SERVICE_ADMIN_USERNAME).getEntity(org.openstack.docs.identity.api.v2.User).value

        then:
        serviceAdminUser instanceof User
    }

    /**
     * This test authenticates as an identity admin and attempts to update the email address of a service admin. This should not be allowed.
     * @return
     */
    def "identity admin can not update service admin information"() {
        setup:
        User serviceAdminUser = cloud20.getUserByName(specificationIdentityAdminToken, SERVICE_ADMIN_USERNAME).getEntity(org.openstack.docs.identity.api.v2.User).value
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

    def "user manage role can update self"() {
        setup:
        User userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)
        User defaultUser = createDefaultUser(userAdminToken)
        def defaultUserToken = authenticate(defaultUser.username)

        ClientRole userManageRole = authorizationService.getCloudUserManagedRole()
        cloud20.addUserRole(userAdminToken, defaultUser.getId(), userManageRole.getId())
        defaultUser.setEmail("anythingelse@anything.com")

        when:
        def updateResult = cloud20.updateUser(defaultUserToken, defaultUser.getId(), defaultUser)

        then:
        updateResult.status == HttpStatus.OK.value()

        cleanup:
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userAdmin)
    }

    def "user manage role CANNOT update another user manage role"() {
        setup:
        User userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        User defaultUser = createDefaultUser(userAdminToken)
        def defaultUserToken = authenticate(defaultUser.username)

        User defaultUser2 = createDefaultUser(userAdminToken)

        ClientRole userManageRole = authorizationService.getCloudUserManagedRole()
        cloud20.addUserRole(userAdminToken, defaultUser.getId(), userManageRole.getId())
        cloud20.addUserRole(userAdminToken, defaultUser2.getId(), userManageRole.getId())

        defaultUser2.setEmail("anythingelse@anything.com")

        when:
        def updateResult = cloud20.updateUser(defaultUserToken, defaultUser2.getId(), defaultUser2)

        then:
        updateResult.status == HttpStatus.FORBIDDEN.value()

        cleanup:
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(defaultUser2)
        deleteUserQuietly(userAdmin)
    }

}
