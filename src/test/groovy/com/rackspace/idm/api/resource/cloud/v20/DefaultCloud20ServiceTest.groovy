package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.converter.cloudv20.QuestionConverterCloudV20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.dao.impl.LdapQuestionRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessPeristenceRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.impl.*
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capability
import com.rackspace.idm.domain.dao.impl.LdapCapabilityRepository
import com.rackspace.idm.api.converter.cloudv20.CapabilityConverterCloudV20

/*
 This class uses the application context but mocks the ldap interactions
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultCloud20ServiceTest extends Specification {

    @Autowired DefaultUserService userService
    @Autowired Configuration configuration
    @Autowired DefaultCloud20Service cloud20Service
    @Autowired DefaultQuestionService questionService
    @Autowired QuestionConverterCloudV20 questionConverter
    @Autowired DefaultCapabilityService capabilityService
    @Autowired CapabilityConverterCloudV20 capabilityConverter

    @Shared DefaultScopeAccessService scopeAccessService
    @Shared DefaultAuthorizationService authorizationService
    @Shared DefaultCloudRegionService cloudRegionService
    @Shared LdapUserRepository userDao
    @Shared LdapScopeAccessPeristenceRepository scopeAccessDao
    @Shared LdapQuestionRepository questionDao
    @Shared LdapCapabilityRepository capabilityDao


    @Shared def authToken = "token"
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def questionId = "id"
    @Shared JAXBObjectFactories objFactories;

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
        objFactories = new JAXBObjectFactories()
    }

    def cleanupSpec() {
    }

    def "create a user sets the default region"() {
        given:
        createMocks()
        allowAccess()

        userDao.isUsernameUnique(_) >> true

        cloudRegionService.getDefaultRegion("US") >> region()

        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        when:
        userService.addUser(user)

        then:
        user.region == "DFW"
    }

    def "create user without default region throws bad request"() {
        given:
        createMocks()
        allowAccess()

        userDao.isUsernameUnique(_) >> true

        cloudRegionService.getDefaultRegion("US") >> null

        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        when:
        userService.addUser(user)

        then:
        thrown(BadRequestException)
    }

    def "question create verifies Identity admin level access"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.addQuestion(uriInfo(), authToken, jaxbQuestion())

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_);
    }

    def "question update verifies Identity admin level access"() {
        given:
        createMocks()
        allowAccess()

        questionDao.getObject(_) >> question()

        when:
        cloud20Service.updateQuestion(authToken, questionId, jaxbQuestion())

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_);
    }

    def "question delete verifies Identity admin level access"() {
        given:
        createMocks()
        allowAccess()

        questionDao.getObject(_) >> question()

        when:
        cloud20Service.deleteQuestion(authToken, questionId)

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_);
    }

    def "get question verifies default and user admin"() {
        given:
        createMocks()
        allowAccess()
        def questions = new ArrayList<Question>()
        def question = question()
        questions.add(question)

        questionDao.getObject(_) >> question
        questionDao.getObjects(_) >> questions

        when:
        cloud20Service.getQuestion(authToken, questionId)
        cloud20Service.getQuestions(authToken)

        then:
        2 *  authorizationService.verifyUserLevelAccess(_);
    }

    def "add question calls ldap"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.addQuestion(uriInfo(), authToken, jaxbQuestion())

        then:
        1 * questionDao.addObject(_)
    }

    def "add question returns 200 on success and location header"() {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.addQuestion(uriInfo(), authToken, jaxbQuestion())

        then:
        Response response = responseBuilder.build()
        response.status == 201
        response.getMetadata().get("location").get(0) != null
    }

    def "get question returns 200 and question"() {
        given:
        createMocks()
        allowAccess()

        questionDao.getObject(_) >> question()

        when:
        def responseBuilder = cloud20Service.getQuestion(authToken, questionId)

        then:
        Response response = responseBuilder.build()
        response.status == 200
        response.entity != null
    }

    def "get questions returns 200 and questions"() {
        given:
        createMocks()
        allowAccess()
        def questions = new ArrayList<Question>()

        questionDao.getObjects(_) >> questions

        when:
        def responseBuilder = cloud20Service.getQuestions(authToken)

        then:
        Response response = responseBuilder.build()
        response.status == 200
        response.entity != null

    }

    def "update question calls ldap update"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getObject(_) >> question()

        when:
        cloud20Service.updateQuestion(authToken, questionId, jaxbQuestion())

        then:
        1 * questionDao.updateObject(_)
    }

    def "update question returns 204"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getObject(_) >> question()

        when:
        def responseBuilder = cloud20Service.updateQuestion(authToken, questionId, jaxbQuestion())

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "delete question calls ldap"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getObject(_) >> question()

        when:
        cloud20Service.deleteQuestion(authToken, questionId)

        then:
        1 * questionDao.deleteObject(_)
    }

    def "delete question returns 204"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getObject(_) >> question()

        when:
        def responseBuilder = cloud20Service.deleteQuestion(authToken, questionId)

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "updateCapabilities return 201" (){
        given:
        createMocks()
        allowAccess()
        Capabilities capabilities = new Capabilities();
        capabilities.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null))
        capabilityDao.getNextCapabilityId() >> "123321"
        capabilityDao.getObjects(_) >> capabilities
        when:
        def responseBuilder = cloud20Service.updateCapabilities(authToken,capabilities,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

//    def "updateCapabilities return 400" (){
//        given:
//        createMocks()
//        allowAccess()
//        Capabilities capabilities = new Capabilities();
//        capabilities.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null))
//        capabilityDao.getNextCapabilityId() >> "123321"
//        capabilityDao.getObjects(_) >> capabilities
//        when:
//        def responseBuilder = cloud20Service.updateCapabilities(authToken,capabilities,"computeTest","1")
//
//        then:
//        Response response = responseBuilder.build()
//        response.status == 400
//    }

    def createMocks() {
        scopeAccessDao = Mock()

        scopeAccessService = Mock()
        cloud20Service.scopeAccessService = scopeAccessService

        authorizationService = Mock()
        cloud20Service.authorizationService = authorizationService

        userDao = Mock()
        userService.userDao = userDao
        userService.scopeAccesss = scopeAccessDao

        cloudRegionService = Mock()
        userService.cloudRegionService = cloudRegionService

        questionDao = Mock()
        questionService.questionDao = questionDao

        capabilityDao = Mock()
        capabilityService.ldapCapabilityRepository = capabilityDao
    }

    def allowAccess() {
        ScopeAccess accessToken = scopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(_) >> accessToken
    }

    def user(String username, String email, Boolean enabled, String displayName) {
        new User().with {
            it.username = username
            it.email = email
            it.enabled = enabled
            it.displayName = displayName
            return it
        }
    }

    def region() {
        new Region().with {
            it.cloud = "US"
            it.name = "DFW"
            it.isEnabled = true
            it.isDefault = true
            return it
        }
    }

    def scopeAccess() {
        new ClientScopeAccess().with {
            Calendar calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            it.accessTokenExp = calendar.getTime()
            it.accessTokenString = calendar.getTime().toString()
            return it
        }
    }

    def jaxbQuestion() {
        return jaxbQuestion("id", "question")
    }

    def jaxbQuestion(String id, String question) {
        objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestion().with {
            it.id = id
            it.question = question
            return it
        }
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

    def getCapability(String action, String id, String name, String description, String url, List<String> resources) {
        new Capability().with {
            it.action = action
            it.id = id
            it.name = name
            it.description = description
            it.url = url
            for(String list : resources){
                it.resources.add(list)
            }
            return it
        }
    }

    def uriInfo() {
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> new URI()
        return uriInfo
    }
}
