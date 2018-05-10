package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.SamlAuthResponse
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.util.SamlSignatureValidator
import org.apache.commons.lang3.RandomStringUtils
import org.opensaml.core.config.InitializationService
import spock.lang.Specification
import testHelpers.IdmAssert
import testHelpers.RootServiceTest
import testHelpers.saml.SamlFactory

class DefaultFederatedIdentityServiceTest extends RootServiceTest {
    DefaultFederatedIdentityService service
    FederatedAuthHandlerV1 federatedAuthHandlerV1 = new FederatedAuthHandlerV1()

    IdentityProviderDao identityProviderDao = Mock()

    SamlSignatureValidator samlSignatureValidator = Mock()

    ProvisionedUserSourceFederationHandler provisionedUserSourceFederationHandler = Mock()

    RackerSourceFederationHandler rackerSourceFederationHandler = Mock()

    SamlFactory assertionFactory = new SamlFactory()

    def IDP_ID = "nam";
    def IDP_URI = "http://my.test.idp"
    IdentityProvider provisionedIdentityProvider = new IdentityProvider().with {
        it.uri = IDP_URI
        it.providerId = IDP_ID
        it.federationType = IdentityProviderFederationTypeEnum.DOMAIN.name()
        it
    }
    IdentityProvider rackerIdentityProvider = new IdentityProvider().with {
        it.uri = IDP_URI
        it.providerId = IDP_ID
        it.federationType = IdentityProviderFederationTypeEnum.RACKER.name()
        it
    }

    def "setup"() {
        service = new DefaultFederatedIdentityService()
        federatedAuthHandlerV1 = new FederatedAuthHandlerV1()

        service.samlSignatureValidator = samlSignatureValidator
        service.provisionedUserSourceFederationHandler = provisionedUserSourceFederationHandler
        service.rackerSourceFederationHandler = rackerSourceFederationHandler
        service.identityProviderDao = identityProviderDao
        service.identityConfig = Mock(IdentityConfig)
        def reloadableConfig = Mock(IdentityConfig.ReloadableConfig)
        service.identityConfig.getReloadableConfig() >> reloadableConfig
        reloadableConfig.getFederatedResponseMaxAge() >> 100000

        service.federatedAuthHandlerV1 = federatedAuthHandlerV1
        federatedAuthHandlerV1.samlSignatureValidator = samlSignatureValidator
        federatedAuthHandlerV1.provisionedUserSourceFederationHandler = provisionedUserSourceFederationHandler
        federatedAuthHandlerV1.rackerSourceFederationHandler = rackerSourceFederationHandler
        federatedAuthHandlerV1.identityProviderDao = identityProviderDao
        federatedAuthHandlerV1.identityConfig = service.identityConfig
        mockAuthorizationService(service)
        mockDomainService(service)
    }

    def "Error thrown when saml missing issuer"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedUser("", "any", 1, "1234", Collections.EMPTY_LIST)

        when:
        SamlAuthResponse response = service.processSamlResponse(saml)

        then:
        BadRequestException ex = thrown()
        ex.message.matches(IdmAssert.generateErrorCodePattern(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_ISSUER))
    }

    def "Error thrown when saml missing username (subject)"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, "", 1, "1234", Collections.EMPTY_LIST)
        identityProviderDao.getIdentityProviderByUri(_) >> provisionedIdentityProvider

        when:
        def response = service.processSamlResponse(saml)

        then:
        BadRequestException ex = thrown()
        ex.message.matches(IdmAssert.generateErrorCodePattern(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_USERNAME))
    }

    def "Error thrown when saml contains old confirmation date"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, "any", -1, "1234", Collections.EMPTY_LIST)
        identityProviderDao.getIdentityProviderByUri(_) >> provisionedIdentityProvider

        when:
        def response = service.processSamlResponse(saml)

        then:
        BadRequestException ex = thrown()
        ex.message.matches(IdmAssert.generateErrorCodePattern(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_SUBJECT_NOTONORAFTER))
    }

    def "Error thrown when saml missing auth instant"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, "any", 1, "1234", Collections.EMPTY_LIST)
        identityProviderDao.getIdentityProviderByUri(_) >> provisionedIdentityProvider
        saml.assertions.get(0).authnStatements.get(0).authnInstant = null

        when:
        def response = service.processSamlResponse(saml)

        then:
        BadRequestException ex = thrown()
        ex.message.matches(IdmAssert.generateErrorCodePattern(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_AUTH_INSTANT))
    }

    def "Error thrown when saml missing AuthnContextClassRef"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, "any", 1, "1234", Collections.EMPTY_LIST)
        identityProviderDao.getIdentityProviderByUri(_) >> provisionedIdentityProvider
        saml.assertions.get(0).authnStatements.get(0).authnContext.authnContextClassRef.authnContextClassRef = null

        when:
        def response = service.processSamlResponse(saml)

        then:
        BadRequestException ex = thrown()
        ex.message.matches(IdmAssert.generateErrorCodePattern(ErrorCodes.ERROR_CODE_FEDERATION_MISSING_AUTH_CONTEXT_CLASSREF))
    }

    def "Error thrown when saml contains invalid AuthnContextClassRef"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, "any", 1, "1234", Collections.EMPTY_LIST)
        identityProviderDao.getIdentityProviderByUri(_) >> provisionedIdentityProvider
        saml.assertions.get(0).authnStatements.get(0).authnContext.authnContextClassRef.authnContextClassRef = "junk"

        when:
        def response = service.processSamlResponse(saml)

        then:
        BadRequestException ex = thrown()
        ex.message.matches(IdmAssert.generateErrorCodePattern(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_AUTH_CONTEXT_CLASSREF))
    }

    def "Sends Provisioned IDP requests to provisioned handler"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, "any", 1, "1234", Collections.EMPTY_LIST)
        identityProviderDao.getIdentityProviderByUri(_) >> provisionedIdentityProvider
        def samlResponse = new SamlAuthResponse(new FederatedUser(), null, null, null)

        when:
        def response = service.processSamlResponse(saml)

        then:
        1 * provisionedUserSourceFederationHandler.processRequestForProvider(_, provisionedIdentityProvider) >> samlResponse
    }

    def "Sends Racker IDP requests to racker handler"() {
        def saml = assertionFactory.generateSamlAssertionResponseForFederatedRacker(Constants.DEFAULT_IDP_URI, "any", 1)
        identityProviderDao.getIdentityProviderByUri(_) >> rackerIdentityProvider
        def samlResponse = new SamlAuthResponse(new FederatedUser(), null, null, null)

        when:
        def response = service.processSamlResponse(saml)

        then:
        1 * rackerSourceFederationHandler.processRequestForProvider(_, rackerIdentityProvider) >> samlResponse
    }

    def "getIdentityProviderByEmailDomain: calls backend service"() {
        given:
        def emailDomain = "emailDomain"

        when:
        service.getIdentityProviderByEmailDomain(emailDomain)

        then:
        1 * identityProviderDao.getIdentityProviderByEmailDomain(emailDomain)
    }

    def "getIdentityProviderByEmailDomain: invalid case"() {
        when:
        service.getIdentityProviderByEmailDomain(null)

        then:
        thrown(IllegalArgumentException)
    }

}
