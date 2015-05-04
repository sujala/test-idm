package com.rackspace.idm.helpers

import com.rackspace.api.idm.v1.AuthData
import com.rackspace.api.idm.v1.Token
import com.rackspace.api.idm.v1.User
import com.rackspace.api.idm.v1.UserPasswordCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.FoundationApiMethods
import testHelpers.FoundationFactory
import testHelpers.V2Factory

import javax.annotation.PostConstruct

import static org.apache.http.HttpStatus.*

@Component
class FoundationApiUtils {

    @Autowired
    FoundationApiMethods methods

    @Autowired
    FoundationFactory factory

    def authenticate(String clientId, String clientSecret) {
        def response = methods.authenticate(clientId, clientSecret)
        assert (response.status == SC_OK)
        response.getEntity(AuthData)
    }

    def validateToken(String adminToken, String token) {
        def response = methods.validateToken(adminToken, token)
        assert (response.status == SC_OK)
        response.getEntity(AuthData)
    }

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

    def authenticateUser(String clientId, String clientSecret, String username, String password) {
        def response = methods.authenticateUser(clientId, clientSecret, username, password)
        assert (response.status == SC_OK)
        response.getEntity(AuthData)
    }

    def authenticateRacker(String clientId, String clientSecret, String racker, String password) {
        def response = methods.authenticateRacker(clientId, clientSecret, racker, password)
        assert (response.status == SC_OK)
        response.getEntity(AuthData)
    }
}
