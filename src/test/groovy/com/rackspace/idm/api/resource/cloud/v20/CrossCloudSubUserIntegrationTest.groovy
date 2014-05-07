package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

/**
 * This tests useradmins from Cloud A being able to create subusers on Cloud B.
 */
class CrossCloudSubUserIntegrationTest extends RootIntegrationTest {
    @Shared def identityAdminToken

    @Autowired def UserService userService

    static def US_CLOUD_FILE = "classpath:com/rackspace/idm/api/resource/cloud/v20/US_cloud.xml"
    static def UK_CLOUD_FILE = "classpath:com/rackspace/idm/api/resource/cloud/v20/UK_cloud.xml"

    def setupSpec() {}

    def cleanupSpec() {
        stopGrizzly()
    }

    def setup() {
        identityAdminToken = utils.getIdentityAdminToken()
    }

    def "UK admin can create UK subuser on US cloud"() {
        setup:
        //spin up the uk cloud
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                UK_CLOUD_FILE)

        //create a 1.1, 2.0 legacy, and 2.0 "one-user" user-admin
        def randomId = testUtils.getRandomUUID()
        def v11User = create11UserAdmin(randomId)
        def v20LegacyUser = create20LegacyUserAdmin(randomId)
        def v20OneUser = create20OneUserAdmin(randomId)

