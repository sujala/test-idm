package com.rackspace.idm.api.resource.cloud.v11

import com.rackspacecloud.docs.auth.api.v1.User
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

public class Cloud11UserIntegrationTest extends RootIntegrationTest{

    def "Valid GET operations - username with special characters" () {
        given:
        User user = utils11.createUser(testUtils.getRandomUUID('test@test'))

        when:
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
    }
}
