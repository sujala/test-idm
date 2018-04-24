package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.DelegationService
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class DelegationReconciliationIntegrationTest extends RootIntegrationTest {

    @Autowired
    DelegationService delegationService

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AUTHENTICATION_PROP, true)
    }

    def "when a user is deleted the DAs for which the user is the principal are deleted"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def userAdmin2 = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin2.domainId, domain.rackspaceCustomerNumber)
        def subUser = utils.createUser(utils.getToken(userAdmin2.username))
        def daToCreate = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, subUser.id)

        when: "get the DA"
        def delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA exists"
        delegationAgreement != null

        when: "the principal user is deleted"
        utils.deleteUser(userAdmin)
        delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA no longer exists"
        delegationAgreement == null

        when: "try to auth as a user that was a delegate of the DA using token and DA"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(subUser.username), da.id)

        then:
        response.status == 404
    }

    def "when a user is moved to a different domain the DAs for which the user is a principal are deleted"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def userAdmin2 = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin2.domainId, domain.rackspaceCustomerNumber)
        def daToCreate = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, userAdmin2.id)

        when: "get the DA"
        def delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA exists"
        delegationAgreement != null

        when: "move the principal user to a different domain"
        def newDomain = utils.createDomainEntity()
        utils.addUserToDomain(utils.getServiceAdminToken(), userAdmin.id, newDomain.id)
        delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA no longer exists"
        delegationAgreement == null

        when: "try to auth as a user that is a delegate of the DA with token and DA"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(userAdmin2.username), da.id)

        then:
        response.status == 404
    }

    def "when a user is deleted the DAs for which the user is a delegate are not deleted but the user is no longer a delegate"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def userAdmin2 = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin2.domainId, domain.rackspaceCustomerNumber)
        def subUser = utils.createUser(utils.getToken(userAdmin2.username))
        def daToCreate = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, subUser.id)
        def fedUserAuth = utils.authenticateFederatedUser(userAdmin.domainId)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, fedUserAuth.user.id)

        when: "list delegates for DA"
        def delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the sub-user is a delegate"
        delegates.delegateReference.find { it -> it.delegateId == subUser.id } != null

        when: "delete the sub-user"
        utils.deleteUser(subUser)
        delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the sub-user is no longer a delegate"
        delegates != null
        delegates.delegateReference.find { it -> it.delegateId == subUser.id } == null

        when: "that under the DA using token and DA using a different user"
        def response = cloud20.authenticateTokenAndDelegationAgreement(fedUserAuth.token.id, da.id)

        then:
        response.status == 200
    }

    def "when a user is moved to a different domain, the DAs for which the user is a delegate are not deleted but the user is no longer a delegate"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def userAdmin2 = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin2.domainId, domain.rackspaceCustomerNumber)
        def daToCreate = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, userAdmin2.id)
        def fedUserAuth = utils.authenticateFederatedUser(userAdmin.domainId)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, fedUserAuth.user.id)

        when: "list delegates for the DA"
        def delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the other user admin is a delegate"
        delegates.delegateReference.find { it -> it.delegateId == userAdmin2.id } != null

        and: "the federated user is a delegate"
        delegates.delegateReference.find { it -> it.delegateId == fedUserAuth.user.id } != null

        when: "move the provisioned user delegate to a new domain"
        def newDomain = utils.createDomainEntity()
        utils.addUserToDomain(utils.getServiceAdminToken(), userAdmin2.id, newDomain.id)
        delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the provisioned user is no longer a delegate"
        delegates.delegateReference.find { it -> it.delegateId == userAdmin2.id } == null

        and: "the federated user is still a delegate"
        delegates.delegateReference.find { it -> it.delegateId == fedUserAuth.user.id } != null

        when: "auth with token and DA using the provisioned user (no longer a delegate)"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(userAdmin2.username), da.id)

        then: "404 b/c the user is no longer a delegate"
        response.status == 404

        when: "auth with token and DA using the federated user (still a delegate)"
        response = cloud20.authenticateTokenAndDelegationAgreement(fedUserAuth.token.id, da.id)

        then: "success"
        response.status == 200
    }

    def "when a user is moved to a different domain, the DAs for which the user is a delegate though user group membership are not modified"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def subUser = utils.createUser(utils.getToken(userAdmin.username))
        def daToCreate = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(subUser.id, userGroup)
        utils.addUserGroupDelegate(utils.getToken(userAdmin.username), da.id, userGroup.id)

        when: "list delegates for DA"
        def delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the user group is a delegate"
        delegates.delegateReference.find { it -> it.delegateId == userGroup.id } != null

        when: "move the sub-user (part of the user group) to a different domain and list delegates for DA"
        def newDomain = utils.createDomainEntity()
        utils.addUserToDomain(utils.getServiceAdminToken(), subUser.id, newDomain.id)
        delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the user group is still a delegate of the DA"
        delegates.delegateReference.find { it -> it.delegateId == userGroup.id } != null

        when: "try to auth as the sub-user (now in a different domain) using token and DA"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(subUser.username), da.id)

        then: "success, the sub-user is still able to auth using token and DA"
        response.status == 200
    }

    def "when a user is moved to a different domain, the DAs for which the user is a principal though user group membership are not modified"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def subUser = utils.createUser(utils.getToken(userAdmin.username))
        def subUser2 = utils.createUser(utils.getToken(userAdmin.username)) // the only user allowed to modify the DA
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(subUser2.id, userGroup)
        def daToCreate = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER_GROUP
            it.principalId = userGroup.id
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(subUser2.username), daToCreate)
        utils.addUserDelegate(utils.getToken(subUser2.username), da.id, subUser.id)

        when: "move the DA admin (the sub-user allowed to modify the DA) to a different domain"
        def newDomain = utils.createDomainEntity()
        utils.addUserToDomain(utils.getServiceAdminToken(), subUser2.id, newDomain.id)
        def delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(subUser2.username), da.id)

        then: "the sub-user is still a delegate"
        delegates != null
        delegates.delegateReference.find { it -> it.delegateId == subUser.id } != null

        when: "auth as the sub-user w/ token and DA"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(subUser.username), da.id)

        then:
        response.status == 200

        when: "move the DA admin (the sub-user allowed to modify the DA) back to the original domain"
        utils.addUserToDomain(utils.getServiceAdminToken(), subUser2.id, userAdmin.domainId)
        delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(subUser2.username), da.id)

        then: "the sub-user is still a delegate"
        delegates != null
        delegates.delegateReference.find { it -> it.delegateId == subUser.id } != null

        when: "auth as the sub-user w/ token and DA"
        response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(subUser.username), da.id)

        then:
        response.status == 200
    }

    def "when a user group is deleted all DA for which the user group is the principal are deleted"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def userAdmin2 = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin2.domainId, domain.rackspaceCustomerNumber)
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        def daToCreate = new DelegationAgreement().with {
            it.principalId = userGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, userAdmin2.id)

        when: "get the DA"
        def delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA exists"
        delegationAgreement != null
        delegationAgreement.principalType == PrincipalType.USER_GROUP
        delegationAgreement.principalId == userGroup.id

        when: "delete the user group (the DA principal)"
        utils.deleteUserGroup(userGroup)
        delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then:
        delegationAgreement == null

        when: "auth as a user delegate using token and DA"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(userAdmin2.username), da.id)

        then:
        response.status == 404
    }

    def "when a user that is part of a user group is deleted, the DAs for which the user group is a delegate remain unchanged"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def subUser = utils.createUser(utils.getToken(userAdmin.username))
        def subUser2 = utils.createUser(utils.getToken(userAdmin.username))
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.addUserToUserGroup(subUser.id, userGroup)
        utils.addUserToUserGroup(subUser2.id, userGroup)
        def daToCreate = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserGroupDelegate(utils.getToken(userAdmin.username), da.id, userGroup.id)

        when: "get the DA"
        def delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA exists"
        delegationAgreement != null
        delegationAgreement.principalType == PrincipalType.USER
        delegationAgreement.principalId == userAdmin.id

        when: "delete a user that is a member of the user group delegate"
        utils.deleteUser(subUser)
        delegationAgreement = delegationService.getDelegationAgreementById(da.id)
        def delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the DA still exists"
        delegationAgreement != null
        delegationAgreement.principalType == PrincipalType.USER
        delegationAgreement.principalId == userAdmin.id

        and: "the user group is still a delegate"
        delegates.delegateReference.find { it -> it.delegateId == userGroup.id } != null

        when: "auth with token and DA as one of the remaining members of the user group"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(subUser2.username), da.id)

        then:
        response.status == 200
    }

    def "when a user that is part of a user group is deleted, the DAs for which the user group is a principal remain unchanged"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def subUser = utils.createUser(utils.getToken(userAdmin.username))
        def domain = utils.getDomain(userAdmin.domainId)
        def userAdmin2 = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin2.domainId, domain.rackspaceCustomerNumber)
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.addUserToUserGroup(subUser.id, userGroup)
        def daToCreate = new DelegationAgreement().with {
            it.principalId = userGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserDelegate(utils.getToken(userAdmin.username), da.id, userAdmin2.id)

        when: "get the DA"
        def delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA exists"
        delegationAgreement != null
        delegationAgreement.principalType == PrincipalType.USER_GROUP
        delegationAgreement.principalId == userGroup.id

        when: "delete the user that is part of the principal user group"
        utils.deleteUser(subUser)
        delegationAgreement = delegationService.getDelegationAgreementById(da.id)

        then: "the DA still exists"
        delegationAgreement != null
        delegationAgreement.principalType == PrincipalType.USER_GROUP
        delegationAgreement.principalId == userGroup.id

        when: "auth as a user delegate"
        def response = cloud20.authenticateTokenAndDelegationAgreement(utils.getToken(userAdmin2.username), da.id)

        then:
        response.status == 200
    }

    def "when a user group is deleted, the user group is removed as a delegate from all DAs"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        def daToCreate = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = userAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(utils.getToken(userAdmin.username), daToCreate)
        utils.addUserGroupDelegate(utils.getToken(userAdmin.username), da.id, userGroup.id)

        when: "list delegates for DA"
        def delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the user group is a delegate"
        delegates.delegateReference.find { it -> it.delegateId == userGroup.id } != null

        when: "get the DA"
        def delegationAgreement = utils.getDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the DA still exists"
        delegationAgreement != null
        delegationAgreement.principalId == userAdmin.id

        when: "delete the user group and list delegates"
        utils.deleteUserGroup(userGroup)
        delegates = utils.listDelegatesForDelegationAgreement(utils.getToken(userAdmin.username), da.id)

        then: "the user group is no longer a delegate"
        delegates.delegateReference.find { it -> it.delegateId == userGroup.id } == null
    }

}
