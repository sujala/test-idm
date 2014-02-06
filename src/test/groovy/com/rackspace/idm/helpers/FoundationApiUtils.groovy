package com.rackspace.idm.helpers

import com.rackspace.api.idm.v1.AuthData
import com.rackspace.api.idm.v1.Tenant
import com.rackspace.api.idm.v1.Token
import com.rackspace.api.idm.v1.User
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

    @PostConstruct
    def init() {
        methods.init()
    }

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

    def createTenant(String token) {
        def tenant = factory.createTenant()
        def response = methods.createTenant(token, tenant)
        assert (response.status == SC_CREATED)
        response.getEntity(Tenant)
    }

    def getTenant(String token, String tenantId) {
        def response = methods.getTenant(token, tenantId)
        assert (response.status == SC_OK)
        response.getEntity(Tenant)
    }
}
