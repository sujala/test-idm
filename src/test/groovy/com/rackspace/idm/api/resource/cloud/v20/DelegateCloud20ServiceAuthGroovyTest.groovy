package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.resource.cloud.CloudClient
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.exception.NotAuthorizedException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.validation.Validator20
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/14/13
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
class DelegateCloud20ServiceAuthGroovyTest extends RootServiceTest {
    @Shared CloudClient cloudClient
    @Shared expiredDate
    @Shared futureDate


    def setupSpec(){
        delegateCloud20Service = new DelegateCloud20Service()
        expiredDate = new Date().minus(5)
        futureDate = new Date().plus(5)
    }

    def setup(){
        setupMocks()
        config.getBoolean("useCloudAuth") >> "true"
    }

    def "Service token: CA - not found or expired & GA - not found | User token: CA - Not Found, expired or valid  & GA - Not found, expired, or valid"(){
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def builtResponse = response.build()
        builtResponse.status == 401
    }

    def "Service token: CA - not found or expired & GA - not found | User token: CA - not found, expired, or valid & GA - expired"(){
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [null, createScopeAccess(expiredDate)]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def builtResponse = response.build()
        builtResponse.status == 401
    }

    def "Service token: CA - not found or expired & GA - not found | User token: CA - not found, expired, or valid & GA - valid"(){
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [null, createScopeAccess(futureDate)]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def builtResponse = response.build()
        builtResponse.status == 401
    }

    def "Service token: CA - valid & GA - not found | User token: CA - not found, expired & GA - not found"(){
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(404)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [null, null]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def builtResponse = response.build()
        builtResponse.status == 404
    }

    def "Service token: CA - valid & GA - not found | User token: CA - valid & GA - not found"(){
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [null, null]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def builtResponse = response.build()
        builtResponse.status == 200
    }

    def "Service token: CA - valid & GA - not found | User token: CA - valid & GA - expired"(){
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [null, createScopeAccess(expiredDate)]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotFoundException)
    }

    def "Service token: CA - valid & GA - not found | User token: CA - not found or expired & GA - valid"(){
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(404)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [null, createScopeAccess(expiredDate)]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def builtResponse = response.build()
        builtResponse.status == 404
    }

    def "Service token: CA - valid & GA - not found | User token: CA - valid & GA - valid"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [null, createScopeAccess(futureDate)]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def builtResponse = response.build()
        builtResponse.status == 200
    }

    def "Service token: CA - not found - & GA - expired | User token: CA - not found, expired or valid & GA - not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), null]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - not found - & GA - expired | User token: CA - not found, expired or valid & GA - expired"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), createScopeAccess(expiredDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - not found - & GA - expired | User token: CA - not found, expired or valid & GA - valid"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), createScopeAccess(futureDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - expired & GA - expired | User token: CA - not found, expired or valid & GA - not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), null]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - expired & GA - expired | User token: CA - not found, expired or valid & GA - expired"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), createScopeAccess(expiredDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - expired & GA - expired | User token: CA - not found, expired or valid & GA -valid"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), createScopeAccess(futureDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA valid - & GA - expired | User token: CA - not found or expired & GA - not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(404)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), null]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - valid & GA - expired | User token: CA valid - & GA - not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), null]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - valid & GA - expired | User token: CA not found or expired - & GA - expired "() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(404)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), createScopeAccess(expiredDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - valid & GA - expired | User token: CA - valid & GA - future"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(expiredDate), createScopeAccess(futureDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Service token: CA - not found & GA - valid | User token: CA - not found, expired, valid & GA - not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), null]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotFoundException)

    }

    def "Service token: CA - not found & GA - valid | User token: CA - not found, expired, valid & GA - expired"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(expiredDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotFoundException)
    }

    def "Service token: CA - not found & GA - valid | User token: CA - not found, expired or valid & GA - valid"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(futureDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        1 * defaultCloud20Service.validateToken(_, _, _, _)
    }

    def "Service token: CA - expired & GA - valid | User token: CA - not found, expired, or valid & GA - not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), null]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotFoundException)
    }

    def "Service token: CA - expired & GA - valid | User token: CA - not found, expired, or valid & GA - expired"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(expiredDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotFoundException)
    }

    def "Service token: CA - expired & GA - valid | User token: CA - not found, expired, or valid & GA - valid"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(401)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(futureDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        1 * defaultCloud20Service.validateToken(_, _, _, _)
    }

    def "Service token: CA - valid & GA - valid | User token: CA - not found, expired & GA - not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(404)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), null]

        when:
        def response = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def responseBuilder = response.build()
        responseBuilder.status == 404
    }

    def "Service token: CA - valid & GA - valid | User token: CA - valid & GA not found"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), null]

        when:
        def builder = delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        def response = builder.build()
        response.status == 200


    }

    def "Service token: CA - valid & GA - valid | User token: CA - not found or expired & GA - expired" () {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(404)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(expiredDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotFoundException)
    }

    def "Service token: CA - valid & GA - valid | User token: CA - valid & GA - expired"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(expiredDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        thrown(NotFoundException)
    }

    def "Service token: CA - valid & GA - valid | User token: CA - not found or expired & GA - valid"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(404)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(futureDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        1 * defaultCloud20Service.validateToken(_, _, _, _)
    }

    def "Service token: CA - valid & GA - valid | User token: CA - valid & GA - valid"() {
        given:
        cloudClient.get(_,_) >> createCloudAuthGetResponse(200)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [createScopeAccess(futureDate), createScopeAccess(futureDate)]

        when:
        delegateCloud20Service.validateToken(null, "token1", "token2", null)

        then:
        1 * defaultCloud20Service.validateToken(_, _, _, _)
    }


    def "Add User: GA is Source of Truth - Routing true - token is invalid - return 401"() {
        given:
        config.getBoolean("useCloudAuth") >> true
        config.getBoolean("gaIsSourceOfTruth") >> true
        scopeAccessService.getAccessTokenByAuthHeader(_) >> null

        when:
        delegateCloud20Service.addUser(null, null, "invalid", null)

        then:
        thrown(NotAuthorizedException)
    }

    def "Verify that get group gets called when /RAX-GRPADM/grousp gets calls" (){
        given:
        config.getBoolean("useCloudAuth") >> true
        config.getBoolean("gaIsSourceOfTruth") >> true

        when:
        delegateCloud20Service.getGroup(null, authToken, "name")

        then:
        1 * defaultCloud20Service.getGroup(_,_,_)
    }

    def createScopeAccess(Date expTime) {
        new ScopeAccess().with {
            it.accessTokenExp = expTime
            it.accessTokenString = "tokenString"
            return it
        }
    }

    def createCloudAuthGetResponse(int status){
        Response.ResponseBuilder responseBuilder = Response.status(status)
        return responseBuilder
    }

    def setupMocks(){
        validator20 = Mock()
        delegateCloud20Service.validator20 = validator20
        scopeAccessService = Mock()
        delegateCloud20Service.scopeAccessService = scopeAccessService
        cloudClient = Mock()
        delegateCloud20Service.cloudClient = cloudClient
        config = Mock()
        delegateCloud20Service.config = config
        defaultCloud20Service = Mock()
        delegateCloud20Service.defaultCloud20Service = defaultCloud20Service

    }
}
