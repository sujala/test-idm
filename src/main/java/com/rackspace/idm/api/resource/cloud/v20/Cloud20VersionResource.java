package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ChangePasswordCredentials;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainAdministratorChange;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordReset;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificate;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.IdentityProviderConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.XMLReader;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.User.UserType;
import com.rackspace.idm.event.ApiKeyword;
import com.rackspace.idm.event.ApiResourceType;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.event.NewRelicApiEventListener;
import com.rackspace.idm.event.ReportableQueryParams;
import com.rackspace.idm.event.SecureResourcePath;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UnsupportedMediaTypeException;
import com.rackspace.idm.modules.endpointassignment.api.resource.EndpointAssignmentRuleResource;
import com.rackspace.idm.modules.usergroups.api.resource.CloudUserGroupResource;
import com.rackspace.idm.util.QueryParamConverter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;
import org.w3c.dom.Document;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

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

    public static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @Autowired
    private DefaultCloud20Service cloud20Service;

    @Autowired
    private CloudMultifactorResource multifactorResource;

    @Autowired
    private CloudMultifactorDomainResource multifactorDomainResource;

    @Autowired
    private EndpointAssignmentRuleResource endpointAssignmentRuleResource;

    @Autowired
    private CloudUserGroupResource userGroupResource;

    @Autowired
    private DelegationAgreementResource delegationAgreementResource;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private IdentityProviderConverterCloudV20 identityProviderConverterCloudV20;

    private static final String JAXBCONTEXT_VERSION_CHOICE_CONTEXT_PATH = "org.openstack.docs.common.api.v1:org.w3._2005.atom";
    private static final String SERVICE_NOT_FOUND_ERROR_MESSAGE = "Service Not Found";
    private static final JAXBContext JAXBCONTEXT_VERSION_CHOICE;

    public static final String FEDERATION_IDP_MAPPING_POLICY_FORMAT_ERROR_MESSAGE = "Acceptable media types for IDP mapping policy are: %s";

    public static final String DOMAIN_ID_PATH_PARAM_NAME = "domainId";
    public static final String GROUP_ID_PATH_PARAM_NAME = "groupId";
    public static final String USER_ID_PATH_PARAM_NAME = "userId";

    static {
        try {
            JAXBCONTEXT_VERSION_CHOICE = JAXBContext.newInstance(JAXBCONTEXT_VERSION_CHOICE_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new IdmException("Error initializing JAXBContext for versionchoice", e);
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name="v2.0 Get version")
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

    @IdentityApi(apiResourceType = ApiResourceType.AUTH, name=GlobalConstants.V2_AUTHENTICATE)
    @ReportableQueryParams(unsecuredQueryParams = {"apply_rcn_roles"})
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

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name="v2.0 Authenticate forgot password")
    @POST
    @Path("/users/RAX-AUTH/forgot-pwd")
    public Response authenticateForForgotPassword(@Context HttpHeaders httpHeaders, ForgotPasswordCredentials forgotPasswordCredentials) {
        return cloud20Service.authenticateForForgotPassword(httpHeaders, forgotPasswordCredentials).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Reset password")
    @POST
    @Path("/users/RAX-AUTH/pwd-reset")
    public Response passwordReset(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            PasswordReset passwordReset) {
        return cloud20Service.passwordReset(httpHeaders, authToken, passwordReset).build();
    }

    /**
     * The Racksace extension for retrieving the roles for a user. Retrieves all roles the user *effectively* has based
     * on applying all logic rules for determining a user's roles (e.g. user group membership, roles assigned to user,
     * implicit roles, rcn logic, etc)
     *
     * @return
     */
    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name=GlobalConstants.V2_LIST_EFFECTIVE_ROLES_FOR_USER)
    @GET
    @Path("/users/{userId}/RAX-AUTH/roles")
    public Response listEffectiveRolesForUser(@Context HttpHeaders httpHeaders,
                                              @HeaderParam(X_AUTH_TOKEN) String authToken,
                                              @PathParam("userId") String userId,
                                              @QueryParam("onTenantId") String onTenantId) throws IOException, JAXBException {
        return cloud20Service.listEffectiveRolesForUser(httpHeaders, authToken, userId, new ListEffectiveRolesForUserParams(onTenantId)).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Revoke token")
    @DELETE
    @Path("tokens")
    public Response revokeToken(@Context HttpHeaders httpHeaders, @HeaderParam(X_AUTH_TOKEN) String authToken) throws IOException, JAXBException {
        return cloud20Service.revokeToken(httpHeaders, authToken).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Revoke token by id")
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v2TokenValidationAbsolutePathPatternRegex)
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
    @IdentityApi(apiResourceType = ApiResourceType.AUTH, name="v2.0 Federated authenticate legacy xml")
    @Deprecated
    @POST
    @Path("RAX-AUTH/saml-tokens")
    @Consumes({MediaType.APPLICATION_XML})
    public Response authenticateSamlResponse(@Context HttpHeaders httpHeaders, String samlResponse)  {
        return federationSamlAuthenticationRawXML(httpHeaders, samlResponse, false);
    }

    @IdentityApi(apiResourceType = ApiResourceType.AUTH, name="v2.0 Federated authenticate legacy encoded")
    @Deprecated
    @POST
    @Path("RAX-AUTH/saml-tokens")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    public Response authenticateSamlResponseFormEncoded(@Context HttpHeaders httpHeaders, @FormParam("SAMLResponse") String samlResponse)  {
        return federationSamlAuthenticationFormEncoded(httpHeaders, samlResponse, false);
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
    @IdentityApi(apiResourceType = ApiResourceType.AUTH, name="v2.0 Federated authenticate encoded")
    @ReportableQueryParams(unsecuredQueryParams = {"apply_rcn_roles"})
    @POST
    @Path("RAX-AUTH/federation/saml/auth")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response federationSamlAuthenticationFormEncoded(@Context HttpHeaders httpHeaders,
                                                            @FormParam("SAMLResponse") String samlResponse,
                                                            @QueryParam("apply_rcn_roles") boolean applyRcnRoles)  {
        Response response;
        if (org.apache.commons.lang.StringUtils.isBlank(samlResponse)) {
            response = exceptionHandler.exceptionResponse(new BadRequestException("Missing SAMLResponse field")).build();
        } else {
            try {
                byte[] samlResponseBytes = Base64.decodeBase64(samlResponse);
                response = cloud20Service.authenticateFederated(httpHeaders, samlResponseBytes, applyRcnRoles).build();
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
    @IdentityApi(apiResourceType = ApiResourceType.AUTH, name="v2.0 Federated authenticate xml")
    @ReportableQueryParams(unsecuredQueryParams = {"apply_rcn_roles"})
    @Deprecated
    @POST
    @Path("RAX-AUTH/federation/saml/auth")
    @Consumes({MediaType.APPLICATION_XML})
    public Response federationSamlAuthenticationRawXML(@Context HttpHeaders httpHeaders,
                                                       String samlResponse,
                                                       @QueryParam("apply_rcn_roles") boolean applyRcnRoles)  {
        Response response;
        if (org.apache.commons.lang.StringUtils.isBlank(samlResponse)) {
            response = exceptionHandler.exceptionResponse(new BadRequestException("Must provide SAMLResponse XML in body")).build();
        } else {
            try {
                byte[] samlResponseBytes = org.apache.commons.codec.binary.StringUtils.getBytesUtf8(samlResponse);
                response = cloud20Service.authenticateFederated(httpHeaders, samlResponseBytes, applyRcnRoles).build();
            } catch (Exception ex) {
                response = exceptionHandler.exceptionResponse(ex).build();
            }
        }

        return response;
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Federated logout")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Federated SAML validity check")
    @POST
    @Path("RAX-AUTH/federation/saml/validate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response federationSamlValidityCheck(@Context HttpHeaders httpHeaders, @FormParam("SAMLRequest") String request)  {
        Response response;
        if (org.apache.commons.lang.StringUtils.isBlank(request)) {
            response = exceptionHandler.exceptionResponse(new BadRequestException("Missing SAMLRequest field")).build();
        } else {
            try {
                byte[] requestBytes = Base64.decodeBase64(request);
                response = cloud20Service.verifySamlRequest(httpHeaders, requestBytes).build();
            } catch (Exception ex) {
                response = exceptionHandler.exceptionResponse(ex).build();
            }
        }
        return response;
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Create IDP")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("RAX-AUTH/federation/identity-providers")
    public Response addIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , IdentityProvider identityProvider) {
        return cloud20Service.addIdentityProvider(httpHeaders, uriInfo, authToken, identityProvider).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Create IDP with metadata")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Path("RAX-AUTH/federation/identity-providers")
    public Response addIdentityProviderXML(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , String requestBody) throws IOException {
        byte[] bytes = org.apache.commons.codec.binary.StringUtils.getBytesUtf8(requestBody);
        try {
            Document xmlDocument = identityProviderConverterCloudV20.getXMLDocument(bytes);
            if (xmlDocument.getDocumentElement().getLocalName().equals(JSONConstants.IDENTITY_PROVIDER)) {
                XMLReader xmlReader = new XMLReader();
                IdentityProvider identityProvider = (IdentityProvider) xmlReader.readFrom(Object.class, IdentityProvider.class, null, null, null, new ByteArrayInputStream(bytes));
                return cloud20Service.addIdentityProvider(httpHeaders, uriInfo, authToken, identityProvider).build();
            }

            return cloud20Service.addIdentityProviderUsingMetadata(httpHeaders, uriInfo, authToken, bytes).build();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update IDP")
    @PUT
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}")
    public Response updateIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , @PathParam("identityProviderId") String identityProviderId
            , IdentityProvider identityProvider) {
        return cloud20Service.updateIdentityProvider(httpHeaders, uriInfo, authToken, identityProviderId, identityProvider).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get IDP")
    @GET
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}")
    public Response getIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , @PathParam("identityProviderId") String identityProviderId)  {
        return cloud20Service.getIdentityProvider(httpHeaders, authToken, identityProviderId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List IDP")
    @ReportableQueryParams(unsecuredQueryParams = {"name","issuer","idpType","approvedDomainId","approvedTenantId"}, securedQueryParams = {"emailDomain"})
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
            @QueryParam("approvedTenantId") String approvedTenantId,
            @QueryParam("emailDomain") String emailDomain) {
        return cloud20Service.getIdentityProviders(httpHeaders, authToken, new IdentityProviderSearchParams(name, issuer, approvedDomainId, approvedTenantId, idpType, emailDomain)).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete IDP")
    @DELETE
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}")
    public Response deleteIdentityProvider(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , @PathParam("identityProviderId") String identityProviderId) {
        return cloud20Service.deleteIdentityProvider(httpHeaders, authToken, identityProviderId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get IDP metadata")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/metadata")
    public Response getIdentityProviderMetadata(
            @Context HttpHeaders httpHeaders
            , @Context UriInfo uriInfo
            , @HeaderParam(X_AUTH_TOKEN) String authToken
            , @PathParam("identityProviderId") String identityProviderId) {
        return cloud20Service.getIdentityProvidersMetadata(httpHeaders, authToken, identityProviderId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update IDP with metadata")
    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/metadata")
    public Response updateIdentityProviderUsingMetadata(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("identityProviderId") String identityProviderId,
            String requestBody) {
        return cloud20Service.updateIdentityProviderUsingMetadata(httpHeaders, uriInfo, authToken,
                identityProviderId, org.apache.commons.codec.binary.StringUtils.getBytesUtf8(requestBody)).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add IDP certificate")
    @PUT
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/certificates")
    public Response addCertToIdp(@Context HttpHeaders httpHeaders,
                                 @HeaderParam(X_AUTH_TOKEN) String authToken,
                                 @PathParam("identityProviderId") String identityProviderId,
                                 PublicCertificate publicCertificate) {
        return cloud20Service.addIdentityProviderCert(httpHeaders, authToken, identityProviderId, publicCertificate).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete IDP certificate")
    @DELETE
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/certificates/{certificateId}")
    public Response deleteCertFromIdp(@Context HttpHeaders httpHeaders,
                                      @HeaderParam(X_AUTH_TOKEN) String authToken,
                                      @PathParam("identityProviderId") String identityProviderId,
                                      @PathParam("certificateId") String certificateId) {
        return cloud20Service.deleteIdentityProviderCert(httpHeaders, authToken, identityProviderId, certificateId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update IDP mapping policy")
    @PUT
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/mapping")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, GlobalConstants.TEXT_YAML})
    public Response updateIdentityProviderPolicy(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("identityProviderId") String identityProviderId,
            String policy) {
        Set acceptableMediaTypes = identityConfig.getReloadableConfig().getMappingPolicyAcceptFormats();
        if (!acceptableMediaTypes.contains(httpHeaders.getMediaType().toString().toLowerCase())) {
            String errMsg = String.format(FEDERATION_IDP_MAPPING_POLICY_FORMAT_ERROR_MESSAGE, acceptableMediaTypes);
            throw new UnsupportedMediaTypeException(errMsg);
        }
        return cloud20Service.updateIdentityProviderPolicy(httpHeaders, authToken, identityProviderId, policy).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get IDP mapping policy")
    @GET
    @Path("RAX-AUTH/federation/identity-providers/{identityProviderId}/mapping")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, GlobalConstants.TEXT_YAML})
    public Response getIdentityProviderPolicy(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("identityProviderId") String identityProviderId)  {
        return cloud20Service.getIdentityProviderPolicy(httpHeaders, authToken, identityProviderId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name=GlobalConstants.V2_VALIDATE_TOKEN)
    @ReportableQueryParams(unsecuredQueryParams = {"belongsTo","apply_rcn_roles"})
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v2TokenValidationAbsolutePathPatternRegex)
    @GET
    @Path("tokens/{tokenId}")
    public Response validateToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tokenId") String tokenId,
            @QueryParam("belongsTo") String belongsTo,
            @QueryParam("apply_rcn_roles") boolean applyRcnRoles) {
        if (applyRcnRoles) {
            return cloud20Service.validateTokenApplyRcnRoles(httpHeaders, authToken, tokenId, belongsTo).build();
        } else {
            return cloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Check token")
    @ReportableQueryParams(unsecuredQueryParams = {"belongsTo","apply_rcn_roles"})
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v2TokenValidationAbsolutePathPatternRegex)
    @HEAD
    @Path("tokens/{tokenId}")
    public Response checkToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tokenId") String tokenId,
            @QueryParam("belongsTo") String belongsTo,
            @QueryParam("apply_rcn_roles") boolean applyRcnRoles) {
        if (applyRcnRoles) {
            return cloud20Service.validateTokenApplyRcnRoles(httpHeaders, authToken, tokenId, belongsTo).build();
        } else {
            return cloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name=GlobalConstants.V2_LIST_TOKEN_ENDPOINTS)
    @ReportableQueryParams(unsecuredQueryParams = {"apply_rcn_roles"})
    @SecureResourcePath(regExPattern = NewRelicApiEventListener.v2TokenEndpointAbsolutePathPatternRegex)
    @GET
    @Path("tokens/{tokenId}/endpoints")
    public Response listEndpointsForToken(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("apply_rcn_roles") boolean applyRcnRoles,
            @PathParam("tokenId") String tokenId) {
        return cloud20Service.listEndpointsForToken(httpHeaders, authToken, tokenId, applyRcnRoles).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List default region services")
    @GET
    @Path("RAX-AUTH/default-region/services")
    public Response listDefaultRegionServices(
            @HeaderParam(X_AUTH_TOKEN) String authToken){
        return cloud20Service.listDefaultRegionServices(authToken).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Set default region services")
    @PUT
    @Path("RAX-AUTH/default-region/services")
    public Response setDefaultRegionServices(@HeaderParam(X_AUTH_TOKEN) String authToken,
                                             DefaultRegionServices defaultRegionServices){
        return cloud20Service.setDefaultRegionServices(authToken, defaultRegionServices).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Impersonate")
    @POST
    @Path("RAX-AUTH/impersonation-tokens")
    public Response impersonate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            ImpersonationRequest impersonationRequest) {
        return cloud20Service.impersonate(httpHeaders, authToken, impersonationRequest).build();
    }


    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add domain")
    @POST
    @Path("RAX-AUTH/domains")
    public Response addDomain(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Domain domain) {
        return cloud20Service.addDomain(authToken, uriInfo, domain).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get domain")
    @GET
    @Path("RAX-AUTH/domains/{domainId}")
    public Response getDomainById(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return cloud20Service.getDomain(authToken, domainId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update domain")
    @PUT
    @Path("RAX-AUTH/domains/{domainId}")
    public Response updateDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            Domain domain) {
        return cloud20Service.updateDomain(authToken, domainId, domain).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete domain")
    @DELETE
    @Path("RAX-AUTH/domains/{domainId}")
    public Response deleteDomainById(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return cloud20Service.deleteDomain(authToken, domainId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update domain password policy")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get domain password policy")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete domain password policy")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Change user password")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List tenants for domain")
    @ReportableQueryParams(unsecuredQueryParams = {"enabled"})
    @GET
    @Path("RAX-AUTH/domains/{domainId}/tenants")
    public Response getDomainTenantsByDomainId(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @QueryParam("enabled") String enabled) {
        return cloud20Service.getDomainTenants(authToken, domainId, enabled).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add tenant to domain")
    @PUT
    @Path("RAX-AUTH/domains/{domainId}/tenants/{tenantId}")
    public Response addTenantToDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.addTenantToDomain(authToken, domainId, tenantId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Remove tenant from domain")
    @DELETE
    @Path("RAX-AUTH/domains/{domainId}/tenants/{tenantId}")
    public Response removeTenantFromDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.removeTenantFromDomain(authToken, domainId, tenantId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List users by domain")
    @ReportableQueryParams(unsecuredQueryParams = {"enabled","user_type"})
    @GET
    @Path("RAX-AUTH/domains/{domainId}/users")
    public Response getUsersByDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @QueryParam("enabled") String enabled,
            @QueryParam("user_type") String userType){
        UserType userTypeEnum = QueryParamConverter.convertUserTypeParamToEnum(userType);
        return cloud20Service.getUsersByDomainIdAndEnabledFlag(authToken, domainId, enabled, userTypeEnum).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add user to domain")
    @PUT
    @Path("RAX-AUTH/domains/{domainId}/users/{userId}")
    public Response addUserToDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId,
            @PathParam("userId") String userId) throws IOException, JAXBException {
        return cloud20Service.addUserToDomain(authToken, domainId, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Change domain administrator")
    @PUT
    @Path("RAX-AUTH/domains/{domainId}/domainAdministratorChange")
    public Response modifyDomainAdministrator(
            @PathParam("domainId") String domainId,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            DomainAdministratorChange domainAdministratorChange) throws IOException, JAXBException {
        return cloud20Service.modifyDomainAdministrator(authToken, domainId, domainAdministratorChange).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add domain to RCN")
    @PUT
    @Path("RAX-AUTH/domains/{domainId}/rcn/{destinationRcn}")
    public Response switchRcnOnDomain(
            @PathParam("domainId") String domainId,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("destinationRcn") String destinationRcn) throws IOException, JAXBException {
        return cloud20Service.switchDomainRcn(authToken, domainId, destinationRcn).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List endpoints for domain")
    @GET
    @Path("RAX-AUTH/domains/{domainId}/endpoints")
    public Response getEndpointsByDomain(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("domainId") String domainId) {
        return cloud20Service.getEndpointsByDomainId(authToken, domainId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List domains")
    @ReportableQueryParams(unsecuredQueryParams = {"marker","limit","rcn"})
    @GET
    @Path("RAX-AUTH/domains")
    public Response getAccessibleDomains(
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit,
            @QueryParam("rcn") String rcn) {
        return cloud20Service.getAccessibleDomains(uriInfo, authToken, validateMarker(marker), validateLimit(limit), rcn).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PUBLIC, name="v2.0 List extensions")
    @GET
    @Path("extensions")
    public Response listExtensions(@Context HttpHeaders httpHeaders) {
        return cloud20Service.listExtensions(httpHeaders).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get extension")
    @GET
    @Path("extensions/{alias}")
    public Response getExtension(@Context HttpHeaders httpHeaders, @PathParam("alias") String alias) {
        return cloud20Service.getExtension(httpHeaders, alias).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, keywords = ApiKeyword.COSTLY, name="v2.0 Get user by name")
    @ReportableQueryParams(unsecuredQueryParams = {"marker","limit"}, securedQueryParams = {"email","name"})
    @GET
    @Path("users")
    public Response getUsers(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("email") String email,
            @QueryParam("tenant_id") String tenantId,
            @QueryParam("domain_id") String domainId,
            @QueryParam("admin_only") Boolean adminOnly,
            @QueryParam("user_type") String userType,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        ListUsersSearchParams listUsersSearchParams = new ListUsersSearchParams(
                name, email, tenantId, domainId, adminOnly, userType, new PaginationParams(marker, limit));
        return cloud20Service.listUsers(httpHeaders, uriInfo, authToken, listUsersSearchParams).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name=GlobalConstants.V2_GET_USER_BY_ID)
    @GET
    @Path("users/{userId}")
    public Response getUserById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserById(httpHeaders, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List user domain roles")
    @ReportableQueryParams(unsecuredQueryParams = {"serviceId","apply_rcn_roles"})
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete user roles of type")
    @ReportableQueryParams(unsecuredQueryParams = {"type"})
    @DELETE
    @Path("users/{userId}/roles")
    public Response deleteUserRoles(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @QueryParam("type") String roleType) {
        return cloud20Service.deleteUserRoles(httpHeaders, authToken, userId, roleType).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List user accessible domains")
    @GET
    @Path("users/{userId}/RAX-AUTH/domains")
    public Response getAccessibleDomainsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getAccessibleDomainsForUser(authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List endpoints on domain for user")
    @GET
    @Path("users/{userId}/RAX-AUTH/domains/{domainId}/endpoints")
    public Response getAccessibleDomainsEndpointsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("domainId") String domainId) {
        return cloud20Service.getAccessibleDomainsEndpointsForUser(authToken, userId, domainId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List admins for user")
    @GET
    @Path("users/{userId}/RAX-AUTH/admins")
    public Response getUserAdminsForUser(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserAdminsForUser(authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List tenants")
    @ReportableQueryParams(unsecuredQueryParams = {"name","marker","limit","apply_rcn_roles"})
    @GET
    @Path("tenants")
    public Response listTenantsAndGetTenantByName(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("apply_rcn_roles") boolean applyRcnRoles,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        // Note: getTenantByName only available to admin
        if (!StringUtils.isBlank(name)) {
            return cloud20Service.getTenantByName(httpHeaders, authToken, name).build();
        } else {
            return cloud20Service.listTenants(httpHeaders, authToken, applyRcnRoles, validateMarker(marker), validateLimit(limit)).build();
        }
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get tenant")
    @GET
    @Path("tenants/{tenantId}")
    public Response getTenantById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantsId) {
        return cloud20Service.getTenantById(httpHeaders, authToken, tenantsId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List user roles on tenant")
    @ReportableQueryParams(unsecuredQueryParams = {"apply_rcn_roles"})
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add user")
    @POST
    @Path("users")
    public Response addUser(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, UserForCreate user) {
        return cloud20Service.addUser(httpHeaders, uriInfo, authToken, user).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add Invite User")
    @POST
    @Path("RAX-AUTH/invite/user")
    public Response addInviteUser(@Context UriInfo uriInfo,
                                  @Context HttpHeaders httpHeaders,
                                  @HeaderParam(X_AUTH_TOKEN) String authToken, User user) {
        return cloud20Service.addInviteUser(httpHeaders, uriInfo, authToken, user).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Send unverified user invite")
    @POST
    @Path("RAX-AUTH/invite/user/{userId}/send")
    public Response sendUnverifiedUserInvite(@Context UriInfo uriInfo,
                                             @Context HttpHeaders httpHeaders,
                                             @HeaderParam(X_AUTH_TOKEN) String authToken,
                                             @PathParam("userId") String userId ) {
        return cloud20Service.sendUnverifiedUserInvite(httpHeaders, uriInfo, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Accept unverified user invite")
    @PUT
    @Path("RAX-AUTH/invite/user/{userId}/accept")
    public Response acceptUnverifiedUserInvite(@Context UriInfo uriInfo,
                                               @Context HttpHeaders httpHeaders,
                                               @HeaderParam(X_AUTH_TOKEN) String authToken,
                                               @PathParam("userId") String userId,
                                               UserForCreate user) {
        user.setId(userId);
        return cloud20Service.acceptUnverifiedUserInvite(httpHeaders, uriInfo, user).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Validate Invite User")
    @HEAD
    @Path("RAX-AUTH/invite/user/{userId}")
    public Response validateInviteUser(@Context UriInfo uriInfo,
                                       @Context HttpHeaders httpHeaders,
                                       @QueryParam("registrationCode") String registrationCode,
                                       @PathParam("userId") String userId) {
        return cloud20Service.verifyInviteUser(httpHeaders, uriInfo, userId, registrationCode).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update user")
    @POST
    @Path("users/{userId}")
    public Response updateUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, UserForCreate user) {
        return cloud20Service.updateUser(httpHeaders, authToken, userId, user).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete user")
    @DELETE
    @Path("users/{userId}")
    public Response deleteUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.deleteUser(httpHeaders, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Enable or disable user")
    @PUT
    @Path("users/{userId}/OS-KSADM/enabled")
    public Response setUserEnabled(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, User user) {
        return cloud20Service.setUserEnabled(httpHeaders, authToken, userId, user).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name=GlobalConstants.V2_LIST_USER_LEGACY_GROUPS)
    @GET
    @Path("users/{userId}/RAX-KSGRP")
    public Response listUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.listUserGroups(httpHeaders, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Grant roles to user")
    @PUT
    @Path("users/{userId}/RAX-AUTH/roles")
    public Response grantRolesToUser(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            RoleAssignments roleAssignments) {
        if (!identityConfig.getReloadableConfig().isGrantRolesToUserServiceEnabled()) {
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.grantRolesToUser(httpHeaders, authToken, userId, roleAssignments).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Grant domain role to user")
    @PUT
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response addUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return cloud20Service.addUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get domain role on user")
    @GET
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response getUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return cloud20Service.getUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Revoke domain role from user")
    @DELETE
    @Path("users/{userId}/roles/OS-KSADM/{roleId}")
    public Response deleteUserRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        return cloud20Service.deleteUserRole(httpHeaders, authToken, userId, roleId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add user credential")
    @POST
    @Path("users/{userId}/OS-KSADM/credentials")
    public Response addUserCredential(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, String body) {
        return cloud20Service.addUserCredential(httpHeaders, uriInfo, authToken, userId, body).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List user credentials")
    @ReportableQueryParams(unsecuredQueryParams = {"userId","marker","limit"})
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update user password credentials")
    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response updateUserPasswordCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId,
        PasswordCredentialsBase creds) {
        return cloud20Service.updateUserPasswordCredentials(httpHeaders, authToken, userId, JSONConstants.PASSWORD_CREDENTIALS, creds).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update user api key credentials")
    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)
    public Response updateUserApiKeyCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId, ApiKeyCredentials creds) {
        return cloud20Service.updateUserApiKeyCredentials(httpHeaders, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS, creds).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Reset user api key credentials")
    @POST
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS + "/RAX-AUTH/reset")
    public Response resetUserApiKeyCredentials(
        @Context HttpHeaders httpHeaders,
        @HeaderParam(X_AUTH_TOKEN) String authToken,
        @PathParam("userId") String userId) {
        return cloud20Service.resetUserApiKeyCredentials(httpHeaders, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get user api key credential")
    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)
    public Response getUserCredentialKey(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserApiKeyCredentials(httpHeaders, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get user password credential")
    @GET
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response getUserCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getUserPasswordCredentials(httpHeaders, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete user api key credential")
    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)
    public Response deleteUserKeyCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.deleteUserCredential(httpHeaders, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete user password credential")
    @DELETE
    @Path("users/{userId}/OS-KSADM/credentials/" + JSONConstants.PASSWORD_CREDENTIALS)
    public Response deleteUserCredential(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.deleteUserCredential(httpHeaders, authToken, userId, JSONConstants.PASSWORD_CREDENTIALS).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add tenant")
    @POST
    @Path("tenants")
    public Response addTenant(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, Tenant tenant) {
        return cloud20Service.addTenant(httpHeaders, uriInfo, authToken, tenant).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update tenant")
    @POST
    @Path("tenants/{tenantId}")
    public Response updateTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId, Tenant tenant) {
        return cloud20Service.updateTenant(httpHeaders, authToken, tenantId, tenant).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete tenant")
    @DELETE
    @Path("tenants/{tenantId}")
    public Response deleteTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.deleteTenant(httpHeaders, authToken, tenantId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List roles explicitly assigned on tenant")
    @ReportableQueryParams(unsecuredQueryParams = {"marker","limit"})
    @GET
    @Path("tenants/{tenantId}/OS-KSADM/roles")
    public Response listRolesForTenant(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        // NOTE: marker and limit are ignored when listing roles for tenant
        return cloud20Service.listRolesForTenant(httpHeaders, authToken, tenantId, validateMarker(marker), validateLimit(limit)).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List users for tenant")
    @ReportableQueryParams(unsecuredQueryParams = {"tenantId","roleId","contactId","marker","limit"})
    @GET
    @Path("tenants/{tenantId}/users")
    public Response listUsersForTenantAndListUsersWithRoleForTenant(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("roleId") String roleId,
            @QueryParam("contactId") String contactId,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit) {
        ListUsersForTenantParams params = new ListUsersForTenantParams(roleId, contactId, new PaginationParams(validateMarker(marker), validateLimit(limit)));
        return cloud20Service.listUsersForTenant(httpHeaders, uriInfo, authToken, tenantId, params).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Grant role on tenant to user")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Revoke role on tenant from user")
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List my assignable roles")
    @ReportableQueryParams(unsecuredQueryParams = {"serviceId","roleName","marker","limit"})
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add role")
    @POST
    @Path("OS-KSADM/roles")
    public Response addRole(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Role role) {
        return cloud20Service.addRole(httpHeaders, uriInfo, authToken, role).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get role")
    @GET
    @Path("OS-KSADM/roles/{roleId}")
    public Response getRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("roleId") String roleId) {
        return cloud20Service.getRole(httpHeaders, authToken, roleId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete role")
    @DELETE
    @Path("OS-KSADM/roles/{roleId}")
    public Response deleteRole(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("roleId") String roleId) {
        return cloud20Service.deleteRole(httpHeaders, authToken, roleId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List users assigned role")
    @ReportableQueryParams(unsecuredQueryParams = {"roleId","marker","limit"})
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List services")
    @ReportableQueryParams(unsecuredQueryParams = {"name","marker","limit"})
    @GET
    @Path("OS-KSADM/services")
    public Response listServices(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("name") String name,
            @QueryParam("marker") Integer marker,
            @QueryParam("limit") Integer limit)
        {
        return cloud20Service.listServices(httpHeaders, uriInfo, authToken, name, validateMarker(marker), validateLimit(limit)).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add service")
    @POST
    @Path("OS-KSADM/services")
    public Response addService(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, Service service) {
        return cloud20Service.addService(httpHeaders, uriInfo, authToken, service).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get service")
    @GET
    @Path("OS-KSADM/services/{serviceId}")
    public Response getService(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("serviceId") String serviceId) {
        return cloud20Service.getService(httpHeaders, authToken, serviceId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete service")
    @DELETE
    @Path("OS-KSADM/services/{serviceId}")
    public Response deleteService(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("serviceId") String serviceId) {
        return cloud20Service.deleteService(httpHeaders, authToken, serviceId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List endpoint templates")
    @ReportableQueryParams(unsecuredQueryParams = {"serviceId"})
    @GET
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response listEndpointTemplates(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @QueryParam("serviceId") String serviceId) {
        return cloud20Service.listEndpointTemplates(httpHeaders, authToken, serviceId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add endpoint template")
    @POST
    @Path("OS-KSCATALOG/endpointTemplates")
    public Response addEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken, EndpointTemplate endpoint) {
        return cloud20Service.addEndpointTemplate(httpHeaders, uriInfo, authToken, endpoint).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update endpoint template")
    @PUT
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response updateEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @PathParam("endpointTemplateId") String endpointTemplateId,
            @HeaderParam(X_AUTH_TOKEN) String authToken, EndpointTemplate endpoint) {
        return cloud20Service.updateEndpointTemplate(httpHeaders, uriInfo, authToken, endpointTemplateId, endpoint).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get endpoint template")
    @GET
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response getEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String endpointTemplateId) {
        return cloud20Service.getEndpointTemplate(httpHeaders, authToken, endpointTemplateId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete endpoint template")
    @DELETE
    @Path("OS-KSCATALOG/endpointTemplates/{endpointTemplateId}")
    public Response deleteEndpointTemplate(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("endpointTemplateId") String enpdointTemplateId) {
        return cloud20Service.deleteEndpointTemplate(httpHeaders, authToken, enpdointTemplateId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List endpoints for tenant")
    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response listEndpoints(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId) {
        return cloud20Service.listEndpoints(httpHeaders, authToken, tenantId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add endpoint to tenant")
    @POST
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints")
    public Response addEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId, EndpointTemplate endpoint) {
        return cloud20Service.addEndpoint(httpHeaders, authToken, tenantId, endpoint).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get endpoint on tenant")
    @GET
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response getEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("endpointId") String endpointId) {
        return cloud20Service.getEndpoint(httpHeaders, authToken, tenantId, endpointId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete endpoint from tenant")
    @DELETE
    @Path("/tenants/{tenantId}/OS-KSCATALOG/endpoints/{endpointId}")
    public Response deleteEndpoint(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("tenantId") String tenantId,
            @PathParam("endpointId") String endpointId) {
        return cloud20Service.deleteEndpoint(httpHeaders, authToken, tenantId, endpointId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get user secret qa")
    @GET
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response getSecretQA(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId) {
        return cloud20Service.getSecretQA(httpHeaders, authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update user secret qa")
    @PUT
    @Path("/users/{userId}/RAX-KSQA/secretqa")
    public Response updateSecretQA(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId, SecretQA secrets) {
        return cloud20Service.updateSecretQA(httpHeaders, authToken, userId, secrets).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List user secret qa")
    @GET
    @Path("users/{userId}/RAX-AUTH/secretqas")
    public Response getSecretQAs(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId){
        return cloud20Service.getSecretQAs(authToken, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add user secret qa")
    @POST
    @Path("users/{userId}/RAX-AUTH/secretqas")
    public Response createSecretQA(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQA){
        return cloud20Service.createSecretQA(authToken, userId, secretQA).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Verify user phone pin")
    @POST
    @Path("users/{userId}/RAX-AUTH/phone-pin/verify")
    public Response verifyPhonePin(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            PhonePin phonePin){
        if(!identityConfig.getReloadableConfig().getEnablePhonePinOnUserFlag()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.verifyPhonePin(authToken, userId, phonePin).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Reset user phone pin")
    @POST
    @Path("users/{userId}/RAX-AUTH/phone-pin/reset")
    public Response resetPhonePin(
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("userId") String userId,
            @QueryParam("only_if_missing") boolean onlyIfMissing) {
        if(!identityConfig.getReloadableConfig().getEnablePhonePinOnUserFlag()){
            throw new NotFoundException(SERVICE_NOT_FOUND_ERROR_MESSAGE);
        }
        return cloud20Service.resetPhonePin(authToken, userId, onlyIfMissing).build();
    }

    // ******************************************************* //
    // RAX-GRPADM Extension //
    // ******************************************************* //

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add legacy group")
    @POST
    @Path("/RAX-GRPADM/groups")
    public Response addGroup(
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            Group group) {
        return cloud20Service.addGroup(httpHeaders, uriInfo, authToken, group).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List legacy groups")
    @ReportableQueryParams(unsecuredQueryParams = {"name","marker","limit"})
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
        // NOTE: QueryParam groupName is never used when listing groups
        return cloud20Service.listGroups(httpHeaders, authToken, groupName, validateMarker(marker), validateLimit(limit)).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get legacy group")
    @GET
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response getGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId) {
        return cloud20Service.getGroupById(httpHeaders, authToken, groupId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update legacy group")
    @PUT
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response updateGroup(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            Group group) {
        return cloud20Service.updateGroup(httpHeaders, authToken, groupId, group).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete legacy group")
    @DELETE
    @Path("/RAX-GRPADM/groups/{groupId}")
    public Response deleteGroupById(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId) {
        return cloud20Service.deleteGroup(httpHeaders, authToken, groupId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List legacy group members")
    @ReportableQueryParams(unsecuredQueryParams = {"groupId","marker","limit"})
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

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add user to legacy group")
    @PUT
    @Path("/RAX-GRPADM/groups/{groupId}/users/{userId}")
    public Response putUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @PathParam("userId") String userId) {
        return cloud20Service.addUserToGroup(httpHeaders, authToken, groupId, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Remove user from legacy group")
    @DELETE
    @Path("/RAX-GRPADM/groups/{groupId}/users/{userId}")
    public Response deleteUserGroups(
            @Context HttpHeaders httpHeaders,
            @HeaderParam(X_AUTH_TOKEN) String authToken,
            @PathParam("groupId") String groupId,
            @PathParam("userId") String userId) {
        return cloud20Service.removeUserFromGroup(httpHeaders, authToken, groupId, userId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add region")
    @POST
    @Path("RAX-AUTH/regions")
    public Response createRegion(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, Region region) {
        return cloud20Service.addRegion(uriInfo, authToken, region).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get region")
    @GET
    @Path("RAX-AUTH/regions/{name}")
    public Response getRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name) {
        return cloud20Service.getRegion(authToken, name).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List regions")
    @GET
    @Path("RAX-AUTH/regions")
    public Response getRegions(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return cloud20Service.getRegions(authToken).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update region")
    @PUT
    @Path("RAX-AUTH/regions/{name}")
    public Response updateRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name, Region region) {
        return cloud20Service.updateRegion(authToken, name, region).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete region")
    @DELETE
    @Path("RAX-AUTH/regions/{name}")
    public Response deleteRegion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name) {
        return cloud20Service.deleteRegion(authToken, name).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add secret qa")
    @POST
    @Path("RAX-AUTH/secretqa/questions")
    public Response createQuestion(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, Question question) {
        return cloud20Service.addQuestion(uriInfo, authToken, question).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get secret qa")
    @GET
    @Path("RAX-AUTH/secretqa/questions/{questionId}")
    public Response getQuestion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("questionId") String questionId) {
        return cloud20Service.getQuestion(authToken, questionId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List secret qas")
    @GET
    @Path("RAX-AUTH/secretqa/questions")
    public Response getQuestions(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        return cloud20Service.getQuestions(authToken).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Update secret qa")
    @PUT
    @Path("RAX-AUTH/secretqa/questions/{name}")
    public Response updateQuestion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("name") String name, Question question) {
        return cloud20Service.updateQuestion(authToken, name, question).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete secret qa")
    @DELETE
    @Path("RAX-AUTH/secretqa/questions/{questionId}")
    public Response deleteQuestion(@HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("questionId") String questionId) {
        return cloud20Service.deleteQuestion(authToken, questionId).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Add tenant type")
    @POST
    @Path("RAX-AUTH/tenant-types")
    public Response createTenantType(@Context UriInfo uriInfo, @HeaderParam(X_AUTH_TOKEN) String authToken, TenantType tenantType) {
        return cloud20Service.addTenantType(uriInfo, authToken, tenantType).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 List tenant types")
    @ReportableQueryParams(unsecuredQueryParams = {"marker","limit"})
    @GET
    @Path("RAX-AUTH/tenant-types")
    public Response listTenantTypes(@Context UriInfo uriInfo,
                                   @HeaderParam(X_AUTH_TOKEN) String authToken,
                                   @QueryParam("marker") Integer marker,
                                   @QueryParam("limit") Integer limit) {
        return cloud20Service.listTenantTypes(uriInfo, authToken, validateMarker(marker), validateLimit(limit)).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Get tenant type")
    @GET
    @Path("RAX-AUTH/tenant-types/{tenantTypeName}")
    public Response getTenantTypes(@Context UriInfo uriInfo,
                                   @HeaderParam(X_AUTH_TOKEN) String authToken, @PathParam("tenantTypeName") String tenantTypeName) {
        return cloud20Service.getTenantType(uriInfo, authToken, tenantTypeName).build();
    }

    @IdentityApi(apiResourceType = ApiResourceType.PRIVATE, name="v2.0 Delete tenant type")
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

    @Path("RAX-AUTH/domains/{" + DOMAIN_ID_PATH_PARAM_NAME + "}/groups")
    public CloudUserGroupResource getUserGroupResource() {
        return userGroupResource;
    }

    @Path("RAX-AUTH/delegation-agreements/")
    public DelegationAgreementResource getDelegationAgreementResource() {
        return delegationAgreementResource;
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



