package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedRackerRequest
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedRackerDao
import com.rackspace.idm.domain.decorator.SamlResponseDecorator
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.RackerScopeAccess
import com.rackspace.idm.domain.entity.SamlAuthResponse
import com.rackspace.idm.domain.entity.TargetUserSourceEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory
import testHelpers.saml.SamlAssertionFactory

class RackerSourceFederationHandlerTest extends Specification {
    //service testing
    RackerSourceFederationHandler federationHandler = new RackerSourceFederationHandler()

    //dependencies
    FederatedRackerDao federatedRackerDao = Mock(FederatedRackerDao)

    ScopeAccessService scopeAccessService = Mock(ScopeAccessService)

    UserService userService = Mock(UserService)

    TenantService tenantService = Mock(TenantService)

    IdentityConfig identityConfig = Mock(IdentityConfig)
    IdentityConfig.StaticConfig staticConfig = Mock(IdentityConfig.StaticConfig)
    IdentityConfig.ReloadableConfig reloadableConfig = Mock(IdentityConfig.ReloadableConfig)

    //constants & test helpers
    @Shared def IDP_NAME = "nam";
    @Shared def IDP_URI = "http://my.test.idp"
    @Shared def IDP_PUBLIC_CERTIFICATE = "--BEGIN CERTIFICATE-- bla bla bla --END CERTIFICATE--"
    @Shared def RACKER_USERNAME = "rackerJoe"

    EntityFactory entityFactory = new EntityFactory()

    IdentityProvider identityProvider = new IdentityProvider().with {
        it.uri = IDP_URI
        it.name = IDP_NAME
        it.targetUserSource = TargetUserSourceEnum.RACKER.name()
        it
    }

    String RACKER_ROLE_ID = 9
    def FOUNDATION_CLIENT_ID = "asdjwehuqrew"
    def CLOUD_AUTH_CLIENT_ID = "345hjkwetugfhj5346hiou"

    def SamlAssertionFactory samlAssertionFactory = new SamlAssertionFactory()

    def setup() {
        identityConfig.getReloadableConfig() >> reloadableConfig
        identityConfig.getStaticConfig() >> staticConfig

        staticConfig.getRackerRoleId() >> RACKER_ROLE_ID
        staticConfig.getFoundationClientId() >> FOUNDATION_CLIENT_ID
        staticConfig.getCloudAuthClientId() >> CLOUD_AUTH_CLIENT_ID
        
        federationHandler.identityConfig = identityConfig
        federationHandler.federatedRackerDao = federatedRackerDao
        federationHandler.userService = userService
        federationHandler.scopeAccessService = scopeAccessService
        federationHandler.tenantService = tenantService

        scopeAccessService.generateToken() >>> ["token1", "token2", "token3", "token4"]
    }

    def "processRequestForProvider - nulls throw IllegalArguments"() {
        when:
        federationHandler.processRequestForProvider(null, new IdentityProvider())

        then:
        thrown(IllegalArgumentException)

        when:
        federationHandler.processRequestForProvider(Mock(SamlResponseDecorator), null)

        then:
        thrown(IllegalArgumentException)
    }


    def "parseSaml - no racker throws error"() {
        given:
        def samlResponse = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, null, 1)
        SamlResponseDecorator decoratedSaml = new SamlResponseDecorator(samlResponse)

        when:
        FederatedRackerRequest request = federationHandler.parseSaml(decoratedSaml, identityProvider)

