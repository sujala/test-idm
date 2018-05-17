package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspacecloud.docs.auth.api.v1.BaseURL
import com.unboundid.ldap.sdk.ReadOnlyEntry
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.opensaml.security.x509.X509Credential
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import spock.lang.Specification
import testHelpers.saml.SamlCredentialUtils

import java.security.cert.X509Certificate

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/23/12
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
class EntityFactory extends Specification {

    private static ID = "id"
    private static CLIENT = "clientId"
    private static NAME = "name"
    private static PASSWORD = "Password1"
    private static USERNAME = "username"
    private static SALT = "a1 b1"
    private static VERSION = "0"

    private static objFactories = new JAXBObjectFactories()

    def createApplication() {
        return createApplication(CLIENT, NAME)
    }

    def createApplication(String clientId, String name) {
        def id = clientId ? clientId : CLIENT
        new Application().with {
            it.uniqueId = "clientId=$id,ou=applications,o=rackspace,dc=rackspace,dc=com"
            it.clientId = clientId
            it.name = name
            it.enabled = true
            return it
        }
    }

    def createAuthCredentials() {
        return createAuthCredentials(CLIENT, "clientSecret", USERNAME, PASSWORD, null)
    }

    def createAuthCredentials(String clientId, String clientSecret, String username, String password, String grantType) {
        new AuthCredentials().with {
            it.clientId = clientId
            it.clientSecret = clientSecret
            it.password = password
            it.username = username
            it.grantType = "password"
            it.OAuthGrantType = OAuthGrantType.PASSWORD
            return it
        }
    }

    def createUserAuthenticationResult() {
        return createUserAuthenticationResult(createUser(), true)
    }

    def createUserAuthenticationResult(BaseUser user, boolean authenticated) {
        new UserAuthenticationResult(user, authenticated).with {
            return it
        }
    }

    def createClientAuthenticationResult() {
        return createClientAuthenticationResult(createApplication(), true)
    }

    def createClientAuthenticationResult(Application application, boolean isAuthenticated) {
        new ClientAuthenticationResult().with {
            it.client = application
            it.authenticated = isAuthenticated
            return it
        }
    }

    def createClientRole(int weight) {
        return createClientRole(NAME, weight)
    }

    def createClientRole() {
        return createClientRole(NAME)
    }

    def createClientRole(String name) {
        return createClientRole(name, 500)
    }

    def createClientRole(String name, int weight) {
        new ClientRole().with {
            it.id = ID
            it.name = name ? name : NAME
            it.rsWeight = weight
            return it
        }
    }

    def createBaseUrl(String serviceName) {
        new BaseURL().with {
            it.serviceName = serviceName
            return it
        }
    }

    def createCloudBaseUrl() {
        return createCloudBaseUrl("CloudServersOpenStack", "region")
    }

    def createCloudBaseUrl(String baseUrlId, Boolean deff) {
        new CloudBaseUrl().with( {
            it.baseUrlId = baseUrlId
            it.setDef(deff)
            return it
        })
    }

    def createCloudBaseUrl(String openstackType, String region) {
        new CloudBaseUrl().with {
            it.v1Default = true
            it.enabled = true
            it.openstackType = openstackType
            it.region = region
            return it
        }
    }

    def createCloudEndpoint() {
        return createCloudEndpoint(1, "nastId")
    }

    def createCloudEndpoint(Integer mossoId, String nastId) {
        new CloudEndpoint().with {
            it.mossoId = mossoId ? mossoId : 1
            it.nastId = nastId ? nastId : "nastId"
            it.baseUrl = createCloudBaseUrl()
            return it
        }
    }

    def createEndpointTemplate(String name) {
        new EndpointTemplate().with {
            it.name = name
            return it
        }
    }

    def createDomain() {
        return createDomain("domainId")
    }

