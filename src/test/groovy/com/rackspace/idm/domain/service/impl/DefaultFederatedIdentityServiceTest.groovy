package com.rackspace.idm.domain.service.impl
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.FederatedToken
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.util.SamlResponseValidator
import com.rackspace.idm.util.SamlUnmarshaller
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.opensaml.DefaultBootstrap
import spock.lang.Shared
import spock.lang.Specification

class DefaultFederatedIdentityServiceTest extends Specification {
    @Shared DefaultFederatedIdentityService service

    @Shared def IDP_NAME = "dedicated";
    @Shared def IDP_URI = "http://my.test.idp"
    @Shared def USERNAME = "john.doe"
    @Shared def UUID = "729238492sklff293824923423423"
    @Shared def DOMAIN_ID = "1234"

    @Shared def samlStr
    @Shared def samlResponse
    @Shared def user
    @Shared def endpoints
    @Shared def roles
    @Shared def tenants;

    @Shared def mockSamlResponseValidator
    @Shared def mockConfig
    @Shared def mockScopeAccessService
    @Shared def mockTenantService
    @Shared def mockIdentityProviderDao
    @Shared def mockFederatedUserDao
    @Shared def mockRoleDao
    @Shared def mockDomainService

    def setupSpec(){
        DefaultBootstrap.bootstrap()

        service = new DefaultFederatedIdentityService()

        samlStr = "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"bc1c335f-8078-4769-81a1-bb519194279c\" IssueInstant=\"2013-10-01T15:02:42.110Z\" Version=\"2.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + IDP_URI + "</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
                "         <ds:Reference URI=\"#bc1c335f-8078-4769-81a1-bb519194279c\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\"/>\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
                "            <ds:DigestValue>MNSPZoA7K27Mv6oIfePTrMS+W4Y=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>LmBStQQ5Xzh/Irlk4/6y123e6xTgvK1xvygCku4qpKoIEgd5vjTVkH7q6ol49Fqe1DcfJ6tYTrmAq9UL+7meGg==</ds:SignatureValue>\n" +
                "   </ds:Signature>\n" +
                "   <saml2p:Status>\n" +
                "      <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
                "   </saml2p:Status>\n" +
                "   <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"461e5f27-3880-4ef3-a51f-8265ef13b7e6\" IssueInstant=\"2013-10-01T15:02:42.107Z\" Version=\"2.0\">\n" +
                "      <saml2:Issuer>" + IDP_URI + "</saml2:Issuer>\n" +
                "      <saml2:Subject>\n" +
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">" + USERNAME + "</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2013-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">observer</saml2:AttributeValue>\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">identity:user-admin</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + DOMAIN_ID + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"
    }

