package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserService
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import static org.apache.http.HttpStatus.*

class FeedsV3UserEventIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityUserService identityUserService

    @Shared
    def identityAdmin

    @Shared
    def identityAdminToken

    def setupSpec() {
        resetCloudFeedsMock()

        // User version 3 user product schema
        reloadableConfiguration.setProperty(IdentityConfig.FEEDS_USER_PRODUCT_SCHEMA_VERSION_PROP, 3)
    }

    def cleanupSpec() {
        resetCloudFeedsMock()
        reloadableConfiguration.reset()
    }

    def setup() {
        identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, IDENTITY_RS_TENANT_ADMIN_ROLE_ID)
        identityAdminToken = utils.getToken(identityAdmin.username)
    }

    def cleanup() {
        utils.deleteUserQuietly(identityAdmin)
    }

    def "verify one user create call sends correct event feeds"() {
        given:
        def username = "testUser" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUser(username, username, "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA

        // Auto generated requestId
        def requestId = UUID.randomUUID().toString()

        when: "create user"
        def response = cloud20.createUser(identityAdminToken, user, MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE, requestId)
        def userEntity = response.getEntity(User).value

        then:
        response.status == SC_CREATED

        and: "verify event feeds"
        def endUser = identityUserService.getEndUserById(userEntity.id)
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.CREATE, FeedsUserStatusEnum.CREATE, requestId),
                VerificationTimes.exactly(1)
        )
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.ROLE, requestId),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteUserQuietly(userEntity)
    }

    def "verify update user call sends correct event feeds"() {
        given:
        def user = utils.createCloudAccount()

        // Auto generated requestId
        def requestId = UUID.randomUUID().toString()

        cloudFeedsMock.reset()

        def userForUpdate = new UserForCreate().with {
            it.contactId = testUtils.getRandomUUID("contactId")
            it
        }

        when: "update user"
        def response = cloud20.updateUser(identityAdminToken, user.id, userForUpdate, MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE, requestId)

        then:
        response.status == SC_OK

        and: "verify event feed"
        def endUser = identityUserService.getEndUserById(user.id)
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.UPDATE, requestId),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteUserQuietly(user)
    }

    def "verify authenticating federated user sends correct feed events"() {
        given:
        def userAdmin = utils.createCloudAccount()

        // Auto generated requestId
        def requestId = UUID.randomUUID().toString()

        // Create federated user
        def fedRequest = utils.createFedRequest(userAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)


        when: "saml auth"
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), APPLICATION_XML, APPLICATION_XML, requestId)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def endUser = identityUserService.getEndUserById(samlAuthResponse.value.user.id)

        then:
        samlResponse.status == SC_OK

        and: "verify event feeds"
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.CREATE, FeedsUserStatusEnum.CREATE, requestId),
                VerificationTimes.exactly(1)
        )
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.ROLE, requestId),
                VerificationTimes.exactly(1)
        )

        when: "saml auth of existing federated user with no changes"
        resetCloudFeedsMock()
        samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), APPLICATION_XML, APPLICATION_XML, requestId)

        then: "assert 200"
        samlResponse.status == SC_OK

        and: "verify that no user event is posted"
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.ROLE, requestId),
                VerificationTimes.exactly(0)
        )
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.CREATE, FeedsUserStatusEnum.CREATE, requestId),
                VerificationTimes.exactly(0)
        )
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.UPDATE, requestId),
                VerificationTimes.exactly(0)
        )

        when: "updating federated user's email"
        resetCloudFeedsMock()
        fedRequest.email = "test@mail.com"
        samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), APPLICATION_XML, APPLICATION_XML, requestId)

        then:
        samlResponse.status == SC_OK

        and: "verify no event feeds"
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.UPDATE, requestId),
                VerificationTimes.exactly(1)
        )

        when: "add role to existing federated user"
        resetCloudFeedsMock()
        fedRequest.roleNames = [ROLE_RBAC1_NAME]
        samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), APPLICATION_XML, APPLICATION_XML, requestId)

        then:
        samlResponse.status == SC_OK

        and: "verify that the user UPDATE event is posted"
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.ROLE, requestId),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteFederatedUserQuietly(endUser.username)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteDomain(userAdmin.domainId)
    }

    def "verify user groups send correct event feeds"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userGroup = utils.createUserGroup(userAdmin.domainId)

        // Retrieve the end user
        def endUser = identityUserService.getEndUserById(userAdmin.id)

        resetCloudFeedsMock()

        when: "adding user to user group"
        def response = cloud20.addUserToUserGroup(identityAdminToken, userAdmin.domainId, userGroup.id, userAdmin.id)

        then:
        response.status == SC_NO_CONTENT

        and: "verify that the user UPDATE event is posted"
        cloudFeedsMock.verify(
                testUtils.createV3UserFeedsRequest(endUser, EventType.UPDATE, FeedsUserStatusEnum.USER_GROUP),
                VerificationTimes.exactly(1)
        )
    }
}