    def createDomain(String domainId) {
        new Domain().with {
            it.name = domainId
            it.domainId = domainId
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

    def createDelegationAgreement(String domainId = RandomStringUtils.randomAlphanumeric(8)) {
        new DelegationAgreement().with {
            it.domainId = domainId
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it
        }
    }

    def createOpenstackEndpoint() {
        return createOpenstackEndpoint("tenantId", "tenantName")
    }

    def createOpenstackEndpoint(String tenantId, String tenantName) {
        new OpenstackEndpoint().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.baseUrls = [].asList()
            return it
        }
    }

    def createPattern(String name, String regex) {
        new Pattern().with {
            it.name = name
            it.regex = regex
            return it
        }
    }

    def createQuestion() {
        return createQuestion(ID, "question")
    }

    def createQuestion(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def createJAXBQuestion() {
        return createJAXBQuestion(ID, "question")
    }

    def createJAXBQuestion(String id, String question) {
        objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestion().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def createRacker() {
        return createRacker("rackerId")
    }

    def createRacker(String rackerId) {
        new Racker().with {
            it.rackerId = rackerId
            it.uniqueId = "rackerId=$rackerId,cn=rackers,ou=rackspace"
            return it
        }
    }

    def createRegion() {
        return createRegion(NAME, "cloud")
    }

    def createRegion(String name, String cloud) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isDefault = true
            it.isEnabled = true
            return it
        }
    }

    def createSecretQA() {
        return createSecretQA(ID, "question", "answer")
    }

    def createSecretQA(String id, String question, String answer) {
        new SecretQA().with {
            it.id = id
            it.question = question
            it.answer = answer
            return it
        }
    }

    def createSecretQAs() {
        return createSecretQAs(null)
    }

    def createSecretQAs(List<SecretQA> qaList) {
        def list = qaList ? qaList : [].asList()
        new SecretQAs().with {
            it.getSecretqa().addAll(list)
            return it
        }
    }

    def createTenant() {
        return createTenant(ID, NAME)
    }

    def createTenant(String id, String name) {
        new Tenant().with {
            it.tenantId = id
            it.name = name
            it.enabled = true
            return it
        }
    }

    def createTenantRole() {
        createTenantRole(NAME)
    }

    def createTenantRole(String name) {
        createTenantRole(name, RoleTypeEnum.STANDARD)
    }

    def createPhonePin() {
        return new PhonePin().with {
            it.pin = "1234"
            return it
        }
    }

    def createTenantRoleForGroup(String groupId = Cloud20Utils.createRandomString(), String roleId = Cloud20Utils.createRandomString()) {
        new TenantRole().with {
            it.name = Cloud20Utils.createRandomString()
            it.roleType = RoleTypeEnum.STANDARD
            it.tenantIds = []
            it.roleRsId = roleId
            it.clientId = CLIENT
            it.uniqueId = String.format("roleRsId=%s,cn=ROLES,rsId=%s,ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com", roleId, groupId)
            return it
        }
    }

    def createTenantRoleForUser(String userId = Cloud20Utils.createRandomString(), String roleId = Cloud20Utils.createRandomString()) {
        new TenantRole().with {
            it.name = Cloud20Utils.createRandomString()
            it.roleType = RoleTypeEnum.STANDARD
            it.tenantIds = []
            it.userId = userId
            it.roleRsId = roleId
            it.clientId = CLIENT
            it.uniqueId = String.format("roleRsId=%s,cn=ROLES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", roleId, userId)
            return it
        }
    }

    def createTenantRole(String name, RoleTypeEnum roleType) {
        createTenantRoleForUser("1", "1").with {
            it.name = name
            it.roleType = roleType
            it
        }
    }

    def createUser() {
        return createUser("username", "id", "domainId", "region")
    }

    def createRandomUser(username = Cloud20Utils.createRandomString(), String id = Cloud20Utils.createRandomString()) {
        return createUser(username, id, "domainId", "region").with {it.uniqueId = null; return it}
    }

    def createFederatedToken(FederatedUser user, String tokenStr=Cloud20Utils.createRandomString(), Date expiration = new Date())   {
        new UserScopeAccess().with {
            it.userRsId = user.id
            it.accessTokenString = tokenStr
            it.accessTokenExp = expiration
            it.clientId = "fakeClientId"
            it.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
            return it
        }
    }

    def createFederatedUser(String username=Cloud20Utils.createRandomString(), String federatedIdpUri = Constants.DEFAULT_IDP_URI) {
        new FederatedUser().with {
            it.id = Cloud20Utils.createRandomString()
            it.username = username
            it.domainId = "983452"
            it.region="ORD"
            it.email="test@rackspace.com"
            it.federatedIdpUri = federatedIdpUri
            return it
        }
    }

    def createUser(String username, String userId, String domainId, String region) {
        def id = userId ? userId : Cloud20Utils.createRandomString()
        def dn = "rsId=$id,ou=users,o=rackspace"
        new User().with {
            it.username = username
            it.id = id
            it.domainId = domainId
            it.region = region
            it.readOnlyEntry = new ReadOnlyEntry(dn)
            it.uniqueId = dn
            it.enabled = true
            return it
        }
    }

    def createGroup(String groupId, String name, String description) {
        return new Group().with {
            it.groupId = groupId
            it.name = name
            it.description = description
            return it
        }

    }

    def createGroups() {
        return new ArrayList<Group>()
    }

    def createClientSecret() {
        return createClientSecret("secret")
    }

    def createClientSecret(secret) {
        return ClientSecret.newInstance(secret)
    }

    def createScopeAccess() {
        new ScopeAccess().with {
            it.accessTokenString = "ats"
            return it
        }
    }

    def createRackerScopeAccess() {
        new RackerScopeAccess().with {
            it.accessTokenString = "ats"
            return it
        }
    }

    def createFederatedToken() {
        new UserScopeAccess().with {
            it.accessTokenString = "ats"
            it.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
            return it
        }
    }

    def createUserToken(String userRsId = UUID.randomUUID().toString(), String tokenString =  UUID.randomUUID().toString(), String clientId = UUID.randomUUID().toString(), Date expiration = new DateTime().plusDays(1).toDate(), List<String> authBy = [GlobalConstants.AUTHENTICATED_BY_PASSWORD]) {
        def dn = "acessToken=$tokenString,cn=TOKENS,rsId=$userRsId,ou=users"

        new UserScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.userRsId = userRsId
            it.clientId = clientId
            it.setUniqueId(dn)
            it.getAuthenticatedBy().addAll(authBy)
            return it
        }
    }

