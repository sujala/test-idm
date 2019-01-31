package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedUserRequest
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.decorator.SamlResponseDecorator
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.SamlAuthResponse
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateUsernameException
import com.rackspace.idm.util.DateHelper
import com.rackspace.idm.util.SamlSignatureValidator
import com.rackspace.idm.util.SamlUnmarshaller
import com.rackspace.idm.validation.PrecedenceValidator
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.opensaml.core.config.InitializationService
import org.opensaml.saml.saml2.core.Response
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory
import testHelpers.IdmExceptionAssert
import testHelpers.saml.SamlFactory

import static com.rackspace.idm.ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

class ProvisionedUserSourceFederationHandlerTest extends Specification {
    @Shared ProvisionedUserSourceFederationHandler provisionedUserSourceFederationHandler

    @Shared def IDP_ID = "dedicated";
    @Shared def IDP_URI = "http://my.test.idp"
    @Shared def IDP_PUBLIC_CERTIFICATE = "--BEGIN CERTIFICATE-- bla bla bla --END CERTIFICATE--"
    @Shared def ROLE_NAME = "rbacRole1"
    @Shared def DOMAIN = "1234"
    @Shared def USERNAME = "john.doe"
    @Shared def UUID = "729238492sklff293824923423423"
    @Shared def DOMAIN_ID = "1234"
    @Shared def EMAIL="federated-noreply@rackspace.com"

    @Shared def samlUnmarshaller
    @Shared def samlStr
    @Shared Response samlResponse
    @Shared def samlResponseDecorator
    @Shared def user

    @Shared IdentityProviderDao mockIdentityProviderDao
    @Shared def mockRoleService
    @Shared def mockSamlSignatureValidator
    @Shared def mockDomainDao
    @Shared def mockPrecedenceValidator
    @Shared def mockConfig
    @Shared def mockDomainService
    @Shared def domainAdmin
    @Shared def mockIdentityUserService
    @Shared FederatedUserDao mockFederatedUserDao
    @Shared def mockRoleDao
    @Shared def mockDateHelper

    @Shared def mockScopeAccessService
    @Shared def mockTenantService
    @Shared def endpoints
    @Shared def roles
    @Shared def tenants
    @Shared def theIdentityProvider
    @Shared def mockAuthenticationContext
    @Shared AtomHopperClient mockAtomHopperClient

    def mockAuthorizationService = Mock(AuthorizationService)

    @Shared EntityFactory entityFactory = new EntityFactory()
    @Shared ClientRole dummyRbacRole = entityFactory.createClientRole(ROLE_NAME, PrecedenceValidator.RBAC_ROLES_WEIGHT)

    def FOUNDATION_CLIENT_ID = "asdjwehuqrew"
    def CLOUD_AUTH_CLIENT_ID = "345hjkwetugfhj5346hiou"

    IdentityConfig identityConfig = Mock(IdentityConfig)
    IdentityConfig.StaticConfig staticConfig = Mock(IdentityConfig.StaticConfig)
    IdentityConfig.ReloadableConfig reloadableConfig = Mock(IdentityConfig.ReloadableConfig)

    def SamlFactory samlAssertionFactory = new SamlFactory()

    def setupSpec() {
        //initializes open saml. allows us use unmarshaller
        InitializationService.initialize();

        provisionedUserSourceFederationHandler = new ProvisionedUserSourceFederationHandler();
    }

