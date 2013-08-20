package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForAuthenticationRequest
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForExtension
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForExtensions
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthCapabilities
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthPolicy
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthQuestion
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthSecretQA
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthRegion
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxKsKeyApiKeyCredentials
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthRegions
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRole
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForUser
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForUserForCreate

import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderRaxAuthForDomain
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderRaxAuthForPolicies
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForAuthenticationRequest
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForExtension
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForExtensions
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxAuthCapabilities
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxAuthQuestion
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxAuthQuestions
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxAuthRegion
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxAuthRegions
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxKsKeyApiKeyCredentials
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRole
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxAuthSecretQAs
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForRaxAuthServiceApis
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForUser
import com.rackspace.idm.api.resource.cloud.JSONWriters.JSONWriterForUserForCreate

import org.apache.commons.io.IOUtils
import org.openstack.docs.common.api.v1.Extension
import org.openstack.docs.common.api.v1.Extensions
import org.openstack.docs.common.api.v1.MediaType
import org.openstack.docs.common.api.v1.MediaTypeList
import org.openstack.docs.common.api.v1.VersionChoice
import org.openstack.docs.common.api.v1.VersionStatus
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.User
import org.w3._2005.atom.Link
import spock.lang.Shared
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capability
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PolicyAlgorithm
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ServiceApis
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ServiceApi
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs
import testHelpers.RootServiceTest

class JSONReaderWriterTest extends RootServiceTest {

    @Shared JSONWriter writer = new JSONWriter();

    @Shared JSONReaderForRaxAuthRegion readerForRaxAuthRegion = new JSONReaderForRaxAuthRegion()
    @Shared JSONWriterForRaxAuthRegion writerForRaxAuthRegion = new JSONWriterForRaxAuthRegion()

    @Shared JSONReaderForRaxAuthRegions readerForRegions = new JSONReaderForRaxAuthRegions()
    @Shared JSONWriterForRaxAuthRegions writerForRegions = new JSONWriterForRaxAuthRegions()

    @Shared JSONWriterForRaxAuthQuestion writerForQuestion = new JSONWriterForRaxAuthQuestion()
    @Shared JSONWriterForRaxAuthQuestions writerForQuestions = new JSONWriterForRaxAuthQuestions()
    @Shared JSONReaderForRaxAuthQuestion readerForQuestion = new JSONReaderForRaxAuthQuestion()

    @Shared JSONWriterForRaxAuthCapabilities writerForCapabilities = new JSONWriterForRaxAuthCapabilities()
    @Shared JSONReaderForRaxAuthCapabilities readerForCapabilities = new JSONReaderForRaxAuthCapabilities()

    @Shared JSONWriterForRaxAuthServiceApis writerForServiceApis = new JSONWriterForRaxAuthServiceApis()

    @Shared JSONWriterForRaxAuthSecretQAs writerForSecretQAs = new JSONWriterForRaxAuthSecretQAs()

    @Shared JSONWriterForUser writerForUser = new JSONWriterForUser()
    @Shared JSONReaderForUser readerForUser = new JSONReaderForUser()

    @Shared JSONReaderRaxAuthForPolicies readerForPolicies = new JSONReaderRaxAuthForPolicies()
    @Shared JSONReaderForRaxAuthPolicy readerForPolicy = new JSONReaderForRaxAuthPolicy()

    @Shared JSONReaderRaxAuthForDomain readerForDomain = new JSONReaderRaxAuthForDomain()

    @Shared JSONReaderForRaxAuthSecretQA readerForRaxAuthSecretQA = new JSONReaderForRaxAuthSecretQA()

    @Shared JSONReaderForRole readerForRole = new JSONReaderForRole()
    @Shared JSONWriterForRole writerForRole = new JSONWriterForRole()

    @Shared JSONReaderForRaxKsKeyApiKeyCredentials readerForApiKeyCredentials = new JSONReaderForRaxKsKeyApiKeyCredentials()
    @Shared JSONWriterForRaxKsKeyApiKeyCredentials writerForApiKeyCredentials = new JSONWriterForRaxKsKeyApiKeyCredentials()

    @Shared JSONReaderForUserForCreate readerForUserForCreate = new JSONReaderForUserForCreate()
    @Shared JSONWriterForUserForCreate writerForUserForCreate = new JSONWriterForUserForCreate()

