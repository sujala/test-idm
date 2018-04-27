package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateReferences
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static org.apache.http.HttpStatus.SC_OK

class ListDelegatesRestIntegrationTest extends RootIntegrationTest {

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
    }

    @Unroll
    def "List delegates returns multiple delegates as expected; mediaType = #mediaType"() {
        def commonRcn = "RCN-543-495-920"
        def userAdminD1 = utils.createCloudAccount()
        def userAdminD1Token = utils.getToken(userAdminD1.username)

        def userAdminD2 = utils.createCloudAccount()

        // Create a user group in domain 2
        UserGroup domain2UserGroup = utils.createUserGroup(userAdminD2.domainId)

        // Update domains to have same ID
        def rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD1.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), userAdminD2.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)

        def samlRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdminD2.domainId
            it.validitySeconds = 100
            it.brokerIssuer = Constants.DEFAULT_BROKER_IDP_URI
            it.originIssuer = Constants.IDP_V2_DOMAIN_URI
            it.email = Constants.DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = UUID.randomUUID().toString()
            it.roleNames = [] as Set
            it.groupNames = [domain2UserGroup.getName()] as Set
            it
        }

        // Create a fed user through v2 that adds user to group
        AuthenticateResponse fedUserAuthResponse = utils.authenticateV2FederatedUser(samlRequest)

        // Create 1 agreement w/ provisioned user, fed user, and user group
        def daToD1ToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it
        }
        def daToD1 = utils.createDelegationAgreement(userAdminD1Token, daToD1ToCreate)
        utils.addUserDelegate(userAdminD1Token, daToD1.id, userAdminD2.id)
        utils.addUserDelegate(userAdminD1Token, daToD1.id, fedUserAuthResponse.user.id)
        utils.addUserGroupDelegate(userAdminD1Token, daToD1.id, domain2UserGroup.id)

        when: "Principal lists delegates"
        def response = cloud20.listDelegates(userAdminD1Token, daToD1.id, mediaType)

        then: "Get 200"
        response.status == SC_OK

        and:
        DelegateReferences entity = response.getEntity(DelegateReferences)
        entity != null
        entity.delegateReference.size() == 3
        entity.delegateReference.find {it.delegateId == userAdminD2.id && it.delegateType == DelegateType.USER} != null
        entity.delegateReference.find {it.delegateId == fedUserAuthResponse.user.id && it.delegateType == DelegateType.USER} != null
        entity.delegateReference.find {it.delegateId == domain2UserGroup.id && it.delegateType == DelegateType.USER_GROUP} != null

        when: "Fed user delegate lists delegates"
        def response2 = cloud20.listDelegates(fedUserAuthResponse.token.id, daToD1.id, mediaType)

        then: "Get 200"
        response2.status == SC_OK

        and:
        DelegateReferences entity2 = response2.getEntity(DelegateReferences)
        entity2 != null
        entity2.delegateReference.size() == 3
        entity2.delegateReference.find {it.delegateId == userAdminD2.id && it.delegateType == DelegateType.USER} != null
        entity2.delegateReference.find {it.delegateId == fedUserAuthResponse.user.id && it.delegateType == DelegateType.USER} != null
        entity2.delegateReference.find {it.delegateId == domain2UserGroup.id && it.delegateType == DelegateType.USER_GROUP} != null

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "List delegates returns an empty list of agreements when no agreements match; mediaType = #mediaType"() {
        def userAdminD1 = utils.createCloudAccount()
        def userAdminD1Token = utils.getToken(userAdminD1.username)

        // Create DA w/o any delegates
        def daToD1ToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it
        }
        def daToD1 = utils.createDelegationAgreement(userAdminD1Token, daToD1ToCreate)

        when: "Principal lists delegates"
        def response = cloud20.listDelegates(userAdminD1Token, daToD1.id, mediaType)

        then: "Get 200"
        response.status == SC_OK

        and:
        DelegateReferences entity = response.getEntity(DelegateReferences)
        entity != null
        entity.delegateReference.size() == 0

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}