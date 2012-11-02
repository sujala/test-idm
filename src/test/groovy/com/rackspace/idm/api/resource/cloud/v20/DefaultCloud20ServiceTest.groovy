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

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20
import com.rackspace.idm.api.resource.pagination.PaginatorContext

import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.ReadOnlyEntry


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
    @Autowired UserConverterCloudV20 userConverterCloudV20

    @Shared DefaultScopeAccessService scopeAccessService
    @Shared DefaultAuthorizationService authorizationService
    @Shared DefaultCloudRegionService cloudRegionService
    @Shared LdapUserRepository userDao
    @Shared LdapScopeAccessPeristenceRepository scopeAccessDao
    @Shared LdapQuestionRepository questionDao
    @Shared LdapCapabilityRepository capabilityDao


    @Shared def authToken = "token"
    @Shared def offset = 0
    @Shared def limit = 25
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

    def "updateCapabilities return 204" (){
        given:
        createMocks()
        allowAccess()
        Capabilities capabilities = new Capabilities();
        capabilities.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null))
        List<com.rackspace.idm.domain.entity.Capability> capabilitiesDO = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getNextCapabilityId() >> "123321"
        capabilityDao.getObjects(_) >> capabilitiesDO

        when:
        def responseBuilder = cloud20Service.updateCapabilities(authToken,capabilities,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "updateCapabilities return 400" (){
        given:
        createMocks()
        allowAccess()
        Capabilities capabilities = new Capabilities();
        capabilities.capability.add(getCapability(null, "get_server", "get_server", "description", "http://someUrl", null))
        capabilityDao.getNextCapabilityId() >> "123321"
        List<com.rackspace.idm.domain.entity.Capability> capabilities2 = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getObjects(_) >> capabilities2
        when:
        def responseBuilder = cloud20Service.updateCapabilities(authToken,capabilities,"computeTest","1")
        def responseBuilder2 = cloud20Service.updateCapabilities(authToken,capabilities,null,"1")
        def responseBuilder3 = cloud20Service.updateCapabilities(authToken,null,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 400
        Response response2 = responseBuilder2.build()
        response2.status == 400
        Response response3 = responseBuilder3.build()
        response3.status == 400
    }

    def "updateCapabilities return 401" (){
        given:
        createMocks()
        Capabilities capabilities = new Capabilities();

        when:
        def responseBuilder = cloud20Service.updateCapabilities("badToken",capabilities,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 401
    }

    def "getCapabilities returns 200" () {
        given:
        createMocks()
        allowAccess()
        List<com.rackspace.idm.domain.entity.Capability> capabilities = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getObjects(_) >> capabilities
        when:
        def responseBuilder = cloud20Service.getCapabilities(authToken,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 200
    }

    def "getCapabilities returns 401" () {
        given:
        createMocks()

        when:
        def responseBuilder = cloud20Service.getCapabilities("badToken","computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 401
    }

    def "getCapabilities returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.getCapabilities(authToken, null , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "getCapabilities null version returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.getCapabilities(authToken, "computeTest" , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "deleteCapabilities returns 204" () {
        given:
        createMocks()
        allowAccess()
        List<com.rackspace.idm.domain.entity.Capability> capabilities = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getObjects(_) >> capabilities

        when:
        def responseBuilder = cloud20Service.removeCapabilities(authToken , "computeTest", "1")

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "deleteCapabilities returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.removeCapabilities(authToken, null , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "deleteCapabilities with null version returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.removeCapabilities(authToken, "computeTest" , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "deleteCapabilities returns 401" () {
        given:
        createMocks()

        when:
        def responseBuilder = cloud20Service.removeCapabilities("badToken", "computeTest" , null)

        then:
        Response response = responseBuilder.build()
        response.status == 401
    }

    def "listUsers verifies token"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(_)

    }

    def "listUsers returns caller"() {
        given:
        createMocks()
        allowAccess()

        userDao.getUserByUsername(_) >> user(sharedRandom)
        authorizationService.authorizeCloudUser(_) >> true

        when:
        def response = cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit).build()

        then:
        response.status == 200
        response.entity.user[0].username.equals(sharedRandom)
    }

    def "listUsers verifies userAdmin Access level"() {
        given:
        createMocks()
        allowAccess()
        authorizationService.authorizeCloudUserAdmin(_) >> false

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "listUsers calls getAllUsersPaged with no filters"() {
        given:
        createMocks()
        allowAccess()

        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * userDao.getAllUsersPaged(null, offset, limit)
    }

    def "listUsers calls getAllUsersPaged with domainId filter"() {
        given:
        createMocks()
        allowAccess()
        def user = user(sharedRandom)
        user.domainId = "123456789"

        def userList = [ ] as User[]
        userList = userList.toList()
        FilterParam[] filters = [new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, "123456789")] as FilterParam[]
        userDao.getUserByUsername(_) >> user
        userDao.getAllUsersPaged(_, _, _) >> userContext(offset, limit, userList)
        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * userDao.getAllUsersPaged(_, _, _)
    }

    def "listUsers throws bad request"() {
        given:
        createMocks()
        allowAccess()

        userDao.getUserByUsername(_) >> user(sharedRandom)
        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        when:
        def response = cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit).build()

        then:
        response.status == 400
    }

    def "listUsers returns userList"() {
        given:
        createMocks()
        allowAccess()
        def userList = [ user("user1"), user("user2"), user("user3") ] as User[]
        userList = userList.toList()
        def userContext = userContext(offset, limit, userList)
        def links = new HashMap<String, String>()
        links.put("first", "first")
        links.put("last", "last")
        links.put("prev", "prev")
        links.put("next", "next")
        userContext.pageLinks = links

        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> true
        userDao.getUserByUsername(_) >> user(sharedRandom)
        userDao.getAllUsersPaged(_, _, _) >> userContext

        when:
        def response = cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit).build()

        then:
        response.status == 200
        response.entity.user[0].username == "user1"
        response.entity.user[1].username == "user2"
        response.entity.user[2].username == "user3"

    }

    //helper methods
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
        def attribute = new Attribute(LdapRepository.ATTR_UID, "uid")
        def entry = new ReadOnlyEntry("DN", attribute)

        ClientScopeAccess clientScopeAccess = Mock()
        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        clientScopeAccess.accessTokenExp = calendar.getTime()
        clientScopeAccess.accessTokenString = calendar.getTime().toString()

        clientScopeAccess.getLDAPEntry() >> entry
        scopeAccessService.getScopeAccessByAccessToken(_) >> clientScopeAccess
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

    def user(name) {
        new User().with {
            it.username = name
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
        uriInfo.getAbsolutePath() >> new URI("http://path.to/resource")
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> new URI()
        return uriInfo
    }

    def userContext(offset, limit, list) {
        new PaginatorContext<User>().with {
            it.limit = limit
            it.offset = offset
            it.totalRecords = list.size
            it.valueList = list
            return it
        }
    }
}
