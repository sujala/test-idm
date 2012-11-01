package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.sun.jersey.api.client.ClientResponse
import spock.lang.Shared
import spock.lang.Specification
import javax.xml.namespace.QName
import org.openstack.docs.identity.api.v2.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import com.sun.jersey.api.client.WebResource
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capability
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions


class Cloud20IntegrationTest extends Specification {
    @Shared WebResource resource
    @Shared JAXBObjectFactories objFactories;

    @Shared def path = "cloud/v2.0/"
    @Shared def serviceAdminToken
    @Shared def identityAdminToken
    @Shared def userAdminToken
    @Shared def defaultUserToken

    @Shared def serviceAdmin
    @Shared def identityAdmin
    @Shared def userAdmin
    @Shared def defaultUser
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom

    def randomness = UUID.randomUUID()
    static def X_AUTH_TOKEN = "X-Auth-Token"
    @Shared def groupLocation
    @Shared def group
    @Shared Region sharedRegion

    static def RAX_GRPADM= "RAX-GRPADM"
    static def RAX_AUTH = "RAX-AUTH"


    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-', "")

        this.resource = ensureGrizzlyStarted("classpath:app-config.xml");
        this.objFactories = new JAXBObjectFactories()
        serviceAdminToken = authenticate("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = getUserByName(serviceAdminToken, "authQE").getEntity(User)
        identityAdminToken = authenticate("auth", "auth123").getEntity(AuthenticateResponse).value.token.id
        identityAdmin = getUserByName(serviceAdminToken, "auth").getEntity(User)
        //User Admin
        def userAdminResponse = getUserByName(identityAdminToken, "testUserAdmin_doNotDeleteMe")
        if (userAdminResponse.getStatus() == 404) {
            userAdmin = createUser(identityAdminToken, userForCreate("testUserAdmin_doNotDeleteMe", "display", "test@rackspace.com", true, "ORD", "domainId", "Password1")).getEntity(User)
        } else if (userAdminResponse.getStatus() == 200) {
            userAdmin = userAdminResponse.getEntity(User)
        }
        userAdminToken = authenticate("testUserAdmin_doNotDeleteMe", "Password1").getEntity(AuthenticateResponse).value.token.id
        //Default User
        def defaultUserResponse = getUserByName(userAdminToken, "testDefaultUser_doNotDeleteMe")
        if (defaultUserResponse.getStatus() == 404) {
            defaultUser = createUser(userAdminToken, userForCreate("testDefaultUser_doNotDeleteMe", "display", "test@rackspace.com", true, null, null, "Password1")).getEntity(User)
        } else if (defaultUserResponse.getStatus() == 200) {
            defaultUser = defaultUserResponse.getEntity(User)
        }
        defaultUserToken = authenticate("testDefaultUser_doNotDeleteMe", "Password1").getEntity(AuthenticateResponse).value.token.id

        //create group
        def createGroupResponse = createGroup(serviceAdminToken, group("group$sharedRandom", "this is a group"))
        groupLocation = createGroupResponse.location
        def getGroupResponse = getGroup(serviceAdminToken, groupLocation)
        group = getGroupResponse.getEntity(Group).value

        def createRegionResponse = createRegion(serviceAdminToken, region("region$sharedRandom", true, false))
        def getRegionResponse = getRegion(serviceAdminToken, "region$sharedRandom")
        sharedRegion = getRegionResponse.getEntity(Region)

    }

    def cleanupSpec() {
        deleteGroup(serviceAdminToken, group.getId())
        deleteRegion(serviceAdminToken, sharedRegion.getName())
    }

