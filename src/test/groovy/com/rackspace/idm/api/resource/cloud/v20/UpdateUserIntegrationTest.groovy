package com.rackspace.idm.api.resource.cloud.v20

import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class UpdateUserIntegrationTest extends RootIntegrationTest {

    def "update user v1.1 without enabled attribute does not enable user"() {
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

    def "update user v2 without enabled attribute does not enable user"() {
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

}
