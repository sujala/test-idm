package com.rackspace.idm.api.resource.cloud.v20

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
    def "update user only allows for identity or service admins to set and view contact ID, userType = #userType, accept = #accept, request = #request"() {
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
        def returnedContactId = getContactIdFromUpdateResponse(updateUserResponse, accept)
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
        def returnedContactId2 = getContactIdFromUpdateResponse(updateUserResponse2, accept)
        if(attributeSet) {
            assert returnedContactId2 == contactId
        } else {
            assert returnedContactId2 == null
        }

        when: "now that the user has a contact ID, update the user without the contact ID to verify it is not deleted"
        userForCreate.contactId = null
        def updateUserResponse3 = cloud20.updateUser(token, user.id, userForCreate, accept, request)

        then: "verify the response"
        updateUserResponse.status == 200
        def returnedContactId3 = getContactIdFromUpdateResponse(updateUserResponse3, accept)
        if(attributeSet) {
            assert returnedContactId3 == contactId
        } else {
            assert returnedContactId3 == null
        }

        and: "verify that the attribute was not modified"
        def userEntity2 = userService.getUserById(user.id)
        userEntity2.contactId == contactId

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), user.id)
        utils.deleteUsers(users)

        where:
        userType                            | attributeSet | accept                          | request
        IdentityUserTypeEnum.SERVICE_ADMIN  | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

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

    def getContactIdFromUpdateResponse(response, accept) {
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            def returnedUser = response.getEntity(User).value
            return returnedUser.contactId
        } else {
            def returnedUser = new JsonSlurper().parseText(response.getEntity(String)).user
            if(returnedUser.hasProperty('RAX-AUTH:contactId')) assert returnedUser['RAX-AUTH:contactId'] != null
            return returnedUser['RAX-AUTH:contactId']
        }

    }

}