    def createMobilePhone(String telephoneNumber = PhoneNumberGenerator.randomUSNumberAsString()) {
        new MobilePhone().with {
            it.telephoneNumber = telephoneNumber
            it.cn = telephoneNumber
            return it
        }
    }

    def createMobilePhoneWithId(String id = Cloud20Utils.createRandomString(), String telephoneNumber = PhoneNumberGenerator.randomUSNumberAsString()) {
        createMobilePhone(telephoneNumber).with {
            it.id = id
            return it
        }
    }

    def createIdentityProviderWithoutCertificate() {
        IdentityProvider provider = new IdentityProvider().with {
            it.providerId = RandomStringUtils.randomAlphabetic(10)
            it.name = RandomStringUtils.randomAlphabetic(10)
            it.uri = it.providerId
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL.getStoredVal()
            return it
        }
        return provider
    }

    def createIdentityProviderWithCredential(X509Credential credential = SamlCredentialUtils.generateX509Credential()) {
        return createIdentityProviderWithCertificates([credential.entityCertificate])
    }

    def createIdentityProviderWithCertificates(List<X509Certificate> certificates = [SamlCredentialUtils.generateX509Credential().entityCertificate]) {
        if (CollectionUtils.isEmpty(certificates)) {
            return createIdentityProviderWithoutCertificate()
        }

        IdentityProvider provider = createIdentityProviderWithoutCertificate().with {
            for (X509Certificate certificate : certificates) {
                it.addUserCertificate(certificate)
            }
            return it
        }

        return provider
    }


}
