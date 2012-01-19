package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class DefaultCloud20Service implements Cloud20Service {

    @Autowired
    private AuthConverterCloudV20 authConverterCloudV20;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ApplicationService clientService;

    @Autowired
    private Configuration config;

    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    @Autowired
    private RoleConverterCloudV20 roleConverterCloudV20;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private ServiceConverterCloudV20 serviceConverterCloudV20;

    @Autowired
    private TenantConverterCloudV20 tenantConverterCloudV20;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TokenConverterCloudV20 tokenConverterCloudV20;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private UserService userService;

    private HashMap<String, JAXBElement<Extension>> extensionMap;

    private JAXBElement<Extensions> currentExtensions;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            Tenant tenant = checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpoint.getId());
            if (baseUrl.getGlobal()) {
                throw new BadRequestException("Cannot add a global endpoint to this tenant.");
            }
            tenant.addBaseUrlId(String.valueOf(endpoint.getId()));
            this.tenantService.updateTenant(tenant);
            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, EndpointTemplate endpoint) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            CloudBaseUrl baseUrl = this.endpointConverterCloudV20.toCloudBaseUrl(endpoint);
            this.endpointService.addBaseUrl(baseUrl);
            return Response.created(uriInfo.getRequestUriBuilder().path(String.valueOf(baseUrl.getBaseUrlId())).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                            .createEndpointTemplate(this.endpointConverterCloudV20.toEndpointTemplate(baseUrl)));
        } catch (BaseUrlConflictException buce) {
            return endpointTemplateConflictException(buce.getMessage());
        } catch (DuplicateException dex) {
            return endpointTemplateConflictException(dex.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            if (role == null) {
                String errMsg = "role cannot be null";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            if (StringUtils.isBlank(role.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            if (StringUtils.isBlank(role.getServiceId())) {
                String errMsg = "Expecting serviceId";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            Application service = checkAndGetApplication(role.getServiceId());

            ClientRole clientRole = new ClientRole();
            clientRole.setClientId(service.getClientId());
            clientRole.setDescription(role.getDescription());
            clientRole.setName(role.getName());

            clientService.addClientRole(clientRole);

            return Response.created(uriInfo.getRequestUriBuilder().path(clientRole.getId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole(roleConverterCloudV20.toRoleFromClientRole(clientRole)));

        } catch (DuplicateException bre) {
            return roleConflictExceptionResponse(bre.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) {

        try {
            checkXAUTHTOKEN(authToken, false, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            User user = checkAndGetUser(userId);

            ClientRole role = checkAndGetClientRole(roleId);

            TenantRole tenantrole = new TenantRole();
            tenantrole.setName(role.getName());
            tenantrole.setClientId(role.getClientId());
            tenantrole.setRoleRsId(role.getId());
            tenantrole.setUserId(user.getId());
            tenantrole.setTenantIds(new String[]{tenant.getTenantId()});

            this.tenantService.addTenantRoleToUser(user, tenantrole);

            return Response.ok();

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            if (service == null) {
                String errMsg = "service cannot be null";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(service.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(service.getType())) {
                String errMsg = "Expecting type";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            Application client = new Application();
            client.setOpenStackType(service.getType());
            client.setDescription(service.getDescription());
            client.setName(service.getName());
            client.setRCN(getRackspaceCustomerId());

            this.clientService.add(client);
            service.setId(client.getClientId());
            return Response.created(
                    uriInfo.getRequestUriBuilder().path(service.getId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createService(service));

        } catch (DuplicateException de) {
            return serviceConflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken,
                                     org.openstack.docs.identity.api.v2.Tenant tenant) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            if (StringUtils.isBlank(tenant.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            // Our implmentation has the id and the name the same
            tenant.setId(tenant.getName());

            Tenant savedTenant = this.tenantConverterCloudV20.toTenantDO(tenant);

            this.tenantService.addTenant(savedTenant);

            return Response.created(uriInfo.getRequestUriBuilder().path(savedTenant.getTenantId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                            .createTenant(this.tenantConverterCloudV20.toTenant(savedTenant)));

        } catch (DuplicateException de) {
            return tenantConflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken,
                                   org.openstack.docs.identity.api.v2.User user) {

        try {
            checkXAUTHTOKEN(authToken, false, null);

            validateUser(user);

            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);

            User userDO = this.userConverterCloudV20.toUserDO(user);

            setDomainId(scopeAccessByAccessToken, userDO);


            userService.addUser(userDO);

            if (authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)) {
                ClientRole roleId = clientService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
                this.addUserRole(httpHeaders, authToken, userDO.getId(), roleId.getId());
            }

            return Response.created(
                    uriInfo.getRequestUriBuilder().path(userDO.getId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(userDO)));

        } catch (DuplicateException de) {
            return userConflictExceptionResponse(de.getMessage());
        } catch (DuplicateUsernameException due) {
            return userConflictExceptionResponse(due.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    User getUser(ScopeAccess scopeAccessByAccessToken) {
        String uid = scopeAccessByAccessToken.getLDAPEntry().getAttributeValue(LdapRepository.ATTR_UID);
        return userService.getUser(uid);
    }

    void setDomainId(ScopeAccess scopeAccessByAccessToken, User userDO) {
        if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
            User caller = getUser(scopeAccessByAccessToken);
            //is userAdmin
            userDO.setDomainId(caller.getDomainId());
        }
    }

    void validateUser(org.openstack.docs.identity.api.v2.User user) {
        if (StringUtils.isBlank(user.getUsername())) {
            String errorMsg = "Expecting username";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        String email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            String errorMsg = "Expecting valid email address";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        if (!email.matches("[a-zA-Z0-9_\\.\"]+@[a-zA-Z0-9_\\.]+\\.[a-zA-Z]+")) {
            String errorMsg = "Expecting valid email address";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

    }

    void validatePasswordCredentials(PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername) {
        String username = passwordCredentialsRequiredUsername.getUsername();
        String password = passwordCredentialsRequiredUsername.getPassword();
        if (StringUtils.isBlank(username)) {
            String errMsg = "Expecting username";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (StringUtils.isBlank(password)) {
            String errMsg = "Expecting password";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    void validatePassword(String password) {
        String errMsg = "Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter and one numeric character.";
        if (password.length() < 8) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[A-Z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[a-z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[0-9].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    void validateApiKeyCredentials(ApiKeyCredentials apiKeyCredentials) {
        String username = apiKeyCredentials.getUsername();
        String apiKey = apiKeyCredentials.getApiKey();
        if (StringUtils.isBlank(username)) {
            String errMsg = "Expecting username";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (StringUtils.isBlank(apiKey)) {
            String errMsg = "Expecting apiKey";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String body) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            JAXBElement<? extends CredentialType> credentials;

            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                credentials = getXMLCredentials(body);
            } else {
                credentials = getJSONCredentials(body);
            }

            User user;

            if (credentials.getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)) {
                PasswordCredentialsRequiredUsername userCredentials = (PasswordCredentialsRequiredUsername) credentials.getValue();
                validatePasswordCredentials(userCredentials);
                validatePassword(userCredentials.getPassword());
                user = checkAndGetUser(userId);
                if (!userCredentials.getUsername().equals(user.getUsername())) {
                    String errMsg = "User and UserId mis-matched";
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
                user.setPassword(userCredentials.getPassword());
                userService.updateUser(user, false);
            } else if (credentials.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                ApiKeyCredentials userCredentials = (ApiKeyCredentials) credentials.getValue();
                validateApiKeyCredentials(userCredentials);
                user = checkAndGetUser(userId);
                if (!userCredentials.getUsername().equals(user.getUsername())) {
                    String errMsg = "User and UserId mis-matched";
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
                user.setApiKey(userCredentials.getApiKey());
                userService.updateUser(user, false);
            }
            return Response.ok(credentials).status(Status.CREATED);
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {

        try {
            checkXAUTHTOKEN(authToken, false, null);
            User user = checkAndGetUser(userId);
            ClientRole cRole = checkAndGetClientRole(roleId);
            TenantRole role = new TenantRole();
            role.setClientId(cRole.getClientId());
            role.setName(cRole.getName());
            role.setRoleRsId(cRole.getId());
            this.tenantService.addTenantRoleToUser(user, role);
            return Response.ok();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    // Core Service Methods
    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException {
        try {
            User user = null;
            UserScopeAccess usa = null;
            if (authenticationRequest.getToken() != null && !StringUtils.isBlank(authenticationRequest.getToken().getId())) {
                ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(authenticationRequest.getToken().getId());
                if (sa == null || ((HasAccessToken) sa).isAccessTokenExpired(new DateTime()) || !(sa instanceof UserScopeAccess)) {
                    String errMsg = "Token not authenticated";
                    logger.warn(errMsg);
                    throw new NotAuthenticatedException(errMsg);
                }
                usa = (UserScopeAccess) sa;
                user = this.checkAndGetUser(usa.getUserRsId());
            } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)) {
                PasswordCredentialsRequiredUsername creds = (PasswordCredentialsRequiredUsername) authenticationRequest.getCredential().getValue();
                validatePasswordCredentials(creds);
                String username = creds.getUsername();
                String password = creds.getPassword();
                user = checkAndGetUserByName(username);
                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(username, password, getCloudAuthClientId());

            } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                ApiKeyCredentials creds = (ApiKeyCredentials) authenticationRequest.getCredential().getValue();
                validateApiKeyCredentials(creds);
                String username = creds.getUsername();
                String key = creds.getApiKey();
                user = checkAndGetUserByName(username);
                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(username, key, getCloudAuthClientId());
            }
            List<TenantRole> roles = tenantService.getTenantRolesForScopeAccess(usa);
            if (!belongsTo(authenticationRequest.getTenantId(), roles) && !belongsTo(authenticationRequest.getTenantName(), roles)) {
                String errMsg = "User does not have access to tenant %s";
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(usa);

            // Remove Admin URLs if non admin token
            if (!this.authorizationService.authorizeCloudIdentityAdmin(usa)) {
                stripEndpoints(endpoints);
            }

            AuthenticateResponse auth = authConverterCloudV20.toAuthenticationResponse(user, usa, roles, endpoints);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createAccess(auth));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
            throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            ScopeAccess sa = checkAndGetToken(tokenId);

            if (!StringUtils.isBlank(belongsTo)) {
                UserScopeAccess usa = (UserScopeAccess) sa;
                List<TenantRole> roles = this.tenantService.getTenantRolesForScopeAccess(usa);
                if (!belongsTo(belongsTo, roles)) {
                    throw new NotFoundException();
                }
            }
            return Response.ok();
        } catch (BadRequestException bre) {
            return badRequestExceptionResponse(bre.getMessage());
        } catch (NotAuthorizedException nae) {
            return Response.ok().status(Status.UNAUTHORIZED);
        } catch (ForbiddenException fe) {
            return Response.ok().status(Status.FORBIDDEN);
        } catch (NotFoundException nfe) {
            return Response.ok().status(Status.NOT_FOUND);
        } catch (Exception ex) {
            return Response.ok().status(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, String endpointId) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            Tenant tenant = checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointId);
            tenant.removeBaseUrlId(String.valueOf(baseUrl.getBaseUrlId()));
            tenantService.updateTenant(tenant);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointTemplateId);
            this.endpointService.deleteBaseUrl(baseUrl.getBaseUrlId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            ClientRole role = checkAndGetClientRole(roleId);
            this.clientService.deleteClientRole(role);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                      String userId, String roleId) {
        try {
            checkXAUTHTOKEN(authToken, true, tenantId);
            Tenant tenant = checkAndGetTenant(tenantId);
            User user = checkAndGetUser(userId);
            ClientRole role = checkAndGetClientRole(roleId);
            TenantRole tenantrole = new TenantRole();
            tenantrole.setClientId(role.getClientId());
            tenantrole.setRoleRsId(role.getId());
            tenantrole.setUserId(user.getId());
            tenantrole.setTenantIds(new String[]{tenant.getTenantId()});
            this.tenantService.deleteTenantRole(user.getUniqueId(), tenantrole);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            Application client = checkAndGetApplication(serviceId);
            this.clientService.delete(client.getClientId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            Tenant tenant = checkAndGetTenant(tenantId);
            tenantService.deleteTenant(tenant.getTenantId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        try {
            checkXAUTHTOKEN(authToken, false, null);
            User user = checkAndGetUser(userId);
            //is same domain?
            if (authorizationService.authorizeCloudUserAdmin(scopeAccessService.getScopeAccessByAccessToken(authToken))) {
                User caller = userService.getUserByAuthToken(authToken);
                verifyDomain(user, caller);
            }

            userService.softDeleteUser(user);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            User user = checkAndGetSoftDeletedUser(userId);
            userService.deleteUser(user);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
            throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            if (credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)) {
                IdentityFault fault = new IdentityFault();
                fault.setCode(HttpServletResponse.SC_NOT_IMPLEMENTED);
                fault.setMessage("Method not implemented");
                return Response.ok(
                        OBJ_FACTORIES.getOpenStackIdentityV2Factory().createIdentityFault(fault))
                        .status(HttpServletResponse.SC_NOT_IMPLEMENTED);
            }

            if (!credentialType.equals(JSONConstants.APIKEY_CREDENTIALS)) {
                throw new BadRequestException("unsupported credential type");
            }

            User user = checkAndGetUser(userId);

            user.setApiKey("");

            userService.updateUser(user, false);

            return Response.noContent();

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {

        try {
            checkXAUTHTOKEN(authToken, true, null);
            User user = checkAndGetUser(userId);
            List<TenantRole> globalRoles = this.tenantService.getGlobalRolesForUser(user);
            TenantRole role = null;
            for (TenantRole globalRole : globalRoles) {
                if (globalRole.getRoleRsId().equals(roleId)) {
                    role = globalRole;
                }
            }
            if (role == null) {
                String errMsg = String.format("Role %s not found for user %s", roleId, userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            this.tenantService.deleteGlobalRole(role);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, String endpointId) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            Tenant tenant = checkAndGetTenant(tenantId);

            if (!tenant.containsBaseUrlId(endpointId)) {
                String errMsg = String.format("Tenant %s does not have endpoint %s", tenantId, endpointId);
                logger.warn(errMsg);
                return notFoundExceptionResponse(errMsg);
            }

            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointId);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointTemplateId);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                    .createEndpointTemplate(this.endpointConverterCloudV20.toEndpointTemplate(baseUrl)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException {
        if (StringUtils.isBlank(alias)) {
            return badRequestExceptionResponse("Invalid extension alias '" + alias + "'.");
        }

        final String normalizedAlias = alias.trim().toUpperCase();

        if (extensionMap == null) {
            extensionMap = new HashMap<String, JAXBElement<Extension>>();

            try {
                if (currentExtensions == null) {
                    JAXBContext jaxbContext = JAXBContextResolver.get();
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
                    StreamSource ss = new StreamSource(is);
                    currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
                }

                Extensions exts = currentExtensions.getValue();

                for (Extension e : exts.getExtension()) {
                    extensionMap.put(e.getAlias().trim().toUpperCase(), OBJ_FACTORIES.getOpenStackCommonV1Factory().createExtension(e));
                }
            } catch (Exception e) {
                // Return 500 error. Is WEB-IN/extensions.xml malformed?
                return serviceExceptionResponse();
            }
        }

        if (!extensionMap.containsKey(normalizedAlias)) {
            return notFoundExceptionResponse("Extension with alias '" + normalizedAlias + "' is not available.");
        }

        return Response.ok(extensionMap.get(normalizedAlias));
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) {

        try {
            checkXAUTHTOKEN(authToken, true, null);
            ClientRole role = checkAndGetClientRole(roleId);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createRole(this.roleConverterCloudV20.toRoleFromClientRole(role)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getSecretQA(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);
            User user = checkAndGetUser(userId);
            SecretQA secrets = OBJ_FACTORIES.getRackspaceIdentityExtKsqaV1Factory().createSecretQA();

            secrets.setAnswer(user.getSecretAnswer());
            secrets.setQuestion(user.getSecretQuestion());

            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsqaV1Factory().createSecretQA(secrets));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) {

        try {
            checkXAUTHTOKEN(authToken, true, null);
            Application client = checkAndGetApplication(serviceId);
            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createService(serviceConverterCloudV20.toService(client)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            Tenant tenant = checkAndGetTenant(tenantsId);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenant)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            Tenant tenant = this.tenantService.getTenantByName(name);
            if (tenant == null) {
                String errMsg = String.format("Tenant with id/name: '%s' was not found.", name);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createTenant(this.tenantConverterCloudV20.toTenant(tenant)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            User user = this.userService.getUserById(userId);

            if (user == null) {
                String errMsg = String.format("User with id: '%s' was not found", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createUser(this.userConverterCloudV20.toUser(user)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            User user = this.userService.getUser(name);

            if (user == null) {
                String errMsg = String.format("User not found: '%s'", name);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(user)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
            throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            if (!(credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)
                    || credentialType.equals(JSONConstants.APIKEY_CREDENTIALS))) {
                throw new BadRequestException("unsupported credential type");
            }

            User user = this.userService.getUserById(userId);

            if (user == null) {
                String errMsg = "Credential type RAX-KSKEY:apiKeyCredentials was not found for User with Id: " + userId;
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            JAXBElement<? extends CredentialType> creds = null;

            if (credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)) {
                if (StringUtils.isBlank(user.getPassword())) {
                    throw new NotFoundException("User doesn't have password credentials");
                }
                PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
                userCreds.setPassword(user.getPassword());
                userCreds.setUsername(user.getUsername());
                creds = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredential(userCreds);
            } else if (credentialType.equals(JSONConstants.APIKEY_CREDENTIALS)) {
                if (StringUtils.isBlank(user.getApiKey())) {
                    throw new NotFoundException("User doesn't have api key credentials");
                }
                ApiKeyCredentials userCreds = new ApiKeyCredentials();
                userCreds.setApiKey(user.getApiKey());
                userCreds.setUsername(user.getUsername());
                creds = OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds);
            }

            return Response.ok(creds);

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            User user = checkAndGetUser(userId);

            List<TenantRole> globalRoles = tenantService.getGlobalRolesForUser(user);

            TenantRole role = null;

            for (TenantRole globalRole : globalRoles) {
                if (globalRole.getRoleRsId().equals(roleId)) {
                    role = globalRole;
                }
            }

            if (role == null) {
                String errMsg = String.format("Role %s not found for user %s", roleId, userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            ClientRole cRole = checkAndGetClientRole(roleId);

            role.setDescription(cRole.getDescription());
            role.setName(cRole.getName());

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createRole(this.roleConverterCloudV20.toRole(role)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit)
            throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            User user = checkAndGetUser(userId);

            CredentialListType creds = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredentialListType();

            if (!StringUtils.isBlank(user.getPassword())) {
                PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
                userCreds.setPassword(user.getPassword());
                userCreds.setUsername(user.getUsername());
                creds.getCredential().add(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createPasswordCredentials(userCreds));
            }

            if (!StringUtils.isBlank(user.getApiKey())) {
                ApiKeyCredentials userCreds = new ApiKeyCredentials();
                userCreds.setApiKey(user.getApiKey());
                userCreds.setUsername(user.getUsername());
                creds.getCredential().add(OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds));
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredentials(creds));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders, String authToken, String tenantId) {

        try {
            checkXAUTHTOKEN(authToken, false, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            List<CloudBaseUrl> baseUrls = this.endpointService.getGlobalBaseUrls();
            if (tenant.getBaseUrlIds() != null) {
                for (String id : tenant.getBaseUrlIds()) {
                    Integer baseUrlId = Integer.parseInt(id);
                    //ToDo: Do not add if in global list also
                    baseUrls.add(this.endpointService.getBaseUrlById(baseUrlId));
                }
            }
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createEndpoints(this.endpointConverterCloudV20.toEndpointListFromBaseUrls(baseUrls)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            ScopeAccess sa = checkAndGetToken(tokenId);

            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(sa);

            EndpointList list = endpointConverterCloudV20.toEndpointList(endpoints);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoints(list));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders, String authToken, String serviceId) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            List<CloudBaseUrl> baseUrls = null;

            if (StringUtils.isBlank(serviceId)) {
                baseUrls = this.endpointService.getBaseUrls();
            } else {
                Application client = checkAndGetApplication(serviceId);
                baseUrls = this.endpointService.getBaseUrlsByServiceId(client.getOpenStackType());
            }

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                            .createEndpointTemplates(endpointConverterCloudV20.toEndpointTemplateList(baseUrls)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders) throws IOException {
        try {
            if (currentExtensions == null) {
                JAXBContext jaxbContext = JAXBContextResolver.get();
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
                StreamSource ss = new StreamSource(is);

                currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
            }
            return Response.ok(currentExtensions);
        } catch (Exception e) {
            // Return 500 error. Is WEB-IN/extensions.xml malformed?
            return serviceExceptionResponse();
        }
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId, String marker, Integer limit) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            List<ClientRole> roles = null;

            if (StringUtils.isBlank(serviceId)) {
                roles = this.clientService.getAllClientRoles(null);
            } else {
                roles = this.clientService.getClientRolesByClientId(serviceId);
            }

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListFromClientRoles(roles)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) {

        try {
            checkXAUTHTOKEN(authToken, true, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            List<TenantRole> roles = this.tenantService.getTenantRolesForTenant(tenant.getTenantId());

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                    String userId) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            User user = checkAndGetUser(userId);

            List<TenantRole> roles = this.tenantService.getTenantRolesForUserOnTenant(user, tenant);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }

    }

    // KSADM Extension Role Methods
    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            List<Application> clients = this.clientService.getOpenStackServices();

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createServices(serviceConverterCloudV20.toServiceList(clients)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit)
            throws IOException {
        try {
            List<Tenant> tenants = new ArrayList<Tenant>();

            ScopeAccess access = this.scopeAccessService.getAccessTokenByAuthHeader(authToken);
            if (access == null) { // ToDo: Send an empty list, it's what Cloud does.
                //return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenants(
                //        this.tenantConverterCloudV20.toTenantList(tenants)));
                throw new NotAuthorizedException("Access is denied");
            }

            ScopeAccess sa = this.scopeAccessService.getScopeAccessByAccessToken(authToken);

            if (sa != null) {
                tenants = this.tenantService.getTenantsForScopeAccessByTenantRoles(sa);
            }

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenants(tenantConverterCloudV20.toTenantList(tenants)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            User user = this.userService.getUserById(userId);

            if (user == null) {
                String errMsg = "No Roles found User with id: " + userId;
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders, String authToken, String userId,
                                                          String serviceId) throws IOException {
        try {
            checkXAUTHTOKEN(authToken, true, null);

            User user = checkAndGetUser(userId);

            List<TenantRole> roles =
                    tenantService.getGlobalRolesForUser(user, new FilterParam[]{new FilterParam(FilterParamName.APPLICATION_ID, serviceId)});

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        try {
            checkXAUTHTOKEN(authToken, true, null);

            if (StringUtils.isBlank(userId)) {
                String errMsg = "Expecting userId";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            User user = this.userService.getUserById(userId);

            if (user == null) {
                String errMsg = String.format("User with id: '%s' was not found.", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            Integer mossoId = user.getMossoId();
            if (mossoId == null) {
                String errMsg = "User missing mosso id";
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            Groups groups = this.userGroupService.getGroups(mossoId);

            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsgrpV1Factory().createGroups(groups));
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    // KSADM Extension User methods
    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            // TODO write me
            return Response.status(Status.NOT_FOUND);
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) {

        try {
            checkXAUTHTOKEN(authToken, true, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            List<User> users = this.tenantService.getUsersForTenant(tenant.getTenantId());

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                      String roleId, String marker, Integer limit) {

        try {
            checkXAUTHTOKEN(authToken, true, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            ClientRole role = checkAndGetClientRole(roleId);

            List<User> users = this.tenantService.getUsersWithTenantRole(tenant, role);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createUsers(this.userConverterCloudV20.toUserList(users)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId,
                                          org.openstack.docs.identity.api.v2.User user) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            User userDO = checkAndGetUser(userId);

            userDO.setEnabled(user.isEnabled());
            this.userService.updateUser(userDO, false);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createUser(this.userConverterCloudV20.toUser(userDO)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    public void setUserGroupService(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseBuilder updateSecretQA(HttpHeaders httpHeaders, String authToken, String userId, SecretQA secrets)
            throws IOException, JAXBException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            if (StringUtils.isBlank(secrets.getAnswer())) {
                throw new BadRequestException("Excpeting answer");
            }

            if (StringUtils.isBlank(secrets.getQuestion())) {
                throw new BadRequestException("Excpeting question");
            }

            User user = checkAndGetUser(userId);

            user.setSecretAnswer(secrets.getAnswer());
            user.setSecretQuestion(secrets.getQuestion());

            this.userService.updateUser(user, false);

            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsqaV1Factory().createSecretQA(secrets));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                        org.openstack.docs.identity.api.v2.Tenant tenant) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            Tenant tenantDO = checkAndGetTenant(tenantId);

            tenantDO.setDescription(tenant.getDescription());
            tenantDO.setDisplayName(tenant.getDisplayName());
            tenantDO.setEnabled(tenant.isEnabled());
            tenantDO.setName(tenant.getName());

            this.tenantService.updateTenant(tenantDO);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenantDO)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId,
                                      org.openstack.docs.identity.api.v2.User user) throws IOException {

        try {
            checkXAUTHTOKEN(authToken, false, null);

            User retrievedUser = checkAndGetUser(userId);

            //if user admin, verify domain
            if(authorizationService.authorizeCloudUserAdmin(scopeAccessService.getScopeAccessByAccessToken(authToken))){
                User caller = userService.getUserByAuthToken(authToken);
                verifyDomain(retrievedUser, caller);
            }


            User userDO = this.userConverterCloudV20.toUserDO(user);

            retrievedUser.copyChanges(userDO);

            this.userService.updateUser(retrievedUser, false);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(retrievedUser)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    void verifyDomain(User retrievedUser, User caller) {
        if (caller.getDomainId() == null || !caller.getDomainId().equals(retrievedUser.getDomainId())) {
            throw new ForbiddenException("Access is denied");
        }
    }

    @Override
    public ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                       String credentialType, ApiKeyCredentials creds) throws IOException, JAXBException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            if (StringUtils.isBlank(creds.getApiKey())) {
                String errMsg = "Expecting apiKey";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            if (StringUtils.isBlank(creds.getUsername())) {
                String errMsg = "Expecting username";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            User credUser = this.userService.getUser(creds.getUsername());

            if (credUser == null) {
                String errMsg = String.format("User: '%s' not found.", creds.getUsername());
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }


            User user = checkAndGetUser(userId);
            if (!creds.getUsername().equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            user.setApiKey(creds.getApiKey());
            this.userService.updateUser(user, false);

            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory()
                    .createApiKeyCredentials(creds));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                         String credentialType, PasswordCredentialsRequiredUsername creds) throws IOException, JAXBException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            validatePasswordCredentials(creds);
            validatePassword(creds.getPassword());
            User user = checkAndGetUser(userId);
            if (!creds.getUsername().equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            user.setPassword(creds.getPassword());
            this.userService.updateUser(user, false);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createPasswordCredentials(creds));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    // Core Admin Token Methods
    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
            throws IOException {

        try {
            checkXAUTHTOKEN(authToken, true, null);

            ScopeAccess sa = checkAndGetToken(tokenId);

            AuthenticateResponse access = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createAuthenticateResponse();

            access.setToken(this.tokenConverterCloudV20.toToken(sa));

            if (sa instanceof UserScopeAccess) {
                UserScopeAccess usa = (UserScopeAccess) sa;
                User user = this.userService.getUserById(usa.getUserRsId());
                if (user == null) {
                    throw new NotFoundException("User not found");
                }
                List<TenantRole> roles = this.tenantService.getTenantRolesForScopeAccess(usa);
                if (roles != null && roles.size() > 0) {
                    access.setUser(this.userConverterCloudV20.toUserForAuthenticateResponse(user, roles));
                }

                if (!belongsTo(belongsTo, roles)) {
                    String errMsg = String.format("Token doesn't belong to Tenant with Id/Name: '%s'", belongsTo);
                    logger.warn(errMsg);
                    throw new NotFoundException(errMsg);
                }
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createAccess(access));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    private Response.ResponseBuilder badRequestExceptionResponse(String message) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_BAD_REQUEST);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(
                OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault));
    }

    Response.ResponseBuilder exceptionResponse(Exception ex) {
        if (ex instanceof BadRequestException) {
            return badRequestExceptionResponse(ex.getMessage());
        } else if (ex instanceof NotAuthorizedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        } else if (ex instanceof NotAuthenticatedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        } else if (ex instanceof ForbiddenException) {
            return forbiddenExceptionResponse(ex.getMessage());
        } else if (ex instanceof NotFoundException) {
            return notFoundExceptionResponse(ex.getMessage());
        } else if (ex instanceof ClientConflictException) {
            return tenantConflictExceptionResponse(ex.getMessage());
        } else {
            return serviceExceptionResponse();
        }
    }

    private boolean belongsTo(String belongsTo, List<TenantRole> roles) {

        if (StringUtils.isBlank(belongsTo)) {
            return true;
        }

        if (roles == null || roles.size() == 0) {
            return false;
        }

        boolean ok = false;

        for (TenantRole role : roles) {
            if (role.containsTenantId(belongsTo)) {
                ok = true;
                break;
            }
        }
        return ok;
    }

    private Application checkAndGetApplication(String applicationId) {
        Application application = this.clientService.getById(applicationId);
        if (application == null) {
            String errMsg = String.format("Service %s not found", applicationId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return application;
    }

    private ClientRole checkAndGetClientRole(String id) {
        ClientRole cRole = this.clientService.getClientRoleById(id);
        if (cRole == null) {
            String errMsg = String.format("Role %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return cRole;
    }

    CloudBaseUrl checkAndGetEndpointTemplate(int baseUrlId) {
        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("EndpointTemplate %s not found", baseUrlId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return baseUrl;
    }

    private CloudBaseUrl checkAndGetEndpointTemplate(String id) {
        Integer baseUrlId;
        try {
            baseUrlId = Integer.parseInt(id);
        } catch (NumberFormatException nfe) {
            String errMsg = String.format("EndpointTemplate %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return checkAndGetEndpointTemplate(baseUrlId);
    }

    Tenant checkAndGetTenant(String tenantId) {
        Tenant tenant = this.tenantService.getTenant(tenantId);

        if (tenant == null) {
            String errMsg = String.format("Tenant with id/name: '%s' was not found.", tenantId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenant;
    }

    private ScopeAccess checkAndGetToken(String tokenId) {
        ScopeAccess sa = this.scopeAccessService.getScopeAccessByAccessToken(tokenId);

        if (sa == null) {
            String errMsg = String.format("Token %s not found", tokenId);
            logger.warn(errMsg);
            throw new NotFoundException("Token not found.");
        }

        return sa;
    }

    private User checkAndGetUser(String id) {
        User user = this.userService.getUserById(id);

        if (user == null) {
            String errMsg = String.format("User %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException("User not found");
        }

        return user;
    }

    private User checkAndGetUserByName(String username) {
        User user = this.userService.getUser(username);

        if (user == null) {
            String errMsg = String.format("User '%s' not found.", username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    private User checkAndGetSoftDeletedUser(String id) {
        User user = this.userService.getSoftDeletedUser(id);

        if (user == null) {
            String errMsg = String.format("User %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException("User not found");
        }

        return user;
    }

    void checkXAUTHTOKEN(String authToken, boolean identityOnly, String tenantId) {
        if (StringUtils.isBlank(authToken)) {
            throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
        }

        ScopeAccess authScopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authToken);
        if (authScopeAccess == null || ((HasAccessToken) authScopeAccess).isAccessTokenExpired(new DateTime())) {
            throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
        }

        boolean authorized = false;

        authorized = this.authorizationService.authorizeCloudIdentityAdmin(authScopeAccess);

        if (!identityOnly && !authorized) { // Check for User Admin Access
            authorized = this.authorizationService.authorizeCloudUserAdmin(authScopeAccess);
            if (authorized && tenantId != null) { // Check tenant access
                authorized = false; // until matched in the loop below
                List<Tenant> adminTenants = this.tenantService.getTenantsForScopeAccessByTenantRoles(authScopeAccess);
                for (Tenant tenant : adminTenants) {
                    if (tenant.getTenantId().equals(tenantId)) { authorized = true; }
                }
            }
        }

        if (!authorized) {
            String errMsg = "Access is denied";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    private void stripEndpoints(List<OpenstackEndpoint> endpoints) {
        for (int i = 0; i < endpoints.size(); i++) {
            for (CloudBaseUrl baseUrl : endpoints.get(i).getBaseUrls()) {
                baseUrl.setAdminUrl(null);
            }
        }
    }

    private Response.ResponseBuilder serviceConflictExceptionResponse(String errMsg) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault));
    }

    private Response.ResponseBuilder roleConflictExceptionResponse(String errMsg) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault));
    }

    private Response.ResponseBuilder endpointTemplateConflictException(String errMsg) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault));
    }

    private Response.ResponseBuilder forbiddenExceptionResponse(String errMsg) {
        ForbiddenFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createForbiddenFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_FORBIDDEN)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createForbidden(fault));
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private String getCloudAuthUserAdminRole() {
        return config.getString("cloudAuth.userAdminRole");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private JAXBElement<? extends CredentialType> getJSONCredentials(String jsonBody) {

        JAXBElement<? extends CredentialType> jaxbCreds = null;

        CredentialType creds = JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(jsonBody);

        if (creds instanceof ApiKeyCredentials) {
            jaxbCreds = OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials((ApiKeyCredentials) creds);
        } else if (creds instanceof PasswordCredentialsRequiredUsername) {
            jaxbCreds = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createPasswordCredentials((PasswordCredentialsRequiredUsername) creds);
        }

        return jaxbCreds;
    }

    @SuppressWarnings("unchecked")
    private JAXBElement<? extends CredentialType> getXMLCredentials(String body) {
        JAXBElement<? extends CredentialType> cred = null;
        try {
            JAXBContext context = JAXBContextResolver.get();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends CredentialType>) unmarshaller.unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            throw new BadRequestException();
        }
        return cred;
    }

    private Response.ResponseBuilder notAuthenticatedExceptionResponse(String message) {
        UnauthorizedFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUnauthorized(fault));
    }

    private Response.ResponseBuilder notFoundExceptionResponse(String message) {
        ItemNotFoundFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createItemNotFound(fault));
    }

    private Response.ResponseBuilder serviceExceptionResponse() {
        IdentityFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createIdentityFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createIdentityFault(fault));
    }

    Response.ResponseBuilder tenantConflictExceptionResponse(String message) {
        TenantConflictFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenantConflictFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenantConflict(fault));
    }

    Response.ResponseBuilder userConflictExceptionResponse(String message) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault));
    }

    Response.ResponseBuilder userDisabledExceptionResponse(String message) {
        UserDisabledFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUserDisabledFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_FORBIDDEN).
                entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUserDisabled(fault));
    }

    public void setOBJ_FACTORIES(JAXBObjectFactories OBJ_FACTORIES) {
        this.OBJ_FACTORIES = OBJ_FACTORIES;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void setTenantConverterCloudV20(TenantConverterCloudV20 tenantConverterCloudV20) {
        this.tenantConverterCloudV20 = tenantConverterCloudV20;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setEndpointConverterCloudV20(EndpointConverterCloudV20 endpointConverterCloudV20) {
        this.endpointConverterCloudV20 = endpointConverterCloudV20;
    }

    public void setClientService(ApplicationService clientService) {
        this.clientService = clientService;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setUserConverterCloudV20(UserConverterCloudV20 userConverterCloudV20) {
        this.userConverterCloudV20 = userConverterCloudV20;
    }

}
