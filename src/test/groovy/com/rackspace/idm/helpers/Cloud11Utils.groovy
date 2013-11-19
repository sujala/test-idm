package com.rackspace.idm.helpers

import com.rackspacecloud.docs.auth.api.v1.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import spock.lang.Shared
import testHelpers.Cloud11Methods
import testHelpers.V1Factory
import testHelpers.V2Factory

import javax.annotation.PostConstruct


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

    def createUser(kwargs = [username:testUtils.getRandomUUID(), key:testUtils.getRandomUUID(), mossoId:testUtils.getRandomInteger(), nastId:testUtils.getRandomUUID(), enabled:true]) {
        User user = v1Factory.createUser(kwargs.username, kwargs.key, kwargs.mossoId, kwargs.nastId, kwargs.enabled)
        def response = methods.createUser(user)
        assert(response.status == 201)
        response.getEntity(User)
    }

}
