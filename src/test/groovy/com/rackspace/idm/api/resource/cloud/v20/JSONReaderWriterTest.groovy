package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForAuthData
import com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForBaseURLList
import com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForBaseURLRefList
import com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForExtension
import com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForExtensions
import com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForServiceCatalog
import com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForVersionChoice
import com.rackspace.idm.api.resource.cloud.v20.json.readers.*
import com.rackspace.idm.api.resource.cloud.v20.json.writers.*
import com.rackspace.idm.exception.BadRequestException
import com.rackspacecloud.docs.auth.api.v1.AuthData
import com.rackspacecloud.docs.auth.api.v1.BaseURLList
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList
import com.rackspacecloud.docs.auth.api.v1.GroupsList
import org.apache.commons.io.IOUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.openstack.docs.common.api.v1.Extension
import org.openstack.docs.common.api.v1.Extensions
import org.openstack.docs.common.api.v1.VersionChoice
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.*
import org.w3._2005.atom.Link
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.xml.datatype.DatatypeFactory

import static com.rackspace.idm.GlobalConstants.*
import static com.rackspace.idm.JSONConstants.*

class JSONReaderWriterTest extends RootServiceTest {

    @Shared com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory()

    @Shared JSONReaderForRaxAuthRegion readerForRaxAuthRegion = new JSONReaderForRaxAuthRegion()
    @Shared JSONWriterForRaxAuthRegion writerForRaxAuthRegion = new JSONWriterForRaxAuthRegion()

    @Shared JSONReaderForRaxAuthRegions readerForRegions = new JSONReaderForRaxAuthRegions()
    @Shared JSONWriterForRaxAuthRegions writerForRegions = new JSONWriterForRaxAuthRegions()

    @Shared JSONWriterForRaxAuthQuestion writerForQuestion = new JSONWriterForRaxAuthQuestion()
    @Shared JSONWriterForRaxAuthQuestions writerForQuestions = new JSONWriterForRaxAuthQuestions()
    @Shared JSONReaderForRaxAuthQuestion readerForQuestion = new JSONReaderForRaxAuthQuestion()

    @Shared JSONWriterForRaxAuthSecretQAs writerForSecretQAs = new JSONWriterForRaxAuthSecretQAs()

    @Shared JSONWriterForUser writerForUser = new JSONWriterForUser()
    @Shared JSONReaderForUser readerForUser = new JSONReaderForUser()

    @Shared JSONReaderRaxAuthForDomain readerForDomain = new JSONReaderRaxAuthForDomain()

    @Shared JSONReaderForRaxAuthMobilePhone readerForRaxAuthMobilePhone = new JSONReaderForRaxAuthMobilePhone()
    @Shared JSONWriterForRaxAuthMobilePhone writerForRaxAuthMobilePhone = new JSONWriterForRaxAuthMobilePhone()


    @Shared JSONReaderForRaxAuthSecretQA readerForRaxAuthSecretQA = new JSONReaderForRaxAuthSecretQA()
    @Shared JSONReaderForRaxKsQaSecretQA readerForRaxKsQaSecretQA = new JSONReaderForRaxKsQaSecretQA()
    @Shared JSONWriterForRaxKsQaSecretQA writerForRaxKsQaSecretQA = new JSONWriterForRaxKsQaSecretQA()

    @Shared JSONReaderForRole readerForRole = new JSONReaderForRole()
    @Shared JSONWriterForRole writerForRole = new JSONWriterForRole()

    @Shared JSONReaderForRaxKsKeyApiKeyCredentials readerForApiKeyCredentials = new JSONReaderForRaxKsKeyApiKeyCredentials()
    @Shared JSONWriterForRaxKsKeyApiKeyCredentials writerForApiKeyCredentials = new JSONWriterForRaxKsKeyApiKeyCredentials()

    @Shared JSONReaderForUserForCreate readerForUserForCreate = new JSONReaderForUserForCreate()
    @Shared JSONWriterForUserForCreate writerForUserForCreate = new JSONWriterForUserForCreate()

    @Shared JSONReaderForAuthenticationRequest readerForAuthenticationRequest = new JSONReaderForAuthenticationRequest()
    @Shared JSONWriterForAuthenticationRequest writerForAuthenticationRequest = new JSONWriterForAuthenticationRequest()

    @Shared JSONReaderForTenant readerForTenant = new JSONReaderForTenant()
    @Shared JSONWriterForTenant writerForTenant = new JSONWriterForTenant()

    @Shared JSONReaderForTenants readerForTenants = new JSONReaderForTenants()
    @Shared JSONWriterForTenants writerForTenants = new JSONWriterForTenants()

    @Shared JSONReaderForOsKsAdmService readerForService = new JSONReaderForOsKsAdmService()
    @Shared JSONWriterForOsKsAdmService writerForService = new JSONWriterForOsKsAdmService()