        then:
        thrown(BadRequestException)
    }

    def "parseSaml - request date in past throws error"() {
        given:
        def samlResponse = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, RACKER_USERNAME, -1)
        SamlResponseDecorator decoratedSaml = new SamlResponseDecorator(samlResponse)

        when:
        FederatedRackerRequest request = federationHandler.parseSaml(decoratedSaml, identityProvider)

        then:
        thrown(BadRequestException)
    }

    /**
     * Test the private method's ability to parse the SAML information out correctly to return a FederatedRackerRequest
     *
     * @return
     */
    def "parseSaml - racker retrieved and processed correctly from valid saml"() {
        given:
        def samlResponse = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, RACKER_USERNAME, 1)
        SamlResponseDecorator decoratedSaml = new SamlResponseDecorator(samlResponse)

        when:
        FederatedRackerRequest request = federationHandler.parseSaml(decoratedSaml, identityProvider)

        then:
        request.user != null
        request.requestedTokenExpirationDate != null
        request.identityProvider == identityProvider

        request.user.domainId == null
        request.user.username == RACKER_USERNAME
        request.user.rackerId == RACKER_USERNAME + "@" + IDP_URI
        request.user.getFederatedUserName() == RACKER_USERNAME
        request.user.getFederatedIdpUri() == IDP_URI
        request.user.getEnabled()
        request.identityProvider == identityProvider
    }

    def "processRequestForProvider - correctly determines whether or not to create a new fed racker user"() {
        given:
        def existingRackerUsername = RACKER_USERNAME
        Racker existingRacker = entityFactory.createRacker(RACKER_USERNAME + "@" + IDP_URI)

        def newRackerUsername = "new" + RACKER_USERNAME
        Racker newRacker = entityFactory.createRacker(newRackerUsername + "@" + IDP_URI)
        federatedRackerDao.getUserById(newRacker.rackerId) >> null
        federatedRackerDao.getUserById(existingRacker.rackerId) >> existingRacker

        when: "racker does not exist"
        def newUserSamlResponse = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, newRackerUsername, 1)
        SamlAuthResponse newRackerResponse = federationHandler.processRequestForProvider(new SamlResponseDecorator(newUserSamlResponse), identityProvider)

        then: "racker added, token added, no cleaning tokens, retrieve racker roles"
        1 * federatedRackerDao.addUser(identityProvider, _)
        0 * scopeAccessService.deleteExpiredTokensQuietly(_)
        1 * scopeAccessService.addUserScopeAccess(_, _); //just test is called
        1 * userService.getRackerRoles(newRackerUsername) >> Collections.EMPTY_LIST
        1 * tenantService.addTenantRoleToUser(_, _)
        newRackerResponse.user != null
        newRackerResponse.user.getFederatedUserName() == newRackerUsername
        newRackerResponse.token != null
        newRackerResponse.userRoles != null
        newRackerResponse.endpoints != null

        when: "racker already exists"
        def existingUserSamlResponse = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, existingRackerUsername, 1)
        SamlAuthResponse existingRackerResponse = federationHandler.processRequestForProvider(new SamlResponseDecorator(existingUserSamlResponse), identityProvider)

        then: "racker not added, token added, clean tokens, retrieve racker roles"
        0 * federatedRackerDao.addUser(identityProvider, existingRacker)
        0 * tenantService.addTenantRoleToUser(_, _)
        1 * scopeAccessService.deleteExpiredTokensQuietly(existingRacker)
        1 * scopeAccessService.addUserScopeAccess(existingRacker, _); //just test is called
        1 * userService.getRackerRoles(existingRackerUsername) >> Collections.EMPTY_LIST
        existingRackerResponse.user != null
        existingRackerResponse.user.getFederatedUserName() == existingRackerUsername
        existingRackerResponse.token != null
        existingRackerResponse.userRoles != null
        existingRackerResponse.endpoints != null
    }

    def "processRequestForProvider - retrieves and creates racker roles correctly"() {
        given:
        def existingRackerUsername = RACKER_USERNAME
        Racker existingRacker = entityFactory.createRacker(RACKER_USERNAME + "@" + IDP_URI)
        federatedRackerDao.getUserById(existingRacker.rackerId) >> existingRacker
        def s1 = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, existingRackerUsername, 1)
        def edirRole1 = "edirRole1"
        def edirRole2 = "edirRole2"

        when: "no edir roles"
        SamlAuthResponse r1 = federationHandler.processRequestForProvider(new SamlResponseDecorator(s1), identityProvider)

        then: "racker not added, token added, clean tokens, retrieve racker roles"
        1 * userService.getRackerRoles(existingRackerUsername) >> Collections.EMPTY_LIST
        r1.userRoles.size() == 1
        r1.userRoles.find() {it.roleRsId == RACKER_ROLE_ID} != null //has racker role

        when: "has edir roles"
        SamlAuthResponse r2 = federationHandler.processRequestForProvider(new SamlResponseDecorator(s1), identityProvider)

        then: "racker not added, token added, clean tokens, retrieve racker roles"
        1 * userService.getRackerRoles(existingRackerUsername) >> [edirRole1, edirRole2]
        r2.userRoles.size() == 3
        r2.userRoles.find() {it.roleRsId == RACKER_ROLE_ID} != null //has racker role
        r2.userRoles.find() {it.name == edirRole1} != null //has edir role
        r2.userRoles.find() {it.name == edirRole2} != null //has edir role
    }

    def "processRequestForProvider - creates new token"() {
        given:
        def existingRackerUsername = RACKER_USERNAME
        Racker existingRacker = entityFactory.createRacker(RACKER_USERNAME + "@" + IDP_URI)
        federatedRackerDao.getUserById(existingRacker.rackerId) >> existingRacker
        def s1 = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, existingRackerUsername, 1)

        when: "first token"
        SamlAuthResponse r1 = federationHandler.processRequestForProvider(new SamlResponseDecorator(s1), identityProvider)

        then: "racker not added, token added, clean tokens, retrieve racker roles"
        1 * scopeAccessService.addUserScopeAccess(_, _); //just test is called
        1 * userService.getRackerRoles(existingRackerUsername) >> Collections.EMPTY_LIST
        r1.token instanceof RackerScopeAccess
        r1.token.accessTokenExp != null
        r1.token.accessTokenString != null
        r1.token.authenticatedBy.find() {it == AuthenticatedByMethodEnum.FEDERATION.value}
        r1.token.clientId == CLOUD_AUTH_CLIENT_ID
        ((RackerScopeAccess)r1.token).rackerId == existingRacker.rackerId
        ((RackerScopeAccess)r1.token).issuedToUserId == existingRacker.rackerId
        ((RackerScopeAccess)r1.token).federatedIdpUri == IDP_URI
        ((RackerScopeAccess)r1.token).federatedRackerToken

        when: "second token"
        SamlAuthResponse r2 = federationHandler.processRequestForProvider(new SamlResponseDecorator(s1), identityProvider)

        then: "new token id"
        1 * scopeAccessService.addUserScopeAccess(_, _); //just test is called
        r1.token.accessTokenString != r2.token.accessTokenString
    }

    def "processRequestForProvider - endpoints are always empty list"() {
        given:
        def s1 = samlAssertionFactory.generateSamlAssertionResponseForFederatedRacker(IDP_URI, RACKER_USERNAME, 1)

        when:
        SamlAuthResponse r1 = federationHandler.processRequestForProvider(new SamlResponseDecorator(s1), identityProvider)

        then:
        1 * userService.getRackerRoles(RACKER_USERNAME) >> Collections.EMPTY_LIST
        r1.endpoints != null
        r1.endpoints.size() == 0
    }

}