        //switch to us cloud
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                US_CLOUD_FILE)

        when: "1.1 UK useradmin creating subuser in US without specified region"
        User v11SubUser = createSubUserWithApiAuth(v11User, null)

        then: "sub user is created"
        v11SubUser != null
        v11SubUser.getRegion() == v11User.getRegion()

        when: "2.0 legacy UK useradmin creating subuser in US without specified region"
        User v20LegacySubUser = createSubUserWithPasswordAuth(v20LegacyUser, null)

        then: "sub user is created"
        v20LegacySubUser != null
        v20LegacySubUser.getRegion() == v11User.getRegion()

        when: "2.0 one user UK useradmin creating subuser in US without specified region"
        User v20OneSubUser = createSubUserWithPasswordAuth(v20OneUser, null)

        then: "sub user is created"
        v20OneSubUser != null
        v20OneSubUser.getRegion() == v11User.getRegion()

        cleanup:
        cleanUpAfterUserQuietly(v20OneSubUser)
        cleanUpAfterUserQuietly(v20LegacySubUser)
        cleanUpAfterUserQuietly(v11SubUser)
        cleanUpAfterUserQuietly(v20OneUser)
        cleanUpAfterUserQuietly(v20LegacyUser)
        cleanUpAfterUserQuietly(v11User)
    }

    def "US admin can create US subuser on UK cloud"() {
        setup:
        //spin up the us cloud
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                US_CLOUD_FILE)

        //create a 1.1, 2.0 legacy, and 2.0 "one-user" user-admin
        def randomId = testUtils.getRandomUUID()
        def v11User = create11UserAdmin(randomId)
        def v20LegacyUser = create20LegacyUserAdmin(randomId)
        def v20OneUser = create20OneUserAdmin(randomId)

        //switch to uk cloud
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                UK_CLOUD_FILE)

        when: "1.1 US useradmin creating subuser in UK without specified region"
        User v11SubUser = createSubUserWithApiAuth(v11User, null)

        then: "sub user is created"
        v11SubUser != null
        v11SubUser.getRegion() == v11User.getRegion()

        when: "2.0 legacy US useradmin creating subuser in UK without specified region"
        User v20LegacySubUser = createSubUserWithPasswordAuth(v20LegacyUser, null)

        then: "sub user is created"
        v20LegacySubUser != null
        v20LegacySubUser.getRegion() == v11User.getRegion()

        when: "2.0 one user US useradmin creating subuser in UK without specified region"
        User v20OneSubUser = createSubUserWithPasswordAuth(v20OneUser, null)

        then: "sub user is created"
        v20OneSubUser != null
        v20OneSubUser.getRegion() == v11User.getRegion()

        cleanup:
        cleanUpAfterUserQuietly(v20OneSubUser)
        cleanUpAfterUserQuietly(v20LegacySubUser)
        cleanUpAfterUserQuietly(v11SubUser)
        cleanUpAfterUserQuietly(v20OneUser)
        cleanUpAfterUserQuietly(v20LegacyUser)
        cleanUpAfterUserQuietly(v11User)
    }

    def "US admin can create US subuser on US cloud"() {
        setup:
        //spin up the us cloud
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                US_CLOUD_FILE)

        //create a 1.1, 2.0 legacy, and 2.0 "one-user" user-admin
        def randomId = testUtils.getRandomUUID()
        def v11User = create11UserAdmin(randomId)
        def v20LegacyUser = create20LegacyUserAdmin(randomId)
        def v20OneUser = create20OneUserAdmin(randomId)

        when: "1.1 US useradmin creating subuser in US without specified region"
        User v11SubUser = createSubUserWithApiAuth(v11User, null)

        then: "sub user is created"
        v11SubUser != null
        v11SubUser.getRegion() == v11User.getRegion()

        when: "2.0 legacy US useradmin creating subuser in US without specified region"
        User v20LegacySubUser = createSubUserWithPasswordAuth(v20LegacyUser, null)

        then: "sub user is created"
        v20LegacySubUser != null
        v20LegacySubUser.getRegion() == v11User.getRegion()

        when: "2.0 one user US useradmin creating subuser in US without specified region"
        User v20OneSubUser = createSubUserWithPasswordAuth(v20OneUser, null)

        then: "sub user is created"
        v20OneSubUser != null
        v20OneSubUser.getRegion() == v11User.getRegion()

        cleanup:
        cleanUpAfterUserQuietly(v20OneSubUser)
        cleanUpAfterUserQuietly(v20LegacySubUser)
        cleanUpAfterUserQuietly(v11SubUser)
        cleanUpAfterUserQuietly(v20OneUser)
        cleanUpAfterUserQuietly(v20LegacyUser)
        cleanUpAfterUserQuietly(v11User)
    }

    def "UK admin can create UK subuser on UK cloud"() {
        setup:
        //spin up the us cloud
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                UK_CLOUD_FILE)

        //create a 1.1, 2.0 legacy, and 2.0 "one-user" user-admin
        def randomId = testUtils.getRandomUUID()
        def v11User = create11UserAdmin(randomId)
        def v20LegacyUser = create20LegacyUserAdmin(randomId)
        def v20OneUser = create20OneUserAdmin(randomId)

        when: "1.1 UK useradmin creating subuser in UK without specified region"
        User v11SubUser = createSubUserWithApiAuth(v11User, null)

        then: "sub user is created"
        v11SubUser != null
        v11SubUser.getRegion() == v11User.getRegion()

        when: "2.0 legacy UK useradmin creating subuser in UK without specified region"
        User v20LegacySubUser = createSubUserWithPasswordAuth(v20LegacyUser, null)

        then: "sub user is created"
        v20LegacySubUser != null
        v20LegacySubUser.getRegion() == v11User.getRegion()

        when: "2.0 one user UK useradmin creating subuser in UK without specified region"
        User v20OneSubUser = createSubUserWithPasswordAuth(v20OneUser, null)

        then: "sub user is created"
        v20OneSubUser != null
        v20OneSubUser.getRegion() == v11User.getRegion()

        cleanup:
        cleanUpAfterUserQuietly(v20OneSubUser)
        cleanUpAfterUserQuietly(v20LegacySubUser)
        cleanUpAfterUserQuietly(v11SubUser)
        cleanUpAfterUserQuietly(v20OneUser)
        cleanUpAfterUserQuietly(v20LegacyUser)
        cleanUpAfterUserQuietly(v11User)
    }

    def User createSubUserWithApiAuth(User userAdmin, String subUserRegion) {
        def userAdminToken = cloud20.authenticateApiKey(userAdmin.username, Constants.DEFAULT_API_KEY).getEntity(AuthenticateResponse).value.token.id
        return createSubUser(userAdmin, userAdminToken, subUserRegion)
    }

    def User createSubUserWithPasswordAuth(User userAdmin, String subUserRegion) {
        def userAdminToken = cloud20.authenticatePassword(userAdmin.username, Constants.DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        return createSubUser(userAdmin, userAdminToken, subUserRegion)
    }

    def User createSubUser(User userAdmin, String userAdminToken, String subUserRegion) {
        def subUserName = userAdmin.username + "SubUser"
        def subUserResponse = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(subUserName, "displayName", "someEmail@rackspace.com", true, subUserRegion, null, Constants.DEFAULT_PASSWORD))
        assert subUserResponse.status == HttpStatus.SC_CREATED

        def subUser = subUserResponse.getEntity(org.openstack.docs.identity.api.v2.User).value
        return userService.getUser(subUserName)
    }

    def User create11UserAdmin(String userNameSuffix) {
        def v11Username = "v11Username" + userNameSuffix
        def v11DomainId = utils.createDomain()

        def user = v1Factory.createUser(v11Username, Constants.DEFAULT_API_KEY, Integer.parseInt(v11DomainId))
        cloud11.createUser(user)
        return userService.getUser(v11Username)
    }

    def User create20LegacyUserAdmin(String userNameSuffix) {
        def v20LegacyUsername = "v20LegacyUsername" + userNameSuffix
        def v20LegacyDomainId = utils.createDomain()

        def user = v2Factory.createUser(v20LegacyUsername, "displayName", "testemail@rackspace.com", true, null, v20LegacyDomainId, Constants.DEFAULT_PASSWORD)
        cloud20.createUser(identityAdminToken, user)
        return userService.getUser(v20LegacyUsername)
    }

    def User create20OneUserAdmin(String userNameSuffix) {
        def v20Username = "v20OneUsername" + userNameSuffix
        def v20DomainId = utils.createDomain()

        def user = v2Factory.createUser(v20Username, "displayName", "testemail@rackspace.com", true, null, v20DomainId, Constants.DEFAULT_PASSWORD)
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA
        cloud20.createUser(identityAdminToken, user)
        return userService.getUser(v20Username)
    }

    /**
     * Will quietly exit if any error deleting the user, tenants, or domain is encountered. Will exit after first error.
     *
     * @param user
     */
    def void cleanUpAfterUserQuietly(User user) {
        try {
            if (user != null && user.id != null) {
                cloud20.deleteUser(identityAdminToken, user.id)
                if (StringUtils.isNotBlank(user.getDomainId())) {
                    def tenants = cloud20.getDomainTenants(identityAdminToken, user.getDomainId()).getEntity(Tenants).value
                    for (Tenant tenant : tenants.tenant) {
                        cloud20.deleteTenant(identityAdminToken, tenant.id)
                    }
                    cloud20.deleteDomain(identityAdminToken, user.getDomainId())
                }
            }
        }
        catch (Exception ex) {
            //eat
        }
    }

}