    @Shared JSONReaderForOsKsAdmServices readerForServices = new JSONReaderForOsKsAdmServices()
    @Shared JSONWriterForOsKsAdmServices writerForServices = new JSONWriterForOsKsAdmServices()

    @Shared JSONReaderForOsKsCatalogEndpointTemplate readerForEndpointTemplate = new JSONReaderForOsKsCatalogEndpointTemplate()
    @Shared JSONWriterForOsKsCatalogEndpointTemplate writerForEndpointTemplate = new JSONWriterForOsKsCatalogEndpointTemplate()

    @Shared JSONReaderForEndpoint readerForEndpoint = new JSONReaderForEndpoint()
    @Shared JSONWriterForEndpoint writerForEndpoint = new JSONWriterForEndpoint()

    @Shared JSONReaderForEndpoints readerForEndpoints = new JSONReaderForEndpoints()
    @Shared JSONWriterForEndpoints writerForEndpoints = new JSONWriterForEndpoints()

    @Shared JSONWriterForOsKsCatalogEndpointTemplates writerForEndpointTemplates = new JSONWriterForOsKsCatalogEndpointTemplates()

    @Shared JSONReaderForPasswordCredentials readerForPasswordCredentials = new JSONReaderForPasswordCredentials()
    @Shared JSONWriterForPasswordCredentials writerForPasswordCredentials = new JSONWriterForPasswordCredentials()

    @Shared JSONWriterForRaxKsGroup writerForGroup = new JSONWriterForRaxKsGroup()

    @Shared JSONReaderForRaxKsGroups readerForGroups = new JSONReaderForRaxKsGroups()
    @Shared JSONWriterForRaxKsGroups writerForGroups = new JSONWriterForRaxKsGroups()

    @Shared JSONWriterForGroups writerForGroupsList = new JSONWriterForGroups()

    @Shared JSONReaderForRaxAuthDefaultRegionServices readerForDefaultRegionServices = new JSONReaderForRaxAuthDefaultRegionServices()
    @Shared JSONWriterForRaxAuthDefaultRegionServices writerForDefaultRegionServices = new JSONWriterForRaxAuthDefaultRegionServices()

    @Shared JSONWriterForExtension writerForExtension = new JSONWriterForExtension()
    @Shared JSONWriterForExtensions writerForExtensions = new JSONWriterForExtensions()
    @Shared JSONWriterForVersionChoice writerForVersionChoice = new JSONWriterForVersionChoice()
    @Shared JSONWriterForCredentialListType writerForCredentialListType = new JSONWriterForCredentialListType()
    @Shared JSONWriterForRoles writerForRoles = new JSONWriterForRoles()
    @Shared JSONWriterForUsers writerForUsers = new JSONWriterForUsers().with {
        it.multiFactorCloud20Service = Mock(MultiFactorCloud20Service)
        return it
    }
    @Shared JSONWriterForAuthenticateResponse writerForAuthenticateResponse = new JSONWriterForAuthenticateResponse()
    @Shared JSONWriterForImpersonationResponse writerForImpersonationResponse = new JSONWriterForImpersonationResponse()
    @Shared JSONWriterForBaseURLList writerForBaseURLList = new JSONWriterForBaseURLList()
    @Shared com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForUser writerForUser11 = new com.rackspace.idm.api.resource.cloud.v11.json.writers.JSONWriterForUser()
    @Shared JSONWriterForBaseURLRefList writerForBaseURLRefList = new JSONWriterForBaseURLRefList()
    @Shared JSONWriterForRaxAuthDomain writerForDomain = new JSONWriterForRaxAuthDomain()
    @Shared JSONWriterForRaxAuthDomains writerForDomains = new JSONWriterForRaxAuthDomains()
    @Shared JSONWriterForAuthData writerForAuthData = new JSONWriterForAuthData()
    @Shared JSONWriterForServiceCatalog writerForServiceCatalog = new JSONWriterForServiceCatalog()

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

    def "can read/write domain as json"() {
        given:
        def domain = getDomain("id","name",true,"desc")

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForDomain.writeTo(domain, Domain, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json);

        Domain readDomain = readerForDomain.readFrom(Domain, null, null, null, null, inputStream);

        then:
        json != null
        readDomain.name == "name"
    }

    def "can read/write mobile phone as json"() {
        given:
        String phoneNumber = "aphone"
        def mobilePhone = getMobilePhone(phoneNumber)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForRaxAuthMobilePhone.writeTo(mobilePhone, MobilePhone, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json);

        MobilePhone readMobilePhone = readerForRaxAuthMobilePhone.readFrom(MobilePhone, null, null, null, null, inputStream);

        then:
        json != null
        readMobilePhone.number == phoneNumber
    }