    @Shared JSONReaderForAuthenticationRequest readerForAuthenticationRequest = new JSONReaderForAuthenticationRequest()
    @Shared JSONWriterForAuthenticationRequest writerForAuthenticationRequest = new JSONWriterForAuthenticationRequest()

    @Shared JSONReaderForExtensions readerForExtensions = new JSONReaderForExtensions()
    @Shared JSONWriterForExtensions writerForExtensions = new JSONWriterForExtensions()

    @Shared JSONReaderForExtension readerForExtension = new JSONReaderForExtension()
    @Shared JSONWriterForExtension writerForExtension = new JSONWriterForExtension()


    def "can read/write region as json"() {
        given:
        def regionEntity = region("name", true, false)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForRaxAuthRegion.writeTo(regionEntity, Region.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def readRegion = readerForRaxAuthRegion.readFrom(Region.class, null, null, null, null, arrayInputStream)

        then:
        regionEntity.name == readRegion.name
        regionEntity.enabled == readRegion.enabled
        regionEntity.isDefault == readRegion.isDefault
    }

    def "region reader throws bad request if root is not found"() {
        when:
        def json = '{ "region": { "enabled": true, "isDefault": true, "name": "DFW" } }'

        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        readerForRaxAuthRegion.readFrom(Region.class, null, null, null, null, arrayInputStream)

        then:
        thrown(BadRequestException)
    }

    def "can read/write regions as json"() {
        given:
        def regionEntity = region("name", true, false)
        def regionsEntity = new Regions()
        regionsEntity.region.add(regionEntity)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForRegions.writeTo(regionsEntity, Regions.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def readRegions = readerForRegions.readFrom(Regions.class, null, null, null, null, arrayInputStream)
        def readRegion = readRegions.region.get(0)

        then:
        regionsEntity.region.size() == readRegions.region.size()
        regionEntity.name == readRegion.name
        regionEntity.enabled == readRegion.enabled
        regionEntity.isDefault == readRegion.isDefault
    }

    def "regions reader throws bad request if root is not found"() {
        given:
        def regionEntity = region("name", true, false)

        when:
        def json = '{ "regions": {} }'

        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        readerForRegions.readFrom(Regions.class, null, null, null, null, arrayInputStream)

        then:
        thrown(BadRequestException)
    }

    def "can read/write question as json"() {
        given:
        def question = question("id", "question")

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForQuestion.writeTo(question, Question, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json);

        Question readQuestion = readerForQuestion.readFrom(Question, null, null, null, null, inputStream);

        then:
        json != null
        question.id == readQuestion.id
        question.question == readQuestion.question
    }

    def "can write questions as json"() {
        given:
        def questions = new Questions()
        def question = question("id", "question")
        questions.question.add(question)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForQuestions.writeTo(questions, Questions, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        json != null
    }

    def "should be able to write empty list"() {
        given:
        def questions = new Questions()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForQuestions.writeTo(questions, Questions, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        json != null
    }

    def "can read/write capabilities json" () {
        given:
        List<Capability> capabilityList = new ArrayList<Capability>();
        capabilityList.add(getCapability("get_server","get_server","GET","http://someUrl",null,null))
        def capabilitiesEntity = getCapabilities(capabilityList)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForCapabilities.writeTo(capabilitiesEntity,Capabilities,null,null,null,null,arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json)

        Capabilities readCapabilities = readerForCapabilities.readFrom(Capabilities, null, null, null, null, inputStream)

        then:
        json != null
        readCapabilities.capability.get(0).action == "GET"
    }

    def "should be able to write empty list of capabilities"() {
        given:
        def capabilities = new Capabilities()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForCapabilities.writeTo(capabilities, Capabilities, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        json != null
    }

    def "can read/write policies json" () {
        given:
        List<Policy> policiesList  = new ArrayList<Policy>();
        policiesList.add(getPolicy("10000321","testPolicy","json-policy-format",true, false,"desc","someblob"))
        def policiesEntity = getPolicies(policiesList)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(policiesEntity,Policies,null,null,null,null,arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json)

        Policies readPolicies = readerForPolicies.readFrom(Policies, null, null, null, null, inputStream)

        then:
        json != null
        readPolicies.policy.get(0).id == "10000321"
    }

    def "can read/write policies json IF_TRUE_ALLOW" () {
        given:
        List<Policy> policiesList  = new ArrayList<Policy>();
        policiesList.add(getPolicy("10000321","testPolicy","json-policy-format",true, false,"desc","someblob"))
        def policiesEntity = getPolicies(policiesList)
        policiesEntity.algorithm = PolicyAlgorithm.IF_TRUE_ALLOW

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(policiesEntity,Policies,null,null,null,null,arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json)

        Policies readPolicies = readerForPolicies.readFrom(Policies, null, null, null, null, inputStream)

        then:
        json != null
        readPolicies.algorithm == PolicyAlgorithm.IF_TRUE_ALLOW
    }

    def "can read/write policies json no id returns BadRequest" () {
        given:
        List<Policy> policiesList  = new ArrayList<Policy>();
        policiesList.add(getPolicy(null,"testPolicy","json-policy-format",true, false,"desc","someblob"))
        def policiesEntity = getPolicies(policiesList)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(policiesEntity,Policies,null,null,null,null,arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json)

        Policies readPolicies = readerForPolicies.readFrom(Policies, null, null, null, null, inputStream)

        then:
        thrown(BadRequestException)
    }

    def "can read/write policy as json"() {
        given:
        def policy = getPolicy("10000321","testPolicy","json-policy-format",true, false,"desc","someblob")

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(policy, Policy, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json);

        Policy readPolicy = readerForPolicy.readFrom(Policy, null, null, null, null, inputStream);

        then:
        json != null
        readPolicy.name == "testPolicy"
    }

    def "can read/write domain as json"() {
        given:
        def domain = getDomain("id","name",true,"desc")

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(domain, Domain, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json);

        Domain readDomain = readerForDomain.readFrom(Domain, null, null, null, null, inputStream);

        then:
        json != null
        readDomain.name == "name"
    }

    def "should be able to write empty list of policies"() {
        given:
        def capabilities = new Capabilities()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForCapabilities.writeTo(capabilities, Capabilities, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        json != null
    }

    def "can read/write serviceApis json" () {
        given:
        List<ServiceApi> serviceApis  = new ArrayList<ServiceApi>();
        serviceApis.add(getServiceApi("computeTest","1","desc"))
        def serviceApisEntity = getServiceApis(serviceApis)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForServiceApis.writeTo(serviceApisEntity,ServiceApis,null,null,null,null,arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json)

        then:
        json != null

    }

    def "can read/write secretqa as json"() {
        given:
        SecretQAs qAs = new SecretQAs()
        def secretqa = getSecretQA("1","question","answer")
        qAs.secretqa.add(secretqa)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForSecretQAs.writeTo(qAs, SecretQAs, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        InputStream inputStream = IOUtils.toInputStream('{"RAX-AUTH:secretqa":{"id": "1","answer": "Himalayas"}}"')
        SecretQA readSecretQA = readerForRaxAuthSecretQA.readFrom(SecretQA, null, null, null, null, inputStream)

        then:
        json != null
        readSecretQA.answer == "Himalayas"
    }

    def "can read/write roles as json"() {
        when:
        def role = v2Factory.createRole(propagate, weight)

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForRole.writeTo(role, Role, null, null, null, null, arrayOutputStream)
        String JSONString = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(JSONString)

        def readJSONObject = readerForRole.readFrom(Role, null, null, null, null, inputStream)
        then:
        if(readJSONObject.propagate != null){
            readJSONObject.propagate as boolean == propagate
        }
        readJSONObject.weight == weight

        where:
        propagate   | weight
        true        | null
        null        | 500
        false       | 100
        null        | null
    }

    def "can read/write user as json" () {
        given:
        def id = "abc123"
        def username = "BANANAS"
        def email = "no@rackspace.com"
        def enabled = true
        def displayName = "displayName"

        User userObject = v2Factory.createUser().with {
            it.displayName = displayName
            it.id = id
            it.username = username
            it.email = email
            it.enabled = enabled
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForUser.writeTo(userObject, User.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def user = readerForUser.readFrom(User.class, null, null, null, null, arrayInputStream)


        then:
        user.id == id
        user.username == username
        user.email == email
        user.enabled == enabled
        user.displayName == displayName
    }

    def "can read/write apiKeyCreds as json" () {
        given:
        ApiKeyCredentials apiKeyCred = v1Factory.createApiKeyCredentials()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForApiKeyCredentials.writeTo(apiKeyCred, ApiKeyCredentials.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def apiKey = readerForApiKeyCredentials.readFrom(ApiKeyCredentials.class, null, null, null, null, arrayInputStream)


        then:
        apiKey.apiKey == apiKeyCred.apiKey
        apiKey.username == apiKeyCred.username
    }

    def "create read/write userForCreate as json" () {
        given:
        def username = "username"
        def displayName = "displayName"
        def email = "someEmail@rackspace.com"
        def enabled = true
        def defaultRegion = "ORD"
        def domainId = "domainId"
        def password = "Password1"
        def user = v2Factory.userForCreate(username, displayName, email, enabled, defaultRegion, domainId, password)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForUserForCreate.writeTo(user, UserForCreate.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def userObject = readerForUserForCreate.readFrom(UserForCreate.class, null, null, null, null, arrayInputStream)

        then:
        userObject.username == username
        userObject.domainId == domainId
        userObject.email == email
        userObject.enabled == enabled
    }

    def "create read/write for authenticationRequest" (){
        given:
        def username = "username"
        def apiKey = "1234567890"
        def domain = "name"
        def authRequest = v2Factory.createApiKeyAuthenticationRequest(username, apiKey)
        authRequest.domain = new Domain().with{
            it.name = domain
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForAuthenticationRequest.writeTo(authRequest, AuthenticationRequest.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def authObject = readerForAuthenticationRequest.readFrom(AuthenticationRequest.class, null, null, null, null, arrayInputStream)

        then:
        authObject.credential.getValue().username == username
        authObject.credential.getValue().apiKey == apiKey
        authObject.domain.name ==  domain
    }

    def "create read/write for extensions" (){
        given:
        def extensions = new Extensions().with {
            it.extension = new ArrayList<Extension>()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForExtensions.writeTo(extensions, Extensions.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def extensionsObject = readerForExtensions.readFrom(Extensions.class, null, null, null, null, arrayInputStream)

        then:
        extensionsObject != null
    }

    def "create read/write for extension" (){
        given:
        List<Link> links = new ArrayList()
        def link = new Link().with {
            it.href = "href"
            it.type = "type"
            it
        }
        def link2 = new Link().with {
            it.href = "href"
            it.type = "type"
            it
        }
        links.add(link)
        links.add(link2)

        def extension = new Extension().with {
            it.alias = "RAX-AUTH"
            it.description = "desc"
            it.name = "name"
            it.namespace = "namespace"
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForExtension.writeTo(extension, Extensions.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        def extensionsObject = readerForExtension.readFrom(Extension.class, null, null, null, null, arrayInputStream)

        then:
        extensionsObject != null
    }

    def getSecretQA(String id, String question, String answer) {
        new SecretQA().with {
            it.id = id
            it.question = question
            it.answer = answer
            return it
        }
    }

    def getServiceApi(String type, String version, String description) {
        new ServiceApi().with {
            it.type = type
            it.version = version
            it.description = description
            return it
        }
    }

    def getServiceApis(ArrayList<ServiceApi> serviceApis) {
        new ServiceApis().with {
            for(ServiceApi api : serviceApis){
                it.serviceApi.add(api)
            }
            return it
        }
    }

    def getDomain(String id, String name, Boolean enabled, String description) {
        new Domain().with {
            it.id = id
            it.name = name
            it.enabled = enabled
            it.description = description
            return it
        }
    }


    def getPolicies(ArrayList<Policy> policies) {
        new Policies().with {
            for(Policy policy : policies){
                it.policy.add(policy)
            }
            it.algorithm = PolicyAlgorithm.IF_FALSE_DENY
            return it
        }
    }

    def getPolicy(String id, String name, String type, Boolean enabled, Boolean global, String description, String blob) {
        new Policy().with {
            it.id = id
            it.name = name
            it.type = type
            it.enabled = enabled
            it.global = global
            it.description = description
            it.blob = blob
            return it
        }
    }

    def getCapabilities(List<Capability> capabilities) {
        new Capabilities().with {
            for(Capability capability : capabilities){
                it.capability.add(capability)
            }
            return it
        }
    }

    def getCapability(String id, String name, String action, String url, String description, List<String> resources) {
        new Capability().with {
            it.id = id
            it.name = name
            it.action = action
            it.url = url
            it.description = description
            for(String resource : resources){
                it.resources.add(resource)
            }
            return it
        }
    }

    def question(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def region(String name, Boolean isEnabled, Boolean isDefault) {
        new Region().with {
            it.name = name
            it.enabled = isEnabled
            it.isDefault = isDefault
            return it
        }
    }
}