    def setup() {
        mockSamlResponseValidator(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockIdentityProviderDao(service)
        mockFederatedUserDao(service)
        mockRoleDao(service)
        mockConfig(service)
        mockDomainService(service)

        user = new User();
        endpoints = [].toList()
        roles = [].toList()
        tenants = [createTenant("tenantId"), createTenant("nastTenantId")].toList()
    }

    def "Generate authentication info from saml response when user has not been lazy provisioned"() {
        given:
        samlResponse = new SamlUnmarshaller().unmarshallResponse(samlStr)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider(IDP_NAME,IDP_URI)
        mockFederatedUserDao.getUserByUsername(USERNAME, IDP_NAME) >> null
        mockTenantService.getTenantsByDomainId(DOMAIN_ID) >> tenants
        mockScopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> endpoints
        mockTenantService.getTenantRolesForFederatedToken(_) >> roles
        mockTenantService.getTenantRolesForUser(_) >> roles
        mockDomainService.getDomainAdmins(_) >> [Mock(User)].asList()

        when:
        def authInfo = service.generateAuthenticationInfo(samlResponse)

        then:
        1 * mockSamlResponseValidator.validate(_)
        1 * mockScopeAccessService.deleteExpiredTokens(_)
        1 * mockFederatedUserDao.addUser(_, IDP_NAME)
        1 * mockScopeAccessService.addUserScopeAccess(_, _)
        1 * mockTenantService.addTenantRolesToFederatedToken(_,_)
        authInfo.token != null
        authInfo.token.roles == roles
        authInfo.endpoints == endpoints
        authInfo.user != user
        authInfo.user.username == USERNAME
        authInfo.user.federated == true
        authInfo.user.federatedIdp == IDP_URI
        authInfo.user.domainId == DOMAIN_ID
        authInfo.user.roles == roles
    }

    def "Generate authentication info from saml response when user has been lazy provisioned"() {
        given:
        samlResponse = new SamlUnmarshaller().unmarshallResponse(samlStr)

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> createIdentityProvider(IDP_NAME,IDP_URI)
        mockFederatedUserDao.getUserByUsername(USERNAME, IDP_NAME) >> user
        mockTenantService.getTenantsByDomainId(DOMAIN_ID) >> tenants
        mockScopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> endpoints
        mockTenantService.getTenantRolesForFederatedToken(_) >> roles
        mockTenantService.getTenantRolesForUser(_) >> roles
        mockDomainService.getDomainAdmins(_) >> [Mock(User)].asList()

        when:
        def authInfo = service.generateAuthenticationInfo(samlResponse)

        then:
        1 * mockSamlResponseValidator.validate(_)
        1 * mockScopeAccessService.deleteExpiredTokens(user)
        1 * mockScopeAccessService.addUserScopeAccess(user, _)
        1 * mockTenantService.addTenantRolesToFederatedToken(_,_)
        authInfo.token != null
        authInfo.token.roles == roles
        authInfo.endpoints == endpoints
        authInfo.user == user
        authInfo.user.federated == true
        authInfo.user.federatedIdp == IDP_URI
        authInfo.user.roles == roles
    }

    def "Generate authentication info throws saml response validation exception"() {
        given:
        samlResponse = new SamlUnmarshaller().unmarshallResponse(samlStr)

        and:
        mockSamlResponseValidator.validate(_) >> { throw new Exception() }

        when:
        def authInfo = service.generateAuthenticationInfo(samlResponse)

        then:
        thrown(Exception)
    }

    def "Get authentication info for federated token"() {
        given:
        def federatedToken = createFederatedToken(UUID, IDP_NAME, USERNAME)

        and:
        mockIdentityProviderDao.getIdentityProviderByName(IDP_NAME) >> createIdentityProvider(IDP_NAME,IDP_URI)
        mockFederatedUserDao.getUserByUsername(USERNAME, IDP_NAME) >> user
        mockTenantService.getTenantRolesForFederatedToken(federatedToken) >> roles

        when:
        def authInfo = service.getAuthenticationInfo(federatedToken)

        then:
        authInfo.token.accessTokenString == UUID
        authInfo.token.accessTokenExp != null
        authInfo.endpoints == null
        authInfo.user == user
        authInfo.user.federated == true
        authInfo.user.federatedIdp == IDP_URI
        authInfo.user.roles == roles
    }

    def createIdentityProvider(String name, String uri) {
        new IdentityProvider().with {
            it.name = name
            it.uri = uri
            return it
        }
    }

    def createFederatedToken(String tokenString, String idpName, String username) {
        new FederatedToken().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = new DateTime().toDate()
            it.idpName = idpName
            it.username = username

            return it
        }
    }

    def createClientRole(String id, String name) {
        new ClientRole().with {
            it.id = id
            it.name = name
            it.clientId = "2398293842342"
            return it
        }
    }

    def createTenant(String id) {
        new Tenant().with {
            it.tenantId
            return it
        }
    }

    def mockSamlResponseValidator(service) {
        mockSamlResponseValidator = Mock(SamlResponseValidator)
        service.samlResponseValidator = mockSamlResponseValidator
    }

    def mockScopeAccessService(service) {
        mockScopeAccessService = Mock(ScopeAccessService)
        service.scopeAccessService = mockScopeAccessService
    }

    def mockTenantService(service) {
        mockTenantService = Mock(TenantService)
        service.tenantService = mockTenantService
    }

    def mockIdentityProviderDao(service) {
        mockIdentityProviderDao = Mock(IdentityProviderDao)
        service.identityProviderDao = mockIdentityProviderDao
    }

    def mockFederatedUserDao(service) {
        mockFederatedUserDao = Mock(FederatedUserDao)
        service.federatedUserDao = mockFederatedUserDao
    }

    def mockRoleDao(service) {
        mockRoleDao = Mock(ApplicationRoleDao)
        mockRoleDao.getRoleByName(_) >> createClientRole("50", "roleName")
        service.roleDao = mockRoleDao
    }

    def mockDomainService(service) {
        mockDomainService = Mock(DomainService)
        service.domainService = mockDomainService
    }

    def mockConfig(service) {
        mockConfig = Mock(Configuration)
        mockConfig.getInt("token.cloudAuthExpirationSeconds") >> 2000
        mockConfig.getString("cloudAuth.clientId") >> "clientId"
        service.config = mockConfig
    }
}
