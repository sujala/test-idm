package com.rackspace.idm.api.resource.cloud

import com.rackspace.idm.JSONConstants
import org.json.simple.JSONObject
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse
import spock.lang.Specification

class JsonWriterHelperTest extends Specification {

    def "getTokenUser does not set federated if attribute is null"() {
        given:
        def userAuthResponse = new UserForAuthenticateResponse().with {
            it.id = "userId"
            it.name = "userName"
            it.defaultRegion = "region"
            it
        }

        when:
        def userJson = JsonWriterHelper.getTokenUser(userAuthResponse)

        then:
        !userJson.containsKey(JSONConstants.RAX_AUTH_FEDERATED)
    }

    def "getTokenUser sets federated if attribute is not null"() {
        given:
        def userAuthResponse = new UserForAuthenticateResponse().with {
            it.id = "userId"
            it.name = "userName"
            it.defaultRegion = "region"
            it.federated = true
            it
        }

        when:
        def userJson = JsonWriterHelper.getTokenUser(userAuthResponse)

        then:
        userJson.containsKey(JSONConstants.RAX_AUTH_FEDERATED)
    }

}

