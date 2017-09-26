package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.http.HttpHeaders
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class GetUsersInUserGroupRestIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "Get users in user group; media = #accept"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "create user group for domain"
        UserGroup userGroup = new UserGroup().with {
            it.domainId = domainId
            it.name = testUtils.getRandomUUID('userGroup')
            it.description = "desc"
            it
        }
        def response = cloud20.createUserGroup(userAdminToken, userGroup)
        def userGroupEntity = response.getEntity(UserGroup)

        then:
        response.status == SC_CREATED

        when: "list users in user group"
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id, null, accept)
        def usersEntity = getUsersFromResponse(response)

        then:
        response.status == SC_OK
        usersEntity.user.size() == 0

        when: "add user to user group"
        response = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, userAdmin.id)

        then:
        response.status == SC_NO_CONTENT

        when: "list users in user group"
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id, null, accept)
        usersEntity = getUsersFromResponse(response)

        then:
        response.status == SC_OK
        usersEntity.user.size() == 1
        def user = usersEntity.user.get(0)
        user.username == userAdmin.username

        when: "remove user from user group"
        response = cloud20.removeUserFromUserGroup(userAdminToken, domainId, userGroupEntity.id, userAdmin.id)

        then:
        response.status == SC_NO_CONTENT

        when: "list users in user group"
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id, null, accept)
        usersEntity = getUsersFromResponse(response)

        then:
        response.status == SC_OK
        usersEntity.user.size() == 0

        cleanup:
        utils.deleteUserGroup(userGroupEntity)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Test pagination on get users in user group: size = '#size'; marker = '#marker'; limit = 'limit'"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "create user group for domain"
        UserGroup userGroup = new UserGroup().with {
            it.domainId = domainId
            it.name = testUtils.getRandomUUID('userGroup')
            it.description = "desc"
            it
        }
        def response = cloud20.createUserGroup(userAdminToken, userGroup)
        def userGroupEntity = response.getEntity(UserGroup)

        then:
        response.status == SC_CREATED

        when: "add users to user group"
        def userAdminResponse = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, userAdmin.id)
        def userManageResponse = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, userManage.id)
        def defaultUserResponse = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, defaultUser.id)

        then:
        userAdminResponse.status == SC_NO_CONTENT
        userManageResponse.status == SC_NO_CONTENT
        defaultUserResponse.status == SC_NO_CONTENT

        when: "list users in user group"
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id, new UserSearchCriteria(new PaginationParams(marker, limit)))
        def usersEntity = response.getEntity(UserList).value

        then:
        response.status == SC_OK
        usersEntity.user.size() == size
        if (size != 0){
            assert response.headers.get(HttpHeaders.LINK) != null
        }

        cleanup:
        utils.deleteUserGroup(userGroupEntity)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        size | marker | limit
        3    | 0      | 3
        3    | 0      | 10
        2    | 1      | 3
        1    | 2      | 3
        3    | 0      | 0
    }

    def "Error check: get users in user group"() {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def userAdmin2, users2
        (userAdmin2, users2) = utils.createUserAdmin(domainId2)

        when: "create user group for domain"
        UserGroup userGroup = new UserGroup().with {
            it.domainId = domainId
            it.name = testUtils.getRandomUUID('userGroup')
            it.description = "desc"
            it
        }
        def response = cloud20.createUserGroup(userAdminToken, userGroup)
        def userGroupEntity = response.getEntity(UserGroup)

        then:
        response.status == SC_CREATED

        when: "list users in user group"
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id)
        def usersEntity = response.getEntity(UserList).value

        then:
        response.status == SC_OK
        usersEntity.user.size() == 0

        when: "add user to user group"
        response = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, userAdmin.id)

        then:
        response.status == SC_NO_CONTENT

        when: "invalid token"
        response = cloud20.getUsersInUserGroup("invalid", domainId, userGroupEntity.id)

        then:
        response.status == SC_UNAUTHORIZED

        when: "forbidden token"
        response = cloud20.getUsersInUserGroup(utils.getToken(userAdmin2.username), domainId, userGroupEntity.id)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUserGroup(userGroupEntity)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)
    }

    def getUsersFromResponse(response) {
        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            return response.getEntity(UserList).value
        }

        UserList userList = new UserList()
        userList.user.addAll(new JsonSlurper().parseText(response.getEntity(String))["users"])

        return userList
    }
}
