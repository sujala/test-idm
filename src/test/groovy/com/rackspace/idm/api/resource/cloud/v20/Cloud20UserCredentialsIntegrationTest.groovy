package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.ErrorCodes
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.*

class Cloud20UserCredentialsIntegrationTest extends RootIntegrationTest{

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId

    @Shared def serviceAdminToken

    def setup(){
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        serviceAdminToken = utils.getServiceAdminToken()
    }

    def cleanup(){
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "No one should be allowed retrieve user's password" () {
        given:
        def users = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        for(User user : users) {
            String token = utils.getToken(user.username)
            def response = cloud20.getPasswordCredentials(token, defaultUser.id)
            assert (response.status == 403)
        }
        def serviceAdminResponse = cloud20.getPasswordCredentials(serviceAdminToken, defaultUser.id)

        then:
        serviceAdminResponse.status == 403
    }

    @Unroll
    def "Add/update apiKey for users: size = #size" () {
        when:
        def domainId = utils.createDomain()
        def users = utils.createUsers(domainId)
        def apiKeyCred = new ApiKeyCredentials()

        then: "Assert added/updated credentials"
        for (def user : users) {
            apiKeyCred.username = user.username
            apiKeyCred.apiKey = testUtils.getRandomUUIDOfLength("key", size)
            // Add credentials resource
            def response = cloud20.addCredential(utils.serviceAdminToken, user.id, apiKeyCred)
            assert response.status == SC_CREATED

            // Update user's apiKey credentials
            response = cloud20.addApiKeyToUser(utils.serviceAdminToken, user.id, apiKeyCred)
            assert response.status == SC_OK
        }

        cleanup:
        utils.deleteUsers(users.reverse())
        utils.deleteDomain(domainId)

        where:
        size << [100, 50]
    }

    def "Add apiKey credentials to user with length greater than 100 characters" () {
        when:
        def domainId = utils.createDomain()
        def users = utils.createUsers(domainId)
        def apiKeyCred = new ApiKeyCredentials()
        apiKeyCred.apiKey = testUtils.getRandomUUIDOfLength("key", 101)

        then: "Assert BadRequest"
        for (def user : users) {
            apiKeyCred.username = user.username
            // Add credentials resource
            def response = cloud20.addCredential(utils.serviceAdminToken, user.id, apiKeyCred)
            IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)
        }

        cleanup:
        utils.deleteUsers(users.reverse())
        utils.deleteDomain(domainId)
    }
}
