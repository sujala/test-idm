package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class UpdateDelegationAgreementIntegrationTest extends RootIntegrationTest {

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
    }

    @Unroll
    def "test updating a delegation agreement - mediaType = #mediaType"() {
        given:
        def user = utils.createCloudAccount()
        def userToken = utils.getToken(user.username)
        DelegationAgreement agreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it
        }
        def createDa = utils.createDelegationAgreement(userToken, agreement)

        when: "get da without any updates"
        def response = cloud20.getDelegationAgreement(userToken, createDa.id, mediaType)
        def returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == agreement.name
        returnedEntity.id == createDa.id
        returnedEntity.domainId == createDa.domainId
        returnedEntity.description == null
        returnedEntity.subAgreementNestLevel == BigInteger.ZERO

        when: "update da with same values"
        DelegationAgreement delegationAgreement = new DelegationAgreement().with {
            it.name = createDa.name
            it.description = createDa.description
            it.subAgreementNestLevel = createDa.subAgreementNestLevel
            it.id = createDa.id
            it
        }
        response = cloud20.updateDelegationAgreement(userToken, createDa.id, delegationAgreement, mediaType)
        returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == agreement.name
        returnedEntity.id == createDa.id
        returnedEntity.domainId == createDa.domainId
        returnedEntity.description == null
        returnedEntity.subAgreementNestLevel == BigInteger.ZERO

        when: "update da without any updates"
        delegationAgreement = new DelegationAgreement()
        response = cloud20.updateDelegationAgreement(userToken, createDa.id, delegationAgreement, mediaType)
        returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == agreement.name
        returnedEntity.id == createDa.id
        returnedEntity.domainId == createDa.domainId
        returnedEntity.description == null
        returnedEntity.subAgreementNestLevel == BigInteger.ZERO

        when: "update da's name"
        delegationAgreement.name = testUtils.getRandomUUIDOfLength("da", 32)
        response = cloud20.updateDelegationAgreement(userToken, createDa.id, delegationAgreement, mediaType)
        returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == delegationAgreement.name
        returnedEntity.id == createDa.id
        returnedEntity.domainId == createDa.domainId
        returnedEntity.description == null
        returnedEntity.subAgreementNestLevel == BigInteger.ZERO

        when: "update da's description"
        delegationAgreement.setDescription("new description")
        response = cloud20.updateDelegationAgreement(userToken, createDa.id, delegationAgreement, mediaType)
        returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == delegationAgreement.name
        returnedEntity.id == createDa.id
        returnedEntity.domainId == createDa.domainId
        returnedEntity.description == delegationAgreement.description
        returnedEntity.subAgreementNestLevel == BigInteger.ZERO

        when: "update da's nest level"
        delegationAgreement.setSubAgreementNestLevel(BigInteger.ONE)
        response = cloud20.updateDelegationAgreement(userToken, createDa.id, delegationAgreement, mediaType)
        returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == delegationAgreement.name
        returnedEntity.id == createDa.id
        returnedEntity.domainId == createDa.domainId
        returnedEntity.description == delegationAgreement.description
        returnedEntity.subAgreementNestLevel == BigInteger.ONE

        cleanup:
        utils.deleteUserQuietly(user)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "test updating a delegation agreement's nested da level - original: originalNestLevel: #originalNestLevel ; newNestLevel: #newNestLevel"() {
        given:
        def user = utils.createCloudAccount()
        def userToken = utils.getToken(user.username)
        DelegationAgreement agreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.subAgreementNestLevel = originalNestLevel
            it
        }
        def createDa = utils.createDelegationAgreement(userToken, agreement)

        when: "update da nest level"
        DelegationAgreement delegationAgreement = new DelegationAgreement().with {
            it.subAgreementNestLevel = newNestLevel
            it
        }
        def response = cloud20.updateDelegationAgreement(userToken, createDa.id, delegationAgreement)
        def returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == agreement.name
        returnedEntity.id == createDa.id
        returnedEntity.domainId == createDa.domainId
        returnedEntity.description == null
        returnedEntity.subAgreementNestLevel == newNestLevel

        cleanup:
        utils.deleteUserQuietly(user)

        where:
        [originalNestLevel, newNestLevel] << [[0,1], [2,3], [2,2], [2,0], [2,1]]
    }

    @Unroll
    def "test updating a nested delegation agreement's nested da level -  originalNestLevel: originalNestLevel: #originalChildNestLevel ; newChildNestLevel: #newChildNestLevel"() {
        given:
        def user = utils.createCloudAccount()
        def userToken = utils.getToken(user.username)
        def subUser = cloud20.createSubUser(userToken)
        def subUserToken = utils.getToken(subUser.username)

        DelegationAgreement parentAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("parentda", 32)
            it.subAgreementNestLevel = BigInteger.valueOf(3)
            it
        }
        def parentDa = utils.createDelegationAgreement(userToken, parentAgreement)

        utils.addUserDelegate(userToken, parentDa.id, subUser.id)

        DelegationAgreement originalChildAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("nestedda", 32)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = originalChildNestLevel
            it
        }
        def childDa = utils.createDelegationAgreement(subUserToken, originalChildAgreement)

        when: "update da nest level"
        DelegationAgreement updatedChildAgreement = new DelegationAgreement().with {
            it.subAgreementNestLevel = newChildNestLevel
            it
        }
        def response = cloud20.updateDelegationAgreement(userToken, childDa.id, updatedChildAgreement)
        def returnedEntity = response.getEntity(DelegationAgreement)

        then:
        response.status == HttpStatus.SC_OK
        returnedEntity.name == childDa.name
        returnedEntity.id == childDa.id
        returnedEntity.domainId == childDa.domainId
        returnedEntity.description == null
        returnedEntity.subAgreementNestLevel == newChildNestLevel

        cleanup:
        utils.deleteUserQuietly(user)

        where:
        [originalChildNestLevel, newChildNestLevel] << [[0,1], [1,2], [2,2], [2,0], [2,1]]
    }

    @Unroll
    def "updateDelegationAgreement: error check - mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        DelegationAgreement agreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it
        }
        def createDa = utils.createDelegationAgreement(userAdminToken, agreement)
        def delegationAgreement = new DelegationAgreement()

        // Add delegate to da
        def defaultUser = utils.createUser(userAdminToken)
        utils.addUserDelegate(userAdminToken, createDa.id, defaultUser.id)
        def delegateToken = utils.getToken(defaultUser.username)

        when: "max name length exceeded"
        delegationAgreement.name = testUtils.getRandomUUIDOfLength("da", 33)
        def response = cloud20.updateDelegationAgreement(userAdminToken, createDa.id, delegationAgreement, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED, "name length cannot exceed 32 characters")

        when: "max description length exceeded"
        delegationAgreement.name = createDa.name
        delegationAgreement.description = testUtils.getRandomUUIDOfLength("description", 256)
        response = cloud20.updateDelegationAgreement(userAdminToken, createDa.id, delegationAgreement, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED, "description length cannot exceed 255 characters")

        when: "delegate token"
        delegationAgreement.name = createDa.name
        delegationAgreement.description = createDa.description
        response = cloud20.updateDelegationAgreement(delegateToken, createDa.id, delegationAgreement, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}