    def setup() {
        samlUnmarshaller = new SamlUnmarshaller()
        samlUnmarshaller.identityConfig = identityConfig

        identityConfig.getReloadableConfig() >> reloadableConfig
        identityConfig.getStaticConfig() >> staticConfig

        staticConfig.getFoundationClientId() >> FOUNDATION_CLIENT_ID
        staticConfig.getCloudAuthClientId() >> CLOUD_AUTH_CLIENT_ID

        provisionedUserSourceFederationHandler.identityConfig = identityConfig
        provisionedUserSourceFederationHandler.authorizationService = mockAuthorizationService

        mockIdentityProviderDao(provisionedUserSourceFederationHandler)
        mockRoleService(provisionedUserSourceFederationHandler)
        mockSamlSignatureValidator(provisionedUserSourceFederationHandler)
        mockDomainDao(provisionedUserSourceFederationHandler)
        mockPrecedenceValidator(provisionedUserSourceFederationHandler)
        mockConfig(provisionedUserSourceFederationHandler)
        mockDomainService(provisionedUserSourceFederationHandler)
        mockIdentityUserService(provisionedUserSourceFederationHandler)
        mockFederatedUserDao(provisionedUserSourceFederationHandler)
        mockRoleDao(provisionedUserSourceFederationHandler)
        mockScopeAccessService(provisionedUserSourceFederationHandler)
        mockTenantService(provisionedUserSourceFederationHandler)
        mockDateHelper(provisionedUserSourceFederationHandler)
        mockDomainDao.getDomain(DOMAIN) >> createDomain()
        mockAuthenticationContext = Mock(AuthenticationContext)
        provisionedUserSourceFederationHandler.authenticationContext = mockAuthenticationContext
        mockAtomHopperClient = Mock(AtomHopperClient)
        provisionedUserSourceFederationHandler.atomHopperClient = mockAtomHopperClient

        reloadableConfig.getFederatedDomainTokenLifetimeMax() >> IdentityConfig.FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME_DEFAULT

        domainAdmin = new User().with {
            it.enabled = true
            it.domainId = DOMAIN_ID
            it

        }
        theIdentityProvider = new IdentityProvider().with {
            it.providerId = IDP_ID
            it.uri = IDP_URI
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL.storedVal
            it.federationType = IdentityProviderFederationTypeEnum.DOMAIN.name()
            return it
        }

        user = new FederatedUser().with{
            it.username = USERNAME
            it.federatedIdpUri = IDP_URI
            it.domainId = DOMAIN_ID
            it.email = EMAIL
            return it
        }

        endpoints = [].toList()
        roles = [].toList()
        tenants = [createTenant("tenantId"), createTenant("nastTenantId")].toList()
        def expireTime = new DateTime().plusHours(12)
        def expireTimeString = DateTimeFormat.forPattern("yyyy-MM-dd'T'H:m:s.SZ").print(expireTime)

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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"" + expireTimeString + "\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + DOMAIN + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"email\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + Constants.DEFAULT_FED_EMAIL + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"
    }

    def "processRequestForProvider - nulls throw IllegalArguments"() {
        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(null, new IdentityProvider())

        then:
        thrown(IllegalArgumentException)

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(Mock(SamlResponseDecorator), null)

        then:
        thrown(IllegalArgumentException)
    }


    def "parseSaml - no racker throws error"() {
        given:
        def samlResponse = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, null, 1)
        SamlResponseDecorator decoratedSaml = new SamlResponseDecorator(samlResponse)

        when:
        FederatedUserRequest request = provisionedUserSourceFederationHandler.parseAndValidateSaml(decoratedSaml, theIdentityProvider)

