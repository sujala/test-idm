package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.modules.endpointassignment.api.resource.EndpointAssignmentRuleResource;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;

/**
 * Cloud Auth 2.0 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private DefaultCloud20Service cloud20Service;

    @Autowired
    private CloudMultifactorResource multifactorResource;

    @Autowired
    private CloudMultifactorDomainResource multifactorDomainResource;

    @Autowired
    private EndpointAssignmentRuleResource endpointAssignmentRuleResource;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private RequestContextHolder requestContextHolder;

    private static final String JAXBCONTEXT_VERSION_CHOICE_CONTEXT_PATH = "org.openstack.docs.common.api.v1:org.w3._2005.atom";
    private static final String SERVICE_NOT_FOUND_ERROR_MESSAGE = "Service Not Found";
    private static final JAXBContext JAXBCONTEXT_VERSION_CHOICE;

    static {
        try {
            JAXBCONTEXT_VERSION_CHOICE = JAXBContext.newInstance(JAXBCONTEXT_VERSION_CHOICE_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new IdmException("Error initializing JAXBContext for versionchoice", e);
        }
    }

    @GET
    public Response getCloud20VersionInfo() throws JAXBException {
        JAXBContext context = JAXBCONTEXT_VERSION_CHOICE;
        if (!identityConfig.getReloadableConfig().reuseJaxbContext()) {
            //TODO causes memory leak...only left for backwards compatibility. Must be removed in future version.
            context = JAXBContext.newInstance(JAXBCONTEXT_VERSION_CHOICE_CONTEXT_PATH);
        }

        String responseXml = cloudContractDescriptionBuilder.buildVersion20Page();
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<VersionChoice> versionChoice = (JAXBElement<VersionChoice>) unmarshaller.unmarshal(new StringReader(responseXml));
        return Response.ok(versionChoice.getValue()).build();
    }

    @POST
    @Path("tokens")
    public Response authenticate(@Context HttpHeaders httpHeaders, @QueryParam("apply_rcn_roles") boolean applyRcnRoles, AuthenticationRequest authenticationRequest) {
        requestContextHolder.getAuthenticationContext().populateAuthRequestData(authenticationRequest);
        if (applyRcnRoles) {
            return cloud20Service.authenticateApplyRcnRoles(httpHeaders, authenticationRequest).build();
        } else {
            return cloud20Service.authenticate(httpHeaders, authenticationRequest).build();
        }
    }

    @POST
    @Path("/users/RAX-AUTH/forgot-pwd")
    public Response authenticateForForgotPassword(@Context HttpHeaders httpHeaders, ForgotPasswordCredentials forgotPasswordCredentials) {
        if(!identityConfig.getReloadableConfig().isForgotPasswordEnabled()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }

        return cloud20Service.authenticateForForgotPassword(httpHeaders, forgotPasswordCredentials).build();
    }

    @PUT
    @Path("users/RAX-AUTH/upgradeUserToCloud")
    public Response upgradeUserToCloud(@Context HttpHeaders httpHeaders,
                                       @Context UriInfo uriInfo,
                                       @HeaderParam(X_AUTH_TOKEN) String authToken,
                                       User upgradeUser) {
        if(!identityConfig.getReloadableConfig().isUpgradeUserToCloudEnabled()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }

        return cloud20Service.upgradeUserToCloud(httpHeaders, uriInfo, authToken, upgradeUser).build();
    }

    @POST
    @Path("/users/RAX-AUTH/pwd-reset")
    public Response passwordReset(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            PasswordReset passwordReset) {
        if(!identityConfig.getReloadableConfig().isForgotPasswordEnabled()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }

        return cloud20Service.passwordReset(httpHeaders, authToken, passwordReset).build();
    }

    @DELETE
    @Path("tokens")
    public Response revokeToken(@Context HttpHeaders httpHeaders, @HeaderParam(X_AUTH_TOKEN) String authToken) throws IOException, JAXBException {
        return cloud20Service.revokeToken(httpHeaders, authToken).build();
    }

    @DELETE
    @Path("tokens/{tokenId}")
    public Response revokeUserToken(@Context HttpHeaders httpHeaders,
                                    @HeaderParam(X_AUTH_TOKEN) String authToken,
                                    @PathParam("tokenId") String tokenId) throws IOException, JAXBException {
        return cloud20Service.revokeToken(httpHeaders, authToken, tokenId).build();
    }

    /**
     * @param httpHeaders
     * @param samlResponse
     * @return
     *
     * @deprecated Consumers should use {@link #federationSamlAuthenticationFormEncoded} version and provide the base64
     * encoded payload rather than pass in raw XML to avoid potential encoding issues.
     */
    @Deprecated
    @POST
    @Path("RAX-AUTH/saml-tokens")
    @Consumes({MediaType.APPLICATION_XML})
    public Response authenticateSamlResponse(@Context HttpHeaders httpHeaders, String samlResponse)  {
        return federationSamlAuthenticationRawXML(httpHeaders, samlResponse);
    }

    @Deprecated
    @POST
    @Path("RAX-AUTH/saml-tokens")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    public Response authenticateSamlResponseFormEncoded(@Context HttpHeaders httpHeaders, @FormParam("SAMLResponse") String samlResponse)  {
        return federationSamlAuthenticationFormEncoded(httpHeaders, samlResponse);
    }

    /**
     * Takes standard SAMLResponse in form (input field is SAMLResponse) that is base64'd and url encoded.
     *
     * curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'SAMLResponse=base64EncodedResponse' 'https://identity.api.rackspacecloud.com/v2.0/RAX-AUTH/federation/saml/auth'
     *
     * @param httpHeaders
     * @param samlResponse
     * @return
     */
    @POST
    @Path("RAX-AUTH/federation/saml/auth")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response federationSamlAuthenticationFormEncoded(@Context HttpHeaders httpHeaders, @FormParam("SAMLResponse") String samlResponse)  {
        Response response;
        if (org.apache.commons.lang.StringUtils.isBlank(samlResponse)) {
            response = exceptionHandler.exceptionResponse(new BadRequestException("Missing SAMLResponse field")).build();
        } else {
            try {
                byte[] samlResponseBytes = Base64.decodeBase64(samlResponse);
                response = cloud20Service.authenticateFederated(httpHeaders, samlResponseBytes).build();
            } catch (Exception ex) {
                response = exceptionHandler.exceptionResponse(ex).build();
            }
        }
        return response;
    }

    /**
     * Allows callers to provide the raw XML rather than the b64 encoded version of the SAML Response. Consumers
     * are encouraged to use {@link #federationSamlAuthenticationFormEncoded} version and provide the base64
     * encoded payload to avoid potential encoding issues with raw XML.
     *
     * @param httpHeaders
     * @param samlResponse
     * @return
     *
     */
    @POST
    @Path("RAX-AUTH/federation/saml/auth")
    @Consumes({MediaType.APPLICATION_XML})
    public Response federationSamlAuthenticationRawXML(@Context HttpHeaders httpHeaders, String samlResponse)  {
        Response response;
        if (org.apache.commons.lang.StringUtils.isBlank(samlResponse)) {
            response = exceptionHandler.exceptionResponse(new BadRequestException("Must provide SAMLResponse XML in body")).build();
        } else {
            try {
                byte[] samlResponseBytes = org.apache.commons.codec.binary.StringUtils.getBytesUtf8(samlResponse);
                response = cloud20Service.authenticateFederated(httpHeaders, samlResponseBytes).build();
            } catch (Exception ex) {
                response = exceptionHandler.exceptionResponse(ex).build();
            }
        }

        return response;
    }

    @POST
    @Path("RAX-AUTH/federation/saml/logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response federationSamlLogout(@Context HttpHeaders httpHeaders, @FormParam("SAMLRequest") String logoutRequest)  {
        if(!identityConfig.getReloadableConfig().isFederationLogoutSupported()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }

        Response response;
        if (org.apache.commons.lang.StringUtils.isBlank(logoutRequest)) {
            response = exceptionHandler.exceptionResponse(new BadRequestException("Missing SAMLRequest field")).build();
        } else {
            try {
                byte[] logoutRequestBytes = Base64.decodeBase64(logoutRequest);
                response = cloud20Service.logoutFederatedUser(httpHeaders, logoutRequestBytes).build();
            } catch (Exception ex) {
                response = exceptionHandler.exceptionResponse(ex).build();
            }
        }
        return response;
    }

    @POST
    @Path("RAX-AUTH/federation/identity-providers")
    public Response addIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , IdentityProvider identityProvider)  {
        if(!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.addIdentityProvider(httpHeaders, uriInfo, authToken, identityProvider).build();
    }

    @PUT
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}")
    public Response updateIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , @PathParam("identityProviderId") String identityProviderId
            , IdentityProvider identityProvider) {
        if (!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()) {
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.updateIdentityProvider(httpHeaders, uriInfo, authToken, identityProviderId, identityProvider).build();
    }

    @GET
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}")
    public Response getIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , @PathParam("identityProviderId") String identityProviderId)  {
        if(!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.getIdentityProvider(httpHeaders, authToken, identityProviderId).build();
    }

    @GET
    @Path("RAX-AUTH/federation/identity-providers")
    public Response getIdentityProviders(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("issuer") String issuer,
            @QueryParam("approvedDomainId") String approvedDomainId,
            @QueryParam("idpType") String idpType,
            @QueryParam("approvedTenantId") String approvedTenantId) {
        if(!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }

        return cloud20Service.getIdentityProviders(httpHeaders, authToken, name, issuer, approvedDomainId, approvedTenantId, idpType).build();
    }

    @DELETE
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}")
    public Response deleteIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , @PathParam("identityProviderId") String identityProviderId) {
        if (!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()) {
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.deleteIdentityProvider(httpHeaders, authToken, identityProviderId).build();
    }

    @PUT
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/certificates")
    public Response addCertToIdp(@Context HttpHeaders httpHeaders,
                                 @HeaderParam(X_AUTH_TOKEN) String authToken,
                                 @PathParam("identityProviderId") String identityProviderId,
                                 PublicCertificate publicCertificate) {
        if(!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.addIdentityProviderCert(httpHeaders, authToken, identityProviderId, publicCertificate).build();
    }

    @DELETE
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/certificates/{certificateId}")
    public Response deleteCertFromIdp(@Context HttpHeaders httpHeaders,
                                      @HeaderParam(X_AUTH_TOKEN) String authToken,
                                      @PathParam("identityProviderId") String identityProviderId,
                                      @PathParam("certificateId") String certificateId) {
        if(!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.deleteIdentityProviderCert(httpHeaders, authToken, identityProviderId, certificateId).build();
    }

    @PUT
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/mapping")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateIdentityProviderPolicy(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("identityProviderId") String identityProviderId,
            String policy) {
        if (!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()) {
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.updateIdentityProviderPolicy(httpHeaders, authToken, identityProviderId, policy).build();
    }

    @GET
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/mapping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIdentityProviderPolicy(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("identityProviderId") String identityProviderId)  {
        if(!identityConfig.getReloadableConfig().isIdentityProviderManagementSupported()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.getIdentityProviderPolicy(httpHeaders, authToken, identityProviderId).build();
    }

    @GET
    @Path("tokens/{tokenId}")
    public Response validateToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tokenId") String tokenId,
            @QueryParam("belongsTo") String belongsTo) {
        return cloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo).build();
    }

    @HEAD
    @Path("tokens/{tokenId}")
    public Response checkToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tokenId") String tokenId,
            @QueryParam("belongsTo") String belongsTo) {
        return cloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo).build();
    }

    @GET
    @Path("tokens/{tokenId}/endpoints")
    public Response listEndpointsForToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("apply_rcn_roles") boolean applyRcnRoles,
            @PathParam("tokenId") String tokenId) {
        return cloud20Service.listEndpointsForToken(httpHeaders, authToken, tokenId, applyRcnRoles).build();
    }

    @GET
    @Path("RAX-AUTH/default-region/services")
    public Response listDefaultRegionServices(
            @HeaderParam(X_AUTH_TOKEN) String authToken){
        return cloud20Service.listDefaultRegionServices(authToken).build();
    }

    @PUT
    @Path("RAX-AUTH/default-region/services")
    public Response setDefaultRegionServices(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                             DefaultRegionServices defaultRegionServices){
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.setDefaultRegionServices(authToken, defaultRegionServices).build();
        }
    }

    @POST
    @Path("RAX-AUTH/impersonation-tokens")
    public Response impersonate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            ImpersonationRequest impersonationRequest) {
        return cloud20Service.impersonate(httpHeaders, authToken, impersonationRequest).build();
    }


    @POST
    @Path("RAX-AUTH/domains")
    public Response addDomain(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Domain domain) {
        return cloud20Service.addDomain(authToken, uriInfo, domain).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}")
    public Response getDomainById(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return cloud20Service.getDomain(authToken, domainId).build();
    }

    @PUT
    @Path("RAX-AUTH/domains/{domainId}")
    public Response updateDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            Domain domain) {
        return cloud20Service.updateDomain(authToken, domainId, domain).build();
    }

    @DELETE
    @Path("RAX-AUTH/domains/{domainId}")
    public Response deleteDomainById(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return cloud20Service.deleteDomain(authToken, domainId).build();
    }

    @PUT
    @Path("RAX-AUTH/domains/{domainId}/password-policy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDomainPasswordPolicy(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            String policy) {
        if (!identityConfig.getReloadableConfig().isPasswordPolicyServicesEnabled()) {
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.updateDomainPasswordPolicy(httpHeaders, authToken, domainId, policy).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}/password-policy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDomainPasswordPolicy(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId)  {
        if(!identityConfig.getReloadableConfig().isPasswordPolicyServicesEnabled()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.getDomainPasswordPolicy(httpHeaders, authToken, domainId).build();
    }

    @DELETE
    @Path("RAX-AUTH/domains/{domainId}/password-policy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDomainPasswordPolicy(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId)  {
        if(!identityConfig.getReloadableConfig().isPasswordPolicyServicesEnabled()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.deleteDomainPasswordPolicy(httpHeaders, authToken, domainId).build();
    }

    @POST
    @Path("/users/RAX-AUTH/change-pwd")
    public Response changeUserPassword(
            @Context HttpHeaders httpHeaders,
            ChangePasswordCredentials changePasswordCredentials) {
        if (!identityConfig.getReloadableConfig().isPasswordPolicyServicesEnabled()) {
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.changeUserPassword(httpHeaders, changePasswordCredentials).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}/tenants")
    public Response getDomainTenantsByDomainId(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @QueryParam("enabled") String enabled) {
        return cloud20Service.getDomainTenants(authToken, domainId, enabled).build();
    }

    @PUT
    @Path("RAX-AUTH/domains/{domainId}/tenants/{tenantId}")
    public Response addTenantToDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.addTenantToDomain(authToken, domainId, tenantId).build();
    }

    @DELETE
    @Path("RAX-AUTH/domains/{domainId}/tenants/{tenantId}")
    public Response removeTenantFromDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.removeTenantFromDomain(authToken, domainId, tenantId).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}/users")
    public Response getUsersByDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @QueryParam("enabled") String enabled) {
        return cloud20Service.getUsersByDomainIdAndEnabledFlag(authToken, domainId, enabled).build();
    }

    @PUT
    @Path("RAX-AUTH/domains/{domainId}/users/{userId}")
    public Response addUserToDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("userId") String userId) throws IOException, JAXBException {
        return cloud20Service.addUserToDomain(authToken, domainId, userId).build();
    }

    @GET
    @Path("RAX-AUTH/domains/{domainId}/endpoints")
    public Response getEndpointsByDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return cloud20Service.getEndpointsByDomainId(authToken, domainId).build();
    }

    @GET
    @Path("RAX-AUTH/domains")
    public Response getAccessibleDomains(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return cloud20Service.getAccessibleDomains(uriInfo, authToken, validateMarker(marker), validateLimit(limit)).build();
    }

    @GET
    @Path("extensions")
    public Response listExtensions(@Context HttpHeaders httpHeaders) {
        return cloud20Service.listExtensions(httpHeaders).build();
    }

    @GET
    @Path("extensions/{alias}")
    public Response getExtension(@Context HttpHeaders httpHeaders, @PathParam("alias") String alias) {
        return cloud20Service.getExtension(httpHeaders, alias).build();
    }

    @GET
    @Path("users")
    public Response getUserByName(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("email") String email,
            @QueryParam("name") String name,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        if (!StringUtils.isBlank(name)) {
            return cloud20Service.getUserByName(httpHeaders, authToken, name).build();
        } else if (!StringUtils.isBlank(email)) {
            return cloud20Service.getUsersByEmail(httpHeaders, authToken, email).build();
        } else {
            return cloud20Service.listUsers(httpHeaders, uriInfo, authToken, validateMarker(marker), validateLimit(limit)).build();
        }
    }

    @GET
    @Path("users/{userId}")
    public Response getUserById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserById(httpHeaders, authToken, userId).build();
    }

    @GET
    @Path("users/{userId}/roles")
    public Response listUserGlobalRoles(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @QueryParam("serviceId") String serviceId,
            @QueryParam("apply_rcn_roles") boolean applyRcnRoles) {
        if (!StringUtils.isBlank(serviceId)) {
            return cloud20Service.listUserGlobalRolesByServiceId(httpHeaders, authToken, userId, serviceId, applyRcnRoles).build();
        } else {
            return cloud20Service.listUserGlobalRoles(httpHeaders, authToken, userId, applyRcnRoles).build();
        }
    }

    @DELETE
    @Path("users/{userId}/roles")
    public Response deleteUserRoles(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @QueryParam("type") String roleType) {
        return cloud20Service.deleteUserRoles(httpHeaders, authToken, userId, roleType).build();
    }

    @GET
    @Path("users/{userId}/RAX-AUTH/domains")
    public Response getAccessibleDomainsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getAccessibleDomainsForUser(authToken, userId).build();
    }

    @GET
    @Path("users/{userId}/RAX-AUTH/domains/{domainId}/endpoints")
    public Response getAccessibleDomainsEndpointsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("domainId") String domainId) {
        return cloud20Service.getAccessibleDomainsEndpointsForUser(authToken, userId, domainId).build();
    }

    @GET
    @Path("users/{userId}/RAX-AUTH/admins")
    public Response getUserAdminsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserAdminsForUser(authToken, userId).build();
    }

    @GET
    @Path("tenants")
    public Response listTenantsAndGetTenantByName(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        // Note: getTenantByName only available to admin
        if (!StringUtils.isBlank(name)) {
            return cloud20Service.getTenantByName(httpHeaders, authToken, name).build();
        } else {
            return cloud20Service.listTenants(httpHeaders, authToken, validateMarker(marker), validateLimit(limit)).build();
        }
    }

    @GET
    @Path("tenants/{tenantId}")
    public Response getTenantById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantsId) {
        return cloud20Service.getTenantById(httpHeaders, authToken, tenantsId).build();
    }

    @GET
    @Path("tenants/{tenantId}/RAX-AUTH/admins")
    public Response listUserAdminsOnTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.getUserByTenantId(httpHeaders, authToken, tenantId).build();
    }

    @GET
    @Path("tenants/{tenantId}/users/{userId}/roles")
    public Response listRolesForUserOnTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("userId") String userId,
            @QueryParam("apply_rcn_roles") boolean applyRcnRoles) {
        return cloud20Service.listRolesForUserOnTenant(httpHeaders, authToken, tenantId, userId, applyRcnRoles).build();
    }

    @POST
    @Path("users")
    public Response addUser(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, UserForCreate user) {
        return cloud20Service.addUser(httpHeaders, uriInfo, authToken, user).build();
    }

    @POST
    @Path("users/{userId}")
    public Response updateUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, UserForCreate user) {
        return cloud20Service.updateUser(httpHeaders, authToken, userId, user).build();
    }

    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.deleteUser(httpHeaders, authToken, userId).build();
    }

    @PUT
    @Path("users/{userId}/OS-KSADM/enabled")
    public Response setUserEnabled(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, User user) {
        return cloud20Service.setUserEnabled(httpHeaders, authToken, userId, user).build();
    }

    @GET
    @Path("users/{userId}/RAX-KSGRP")
    public Response listUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.listUserGroups(httpHeaders, authToken, userId).build();
    }

    @PUT
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return cloud20Service.addUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @GET
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response getUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return cloud20Service.getUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @DELETE
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return cloud20Service.deleteUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response addUserCredential(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, String body) {
        return cloud20Service.addUserCredential(httpHeaders, uriInfo, authToken, userId, body).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response listCredentials(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return cloud20Service.listCredentials(httpHeaders, authToken, userId, validateMarker(marker), validateLimit(limit)).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response updateUserPasswordCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        PasswordCredentialsBase creds) {
        return cloud20Service.updateUserPasswordCredentials(httpHeaders, authToken, userId, JSONConstants.PASSWORD_CREDENTIALS, creds).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)
    public Response updateUserApiKeyCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, ApiKeyCredentials creds) {
        return cloud20Service.updateUserApiKeyCredentials(httpHeaders, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS, creds).build();
    }

    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS + "/RAX-AUTH/reset")
    public Response resetUserApiKeyCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) {
        return cloud20Service.resetUserApiKeyCredentials(httpHeaders, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)
    public Response getUserCredentialKey(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserApiKeyCredentials(httpHeaders, authToken, userId).build();
    }

    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response getUserCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserPasswordCredentials(httpHeaders, authToken, userId).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)
    public Response deleteUserKeyCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.deleteUserCredential(httpHeaders, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build();
    }

    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response deleteUserCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.deleteUserCredential(httpHeaders, authToken, userId, JSONConstants.PASSWORD_CREDENTIALS).build();
    }

    @POST
    @Path("tenants")
    public Response addTenant(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, Tenant tenant) {
        return cloud20Service.addTenant(httpHeaders, uriInfo, authToken, tenant).build();
    }

    @POST
    @Path("tenants/{tenantId}")
    public Response updateTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId, Tenant tenant) {
        return cloud20Service.updateTenant(httpHeaders, authToken, tenantId, tenant).build();
    }

    @DELETE
    @Path("tenants/{tenantId}")
    public Response deleteTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.deleteTenant(httpHeaders, authToken, tenantId).build();
    }

    @GET
    @Path("tenants/{tenantId}/OS-KSADM/roles")
    public Response listRolesForTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return cloud20Service.listRolesForTenant(httpHeaders, authToken, tenantId, validateMarker(marker), validateLimit(limit)).build();
    }

    @GET
    @Path("tenants/{tenantId}/users")
    public Response listUsersForTenantAndListUsersWithRoleForTenant(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("roleId") String roleId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        if (roleId != null) {
            return cloud20Service.listUsersWithRoleForTenant(httpHeaders, uriInfo, authToken, tenantId, roleId, validateMarker(marker), validateLimit(limit)).build();
        } else {
            return cloud20Service.listUsersForTenant(httpHeaders, uriInfo, authToken, tenantId, validateMarker(marker), validateLimit(limit)).build();
        }
    }

    @PUT
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addRolesToUserOnTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return cloud20Service.addRolesToUserOnTenant(httpHeaders, authToken, tenantId, userId, roleId).build();
    }

    @DELETE
    @Path("tenants/{tenantId}/users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteRoleFromUserOnTenant(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("tenantId") String tenantId,
        @PathParam("userId") String userId,
        @PathParam("roleId") String roleId) {
        return cloud20Service.deleteRoleFromUserOnTenant(httpHeaders, authToken, tenantId, userId, roleId).build();
    }

    @GET
    @Path("OS-KSADM/roles")
    public Response listRoles(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("serviceId") String serviceId,
            @QueryParam("roleName") String roleName,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return cloud20Service.listRoles(httpHeaders, uriInfo, authToken, serviceId, roleName, validateMarker(marker), validateLimit(limit)).build();
    }

    @POST
    @Path("OS-KSADM/roles")
    public Response addRole(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Role role) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.addRole(httpHeaders, uriInfo, authToken, role).build();
        }
    }

    @GET
    @Path("OS-KSADM/roles/{roleId}")
    public Response getRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("roleId") String roleId) {
        return cloud20Service.getRole(httpHeaders, authToken, roleId).build();
    }

    @DELETE
    @Path("OS-KSADM/roles/{roleId}")
    public Response deleteRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("roleId") String roleId) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.deleteRole(httpHeaders, authToken, roleId).build();
        }
    }

    @GET
    @Path("OS-KSADM/roles/{roleId}/RAX-AUTH/users")
    public Response listUsersWithRole(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("roleId") String roleId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return cloud20Service.listUsersWithRole(httpHeaders, uriInfo, authToken, roleId, validateMarker(marker), validateLimit(limit)).build();
    }

    @GET
    @Path("OS-KSADM/services")
    public Response listServices(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit)
        {
        return cloud20Service.listServices(httpHeaders, authToken, name, validateMarker(marker), validateLimit(limit)).build();
    }

    @POST
    @Path("OS-KSADM/services")
    public Response addService(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, Service service) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.addService(httpHeaders, uriInfo, authToken, service).build();
        }
    }

    @GET
    @Path("OS-KSADM/services/{serviceId}")
    public Response getService(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("serviceId") String serviceId) {
        return cloud20Service.getService(httpHeaders, authToken, serviceId).build();
    }

    @DELETE
    @Path("OS-KSADM/services/{serviceId}")
    public Response deleteService(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("serviceId") String serviceId) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.deleteService(httpHeaders, authToken, serviceId).build();
        }
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response listEndpointTemplates(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("serviceId") String serviceId) {
        return cloud20Service.listEndpointTemplates(httpHeaders, authToken, serviceId).build();
    }

    @POST
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response addEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, EndpointTemplate endpoint) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.addEndpointTemplate(httpHeaders, uriInfo, authToken, endpoint).build();
        }
    }

    @PUT
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response updateEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("endpointTemplateId") String endpointTemplateId,
            @HeaderParam(X_AUTH_TOKEN) String authToken, EndpointTemplate endpoint) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.updateEndpointTemplate(httpHeaders, uriInfo, authToken, endpointTemplateId, endpoint).build();
        }
    }

    @GET
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response getEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId) {
        return cloud20Service.getEndpointTemplate(httpHeaders, authToken, endpointTemplateId).build();
    }

    @DELETE
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response deleteEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String enpdointTemplateId) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.deleteEndpointTemplate(httpHeaders, authToken, enpdointTemplateId).build();
        }
    }

    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response listEndpoints(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.listEndpoints(httpHeaders, authToken, tenantId).build();
    }

    @POST
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response addEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId, EndpointTemplate endpoint) {
        return cloud20Service.addEndpoint(httpHeaders, authToken, tenantId, endpoint).build();
    }

    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response getEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("endpointId") String endpointId) {
        return cloud20Service.getEndpoint(httpHeaders, authToken, tenantId, endpointId).build();
    }

    @DELETE
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response deleteEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("endpointId") String endpointId) {
        return cloud20Service.deleteEndpoint(httpHeaders, authToken, tenantId, endpointId).build();
    }

    @GET
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response getSecretQA(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getSecretQA(httpHeaders, authToken, userId).build();
    }

    @PUT
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response updateSecretQA(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, SecretQA secrets) {
        return cloud20Service.updateSecretQA(httpHeaders, authToken, userId, secrets).build();
    }

    @GET
    @Path("users/{userId}/RAX-AUTH/secretqas")
    public Response getSecretQAs(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId){
        return cloud20Service.getSecretQAs(authToken, userId).build();
    }

    @POST
    @Path("users/{userId}/RAX-AUTH/secretqas")
    public Response createSecretQA(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQA){
        return cloud20Service.createSecretQA(authToken, userId, secretQA).build();
    }

    // ******************************************************* //
    // RAX-GRPADM Extension //
    // ******************************************************* //

    @POST
    @Path("/RAX-GRPADM/groups")
    public Response addGroup(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Group group) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.addGroup(httpHeaders, uriInfo, authToken, group).build();
        }
    }

    @GET
    @Path("/RAX-GRPADM/groups")
    public Response getGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String groupName,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        if(groupName != null){
            return cloud20Service.getGroup(httpHeaders, authToken, groupName).build();
        }
        return cloud20Service.listGroups(httpHeaders, authToken, groupName, validateMarker(marker), validateLimit(limit)).build();
    }

    @GET
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response getGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId) {
        return cloud20Service.getGroupById(httpHeaders, authToken, groupId).build();
    }

    @PUT
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response updateGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            Group group) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.updateGroup(httpHeaders, authToken, groupId, group).build();
        }
    }

    @DELETE
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response deleteGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.deleteGroup(httpHeaders, authToken, groupId).build();
        }
    }

    @GET
    @Path("/RAX-GRPADM/groups/{groupId}/users")
    public Response getUsersFromGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        return cloud20Service.getUsersForGroup(httpHeaders, authToken, groupId, validateMarker(marker), validateLimit(limit)).build();
    }

    @PUT
    @Path("/RAX-GRPADM/groups/{groupId}/users/{userId}")
    public Response putUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @PathParam("userId") String userId) {
        return cloud20Service.addUserToGroup(httpHeaders, authToken, groupId, userId).build();
    }

    @DELETE
    @Path("/RAX-GRPADM/groups/{groupId}/users/{userId}")
    public Response deleteUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @PathParam("userId") String userId) {
        return cloud20Service.removeUserFromGroup(httpHeaders, authToken, groupId, userId).build();
    }

    @POST
    @Path("RAX-AUTH/regions")
    public Response createRegion(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, Region region) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.addRegion(uriInfo, authToken, region).build();
        }
    }

    @GET
    @Path("RAX-AUTH/regions/{name}")
    public Response getRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name) {
        return cloud20Service.getRegion(authToken, name).build();
    }

    @GET
    @Path("RAX-AUTH/regions")
    public Response getRegions(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return cloud20Service.getRegions(authToken).build();
    }

    @PUT
    @Path("RAX-AUTH/regions/{name}")
    public Response updateRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name, Region region) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.updateRegion(authToken, name, region).build();
        }
    }

    @DELETE
    @Path("RAX-AUTH/regions/{name}")
    public Response deleteRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.deleteRegion(authToken, name).build();
        }
    }

    @POST
    @Path("RAX-AUTH/secretqa/questions")
    public Response createQuestion(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, Question question) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.addQuestion(uriInfo, authToken, question).build();
        }
    }

    @GET
    @Path("RAX-AUTH/secretqa/questions/{questionId}")
    public Response getQuestion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("questionId") String questionId) {
        return cloud20Service.getQuestion(authToken, questionId).build();
    }

    @GET
    @Path("RAX-AUTH/secretqa/questions")
    public Response getQuestions(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return cloud20Service.getQuestions(authToken).build();
    }

    @PUT
    @Path("RAX-AUTH/secretqa/questions/{name}")
    public Response updateQuestion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name, Question question) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.updateQuestion(authToken, name, question).build();
        }
    }

    @DELETE
    @Path("RAX-AUTH/secretqa/questions/{questionId}")
    public Response deleteQuestion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("questionId") String questionId) {
        if (identityConfig.getReloadableConfig().migrationReadOnlyEnabled()) {
            return exceptionHandler.exceptionResponse(new MigrationReadOnlyIdmException()).build();
        } else {
            return cloud20Service.deleteQuestion(authToken, questionId).build();
        }
    }

    @POST
    @Path("RAX-AUTH/tenant-types")
    public Response createTenantType(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, TenantType tenantType) {
        return cloud20Service.addTenantType(uriInfo, authToken, tenantType).build();
    }

    @GET
    @Path("RAX-AUTH/tenant-types")
    public Response listTenantTypes(@Context UriInfo uriInfo,
                                   @HeaderParam(X_AUTH_TOKEN) String authToken,
                                   @QueryParam("marker") Integer marker,
                                   @QueryParam("limit") Integer limit) {
        return cloud20Service.listTenantTypes(uriInfo, authToken, validateMarker(marker), validateLimit(limit)).build();
    }

    @GET
    @Path("RAX-AUTH/tenant-types/{tenantTypeName}")
    public Response getTenantTypes(@Context UriInfo uriInfo,
                                   @HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("tenantTypeName") String tenantTypeName) {
        return cloud20Service.getTenantType(uriInfo, authToken, tenantTypeName).build();
    }

    @DELETE
    @Path("RAX-AUTH/tenant-types/{tenantTypeName}")
    public Response deleteTenantType(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("tenantTypeName") String tenantTypeName) {
        return cloud20Service.deleteTenantType(authToken, tenantTypeName).build();
    }

    @Path("users/{userId}/RAX-AUTH/multi-factor")
    public CloudMultifactorResource getMultifactorResource() {
        return multifactorResource;
    }

    @Path("RAX-AUTH/domains/{domainId}/multi-factor")
    public CloudMultifactorDomainResource getMultifactorDomainResource() {
        return multifactorDomainResource;
    }

    @Path("OS-KSCATALOG/endpointTemplates/RAX-AUTH/rules")
    public EndpointAssignmentRuleResource getEndpointAssignmentRuleResource() {
        return endpointAssignmentRuleResource;
    }

    protected int validateMarker(Integer offset) {
        if (offset == null) {
            return 0;
        }
        if (offset < 0) {
            throw new BadRequestException("Marker must be non negative");
        }
        return offset;
    }

    protected int validateLimit(Integer limit) {
        if (limit == null) {
            return config.getInt("ldap.paging.limit.default");
        }
        if (limit < 0) {
            throw new BadRequestException("Limit must be non negative");
        } else if (limit == 0) {
            return config.getInt("ldap.paging.limit.default");
        } else if (limit > config.getInt("ldap.paging.limit.max")) {
            return config.getInt("ldap.paging.limit.max");
        } else {
            return limit;
        }
    }

    public void setCloud20Service(DefaultCloud20Service cloud20Service) {
        this.cloud20Service = cloud20Service;
    }

    public void setIdentityConfig(IdentityConfig identityConfig) {
        this.identityConfig = identityConfig;
    }
}



