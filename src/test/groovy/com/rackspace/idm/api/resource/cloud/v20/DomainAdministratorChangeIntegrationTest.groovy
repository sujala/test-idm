package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainAdministratorChange
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static com.rackspace.idm.Constants.USER_MANAGE_ROLE_ID
import static org.apache.http.HttpStatus.SC_OK

class DomainAdministratorChangeIntegrationTest extends RootIntegrationTest {
    @Shared
    String serviceAdminToken

    @Shared
    String identityAdminToken

    void doSetupSpec() {
        def response = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert (response.status == SC_OK)
        serviceAdminToken = response.getEntity(AuthenticateResponse).value.token.id

        response = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert (response.status == SC_OK)
        identityAdminToken = response.getEntity(AuthenticateResponse).value.token.id
    }

    def "Caller must have appropriate role"() {
        given:
        def idAdmin = utils.createIdentityAdmin()
        def idAdminToken = utils.getToken(idAdmin.username)

        // Create invalid request. Will fail for authorized users, but will fail differently than for unauthorized
        def promoteAdminChange = new DomainAdministratorChange()

        when: "Try to change admins w/ non-priv'd identity-admin"
        def response = cloud20.changeDomainAdministrator(idAdminToken, "domainx", promoteAdminChange)

        then: "denied"
        response.status == HttpStatus.SC_FORBIDDEN

        when: "Try to change admins w/ non-priv'd service-admin"
        def response2 = cloud20.changeDomainAdministrator(serviceAdminToken, "domainx", promoteAdminChange)

        then: "denied"
        response2.status == HttpStatus.SC_FORBIDDEN

        when: "assign user-admin appropriate role"
        utils.addRoleToUser(idAdmin, Constants.IDENTITY_CHANGE_DOMAIN_ADMIN_ROLE_ID, serviceAdminToken)
        def response3 = cloud20.changeDomainAdministrator(idAdminToken, "domainx", promoteAdminChange)

        then: "now fails with 404"
        response3.status == HttpStatus.SC_BAD_REQUEST
    }

    @Unroll
    def "Can upgrade user-manager and default users: request: #request; accept: #accept"() {
        given:
        def userAdmin = utils.createCloudAccount(identityAdminToken)
        def domainId = userAdmin.domainId
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManage = utils.createUser(userAdminToken, testUtils.getRandomUUID("userManage"), domainId)
        utils.addRoleToUser(userManage, USER_MANAGE_ROLE_ID)
        def userManageToken = utils.getToken(userManage.username)

        def userDefault = utils.createUser(userAdminToken, testUtils.getRandomUUID("userDefault"), domainId)
        def userDefaultToken = utils.getToken(userDefault.username)

        when: "upgrade user-manager"
        def promoteUserManagerChange = createAdminChange(userManage.id, userAdmin.id)
        def userManagePromoteResponse = cloud20.changeDomainAdministrator(identityAdminToken, domainId, promoteUserManagerChange, request, accept)

        then: "allowed"
        userManagePromoteResponse.status == HttpStatus.SC_NO_CONTENT

        when: "validate token for new admin includes appropriate user type roles"
        AuthenticateResponse valResponse = utils.validateToken(userManageToken)

        then:
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) != null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) == null

        when: "validate token for old admin includes appropriate user type roles"
        AuthenticateResponse valResponse2 = utils.validateToken(userAdminToken)

        then:
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) != null

        when: "upgrade default user"
        def promoteUserDefaultChange = createAdminChange(userDefault.id, userManage.id)
        def userDefaultPromoteResponse = cloud20.changeDomainAdministrator(identityAdminToken, domainId, promoteUserDefaultChange, request, accept)

        then: "allowed"
        userDefaultPromoteResponse.status == HttpStatus.SC_NO_CONTENT

        when: "validate token for new admin includes appropriate user type roles"
        AuthenticateResponse valResponse3 = utils.validateToken(userDefaultToken)

        then:
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) != null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) == null

        when: "validate token for old admin includes appropriate user type roles"
        AuthenticateResponse valResponse4 = utils.validateToken(userManageToken)

        then:
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) != null

        where:
        request | accept
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
    }


    def createAdminChange(def promoteUserId, def demoteUserId) {
        new DomainAdministratorChange().with {
            it.promoteUserId = promoteUserId
            it.demoteUserId = demoteUserId
            it
        }
    }

}
