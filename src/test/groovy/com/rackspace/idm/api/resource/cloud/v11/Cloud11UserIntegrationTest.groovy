package com.rackspace.idm.api.resource.cloud.v11

import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.User
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

public class Cloud11UserIntegrationTest extends RootIntegrationTest{

    def "Valid GET operations - username with special characters" () {
        given:
        User user = utils11.createUser(testUtils.getRandomUUID('test@test'))

        when:
        utils11.authenticateWithKey(user.id, user.key)
        utils11.getUserByName(user.id)
        def userEnabled = utils11.getUserEnabled(user)
        assert (userEnabled.id == user.id)

        def userKey = utils11.getUserKey(user)
        assert (userKey.id == user.id)

        def userBaseUrlRefs = utils11.getBaseURLRefs(user)
        assert (userBaseUrlRefs.baseURLRef.size() != 0)

        def baseURLRefId = userBaseUrlRefs.baseURLRef.id[0]
        def userBaseUrlRef = utils11.getUserBaseURLRef(user, baseURLRefId.toString())
        assert (userBaseUrlRef.id == baseURLRefId)

        def groups = utils11.getUserGroups(user)
        assert (groups.group.id.contains(DEFAULT_GROUP))

        then:
        true

        cleanup:
        utils11.deleteUser(user)
        utils.deleteDomain(String.valueOf(user.mossoId))
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
    }

    def "Update user's apiKey - validate encryption" () {
        given:
        User user = utils11.createUser(testUtils.getRandomUUID('testApiKey'))
        String key = "key"

        when:
        def userKey = utils11.getUserKey(user)
        assert (userKey.id == user.id)
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)
        utils.authenticateApiKey(user20, userKey.key)
        utils11.setUserKey(user, key)
        userKey = utils11.getUserKey(user)
        assert (userKey.key == key)
        utils.authenticateApiKey(user20, userKey.key)
        utils.resetApiKey(user20)
        utils.addApiKeyToUser(user20)
        def cred = utils.getUserApiKey(user20)

        then:
        utils.authenticateApiKey(user20, cred.apiKey)

        cleanup:
        utils11.deleteUser(user)
        utils.deleteDomain(String.valueOf(user.mossoId))
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
    }

    def "Add/Update user's secretQA - validate encryption" () {
        given:
        User user = utils11.createUser(testUtils.getRandomUUID('testSecretQA'))
        String key = "key"

        when:
        utils11.setUserKey(user, key)
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)
        utils.createSecretQA(user20)
        def secretQA = utils.getSecretQA(user20)
        utils.updateSecretQA(user20)
        def updatedSecretQA = utils.getSecretQA(user20)
        utils.authenticateApiKey(user20, key)

        then:
        secretQA.answer == DEFAULT_SECRET_ANWSER
        updatedSecretQA.question == DEFAULT_RAX_KSQA_SECRET_QUESTION
        updatedSecretQA.answer == DEFAULT_RAX_KSQA_SECRET_ANWSER

        cleanup:
        utils11.deleteUser(user)
        utils.deleteDomain(String.valueOf(user.mossoId))
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
    }

    def "Verify v1Defaults on user creation" () {
        when:
        User user = utils11.createUser(testUtils.getRandomUUID('testV1Default'))
        User getUser = utils11.getUserByName(user.id)


        then:
        validateV1Default(user.baseURLRefs.baseURLRef)
        validateV1Default(getUser.baseURLRefs.baseURLRef)

        cleanup:
        utils11.deleteUser(user)
        utils.deleteDomain(String.valueOf(user.mossoId))
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
    }

    def "Replacing v1Default on existing service on user" () {
        given:
        User user = utils11.createUser(testUtils.getRandomUUID('testNewV1Default'))

        when:
        def addBaseUrlResponse = utils11.addBaseUrl(testUtils.getRandomInteger(), "cloudFiles")
        def baseUrlLocation = addBaseUrlResponse.getHeaders().get('Location')[0]
        def baseUrlId = utils11.baseUrlIdFromLocation(baseUrlLocation)
        utils11.addBaseUrlRef(user.id, baseUrlId, true)

        User updatedUser = utils11.getUserByName(user.id)

        then:
        for(BaseURLRef baseURLRef : updatedUser.baseURLRefs.baseURLRef) {
            String baseUrlRefId = baseURLRef.id
            if(baseUrlRefId == NAST_V1_DEF[0]){
                assert (baseURLRef.v1Default == false)
            }
            if(baseUrlRefId == baseUrlId){
                assert (baseURLRef.v1Default == true)
            }
        }

        cleanup:
        utils11.deleteUser(user)
        utils.deleteDomain(String.valueOf(user.mossoId))
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
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
