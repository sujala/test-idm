package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreements
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static org.apache.http.HttpStatus.SC_OK

class ListDelegationAgreementRestIntegrationTest extends RootIntegrationTest {
    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
    }

    @Unroll
    def "List DAs returns multiple agreements as expected; mediaType = #mediaType"() {
        def commonRcn = "RCN-123-098-234"
        def userAdminD1 = utils.createCloudAccount()
        def userAdminD1Token = utils.getToken(userAdminD1.username)

        def userAdminD2 = utils.createCloudAccount()

        // Update domains to have same RCN
        def rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD1.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD2.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)

        def daToD1ToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it
        }

        // Create 1 agreement
        def daToD1 = utils.createDelegationAgreement(userAdminD1Token, daToD1ToCreate)
        utils.addUserDelegate(userAdminD1Token, daToD1.id, userAdminD2.id)

        // Create 2nd agreement to same user
        def daToD2 = utils.createDelegationAgreement(userAdminD1Token, daToD1ToCreate)
        utils.addUserDelegate(userAdminD1Token, daToD2.id, userAdminD2.id)

        when: "Principal lists DAs"
        def response = cloud20.listDelegationAgreements(userAdminD1Token, null, mediaType)

        then: "Get 200"
        response.status == SC_OK

        and:
        DelegationAgreements entity = response.getEntity(DelegationAgreements)
        entity != null
        entity.delegationAgreement.size() == 2

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "List DAs returns an empty list of agreements when no agreements match; mediaType = #mediaType"() {
        def userAdminD1 = utils.createCloudAccount()
        def userAdminD1Token = utils.getToken(userAdminD1.username)

        when: "Principal lists DAs"
        def response = cloud20.listDelegationAgreements(userAdminD1Token, null, mediaType)

        then: "Get 200"
        response.status == SC_OK

        and:
        DelegationAgreements entity = response.getEntity(DelegationAgreements)
        entity != null
        entity.delegationAgreement.size() == 0

        when: "Identity admin lists DAs"
        response = cloud20.listDelegationAgreements(utils.getIdentityAdminToken())

        then: "Get 200"
        response.status == SC_OK

        and:
        DelegationAgreements entity1 = response.getEntity(DelegationAgreements)
        entity1 != null
        entity1.delegationAgreement.size() == 0

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "List DAs limits results based on relationship query param"() {
        def commonRcn = "RCN-123-098-234"
        def userAdminD1 = utils.createCloudAccount()
        def userAdminD1Token = utils.getToken(userAdminD1.username)
        def subUserD1 = cloud20.createSubUser(userAdminD1Token)
        def subUserD1Token = utils.getToken(subUserD1.username)

        def userAdminD2 = utils.createCloudAccount()
        def userAdminD2Token = utils.getToken(userAdminD2.username)
        def subUserD2 = cloud20.createSubUser(userAdminD2Token)
        def subUserD2Token = utils.getToken(subUserD2.username)

        // Update domains to have same RCN
        def rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD1.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD2.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it
        }

        // Create 1 agreement from d1 user admin to d2 subuser
        def da_userAdminD1ToSubUserD2 = utils.createDelegationAgreement(userAdminD1Token, daToCreate)
        utils.addUserDelegate(userAdminD1Token, da_userAdminD1ToSubUserD2.id, subUserD2.id)

        // Create 1 agreement from d2 user admin to d1 useradmin
        def da_userAdminD2ToUserAdminD1 = utils.createDelegationAgreement(userAdminD2Token, daToCreate)
        utils.addUserDelegate(userAdminD2Token, da_userAdminD2ToUserAdminD1.id, userAdminD1.id)

        when: "userAdminD1 lists DAs w/o relationship"
        DelegationAgreements agreements = utils.listDelegationAgreements(userAdminD1Token, null)

        then: "Get 2 agreements"
        agreements.delegationAgreement.size() == 2

        when: "userAdminD1 lists DAs w/ relationship = principal"
        agreements = utils.listDelegationAgreements(userAdminD1Token, "principal")

        then: "Get 1 agreement"
        agreements.delegationAgreement.size() == 1
        agreements.delegationAgreement[0].id == da_userAdminD1ToSubUserD2.id

        when: "userAdminD1 lists DAs w/ relationship = delegate"
        agreements = utils.listDelegationAgreements(userAdminD1Token, "delegate")

        then: "Get 1 agreement"
        agreements.delegationAgreement.size() == 1
        agreements.delegationAgreement[0].id == da_userAdminD2ToUserAdminD1.id

        when: "userAdminD2 lists DAs w/o relationship"
        agreements = utils.listDelegationAgreements(userAdminD2Token)

        then: "Get 1 agreement"
        agreements.delegationAgreement.size() == 1
        agreements.delegationAgreement[0].id == da_userAdminD2ToUserAdminD1.id

        when: "userAdminD2 lists DAs w/ relationship = delegate"
        agreements = utils.listDelegationAgreements(userAdminD2Token, "delegate")

        then: "Get 0 agreements"
        agreements.delegationAgreement.size() == 0

        when: "subUserD1 lists DAs w/o relationship"
        agreements = utils.listDelegationAgreements(subUserD1Token)

        then: "Get 0 agreements"
        agreements.delegationAgreement.size() == 0

        when: "subUserD2 lists DAs w/ relationship = principal"
        agreements = utils.listDelegationAgreements(subUserD2Token, "principal")

        then: "Get 0 agreements"
        agreements.delegationAgreement.size() == 0

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "List DAs returns nested agreement attributes; mediaType = #mediaType"() {
        reloadableConfiguration.setProperty(IdentityConfig.DELEGATION_MAX_NEST_LEVEL_PROP, 5)

        def commonRcn = "RCN-123-098-234"
        def userAdminD1 = utils.createCloudAccount()
        def userAdminD1Token = utils.getToken(userAdminD1.username)

        def userAdminD2 = utils.createCloudAccount()
        def userAdminD2Token = utils.getToken(userAdminD2.username)
        def d2SubUser = cloud20.createSubUser(userAdminD2Token)

        // Update domains to have same ID
        def rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD1.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD2.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)

        // Create DA to give user in D2 delegate access to D1
        def daToD1ToCreate = new DelegationAgreement().with {
            it.name = "parent"
            it.subAgreementNestLevel = BigInteger.valueOf(4)
            it
        }
        def daToD1Parent = utils.createDelegationAgreement(userAdminD1Token, daToD1ToCreate)
        utils.addUserDelegate(userAdminD1Token, daToD1Parent.id, userAdminD2.id)

        // Create nested agreement w/ d2 useradmin as the principal
        def daToD1Nested2ToCreate = new DelegationAgreement().with {
            it.name = "nested2"
            it.subAgreementNestLevel = BigInteger.valueOf(2)
            it.parentDelegationAgreementId = daToD1Parent.id
            it
        }
        def daToD1Nested2 = utils.createDelegationAgreement(userAdminD2Token, daToD1Nested2ToCreate)
        utils.addUserDelegate(userAdminD2Token, daToD1Nested2.id, d2SubUser.id)

        // Create nested agreement w/ d2 useradmin as the principal
        def daToD1Nested3ToCreate = new DelegationAgreement().with {
            it.name = "nested3"
            it.subAgreementNestLevel = BigInteger.valueOf(3)
            it.parentDelegationAgreementId = daToD1Parent.id
            it
        }
        def daToD1Nested3 = utils.createDelegationAgreement(userAdminD2Token, daToD1Nested3ToCreate)
        utils.addUserDelegate(userAdminD2Token, daToD1Nested3.id, d2SubUser.id)


        when: "List DAs for user-admin in d2"
        def response2 = cloud20.listDelegationAgreements(userAdminD2Token, null, mediaType)

        then: "Returns ok"
        response2.status == SC_OK
        DelegationAgreements entity2 = response2.getEntity(DelegationAgreements)
        entity2 != null
        entity2.delegationAgreement.size() == 3 // parent as delegate, nested as principal

        and: "parent da returned w/ nest fields"
        def parentDa = entity2.delegationAgreement.find { it.name == daToD1Parent.name}
        parentDa.subAgreementNestLevel == daToD1Parent.subAgreementNestLevel
        parentDa.allowSubAgreements
        parentDa.parentDelegationAgreementId == null

        and: "first nested da returned w/ nest fields"
        def nestedDa2 = entity2.delegationAgreement.find { it.name == daToD1Nested2.name}
        nestedDa2.subAgreementNestLevel == daToD1Nested2.subAgreementNestLevel
        nestedDa2.allowSubAgreements
        nestedDa2.parentDelegationAgreementId == parentDa.id

        and: "other nested da returned w/ nest fields"
        def nestedDa3 = entity2.delegationAgreement.find { it.name == daToD1Nested3.name}
        nestedDa3.subAgreementNestLevel == daToD1Nested3.subAgreementNestLevel
        nestedDa3.allowSubAgreements
        nestedDa3.parentDelegationAgreementId == parentDa.id

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "List DAs using authorized users; mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.DELEGATION_MAX_NEST_LEVEL_PROP, 5)

        def commonRcn = testUtils.getRandomRCN()

        // Create users
        def rcnAdmin = utils.createCloudAccount()
        def userAdminD1 = utils.createCloudAccount()
        def userAdminD1Token = utils.getToken(userAdminD1.username)
        def fedUserResponse = utils.authenticateFederatedUser(userAdminD1.domainId)
        def userAdminD2 = utils.createCloudAccount()
        def userAdminD2Token = utils.getToken(userAdminD2.username)
        def userManageD2 = cloud20.createSubUser(userAdminD2Token)
        utils.addRoleToUser(userManageD2, Constants.USER_MANAGE_ROLE_ID)
        def subUserD2 = cloud20.createSubUser(userAdminD2Token)
        utils.addRoleToUser(subUserD2, Constants.ROLE_RBAC1_ID)

        // Update domains to have same RCN
        utils.domainRcnSwitch(userAdminD1.domainId, commonRcn)
        utils.domainRcnSwitch(userAdminD2.domainId, commonRcn)
        utils.domainRcnSwitch(rcnAdmin.domainId, commonRcn)

        // Get tokens
        def fedUserToken = fedUserResponse.token.id
        def userManageD2Token = utils.getToken(userManageD2.username)
        def subUserD2Token = utils.getToken(subUserD2.username)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdmin.username)

        // Create agreement using fed user token
        def daD1FedUserCreate = new DelegationAgreement().with {
            it.name = "fedUserDa"
            it
        }
        def daD1FedUser = utils.createDelegationAgreement(fedUserToken, daD1FedUserCreate)

        // Create DA to give user in D2 delegate access to D1
        def daToD1ToCreate = new DelegationAgreement().with {
            it.name = "parent"
            it.subAgreementNestLevel = BigInteger.valueOf(4)
            it
        }
        def daToD1Parent = utils.createDelegationAgreement(userAdminD1Token, daToD1ToCreate)
        utils.addUserDelegate(userAdminD1Token, daToD1Parent.id, userAdminD2.id)

        // Create nested agreement w/ d2 useradmin as the principal
        def daToD1Nested2ToCreate = new DelegationAgreement().with {
            it.name = "nested2"
            it.subAgreementNestLevel = BigInteger.valueOf(2)
            it.parentDelegationAgreementId = daToD1Parent.id
            it
        }
        def daToD1Nested2 = utils.createDelegationAgreement(userAdminD2Token, daToD1Nested2ToCreate)
        utils.addUserDelegate(userAdminD2Token, daToD1Nested2.id, subUserD2.id)

        // Create agreement using sub user token
        def daD2SubUserCreate = new DelegationAgreement().with {
            it.name = "subUserDa"
            it
        }
        def daSubUser = utils.createDelegationAgreement(subUserD2Token, daD2SubUserCreate)

        when: "List DAs using rcnAdmin's token"
        def response = cloud20.listDelegationAgreements(rcnAdminToken, null, mediaType)
        DelegationAgreements entity = response.getEntity(DelegationAgreements)

        then: "Returns ok"
        response.status == SC_OK

        entity != null
        entity.delegationAgreement.size() == 4 // All DAs under RCN

        when: "List DAs using userAdminD1's token"
        response = cloud20.listDelegationAgreements(userAdminD1Token, null, mediaType)
        entity = response.getEntity(DelegationAgreements)

        then: "Returns ok"
        response.status == SC_OK

        entity != null
        entity.delegationAgreement.size() == 2 // All DAs under principalDomainId matches the userAdminD1's domainId.
        entity.delegationAgreement.find {it.id == daToD1Parent.id && it.principalDomainId == userAdminD1.domainId} != null
        entity.delegationAgreement.find {it.id == daD1FedUser.id && it.principalDomainId == userAdminD1.domainId} != null

        when: "List DAs using userAdminD2's token"
        response = cloud20.listDelegationAgreements(userAdminD2Token, null, mediaType)
        entity = response.getEntity(DelegationAgreements)

        then: "Returns ok"
        response.status == SC_OK

        entity != null
        entity.delegationAgreement.size() == 3 // All DAs userAdminD2 can list
        entity.delegationAgreement.find {it.id == daToD1Parent.id && it.principalDomainId == userAdminD1.domainId} != null
        entity.delegationAgreement.find {it.id == daToD1Nested2.id && it.principalDomainId == userAdminD2.domainId} != null
        entity.delegationAgreement.find {it.id == daSubUser.id && it.principalDomainId == subUserD2.domainId} != null

        when: "List DAs using userManageD2's token"
        response = cloud20.listDelegationAgreements(userManageD2Token, null, mediaType)
        entity = response.getEntity(DelegationAgreements)

        then: "Returns ok"
        response.status == SC_OK

        entity != null
        entity.delegationAgreement.size() == 1 // All DAs under userManageD2's domain except for those which principal is a user-admin.
        entity.delegationAgreement.find {it.id == daSubUser.id && it.principalDomainId == subUserD2.domainId} != null

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "user delegate can list DA where principal is a user-admin; mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.DELEGATION_MAX_NEST_LEVEL_PROP, 5)

        // Create users and tokens
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def userManage = cloud20.createSubUser(userAdminToken)
        utils.addRoleToUser(userManage, Constants.USER_MANAGE_ROLE_ID)
        def userManageToken = utils.getToken(userManage.username)
        def subUser = cloud20.createSubUser(userAdminToken)
        def subUserToken = utils.getToken(subUser.username)
        def fedUserResponse = utils.authenticateFederatedUser(userAdmin.domainId)
        def fedUserToken = fedUserResponse.token.id

        // Create DA to userAdmin token
        def daUACreate = new DelegationAgreement().with {
            it.name = "userAdminDA"
            it
        }
        def daUA = utils.createDelegationAgreement(userAdminToken, daUACreate)

        // Add delegates
        utils.addUserDelegate(userAdminToken, daUA.id, userManage.id)
        utils.addUserDelegate(userAdminToken, daUA.id, subUser.id)
        utils.addUserDelegate(userAdminToken, daUA.id, fedUserResponse.user.id)

        when: "List DAs using user manager's token"
        def response = cloud20.listDelegationAgreements(userManageToken, null, mediaType)
        DelegationAgreements entity = response.getEntity(DelegationAgreements)

        then: "Returns ok"
        response.status == SC_OK

        entity != null
        entity.delegationAgreement.size() == 1
        entity.delegationAgreement.find {it.id == daUA.id && it.principalDomainId == userAdmin.domainId} != null

        when: "List DAs using default user's token"
        response = cloud20.listDelegationAgreements(userManageToken, null, mediaType)
        entity = response.getEntity(DelegationAgreements)

        then: "Returns ok"
        response.status == SC_OK

        entity != null
        entity.delegationAgreement.size() == 1
        entity.delegationAgreement.find {it.id == daUA.id && it.principalDomainId == userAdmin.domainId} != null

        when: "List DAs using federated user's token"
        response = cloud20.listDelegationAgreements(userManageToken, null, mediaType)
        entity = response.getEntity(DelegationAgreements)

        then: "Returns ok"
        response.status == SC_OK

        entity != null
        entity.delegationAgreement.size() == 1
        entity.delegationAgreement.find {it.id == daUA.id && it.principalDomainId == userAdmin.domainId} != null

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}