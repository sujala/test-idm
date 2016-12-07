package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ApprovedDomainIds
import com.rackspace.docs.identity.api.ext.rax_auth.v1.BypassCodes
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasscodeCredentials
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordReset
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificate
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificates
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RsaCredentials
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ScopeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.Constants
import com.rackspace.idm.RaxAuthConstants
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import org.apache.commons.collections4.CollectionUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.*;
import org.springframework.stereotype.Component

import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.Duration
import javax.xml.namespace.QName

import static com.rackspace.idm.RaxAuthConstants.*

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/24/12
 * Time: 10:14 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
class V2Factory {

    private static ID = "id"
    private static USERNAME = "username"
    private static NAME = "name"
    private static objFactory = new ObjectFactory()
    private static raxAuthObjFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

    def createAuthenticationRequest() {
        return createAuthenticationRequest("tenantId", "tenantName", null)
    }

    def createAuthenticationRequest(String tenantId, String tenantName, Object credential) {
        new AuthenticationRequest().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.credential = credential
            return it
        }
    }

    def createAuthenticationRequest(String tokenId, String tenantId, String tenantName) {
        def token = new TokenForAuthenticationRequest().with {
            it.id = tokenId
            return it
        }

        return new AuthenticationRequest().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.token = token
            return it
        }
    }

    def createTokenAuthenticationRequest(String tokenId, String tenantId, String tenantName) {
        def token = new TokenForAuthenticationRequest().with {
            it.id = tokenId
            return it
        }

        return new AuthenticationRequest().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.token = token
            return it
        }
    }

    def createPasswordAuthenticationRequestWithTenantId(String username, String password, String tenantId) {
        def credentials = createPasswordCredentialsRequiredUsername(username, password)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createPasswordCredentials(credentials))
            it.setTenantId(tenantId)
            return it
        }
    }

    def createPasswordAuthenticationRequestWithTenantName(String username, String password, String tenantName) {
        def credentials = createPasswordCredentialsRequiredUsername(username, password)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createPasswordCredentials(credentials))
            it.setTenantName(tenantName)
            return it
        }
    }

    def createPasswordAuthenticationRequestWithScope(String username, String password, String scope) {
        def credentials = createPasswordCredentialsRequiredUsername(username, password)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createPasswordCredentials(credentials))
            it.scope = ScopeEnum.fromValue(scope)
            return it
        }
    }

    def createApiKeyAuthenticationRequestWithScope(String username, String apiKey, String scope) {
        def credentials = createApiKeyCredentials(username, apiKey)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createCredential(credentials))
            it.scope = ScopeEnum.fromValue(scope)
            return it
        }
    }

    def createApiKeyAuthenticationRequestWithTenantId(String username, String apiKey, String tenantId) {
        def credentials = createApiKeyCredentials(username, apiKey)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createCredential(credentials))
            it.tenantId = tenantId
            return it
        }
    }


    def createPasswordAuthenticationRequest(String username, String password) {
        def credentials = createPasswordCredentialsRequiredUsername(username, password)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createPasswordCredentials(credentials))
            return it
        }
    }

    def createPasscodeAuthenticationRequest(String passcode) {
        def credentials = createPasscodeCredentials(passcode)

        new AuthenticationRequest().with {
            it.setCredential(raxAuthObjFactory.createPasscodeCredentials(credentials))
            return it
        }
    }

    def createPasscodeAuthenticationRequestWithTenantId(String passcode, String tenantId) {
        def credentials = createPasscodeCredentials(passcode)

        new AuthenticationRequest().with {
            it.setCredential(raxAuthObjFactory.createPasscodeCredentials(credentials))
            it.tenantId = tenantId
            return it
        }
    }

    def createApiKeyAuthenticationRequest(String username, String apiKey) {
        def credentials = createApiKeyCredentials(username, apiKey)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createCredential(credentials))
            return it
        }
    }

    def createRsaAuthenticationRequest(String username, String tokenKey ) {
        def credentials = createRsaCredentials(username, tokenKey)

        new AuthenticationRequest().with {
            it.setCredential(objFactory.createCredential(credentials))
            return it
        }
    }

    def createAuthenticateResponse() {
        return createAuthenticateResponse(createToken(), null, null)
    }

    def createAuthenticateResponse(Token token, ServiceCatalog serviceCatalog, UserForAuthenticateResponse user) {
        new AuthenticateResponse().with {
            it.token = token ? token : createToken()
            it.serviceCatalog = serviceCatalog ? serviceCatalog : new ServiceCatalog()
            it.user = user ? user : new UserForAuthenticateResponse()
            return it
        }
    }

    def createEndpoint() {
        return createEndpoint(1, "tenantId", NAME, "region")
    }

    def createEndpoint(int id, String tenantId, String name, String region) {
        new Endpoint().with {
            it.id = id
            it.tenantId = tenantId
            it.name = name
            it.region = region
            return it
        }
    }

    def createEndpointList() {
        return createEndpointList(null)
    }

    def createEndpointList(List<Endpoint> endpoints) {
        def list = endpoints ? endpoints : [].asList()
        new EndpointList().with {
            it.getEndpoint().addAll(list)
            return it
        }
    }

    def createPasswordCredentialsBase() {
        return createPasswordCredentialsBase("username", "Password1")
    }

    def createPasswordCredentialsBase(String username, String password) {
        new PasswordCredentialsBase().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def createJAXBPasswordCredentialsBase(String username, String password) {
        def credential = createPasswordCredentialsBase(username, password)
        return objFactory.createCredential(credential)
    }

    def createPasswordCredentialsRequiredUsername(String username, String password) {
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def createPasscodeCredentials(String passcode) {
        new PasscodeCredentials().with {
            it.passcode = passcode
            return it
        }
    }

    def createJAXBApiKeyCredentials(String username, String apiKey){
        def credential = createApiKeyCredentials(username, apiKey)
        return objFactory.createCredential(credential)
    }

    def createApiKeyCredentials(String username, String apiKey){
        return new ApiKeyCredentials().with {
            it.apiKey = apiKey
            it.username = username
            return it
        }
    }

    def createJAXBRsaCredentials(String username, String tokenKey){
        def credential = createRsaCredentials(username, tokenKey)
        return objFactory.createCredential(credential)
    }

    def createRsaCredentials(String username, String tokenKey){
        return new RsaCredentials().with {
            it.tokenKey = tokenKey
            it.username = username
            return it
        }
    }

    def createGroup(String name) {
        new Group().with {
            it.name = name
            return it
        }
    }

    def createGroups(List<Group> groups) {
        def list = groups ? groups : [].asList()
        new Groups().with {
            it.getGroup().addAll(groups)
            return it
        }
    }

    def createRole() {
        return createRole(NAME, "applicationId", "tenantId")
    }

    def createRole(String name) {
        new Role().with {
            it.name = name
            it.description = "Test Global Role"
            return it
        }
    }

    def createRole(String name, String serviceId, String tenantId) {
        new Role().with {
            it.name = name
            it.serviceId = serviceId
            it.tenantId = tenantId
            return it
        }
    }

    def createRole(String name, String serviceId) {
        new Role().with {
            it.name = name
            it.serviceId = serviceId
            return it
        }
    }

    def createRole(Object propagate, Object assignment = RoleAssignmentEnum.BOTH) {
        def other = createOtherMap(propagate)
        def random = UUID.randomUUID().toString().replace("-", "")
        return new Role().with {
            it.name = "role$random"
            it.description = "desc"
            it.propagate = propagate
            it.assignment = assignment
            it.otherAttributes = other
            return it
        }
    }

    def createOtherMap(Object propagate) {
        def map = new HashMap<QName, Object>()
        if (propagate != null) {
            map.put(QNAME_PROPAGATE, Boolean.toString(propagate))
        }
        return map
    }

    def createRoleList() {
        return createRoleList(null)
    }

    def createRoleList(List<Role> roleList) {
        def list = roleList ? roleList : [].asList()
        new RoleList().with {
            it.getRole().addAll(list)
            return it
        }
    }

    def createServiceCatalog() {
        return createServiceCatalog(null)
    }

    def createServiceCatalog(List<ServiceForCatalog> services) {
        def list = services ? services : [].asList()
        new ServiceCatalog().with {
            it.getService().addAll(list)
            it
        }
    }
    def createTenant() {
        return createTenant(ID, NAME)
    }

    def createTenant(String id, String name, List<String> typelist) {
        def tenant = createTenant(id, name)
        Types types = new Types()
        for (String type : typelist) {
            types.type.add(type)
        }
        tenant.setTypes(types)
        return tenant
    }

    def createTenant(String id, String name) {
        new Tenant().with {
            it.id = id
            it.name = name
            it.enabled = true
            return it
        }
    }

    def createTenant(name, displayName, boolean enabled, domainId=null) {
        new Tenant().with {
            it.name = name.toString()
            it.displayName = displayName.toString()
            it.enabled = enabled
            it.domainId = domainId
            return it
        }
    }

    def createTenants(List<Tenant> tenantList) {
        def list = tenantList ? tenantList : [].asList
        new Tenants().with {
            it.getTenant().addAll(list)
            return it
        }
    }

    def createToken() {
        return createToken(ID)
    }

    def createTokenForAuthenticationRequest(){
        return createTokenForAuthenticationRequest(ID)
    }

    def createTokenForAuthenticationRequest(id){
        return new TokenForAuthenticationRequest().with {
            it.id = id
            return it
        }
    }

    def createToken(String id) {
        new Token().with {
            it.id = id ? id : ID
            it.expires = DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar())
            it.tenant = new TenantForAuthenticateResponse()
            return it
        }
    }

    def createUser() {
        return createUser(ID, USERNAME)
    }

    def createUser(String id, String username) {
        new User().with {
            it.id = id
            it.username = username
            it.enabled = true
            return it
        }
    }

    def createUserForCreate(String username, String displayName, String email, Boolean enabled, String defaultRegion,
                            String domainId, String password, List<String> roleNames, List<String> groupNames, String secretQuestion, String secretAnswer ) {
        new User().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            it.defaultRegion = defaultRegion
            it.domainId = domainId
            it.password = password;
            if (roleNames != null) {
                def list = []
                for (roleName in roleNames) {
                   list.add(createRole(roleName))
                }

                it.roles = createRoleList(list.asList())
            }

            if (groupNames != null) {
                def list = []
                for (groupName in groupNames) {
                    list.add(createGroup(groupName))
                }

                it.groups = createGroups(list.asList())
            }

            if (secretQuestion != null || secretAnswer != null) {
                it.secretQA = createSecretQA(secretQuestion, secretAnswer)
            }

            return it
        }
    }

    def createUser(String username, String displayName, String email, Boolean enabled, String defaultRegion, String domainId, String password) {
        new User().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            it.defaultRegion = defaultRegion
            it.domainId = domainId
            it.password = password;

            return it
        }
    }

    def createUserForCreate(String username, String displayName, String email, Boolean enabled, String defaultRegion, String domainId, String password) {
        new User().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            it.defaultRegion = defaultRegion
            it.domainId = domainId
            it.password = password;

            return it
        }
    }

    def userForCreate(String username, String displayName, String email, Boolean enabled, String defaultRegion, String domainId, String password) {
        new UserForCreate().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            it.password = (password != null) ? password : null
            it.defaultRegion = (defaultRegion != null) ? defaultRegion : null
            it.domainId = (domainId != null) ? domainId : null
            return it
        }
    }

    def createUserForUpdate(String id, String username, String displayName, String email, Boolean enabled, String defaultRegion, String password) {
        new User().with {
            it.id = id
            it.username = username
            it.email = email
            it.enabled = enabled
            it.displayName = displayName
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
            it.defaultRegion = defaultRegion
            return it
        }
    }

    def createUserForAuthenticateResponse() {
        return createUserForAuthenticateResponse(ID, NAME, null)
    }

    def createUserForAuthenticateResponse(String id, String name, RoleList roleList) {
        def RoleList list = roleList ? roleList : new RoleList()
        new UserForAuthenticateResponse().with {
            it.id = id
            it.name = name
            it.roles = list
            return it
        }
    }

    def createUserList() {
        return createUserList(null)
    }

    def createUserList(List<User> userList) {
        def list = userList ? userList : [].asList()
        new UserList().with {
            it.getUser().addAll(list)
            return it
        }
    }

    def createVersionForService(){
        return createVersionForService(1,"info","list")
    }

    def createVersionForService(int id, String info, String list){
        new VersionForService().with {
            it.id = id
            it.info = info
            it.list = list
            return it
        }
    }

    def createMobilePhone(String telephoneNumber = PhoneNumberGenerator.randomUSNumberAsString()) {
        new MobilePhone().with {
            it.number = telephoneNumber
            return it
        }
    }

    def createMobilePhoneWithId(String id = Cloud20Utils.createRandomString(), String telephoneNumber = PhoneNumberGenerator.randomUSNumberAsString()) {
        createMobilePhone(telephoneNumber).with {
            it.id = id
            return it
        }
    }

    def createVerificationCode(String verificationCode) {
        new VerificationCode().with {
            it.code = verificationCode
            return it
        }
    }

    def createMultiFactorSettings(Boolean enabled = true, Boolean unlock = null) {
        new MultiFactor().with {
            it.enabled = enabled
            it.unlock = unlock
            return it
        }
    }

    def createMultiFactorDomainSettings(domainMfaEnforcementLevel = DomainMultiFactorEnforcementLevelEnum.OPTIONAL) {
        new MultiFactorDomain().with {
            it.domainMultiFactorEnforcementLevel = domainMfaEnforcementLevel
            return it
        }
    }

    def createSecretQA(String secretQuestion = Constants.DEFAULT_RAX_KSQA_SECRET_QUESTION, String secretAnswer = Constants.DEFAULT_RAX_KSQA_SECRET_ANWSER) {
        new SecretQA().with {
            it.question = secretQuestion
            it.answer = secretAnswer
            return it
        }
    }

    def createBypassCode(seconds, numberOfCodes = null) {
        new BypassCodes().with {
            it.validityDuration = DatatypeFactory.newInstance().newDuration(seconds * 1000)
            it.numberOfCodes = numberOfCodes
            return it
        }
    }

    def createDomain(id, name) {
        new Domain().with {
            it.id = id
            it.name = name
            it
        }
    }

    def createIdentityProvider(description, issuerUri, federationType) {
        new IdentityProvider().with {
            it.description = description
            it.issuer = issuerUri
            it.authenticationUrl = "https://log.me.in"
            it.federationType = federationType
            it.publicCertificates
            if (federationType == IdentityProviderFederationTypeEnum.DOMAIN) {
                it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL
            }
            it
        }
    }

    def createIdentityProvider(description, issuerUri, federationType, approvedDomainGroup, List<String> approvedDomainIdsList) {
        new IdentityProvider().with {
            it.description = description
            it.issuer = issuerUri
            it.authenticationUrl = "http://random.url"
            it.federationType = federationType
            it.publicCertificates
            it.approvedDomainGroup = approvedDomainGroup
            if (approvedDomainIdsList != null) {
                ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
                approvedDomainIds.getApprovedDomainId().addAll(approvedDomainIdsList)
                it.approvedDomainIds = approvedDomainIds
            }
            it
        }
    }

    def createPublicCertificate(pemEncodedCert) {
        new PublicCertificate().with {
            it.pemEncoded = pemEncodedCert
            it
        }
    }

    def createPublicCertificates(PublicCertificate... certificates) {
        new PublicCertificates().with {
            it.publicCertificate.addAll(certificates)
            it
        }
    }

    def createForgotPasswordCredentials(String username, String portal) {
        new ForgotPasswordCredentials().with {
            it.username = username
            it.portal = portal
            it
        }
    }

    def createPasswordReset(String password) {
        new PasswordReset().with {
            it.password = password
            it
        }
    }

    def createTenantTypeEndpointRule(String tenantType = "tenantType", List<String> endpointTemplateIds = [], String description = "description") {
        new TenantTypeEndpointRule().with {
            it.tenantType = tenantType
            it.description = description

            EndpointTemplateList endpointTemplateList = new EndpointTemplateList()

            for (String endpointTemplateId : endpointTemplateIds) {
                EndpointTemplate endpointTemplate = new EndpointTemplate().with {
                    it.id = endpointTemplateId as int
                    it
                }
                endpointTemplateList.endpointTemplate.add(endpointTemplate)
            }

            it.setEndpointTemplates(endpointTemplateList)
            it
        }
    }

}