        then:
        thrown(BadRequestException)
    }

    def "parseSaml - request date in past throws error"() {
        given:
        def samlResponse = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, USERNAME, -1)
        SamlResponseDecorator decoratedSaml = new SamlResponseDecorator(samlResponse)

        when:
        FederatedUserRequest request = provisionedUserSourceFederationHandler.parseAndValidateSaml(decoratedSaml, theIdentityProvider)

        then:
        thrown(BadRequestException)
    }

    def "validate correct saml response" (){
        given:
        reloadableConfig.getIdentityFederationMaxUserCountPerDomainForIdp(_) >> 1000
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        mockDomainService.getDomainAdmins(_) >> [domainAdmin].asList()
        mockIdentityUserService.getFederatedUsersByDomainIdAndIdentityProviderIdCount(_, _) >> 0
        mockFederatedUserDao.getUserByUsernameForIdentityProviderId(_, _) >> null
        def idp = createIdentityProvider()
        mockTenantService.getTenantsByDomainId(_, Boolean.toString(true)) >> tenants
        mockScopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> endpoints
        mockTenantService.getTenantRolesForUser(_) >> roles
        mockDomainService.getDomainAdmins(_) >> [new User()].asList()
        mockAuthorizationService.restrictUserAuthentication(_, _) >> false

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp
        mockRoleService.getRoleByName(ROLE_NAME) >> dummyRbacRole
        mockDomainDao.getDomain(DOMAIN) >> createDomain()
        mockScopeAccessService.getServiceCatalogInfo(_) >> new ServiceCatalogInfo(roles, tenants, endpoints, IdentityUserTypeEnum.DEFAULT_USER)

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        noExceptionThrown()
    }

    def "validate saml response when subject is not specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getSubject().setNameID(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        def idp = createIdentityProvider()

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when subject confirmation NotOnOrAfter is not specified" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().setNotOnOrAfter(null)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        def idp = createIdentityProvider()

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when subject confirmation NotOnOrAfter is in the past" (){
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponse.getAssertions().get(0).getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().setNotOnOrAfter(new DateTime().minusDays(2))
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        def idp = createIdentityProvider()

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when domain attribute is not specified" (){
        given:

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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)

        and:
        def idp = createIdentityProvider()
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp
        mockRoleService.getRoleByName(ROLE_NAME) >> Mock(ClientRole)

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when domain attribute is specified more than once" (){
        given:
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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">5678</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"email\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + Constants.DEFAULT_FED_EMAIL + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        def idp = createIdentityProvider()

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp
        mockRoleService.getRoleByName(ROLE_NAME) >> Mock(ClientRole)

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when domain does not exist" (){
        given:
        mockDomainDao(provisionedUserSourceFederationHandler)
        mockDomainDao.getDomain(DOMAIN) >> null

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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"email\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + Constants.DEFAULT_FED_EMAIL + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        def idp = createIdentityProvider()
        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp
        mockRoleService.getRoleByName(ROLE_NAME) >> Mock(ClientRole)

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response is accepted when roles attribute is not specified" (){
        given:
        def expireTime = new DateTime().plusHours(12)
        def expireTimeString = DateTimeFormat.forPattern("yyyy-MM-dd'T'H:m:s.SZ").print(expireTime)

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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"" + expireTimeString + "\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"email\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + Constants.DEFAULT_FED_EMAIL + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        reloadableConfig.getIdentityFederationMaxUserCountPerDomainForIdp(_) >> 1000
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        mockDomainService.getDomainAdmins(_) >> [domainAdmin].asList()
        mockIdentityUserService.getFederatedUsersByDomainIdAndIdentityProviderIdCount(_, _) >> 0
        mockFederatedUserDao.getUserByUsernameForIdentityProviderId(_, _) >> null
        def idp = createIdentityProvider()
        mockTenantService.getTenantsByDomainId(_, Boolean.toString(true)) >> tenants
        mockScopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> endpoints
        mockTenantService.getTenantRolesForUser(_) >> roles
        mockDomainService.getDomainAdmins(_) >> [new User()].asList()
        mockScopeAccessService.getServiceCatalogInfo(_) >> new ServiceCatalogInfo(roles, tenants, endpoints, IdentityUserTypeEnum.DEFAULT_USER)
        mockAuthorizationService.restrictUserAuthentication(_, _) >> false

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        noExceptionThrown()

        and: "created user feed event is sent"
        1 * mockAtomHopperClient.asyncPost(_, FeedsUserStatusEnum.CREATE, _)
    }

    def "validate saml response when role does not exist" (){
        given:

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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"email\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + Constants.DEFAULT_FED_EMAIL + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        mockDomainService.getDomainAdmins(_) >> [domainAdmin].asList()
        mockIdentityUserService.getFederatedUsersByDomainIdAndIdentityProviderId(_, _) >> [].asList()
        def idp = createIdentityProvider()

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp
        mockRoleService.getRoleByName(ROLE_NAME) >> null

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "validate saml response when same role specified more than once" (){
        given:

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
                "         <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">john.doe</saml2:NameID>\n" +
                "         <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "            <saml2:SubjectConfirmationData NotOnOrAfter=\"2020-10-01T17:02:42.101Z\"/>\n" +
                "         </saml2:SubjectConfirmation>\n" +
                "      </saml2:Subject>\n" +
                "      <saml2:AuthnStatement AuthnInstant=\"2013-10-01T15:02:42.103Z\">\n" +
                "         <saml2:AuthnContext>\n" +
                "            <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>\n" +
                "         </saml2:AuthnContext>\n" +
                "      </saml2:AuthnStatement>\n" +
                "      <saml2:AttributeStatement>\n" +
                "         <saml2:Attribute Name=\"domain\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">1234</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"roles\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + ROLE_NAME + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "         <saml2:Attribute Name=\"email\">\n" +
                "            <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" + Constants.DEFAULT_FED_EMAIL + "</saml2:AttributeValue>\n" +
                "         </saml2:Attribute>\n" +
                "      </saml2:AttributeStatement>\n" +
                "   </saml2:Assertion>\n" +
                "</saml2p:Response>"

        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        samlResponseDecorator = new SamlResponseDecorator(samlResponse)
        mockDomainService.getDomainAdmins(_) >> [domainAdmin].asList()
        mockIdentityUserService.getFederatedUsersByDomainIdAndIdentityProviderIdCount(_, _) >> 0
        def idp = createIdentityProvider()

        and:
        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> idp
        mockRoleService.getRoleByName(ROLE_NAME) >> dummyRbacRole

        when:
        provisionedUserSourceFederationHandler.processRequestForProvider(samlResponseDecorator, idp)

        then:
        thrown(BadRequestException)
    }

    def "Generate authentication info from saml response when user does not exist"() {
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)

        mockFederatedUserDao.getUserByUsernameForIdentityProviderId(USERNAME, IDP_ID) >> null
        mockTenantService.getTenantsByDomainId(_, Boolean.toString(true)) >> tenants
        mockScopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> endpoints
        mockTenantService.getTenantRolesForUser(_) >> roles
        mockDomainService.getDomainAdmins(_) >> [domainAdmin].asList()
        reloadableConfig.getIdentityFederationMaxUserCountPerDomainForIdp(_) >> 1000
        mockRoleService.getRoleByName(ROLE_NAME) >> dummyRbacRole
        mockAuthorizationService.restrictUserAuthentication(_, _) >> false
        when:
        def authInfo = provisionedUserSourceFederationHandler.processRequestForProvider(new SamlResponseDecorator(samlResponse), theIdentityProvider)

        then:
        //shouldn't try to delete tokens since it's a new user
        0 * mockScopeAccessService.deleteExpiredTokensQuietly(_)
        1 * mockFederatedUserDao.addUser(_, _)
        1 * mockScopeAccessService.addUserScopeAccess(_, _)
        1 * mockTenantService.addTenantRolesToUser(_,_)
        1 * mockScopeAccessService.getServiceCatalogInfo(_) >> new ServiceCatalogInfo(roles, tenants, endpoints, IdentityUserTypeEnum.DEFAULT_USER)

        authInfo.token != null
        authInfo.endpoints == endpoints
        authInfo.user.username == USERNAME
        ((FederatedUser) authInfo.user).getFederatedIdpUri() == IDP_URI
        authInfo.user.domainId == DOMAIN_ID
    }

    def "Generate authentication info from saml response when user already exists"() {
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)

        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> theIdentityProvider
        mockFederatedUserDao.getUserByUsernameForIdentityProviderId(USERNAME, IDP_ID) >> user
        mockTenantService.getTenantsByDomainId(DOMAIN_ID, Boolean.toString(true)) >> tenants
        mockScopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> endpoints
        mockTenantService.getTenantRolesForUser(_) >> roles
        mockDomainService.getDomainAdmins(_) >> [Mock(User)].asList()
        reloadableConfig.getIdentityFederationMaxUserCountPerDomainForIdp(_) >> 1000
        mockRoleService.getRoleByName(ROLE_NAME) >> dummyRbacRole
        mockScopeAccessService.getServiceCatalogInfo(_) >> new ServiceCatalogInfo(roles, tenants, endpoints, IdentityUserTypeEnum.DEFAULT_USER)
        mockAuthorizationService.restrictUserAuthentication(_, _) >> false

        when:
        SamlAuthResponse authInfo = provisionedUserSourceFederationHandler.processRequestForProvider(new SamlResponseDecorator(samlResponse), theIdentityProvider)

        then:
        1 * mockScopeAccessService.deleteExpiredTokensQuietly(user)
        1 * mockScopeAccessService.addUserScopeAccess(user, _)
        0 * mockTenantService.addTenantRolesToUser(_,_)
        0 * mockFederatedUserDao.addUser(_,_)
        1 * mockTenantService.getGlobalRbacRolesForUser(_) >> Collections.EMPTY_LIST

        authInfo.token != null
        authInfo.endpoints == endpoints
        authInfo.user == user
        ((FederatedUser) authInfo.user).getFederatedIdpUri() == IDP_URI
        authInfo.userRoles == roles
        authInfo.user.domainId == DOMAIN_ID
    }

    def "Generate authentication info from saml response when user exists under different domainId throws exception"() {
        given:
        samlResponse = samlUnmarshaller.unmarshallResponse(samlStr)
        FederatedUser existingUser = new FederatedUser().with{
            it.domainId="diffDomain"
            return it
        }

        mockIdentityProviderDao.getIdentityProviderByUri(IDP_URI) >> theIdentityProvider
        mockFederatedUserDao.getUserByUsernameForIdentityProviderId(USERNAME, IDP_ID) >> existingUser
        mockDomainService.getDomainAdmins(_) >> [Mock(User)].asList()
        reloadableConfig.getIdentityFederationMaxUserCountPerDomainForIdp(_) >> 1000
        mockRoleService.getRoleByName(ROLE_NAME) >> dummyRbacRole

        when:
        def authInfo = provisionedUserSourceFederationHandler.processRequestForProvider(new SamlResponseDecorator(samlResponse), theIdentityProvider)

        then:
        0 * mockTenantService.addTenantRolesToUser(_,_)
        0 * mockFederatedUserDao.addUser(_,_)

        DuplicateUsernameException ex = thrown()
        IdmExceptionAssert.assertException(ex, DuplicateUsernameException, ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE, ProvisionedUserSourceFederationHandler.DUPLICATE_USERNAME_ERROR_MSG)
    }



    def mockIdentityProviderDao(validator) {
        mockIdentityProviderDao = Mock(IdentityProviderDao)
        validator.identityProviderDao = mockIdentityProviderDao
    }

    def mockRoleService(validator) {
        mockRoleService = Mock(RoleService)
        validator.roleService = mockRoleService
    }

    def mockDomainDao(validator) {
        mockDomainDao = Mock(DomainDao)
        validator.domainDao = mockDomainDao
    }

    def mockSamlSignatureValidator(validator) {
        mockSamlSignatureValidator = Mock(SamlSignatureValidator)
        validator.samlSignatureValidator = mockSamlSignatureValidator
    }

    def mockPrecedenceValidator(validator) {
        mockPrecedenceValidator = Mock(PrecedenceValidator)
        validator.precedenceValidator = mockPrecedenceValidator
    }

    def mockConfig(validator) {
        mockConfig = Mock(Configuration)
        validator.config = mockConfig
    }

    def mockDomainService(validator) {
        mockDomainService = Mock(DomainService)
        validator.domainService = mockDomainService
    }

    def mockIdentityUserService(validator) {
        mockIdentityUserService = Mock(IdentityUserService)
        validator.identityUserService = mockIdentityUserService
    }

    def mockFederatedUserDao(validator) {
        mockFederatedUserDao = Mock(FederatedUserDao)
        validator.federatedUserDao = mockFederatedUserDao
    }

    def mockRoleDao(service) {
        mockRoleDao = Mock(ApplicationRoleDao)
        mockRoleDao.getRoleByName(_) >> createClientRole("1000", "roleName")
        service.roleDao = mockRoleDao
    }
    def mockScopeAccessService(service) {
        mockScopeAccessService = Mock(ScopeAccessService)
        service.scopeAccessService = mockScopeAccessService
    }

    def mockTenantService(service) {
        mockTenantService = Mock(TenantService)
        service.tenantService = mockTenantService
    }

    def mockDateHelper(service) {
        mockDateHelper = Mock(DateHelper)
        mockDateHelper.addSecondsToDate(_, _) >> new Date()
        service.dateHelper = mockDateHelper
    }

    def createIdentityProvider() {
        new IdentityProvider().with({
            it.providerId = IDP_ID
            it.uri = IDP_URI
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL.storedVal
            return it
        })
    }

    def createDomain() {
        new Domain().with({
            it.enabled = true
            it.domainId = DOMAIN
            it.name = DOMAIN
            return it
        })
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
}