package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspacecloud.docs.auth.api.v1.User
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Token

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/24/12
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
class V1Factory {

    private static ID = "id"
    private static NAME = "name"
    private static DESCRIPTION = "description"
    private static USERNAME = "username"
    private static DISPLAY = "displayName"
    private static EMAIL = "email@example.com"
    private static PASSWORD = "Password1"
    private static KEY = "key";
    private static MOSSOID = 10000123

    private static objFactory = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory()
    private static V2Factory v2Factory = new V2Factory()

    def createApiKeyCredentials() {
        return createApiKeyCredentials("username", "apiKey")
    }

    def createApiKeyCredentials(String username, String apiKey) {
        new ApiKeyCredentials().with {
            it.username = username
            it.apiKey = apiKey
            return it
        }
    }

    def createJAXBApiKeyCredentials(String username, String apiKey) {
        def credentials = createApiKeyCredentials(username, apiKey)
        return objFactory.createApiKeyCredentials(credentials)
    }

    def createCapability() {
        return createCapability(ID, NAME)
    }

    def createCapability(String id, String name) {
        new Capability().with {
            it.id = id ? id : ID
            it.name = name ? name : NAME
            return it
        }
    }
    def createCapabilities() {
        return createCapabilities(null)
    }

    def createCapabilities(List<Capability> capabilityList) {
        def list = capabilityList ? capabilityList : [].asList()
        new Capabilities().with {
            it.getCapability().addAll(list)
            return it
        }
    }

    def createDomain() {
        return createDomain("id", "name")
    }

    def createDomain(String id, String name) {
        new Domain().with {
            it.id = id
            it.name = name
            it.enabled = true
            return it
        }
    }

    def createDomains() {
        return createDomains(null)
    }

    def createDomains(List<Domain> domainList) {
        def list = domainList ? domainList : [].asList()
        new Domains().with {
            it.getDomain().addAll(list)
            return it
        }
    }

    def createEndpointTemplate() {
        return createEndpointTemplate(1, NAME)
    }

    def createEndpointTemplate(int id, String name) {
        new EndpointTemplate().with {
            it.id = id
            it.name = name
            it.enabled = true
            it.global = true
            return it
        }
    }

    def createEndpointTemplateList() {
        return createEndpointTemplateList(null)
    }

    def createEndpointTemplateList(List<EndpointTemplate> templates) {
        def list = templates ? templates : [].asList()
        new EndpointTemplateList().with {
            it.getEndpointTemplate().addAll(list)
            return it
        }
    }

    def createImpersonationRequest(org.openstack.docs.identity.api.v2.User user) {
        new ImpersonationRequest().with {
            it.user = user
            return it
        }
    }

    def createImpersonationResponse() {
        return createImpersonationResponse(v2Factory.createToken())
    }

    def createImpersonationResponse(Token token) {
        new ImpersonationResponse().with {
            it.token = token
            return it
        }
    }

    def createPolicies() {
        return createPolicies(null)
    }

    def createPolicies(List<Policy> policyList) {
        def list = policyList ? policyList : [].asList()
        new Policies().with {
            it.policy = list
            return it
        }
    }

    def createPolicy() {
        return createPolicy(ID, "blob")
    }

    def createPolicy(String id, String blob) {
        new Policy().with {
            it.id = id
            it.blob = blob
            it.enabled = true
            it.global = true
            return it
        }
    }

    def createRole() {
        return createRole(NAME)
    }

    def createRole(String name) {
        new Role().with {
            it.name = name
            return it
        }
    }

    def createRsaCredentials() {
        return createRsaCredentials("username", "tokenKey")
    }

    def createRsaCredentials(String username, String tokenKey) {
        new RsaCredentials().with {
            it.username = username
            it.tokenKey = tokenKey
            return it
        }
    }

    def createSecretQA() {
        return createSecretQA("1", "answer", "question")
    }

    def createSecretQA(String id, String answer, String question) {
        new SecretQA().with {
            it.id = id
            it.answer  = answer
            it.question = question
            return it
        }
    }

    def createSecretQA_Keystone() {
        return createSecretQA_Keystone("username", "answer", "question")
    }

    def createSecretQA_Keystone(String username, String answer, String question) {
        new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA().with{
            it.username = username
            it.answer  = answer
            it.question = question
            return it
        }
    }
    def createService() {
        return createService(ID, NAME)
    }

    def createService(String id, String name) {
        new Service().with {
            it.id = id
            it.name = name ? name : NAME
            return it
        }
    }

    def createServiceList() {
        return createServiceList(null)
    }

    def createServiceList(List<Service> serviceList) {
        def list = serviceList ? serviceList : [].asList()
        new ServiceList().with {
            it.getService().addAll(list)
            return it
        }
    }

    def createUserForCreate() {
        return createUserForCreate(USERNAME, PASSWORD, EMAIL)
    }

    def createUserForCreate(String username, String password, String email) {
        new UserForCreate().with {
            it.username = username
            it.password = password
            it.email = email
            it.enabled = true
            return it
        }
    }

    def createUser(){
        return createUser(USERNAME, KEY, MOSSOID)
    }

    def createUser(String id, String key, Integer mossoId){
        new User().with {
            it.id = id
            it.key = key
            it.mossoId = mossoId
            it.enabled = true
            return it
        }
    }

    def createRegion(){
        return createRegion("name",true, false)
    }

    def createRegion(String name, Boolean enabled, Boolean isDefault){
        return new Region().with {
            it.name = name
            it.enabled = enabled
            it.isDefault = isDefault
            return it
        }
    }

    def createGroup(){
        return createGroup("name","id", "description")
    }

    def createGroup(String name, String id, String description){
        return new Group().with {
            it.name = name
            it.id = id
            it.description = description
            return it
        }
    }

    def createQuestion(){
        return createQuestion("id", "question?")
    }

    def createQuestion(String id, String question){
        return new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def createDefaultRegionServices(List<String> serviceName){
        new DefaultRegionServices().with {
            it.serviceName = serviceName
            return it
        }
    }

}
