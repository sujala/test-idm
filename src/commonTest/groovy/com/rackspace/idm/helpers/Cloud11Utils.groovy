package com.rackspace.idm.helpers

import com.rackspacecloud.docs.auth.api.v1.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.Cloud11Methods
import testHelpers.V1Factory
import testHelpers.V2Factory

import static com.rackspace.idm.Constants.*
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

    def createUser(String username=testUtils.getRandomUUID(), String key=testUtils.getRandomUUID(), Integer mossoId=testUtils.getRandomInteger(), String nastId=testUtils.getRandomUUID(), Boolean enabled=true) {
        User user = v1Factory.createUser(username, key, mossoId, nastId, enabled)
        def response = methods.createUser(user)
        assert(response.status == SC_CREATED)
        response.getEntity(User)
    }

    def getUserByName(String username) {
        def response = methods.getUserByName(username)
        assert (response.status == SC_OK)
        response.getEntity(User)
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

    def setUserKey(User user, String key=testUtils.getRandomUUID()) {
        def userWithKey = v1Factory.createUserWithOnlyKey(key)
        def response = methods.setUserKey(user.id, userWithKey)
        assert (response.status == SC_OK)
        response.getEntity(User)
    }

    def authenticateWithKey(String id, String key) {
        def cred = v1Factory.createUserKeyCredentials(id, key)
        def response = methods.authenticate(cred)
        assert (response.status == SC_OK)
        response.getEntity(AuthData)
    }

    def getToken(def username, def key = DEFAULT_API_KEY) {
        def authData = authenticateWithKey(username, key)
        return authData.token.id
    }

    def addBaseUrl(id=testUtils.getRandomInteger(), serviceName="serviceName", region="ORD", enabled=true, defaul=true, publicURL="http://public.com/v1", adminURL="http://adminURL.com/v1", internalURL="http://internalURL.com/v1", userType="NAST") {
        def baseUrl = v1Factory.createBaseUrl(id,serviceName, region, enabled, defaul, publicURL, adminURL, internalURL)
        baseUrl.userType = userType
        def response = methods.addBaseUrl(baseUrl)
        assert (response.status == SC_CREATED)
        response
    }

    def baseUrlIdFromLocation(String location){
        String[] parts = location.split('/')
        return parts[parts.length - 1]
    }

    def addBaseUrlRef(String username, String baseUrlId, boolean v1Default=false) {
        def baseUrlRef = v1Factory.createBaseUrlRef(Integer.valueOf(baseUrlId), null, v1Default)
        def response = methods.addBaseUrlRefs(username, baseUrlRef)
        //This should be a 200 since its only adding a reference, but its been like this for a while.
        assert (response.status == SC_CREATED)
        response.getEntity(BaseURLRef)
    }

    def getBaseURLById(String id) {
        def response = methods.getBaseURLById(id)
        assert (response.status == SC_OK)
        response.getEntity(BaseURL)
    }

    void validateV1Default(List<BaseURLRef> baseURLRefList){
        def mossoV1Def = MOSSO_V1_DEF
        def nastV1Def = NAST_V1_DEF
        for(BaseURLRef baseURLRef : baseURLRefList){
            String baseUrlRefId = baseURLRef.id
            if(mossoV1Def.contains(baseUrlRefId)){
                assert (baseURLRef.v1Default == true)
            } else if(nastV1Def.contains(baseUrlRefId)){
                assert (baseURLRef.v1Default == true)
            } else {
                assert (baseURLRef.v1Default == false)
            }
        }
    }
}
