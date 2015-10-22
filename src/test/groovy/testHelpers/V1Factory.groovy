package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspacecloud.docs.auth.api.v1.BaseURL
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.MossoCredentials
import com.rackspacecloud.docs.auth.api.v1.NastCredentials
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials
import com.rackspacecloud.docs.auth.api.v1.User
import com.rackspacecloud.docs.auth.api.v1.UserCredentials
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey
import org.joda.time.DateTime
import org.openstack.docs.common.api.v1.Extension
import org.openstack.docs.common.api.v1.VersionChoice
import org.openstack.docs.common.api.v1.VersionStatus
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse
import org.openstack.docs.identity.api.v2.Token
import org.openstack.docs.identity.api.v2.VersionForService
import org.springframework.stereotype.Component
import org.w3._2005.atom.Link

import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeFactory
import javax.xml.namespace.QName

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/24/12
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
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
    private static TYPE="type"

    private static objFactory = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory()

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

    def createUserKeyCredentials(String username, String apiKey) {
        new UserCredentials().with {
            it.username = username
            it.key = apiKey
            return it
        }
    }

    def createMossoCredentials(Integer mossoId, String apiKey) {
        new MossoCredentials().with {
            it.mossoId = mossoId
            it.key = apiKey
            it
        }
    }

    def createNastCredentials(String nastId, String apiKey) {
        new NastCredentials().with {
            it.nastId = nastId
            it.key = apiKey
            it
        }
    }

    def createJAXBApiKeyCredentials(String username, String apiKey) {
        def credentials = createApiKeyCredentials(username, apiKey)
        return objFactory.createApiKeyCredentials(credentials)
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

    def createEndpointTemplate(String id, String type = "compute", String publicUrl = "http://public.url", String name, enabled = true, region = null) {
        def localPublicUrl = publicUrl
        new EndpointTemplate().with {
            it.id = id as int
            it.type = type
            it.publicURL = localPublicUrl
            it.name = name
            it.enabled = enabled
            it.region = region
            it.version = new VersionForService().with {
                it.id = "id"
                it.info = "info"
                it.list = "list"
                it
            }
            return it
        }
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

    def createEndpointTemplateForUpdate(boolean enabled, boolean global, boolean _default) {
        new EndpointTemplate().with {
            it.enabled = enabled
            it.global = global
            it.setDefault(_default)
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
        def token = new Token().with {
            it.id = ID
            it.expires = DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar())
            it.tenant = new TenantForAuthenticateResponse()
            return it
        }
        return createImpersonationResponse(token)
    }

    def createImpersonationResponse(Token token) {
        new ImpersonationResponse().with {
            it.token = token
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

    def createRole(String name, String tenantId) {
        new Role().with {
            it.name = name
            it.tenantId = tenantId
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
            it
        }
    }

    def createSecretQA(String id, String answer) {
        new SecretQA().with {
            it.id = id
            it.answer = answer
            return it
        }
    }

    def createRaxKsQaSecretQA() {
        return createRaxKsQaSecretQA("username", "answer", "question")
    }

    def createRaxKsQaSecretQA(String username, String answer, String question) {
        new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA().with {
            it.username = username
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
            it.description = DESCRIPTION
            it.type = TYPE
            return it
        }
    }

    def createService(String id, String name, String type) {
        new Service().with {
            it.name = name
            it.type = type
            if (id != null) {
                it.id = id
            }
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

    def createUserWithOnlyEnabled(boolean enabled){
        new UserWithOnlyEnabled().with {
            it.enabled = enabled
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

    def createUser(String id, String key, Integer mossoId, String nastId, Boolean enabled) {
        new User().with {
            it.id = id
            it.key = key
            it.mossoId = mossoId
            it.nastId = nastId
            it.enabled = enabled
            return it
        }
    }

    def createRegion(){
        return createRegion("name", true, false)
    }

    def createRegion(String name) {
        return createRegion(name, true, false)
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

    def createGroup(String name, String description) {
        return createGroup(name, null, description)
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

    def createBaseUrlRef() {
        return createBaseUrlRef(1,"href", true)
    }

    def createBaseUrlRef(Integer id, String href, Boolean v1Default) {
        new BaseURLRef().with {
            it.id = id
            it.href = href
            it.v1Default = v1Default
            return it
        }
    }

    def createBaseUrl(){
        return createBaseUrl(1,"serviceName", "DFW", true, false, "http://public", "http://admin", "http://internal")
    }

    def createBaseUrl(Integer id, String serviceName, String region, Boolean enabled, Boolean defaul, String publicURL, String adminURL, String internalURL) {
        new BaseURL().with {
            it.id = id
            it.serviceName = serviceName
            it.region = region
            it.enabled = enabled
            it.default = defaul
            it.publicURL = publicURL
            it.adminURL = adminURL
            it.internalURL = internalURL
            return it
        }
    }

    def createPasswordCredentials(String username, String password) {
        new PasswordCredentials().with {
            it.password = password
            it.username = username
            return it
        }
    }

    def createExtension(){
        return createExtension("alias", "description", "name", "namespace")
    }

    def createExtension(String alias, String description, String name, String namespace){
        def link = new Link().with {
            it.href = "href"
            it.type = "application/xhtml+xml"
            it
        }
        def jaxBLink = new JAXBElement(new QName("org.w3._2005.atom", "link"), Link, link)

        new Extension().with {
            it.alias = alias
            it.description = description
            it.name = name
            it.namespace = namespace
            it.any = [jaxBLink, jaxBLink].asList()
            it
        }
    }

    def createVersionChoice(){
        return createVersionChoice("id", VersionStatus.CURRENT)
    }

    def createVersionChoice(String id, VersionStatus status){
        def link = new Link().with {
            it.href = "href"
            it.type = "application/xhtml+xml"
            it
        }
        def jaxBLink = new JAXBElement(new QName("org.w3._2005.atom", "link"), Link, link)
        new VersionChoice().with {
            it.id = id
            it.status = status
            it.any = [jaxBLink, jaxBLink].asList()
            it
        }
    }

    def createUserWithOnlyKey(String key){
        new UserWithOnlyKey().with {
            it.key = key
            it
        }
    }
}
