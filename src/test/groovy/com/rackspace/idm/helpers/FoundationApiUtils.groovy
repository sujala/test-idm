package com.rackspace.idm.helpers

import com.rackspace.api.idm.v1.User
import com.rackspace.api.idm.v1.UserPasswordCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.FoundationApiMethods
import testHelpers.FoundationFactory

import static org.apache.http.HttpStatus.*

@Component
class FoundationApiUtils {

    @Autowired
    FoundationApiMethods methods

    @Autowired
    FoundationFactory factory

    def createUser(String token) {
        def user = factory.createUser()
        def response = methods.createUser(token, user)
        assert (response.status == SC_CREATED)
        response.getEntity(User)
    }

    def getUser(String token, String userId) {
        def response = methods.getUser(token, userId)
        assert (response.status == SC_OK)
        response.getEntity(User)
    }

    def getUserPasswordCredentials(String token, String userId) {
        def response = methods.getUserPasswordCredentials(token, userId)
        assert (response.status == SC_OK)
        response.getEntity(UserPasswordCredentials)
    }
}