    def "can read/write rax-ksqa secretQa as json"() {
        given:
        def secretQA = v1Factory.createRaxKsQaSecretQA()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForRaxKsQaSecretQA.writeTo(secretQA, com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        InputStream inputStream = IOUtils.toInputStream('{"RAX-KSQA:secretQA":{"username": "username","question":"question","answer": "Himalayas"}}"')
        com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA readSecretQA = readerForRaxKsQaSecretQA.readFrom(SecretQA, null, null, null, null, inputStream)

        then:
        JSONObject o = outer.get(RAX_KSQA_SECRET_QA)
        o.get(USERNAME) == "username"
        o.get(ANSWER) == "answer"
        o.get(QUESTION) == "question"
        readSecretQA.answer == "Himalayas"
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
        def role = v2Factory.createRole(propagate, assignment)

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForRole.writeTo(role, Role, null, null, null, null, arrayOutputStream)
        String JSONString = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(JSONString)

        def readJSONObject = readerForRole.readFrom(Role, null, null, null, null, inputStream)
        then:
        if(readJSONObject.propagate != null){
            readJSONObject.propagate as boolean == propagate
        }

        readJSONObject.assignment == assignment

        where:
        propagate | assignment
        true      | RoleAssignmentEnum.GLOBAL
        false     | RoleAssignmentEnum.TENANT
        null      | RoleAssignmentEnum.BOTH
    }

    def "can read/write user as json" () {
        given:
        def id = "abc123"
        def username = "BANANAS"
        def email = "no@rackspace.com"
        def enabled = true
        def displayName = "displayName"
        def multiFactorEnabled = true

        User userObject = v2Factory.createUser().with {
            it.displayName = displayName
            it.id = id
            it.username = username
            it.email = email
            it.enabled = enabled
            it.multiFactorEnabled = multiFactorEnabled
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
        user.multiFactorEnabled == multiFactorEnabled
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

    def "create read/write for authenticationRequest - apiKey credentials" (){
        given:
        def username = "username"
        def apiKey = "1234567890"
        def domain = "name"
        def tenantId = "tenantId"
        def authRequest = v2Factory.createApiKeyAuthenticationRequest(username, apiKey)
        authRequest.tenantId = tenantId
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
        authObject.tenantId == tenantId
    }

    def "create read/write for authenticationRequest - password credentials" (){
        given:
        def username = "username"
        def password = "password"
        def domain = "name"
        def tenantId = "tenantId"
        def authRequest = v2Factory.createPasswordAuthenticationRequest(username, password)
        authRequest.tenantId = tenantId
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
        authObject.credential.getValue().password == password
        authObject.domain.name ==  domain
    }

    def "create read/write for authenticationRequest - rsa credentials" (){
        given:
        def username = "username"
        def tokenKey = "tokenKey"
        def domain = "name"
        def authRequest = v2Factory.createRsaAuthenticationRequest(username, tokenKey)
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
        authObject.credential.getValue().tokenKey == tokenKey
        authObject.domain.name ==  domain
        authObject.tenantId == null
    }

    def "create read for authenticationRequest - multiple credentials" (){
        given:
        String json = '{"auth":{"tenantId":"tenantId","RAX-AUTH:domain":{"name":"name"},"passwordCredentials":{"username":"username","password":"password"}, "RAX-KSKEY:apiKeyCredentials":{"username":"username","apiKey":"1234567890"}}}'
        when:
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        readerForAuthenticationRequest.readFrom(AuthenticationRequest.class, null, null, null, null, arrayInputStream)

        then:
        thrown(BadRequestException)
    }

    def "create read/write for authenticationRequest - token" (){
        given:
        def domain = "name"
        def token = "token"
        def authRequest = new AuthenticationRequest().with {
            it.token = v2Factory.createTokenForAuthenticationRequest(token)
            it
        }
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
        authObject.credential == null
        authObject.token != null
        authObject.token.id == token
        authObject.domain.name ==  domain
        authObject.tenantId == null
    }

    def "create read/writer for tenant" () {
        given:
        def tenant = v2Factory.createTenant("id", "name", ["type1"])

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForTenant.writeTo(tenant, Tenant.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Tenant tenantObject = readerForTenant.readFrom(Tenant.class, null, null, null, null, arrayInputStream)

        then:
        tenantObject != null
        tenantObject.name == tenant.name
        tenantObject.types.type.get(0) == tenant.types.type.get(0)
    }

    def "create read/writer for tenants" () {
        given:
        def tenant = v2Factory.createTenant()
        def tenants = new Tenants().with {
            it.tenant = [tenant, tenant].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForTenants.writeTo(tenants, Tenants.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Tenants tenantsObject = readerForTenants.readFrom(Tenants.class, null, null, null, null, arrayInputStream)

        then:
        tenantsObject != null
        tenantsObject.tenant[0].name == tenant.name
    }

    def "create read/writer for service" () {
        given:
        def service = v1Factory.createService()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForService.writeTo(service, Service.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Service serviceObject = readerForService.readFrom(Service.class, null, null, null, null, arrayInputStream)

        then:
        serviceObject != null
        serviceObject.id == "id"
        serviceObject.description == "description"
        serviceObject.name == "name"
        serviceObject.type == "type"
    }

    def "create read/writer for services" () {
        given:
        def service = v1Factory.createService()
        def services = new ServiceList().with {
            it.service = [service, service].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForServices.writeTo(services, ServiceList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        ServiceList servicesObject = readerForServices.readFrom(ServiceList.class, null, null, null, null, arrayInputStream)

        then:
        servicesObject != null
        servicesObject.service[0].id == "id"
        servicesObject.service[0].description == "description"
        servicesObject.service[0].name == "name"
        servicesObject.service[0].type == "type"
    }

    def "create read/writer for endpointTemplate" () {
        given:
        def endpointTemplate = v1Factory.createEndpointTemplate("1", "type", "publicUrl", "name").with {
            it.tenantAlias = "prefix${TENANT_ALIAS_PATTERN}"
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpointTemplate.writeTo(endpointTemplate, EndpointTemplate.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        EndpointTemplate endpointTemplateObject = readerForEndpointTemplate.readFrom(EndpointTemplate.class, null, null, null, null, arrayInputStream)

        then:
        endpointTemplateObject != null
        endpointTemplateObject.type == endpointTemplate.type
        endpointTemplateObject.publicURL == endpointTemplate.publicURL
        endpointTemplateObject.name == endpointTemplate.name
        endpointTemplateObject.id == endpointTemplate.id
        endpointTemplateObject.enabled == endpointTemplate.enabled
        endpointTemplateObject.version.id == "id"
    }

    def "create read/writer for endpoint" () {
        given:
        def link = new Link().with {
            it.type = "type"
            it.href = "href"
            it
        }
        def version = new VersionForService().with {
            it.id = "id"
            it.info = "info"
            it.list = "someList"
            it
        }
        def endpoint = v2Factory.createEndpoint().with {
            it.link = [link, link].asList()
            it.version = version
            it.publicURL = "publicUrl"
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpoint.writeTo(endpoint, Endpoint.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Endpoint endpointObject = readerForEndpoint.readFrom(Endpoint.class, null, null, null, null, arrayInputStream)

        then:
        endpointObject != null
        endpointObject.type == endpoint.type
        endpointObject.publicURL == endpoint.publicURL
        endpointObject.name == endpoint.name
        endpointObject.id == endpoint.id
        endpointObject.version.id == "id"
        endpointObject.version.list == "someList"
        endpointObject.link != null
        endpointObject.link.size() == 2
        endpointObject.link.get(0).type == "type"
        endpointObject.link.get(0).href == "href"
        endpointObject.link.get(1).type == "type"
        endpointObject.link.get(1).href == "href"
    }

    def "create read for endpoint - null info" () {
        given:
        def link = new Link().with {
            it.type = "type"
            it.href = "href"
            it
        }
        def version = new VersionForService().with {
            it.id = "id"
            it.info = null
            it.list = "someList"
            it
        }
        def endpoint = v2Factory.createEndpoint().with {
            it.link = [link, link].asList()
            it.version = version
            it.publicURL = "publicUrl"
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpoint.writeTo(endpoint, Endpoint.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Endpoint endpointObject = readerForEndpoint.readFrom(Endpoint.class, null, null, null, null, arrayInputStream)

        then:
        endpointObject != null
        endpointObject.type == endpoint.type
        endpointObject.publicURL == endpoint.publicURL
        endpointObject.name == endpoint.name
        endpointObject.id == endpoint.id
        endpointObject.version == null
        endpointObject.link != null
        endpointObject.link.size() == 2
        endpointObject.link.get(0).type == "type"
        endpointObject.link.get(0).href == "href"
        endpointObject.link.get(1).type == "type"
        endpointObject.link.get(1).href == "href"
    }

    def "create read for endpoint - null version id" () {
        given:
        def link = new Link().with {
            it.type = "type"
            it.href = "href"
            it
        }
        def version = new VersionForService().with {
            it.info = "info"
            it.list = "someList"
            it
        }
        def endpoint = v2Factory.createEndpoint().with {
            it.link = [link, link].asList()
            it.version = version
            it.publicURL = "publicUrl"
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpoint.writeTo(endpoint, Endpoint.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Endpoint endpointObject = readerForEndpoint.readFrom(Endpoint.class, null, null, null, null, arrayInputStream)

        then:
        endpointObject != null
        endpointObject.type == endpoint.type
        endpointObject.publicURL == endpoint.publicURL
        endpointObject.name == endpoint.name
        endpointObject.id == endpoint.id
        endpointObject.version == null
        endpointObject.link != null
        endpointObject.link.size() == 2
        endpointObject.link.get(0).type == "type"
        endpointObject.link.get(0).href == "href"
        endpointObject.link.get(1).type == "type"
        endpointObject.link.get(1).href == "href"
    }

    def "create read/writer for endpoint - empty link" () {
        def endpoint = v2Factory.createEndpoint().with {
            it.version = new VersionForService().with {
                it.id = "id"
                it.list = "someList"
                it.info = "info"
                it
            }
            it.link = new ArrayList<>()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpoint.writeTo(endpoint, Endpoint.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Endpoint endpointObject = readerForEndpoint.readFrom(Endpoint.class, null, null, null, null, arrayInputStream)

        then:
        endpointObject != null
        endpointObject.type == endpoint.type
        endpointObject.publicURL == endpoint.publicURL
        endpointObject.name == endpoint.name
        endpointObject.id == endpoint.id
        endpointObject.version.id == "id"
        endpointObject.link != null
        endpointObject.link.size() == 0
    }

    def "create read/writer for endpoint - null link" () {
        def endpoint = v2Factory.createEndpoint().with {
            it.version = new VersionForService().with {
                it.id = "id"
                it.list = "someList"
                it.info = "info"
                it
            }
            it.publicURL = "publicUrl"
            it.link = new ArrayList<>()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpoint.writeTo(endpoint, Endpoint.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Endpoint endpointObject = readerForEndpoint.readFrom(Endpoint.class, null, null, null, null, arrayInputStream)

        then:
        endpointObject != null
        endpointObject.type == endpoint.type
        endpointObject.publicURL == endpoint.publicURL
        endpointObject.name == endpoint.name
        endpointObject.id == endpoint.id
        endpointObject.version.id == "id"
        endpointObject.version.list == "someList"
        endpointObject.link != null
        endpointObject.link.size() == 0
    }


    def "create read/writer for endpoints" () {
        def endpoint = v2Factory.createEndpoint().with {
            it.version = new VersionForService().with {
                it.id = "id"
                it.list = "someList"
                it.info = "info"
                it
            }
            it.publicURL = "publicUrl"
            it.link = new ArrayList<>()
            it
        }
        def endpoints = new EndpointList().with {
            it.endpoint = [endpoint, endpoint].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpoints.writeTo(endpoints, EndpointList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        EndpointList endpointsObject = readerForEndpoints.readFrom(EndpointList.class, null, null, null, null, arrayInputStream)

        then:
        endpointsObject != null
        endpointsObject.endpoint.size() == 2
    }

    def "create read/writer for endpointTemplates" () {
        def version = new VersionForService().with {
            it.id = "id"
            it.list = "someList"
            it.info = "info"
            it
        }
        def endpointTemplate = v1Factory.createEndpointTemplate().with {
            it.version = version
            it
        }

        def endpointTemplates = new EndpointTemplateList().with {
            it.endpointTemplate = [endpointTemplate, endpointTemplate].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForEndpointTemplates.writeTo(endpointTemplates, EndpointTemplateList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        outer != null
        JSONArray a = outer.get(OS_KSCATALOG_ENDPOINT_TEMPLATES)
        a != null
        a.size() == 2
        a[0].id == 1
        a[0].enabled == true
        a[0].versionId == "id"
        a[0].versionList == "someList"
        a[0].versionInfo == "info"
        a[0].global == true
        a[0].name == "name"
        a[1].id == 1
        a[1].enabled == true
        a[1].versionId == "id"
        a[1].versionList == "someList"
        a[1].versionInfo == "info"
        a[1].global == true
        a[1].name == "name"
    }

    def "create read/writer for passwordCredentials" () {
        given:
        def cred = new PasswordCredentialsBase().with {
            it.password = "password"
            it.username = "username"
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForPasswordCredentials.writeTo(cred, PasswordCredentialsBase.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        PasswordCredentialsBase passwordCredentialsObject = readerForPasswordCredentials.readFrom(PasswordCredentialsBase.class, null, null, null, null, arrayInputStream)

        then:
        passwordCredentialsObject != null
        passwordCredentialsObject.password == "password"
        passwordCredentialsObject.username == "username"
    }

    def "create read/writer for group" () {
        given:
        def group = v1Factory.createGroup()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForGroup.writeTo(group, Group.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        JSONObject o = outer.get(RAX_KSGRP_GROUP)
        o.get(NAME) == "name"
        o.get(ID) == "id"
        o.get(DESCRIPTION) == "description"
    }

    def "create read/writer for groups" () {
        given:
        Group group = new Group().with {
            it.name = "name"
            it
        }
        def groups = new Groups().with {
            it.group = [group, group].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForGroups.writeTo(groups, Groups.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Groups groupsObject = readerForGroups.readFrom(Groups.class, null, null, null, null, arrayInputStream)

        then:
        groupsObject != null
        groupsObject.group.size() == 2
    }

    def "create read/writer for groupsList" () {
        given:
        com.rackspacecloud.docs.auth.api.v1.Group group = new com.rackspacecloud.docs.auth.api.v1.Group().with {
            it.id = "id"
            it.description = "description"
            it
        }
        com.rackspacecloud.docs.auth.api.v1.Group group2 = new com.rackspacecloud.docs.auth.api.v1.Group().with {
            it.id = "otherId"
            it.description = "desc"
            it
        }
        def groupsList = new GroupsList().with {
            it.group = [group, group2].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForGroupsList.writeTo(groupsList, GroupsList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        outer.get(GROUPS) != null
        JSONObject o = outer.get(GROUPS)
        o != null
        JSONArray a = o.get(VALUES)
        a.size() == 2
        a[0].id == "id"
        a[0].description == "description"
        a[1].id == "otherId"
        a[1].description == "desc"
    }

    def "create read/writer for defaultRegionServices" () {
        given:
        def serviceName = ["cloudFile", "cloudServers"].asList()
        def defaultRegionServices = new DefaultRegionServices().with {
            it.serviceName = serviceName
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForDefaultRegionServices.writeTo(defaultRegionServices, DefaultRegionServices.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        DefaultRegionServices defaultRegionServicesObject = readerForDefaultRegionServices.readFrom(DefaultRegionServices.class, null, null, null, null, arrayInputStream)

        then:
        defaultRegionServicesObject != null
        defaultRegionServices.serviceName[0] == "cloudFile"
        defaultRegionServices.serviceName[1] == "cloudServers"
    }

    def "create read/writer for extension" () {
        given:
        def extension = v1Factory.createExtension()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForExtension.writeTo(extension, Extension.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(EXTENSION)
        o != null
        o.get(NAME) == "name"
        o.get(DESCRIPTION) == "description"
        JSONArray a = o.get(LINKS)
        a.size() == 2
    }

    def "create read/writer for versionChoice" () {
        given:
        def versionChoice = v1Factory.createVersionChoice()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForVersionChoice.writeTo(versionChoice, VersionChoice.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(VERSION)
        o.get(ID) == "id"
        o.get(STATUS) == "CURRENT"
        JSONArray a = o.get(LINKS)
        a.size() == 2
    }

    def "create read/writer for extensions" () {
        given:
        def extension = v1Factory.createExtension()
        def extensions = new Extensions().with {
            it.extension = [extension, extension].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForExtensions.writeTo(extensions, Extensions.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONArray a = outer.get(EXTENSIONS)
        a.size() == 2
    }

    def "create read/writer for credentials" () {
        given:
        def apiKeyCred = v2Factory.createJAXBApiKeyCredentials("username", "apiKey")
        def pwdCred = v2Factory.createJAXBPasswordCredentialsBase("username", "password")
        def credentials = new CredentialListType().with {
            it.credential = [apiKeyCred, pwdCred].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForCredentialListType.writeTo(credentials, CredentialListType.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONArray o = outer.get(CREDENTIALS)
        def api = o.getAt(RAX_KSKEY_API_KEY_CREDENTIALS)
        api.getAt(API_KEY)[0] == "apiKey"
        def pwd = o.getAt(PASSWORD_CREDENTIALS)
        pwd.getAt(PASSWORD)[0] == "password"

    }

    def "create read/writer for roles" () {
        given:
        def role = v2Factory.createRole()
        def roles = new RoleList().with {
            it.role = [role, role].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForRoles.writeTo(roles, RoleList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONArray o = outer.get(ROLES)
        o.size() == 2
        ((JSONObject)o[0]).get(NAME) == "name"
    }

    def "create read/writer for users" () {
        given:
        def user = v2Factory.createUserForCreate("username", "displayName", "email", true, "ORD", "domainId", "password")
        def users = new UserList().with {
            it.user = [user, user].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForUsers.writeTo(users, UserList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONArray o = outer.get(USERS)
        o.size() == 2
        ((JSONObject)o[0]).get(USERNAME) == "username"
        ((JSONObject)o[0]).get(OS_KSADM_PASSWORD) == "password"
    }


    def "create read/writer for AuthenticateResponse" () {
        given:
        Token token = v2Factory.createToken()
        ServiceCatalog sc = v2Factory.createServiceCatalog()
        def role = v2Factory.createRole()
        def roles = new RoleList().with {
            it.role = [role, role].asList()
            it
        }
        def user = v2Factory.createUserForAuthenticateResponse("id", "name", roles)
        user.defaultRegion = "defaultRegion"
        def authResponse = v2Factory.createAuthenticateResponse(token, sc, user)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForAuthenticateResponse.writeTo(authResponse, AuthenticateResponse.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(ACCESS)
        JSONObject t = o.get(TOKEN)
        t.get(ID) == "id"
        JSONObject u = o.get(USER)
        u.get(RAX_AUTH_DEFAULT_REGION) == "defaultRegion"
        u.get(NAME) == "name"
    }

    def "can write authenticatedBy using json"() {
        when:
        def response = v2Factory.createAuthenticateResponse()
        def authenticatedBy = new AuthenticatedBy().with {
            it.credential = input.asList()
            it
        }
        response.getToken().setAuthenticatedBy(authenticatedBy)

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForAuthenticateResponse.writeTo(response, AuthenticateResponse.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        JSONParser parser = new JSONParser();
        JSONObject authResponse = (JSONObject) parser.parse(json);
        JSONObject access = (JSONObject)authResponse.get(JSONConstants.ACCESS)
        JSONObject token = (JSONObject)access.get(JSONConstants.TOKEN)
        JSONArray authenticatedByList = (JSONArray) token.get(JSONConstants.RAX_AUTH_AUTHENTICATED_BY)
        authenticatedByList as Set == input as Set


        where:
        input << [
                ["RSA"],
                ["RSA", "Password"]
        ]
    }

    def "create read/writer for ImpersonationResponse" () {
        given:
        Token token = v2Factory.createToken()
        def impersonationResponse = new ImpersonationResponse().with {
            it.token =  token
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForImpersonationResponse.writeTo(impersonationResponse, ImpersonationResponse.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(ACCESS)
        JSONObject t = o.get(TOKEN)
        t.get(ID) == "id"
    }

    def "create read/writer for baseURLList" () {
        given:
        def baseUrl = v1Factory.createBaseUrl()
        def baseUrlList = new BaseURLList().with {
            it.baseURL = [baseUrl, baseUrl].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForBaseURLList.writeTo(baseUrlList, BaseURLList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONArray a = outer.get(JSONConstants.BASE_URLS)
        a.size() == 2
    }

    def "create read/writer for v1.1 user" () {
        given:
        def baseUrlRef = v1Factory.createBaseUrlRef()
        def baseUrlList = new BaseURLRefList().with {
            it.baseURLRef = [baseUrlRef, baseUrlRef].asList()
            it
        }

        def user = v1Factory.createUser().with {
            it.baseURLRefs = baseUrlList
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForUser11.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(USER)
        o.get(ID) == "username"
        o.get(ENABLED) == true
        o.get(KEY) == "key"
        JSONArray a = o.get(BASE_URL_REFS)
        a.size() == 2
    }

    def "create read/writer for v1.1 user - test null values" () {
        given:
        def baseUrlRef = v1Factory.createBaseUrlRef()
        def baseUrlList = new BaseURLRefList().with {
            it.baseURLRef = [baseUrlRef, baseUrlRef].asList()
            it
        }

        def user = v1Factory.createUser().with {
            it.baseURLRefs = baseUrlList
            it.nastId = nastId
            it.mossoId = mossoId
            it.key = key
            it.id = id
            it.enabled = enabled
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForUser11.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(USER)
        o.get(ID) == id
        if(enabled != null){
            o.get(ENABLED) == enabled
        }else{
            o.get(ENABLED) == true
        }
        o.get(KEY) == key
        o.get(MOSSO_ID) == mossoId
        o.get(NAST_ID) == nastId
        JSONArray a = o.get(BASE_URL_REFS)
        a.size() == 2

        where:
        id   | nastId | mossoId | key   | enabled
        "id" | "nast" | 1       | "key" | true
        "id" | "nast" | 1       | null  | true
        "id" | "nast" | null    | null  | false
        "id" | null   | null    | null  | true
        null | "nast" | 1       | "key" | false
        null | null   | 1       | "key" | false
        null | "nast" | null    | "key" | false
        null | "nast" | 1       | null  | false
        null | "nast" | 1       | null  | null
    }

    def "create read/writer for baseURLRefList" () {
        given:
        def baseUrlRef = v1Factory.createBaseUrlRef()
        def baseUrlList = new BaseURLRefList().with {
            it.baseURLRef = [baseUrlRef, baseUrlRef].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForBaseURLRefList.writeTo(baseUrlList, BaseURLRefList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONArray a = outer.get(JSONConstants.BASE_URL_REFS)
        a.size() == 2
        a[0].id == 1
        a[0].v1Default == true
        a[0].href == "href"
        a[1].id == 1
        a[1].v1Default == true
        a[1].href == "href"
    }

    def "create read/writer for domain" () {
        given:
        def domain = v1Factory.createDomain()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForDomain.writeTo(domain, Domain.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject a = outer.get(JSONConstants.RAX_AUTH_DOMAIN)
        a.get(ID) == "id"
        a.get(NAME) == "name"
        a.get(ENABLED) == true
    }

    def "create read/writer for domains" () {
        given:
        def domain = v1Factory.createDomain()
        def domains = v1Factory.createDomains([domain, domain].asList())

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForDomains.writeTo(domains, Domains.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONArray a = outer.get(JSONConstants.RAX_AUTH_DOMAINS)
        a.size() == 2
        ((JSONObject)a[0]).get(ID) == "id"
        ((JSONObject)a[0]).get(NAME) == "name"
        ((JSONObject)a[0]).get(ENABLED) == true
    }

    def "can write baseUrlRefList"() {
        when:
        def baseUrlRefs = new ArrayList<BaseURLRefList>();
        baseUrlRefs.add(v1Factory.createBaseUrlRef(baseUrlId,publicUrl,v1Default));
        def baseUrlRefList = new BaseURLRefList();
        baseUrlRefList.baseURLRef = baseUrlRefs;

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForBaseURLRefList.writeTo(baseUrlRefList, BaseURLRefList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        JSONParser parser = new JSONParser();
        JSONObject baseResponse = (JSONObject) parser.parse(json);
        baseResponse.size() == 1;
        baseResponse.get("baseURLRefs")[0]["id"] == baseUrlId;
        baseResponse.get("baseURLRefs")[0]["href"] == publicUrl;
        baseResponse.get("baseURLRefs")[0]["v1Default"] == v1Default;

        where:
        baseUrlId = 1;
        publicUrl = "http://public/";
        v1Default = false;
    }

    def "can writer authData" () {
        given:
        GregorianCalendar c = new GregorianCalendar()
        c.setTime(new Date().plus(1))
        def token = new com.rackspacecloud.docs.auth.api.v1.Token().with {
            it.id = "token"
            it.expires = DatatypeFactory.newInstance().newXMLGregorianCalendar(c)
            it
        }
        def endpoint = new com.rackspacecloud.docs.auth.api.v1.Endpoint().with {
            it.adminURL = "adminUrl"
            it.internalURL = "internalUrl"
            it.publicURL = "publicUrl"
            it.region = "region"
            it.v1Default = false
            it
        }
        def service = new com.rackspacecloud.docs.auth.api.v1.Service().with {
            it.endpoint = [endpoint, endpoint].asList()
            it.name = "name"
            it
        }
        def service2 = new com.rackspacecloud.docs.auth.api.v1.Service().with {
            it.endpoint = [endpoint, endpoint].asList()
            it.name = "other"
            it
        }
        def serviceCatalog = new com.rackspacecloud.docs.auth.api.v1.ServiceCatalog().with {
            it.service = [service, service2].asList()
            it
        }
        def authData = new AuthData().with {
            it.token = token
            it.serviceCatalog = serviceCatalog
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForAuthData.writeTo(authData, AuthData.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(AUTH)
        JSONObject tokenObject = o.get(TOKEN)
        tokenObject.get(ID) == "token"
        JSONObject a = o.get(SERVICECATALOG)
        JSONArray oArray = a.get("other")
        JSONArray nArray = a.get("name")
        oArray.size() == 2
        nArray.size() == 2
        oArray[0].get(REGION) == "region"
        nArray[0].get(REGION) == "region"
        oArray[1].get(PUBLIC_URL) == "publicUrl"
        nArray[1].get(PUBLIC_URL) == "publicUrl"
    }

    def "create read/writer for serviceCatalog" () {
        given:
        def endpoint = new com.rackspacecloud.docs.auth.api.v1.Endpoint().with {
            it.adminURL = "adminUrl"
            it.publicURL = "publicUrl"
            it.region = "ORD"
            it.v1Default = false
            it.internalURL = "internalUrl"
            it
        }

        def service = new com.rackspacecloud.docs.auth.api.v1.Service().with {
            it.name = "name"
            it.endpoint  = [endpoint, endpoint].asList()
            it
        }
        def service2 = new com.rackspacecloud.docs.auth.api.v1.Service().with {
            it.name = "other"
            it.endpoint  = [endpoint, endpoint].asList()
            it
        }

        def serviceCatalog = new com.rackspacecloud.docs.auth.api.v1.ServiceCatalog().with {
            it.service = [service, service2].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForServiceCatalog.writeTo(serviceCatalog, com.rackspacecloud.docs.auth.api.v1.ServiceCatalog.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        JSONParser parser = new JSONParser();
        JSONObject outer = (JSONObject) parser.parse(json);

        then:
        json != null
        JSONObject o = outer.get(SERVICECATALOG)
        JSONArray array = o.get("name")
        array[0].get(REGION) == "ORD"
        array[0].get(PUBLIC_URL) == "publicUrl"
        array[0].get(INTERNAL_URL) == "internalUrl"
        array[0].get(ADMIN_URL) == "adminUrl"

        JSONArray otherArray = o.get("other")
        otherArray[0].get(REGION) == "ORD"
        otherArray[0].get(PUBLIC_URL) == "publicUrl"
        otherArray[0].get(INTERNAL_URL) == "internalUrl"
        otherArray[0].get(ADMIN_URL) == "adminUrl"
    }

    def getSecretQA(String id, String question, String answer) {
        new SecretQA().with {
            it.id = id
            it.question = question
            it.answer = answer
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

    def getMobilePhone(String phoneNumber) {
        new MobilePhone().with {
            it.number = phoneNumber
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
