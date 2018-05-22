package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import testHelpers.RootIntegrationTest

class DelegationManagementAccessIntegrationTest extends RootIntegrationTest {

    @Shared def rcn
    @Shared def rcnAdmin
    @Shared def rcnAdminToken
    @Shared def userAdmin
    @Shared def userAdminToken
    @Shared def userAdminDa
    @Shared def userManager
    @Shared def userManagerToken
    @Shared def userManagerDa
    @Shared def otherUserAdmin
    @Shared def otherUserAdminDa
    @Shared def otherUserManager
    @Shared def otherUserManagerDa
    @Shared def userGroupUserAdminDomain
    @Shared def userGroupUserAdminDomainDa
    @Shared def userGroupOtherUserAdminDomain
    @Shared def userGroupOtherUserAdminDomainDa

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)

        rcn = "RCN-${RandomStringUtils.randomAlphanumeric(3)}-${RandomStringUtils.randomAlphanumeric(3)}-${RandomStringUtils.randomAlphanumeric(3)}"
        rcnAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(rcnAdmin.domainId, rcn)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        rcnAdminToken = utils.getToken(rcnAdmin.username)

        userAdmin = utils.createCloudAccount()
        userAdminDa = createDelegationAgreementInDomain(utils.getToken(userAdmin.username), userAdmin.domainId)
        userAdminToken = utils.getToken(userAdmin.username)
        userManager = utils.createUser(utils.getToken(userAdmin.username))
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID)
        userManagerDa = createDelegationAgreementInDomain(utils.getToken(userManager.username), userManager.domainId)
        userManagerToken = utils.getToken(userManager.username)
        utils.domainRcnSwitch(userAdmin.domainId, rcn)

        otherUserAdmin = utils.createCloudAccount()
        otherUserAdminDa = createDelegationAgreementInDomain(utils.getToken(otherUserAdmin.username), otherUserAdmin.domainId)
        otherUserManager = utils.createUser(utils.getToken(otherUserAdmin.username))
        utils.addRoleToUser(otherUserManager, Constants.USER_MANAGE_ROLE_ID)
        otherUserManagerDa = createDelegationAgreementInDomain(utils.getToken(otherUserManager.username), otherUserManager.domainId)
        utils.domainRcnSwitch(otherUserAdmin.domainId, rcn)

        userGroupUserAdminDomain = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroupUserAdminDomain)
        def userGroupDaData = new DelegationAgreement().with {
            it.name = "DA for management access testing"
            it.domainId = userAdmin.domainId
            it.principalId = userGroupUserAdminDomain.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def response = cloud20.createDelegationAgreement(utils.getToken(userAdmin.username), userGroupDaData)
        assert (response.status == 201)
        userGroupUserAdminDomainDa = response.getEntity(DelegationAgreement)

        userGroupOtherUserAdminDomain = utils.createUserGroup(otherUserAdmin.domainId)
        utils.addUserToUserGroup(otherUserAdmin.id, userGroupOtherUserAdminDomain)
        userGroupDaData = new DelegationAgreement().with {
            it.name = "DA for management access testing"
            it.domainId = otherUserAdmin.domainId
            it.principalId = userGroupOtherUserAdminDomain.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        response = cloud20.createDelegationAgreement(utils.getToken(otherUserAdmin.username), userGroupDaData)
        assert (response.status == 201)
        userGroupOtherUserAdminDomainDa = response.getEntity(DelegationAgreement)
    }

    def "RCN admins have management access to a DA within their RCN"() {
        given:
        def dasRcnAdminsCanManage = [userAdminDa, userManagerDa, otherUserAdminDa, otherUserManagerDa, userGroupUserAdminDomainDa, userGroupOtherUserAdminDomainDa] as List<DelegationAgreement>

        when: "add user to DAs with management access"
        def addResponses = []
        def userToAdd = utils.createUser(utils.getToken(userAdmin.username))
        def federatedUserToAdd = utils.createFederatedUser(userAdmin.domainId)
        def userGroupToAdd = utils.createUserGroup(userAdmin.domainId)
        dasRcnAdminsCanManage.each { daToManage ->
            addResponses << cloud20.addUserDelegate(rcnAdminToken, daToManage.id, userToAdd.id)
            addResponses << cloud20.addUserDelegate(rcnAdminToken, daToManage.id, federatedUserToAdd.id)
            addResponses << cloud20.addUserGroupDelegate(rcnAdminToken, daToManage.id, userGroupToAdd.id)
        }

        then:
        addResponses.each { responseToValidate ->
            assert responseToValidate.status == 204
        }

        when: "list delegates on DAs with management access"
        def listResponses = []
        dasRcnAdminsCanManage.each { daToManage ->
            listResponses<< cloud20.listDelegates(rcnAdminToken, daToManage.id)
        }

        then:
        listResponses.each { responseToValidate ->
            assert responseToValidate.status == 200
        }

        when: "delete delegates on DAs with management access"
        def deleteResponses = []
        dasRcnAdminsCanManage.each { daToManage ->
            deleteResponses << cloud20.deleteUserDelegate(rcnAdminToken, daToManage.id, userToAdd.id)
            deleteResponses << cloud20.deleteUserGroupDelegate(rcnAdminToken, daToManage.id, userGroupToAdd.id)
        }

        then:
        deleteResponses.each { responseToValidate ->
            assert responseToValidate.status == 204
        }
    }

    def "user admins have management access to a DA within their domain"() {
        given:
        def dasUserAdminsCanManage = [userAdminDa, userManagerDa, userGroupUserAdminDomainDa] as List<DelegationAgreement>
        def dasUserAdminsCannotManage = [otherUserAdminDa, otherUserManagerDa, userGroupOtherUserAdminDomainDa] as List<DelegationAgreement>

        when: "add user to DAs with management access"
        def addResponsesCanManage = []
        def addResponsesCannotManage = []
        def userToAdd = utils.createUser(utils.getToken(userAdmin.username))
        def federatedUserToAdd = utils.createFederatedUser(userAdmin.domainId)
        def userGroupToAdd = utils.createUserGroup(userAdmin.domainId)
        dasUserAdminsCanManage.each { daToManage ->
            addResponsesCanManage << cloud20.addUserDelegate(userAdminToken, daToManage.id, userToAdd.id)
            addResponsesCanManage << cloud20.addUserDelegate(userAdminToken, daToManage.id, federatedUserToAdd.id)
            addResponsesCanManage << cloud20.addUserGroupDelegate(userAdminToken, daToManage.id, userGroupToAdd.id)
        }
        dasUserAdminsCannotManage.each { daToManage ->
            addResponsesCannotManage << cloud20.addUserDelegate(userAdminToken, daToManage.id, userToAdd.id)
            addResponsesCannotManage << cloud20.addUserDelegate(userAdminToken, daToManage.id, federatedUserToAdd.id)
            addResponsesCannotManage << cloud20.addUserGroupDelegate(userAdminToken, daToManage.id, userGroupToAdd.id)
        }

        then:
        addResponsesCanManage.each { responseToValidate ->
            assert responseToValidate.status == 204
        }
        addResponsesCannotManage.each { responseToValidate ->
            assert responseToValidate.status == 404
        }

        when: "list delegates on DAs with management access"
        def listResponsesCanManage = []
        def listResponsesCannotManage = []
        dasUserAdminsCanManage.each { daToManage ->
            listResponsesCanManage << cloud20.listDelegates(userAdminToken, daToManage.id)
        }
        dasUserAdminsCannotManage.each { daToManage ->
            listResponsesCannotManage<< cloud20.listDelegates(userAdminToken, daToManage.id)
        }

        then:
        listResponsesCanManage.each { responseToValidate ->
            assert responseToValidate.status == 200
        }
        listResponsesCannotManage.each { responseToValidate ->
            assert responseToValidate.status == 404
        }

        when: "delete delegates on DAs with management access"
        def deleteResponsesCanManage = []
        def deleteResponsesCannotManage = []
        dasUserAdminsCanManage.each { daToManage ->
            deleteResponsesCanManage << cloud20.deleteUserDelegate(userAdminToken, daToManage.id, userToAdd.id)
            deleteResponsesCanManage << cloud20.deleteUserGroupDelegate(userAdminToken, daToManage.id, userGroupToAdd.id)
        }
        dasUserAdminsCannotManage.each { daToManage ->
            deleteResponsesCannotManage << cloud20.deleteUserDelegate(userAdminToken, daToManage.id, userToAdd.id)
            deleteResponsesCannotManage << cloud20.deleteUserGroupDelegate(userAdminToken, daToManage.id, userGroupToAdd.id)
        }

        then:
        deleteResponsesCanManage.each { responseToValidate ->
            assert responseToValidate.status == 204
        }
        deleteResponsesCannotManage.each { responseToValidate ->
            assert responseToValidate.status == 404
        }
    }

    def "user managers have management access to a DA within their domain except for DAs for the user admin of their domain"() {
        given:
        def dasUserAdminsCanManage = [userManagerDa, userGroupUserAdminDomainDa] as List<DelegationAgreement>
        def dasUserAdminsCannotManage = [userAdminDa, otherUserAdminDa, otherUserManagerDa, userGroupOtherUserAdminDomainDa] as List<DelegationAgreement>

        when: "add user to DAs with management access"
        def addResponsesCanManage = []
        def addResponsesCannotManage = []
        def userToAdd = utils.createUser(utils.getToken(userAdmin.username))
        def federatedUserToAdd = utils.createFederatedUser(userAdmin.domainId)
        def userGroupToAdd = utils.createUserGroup(userAdmin.domainId)
        dasUserAdminsCanManage.each { daToManage ->
            addResponsesCanManage << cloud20.addUserDelegate(userManagerToken, daToManage.id, userToAdd.id)
            addResponsesCanManage << cloud20.addUserDelegate(userManagerToken, daToManage.id, federatedUserToAdd.id)
            addResponsesCanManage << cloud20.addUserGroupDelegate(userManagerToken, daToManage.id, userGroupToAdd.id)
        }
        dasUserAdminsCannotManage.each { daToManage ->
            addResponsesCannotManage << cloud20.addUserDelegate(userManagerToken, daToManage.id, userToAdd.id)
            addResponsesCannotManage << cloud20.addUserDelegate(userManagerToken, daToManage.id, federatedUserToAdd.id)
            addResponsesCannotManage << cloud20.addUserGroupDelegate(userManagerToken, daToManage.id, userGroupToAdd.id)
        }

        then:
        addResponsesCanManage.each { responseToValidate ->
            assert responseToValidate.status == 204
        }
        addResponsesCannotManage.each { responseToValidate ->
            assert responseToValidate.status == 404
        }

        when: "list delegates on DAs with management access"
        def listResponsesCanManage = []
        def listResponsesCannotManage = []
        dasUserAdminsCanManage.each { daToManage ->
            listResponsesCanManage << cloud20.listDelegates(userManagerToken, daToManage.id)
        }
        dasUserAdminsCannotManage.each { daToManage ->
            listResponsesCannotManage<< cloud20.listDelegates(userManagerToken, daToManage.id)
        }

        then:
        listResponsesCanManage.each { responseToValidate ->
            assert responseToValidate.status == 200
        }
        listResponsesCannotManage.each { responseToValidate ->
            assert responseToValidate.status == 404
        }

        when: "delete delegates on DAs with management access"
        def deleteResponsesCanManage = []
        def deleteResponsesCannotManage = []
        dasUserAdminsCanManage.each { daToManage ->
            deleteResponsesCanManage << cloud20.deleteUserDelegate(userManagerToken, daToManage.id, userToAdd.id)
            deleteResponsesCanManage << cloud20.deleteUserGroupDelegate(userManagerToken, daToManage.id, userGroupToAdd.id)
        }
        dasUserAdminsCannotManage.each { daToManage ->
            deleteResponsesCannotManage << cloud20.deleteUserDelegate(userManagerToken, daToManage.id, userToAdd.id)
            deleteResponsesCannotManage << cloud20.deleteUserGroupDelegate(userManagerToken, daToManage.id, userGroupToAdd.id)
        }

        then:
        deleteResponsesCanManage.each { responseToValidate ->
            assert responseToValidate.status == 204
        }
        deleteResponsesCannotManage.each { responseToValidate ->
            assert responseToValidate.status == 404
        }
    }

    def "RCN admins can perform CRUD operations on DAs within their RCN"() {
        given:
        def entitiesWithManagementAccessOver = [[userAdmin, PrincipalType.USER],
                                                [userManager, PrincipalType.USER],
                                                [otherUserAdmin, PrincipalType.USER],
                                                [otherUserManager, PrincipalType.USER],
                                                [userGroupUserAdminDomain, PrincipalType.USER_GROUP],
                                                [userGroupOtherUserAdminDomain, PrincipalType.USER_GROUP]]
        when: "create DAs for all entities"
        def delegationAgreements = []
        def createResponses = []
        for (entityData in entitiesWithManagementAccessOver) {
            def da = new DelegationAgreement().with {
                it.name = "Test DA name"
                it.domainId = domainId
                it.principalType = entityData[1]
                it.principalId = entityData[0].id
                it
            }
            createResponses << cloud20.createDelegationAgreement(rcnAdminToken, da)
        }

        then:
        for (response in createResponses) {
            assert response.status == 201
            delegationAgreements << response.getEntity(DelegationAgreement)
        }

        when: "get DAs"
        def getResponses = []
        for (da in delegationAgreements) {
            getResponses << cloud20.getDelegationAgreement(rcnAdminToken, da.id)
        }

        then:
        for (response in getResponses) {
            assert response.status == 200
        }

        when: "update the DAs"
        def updateResponses = []
        for (da in delegationAgreements) {
            da.name = RandomStringUtils.randomAlphanumeric(8)
            da.subAgreementNestLevel = null
            updateResponses << cloud20.updateDelegationAgreement(rcnAdminToken, da.id, da)
        }

        then:
        for (response in updateResponses) {
            assert response.status == 200
        }

        when: "delete the DAs"
        def deleteResponses = []
        for (da in delegationAgreements) {
            deleteResponses << cloud20.deleteDelegationAgreement(rcnAdminToken, da.id)
        }

        then:
        for (response in deleteResponses) {
            assert response.status == 204
        }
    }

    def "user admins can perform CRUD operations on DAs within their domain"() {
        given:
        def entitiesWithManagementAccessOver = [[userAdmin, PrincipalType.USER],
                                                [userManager, PrincipalType.USER],
                                                [userGroupUserAdminDomain, PrincipalType.USER_GROUP]]
        def entitiesWithoutManagementAccessOver = [[otherUserAdmin, PrincipalType.USER],
                                                   [otherUserManager, PrincipalType.USER],
                                                   [userGroupOtherUserAdminDomain, PrincipalType.USER_GROUP]]
        def delegationAgreementsWithoutAccess = [otherUserAdminDa, otherUserManagerDa, userGroupOtherUserAdminDomainDa]

        when: "create DAs for all entities"
        def delegationAgreements = []
        def createResponsesWithManagementAccessOver = []
        def createResponsesWithoutManagementAccessOver = []
        for (entityData in entitiesWithManagementAccessOver) {
            def da = new DelegationAgreement().with {
                it.name = "Test DA name"
                it.domainId = domainId
                it.principalType = entityData[1]
                it.principalId = entityData[0].id
                it
            }
            createResponsesWithManagementAccessOver << cloud20.createDelegationAgreement(userAdminToken, da)
        }
        for (entityData in entitiesWithoutManagementAccessOver) {
            def da = new DelegationAgreement().with {
                it.name = "Test DA name"
                it.domainId = domainId
                it.principalType = entityData[1]
                it.principalId = entityData[0].id
                it
            }
            createResponsesWithoutManagementAccessOver << cloud20.createDelegationAgreement(userAdminToken, da)
        }

        then:
        for (response in createResponsesWithManagementAccessOver) {
            assert response.status == 201
            delegationAgreements << response.getEntity(DelegationAgreement)
        }
        for (response in createResponsesWithoutManagementAccessOver) {
            assert response.status == 404
        }

        when: "get DAs"
        def getResponses = []
        def getResponsesWithoutAccess = []
        for (da in delegationAgreements) {
            getResponses << cloud20.getDelegationAgreement(userAdminToken, da.id)
        }
        for (da in delegationAgreementsWithoutAccess) {
            getResponsesWithoutAccess << cloud20.getDelegationAgreement(userAdminToken, da.id)
        }

        then:
        for (response in getResponses) {
            assert response.status == 200
        }
        for (response in getResponsesWithoutAccess) {
            assert response.status == 404
        }

        when: "update the DAs"
        def updateResponses = []
        def updateResponsesWithoutAccess = []
        for (da in delegationAgreements) {
            da.name = RandomStringUtils.randomAlphanumeric(8)
            da.subAgreementNestLevel = null
            updateResponses << cloud20.updateDelegationAgreement(userAdminToken, da.id, da)
        }
        for (da in delegationAgreementsWithoutAccess) {
            da.name = RandomStringUtils.randomAlphanumeric(8)
            da.subAgreementNestLevel = null
            updateResponsesWithoutAccess << cloud20.updateDelegationAgreement(userAdminToken, da.id, da)
        }

        then:
        for (response in updateResponses) {
            assert response.status == 200
        }
        for (response in updateResponsesWithoutAccess) {
            assert response.status == 404
        }

        when: "delete the DAs"
        def deleteResponses = []
        def deleteResponsesWithoutAccess = []
        for (da in delegationAgreements) {
            deleteResponses << cloud20.deleteDelegationAgreement(userAdminToken, da.id)
        }
        for (da in delegationAgreementsWithoutAccess) {
            deleteResponsesWithoutAccess << cloud20.deleteDelegationAgreement(userAdminToken, da.id)
        }

        then:
        for (response in deleteResponses) {
            assert response.status == 204
        }
        for (response in deleteResponsesWithoutAccess) {
            assert response.status == 404
        }
    }

    def "user managers can perform CRUD operations on DAs within their domain that do not have a user admin as the principal"() {
        given:
        def entitiesWithManagementAccessOver = [[userManager, PrincipalType.USER],
                                                [userGroupUserAdminDomain, PrincipalType.USER_GROUP]]
        def entitiesWithoutManagementAccessOver = [[userAdmin, PrincipalType.USER],
                                                   [otherUserAdmin, PrincipalType.USER],
                                                   [otherUserManager, PrincipalType.USER],
                                                   [userGroupOtherUserAdminDomain, PrincipalType.USER_GROUP]]
        def delegationAgreementsWithoutAccess = [userAdminDa, otherUserAdminDa, otherUserManagerDa, userGroupOtherUserAdminDomainDa]

        when: "create DAs for all entities"
        def delegationAgreements = []
        def createResponsesWithManagementAccessOver = []
        def createResponsesWithoutManagementAccessOver = []
        for (entityData in entitiesWithManagementAccessOver) {
            def da = new DelegationAgreement().with {
                it.name = "Test DA name"
                it.domainId = domainId
                it.principalType = entityData[1]
                it.principalId = entityData[0].id
                it
            }
            createResponsesWithManagementAccessOver << cloud20.createDelegationAgreement(userManagerToken, da)
        }
        for (entityData in entitiesWithoutManagementAccessOver) {
            def da = new DelegationAgreement().with {
                it.name = "Test DA name"
                it.domainId = domainId
                it.principalType = entityData[1]
                it.principalId = entityData[0].id
                it
            }
            createResponsesWithoutManagementAccessOver << cloud20.createDelegationAgreement(userManagerToken, da)
        }

        then:
        for (response in createResponsesWithManagementAccessOver) {
            assert response.status == 201
            delegationAgreements << response.getEntity(DelegationAgreement)
        }
        for (response in createResponsesWithoutManagementAccessOver) {
            assert response.status == 404
        }

        when: "get DAs"
        def getResponses = []
        def getResponsesWithoutAccess = []
        for (da in delegationAgreements) {
            getResponses << cloud20.getDelegationAgreement(userManagerToken, da.id)
        }
        for (da in delegationAgreementsWithoutAccess) {
            getResponsesWithoutAccess << cloud20.getDelegationAgreement(userManagerToken, da.id)
        }

        then:
        for (response in getResponses) {
            assert response.status == 200
        }
        for (response in getResponsesWithoutAccess) {
            assert response.status == 404
        }

        when: "update the DAs"
        def updateResponses = []
        def updateResponsesWithoutAccess = []
        for (da in delegationAgreements) {
            da.name = RandomStringUtils.randomAlphanumeric(8)
            da.subAgreementNestLevel = null
            updateResponses << cloud20.updateDelegationAgreement(userManagerToken, da.id, da)
        }
        for (da in delegationAgreementsWithoutAccess) {
            da.name = RandomStringUtils.randomAlphanumeric(8)
            da.subAgreementNestLevel = null
            updateResponsesWithoutAccess << cloud20.updateDelegationAgreement(userManagerToken, da.id, da)
        }

        then:
        for (response in updateResponses) {
            assert response.status == 200
        }
        for (response in updateResponsesWithoutAccess) {
            assert response.status == 404
        }

        when: "delete the DAs"
        def deleteResponses = []
        def deleteResponsesWithoutAccess = []
        for (da in delegationAgreements) {
            deleteResponses << cloud20.deleteDelegationAgreement(userManagerToken, da.id)
        }
        for (da in delegationAgreementsWithoutAccess) {
            deleteResponsesWithoutAccess << cloud20.deleteDelegationAgreement(userManagerToken, da.id)
        }

        then:
        for (response in deleteResponses) {
            assert response.status == 204
        }
        for (response in deleteResponsesWithoutAccess) {
            assert response.status == 404
        }
    }

    def createDelegationAgreementInDomain(String token, String domainId) {
        def da = new DelegationAgreement().with {
            it.name = "Test DA name"
            it.domainId = domainId
            it
            }
        def response = cloud20.createDelegationAgreement(token, da)
        assert (response.status == 201)
        response.getEntity(DelegationAgreement)
    }

}
