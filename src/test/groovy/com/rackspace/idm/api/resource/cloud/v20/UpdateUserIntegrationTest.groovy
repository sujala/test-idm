package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.UserService
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class UpdateUserIntegrationTest extends RootIntegrationTest {

    @Autowired UserService userService

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
