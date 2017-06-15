package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.IdentityFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class UpdateUserIntegrationTest extends RootIntegrationTest {

    @Autowired UserService userService
    @Autowired TenantService tenantService
    @Autowired TenantRoleDao tenantRoleDao
    @Autowired ApplicationService applicationService

    @Unroll
    def "update user v1.1 without enabled attribute does not enable user, accept = #acceptContentType, request = #requestContentType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userForUpdate = v2Factory.createUser(userAdmin.id, userAdmin.username)
        userForUpdate.enabled = false
        cloud20.updateUser(utils.getServiceAdminToken(), userAdmin.id, userForUpdate)

        when: "calling the v1.1 update user call with a null enabled attribute"
        userForUpdate = v1Factory.createUser(userAdmin.username, null, testUtils.getRandomInteger(), "someNastId", null)
        cloud11.updateUser(userAdmin.username, userForUpdate, acceptContentType, requestContentType)
        def userAdminById = utils.getUserById(userAdmin.id)

        then: "does not update the user's enabled state (user is still disabled)"
        userAdminById.enabled == false

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType | requestContentType
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "update user v2 without enabled attribute does not enable user, accept = #acceptContentType, request = #requestContentType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userForUpdate = v2Factory.createUser(userAdmin.id, userAdmin.username)
        userForUpdate.enabled = false
        cloud20.updateUser(utils.getServiceAdminToken(), userAdmin.id, userForUpdate)

        when: "calling the v2.0 update user call with a null enabled attribute"
        userForUpdate = v2Factory.createUser(userAdmin.id, userAdmin.username)
        userForUpdate.enabled = null
        cloud20.updateUser(utils.getServiceAdminToken(), userAdmin.id, userForUpdate, acceptContentType, requestContentType)
        def userAdminById = utils.getUserById(userAdmin.id)

        then: "does not update the user's enabled state (user is still disabled)"
        userAdminById.enabled == false

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType | requestContentType
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def "update user v1.1 without enabled attribute does not disable user"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "calling the v1.1 update user call with a null enabled attribute"
        def userForUpdate = v1Factory.createUser(userAdmin.username, null, testUtils.getRandomInteger(), "someNastId", null)
        cloud11.updateUser(userAdmin.username, userForUpdate, acceptContentType, requestContentType)
        def userAdminById = utils.getUserById(userAdmin.id)

        then: "does not update the user's enabled state (user is still enabled)"
        userAdminById.enabled == true

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType | requestContentType
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def "update user v2.0 without enabled attribute does not disable user"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "calling the v2.0 update user call with a null enabled attribute"
        def userForUpdate = v2Factory.createUser(userAdmin.id, userAdmin.username)
        userForUpdate.enabled = null
        cloud20.updateUser(utils.getServiceAdminToken(), userAdmin.id, userForUpdate, acceptContentType, requestContentType)
        def userAdminById = utils.getUserById(userAdmin.id)

        then: "does not update the user's enabled state (user is still enabled)"
        userAdminById.enabled == true

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType | requestContentType
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "update user only allows for identity or service admins to set contact ID, userType = #userType, accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def username = testUtils.getRandomUUID("defaultUser")
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD)
        def user = cloud20.createUser(utils.getToken(userAdmin.username), userForCreate).getEntity(User).value

        when: "update the user w/o a contact ID trying to give them a contact ID"
        def token
        switch(userType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                token = utils.getServiceAdminToken()
                break
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                token = utils.getIdentityAdminToken()
                break
            case IdentityUserTypeEnum.USER_ADMIN:
                token = utils.getToken(userAdmin.username)
                break
            case IdentityUserTypeEnum.USER_MANAGER:
                token = utils.getToken(userManage.username)
                break
        }
        userForCreate.contactId = contactId
        def updateUserResponse = cloud20.updateUser(token, user.id, userForCreate, accept, request)

        then:
        updateUserResponse.status == 200
        def returnedContactId = getContactIdFromUpdateResponse(updateUserResponse)
        if(attributeSet) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        when: "actually give the user a contact ID and then update the user again"
        def userEntity = userService.getUserById(user.id)
        userEntity.contactId = contactId
        userService.updateUser(userEntity)
        def updateUserResponse2 = cloud20.updateUser(token, user.id, userForCreate, accept, request)

        then:
        updateUserResponse.status == 200
        def returnedContactId2 = getContactIdFromUpdateResponse(updateUserResponse2)
        assert returnedContactId2 == contactId

        when: "now that the user has a contact ID, update the user without the contact ID to verify it is not deleted"
        userForCreate.contactId = null
        def updateUserResponse3 = cloud20.updateUser(token, user.id, userForCreate, accept, request)

        then: "verify the response"
        updateUserResponse.status == 200
        def returnedContactId3 = getContactIdFromUpdateResponse(updateUserResponse3)
        assert returnedContactId3 == contactId

        and: "verify that the attribute was not modified"
        def userEntity2 = userService.getUserById(user.id)
        userEntity2.contactId == contactId

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), user.id)
        utils.deleteUsers(users)

        where:
        userType                            | attributeSet | accept                          | request
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update user should ignore MFA attributes, accept = #acceptContentType, request = #requestContentType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userForUpdate = v2Factory.createUser(userAdmin.id, userAdmin.username)

        when: "Updating user's MFA attributes"
        def mfEnabled = true
        def mfState = MultiFactorStateEnum.LOCKED
        def factorType = FactorTypeEnum.OTP
        def mfEnforcementLevel = UserMultiFactorEnforcementLevelEnum.REQUIRED
        userForUpdate.multiFactorEnabled = mfEnabled
        userForUpdate.multiFactorState = mfState
        userForUpdate.factorType = factorType
        userForUpdate.userMultiFactorEnforcementLevel = mfEnforcementLevel
        def response = cloud20.updateUser(utils.getServiceAdminToken(), userAdmin.id, userForUpdate, acceptContentType, requestContentType)
        def updatedUser = getUserFromResponse(response)

        then: "Assert MFA attributes are unchanged"
        response.status == 200
        updatedUser.multiFactorEnabled != mfEnabled
        updatedUser.multiFactorState != mfState
        updatedUser.factorType != factorType
        updatedUser.userMultiFactorEnforcementLevel != mfEnforcementLevel

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType               | requestContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Assert correct user level access exposes the contactId after admin has set contactId, accept = #acceptContentType" () {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        when: "Update default user's contactId"
        def userForUpdate = new User().with {
            it.contactId = contactId
            it
        }
        cloud20.updateUser(utils.getIdentityAdminToken(), defaultUser.id, userForUpdate)

        then: "Update default user"
        for (def user : users) {
            def token = utils.getToken(user.username)
            userForUpdate = new User().with {
                it.displayName = testUtils.getRandomUUID("displayName")
                it
            }
            def updateResponse = cloud20.updateUser(token, defaultUser.id, userForUpdate)
            assert updateResponse.status == 200
            def returnedContactId = getContactIdFromUpdateResponse(updateResponse)
            assert returnedContactId == contactId
        }

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test service admin username can be updated: featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_USERNAME_UPDATE_PROP, featureEnabled)
        def serviceAdmin = utils.createIdentityAdmin()
        def serviceAdminEntity = userService.getUser(serviceAdmin.username)
        tenantRoleDao.deleteTenantRoleForUser(serviceAdminEntity, tenantService.getTenantRoleForUserById(serviceAdminEntity, Constants.IDENTITY_ADMIN_ROLE_ID))
        def serviceAdminClientRole = applicationService.getClientRoleById(Constants.SERVICE_ADMIN_ROLE_ID)
        TenantRole role = new TenantRole()
        role.setClientId(serviceAdminClientRole.getClientId())
        role.setName(serviceAdminClientRole.getName())
        role.setRoleRsId(serviceAdminClientRole.getId())
        tenantService.addTenantRoleToUser(serviceAdminEntity, role)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userManager = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphabetic(8), domainId)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID)
        users = [userManager, *users]
        def defaultUser = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphabetic(8), domainId)
        users = [defaultUser, *users]
        def userUpdates = new UserForCreate()

        when: "update w/ same service admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        def response = cloud20.updateUser(utils.getToken(serviceAdmin.username), serviceAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ different service admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getServiceAdminToken(), serviceAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ identity admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getIdentityAdminToken(), serviceAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultCloud20Service.ERROR_CANNOT_UPDATE_USER_WITH_HIGHER_ACCESS)

        when: "update w/ user admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userAdmin.username), serviceAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ user manager token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManager.username), serviceAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUser.username), serviceAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        userService.deleteUser(serviceAdminEntity)

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "test identity admin username can be updated: featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_USERNAME_UPDATE_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        identityAdmin = users[0]
        def userManager = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphabetic(8), domainId)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID)
        users = [userManager, *users]
        def defaultUser = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphabetic(8), domainId)
        users = [defaultUser, *users]
        def userUpdates = new UserForCreate()

        when: "update w/ same user token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        def response = cloud20.updateUser(utils.getToken(identityAdmin.username), identityAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ service admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getServiceAdminToken(), identityAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ different identity admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getIdentityAdminToken(), identityAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ user admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userAdmin.username), identityAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ user manager token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManager.username), identityAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUser.username), identityAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "test user admin's username can be updated: featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_USERNAME_UPDATE_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def userAdmin, userAdmin2, users, users2
        (userAdmin, users) = utils.createUserAdmin(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId2)
        def userManagerSameDomain = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphabetic(8), domainId)
        utils.addRoleToUser(userManagerSameDomain, Constants.USER_MANAGE_ROLE_ID)
        users = [userManagerSameDomain, *users]
        def userManagerDifferentDomain = utils.createUser(utils.getToken(userAdmin2.username), RandomStringUtils.randomAlphabetic(8), domainId2)
        utils.addRoleToUser(userManagerDifferentDomain, Constants.USER_MANAGE_ROLE_ID)
        users2 = [userManagerDifferentDomain, *users2]
        def defaultUserSameDomain = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphabetic(8), domainId)
        users = [defaultUserSameDomain, *users]
        def defaultUserDifferentDomain = utils.createUser(utils.getToken(userAdmin2.username), RandomStringUtils.randomAlphabetic(8), domainId2)
        users2 = [defaultUserDifferentDomain, *users2]
        def userUpdates = new UserForCreate()

        when: "update w/ same user token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        def response = cloud20.updateUser(utils.getToken(userAdmin.username), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ service admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getServiceAdminToken(), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ identity admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getIdentityAdminToken(), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ different user admin token (different domain)"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userAdmin2.username), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ user manager token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManagerSameDomain.username), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultCloud20Service.UPDATE_USER_CANNOT_UPDATE_HIGHER_LEVEL_USER_ERROR_MESSAGE)

        when: "update w/ user manager token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManagerDifferentDomain.username), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUserSameDomain.username), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUserDifferentDomain.username), userAdmin.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId2)

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "test user manager username can be updated: featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_USERNAME_UPDATE_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def userAdmin1, userAdmin2, users, users2
        (userAdmin1, users) = utils.createUserAdmin(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId2)
        def userManager = utils.createUser(utils.getToken(userAdmin1.username), RandomStringUtils.randomAlphabetic(8), domainId)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID)
        users = [userManager, *users]
        def userManagerSameDomain = utils.createUser(utils.getToken(userAdmin1.username), RandomStringUtils.randomAlphabetic(8), domainId)
        utils.addRoleToUser(userManagerSameDomain, Constants.USER_MANAGE_ROLE_ID)
        users = [userManagerSameDomain, *users]
        def userManagerDifferentDomain = utils.createUser(utils.getToken(userAdmin2.username), RandomStringUtils.randomAlphabetic(8), domainId2)
        utils.addRoleToUser(userManagerDifferentDomain, Constants.USER_MANAGE_ROLE_ID)
        users2 = [userManagerDifferentDomain, *users2]
        def defaultUserSameDomain = utils.createUser(utils.getToken(userAdmin1.username), RandomStringUtils.randomAlphabetic(8), domainId)
        users = [defaultUserSameDomain, *users]
        def defaultUserDifferentDomain = utils.createUser(utils.getToken(userAdmin2.username), RandomStringUtils.randomAlphabetic(8), domainId2)
        users2 = [defaultUserDifferentDomain, *users2]
        def userUpdates = new UserForCreate()

        when: "update w/ same user token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        def response = cloud20.updateUser(utils.getToken(userManager.username), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ service admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getServiceAdminToken(), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ identity admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getIdentityAdminToken(), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ user admin token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userAdmin1.username), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ user admin token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userAdmin2.username), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ user manager token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManagerSameDomain.username), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultCloud20Service.UPDATE_USER_CANNOT_UPDATE_HIGHER_LEVEL_USER_ERROR_MESSAGE)

        when: "update w/ user manager token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManagerDifferentDomain.username), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUserSameDomain.username), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUserDifferentDomain.username), userManager.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId2)

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "test default user username can be updated: featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_USERNAME_UPDATE_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def userAdmin1, userAdmin2, users, users2
        (userAdmin1, users) = utils.createUserAdmin(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId2)
        def userManagerSameDomain = utils.createUser(utils.getToken(userAdmin1.username), RandomStringUtils.randomAlphabetic(8), domainId)
        utils.addRoleToUser(userManagerSameDomain, Constants.USER_MANAGE_ROLE_ID)
        users = [userManagerSameDomain, *users]
        def userManagerDifferentDomain = utils.createUser(utils.getToken(userAdmin2.username), RandomStringUtils.randomAlphabetic(8), domainId2)
        utils.addRoleToUser(userManagerDifferentDomain, Constants.USER_MANAGE_ROLE_ID)
        users2 = [userManagerDifferentDomain, *users2]
        def defaultUser = utils.createUser(utils.getToken(userAdmin1.username), RandomStringUtils.randomAlphabetic(8), domainId)
        users = [defaultUser, *users]
        def defaultUserSameDomain = utils.createUser(utils.getToken(userAdmin1.username), RandomStringUtils.randomAlphabetic(8), domainId)
        users = [defaultUserSameDomain, *users]
        def defaultUserDifferentDomain = utils.createUser(utils.getToken(userAdmin2.username), RandomStringUtils.randomAlphabetic(8), domainId2)
        users2 = [defaultUserDifferentDomain, *users2]
        def userUpdates = new UserForCreate()

        when: "update w/ same user token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        def response = cloud20.updateUser(utils.getToken(defaultUser.username), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ service admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getServiceAdminToken(), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ identity admin token"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getIdentityAdminToken(), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ user admin token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userAdmin1.username), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ user admin token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userAdmin2.username), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ user manager token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManagerSameDomain.username), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, featureEnabled)

        when: "update w/ user manager token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(userManagerDifferentDomain.username), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token from same domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUserSameDomain.username), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        when: "update w/ default user token from different domain"
        userUpdates.username = RandomStringUtils.randomAlphabetic(8)
        response = cloud20.updateUser(utils.getToken(defaultUserDifferentDomain.username), defaultUser.id, userUpdates)

        then:
        assertUsernameUpdated(response, userUpdates, false, DefaultAuthorizationService.NOT_AUTHORIZED_MSG)

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId2)

        where:
        featureEnabled << [true, false]
    }

    void assertUsernameUpdated(response, userUpdates, usernameCanBeUpdated, errorMessage = DefaultCloud20Service.USERNAME_CANNOT_BE_UPDATED_ERROR_MESSAGE) {
        if (usernameCanBeUpdated) {
            assert response.status == 200
            assert response.getEntity(UserForCreate).value.username == userUpdates.username
        } else {
            assert response.status == 403
            assert response.getEntity(IdentityFault).value.message == errorMessage
        }

    }

    def getUserFromResponse(response) {
        def user = null
        if (response.getType() == MediaType.APPLICATION_XML_TYPE) {
            user = response.getEntity(User).value
        } else {
            def entity = new JsonSlurper().parseText(response.getEntity(String))
            user = entity["user"]
        }
        return user
    }

    def getContactIdFromUpdateResponse(response) {
        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            def returnedUser = response.getEntity(User).value
            return returnedUser.contactId
        } else {
            def returnedUser = new JsonSlurper().parseText(response.getEntity(String)).user
            if(returnedUser.hasProperty('RAX-AUTH:contactId')) assert returnedUser['RAX-AUTH:contactId'] != null
            return returnedUser['RAX-AUTH:contactId']
        }
    }

}
