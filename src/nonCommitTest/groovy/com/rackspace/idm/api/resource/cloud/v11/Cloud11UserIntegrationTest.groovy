package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.User
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

public class Cloud11UserIntegrationTest extends RootIntegrationTest{

    def "Valid GET operations - username with special characters" () {
        given:
        User user = utils11.createUser(testUtils.getRandomUUID('test@test'))
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)

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
        assert (groups.group.id.contains(Constants.DEFAULT_GROUP))

        then:
        true

        cleanup:
        utils.deleteUser(user20)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(String.valueOf(user.mossoId))
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
        utils.deleteUser(user20)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(String.valueOf(user.mossoId))
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
        secretQA.answer == Constants.DEFAULT_SECRET_ANWSER
        updatedSecretQA.question == Constants.DEFAULT_RAX_KSQA_SECRET_QUESTION
        updatedSecretQA.answer == Constants.DEFAULT_RAX_KSQA_SECRET_ANWSER

        cleanup:
        utils.deleteUser(user20)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(String.valueOf(user.mossoId))
    }

    def "Verify v1Defaults on user creation" () {
        when:
        User user = utils11.createUser(testUtils.getRandomUUID('testV1Default'))
        User getUser = utils11.getUserByName(user.id)
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)

        then:
        utils11.validateV1Default(user.baseURLRefs.baseURLRef)
        utils11.validateV1Default(getUser.baseURLRefs.baseURLRef)

        cleanup:
        utils.deleteUser(user20)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(String.valueOf(user.mossoId))
    }

    def "Replacing v1Default on existing service on user" () {
        given:
        User user = utils11.createUser(testUtils.getRandomUUID('testNewV1Default'))
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)

        when:
        def addBaseUrlResponse = utils11.addBaseUrl(testUtils.getRandomInteger(), "cloudFiles")
        def baseUrlLocation = addBaseUrlResponse.getHeaders().get('Location')[0]
        def baseUrlId = utils11.baseUrlIdFromLocation(baseUrlLocation)
        utils11.addBaseUrlRef(user.id, baseUrlId, true)

        User updatedUser = utils11.getUserByName(user.id)

        then:
        for(BaseURLRef baseURLRef : updatedUser.baseURLRefs.baseURLRef) {
            String baseUrlRefId = baseURLRef.id
            def baseUrl = utils11.getBaseURLById(baseUrlRefId)
            if(baseUrl.getServiceName().equals("cloudFiles") && !baseUrlRefId.equals(baseUrlId)) {
                assert (baseURLRef.v1Default == false)
            }
            if(baseUrlRefId == baseUrlId){
                assert (baseURLRef.v1Default == true)
            }
        }

        cleanup:
        utils.deleteUser(user20)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(String.valueOf(user.mossoId))
    }

    def "Verify attributes on the v1.1 create user" () {
        given:
        String username=testUtils.getRandomUUID()
        String key=testUtils.getRandomUUID()
        Integer mossoId=testUtils.getRandomInteger()
        String nastId=testUtils.getRandomUUID()
        Boolean enabled=true

        when:
        def user = utils11.createUser(username, key, mossoId, nastId, enabled)
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)

        then:
        user.nastId != nastId
        user.mossoId == mossoId
        user.enabled == true
        user.key == key

        cleanup:
        utils.deleteUserQuietly(user20)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(String.valueOf(user.mossoId))
    }

    def "default user group list JSON doesn't have extra data [D-17806]"() {
        given:
        String username=testUtils.getRandomUUID()
        String key=testUtils.getRandomUUID()
        Integer mossoId=testUtils.getRandomInteger()
        String nastId=testUtils.getRandomUUID()
        Boolean enabled=true

        when:
        def user = utils11.createUser(username, key, mossoId, nastId, enabled)
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)
        def response = cloud11.getGroups(username, MediaType.APPLICATION_JSON)

        then:
        response.status == 200
        def body = response.getEntity(String.class)
        def slurper = new JsonSlurper().parseText(body)
        slurper.getAt("valuess") == null

        cleanup:
        utils.deleteUser(user20)
    }

    @Unroll
    def "getUserFromNastId: get correct user-admin - feature.enable.user.admin.look.up.by.domain = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, featureEnabled)
        def user = utils11.createUser()
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)

        when:
        def response = cloud11.getUserFromNastId(user.nastId)

        then:
        response.status == HttpStatus.SC_MOVED_PERMANENTLY

        cleanup:
        utils.deleteUser(user20)
        reloadableConfiguration.reset()

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "getUserFromMossoId: get correct user-admin - feature.enable.user.admin.look.up.by.domain = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, featureEnabled)
        def user = utils11.createUser()
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)

        when:
        def response = cloud11.getUserFromMossoId(String.valueOf(user.mossoId))

        then:
        response.status == HttpStatus.SC_MOVED_PERMANENTLY

        cleanup:
        utils.deleteUser(user20)
        reloadableConfiguration.reset()

        where:
        featureEnabled << [true, false]
    }
}
