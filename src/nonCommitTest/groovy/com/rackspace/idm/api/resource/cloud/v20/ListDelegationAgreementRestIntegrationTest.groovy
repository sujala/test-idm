package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreements
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.DelegationService
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE
import static org.apache.http.HttpStatus.*

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

        // Update domains to have same ID
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

        // Update domains to have same ID
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

        when: "subUserD1 lists DAs w/o relationship"
        agreements = utils.listDelegationAgreements(subUserD1Token)

        then: "Get 0 agreements"
        agreements.delegationAgreement.size() == 0

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}