    def 'User CRUD'() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = userForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = createUser(serviceAdminToken, user)
        //Get user
        def getUserResponse = getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User)
        //Get user by id
        def getUserByIdResponse = getUserById(serviceAdminToken, userEntity.getId())
        def getUserByNameResponse = getUserByName(serviceAdminToken, userEntity.getUsername())
        //Update User
        def userForUpdate = userForUpdate(null, "updatedBob" + random, "Bob", "test@rackspace.com", true, null, null)
        def updateUserResponse = updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = deleteUser(serviceAdminToken, userEntity.getId())
        //Hard delete user
        def hardDeleteResponses = hardDeleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        getUserByIdResponse.status == 200
        getUserByNameResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponses.status == 204
    }

    def "operations on non-existent users return 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                getUserById(serviceAdminToken, "badId"),
                getUserByName(serviceAdminToken, "badName"),
                updateUser(serviceAdminToken, "badId", new User()),
                deleteUser(serviceAdminToken, "badId"),
                addCredential(serviceAdminToken, "badId", getCredentials("someUser", "SomePassword1"))

        ]
    }

    def "invalid operations on create/update user returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createUser(serviceAdminToken, userForCreate("!@#What", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("1one", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("one name", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate(null, "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "junk!@#", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", null, true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Pop1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "longpassword1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Longpassword")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", "someId", "Longpassword")),
                createUser(identityAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Longpassword1")),
                updateUser(userAdminToken, defaultUser.getId(), userForUpdate("1", "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
                updateUser(defaultUserToken, defaultUser.getId(), userForUpdate(null, "someOtherName", "someOtherDisplay", "some@rackspace.com", false, "ORD", "SomeOtherPassword1"))
        ]
    }

    def 'operations with invalid tokens'() {
        expect:
        response.status == 401

        where:
        response << [
                createUser("invalidToken", userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(null, userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),    \
                  getUserById("invalidToken", "badId"),
                getUserById(null, "badId"),
                getUserByName("invalidToken", "badId"),
                getUserByName(null, "badId"),
                updateUser("invalidToken", "badId", new User()),
                updateUser(null, "badId", new User()),
                deleteUser("invalidToken", "badId"),
                deleteUser(null, "badId"),
                listUser("invalidToken"),
                listUser(null),
                getCapabilities("invalidToken", "badId"),
                getCapabilities(null, "badId")
        ]

    }

    def 'forbidden operations for users'() {
        expect:
        response.status == 403

        where:
        response << [
                createUser(defaultUserToken, userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                updateUser(defaultUserToken, userAdmin.getId(), userForUpdate(null, "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
                getUserById(defaultUserToken, userAdmin.getId()),
                getUserById(defaultUserToken, identityAdmin.getId()),
                getUserById(defaultUserToken, serviceAdmin.getId()),
                getUserById(userAdminToken, identityAdmin.getId()),
                getUserById(userAdminToken, serviceAdmin.getId()),
                getUserByName(defaultUserToken, userAdmin.getUsername()),
                getUserByName(defaultUserToken, identityAdmin.getUsername()),
                getUserByName(defaultUserToken, serviceAdmin.getUsername()),
                getUserByName(userAdminToken, identityAdmin.getUsername()),
                getUserByName(userAdminToken, serviceAdmin.getUsername()),
                createGroup(defaultUserToken, group()),
                updateGroup(defaultUserToken, group.getId(), group()),
                deleteGroup(defaultUserToken, group.getId()),
                getGroup(defaultUserToken, groupLocation),
                getGroups(defaultUserToken),
                getCapabilities(defaultUserToken, "badId"),
                getCapabilities(userAdminToken, "badId"),
                createRegion(defaultUserToken, region()),
                updateRegion(defaultUserToken, sharedRegion.getName(), sharedRegion),
                deleteRegion(defaultUserToken, sharedRegion.getName()),
                getRegion(defaultUserToken, sharedRegion.getName()),
                createQuestion(defaultUserToken, question()),
                updateQuestion(defaultUserToken, "id", question()),
                deleteQuestion(defaultUserToken, "id"),
        ]
    }

    def 'valid operations on retrieving users'() {
        expect:
        response.status == 200

        where:
        response << [
                listUser(serviceAdminToken),
                listUser(identityAdminToken),
                listUser(userAdminToken),
                listUser(defaultUserToken),
                getUserById(defaultUserToken, defaultUser.getId()),
                getUserById(userAdminToken, defaultUser.getId()),
                getUserById(userAdminToken, userAdmin.getId()),
                getUserById(identityAdminToken, userAdmin.getId()),
                getUserById(serviceAdminToken, userAdmin.getId()),
                getUserByName(defaultUserToken, defaultUser.getUsername()),
                getUserByName(userAdminToken, defaultUser.getUsername()),
                getUserByName(userAdminToken, userAdmin.getUsername()),
                getUserByName(identityAdminToken, userAdmin.getUsername()),
                getUserByName(serviceAdminToken, userAdmin.getUsername())
        ]

    }

    def "Group CRUD"() {
        when:
        def random = ("$randomness").replace('-', "")
        def createGroupResponse = createGroup(serviceAdminToken, group("group$random", "this is a group"))

        def getGroupResponse = getGroup(serviceAdminToken, createGroupResponse.location)
        def groupEntity = getGroupResponse.getEntity(Group)
        def groupId = groupEntity.value.id

        def getGroupsResponse = getGroups(serviceAdminToken)
        def groupsEntity = getGroupsResponse.getEntity(Groups)

        def updateGroupResponse = updateGroup(serviceAdminToken, groupId, group("group$random", "updated group"))

        def deleteGroupResponse = deleteGroup(serviceAdminToken, groupId)


        then:
        createGroupResponse.status == 201
        createGroupResponse.location != null
        getGroupResponse.status == 200
        getGroupsResponse.status == 200
        groupsEntity.value.getGroup().size() > 0
        updateGroupResponse.status == 200
        deleteGroupResponse.status == 204
    }

    def "Group Assignment CRUD"() {
        when:
        def addUserToGroupResponse = addUserToGroup(serviceAdminToken, group.getId(), defaultUser.getId())

        def listGroupsForUserResponse = listGroupsForUser(serviceAdminToken, defaultUser.getId())
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        def getUsersFromGroupResponse = getUsersFromGroup(serviceAdminToken, group.getId())
        def users = getUsersFromGroupResponse.getEntity(UserList).value

        def removeUserFromGroupRespone = removeUserFromGroup(serviceAdminToken, group.getId(), defaultUser.getId())

        then:
        addUserToGroupResponse.status == 204

        listGroupsForUserResponse.status == 200
        groups.getGroup().size() == 1
        getUsersFromGroupResponse.status == 200
        users.getUser().size() == 1

        removeUserFromGroupRespone.status == 204
    }

    def "invalid operations on create/update group returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createGroup(serviceAdminToken, group(null, "this is a group")),
                createGroup(serviceAdminToken, group("", "this is a group")),
                createGroup(serviceAdminToken, group("group", null)),
                updateGroup(serviceAdminToken, group.getId(), group(null, "this is a group")),
                updateGroup(serviceAdminToken, group.getId(), group("", "this is a group")),
                updateGroup(serviceAdminToken, group.getId(), group("group", null)),
                addUserToGroup(serviceAdminToken, "doesnotexist", defaultUser.getId()),
        ]
    }

    def "invalid operations on create/update group returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                addUserToGroup(serviceAdminToken, group.getId(), "doesnotexist"),
        ]
    }


//    def "CRUD capabilities"(){
    //        when:
    //        List<Capability> capabilityList = new ArrayList<Capability>();
    //        def capability1 = createCapability("GET","id","name","http://someUrl")
    //        def capability2 = createCapability("GET","id","name","http://someUrl")
    //        capabilityList.add(capability1)
    //        capabilityList.add(capability2)
    //        Capabilities capabilities = createCapabilities(capabilityList)
    //        updateCapabilities(serviceAdminToken,"1000",capabilities)
    //
    //        then:
    //    }

    def "operations on non-existing endpoint Template returns 'not found'"() {
        expect:
        response.status == 404
        where:
        response << [
                getCapabilities(serviceAdminToken, "1000001234")
        ]
    }


    def "update region name is not allowed"() {
        given:
        Region region1 = region("somename", false, false)

        when:
        def updateRegionResponse = updateRegion(serviceAdminToken, sharedRegion.getName(), region1)

        then:
        updateRegionResponse.status == 400
    }

    def "region crud"() {
        given:
        def random = ("$randomness").replace('-', "")
        def regionName = "region${random}"
        Region region1 = region(regionName, false, false)
        Region region2 = region(regionName, true, true)

        when:
        def createRegionResponse = createRegion(serviceAdminToken, region1)
        def getRegionResponse = getRegion(serviceAdminToken, regionName)
        Region createdRegion = getRegionResponse.getEntity(Region)

        def updateRegionResponse = updateRegion(serviceAdminToken, regionName, region2)
        def getUpdatedRegionResponse = getRegion(serviceAdminToken, regionName)
        Region updatedRegion = getUpdatedRegionResponse.getEntity(Region)

        def getRegionsResponse = getRegions(serviceAdminToken)
        Regions regions = getRegionsResponse.getEntity(Regions)

        def deleteRegionResponse = deleteRegion(serviceAdminToken, regionName)
        def getDeletedRegionResponse = getRegion(serviceAdminToken, regionName)


        then:
        createRegionResponse.status == 201
        createRegionResponse.location != null
        getRegionResponse.status == 200
        region1.name.equals(createdRegion.name)
        region1.enabled.equals(createdRegion.enabled)
        region1.isDefault.equals(createdRegion.isDefault)
        updateRegionResponse.status == 204
        region2.name.equals(updatedRegion.name)
        region2.enabled.equals(updatedRegion.enabled)
        region2.isDefault.equals(updatedRegion.isDefault)
        getRegionsResponse.status == 200
        regions.region.size() > 0
        deleteRegionResponse.status == 204
        getDeletedRegionResponse.status == 404
    }

    def "invalid operations on region returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [updateRegion(serviceAdminToken, "notfound", region()),
                deleteRegion(serviceAdminToken, "notfound"),
                getRegion(serviceAdminToken, "notfound"),
        ]
    }

    def "bad operations on capabilities return 'bad request'" () {
        expect:
        response.status == 400

        where:
        response << [
                getCapabilities(serviceAdminToken, "blah"),
                updateCapabilities(serviceAdminToken, "blah", createCapabilities(createCapability("GET", "get_server", "get_server", "http://someUrl", "desc", null)))
        ]
    }

    def "invalid operations on create regions returns 'bad request'"() {
        expect:
        response.status == 400
        where:
        response << [
                createRegion(serviceAdminToken, region(null, true, false)),
        ]
    }

    def "create region that already exists returns conflict"() {
        when:
        def createRegionResponse = createRegion(serviceAdminToken, sharedRegion)

        then:
        createRegionResponse.status == 409
    }

    def "question crud"() {
        given:
        def random = ("$randomness").replace('-', "")
        def questionId = "region${random}"
        def question1 = question(questionId, "question")
        def question2 = question(questionId, "question changed")

        when:
        def createResponse = createQuestion(serviceAdminToken, question1)
        def getCreateResponse = getQuestion(serviceAdminToken, questionId)
        def createEntity = getCreateResponse.getEntity(Question)

        def updateResponse = updateQuestion(serviceAdminToken, questionId, question2)
        def getUpdateResponse = getQuestion(serviceAdminToken, questionId)
        def updateEntity = getUpdateResponse.getEntity(Question)

        def deleteResponse = deleteQuestion(serviceAdminToken, questionId)
        def getDeleteResponse = getQuestion(serviceAdminToken, questionId)

        def getQuestionResponse = getQuestions(serviceAdminToken)
        def questions = getQuestionResponse.getEntity(Questions)

        then:
        createResponse.status == 201
        createResponse.location != null
        question1.id == createEntity.id
        question1.question == createEntity.question

        updateResponse.status == 204
        updateEntity.id == question2.id
        updateEntity.question == question2.question

        deleteResponse.status == 204
        getDeleteResponse.status == 404

        getQuestionResponse.status == 200
        questions != null
    }

    def "invalid operations on question returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                updateQuestion(serviceAdminToken, "notfound", question("notfound", "question")),
                deleteQuestion(serviceAdminToken, "notfound"),
                getQuestion(serviceAdminToken, "notfound"),
        ]
    }

    def "invalid operations on question returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                updateQuestion(serviceAdminToken, "ids", question("dontmatch", "question")),
                updateQuestion(serviceAdminToken, "id", question(null, "question")),
                updateQuestion(serviceAdminToken, "id", question("id", null)),
                createQuestion(serviceAdminToken, question("id", null)),
                createQuestion(serviceAdminToken, question(null, "question")),
        ]
    }

    //Resource Calls
    def createUser(String token, user) {
        resource.path(path).path('users').header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def getUser(String token, URI location) {
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listUser(String token) {
        resource.path(path).path('users').accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUserById(String token, String userId) {
        resource.path(path).path('users').path(userId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUserByName(String token, String name) {
        resource.path(path).path('users').queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateUser(String token, String userId, user) {
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def addCredential(String token, String userId, credential) {
        resource.path(path).path('users').path(userId).path('OS-KSADM').path('credentials').entity(credential).header(X_AUTH_TOKEN, token).post(ClientResponse)
    }

    def deleteUser(String token, String userId) {
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def hardDeleteUser(String token, String userId) {
        resource.path(path).path('softDeleted').path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def createGroup(String token, group) {
        resource.path(path).path(RAX_GRPADM).path('groups').header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).post(ClientResponse)
    }

    def getGroup(String token, URI uri) {
        resource.uri(uri).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroups(String token) {
        resource.path(path).path(RAX_GRPADM).path('groups').accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateGroup(String token, String groupId, group) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).put(ClientResponse)
    }

    def deleteGroup(String token, String groupId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addUserToGroup(String token, String groupId, String userId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).put(ClientResponse)
    }

    def removeUserFromGroup(String token, String groupId, String userId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listGroupsForUser(String token, String userId) {
        resource.path(path).path('users').path(userId).path("RAX-KSGRP").accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersFromGroup(String token, String groupId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateCapabilities(String token, String endpointTemplateId, capabilities) {
        resource.path(path).path('OS-KSCATALOG').path('endpointTemplates').path(endpointTemplateId).path('RAX-AUTH').path('capabilities').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).entity(capabilities).put(ClientResponse)
    }

    def getCapabilities(String token, String endpointTemplateId) {
        resource.path(path).path('OS-KSCATALOG').path('endpointTemplates').path(endpointTemplateId).path('RAX-AUTH').path('capabilities').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def authenticate(username, password) {
        resource.path(path + 'tokens').accept(APPLICATION_XML).entity(authenticateRequest(username, password)).post(ClientResponse)
    }

    def createRegion(String token, region) {
        resource.path(path).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).post(ClientResponse)
    }

    def getRegion(String token, String regionId) {
        resource.path(path).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getRegions(String token) {
        resource.path(path).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateRegion(String token, String regionId, region) {
        resource.path(path).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).put(ClientResponse)
    }

    def deleteRegion(String token, String regionId) {
        resource.path(path).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def createQuestion(String token, question) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).post(ClientResponse)
    }

    def getQuestion(String token, questionId) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestions(String token) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateQuestion(String token, String questionId, question) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).put(ClientResponse)
    }

    def deleteQuestion(String token, String questionId) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    //Helper Methods
    def getCredentials(String username, String password) {
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def authenticateRequest(String username, String password) {
        def credentials = getCredentials(username, password)

        def objectFactory = new ObjectFactory()

        new AuthenticationRequest().with {
            it.setCredential(objectFactory.createPasswordCredentials(credentials))
            return it
        }
    }

    def userForUpdate(String id, String username, String displayName, String email, Boolean enabled, String defaultRegion, String password) {
        new User().with {
            it.id = id
            it.username = username
            it.email = email
            it.enabled = enabled
            it.displayName = displayName
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
            if (defaultRegion != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), defaultRegion)
            }
            return it
        }
    }

    def userForCreate(String username, String displayName, String email, Boolean enabled, String defaultRegion, String domainId, String password) {
        new User().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            if (defaultRegion != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), defaultRegion)
            }
            if (domainId != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "domainId"), domainId)
            }
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
            return it
        }
    }

    def group(String name, String description) {
        new Group().with {
            it.name = name
            it.description = description
            return it
        }
    }

    def group() {
        return group("group", "description")
    }

    def createCapability(String action, String id, String name, String url, String description, List<String> resources) {
        new Capability().with {
            it.action = action
            it.id = id
            it.name = name
            it.url = url
            it.description = description
            if (resources != null) {
                for (String resource: resources) {
                    it.resources.add(resource)
                }
            }

        }

    }


    def region(String name, Boolean enabled, Boolean isDefault) {
        Region regionEntity = new Region().with {
            it.name = name
            it.enabled = enabled
            it.isDefault = isDefault
            return it
        }
    }

    def createCapabilities(Capability capability) {
        new Capabilities().with {
            it.capability.add(capability)
            return it
        }
    }

    def region(String name) {
        return region(name, true, false)
    }

    def region() {
        return region("name", true, false)
    }

    def question() {
        question("id", "question")
    }

    def question(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }
}
