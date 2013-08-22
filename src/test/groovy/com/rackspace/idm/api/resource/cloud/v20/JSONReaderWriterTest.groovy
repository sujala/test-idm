package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.api.resource.cloud.JSONReaders.*
import com.rackspace.idm.api.resource.cloud.JSONWriters.*
import com.rackspace.idm.exception.BadRequestException
import com.rackspacecloud.docs.auth.api.v1.GroupsList
import org.apache.commons.io.IOUtils
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.*
import org.w3._2005.atom.Link
import spock.lang.Shared
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

    @Shared JSONReaderForOsKsCatalogEndpointTemplates readerForEndpointTemplates = new JSONReaderForOsKsCatalogEndpointTemplates()
    @Shared JSONWriterForOsKsCatalogEndpointTemplates writerForEndpointTemplates = new JSONWriterForOsKsCatalogEndpointTemplates()

    @Shared JSONReaderForPasswordCredentials readerForPasswordCredentials = new JSONReaderForPasswordCredentials()
    @Shared JSONWriterForPasswordCredentials writerForPasswordCredentials = new JSONWriterForPasswordCredentials()

    @Shared JSONReaderForRaxKsGroup readerForGroup = new JSONReaderForRaxKsGroup()
    @Shared JSONWriterForRaxKsGroup writerForGroup = new JSONWriterForRaxKsGroup()

    @Shared JSONReaderForRaxKsGroups readerForGroups = new JSONReaderForRaxKsGroups()
    @Shared JSONWriterForRaxKsGroups writerForGroups = new JSONWriterForRaxKsGroups()

    @Shared JSONReaderForGroups readerForGroupsList = new JSONReaderForGroups()
    @Shared JSONWriterForGroups writerForGroupsList = new JSONWriterForGroups()

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
        def endpointTemplate = v1Factory.createEndpointTemplate("1", "type", "publicUrl", "name")

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
            it.type = type
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
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        EndpointTemplateList endpointTemplatesObject = readerForEndpointTemplates.readFrom(EndpointTemplateList.class, null, null, null, null, arrayInputStream)

        then:
        endpointTemplatesObject != null
        endpointTemplatesObject.endpointTemplate.size() == 2
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
    }

    def "create read/writer for group" () {
        given:
        def group = v1Factory.createGroup()

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForGroup.writeTo(group, Group.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        Group groupObject = readerForGroup.readFrom(Group.class, null, null, null, null, arrayInputStream)

        then:
        groupObject != null
        groupObject.name == "name"
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
        def groupsList = new GroupsList().with {
            it.group = [group, group].asList()
            it
        }

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForGroupsList.writeTo(groupsList, GroupsList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes())
        GroupsList groupsListObject = readerForGroupsList.readFrom(GroupsList.class, null, null, null, null, arrayInputStream)

        then:
        groupsListObject != null
        groupsListObject.group.size() == 2
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
