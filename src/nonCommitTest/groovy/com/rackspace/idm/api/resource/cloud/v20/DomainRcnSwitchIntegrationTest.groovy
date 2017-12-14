package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.validation.Validator20
import org.apache.commons.lang3.RandomStringUtils
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.v2.BadRequestFault
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

class DomainRcnSwitchIntegrationTest extends RootIntegrationTest {

    def "test switch domain RCN happy path"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def originRcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(domainId, originRcn)
        def destinationRcn = testUtils.getRandomRCN()
        utils.addRoleToUser(userAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def defaultUserWithRcnRole = utils.createUser(utils.getToken(userAdmin.username))
        utils.addRoleToUser(defaultUserWithRcnRole, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        def defaultUserWithoutRcnRole = utils.createUser(utils.getToken(userAdmin.username))

        when: "switch the domain RCN"
        resetCloudFeedsMock()
        def response = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), domainId, destinationRcn)

        then: "success"
        response.status == 204

        and: "domain was updated"
        def updatedDomain = utils.getDomain(domainId)
        updatedDomain.rackspaceCustomerNumber == destinationRcn

        and: "user admin had the rcn:admin role removed"
        utils.listUserGlobalRoles(utils.getServiceAdminToken(), userAdmin.id).role.find { role -> role.name == Constants.RCN_ADMIN_ROLE_NAME} == null

        and: "user update feed event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        and: "default user had the rcn cloud role removed"
        utils.listUserGlobalRoles(utils.getServiceAdminToken(), defaultUserWithRcnRole.id).role.find { role -> role.name == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_NAME} == null

        and: "user update feed event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(defaultUserWithRcnRole, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        and: "no update feed event was posted for the user w/o an RCN role"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(defaultUserWithoutRcnRole, EventType.UPDATE),
                VerificationTimes.exactly(0)
        )

        when: "list domains"
        def originRcnDomainsResponse = cloud20.getAccessibleDomains(utils.getServiceAdminToken(), null, null, originRcn)
        def destRcnDomainsResponse  = cloud20.getAccessibleDomains(utils.getServiceAdminToken(), null, null, destinationRcn)

        then: "the domain is no longer in the origin RCN"
        def domainsInOriginRcn = originRcnDomainsResponse.getEntity(Domains)
        domainsInOriginRcn.domain.find { d -> d.id == domainId } == null

        and: "the domain is in the destination RCN"
        def domainsInDestRcn = destRcnDomainsResponse.getEntity(Domains)
        domainsInDestRcn.domain.find { d -> d.id == domainId } != null

        cleanup:
        utils.deleteUser(defaultUserWithRcnRole)
        utils.deleteUser(defaultUserWithoutRcnRole)
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)
    }

    def "test switch RCN to current RCN is a no-op"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def originRcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(domainId, originRcn)
        def destinationRcn = originRcn
        utils.addRoleToUser(userAdmin, Constants.RCN_ADMIN_ROLE_ID)

        when: "switch the domain RCN to the current RCN"
        resetCloudFeedsMock()
        def response = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), domainId, destinationRcn)

        then: "success"
        response.status == 204

        and: "domain was updated"
        def updatedDomain = utils.getDomain(domainId)
        updatedDomain.rackspaceCustomerNumber == destinationRcn

        and: "user admin did NOT have the rcn:admin role removed"
        utils.listUserGlobalRoles(utils.getServiceAdminToken(), userAdmin.id).role.find { role -> role.name == Constants.RCN_ADMIN_ROLE_NAME} != null

        and: "user update feed event was NOT posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(0)
        )

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)
    }

    def "switch domain RCN call returns a 404 when the domain does not exist"() {
        when:
        def response = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), "doesNotExist", testUtils.getRandomRCN())

        then:
        response.status == 404
    }

    def "cannot switch the RCN on a domain containing an RCN tenant"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def originRcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(domainId, originRcn)
        def destinationRcn = testUtils.getRandomRCN()
        def rcnTenant = utils.createTenantWithTypes(RandomStringUtils.randomAlphanumeric(8), [GlobalConstants.TENANT_TYPE_RCN])
        utils.addTenantToDomain(domainId, rcnTenant.id)

        when: "switch the domain RCN"
        resetCloudFeedsMock()
        def response = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), domainId, destinationRcn)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, 400, DefaultCloud20Service.ERROR_SWITCH_RCN_ON_DOMAIN_CONTAINING_RCN_TENANT)

        and: "no user update feed event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(0)
        )

        when: "delete the RCN tenant from the domain and switch the domain RCN"
        utils.deleteTenantFromDomain(domainId, rcnTenant.id)
        response = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), domainId, destinationRcn)

        then: "success"
        response.status == 204

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)
        utils.deleteTenant(rcnTenant)
    }

    def "switch RCN on disabled domain with no users"() {
        given:
        def domainId = utils.createDomain()
        utils.createDomainEntity(domainId)
        def originRcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(domainId, originRcn)
        utils.disableDomain(domainId)
        def destinationRcn = testUtils.getRandomRCN()

        when: "switch the domain RCN"
        resetCloudFeedsMock()
        def response = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), domainId, destinationRcn)

        then: "success"
        response.status == 204

        and: "domain was updated"
        def updatedDomain = utils.getDomain(domainId)
        updatedDomain.rackspaceCustomerNumber == destinationRcn

        and: "no update feed event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(null, EventType.UPDATE),
                VerificationTimes.exactly(0)
        )
    }

    def "users without the required role cannot use the domain rcn switch service"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def originRcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(domainId, originRcn)
        def destinationRcn = testUtils.getRandomRCN()
        def identityAdmin = utils.createIdentityAdmin()

        when: "switch the domain RCN"
        def response = cloud20.domainRcnSwitch(utils.getToken(identityAdmin.username), domainId, destinationRcn)

        then: "error"
        response.status == 403

        when: "add the required role to the user and try again"
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_SWITCH_DOMAIN_RCN_ROLE_ID)
        response = cloud20.domainRcnSwitch(utils.getToken(identityAdmin.username), domainId, destinationRcn)

        then:
        response.status == 204

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUser(identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "cannot switch domain RCN to an invalid RCN"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def originRcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(domainId, originRcn)

        when:
        def destinationRcn = RandomStringUtils.randomAlphanumeric(Validator20.MAX_RCN_LENGTH + 1)
        def response = cloud20.domainRcnSwitch(utils.getServiceAdminToken(), domainId, destinationRcn)

        then:
        response.status == 400

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)
    }

}
