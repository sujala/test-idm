package com.rackspace.idm.helpers

import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList
import com.rackspacecloud.docs.auth.api.v1.GroupsList
import com.rackspacecloud.docs.auth.api.v1.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.Cloud11Methods
import testHelpers.V1Factory
import testHelpers.V2Factory

import javax.annotation.PostConstruct

import static org.apache.http.HttpStatus.*

@Component
class Cloud11Utils {

    @Autowired
    Cloud11Methods methods

    @Autowired
    V2Factory factory

    @Autowired
    V1Factory v1Factory

    @Autowired
    CloudTestUtils testUtils

    @PostConstruct
    def init() {
        methods.init()
    }

    def createUser(String username=testUtils.getRandomUUID(), String key=testUtils.getRandomUUID(), Integer mossoId=testUtils.getRandomInteger(), String nastId=testUtils.getRandomUUID(), Boolean enabled=true) {
        User user = v1Factory.createUser(username, key, mossoId, nastId, enabled)
        def response = methods.createUser(user)
        assert(response.status == SC_CREATED)
        response.getEntity(User)
    }

    def deleteUser(User user) {
        def response = methods.deleteUser(user.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def getUserByName(String username) {
        def response = methods.getUserByName(username)
        assert (response.status == SC_OK)
    }

    def getUserEnabled(User user) {
        def response = methods.getUserEnabled(user.id)
        assert (response.status == SC_OK)
        response.getEntity(User)
    }

    def getUserKey(User user) {
        def response = methods.getUserKey(user.id)
        assert (response.status == SC_OK)
        response.getEntity(User)
    }

    def getBaseURLRefs(User user) {
        def response = methods.getBaseURLRefs(user.id)
        assert (response.status == SC_OK)
        response.getEntity(BaseURLRefList)
    }

    def getUserGroups(User user) {
        def response = methods.getGroups(user.id)
        assert (response.status == SC_OK)
        response.getEntity(GroupsList)
    }

    def getUserBaseURLRef(User user, String baseUrlRefId) {
        def response = methods.getUserBaseURLRef(user.id, baseUrlRefId)
        assert (response.status == SC_OK)
        response.getEntity(BaseURLRef)
    }
}
