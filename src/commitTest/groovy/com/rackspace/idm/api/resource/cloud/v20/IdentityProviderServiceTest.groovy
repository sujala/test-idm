package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.IdentityProperty
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.http.HttpStatus
import org.opensaml.core.config.InitializationService
import org.w3c.dom.Document
import spock.lang.Shared
import testHelpers.RootServiceTest

class IdentityProviderServiceTest extends RootServiceTest {

    @Shared
    DefaultCloud20Service service

    def setup(){
        InitializationService.initialize()

        service = new DefaultCloud20Service()

        // Setup mocks
        mockValidator20(service)
        mockUserService(service)
        mockIdentityConfig(service)
        mockAtomHopperClient(service)
        mockExceptionHandler(service)
        mockRequestContextHolder(service)
        mockAuthorizationService(service)
        mockFederatedIdentityService(service)
        mockIdentityProviderConverterCloudV20(service)
    }

    def "addIdentityProvider - can successfully create identity provider"() {
        given:
        def identityProvider = new IdentityProvider()

        when:
        def response = service.addIdentityProvider(headers, uriInfo(), authToken, identityProvider)

        then:
        response.build().status == HttpStatus.SC_CREATED

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName())
        1 * identityProviderConverterCloudV20.fromIdentityProvider(_) >> entityFactory.createIdentityProviderWithoutCertificate()
        1 * federatedIdentityService.checkAndGetDefaultMappingPolicyProperty() >> new IdentityProperty().with{it.valueType = "type"; it}
        1 * federatedIdentityService.addIdentityProvider(_)
        1 * atomHopperClient.asyncPostIdpEvent(_, EventType.CREATE)
        1 * identityProviderConverterCloudV20.toIdentityProvider(_) >> identityProvider
    }

    def "updateIdentityProvider - can successfully update identity provider"() {
        given:
        def identityProvider = new IdentityProvider()

        when:
        def response = service.updateIdentityProvider(headers, uriInfo(), authToken, "id", identityProvider)

        then:
        response.build().status == HttpStatus.SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getEffectiveCaller() >> entityFactory.createUser()
        1 * authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                IdentityRole.RCN_ADMIN.getRoleName()))
        1 * federatedIdentityService.checkAndGetIdentityProviderWithMetadataById("id") >> entityFactory.createIdentityProviderWithoutCertificate()
        1 * requestContextHolder.getRequestContext().getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        1 * federatedIdentityService.updateIdentityProvider(_)
        1 * atomHopperClient.asyncPostIdpEvent(_, EventType.UPDATE);
        1 * identityProviderConverterCloudV20.toIdentityProvider(_) >> identityProvider
    }

    def "deleteIdentityProvider - can successfully delete identity provider"() {
        when:
        def response = service.deleteIdentityProvider(headers, authToken, "id")

        then:
        response.build().status == HttpStatus.SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getEffectiveCaller() >> entityFactory.createUser()
        1 * authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                IdentityRole.RCN_ADMIN.getRoleName()))
        1 * userService.getGroupsForUser(_) >> new ArrayList<>()
        1 * federatedIdentityService.checkAndGetIdentityProvider("id") >> entityFactory.createIdentityProviderWithoutCertificate()
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList((
                IdentityUserTypeEnum.USER_ADMIN.getRoleName()),
                IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                IdentityRole.RCN_ADMIN.getRoleName())) >> false
        1 * federatedIdentityService.deleteIdentityProviderById(_)
        1 * atomHopperClient.asyncPostIdpEvent(_, EventType.DELETE)
    }

    def "getIdentityProvidersMetadata - can successfully get identity provider's metadata"() {
        when:
        def response = service.getIdentityProvidersMetadata(headers, authToken, "id")

        then:
        response.build().status == HttpStatus.SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getEffectiveCaller() >> entityFactory.createUser()
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList((
                IdentityUserTypeEnum.USER_ADMIN.getRoleName()),
                IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                IdentityRole.RCN_ADMIN.getRoleName())) >> false
        1 * federatedIdentityService.checkAndGetIdentityProvider("id");
        1 * federatedIdentityService.getIdentityProviderWithMetadataById("id") >> entityFactory.createIdentityProviderWithoutCertificate().with {
            it.xmlMetadata = new byte[1]
            it
        }
        1 * identityProviderConverterCloudV20.getXMLDocument(_) >> Mock(Document)
    }
}
