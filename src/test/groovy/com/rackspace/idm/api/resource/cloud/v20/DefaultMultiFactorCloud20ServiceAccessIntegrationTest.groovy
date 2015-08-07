package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.helpers.Cloud20Utils
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.SERVICE_ADMIN_2_PASSWORD
import static com.rackspace.idm.Constants.USER_MANAGE_ROLE_ID
import static com.rackspace.idm.Constants.SERVICE_ADMIN_USERNAME
import static com.rackspace.idm.Constants.SERVICE_ADMIN_2_USERNAME

class DefaultMultiFactorCloud20ServiceAccessIntegrationTest extends RootIntegrationTest  {

    @Autowired
    Cloud20Utils utils

    @Autowired
    BasicMultiFactorService multiFactorService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Shared def serviceAdmin
    @Shared def serviceAdmin2
    @Shared def users1
    @Shared def users2
    @Shared def domain1
    @Shared def domain2
    @Shared def userAdmin1
    @Shared def userAdmin2
    @Shared def userManage1
    @Shared def userManage2
    @Shared def userManage3
    @Shared def identityAdmin1
    @Shared def identityAdmin2
    @Shared def defaultUser1
    @Shared def defaultUser2
    @Shared def defaultUser3

    @Shared def serviceAdminToken
    @Shared def serviceAdminToken2
    @Shared def identityAdminToken
    @Shared def userAdminToken
    @Shared def userManageToken
    @Shared def defaultUserToken

    @Shared def mobilePhone
    @Shared def constantVerificationCode

    def setup() {
        serviceAdminToken = utils.getServiceAdminToken()

        serviceAdmin = utils.getUserByName(SERVICE_ADMIN_USERNAME)
        serviceAdmin2 = utils.getUserByName(SERVICE_ADMIN_2_USERNAME)

        serviceAdminToken2 = utils.getToken(SERVICE_ADMIN_2_USERNAME, SERVICE_ADMIN_2_PASSWORD)

        domain1 = utils.createDomain()
        domain2 = utils.createDomain()

        (userManage1, users1) = utils.createDefaultUser(domain1)
        utils.addRoleToUser(userManage1, USER_MANAGE_ROLE_ID)
        userManageToken = utils.getToken(userManage1.username)

        (userManage2, users2) = utils.createDefaultUser(domain2)
        utils.addRoleToUser(userManage2, USER_MANAGE_ROLE_ID)

        userAdmin1 = users1.get(1)
        userAdminToken = utils.getToken(userAdmin1.username)
        identityAdmin1 = users1.get(2)
        identityAdminToken = utils.getToken(identityAdmin1.username)

        userAdmin2 = users2.get(1)
        def userAdminToken2 = utils.getToken(userAdmin2.username)
        identityAdmin2 = users2.get(2)

        defaultUser1 = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domain1)
        defaultUserToken = utils.getToken(defaultUser1.username)

