package com.rackspace.idm.modules.endpointassignment.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule
import com.rackspace.idm.Constants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointList
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.SamlProducer
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

class TenantTypeRuleApplicationIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "endpoint mapping rules are applied based on inferred tenant types - apply mapping rules == #featureEnabled, using an empty list of endpoints for the rule == #emptyEndpoints" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        // Create the tenant before the tenant type so that it does not have the type auto-assigned
        def tenantType = "${RandomStringUtils.randomAlphabetic(8)}".toLowerCase()
        def tenant = utils.createTenant("${tenantType}:${RandomStringUtils.randomAlphanumeric(8)}", true, RandomStringUtils.randomAlphanumeric(8), domainId)
        utils.createTenantType(tenantType)
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = tenantType
            it.endpointTemplates = new EndpointTemplateList()
            if (emptyEndpoints) {
                it.endpointTemplates.endpointTemplate = [] as List<EndpointTemplate>
            } else {
                it.endpointTemplates.endpointTemplate = [v1Factory.createEndpointTemplate(Constants.MOSSO_ENDPOINT_TEMPLATE_ID, "whatever"), v1Factory.createEndpointTemplate(Constants.NAST_ENDPOINT_TEMPLATE_ID, "whatever")] as List<EndpointTemplate>
            }
            it
        }
        cloud20.addEndpointAssignmentRule(utils.getIdentityAdminToken(), rule)

        // Create an IDP to federate to
        def issuer = UUID.randomUUID().toString()
        def idpUrl = UUID.randomUUID().toString()
        def idpCredential = SamlCredentialUtils.generateX509Credential()
        def samlProducer = new SamlProducer(idpCredential)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(idpCredential.entityCertificate)
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts)
        def idpData = v2Factory.createIdentityProvider(issuer, "blah", idpUrl, IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData).getEntity(IdentityProvider)

        // Create a broker IDP to use for v2 fed
        def brokerCredential = SamlCredentialUtils.generateX509Credential()
        def brokerIdp = cloud20.createIdentityProviderWithCred(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.BROKER, brokerCredential).getEntity(IdentityProvider)
        def v2authRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerCredential, idpCredential)

        def inferredEndpoint1 = "${Constants.MOSSO_ENDPOINT_TEMPLATE_PUBLIC_URL}/${tenant.id}"
        def expectedEndpoint1 = featureEnabled && !emptyEndpoints ? inferredEndpoint1 : null
        def inferredEndpoint2 = "${Constants.NAST_ENDPOINT_TEMPLATE_PUBLIC_URL}/${tenant.id}"
        def expectedEndpoint2 = featureEnabled && !emptyEndpoints ? inferredEndpoint2 : null

        when: "authenticate"
        AuthenticateResponse authResponse = utils.authenticate(userAdmin.username)

        then:
        authResponse.serviceCatalog.service.endpoint.publicURL.flatten().find { url -> url == inferredEndpoint1 } == expectedEndpoint1
        authResponse.serviceCatalog.service.endpoint.publicURL.flatten().find { url -> url == inferredEndpoint2 } == expectedEndpoint2

        when: "list endpoints for token"
        def token = utils.getToken(userAdmin.username)
        EndpointList endpointList = utils.listEndpointsForToken(token, token)

        then:
        endpointList.endpoint.publicURL.find() { url -> url == inferredEndpoint1 } == expectedEndpoint1
        endpointList.endpoint.publicURL.find() { url -> url == inferredEndpoint2 } == expectedEndpoint2

        when: "v2 federated auth"
        def v2AuthRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = domainId
            it.validitySeconds = 1000
            it.brokerIssuer = brokerIdp.issuer
            it.originIssuer = idp.issuer
            it.email = "${RandomStringUtils.randomAlphanumeric(8)}@example.com"
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.roleNames = [] as Set
            it
        }
        def samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        def v2FedResponse = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse)).getEntity(AuthenticateResponse).value

        then:
        v2FedResponse.serviceCatalog.service.endpoint.publicURL.flatten().find { url -> url == inferredEndpoint1 } == expectedEndpoint1
        v2FedResponse.serviceCatalog.service.endpoint.publicURL.flatten().find { url -> url == inferredEndpoint2 } == expectedEndpoint2

        cleanup:
        utils.deleteIdentityProvider(idp)
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)

        where:
        [featureEnabled, emptyEndpoints] << [[true, false], [true, false]].combinations()
    }

}
