package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapConnectionPools
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.service.DelegationService
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE
import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE_PASSWORD
import static org.apache.http.HttpStatus.*

class RootDelegationAgreementCrudRestIntegrationTest extends RootIntegrationTest {

    @Shared
    def sharedServiceAdminToken
    @Shared
    def sharedIdentityAdminToken

    @Shared
    User sharedUserAdmin
    @Shared
    def sharedUserAdminToken

    @Shared
    User sharedSubUser
    @Shared
    def sharedSubUserToken

    @Shared
    User sharedUserManager
    @Shared
    def sharedUserManagerToken

    @Autowired
    DelegationService delegationService

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    protected LdapConnectionPools connPools

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
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP, 30)

    }

    /**
     * Verify that all users can create root DA when feature flag is turned ON and only users with user admin or above
     * can create root DA when feature flag is turned off
     */
    @Unroll
    def "All users can create root DA when feature.enable.global.root.da.creation is enabled #flag"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GLOBAL_ROOT_DELEGATION_AGREEMENT_CREATION_PROP, flag)

        if (callerToken == "fedUserToken"){
            callerToken = utils.createFederatedUserForAuthResponse(sharedUserAdmin.domainId).token.id
        }

        if (callerToken == "rackerImpersonationToken") {
            def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
            callerToken = utils.impersonateWithToken(rackerToken, sharedUserAdmin).token.id
        }

        if (callerToken == "idAdminImpersonationToken") {
            callerToken = utils.impersonateWithToken(utils.getIdentityAdminToken(), sharedUserAdmin).token.id
        }

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 0
            it
        }

        when: "different users creates the DA "
        def createResponse = cloud20.createDelegationAgreement(callerToken, webDa)

        then:
        createResponse.status == respCode

        cleanup:
        try {
            def daId = createResponse.getEntity(DelegationAgreement).id
            cloud20.deleteDelegationAgreement(sharedUserAdminToken, daId)
        } catch (Exception ex) {
            //eat
        }

        where:
        callerToken                 | respCode | flag
        sharedUserAdminToken        | 201      | false
        sharedUserManagerToken      | 403      | false
        sharedSubUserToken          | 403      | false
        "fedUserToken"              | 403      | false
        "idAdminImpersonationToken" | 201      | false
        "rackerImpersonationToken"  | 201      | false

        sharedUserAdminToken        | 201      | true
        sharedUserManagerToken      | 201      | true
        sharedSubUserToken          | 201      | true
        "fedUserToken"              | 201      | true
        "idAdminImpersonationToken" | 201      | true
        "rackerImpersonationToken"  | 201      | true
    }

    /**
     * Verifies the delegation services accessibility is controlled via the high level feature flag. Uses as invalid
     * token as token validity is the first thing checked by services.
     *
     * - If the services are disabled, a 503 should be returned
     * - If the services are enabled, the token will be validated, found invalid, and a 401 will be returned.
     */
    @Unroll
    def "Delegation service '#name' is protected by feature flag 'feature.enable.delegation.agreement.services'"() {

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, false)

        // Execute the closure
        def response = responseCall("invalidToken")

        then:
        response.status == SC_SERVICE_UNAVAILABLE

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        response = responseCall()

        then:
        response.status == SC_UNAUTHORIZED

        where:
        [name, responseCall] << [
                ["createDelegationAgreement", { cloud20.createDelegationAgreement(it, new DelegationAgreement()) }]
                , ["getDelegationAgreement", { cloud20.getDelegationAgreement(it, "id") }]
                , ["deleteDelegationAgreement", { cloud20.deleteDelegationAgreement(it, "id") }]
                , ["addUserDelegate", { cloud20.addUserDelegate(it, "id", "id") }]
                , ["addUserGroupDelegate", { cloud20.addUserGroupDelegate(it, "id", "id") }]
                , ["deleteUserDelegate", { cloud20.addUserDelegate(it, "id", "id") }]
                , ["deleteUserGroupDelegate", { cloud20.addUserGroupDelegate(it, "id", "id") }]
                , ["listDelegationAgreements", { cloud20.listDelegationAgreements(it, null) }]
        ]
    }

    @Unroll
    def "Delegation service '#name' rejects racker tokens"() {
        def token = utils.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD).token.id

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        def response = responseCall(token)

        then:
        response.status == SC_FORBIDDEN

        where:
        [name, responseCall] << [
                ["createDelegationAgreement", { cloud20.createDelegationAgreement(it, new DelegationAgreement()) }]
                , ["getDelegationAgreement", { cloud20.getDelegationAgreement(it, "id") }]
                , ["deleteDelegationAgreement", { cloud20.deleteDelegationAgreement(it, "id") }]
                , ["addUserDelegate", { cloud20.addUserDelegate(it, "id", "id") }]
                , ["addUserGroupDelegate", { cloud20.addUserGroupDelegate(it, "id", "id") }]
                , ["deleteUserDelegate", { cloud20.addUserDelegate(it, "id", "id") }]
                , ["deleteUserGroupDelegate", { cloud20.addUserGroupDelegate(it, "id", "id") }]
                , ["listDelegationAgreements", { cloud20.listDelegationAgreements(it, null) }]
        ]
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
        createdDa.subAgreementNestLevel == 0 // default when when not specified

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
        createdDa.subAgreementNestLevel == 0 // default when when not specified

        when:
        resetCloudFeedsMock()
        utils.addUserDelegate(sharedUserAdminToken, createdDa.id, sharedUserAdmin.id)
        def deleteResponse = cloud20.deleteDelegationAgreement(sharedUserAdminToken, createdDa.id, mediaType)

        then:
        deleteResponse.status == SC_NO_CONTENT

        and: "verify an update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(sharedUserAdmin, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

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
            it.subAgreementNestLevel = 1
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
        createdDa.subAgreementNestLevel == 1

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
        createdDa.subAgreementNestLevel == 1

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

    def "Fed user can create/get/delete basic root delegation agreement for self"() {
        def fedAuthResponse = utils.createFederatedUserForAuthResponse(sharedUserAdmin.domainId)
        def fedUser = utils.getUserById(fedAuthResponse.user.id)
        def token = fedAuthResponse.token.id

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
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
        createdDa.subAgreementNestLevel == 0

        when:
        def getResponse = cloud20.getDelegationAgreement(token, createdDa.id)

        then:
        getResponse.status == SC_OK

        and: "da was returned appropriately"
        def getDa = getResponse.getEntity(DelegationAgreement)
        getDa != null
        getDa.id == createdDa.id
        getDa.domainId == sharedUserAdmin.domainId
        getDa.name == webDa.name
        getDa.description == webDa.description
        getDa.principalId == fedAuthResponse.user.id
        getDa.principalType == PrincipalType.USER

        when:
        resetCloudFeedsMock()
        utils.addUserDelegate(token, createdDa.id, fedAuthResponse.user.id)
        def deleteResponse = cloud20.deleteDelegationAgreement(token, createdDa.id)

        then:
        deleteResponse.status == SC_NO_CONTENT

        and: "verify no update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(fedUser, EventType.UPDATE),
                VerificationTimes.exactly(0)
        )

        when: "Get DA after deleting"
        def getResponse2 = cloud20.getDelegationAgreement(token, createdDa.id)

        then:
        getResponse2.status == SC_NOT_FOUND
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

        when:
        def getResponse = cloud20.getDelegationAgreement(groupSubUserToken, createdDa.id)

        then:
        getResponse.status == SC_OK

        and: "da was returned appropriately"
        def getDa = getResponse.getEntity(DelegationAgreement)
        getDa != null
        getDa.id == createdDa.id
        getDa.domainId == userGroup.domainId
        getDa.name == webDa.name
        getDa.description == webDa.description
        getDa.principalId == userGroup.id
        getDa.principalType == PrincipalType.USER_GROUP

        when:
        def deleteResponse = cloud20.deleteDelegationAgreement(groupSubUserToken, createdDa.id)

        then:
        deleteResponse.status == SC_NO_CONTENT

        when: "Get DA after deleting"
        def getResponse2 = cloud20.getDelegationAgreement(groupSubUserToken, createdDa.id)

        then:
        getResponse2.status == SC_NOT_FOUND
    }

    @Unroll
    def "addAgreement: subAgreementNestLevel set appropriately based on input: maxNest: #maxNest; nestLevel: #nestLevel"() {
        reloadableConfiguration.setProperty(IdentityConfig.DELEGATION_MAX_NEST_LEVEL_PROP, maxNest)

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = nestLevel
            it
        }

        when:
        def createResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        and: "created da was returned appropriately"
        def createdDa = createResponse.getEntity(DelegationAgreement)
        createdDa != null
        createdDa.getSubAgreementNestLevel() == nestLevel

        when:
        def getDa = utils.getDelegationAgreement(sharedUserAdminToken, createdDa.id)

        then:
        getDa.getSubAgreementNestLevel() == nestLevel

        cleanup:
        try {
            cloud20.deleteDelegationAgreement(sharedUserAdminToken, createdDa.id)
        } catch (Exception ex) {
            //eat
        }

        where:
        maxNest | nestLevel
        5 | 2
        3 | 0
        5 | 5

    }

    @Unroll
    def "addAgreement: creating subagreement with different nest levels: parentNest: #parentNest; childNest: #childNest"() {
        reloadableConfiguration.setProperty(IdentityConfig.DELEGATION_MAX_NEST_LEVEL_PROP, 5)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = parentNest
            it
        }
        parentDa = utils.createDelegationAgreement(sharedUserAdminToken, parentDa)
        utils.addUserDelegate(sharedUserAdminToken, parentDa.id, sharedUserAdmin.id)

        when:
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = childNest
            it
        }
        subAgreement = utils.createDelegationAgreement(sharedUserAdminToken, subAgreement)

        then: "was successful"
        subAgreement != null
        subAgreement.getSubAgreementNestLevel() == childNest
        subAgreement.parentDelegationAgreementId == parentDa.id

        cleanup:
        cloud20.deleteDelegationAgreement(sharedUserAdminToken, parentDa.id)
        cloud20.deleteDelegationAgreement(sharedUserAdminToken, subAgreement.id)

        where:
        parentNest | childNest
        3 | 1
        2 | 0
    }

    def "addAgreement: Can't create nested agreement when parent doesn't allow"() {
        reloadableConfiguration.setProperty(IdentityConfig.DELEGATION_MAX_NEST_LEVEL_PROP, 5)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 0
            it
        }
        parentDa = utils.createDelegationAgreement(sharedUserAdminToken, parentDa)
        utils.addUserDelegate(sharedUserAdminToken, parentDa.id, sharedUserAdmin.id)

        when:
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 0
            it
        }
        def subAgreementResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, subAgreement)

        then:
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(subAgreementResponse, ForbiddenFault, SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION)

        cleanup:
        cloud20.deleteDelegationAgreement(sharedUserAdminToken, parentDa.id)
    }

    def "addAgreement: Can't create nested agreement that matches or exceeds parent nest level"() {
        reloadableConfiguration.setProperty(IdentityConfig.DELEGATION_MAX_NEST_LEVEL_PROP, 5)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 3
            it
        }
        parentDa = utils.createDelegationAgreement(sharedUserAdminToken, parentDa)
        utils.addUserDelegate(sharedUserAdminToken, parentDa.id, sharedUserAdmin.id)

        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it
        }

        when: "Matches parent nest level"
        subAgreement.setSubAgreementNestLevel(parentDa.subAgreementNestLevel)
        def subAgreementResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, subAgreement)

        then:
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(subAgreementResponse, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_INVALID_VALUE)

        when: "Exceeds parent nest level"
        subAgreement.setSubAgreementNestLevel(parentDa.subAgreementNestLevel.add(BigInteger.ONE))
        def subAgreementResponse2 = cloud20.createDelegationAgreement(sharedUserAdminToken, subAgreement)

        then:
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(subAgreementResponse2, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_INVALID_VALUE)

        cleanup:
        cloud20.deleteDelegationAgreement(sharedUserAdminToken, parentDa.id)
    }

    def "Can manage user delegates on root delegation agreement"() {
        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it
        }

        def da = utils.createDelegationAgreement(sharedUserAdminToken, webDa)

        EndUser delegateEntity = identityUserService.getEndUserById(sharedSubUser.id)

        when: "Add user delegate"
        def addResponse = cloud20.addUserDelegate(sharedUserAdminToken, da.id, delegateEntity.id)
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = delegationService.getDelegationAgreementById(da.getId())

        then: "successful"
        addResponse.status == SC_NO_CONTENT

        and: "backend lists user as delegate"
        daEntity.isEffectiveDelegate(delegateEntity)

        when: "Delete delegate"
        def deleteResponse = cloud20.deleteUserDelegate(sharedUserAdminToken, da.id, sharedSubUser.id)
        daEntity = delegationService.getDelegationAgreementById(da.getId())

        then: "successful"
        deleteResponse.status == SC_NO_CONTENT

        and: "backend no longer lists user as delegate"
        !daEntity.isEffectiveDelegate(delegateEntity)

        when: "Delete delegate again"
        def deleteResponse2 = cloud20.deleteUserDelegate(sharedUserAdminToken, da.id, sharedSubUser.id)

        then: "error"
        deleteResponse2.status == SC_NOT_FOUND
    }

    /**
     * Test managing a user group as a delegate on a DA and that users are considered delegates when they are a member
     * of the group.
     *
     * @return
     */
    def "Can manage user group delegates on root delegation agreement"() {
        def userGroup = utils.createUserGroup(sharedUserAdmin.domainId)
        def groupSubUser = cloud20.createSubUser(sharedUserAdminToken)

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it
        }

        def da = utils.createDelegationAgreement(sharedUserAdminToken, webDa)
        EndUser groupUserEntity = identityUserService.getEndUserById(groupSubUser.id)

        when: "Add user group delegate"
        def addResponse = cloud20.addUserGroupDelegate(sharedUserAdminToken, da.id, userGroup.id)
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = delegationService.getDelegationAgreementById(da.getId())

        then: "successful"
        addResponse.status == SC_NO_CONTENT

        and: "Backend does not list non-group member as delegate"
        !daEntity.isEffectiveDelegate(groupUserEntity)

        when: "When user added to group"
        utils.addUserToUserGroup(groupUserEntity.id, userGroup)
        groupUserEntity = identityUserService.getEndUserById(groupSubUser.id) // Reload as user's group membership changed

        then: "User is now a delegate"
        daEntity.isEffectiveDelegate(groupUserEntity)

        when: "Delete user group delegate"
        resetCloudFeedsMock()
        def deleteResponse = cloud20.deleteUserGroupDelegate(sharedUserAdminToken, da.id, userGroup.id)
        daEntity = delegationService.getDelegationAgreementById(da.getId()) // Reload as delegates changed

        then: "successful"
        deleteResponse.status == SC_NO_CONTENT

        and: "verify an update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(groupUserEntity, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        and: "User no longer a delegate"
        !daEntity.isEffectiveDelegate(groupUserEntity)
    }

    /**
     * Test setting multiple delegates on a DA
     *
     * @return
     */
    def "Can manage multiple delegates on root delegation agreement"() {
        def userGroup = utils.createUserGroup(sharedUserAdmin.domainId)
        def groupSubUser = cloud20.createSubUser(sharedUserAdminToken)
        utils.addUserToUserGroup(groupSubUser.id, userGroup)

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it
        }

        def da = utils.createDelegationAgreement(sharedUserAdminToken, webDa)
        EndUser groupUserEntity = identityUserService.getEndUserById(groupSubUser.id)
        EndUser userDelegateEntity = identityUserService.getEndUserById(sharedSubUser.id)

        // Verify start state. Neither are delegates
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = delegationService.getDelegationAgreementById(da.getId())
        assert !daEntity.isEffectiveDelegate(groupUserEntity)
        assert !daEntity.isEffectiveDelegate(userDelegateEntity)

        when: "Add user group delegate"
        def addResponse = cloud20.addUserGroupDelegate(sharedUserAdminToken, da.id, userGroup.id)
        daEntity = delegationService.getDelegationAgreementById(da.getId())

        then: "successful"
        addResponse.status == SC_NO_CONTENT

        and: "Group member is now a delegate"
        daEntity.isEffectiveDelegate(groupUserEntity)

        and: "User is still not a delegate"
        !daEntity.isEffectiveDelegate(userDelegateEntity)

        when: "Add user delegate"
        addResponse = cloud20.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser.id)
        daEntity = delegationService.getDelegationAgreementById(da.getId())

        then: "successful"
        addResponse.status == SC_NO_CONTENT

        and: "User is now a delegate"
        daEntity.isEffectiveDelegate(userDelegateEntity)

        and: "Group member is still a delegate"
        daEntity.isEffectiveDelegate(groupUserEntity)

        when: "Remove group delegate"
        def response = cloud20.deleteUserGroupDelegate(sharedUserAdminToken, da.id, userGroup.id)
        daEntity = delegationService.getDelegationAgreementById(da.getId())

        then: "successful"
        response.status == SC_NO_CONTENT

        and: "User is still a delegate"
        daEntity.isEffectiveDelegate(userDelegateEntity)

        and: "Group member is no longer a delegate"
        !daEntity.isEffectiveDelegate(groupUserEntity)
    }

    @Unroll
    def "Error on DA creation when maximum number of DAs for principal are exceeded - mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP, 1)

        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManage = cloud20.createSubUser(userAdminToken)
        utils.addRoleToUser(userManage, Constants.USER_MANAGE_ROLE_ID)
        def userManageToken = utils.getToken(userManage.username)

        def fedUserResponse = utils.createFederatedUserForAuthResponse(userAdmin.domainId)
        def fedUserToken = fedUserResponse.token.id

        def userGroup = utils.createUserGroup(userAdmin.domainId)

        // Add users to group
        utils.addUserToUserGroup(userAdmin.id, userGroup)

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it
        }

        when: "create first DA with user-Admin principal"
        webDa.principalId = userAdmin.id
        webDa.principalType = PrincipalType.USER
        def createResponse = cloud20.createDelegationAgreement(userAdminToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        when: "create second DA with user-Admin principal"
        createResponse = cloud20.createDelegationAgreement(userAdminToken, webDa)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(createResponse
                , BadRequestFault
                , SC_BAD_REQUEST
                , ErrorCodes.ERROR_CODE_THRESHOLD_REACHED
                , "Maximum number of delegation agreements has been reached for principal")

        when: "create first DA with user-manage principal"
        webDa.principalId = userManage.id
        webDa.principalType = PrincipalType.USER
        createResponse = cloud20.createDelegationAgreement(userManageToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        when: "create second DA with user-manage principal"
        createResponse = cloud20.createDelegationAgreement(userManageToken, webDa)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(createResponse
                , BadRequestFault
                , SC_BAD_REQUEST
                , ErrorCodes.ERROR_CODE_THRESHOLD_REACHED
                , "Maximum number of delegation agreements has been reached for principal")

        when: "create first DA with federated user principal"
        webDa.principalId = fedUserResponse.user.id
        webDa.principalType = PrincipalType.USER
        createResponse = cloud20.createDelegationAgreement(fedUserToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        when: "create second DA with federated user principal"
        createResponse = cloud20.createDelegationAgreement(fedUserToken, webDa)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(createResponse
                , BadRequestFault
                , SC_BAD_REQUEST
                , ErrorCodes.ERROR_CODE_THRESHOLD_REACHED
                , "Maximum number of delegation agreements has been reached for principal")

        when: "create first DA with user-group principal"
        webDa.principalId = userGroup.id
        webDa.principalType = PrincipalType.USER_GROUP
        createResponse = cloud20.createDelegationAgreement(userAdminToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        when: "create second DA with user-group principal"
        createResponse = cloud20.createDelegationAgreement(userAdminToken, webDa)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(createResponse
                , BadRequestFault
                , SC_BAD_REQUEST
                , ErrorCodes.ERROR_CODE_THRESHOLD_REACHED
                , "Maximum number of delegation agreements has been reached for principal")

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Verify maximum number of DAs per principal does not affect member of a user group - mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP, 1)

        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userGroup1 = utils.createUserGroup(userAdmin.domainId)
        def userGroup2 = utils.createUserGroup(userAdmin.domainId)

        // Add user to groups
        utils.addUserToUserGroup(userAdmin.id, userGroup1)
        utils.addUserToUserGroup(userAdmin.id, userGroup2)

        DelegationAgreement webDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.principalType = PrincipalType.USER_GROUP
            it
        }

        when: "create first DA with userGroup1 principal"
        webDa.principalId = userGroup1.id
        def createResponse = cloud20.createDelegationAgreement(userAdminToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        when: "create second DA with userGroup2 principal"
        webDa.principalId = userGroup2.id
        createResponse = cloud20.createDelegationAgreement(userAdminToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        when: "create DA with user principal"
        webDa.principalId = userAdmin.id
        webDa.principalType = PrincipalType.USER
        createResponse = cloud20.createDelegationAgreement(userAdminToken, webDa)

        then: "was successful"
        createResponse.status == SC_CREATED

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

}