        userManage3 = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domain1)
        utils.addRoleToUser(userManage3, USER_MANAGE_ROLE_ID)

        defaultUser3 = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domain1)

        defaultUser2 = utils.createUser(userAdminToken2, testUtils.getRandomUUID("defaultUser"), domain2)

        mobilePhone = v2Factory.createMobilePhone()

        constantVerificationCode = new VerificationCode().with {
            it.code = Constants.MFA_DEFAULT_PIN
            it
        }
    }

    def cleanup() {
        resetUsers()
        resetTokens()
        utils.deleteUser(defaultUser1)
        utils.deleteUser(defaultUser2)
        utils.deleteUser(defaultUser3)
        utils.deleteUser(userManage3)
        utils.deleteUsers(users1)
        utils.deleteDomain(domain1)
        utils.deleteUsers(users2)
        utils.deleteDomain(domain2)
    }

    def "Users cannot unlock multi-factor for own account"() {
        setup:
        MultiFactor settings = v2Factory.createMultiFactorSettings(null, true)

        when:
        def serviceAdminResponse = cloud20.updateMultiFactorSettings(serviceAdminToken, serviceAdmin.id, settings)
        def identityAdminResponse = cloud20.updateMultiFactorSettings(identityAdminToken, identityAdmin1.id, settings)
        def userAdminResponse = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin1.id, settings)
        def userManageResponse = cloud20.updateMultiFactorSettings(userManageToken, userManage1.id, settings)
        def defaultUserResponse = cloud20.updateMultiFactorSettings(defaultUserToken, defaultUser1.id, settings)

        then:
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def "Access to update multi-factor is verified"() {
        setup:
        MultiFactor settings = v2Factory.createMultiFactorSettings(false)

        when: "Service Admin calls update multi-factor"
        def selfResponse = cloud20.updateMultiFactorSettings(serviceAdminToken, serviceAdmin.id, settings)
        def serviceAdminResponse = cloud20.updateMultiFactorSettings(identityAdminToken, serviceAdmin2.id, settings)
        def identityAdminResponse = cloud20.updateMultiFactorSettings(serviceAdminToken, identityAdmin2.id, settings)
        def userAdminResponse = cloud20.updateMultiFactorSettings(serviceAdminToken, userAdmin2.id, settings)
        def userManageResponse = cloud20.updateMultiFactorSettings(serviceAdminToken, userManage2.id, settings)
        def defaultUserResponse = cloud20.updateMultiFactorSettings(serviceAdminToken, defaultUser2.id, settings)
        resetUsers()

        then: "can update all users except other service admins"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT

        when: "Identity Admin calls update multi-factor"
        selfResponse = cloud20.updateMultiFactorSettings(identityAdminToken, identityAdmin1.id, settings)
        serviceAdminResponse = cloud20.updateMultiFactorSettings(identityAdminToken, serviceAdmin.id, settings)
        identityAdminResponse = cloud20.updateMultiFactorSettings(identityAdminToken, identityAdmin2.id, settings)
        userAdminResponse = cloud20.updateMultiFactorSettings(identityAdminToken, userAdmin2.id, settings)
        userManageResponse = cloud20.updateMultiFactorSettings(identityAdminToken, userManage2.id, settings)
        defaultUserResponse = cloud20.updateMultiFactorSettings(identityAdminToken, defaultUser2.id, settings)
        resetUsers()

        then: "can update all users except service admins and other identity admins"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT

        when: "User Admin calls update multi-factor"
        selfResponse = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin1.id, settings)
        serviceAdminResponse = cloud20.updateMultiFactorSettings(userAdminToken, serviceAdmin.id, settings)
        identityAdminResponse = cloud20.updateMultiFactorSettings(userAdminToken, identityAdmin2.id, settings)
        userAdminResponse = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin2.id, settings)
        userManageResponse = cloud20.updateMultiFactorSettings(userAdminToken, userManage1.id, settings)
        defaultUserResponse = cloud20.updateMultiFactorSettings(userAdminToken, defaultUser1.id, settings)
        def userManageResponseOtherDomain = cloud20.updateMultiFactorSettings(userAdminToken, userManage2.id, settings)
        def defaultUserResponseOtherDomain = cloud20.updateMultiFactorSettings(userAdminToken, defaultUser2.id, settings)
        resetUsers()

        then: "can only update self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "User Manage calls update multi-factor"
        selfResponse = cloud20.updateMultiFactorSettings(userManageToken, userManage1.id, settings)
        serviceAdminResponse = cloud20.updateMultiFactorSettings(userManageToken, serviceAdmin.id, settings)
        identityAdminResponse = cloud20.updateMultiFactorSettings(userManageToken, identityAdmin2.id, settings)
        userAdminResponse = cloud20.updateMultiFactorSettings(userManageToken, userAdmin2.id, settings)
        userManageResponse = cloud20.updateMultiFactorSettings(userManageToken, userManage3.id, settings)
        defaultUserResponse = cloud20.updateMultiFactorSettings(userManageToken, defaultUser1.id, settings)
        userManageResponseOtherDomain = cloud20.updateMultiFactorSettings(userManageToken, userManage2.id, settings)
        defaultUserResponseOtherDomain = cloud20.updateMultiFactorSettings(userManageToken, defaultUser2.id, settings)
        resetUsers()

        then: "can only update self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "Default User calls update multi-factor"
        selfResponse = cloud20.updateMultiFactorSettings(defaultUserToken, defaultUser1.id, settings)
        serviceAdminResponse = cloud20.updateMultiFactorSettings(defaultUserToken, serviceAdmin.id, settings)
        identityAdminResponse = cloud20.updateMultiFactorSettings(defaultUserToken, identityAdmin2.id, settings)
        userAdminResponse = cloud20.updateMultiFactorSettings(defaultUserToken, userAdmin2.id, settings)
        userManageResponse = cloud20.updateMultiFactorSettings(defaultUserToken, userManage3.id, settings)
        defaultUserResponse = cloud20.updateMultiFactorSettings(defaultUserToken, defaultUser3.id, settings)
        userManageResponseOtherDomain = cloud20.updateMultiFactorSettings(defaultUserToken, userManage2.id, settings)
        defaultUserResponseOtherDomain = cloud20.updateMultiFactorSettings(defaultUserToken, defaultUser2.id, settings)
        resetUsers()

        then: "can only update self"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def "Access to delete multi-factor is verified"() {
        when: "Service Admin calls delete multi-factor"
        def selfResponse = cloud20.deleteMultiFactor(serviceAdminToken, serviceAdmin.id)
        def serviceAdminResponse = cloud20.deleteMultiFactor(serviceAdminToken, serviceAdmin2.id)
        def identityAdminResponse = cloud20.deleteMultiFactor(serviceAdminToken, identityAdmin2.id)
        def userAdminResponse = cloud20.deleteMultiFactor(serviceAdminToken, userAdmin2.id)
        def userManageResponse = cloud20.deleteMultiFactor(serviceAdminToken, userManage2.id)
        def defaultUserResponse = cloud20.deleteMultiFactor(serviceAdminToken, defaultUser2.id)

        then: "can access all users except other service admins"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT

        when: "Identity Admin calls delete multi-factor"
        selfResponse = cloud20.deleteMultiFactor(identityAdminToken, identityAdmin1.id)
        serviceAdminResponse = cloud20.deleteMultiFactor(identityAdminToken, serviceAdmin.id)
        identityAdminResponse = cloud20.deleteMultiFactor(identityAdminToken, identityAdmin2.id)
        userAdminResponse = cloud20.deleteMultiFactor(identityAdminToken, userAdmin2.id)
        userManageResponse = cloud20.deleteMultiFactor(identityAdminToken, userManage2.id)
        defaultUserResponse = cloud20.deleteMultiFactor(identityAdminToken, defaultUser2.id)

        then: "can access all users except service admins and other identity admins"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT

        when: "User Admin calls delete multi-factor"
        selfResponse = cloud20.deleteMultiFactor(userAdminToken, userAdmin1.id)
        serviceAdminResponse = cloud20.deleteMultiFactor(userAdminToken, serviceAdmin.id)
        identityAdminResponse = cloud20.deleteMultiFactor(userAdminToken, identityAdmin2.id)
        userAdminResponse = cloud20.deleteMultiFactor(userAdminToken, userAdmin2.id)
        userManageResponse = cloud20.deleteMultiFactor(userAdminToken, userManage1.id)
        defaultUserResponse = cloud20.deleteMultiFactor(userAdminToken, defaultUser1.id)
        def userManageResponseOtherDomain = cloud20.deleteMultiFactor(userAdminToken, userManage2.id)
        def defaultUserResponseOtherDomain = cloud20.deleteMultiFactor(userAdminToken, defaultUser2.id)

        then: "can only access self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "User Manage calls delete multi-factor"
        selfResponse = cloud20.deleteMultiFactor(userManageToken, userManage1.id)
        serviceAdminResponse = cloud20.deleteMultiFactor(userManageToken, serviceAdmin.id)
        identityAdminResponse = cloud20.deleteMultiFactor(userManageToken, identityAdmin2.id)
        userAdminResponse = cloud20.deleteMultiFactor(userManageToken, userAdmin2.id)
        userManageResponse = cloud20.deleteMultiFactor(userManageToken, userManage3.id)
        defaultUserResponse = cloud20.deleteMultiFactor(userManageToken, defaultUser1.id)
        userManageResponseOtherDomain = cloud20.deleteMultiFactor(userManageToken, userManage2.id)
        defaultUserResponseOtherDomain = cloud20.deleteMultiFactor(userManageToken, defaultUser2.id)

        then: "can only access self and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "Default User calls delete multi-factor"
        selfResponse = cloud20.deleteMultiFactor(defaultUserToken, defaultUser1.id)
        serviceAdminResponse = cloud20.deleteMultiFactor(defaultUserToken, serviceAdmin.id)
        identityAdminResponse = cloud20.deleteMultiFactor(defaultUserToken, identityAdmin2.id)
        userAdminResponse = cloud20.deleteMultiFactor(defaultUserToken, userAdmin2.id)
        userManageResponse = cloud20.deleteMultiFactor(defaultUserToken, userManage3.id)
        defaultUserResponse = cloud20.deleteMultiFactor(defaultUserToken, defaultUser3.id)
        userManageResponseOtherDomain = cloud20.deleteMultiFactor(defaultUserToken, userManage2.id)
        defaultUserResponseOtherDomain = cloud20.deleteMultiFactor(defaultUserToken, defaultUser2.id)

        then: "can only access self"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def "Access to list user devices is verified"() {
        when: "Service Admin calls list user devices"
        def selfResponse = cloud20.listDevices(serviceAdminToken, serviceAdmin.id)
        def serviceAdminResponse = cloud20.listDevices(serviceAdminToken, serviceAdmin2.id)
        def identityAdminResponse = cloud20.listDevices(serviceAdminToken, identityAdmin2.id)
        def userAdminResponse = cloud20.listDevices(serviceAdminToken, userAdmin2.id)
        def userManageResponse = cloud20.listDevices(serviceAdminToken, userManage2.id)
        def defaultUserResponse = cloud20.listDevices(serviceAdminToken, defaultUser2.id)

        then: "can access all users except other service admins"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_OK
        userAdminResponse.getStatus() == HttpStatus.SC_OK
        userManageResponse.getStatus() == HttpStatus.SC_OK
        defaultUserResponse.getStatus() == HttpStatus.SC_OK

        when: "Identity Admin calls list user devices"
        selfResponse = cloud20.listDevices(identityAdminToken, identityAdmin1.id)
        serviceAdminResponse = cloud20.listDevices(identityAdminToken, serviceAdmin.id)
        identityAdminResponse = cloud20.listDevices(identityAdminToken, identityAdmin2.id)
        userAdminResponse = cloud20.listDevices(identityAdminToken, userAdmin2.id)
        userManageResponse = cloud20.listDevices(identityAdminToken, userManage2.id)
        defaultUserResponse = cloud20.listDevices(identityAdminToken, defaultUser2.id)

        then: "can access all users except service admins and other identity admins"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_OK
        userManageResponse.getStatus() == HttpStatus.SC_OK
        defaultUserResponse.getStatus() == HttpStatus.SC_OK

        when: "User Admin calls list user devices"
        selfResponse = cloud20.listDevices(userAdminToken, userAdmin1.id)
        serviceAdminResponse = cloud20.listDevices(userAdminToken, serviceAdmin.id)
        identityAdminResponse = cloud20.listDevices(userAdminToken, identityAdmin2.id)
        userAdminResponse = cloud20.listDevices(userAdminToken, userAdmin2.id)
        userManageResponse = cloud20.listDevices(userAdminToken, userManage1.id)
        defaultUserResponse = cloud20.listDevices(userAdminToken, defaultUser1.id)
        def userManageResponseOtherDomain = cloud20.listDevices(userAdminToken, userManage2.id)
        def defaultUserResponseOtherDomain = cloud20.listDevices(userAdminToken, defaultUser2.id)

        then: "can only access self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_OK
        defaultUserResponse.getStatus() == HttpStatus.SC_OK
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "User Manage calls list user devices"
        selfResponse = cloud20.listDevices(userManageToken, userManage1.id)
        serviceAdminResponse = cloud20.listDevices(userManageToken, serviceAdmin.id)
        identityAdminResponse = cloud20.listDevices(userManageToken, identityAdmin2.id)
        userAdminResponse = cloud20.listDevices(userManageToken, userAdmin2.id)
        userManageResponse = cloud20.listDevices(userManageToken, userManage3.id)
        defaultUserResponse = cloud20.listDevices(userManageToken, defaultUser1.id)
        userManageResponseOtherDomain = cloud20.listDevices(userManageToken, userManage2.id)
        defaultUserResponseOtherDomain = cloud20.listDevices(userManageToken, defaultUser2.id)

        then: "can only access self and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_OK
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "Default User calls list user devices"
        selfResponse = cloud20.listDevices(defaultUserToken, defaultUser1.id)
        serviceAdminResponse = cloud20.listDevices(defaultUserToken, serviceAdmin.id)
        identityAdminResponse = cloud20.listDevices(defaultUserToken, identityAdmin2.id)
        userAdminResponse = cloud20.listDevices(defaultUserToken, userAdmin2.id)
        userManageResponse = cloud20.listDevices(defaultUserToken, userManage3.id)
        defaultUserResponse = cloud20.listDevices(defaultUserToken, defaultUser3.id)
        userManageResponseOtherDomain = cloud20.listDevices(defaultUserToken, userManage2.id)
        defaultUserResponseOtherDomain = cloud20.listDevices(defaultUserToken, defaultUser2.id)

        then: "can only access self"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def "Access to verify verification code is verified"() {
        setup:
        def phoneId = addPhoneToUsersAndSendVerificationCode()

        when: "Service Admin calls verify verification code"
        def selfResponse = cloud20.verifyVerificationCode(serviceAdminToken, serviceAdmin.id, phoneId, constantVerificationCode)
        def serviceAdminResponse = cloud20.verifyVerificationCode(serviceAdminToken, serviceAdmin2.id, phoneId, constantVerificationCode)
        def identityAdminResponse = cloud20.verifyVerificationCode(serviceAdminToken, identityAdmin2.id, phoneId, constantVerificationCode)
        def userAdminResponse = cloud20.verifyVerificationCode(serviceAdminToken, userAdmin2.id, phoneId, constantVerificationCode)
        def userManageResponse = cloud20.verifyVerificationCode(serviceAdminToken, userManage2.id, phoneId, constantVerificationCode)
        def defaultUserResponse = cloud20.verifyVerificationCode(serviceAdminToken, defaultUser2.id, phoneId, constantVerificationCode)
        resetUsers()

        then: "can verify verification code to all users except other service admins"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT

        when: "Identity Admin calls verify verification code"
        phoneId = addPhoneToUsersAndSendVerificationCode()
        selfResponse = cloud20.verifyVerificationCode(identityAdminToken, identityAdmin1.id, phoneId, constantVerificationCode)
        serviceAdminResponse = cloud20.verifyVerificationCode(identityAdminToken, serviceAdmin.id, phoneId, constantVerificationCode)
        identityAdminResponse = cloud20.verifyVerificationCode(identityAdminToken, identityAdmin2.id, phoneId, constantVerificationCode)
        userAdminResponse = cloud20.verifyVerificationCode(identityAdminToken, userAdmin2.id, phoneId, constantVerificationCode)
        userManageResponse = cloud20.verifyVerificationCode(identityAdminToken, userManage2.id, phoneId, constantVerificationCode)
        defaultUserResponse = cloud20.verifyVerificationCode(identityAdminToken, defaultUser2.id, phoneId, constantVerificationCode)
        resetUsers()

        then: "can verify verification code to all users except service admins and other identity admins"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT

        when: "User Admin calls verify verification code"
        phoneId = addPhoneToUsersAndSendVerificationCode()
        selfResponse = cloud20.verifyVerificationCode(userAdminToken, userAdmin1.id, phoneId, constantVerificationCode)
        serviceAdminResponse = cloud20.verifyVerificationCode(userAdminToken, serviceAdmin.id, phoneId, constantVerificationCode)
        identityAdminResponse = cloud20.verifyVerificationCode(userAdminToken, identityAdmin2.id, phoneId, constantVerificationCode)
        userAdminResponse = cloud20.verifyVerificationCode(userAdminToken, userAdmin2.id, phoneId, constantVerificationCode)
        userManageResponse = cloud20.verifyVerificationCode(userAdminToken, userManage1.id, phoneId, constantVerificationCode)
        defaultUserResponse = cloud20.verifyVerificationCode(userAdminToken, defaultUser1.id, phoneId, constantVerificationCode)
        def userManageResponseOtherDomain = cloud20.verifyVerificationCode(userAdminToken, userManage2.id, phoneId, constantVerificationCode)
        def defaultUserResponseOtherDomain = cloud20.verifyVerificationCode(userAdminToken, defaultUser2.id, phoneId, constantVerificationCode)
        resetUsers()

        then: "can only verify verification code to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "User Manage calls verify verification code"
        phoneId = addPhoneToUsersAndSendVerificationCode()
        selfResponse = cloud20.verifyVerificationCode(userManageToken, userManage1.id, phoneId, constantVerificationCode)
        serviceAdminResponse = cloud20.verifyVerificationCode(userManageToken, serviceAdmin.id, phoneId, constantVerificationCode)
        identityAdminResponse = cloud20.verifyVerificationCode(userManageToken, identityAdmin2.id, phoneId, constantVerificationCode)
        userAdminResponse = cloud20.verifyVerificationCode(userManageToken, userAdmin2.id, phoneId, constantVerificationCode)
        userManageResponse = cloud20.verifyVerificationCode(userManageToken, userManage3.id, phoneId, constantVerificationCode)
        defaultUserResponse = cloud20.verifyVerificationCode(userManageToken, defaultUser1.id, phoneId, constantVerificationCode)
        userManageResponseOtherDomain = cloud20.verifyVerificationCode(userManageToken, userManage2.id, phoneId, constantVerificationCode)
        defaultUserResponseOtherDomain = cloud20.verifyVerificationCode(userManageToken, defaultUser2.id, phoneId, constantVerificationCode)
        resetUsers()

        then: "can only verify verification code to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "Default User calls verify verification code"
        phoneId = addPhoneToUsersAndSendVerificationCode()
        selfResponse = cloud20.verifyVerificationCode(defaultUserToken, defaultUser1.id, phoneId, constantVerificationCode)
        serviceAdminResponse = cloud20.verifyVerificationCode(defaultUserToken, serviceAdmin.id, phoneId, constantVerificationCode)
        identityAdminResponse = cloud20.verifyVerificationCode(defaultUserToken, identityAdmin2.id, phoneId, constantVerificationCode)
        userAdminResponse = cloud20.verifyVerificationCode(defaultUserToken, userAdmin2.id, phoneId, constantVerificationCode)
        userManageResponse = cloud20.verifyVerificationCode(defaultUserToken, userManage3.id, phoneId, constantVerificationCode)
        defaultUserResponse = cloud20.verifyVerificationCode(defaultUserToken, defaultUser3.id, phoneId, constantVerificationCode)
        userManageResponseOtherDomain = cloud20.verifyVerificationCode(defaultUserToken, userManage2.id, phoneId, constantVerificationCode)
        defaultUserResponseOtherDomain = cloud20.verifyVerificationCode(defaultUserToken, defaultUser2.id, phoneId, constantVerificationCode)
        resetUsers()

        then: "can only verify verification code to self"
        selfResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def "Access to send verification code is verified"() {
        setup:
        def phoneId = addPhoneToUsers()

        when: "Service Admin calls send verification code"
        def selfResponse = cloud20.sendVerificationCode(serviceAdminToken, serviceAdmin.id, phoneId)
        def serviceAdminResponse = cloud20.sendVerificationCode(serviceAdminToken, serviceAdmin2.id, phoneId)
        def identityAdminResponse = cloud20.sendVerificationCode(serviceAdminToken, identityAdmin2.id, phoneId)
        def userAdminResponse = cloud20.sendVerificationCode(serviceAdminToken, userAdmin2.id, phoneId)
        def userManageResponse = cloud20.sendVerificationCode(serviceAdminToken, userManage2.id, phoneId)
        def defaultUserResponse = cloud20.sendVerificationCode(serviceAdminToken, defaultUser2.id, phoneId)
        resetUsers()

        then: "can send verification code to all users except other service admins"
        selfResponse.getStatus() == HttpStatus.SC_ACCEPTED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_ACCEPTED
        userAdminResponse.getStatus() == HttpStatus.SC_ACCEPTED
        userManageResponse.getStatus() == HttpStatus.SC_ACCEPTED
        defaultUserResponse.getStatus() == HttpStatus.SC_ACCEPTED

        when: "Identity Admin calls send verification code"
        phoneId = addPhoneToUsers()
        selfResponse = cloud20.sendVerificationCode(identityAdminToken, identityAdmin1.id, phoneId)
        serviceAdminResponse = cloud20.sendVerificationCode(identityAdminToken, serviceAdmin.id, phoneId)
        identityAdminResponse = cloud20.sendVerificationCode(identityAdminToken, identityAdmin2.id, phoneId)
        userAdminResponse = cloud20.sendVerificationCode(identityAdminToken, userAdmin2.id, phoneId)
        userManageResponse = cloud20.sendVerificationCode(identityAdminToken, userManage2.id, phoneId)
        defaultUserResponse = cloud20.sendVerificationCode(identityAdminToken, defaultUser2.id, phoneId)
        resetUsers()

        then: "can send verification code to all users except service admins and other identity admins"
        selfResponse.getStatus() == HttpStatus.SC_ACCEPTED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_ACCEPTED
        userManageResponse.getStatus() == HttpStatus.SC_ACCEPTED
        defaultUserResponse.getStatus() == HttpStatus.SC_ACCEPTED

        when: "User Admin calls send verification code"
        phoneId = addPhoneToUsers()
        selfResponse = cloud20.sendVerificationCode(userAdminToken, userAdmin1.id, phoneId)
        serviceAdminResponse = cloud20.sendVerificationCode(userAdminToken, serviceAdmin.id, phoneId)
        identityAdminResponse = cloud20.sendVerificationCode(userAdminToken, identityAdmin2.id, phoneId)
        userAdminResponse = cloud20.sendVerificationCode(userAdminToken, userAdmin2.id, phoneId)
        userManageResponse = cloud20.sendVerificationCode(userAdminToken, userManage1.id, phoneId)
        defaultUserResponse = cloud20.sendVerificationCode(userAdminToken, defaultUser1.id, phoneId)
        def userManageResponseOtherDomain = cloud20.sendVerificationCode(userAdminToken, userManage2.id, phoneId)
        def defaultUserResponseOtherDomain = cloud20.sendVerificationCode(userAdminToken, defaultUser2.id, phoneId)
        resetUsers()

        then: "can only send verification code to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_ACCEPTED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_ACCEPTED
        defaultUserResponse.getStatus() == HttpStatus.SC_ACCEPTED
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "User Manage calls send verification code"
        phoneId = addPhoneToUsers()
        selfResponse = cloud20.sendVerificationCode(userManageToken, userManage1.id, phoneId)
        serviceAdminResponse = cloud20.sendVerificationCode(userManageToken, serviceAdmin.id, phoneId)
        identityAdminResponse = cloud20.sendVerificationCode(userManageToken, identityAdmin2.id, phoneId)
        userAdminResponse = cloud20.sendVerificationCode(userManageToken, userAdmin2.id, phoneId)
        userManageResponse = cloud20.sendVerificationCode(userManageToken, userManage3.id, phoneId)
        defaultUserResponse = cloud20.sendVerificationCode(userManageToken, defaultUser1.id, phoneId)
        userManageResponseOtherDomain = cloud20.sendVerificationCode(userManageToken, userManage2.id, phoneId)
        defaultUserResponseOtherDomain = cloud20.sendVerificationCode(userManageToken, defaultUser2.id, phoneId)
        resetUsers()

        then: "can only send verification code to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_ACCEPTED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_ACCEPTED
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "Default User calls send verification code"
        phoneId = addPhoneToUsers()
        selfResponse = cloud20.sendVerificationCode(defaultUserToken, defaultUser1.id, phoneId)
        serviceAdminResponse = cloud20.sendVerificationCode(defaultUserToken, serviceAdmin.id, phoneId)
        identityAdminResponse = cloud20.sendVerificationCode(defaultUserToken, identityAdmin2.id, phoneId)
        userAdminResponse = cloud20.sendVerificationCode(defaultUserToken, userAdmin2.id, phoneId)
        userManageResponse = cloud20.sendVerificationCode(defaultUserToken, userManage3.id, phoneId)
        defaultUserResponse = cloud20.sendVerificationCode(defaultUserToken, defaultUser3.id, phoneId)
        userManageResponseOtherDomain = cloud20.sendVerificationCode(defaultUserToken, userManage2.id, phoneId)
        defaultUserResponseOtherDomain = cloud20.sendVerificationCode(defaultUserToken, defaultUser2.id, phoneId)
        resetUsers()

        then: "can only send verification code to self"
        selfResponse.getStatus() == HttpStatus.SC_ACCEPTED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def "Access to add phone is verified"() {
        setup:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone mobilePhone = v2Factory.createMobilePhone()

        when: "Service Admin calls add phone"
        def selfResponse = cloud20.addPhoneToUser(serviceAdminToken, serviceAdmin.id, mobilePhone)
        def serviceAdminResponse = cloud20.addPhoneToUser(serviceAdminToken, serviceAdmin2.id, mobilePhone)
        def identityAdminResponse = cloud20.addPhoneToUser(serviceAdminToken, identityAdmin2.id, mobilePhone)
        def userAdminResponse = cloud20.addPhoneToUser(serviceAdminToken, userAdmin2.id, mobilePhone)
        def userManageResponse = cloud20.addPhoneToUser(serviceAdminToken, userManage2.id, mobilePhone)
        def defaultUserResponse = cloud20.addPhoneToUser(serviceAdminToken, defaultUser2.id, mobilePhone)
        resetUsers()

        then: "can add phone to all users except other service admins"
        selfResponse.getStatus() == HttpStatus.SC_CREATED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_CREATED
        userAdminResponse.getStatus() == HttpStatus.SC_CREATED
        userManageResponse.getStatus() == HttpStatus.SC_CREATED
        defaultUserResponse.getStatus() == HttpStatus.SC_CREATED

        when: "Identity Admin calls add phone"
        selfResponse = cloud20.addPhoneToUser(identityAdminToken, identityAdmin1.id, mobilePhone)
        serviceAdminResponse = cloud20.addPhoneToUser(identityAdminToken, serviceAdmin.id, mobilePhone)
        identityAdminResponse = cloud20.addPhoneToUser(identityAdminToken, identityAdmin2.id, mobilePhone)
        userAdminResponse = cloud20.addPhoneToUser(identityAdminToken, userAdmin2.id, mobilePhone)
        userManageResponse = cloud20.addPhoneToUser(identityAdminToken, userManage2.id, mobilePhone)
        defaultUserResponse = cloud20.addPhoneToUser(identityAdminToken, defaultUser2.id, mobilePhone)
        resetUsers()

        then: "can add phone to all users except service admins and other identity admins"
        selfResponse.getStatus() == HttpStatus.SC_CREATED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_CREATED
        userManageResponse.getStatus() == HttpStatus.SC_CREATED
        defaultUserResponse.getStatus() == HttpStatus.SC_CREATED

        when: "User Admin calls add phone"
        selfResponse = cloud20.addPhoneToUser(userAdminToken, userAdmin1.id, mobilePhone)
        serviceAdminResponse = cloud20.addPhoneToUser(userAdminToken, serviceAdmin.id, mobilePhone)
        identityAdminResponse = cloud20.addPhoneToUser(userAdminToken, identityAdmin2.id, mobilePhone)
        userAdminResponse = cloud20.addPhoneToUser(userAdminToken, userAdmin2.id, mobilePhone)
        userManageResponse = cloud20.addPhoneToUser(userAdminToken, userManage1.id, mobilePhone)
        defaultUserResponse = cloud20.addPhoneToUser(userAdminToken, defaultUser1.id, mobilePhone)
        def userManageResponseOtherDomain = cloud20.addPhoneToUser(userAdminToken, userManage2.id, mobilePhone)
        def defaultUserResponseOtherDomain = cloud20.addPhoneToUser(userAdminToken, defaultUser2.id, mobilePhone)
        resetUsers()

        then: "can only add phone to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_CREATED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_CREATED
        defaultUserResponse.getStatus() == HttpStatus.SC_CREATED
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "User Manage calls add phone"
        selfResponse = cloud20.addPhoneToUser(userManageToken, userManage1.id, mobilePhone)
        serviceAdminResponse = cloud20.addPhoneToUser(userManageToken, serviceAdmin.id, mobilePhone)
        identityAdminResponse = cloud20.addPhoneToUser(userManageToken, identityAdmin2.id, mobilePhone)
        userAdminResponse = cloud20.addPhoneToUser(userManageToken, userAdmin2.id, mobilePhone)
        userManageResponse = cloud20.addPhoneToUser(userManageToken, userManage3.id, mobilePhone)
        defaultUserResponse = cloud20.addPhoneToUser(userManageToken, defaultUser1.id, mobilePhone)
        userManageResponseOtherDomain = cloud20.addPhoneToUser(userManageToken, userManage2.id, mobilePhone)
        defaultUserResponseOtherDomain = cloud20.addPhoneToUser(userManageToken, defaultUser2.id, mobilePhone)
        resetUsers()

        then: "can only add phone to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_CREATED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_CREATED
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "Default User calls add phone"
        selfResponse = cloud20.addPhoneToUser(defaultUserToken, defaultUser1.id, mobilePhone)
        serviceAdminResponse = cloud20.addPhoneToUser(defaultUserToken, serviceAdmin.id, mobilePhone)
        identityAdminResponse = cloud20.addPhoneToUser(defaultUserToken, identityAdmin2.id, mobilePhone)
        userAdminResponse = cloud20.addPhoneToUser(defaultUserToken, userAdmin2.id, mobilePhone)
        userManageResponse = cloud20.addPhoneToUser(defaultUserToken, userManage3.id, mobilePhone)
        defaultUserResponse = cloud20.addPhoneToUser(defaultUserToken, defaultUser3.id, mobilePhone)
        userManageResponseOtherDomain = cloud20.addPhoneToUser(defaultUserToken, userManage2.id, mobilePhone)
        defaultUserResponseOtherDomain = cloud20.addPhoneToUser(defaultUserToken, defaultUser2.id, mobilePhone)
        resetUsers()

        then: "can only add phone to self"
        selfResponse.getStatus() == HttpStatus.SC_CREATED
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def "Access to bypass codes is verified"() {
        setup:
        def bypassCodes = v2Factory.createBypassCode(10, 1)

        when: "Service Admin calls generate bypass code"
        setupVerifiedPhonesForUsers()
        enableMulitfactorForUsers()
        def selfResponse = cloud20.getBypassCodes(serviceAdminToken, serviceAdmin.id, bypassCodes)
        def serviceAdminResponse = cloud20.getBypassCodes(serviceAdminToken, serviceAdmin2.id, bypassCodes)
        def identityAdminResponse = cloud20.getBypassCodes(serviceAdminToken, identityAdmin2.id, bypassCodes)
        def userAdminResponse = cloud20.getBypassCodes(serviceAdminToken, userAdmin2.id, bypassCodes)
        def userManageResponse = cloud20.getBypassCodes(serviceAdminToken, userManage2.id, bypassCodes)
        def defaultUserResponse = cloud20.getBypassCodes(serviceAdminToken, defaultUser2.id, bypassCodes)
        resetUsers()

        then: "can generate bypass code to all users except other service admins"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_OK
        userAdminResponse.getStatus() == HttpStatus.SC_OK
        userManageResponse.getStatus() == HttpStatus.SC_OK
        defaultUserResponse.getStatus() == HttpStatus.SC_OK

        when: "Identity Admin calls generate bypass code"
        setupVerifiedPhonesForUsers()
        enableMulitfactorForUsers()
        selfResponse = cloud20.getBypassCodes(identityAdminToken, identityAdmin1.id, bypassCodes)
        serviceAdminResponse = cloud20.getBypassCodes(identityAdminToken, serviceAdmin.id, bypassCodes)
        identityAdminResponse = cloud20.getBypassCodes(identityAdminToken, identityAdmin2.id, bypassCodes)
        userAdminResponse = cloud20.getBypassCodes(identityAdminToken, userAdmin2.id, bypassCodes)
        userManageResponse = cloud20.getBypassCodes(identityAdminToken, userManage2.id, bypassCodes)
        defaultUserResponse = cloud20.getBypassCodes(identityAdminToken, defaultUser2.id, bypassCodes)
        resetUsers()

        then: "can generate bypass code to all users except service admins and other identity admins"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_OK
        userManageResponse.getStatus() == HttpStatus.SC_OK
        defaultUserResponse.getStatus() == HttpStatus.SC_OK

        when: "User Admin calls generate bypass code"
        setupVerifiedPhonesForUsers()
        enableMulitfactorForUsers()
        selfResponse = cloud20.getBypassCodes(userAdminToken, userAdmin1.id, bypassCodes)
        serviceAdminResponse = cloud20.getBypassCodes(userAdminToken, serviceAdmin.id, bypassCodes)
        identityAdminResponse = cloud20.getBypassCodes(userAdminToken, identityAdmin2.id, bypassCodes)
        userAdminResponse = cloud20.getBypassCodes(userAdminToken, userAdmin2.id, bypassCodes)
        userManageResponse = cloud20.getBypassCodes(userAdminToken, userManage1.id, bypassCodes)
        defaultUserResponse = cloud20.getBypassCodes(userAdminToken, defaultUser1.id, bypassCodes)
        def userManageResponseOtherDomain = cloud20.getBypassCodes(userAdminToken, userManage2.id, bypassCodes)
        def defaultUserResponseOtherDomain = cloud20.getBypassCodes(userAdminToken, defaultUser2.id, bypassCodes)
        resetUsers()

        then: "can only generate bypass code to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_OK
        defaultUserResponse.getStatus() == HttpStatus.SC_OK
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "User Manage calls generate bypass code"
        setupVerifiedPhonesForUsers()
        enableMulitfactorForUsers()
        selfResponse = cloud20.getBypassCodes(userManageToken, userManage1.id, bypassCodes)
        serviceAdminResponse = cloud20.getBypassCodes(userManageToken, serviceAdmin.id, bypassCodes)
        identityAdminResponse = cloud20.getBypassCodes(userManageToken, identityAdmin2.id, bypassCodes)
        userAdminResponse = cloud20.getBypassCodes(userManageToken, userAdmin2.id, bypassCodes)
        userManageResponse = cloud20.getBypassCodes(userManageToken, userManage3.id, bypassCodes)
        defaultUserResponse = cloud20.getBypassCodes(userManageToken, defaultUser1.id, bypassCodes)
        userManageResponseOtherDomain = cloud20.getBypassCodes(userManageToken, userManage2.id, bypassCodes)
        defaultUserResponseOtherDomain = cloud20.getBypassCodes(userManageToken, defaultUser2.id, bypassCodes)
        resetUsers()

        then: "can only generate bypass code to self and user-managers and default users in same domain"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_OK
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN

        when: "Default User calls generate bypass code"
        setupVerifiedPhonesForUsers()
        enableMulitfactorForUsers()
        selfResponse = cloud20.getBypassCodes(defaultUserToken, defaultUser1.id, bypassCodes)
        serviceAdminResponse = cloud20.getBypassCodes(defaultUserToken, serviceAdmin.id, bypassCodes)
        identityAdminResponse = cloud20.getBypassCodes(defaultUserToken, identityAdmin2.id, bypassCodes)
        userAdminResponse = cloud20.getBypassCodes(defaultUserToken, userAdmin2.id, bypassCodes)
        userManageResponse = cloud20.getBypassCodes(defaultUserToken, userManage3.id, bypassCodes)
        defaultUserResponse = cloud20.getBypassCodes(defaultUserToken, defaultUser3.id, bypassCodes)
        userManageResponseOtherDomain = cloud20.getBypassCodes(defaultUserToken, userManage2.id, bypassCodes)
        defaultUserResponseOtherDomain = cloud20.getBypassCodes(defaultUserToken, defaultUser2.id, bypassCodes)
        resetUsers()

        then: "cannot generate bypass code"
        selfResponse.getStatus() == HttpStatus.SC_OK
        serviceAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userAdminResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponse.getStatus() == HttpStatus.SC_FORBIDDEN
        userManageResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
        defaultUserResponseOtherDomain.getStatus() == HttpStatus.SC_FORBIDDEN
    }

    def resetUsers() {
        multiFactorService.removeMultiFactorForUser(serviceAdmin.id)
        multiFactorService.removeMultiFactorForUser(serviceAdmin2.id)
        multiFactorService.removeMultiFactorForUser(identityAdmin1.id)
        multiFactorService.removeMultiFactorForUser(identityAdmin2.id)
        multiFactorService.removeMultiFactorForUser(userAdmin1.id)
        multiFactorService.removeMultiFactorForUser(userAdmin2.id)
        multiFactorService.removeMultiFactorForUser(userManage1.id)
        multiFactorService.removeMultiFactorForUser(userManage2.id)
        multiFactorService.removeMultiFactorForUser(userManage3.id)
        multiFactorService.removeMultiFactorForUser(defaultUser1.id)
        multiFactorService.removeMultiFactorForUser(defaultUser2.id)
        multiFactorService.removeMultiFactorForUser(defaultUser3.id)
    }

    def addPhoneToUsers() {
        resetUsers()
        def phoneId = utils.addPhone(serviceAdminToken, serviceAdmin.id, mobilePhone).id
        utils.addPhone(serviceAdminToken2, serviceAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, identityAdmin1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, identityAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userAdmin1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage3.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser3.id, mobilePhone)
        phoneId
    }

    def addPhoneToUsersAndSendVerificationCode() {
        resetUsers()
        def phoneId = utils.addPhone(serviceAdminToken, serviceAdmin.id, mobilePhone).id
        utils.addPhone(serviceAdminToken2, serviceAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, identityAdmin1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, identityAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userAdmin1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage3.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser3.id, mobilePhone)
        utils.sendVerificationCodeToPhone(serviceAdminToken, serviceAdmin.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken2, serviceAdmin2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, identityAdmin1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, identityAdmin2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userAdmin1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userAdmin2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userManage1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userManage2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userManage3.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, defaultUser1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, defaultUser2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, defaultUser3.id, phoneId)
        phoneId
    }

    def enableMulitfactorForUsers() {
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        utils.updateMultiFactor(serviceAdminToken, serviceAdmin.id, settings)

        Date newExpiry = new Date()
        newExpiry.setYear(newExpiry.getYear() + 1)

        def saScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(serviceAdminToken)
        saScopeAccess.setAccessTokenExp(newExpiry)
        scopeAccessRepository.updateScopeAccess(saScopeAccess)

        utils.updateMultiFactor(serviceAdminToken2, serviceAdmin2.id, settings)

        def sa2ScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(serviceAdminToken2)
        sa2ScopeAccess.setAccessTokenExp(newExpiry)
        scopeAccessRepository.updateScopeAccess(sa2ScopeAccess)

        utils.updateMultiFactor(serviceAdminToken, identityAdmin1.id, settings)
        utils.updateMultiFactor(serviceAdminToken, identityAdmin2.id, settings)
        utils.updateMultiFactor(serviceAdminToken, userAdmin1.id, settings)
        utils.updateMultiFactor(serviceAdminToken, userAdmin2.id, settings)
        utils.updateMultiFactor(serviceAdminToken, userManage1.id, settings)
        utils.updateMultiFactor(serviceAdminToken, userManage2.id, settings)
        utils.updateMultiFactor(serviceAdminToken, userManage3.id, settings)
        utils.updateMultiFactor(serviceAdminToken, defaultUser1.id, settings)
        utils.updateMultiFactor(serviceAdminToken, defaultUser2.id, settings)
        utils.updateMultiFactor(serviceAdminToken, defaultUser3.id, settings)

        def iaScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(identityAdminToken)
        def uaScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken)
        def umScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(userManageToken)
        def duScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(defaultUserToken)

        iaScopeAccess.setAccessTokenExp(newExpiry)
        uaScopeAccess.setAccessTokenExp(newExpiry)
        umScopeAccess.setAccessTokenExp(newExpiry)
        duScopeAccess.setAccessTokenExp(newExpiry)

        scopeAccessRepository.updateScopeAccess(iaScopeAccess)
        scopeAccessRepository.updateScopeAccess(uaScopeAccess)
        scopeAccessRepository.updateScopeAccess(umScopeAccess)
        scopeAccessRepository.updateScopeAccess(duScopeAccess)
    }

    def resetTokens() {
        Date newExpiry = new Date()
        newExpiry.setYear(newExpiry.getYear() + 1)

        def saScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(serviceAdminToken)
        saScopeAccess.setAccessTokenExp(newExpiry)
        scopeAccessRepository.updateScopeAccess(saScopeAccess)

        def sa2ScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(serviceAdminToken2)
        sa2ScopeAccess.setAccessTokenExp(newExpiry)
        scopeAccessRepository.updateScopeAccess(sa2ScopeAccess)

        def iaScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(identityAdminToken)
        def uaScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken)
        def umScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(userManageToken)
        def duScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(defaultUserToken)

        iaScopeAccess.setAccessTokenExp(newExpiry)
        uaScopeAccess.setAccessTokenExp(newExpiry)
        umScopeAccess.setAccessTokenExp(newExpiry)
        duScopeAccess.setAccessTokenExp(newExpiry)

        scopeAccessRepository.updateScopeAccess(iaScopeAccess)
        scopeAccessRepository.updateScopeAccess(uaScopeAccess)
        scopeAccessRepository.updateScopeAccess(umScopeAccess)
        scopeAccessRepository.updateScopeAccess(duScopeAccess)
    }

    def setupVerifiedPhonesForUsers() {
        resetUsers()
        def phoneId = utils.addPhone(serviceAdminToken, serviceAdmin.id, mobilePhone).id
        utils.addPhone(serviceAdminToken2, serviceAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, identityAdmin1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, identityAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userAdmin1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userAdmin2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, userManage3.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser1.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser2.id, mobilePhone)
        utils.addPhone(serviceAdminToken, defaultUser3.id, mobilePhone)
        utils.sendVerificationCodeToPhone(serviceAdminToken, serviceAdmin.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken2, serviceAdmin2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, identityAdmin1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, identityAdmin2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userAdmin1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userAdmin2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userManage1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userManage2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, userManage3.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, defaultUser1.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, defaultUser2.id, phoneId)
        utils.sendVerificationCodeToPhone(serviceAdminToken, defaultUser3.id, phoneId)
        cloud20.verifyVerificationCode(serviceAdminToken, serviceAdmin.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken2, serviceAdmin2.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, identityAdmin1.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, identityAdmin2.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, userAdmin1.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, userAdmin2.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, userManage1.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, userManage2.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, userManage3.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, defaultUser1.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, defaultUser2.id, phoneId, constantVerificationCode)
        cloud20.verifyVerificationCode(serviceAdminToken, defaultUser3.id, phoneId, constantVerificationCode)
        phoneId
    }
}
