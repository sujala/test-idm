package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class RootDelegationAgreementCrudRestIntegrationTest extends RootIntegrationTest {

    @Shared def sharedServiceAdminToken
    @Shared def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared def sharedUserAdminToken

    @Shared User sharedSubUser
    @Shared def sharedSubUserToken

    @Shared User sharedUserManager
    @Shared def sharedUserManagerToken

    @Shared EndpointTemplate endpointTemplate

    def setupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedServiceAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Create a cloud account
        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        authResponse = cloud20.authenticatePassword(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedUserAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedSubUser = cloud20.createSubUser(sharedUserAdminToken)
        authResponse = cloud20.authenticatePassword(sharedSubUser.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedSubUserToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserManager = cloud20.createSubUser(sharedUserAdminToken)
        cloud20.addUserRole(sharedUserAdminToken, sharedUserManager.id, Constants.USER_MANAGE_ROLE_ID)
        authResponse = cloud20.authenticatePassword(sharedUserManager.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedUserManagerToken = authResponse.getEntity(AuthenticateResponse).value.token.id
  }

    /**
     * By default for these tests open up DAs to all RCNs. Tests that verify limiting the availability will need to
     * reset these properties.
     *
     * @return
     */
    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
    }

    /**
     * Verifies the delegation services accessibility is controlled via the high level feature flag. Uses as invalid
     * token to verify. If the services are disabled, the token will never be checked and a 503 will
     * be immediately returned. If the services are enabled, the token will be found invalid and a 401 will be returned.
     */
    @Unroll
    def "Create/get/delete require delegation feature flag to be enabled: enabled: #enableServices"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, enableServices)

        def invalidToken = "invalidToken"

        when: "Call the create service"
        def createResponse = cloud20.createDelegationAgreement(invalidToken, new DelegationAgreement())

        then: "Service is properly controlled"
        if (enableServices) {
            assert createResponse.status == SC_UNAUTHORIZED
        } else {
            assert createResponse.status == SC_SERVICE_UNAVAILABLE
        }

        when: "Call the get service"
        def getResponse = cloud20.getDelegationAgreement(invalidToken, "id")

        then: "Service is properly controlled"
        if (enableServices) {
            assert getResponse.status == SC_UNAUTHORIZED
        } else {
            assert getResponse.status == SC_SERVICE_UNAVAILABLE
        }

        when: "Call the delete service"
        def deleteResponse = cloud20.deleteDelegationAgreement(invalidToken, "id")

        then: "Service is properly controlled"
        if (enableServices) {
            assert deleteResponse.status == SC_UNAUTHORIZED
        } else {
            assert deleteResponse.status == SC_SERVICE_UNAVAILABLE
        }

        where:
        enableServices << [true, false]
    }

    /**
     * The primary purpose of this test is to validate the overall functionality for different content/accept
     * types. The user used for this test is arbitrary.
     */
    @Unroll
    def "Create/get/delete basic Root delegation agreement: mediaType: #mediaType"() {

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.delegateId = sharedSubUser.id
            it
        }

        when:
        def createResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, webDa, mediaType)

        then: "was successful"
        createResponse.status == SC_CREATED

        and: "created da was returned appropriately"
        def createdDa = createResponse.getEntity(DelegationAgreement)
        createdDa != null
        createdDa.id != null
        createdDa.domainId == sharedUserAdmin.domainId
        createdDa.name == webDa.name
        createdDa.description == webDa.description
        createdDa.principalId == sharedUserAdmin.id
        createdDa.principalType == PrincipalType.USER
        createdDa.delegateId == sharedSubUser.id

        when:
        def getResponse = cloud20.getDelegationAgreement(sharedUserAdminToken, createdDa.id, mediaType)

        then:
        getResponse.status == SC_OK

        and: "da was returned appropriately"
        def getDa = getResponse.getEntity(DelegationAgreement)
        getDa != null
        getDa.id == createdDa.id
        getDa.domainId == sharedUserAdmin.domainId
        getDa.name == webDa.name
        getDa.description == webDa.description
        getDa.principalId == sharedUserAdmin.id
        getDa.principalType == PrincipalType.USER
        getDa.delegateId == sharedSubUser.id

        when:
        def deleteResponse = cloud20.deleteDelegationAgreement(sharedUserAdminToken, createdDa.id, mediaType)

        then:
        deleteResponse.status == SC_NO_CONTENT

        when: "Get DA after deleting"
        def getResponse2 = cloud20.getDelegationAgreement(sharedUserAdminToken, createdDa.id, mediaType)

        then:
        getResponse2.status == SC_NOT_FOUND

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }

    @Unroll
    def "Each user type can create/get/delete basic root delegation agreement for self"() {
        def token = utils.getToken(caller.username)

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.delegateId = sharedSubUser.id
            it
        }

        when:
        def createResponse = cloud20.createDelegationAgreement(token, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        and: "created da was returned appropriately"
        def createdDa = createResponse.getEntity(DelegationAgreement)
        createdDa != null
        createdDa.id != null
        createdDa.domainId == caller.domainId
        createdDa.name == webDa.name
        createdDa.description == webDa.description
        createdDa.principalId == caller.id
        createdDa.principalType == PrincipalType.USER
        createdDa.delegateId == sharedSubUser.id

        when:
        def getResponse = cloud20.getDelegationAgreement(token, createdDa.id)

        then:
        getResponse.status == SC_OK

        and: "da was returned appropriately"
        def getDa = getResponse.getEntity(DelegationAgreement)
        getDa != null
        getDa.id == createdDa.id
        getDa.domainId == caller.domainId
        getDa.name == webDa.name
        getDa.description == webDa.description
        getDa.principalId == caller.id
        getDa.principalType == PrincipalType.USER
        getDa.delegateId == sharedSubUser.id

        when:
        def deleteResponse = cloud20.deleteDelegationAgreement(token, createdDa.id)

        then:
        deleteResponse.status == SC_NO_CONTENT

        when: "Get DA after deleting"
        def getResponse2 = cloud20.getDelegationAgreement(token, createdDa.id)

        then:
        getResponse2.status == SC_NOT_FOUND

        where:
        caller << [sharedUserAdmin, sharedUserManager, sharedSubUser]
    }

    @Unroll
    def "Fed user can create/get/delete basic root delegation agreement for self"() {
        def fedAuthResponse = utils.createFederatedUserForAuthResponse(sharedUserAdmin.domainId)
        def token = fedAuthResponse.token.id

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.delegateId = sharedSubUser.id
            it
        }

        when:
        def createResponse = cloud20.createDelegationAgreement(token, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        and: "created da was returned appropriately"
        def createdDa = createResponse.getEntity(DelegationAgreement)
        createdDa != null
        createdDa.id != null
        createdDa.domainId == sharedUserAdmin.domainId
        createdDa.name == webDa.name
        createdDa.description == webDa.description
        createdDa.principalId == fedAuthResponse.user.id
        createdDa.principalType == PrincipalType.USER
        createdDa.delegateId == sharedSubUser.id

        /*
        This will be uncommented in next PR when update get/delete to support fed users
         */
//        when:
//        def getResponse = cloud20.getDelegationAgreement(token, createdDa.id)
//
//        then:
//        getResponse.status == SC_OK
//
//        and: "da was returned appropriately"
//        def getDa = getResponse.getEntity(DelegationAgreement)
//        getDa != null
//        getDa.id == createdDa.id
//        getDa.domainId == sharedUserAdmin.domainId
//        getDa.name == webDa.name
//        getDa.description == webDa.description
//        getDa.principalId == fedAuthResponse.user.id
//        getDa.principalType == PrincipalType.USER
//        getDa.delegateId == sharedSubUser.id
//
//        when:
//        def deleteResponse = cloud20.deleteDelegationAgreement(token, createdDa.id)
//
//        then:
//        deleteResponse.status == SC_NO_CONTENT
//
//        when: "Get DA after deleting"
//        def getResponse2 = cloud20.getDelegationAgreement(token, createdDa.id)
//
//        then:
//        getResponse2.status == SC_NOT_FOUND
    }

    def "Can create/get/delete basic root delegation agreement for user group belong to"() {
        def userGroup = utils.createUserGroup(sharedUserAdmin.domainId)
        def groupSubUser = cloud20.createSubUser(sharedUserAdminToken)
        def groupSubUserToken = utils.getToken(groupSubUser.username)
        utils.addUserToUserGroup(groupSubUser.id, userGroup)

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.principalType = PrincipalType.USER_GROUP
            it.principalId = userGroup.id
            it.delegateId = sharedSubUser.id
            it
        }

        when:
        def createResponse = cloud20.createDelegationAgreement(groupSubUserToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        and: "created da was returned appropriately"
        def createdDa = createResponse.getEntity(DelegationAgreement)
        createdDa != null
        createdDa.id != null
        createdDa.domainId == userGroup.domainId
        createdDa.name == webDa.name
        createdDa.description == webDa.description
        createdDa.principalId == userGroup.id
        createdDa.principalType == PrincipalType.USER_GROUP
        createdDa.delegateId == sharedSubUser.id

        /*
        This will be uncommented in next PR when update get/delete to support usergroups
         */
//        when:
//        def getResponse = cloud20.getDelegationAgreement(groupSubUserToken, createdDa.id)
//
//        then:
//        getResponse.status == SC_OK
//
//        and: "da was returned appropriately"
//        def getDa = getResponse.getEntity(DelegationAgreement)
//        getDa != null
//        getDa.id == createdDa.id
//        getDa.domainId == userGroup.domainId
//        getDa.name == webDa.name
//        getDa.description == webDa.description
//        getDa.principalId == userGroup.id
//        getDa.principalType == PrincipalType.USER_GROUP
//        getDa.delegateId == sharedSubUser.id
//
//        when:
//        def deleteResponse = cloud20.deleteDelegationAgreement(groupSubUserToken, createdDa.id)
//
//        then:
//        deleteResponse.status == SC_NO_CONTENT
//
//        when: "Get DA after deleting"
//        def getResponse2 = cloud20.getDelegationAgreement(groupSubUserToken, createdDa.id)
//
//        then:
//        getResponse2.status == SC_NOT_FOUND
    }

}
