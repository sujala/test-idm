package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.CloudUserExtractor;
import com.rackspace.idm.api.resource.cloud.HttpHeadersAcceptXml;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.HasAccessToken;
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.validation.Validator20;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.ws.commons.util.Base64;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class DelegateCloud20Service implements Cloud20Service {

    public static final String X_AUTH_TOKEN = "X-Auth-Token";
    public static final String TOKENS = "tokens";
    public static final String USERS = "users";
    public static final String MARKER = "marker";
    public static final String LIMIT = "limit";
    public static final String RAX_GRPADM_GROUPS = "RAX-GRPADM/groups/";
    public static final String NAME = "name";
    public static final String ROLES = "roles";
    public static final String TENANTS = "tenants";
    public static final String OS_KSADM_CREDENTIALS = "/OS-KSADM/credentials/";
    public static final String ROLES_OS_KSADM = "/roles/OS-KSADM/";
    @Autowired
    private CloudClient cloudClient;

    @Autowired
    private Configuration config;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private DefaultCloud20Service defaultCloud20Service;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private CloudUserExtractor cloudUserExtractor;

    @Autowired
    private Validator20 validator20;

    public static final String CLOUD_AUTH_ROUTING = "useCloudAuth";

    public static final String GA_SOURCE_OF_TRUTH = "gaIsSourceOfTruth";

    private org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = new org.openstack.docs.identity.api.v2.ObjectFactory();

    private org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory objectFactoryOSADMN = new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory();

    private org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory objectFactoryOSCATALOG = new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory();

    private com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory objectFactoryRAXKSKEY = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory();

    private com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory objectFactorySECRETQA = new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory();

    private com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory objectFactoryRAXGRP = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory();

    public static final Logger LOG = Logger.getLogger(DelegateCloud20Service.class);

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) {
        try {
            //Check for impersonated token if authenticating with token creds
            if (authenticationRequest.getToken() != null && !StringUtils.isBlank(authenticationRequest.getToken().getId())) {
                ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(authenticationRequest.getToken().getId());
                if (sa instanceof ImpersonatedScopeAccess) {
                    //check expiration
                    return authenticateImpersonated(httpHeaders, authenticationRequest, sa);
                }
            }

            //Get "user" from LDAP
            com.rackspace.idm.domain.entity.User user = cloudUserExtractor.getUserByV20CredentialType(authenticationRequest);
            if (userService.isMigratedUser(user)) {
                return defaultCloud20Service.authenticate(httpHeaders, authenticationRequest);
            }

            //Get Cloud Auth response
            String body = marshallObjectToString(objectFactory.createAuth(authenticationRequest));
            Response.ResponseBuilder serviceResponse = cloudClient.post(getCloudAuthV20Url() + TOKENS, httpHeaders, body);
            Response dummyResponse = serviceResponse.clone().build();
            //If SUCCESS and "user" is not null, store token to "user" and return cloud response
            int status = dummyResponse.getStatus();
            if (status == HttpServletResponse.SC_OK && user != null) {
                Token token = unmarshallAuthenticateResponse(dummyResponse.getEntity().toString()).getToken();

                if (token == null) {
                    throw new IdmException("Unable to sync tokens");
                }

                XMLGregorianCalendar authResExpires = token.getExpires();
                LOG.info("authResExpires = " + authResExpires);
                GregorianCalendar gregorianCalendar = authResExpires.toGregorianCalendar();
                LOG.info("GregorianCalander = " + gregorianCalendar);
                Date expires = gregorianCalendar.getTime();
                LOG.info("expires = " + expires);
                scopeAccessService.updateUserScopeAccessTokenForClientIdByUser(user, getCloudAuthClientId(), token.getId(), expires);
                return defaultCloud20Service.authenticate(httpHeaders, authenticationRequest);
            } else if (user == null) { //If "user" is null return cloud response
                return serviceResponse;
            } else { //If we get this far, return Default Service Response
                return defaultCloud20Service.authenticate(httpHeaders, authenticationRequest);
            }
        } catch (Exception ex) {
            LOG.info("unable to authenticate impersonated authenticationRequest successfully: " + ex.getMessage());
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    ResponseBuilder authenticateImpersonated(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest, ScopeAccess sa) {
        try {
            if (((HasAccessToken) sa).isAccessTokenExpired(new DateTime())) {
                throw new NotAuthorizedException("Token not authenticated");
            }
            ImpersonatedScopeAccess isa = (ImpersonatedScopeAccess) sa;
            com.rackspace.idm.domain.entity.User user = userService.getUserByAuthToken(isa.getImpersonatingToken());
            if (user == null) {
                authenticationRequest.getToken().setId(isa.getImpersonatingToken());
                String body = marshallObjectToString(objectFactory.createAuth(authenticationRequest));

                HttpHeadersAcceptXml httpHeadersAcceptXml = new HttpHeadersAcceptXml(httpHeaders);

                Response.ResponseBuilder serviceResponse = cloudClient.post(getCloudAuthV20Url() + TOKENS, httpHeadersAcceptXml, body);
                Response dummyResponse = serviceResponse.clone().build();
                int status = dummyResponse.getStatus();
                if (status == HttpServletResponse.SC_OK) {
                    // Need to replace token info with original from sa
                    AuthenticateResponse authenticateResponse = unmarshallAuthenticateResponse(dummyResponse.getEntity().toString());
                    if(authenticateResponse.getToken() == null){
                        throw new IdmException("Unable to sync tokens");
                    }
                    authenticateResponse.getToken().setId(isa.getAccessTokenString());
                    GregorianCalendar calendar = new GregorianCalendar();
                    calendar.setTime(isa.getAccessTokenExp());
                    try {
                        authenticateResponse.getToken().setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
                    } catch (DatatypeConfigurationException e) {
                        LOG.info("failed to create XMLGregorianCalendar: " + e.getMessage());
                    }
                    return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(authenticateResponse).getValue());
                }
                return serviceResponse;
            }
        } catch (Exception ex) {
            LOG.info("unable to authenticate impersonated authenticationRequest successfully: " + ex.getMessage());
            return exceptionHandler.exceptionResponse(ex);
        }

        return defaultCloud20Service.authenticate(httpHeaders, authenticationRequest);
    }

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
            {
        if (tokenId == null) {
            throw new BadRequestException("Token cannot be null.");
        }
        if (tokenId.trim().equals("")) {
            throw new BadRequestException("Token cannot be empty.");
        }
        validator20.validateToken(tokenId);
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenId);
        if (isCloudAuthRoutingEnabled() && scopeAccess == null) {
            String request = getCloudAuthV20Url() + TOKENS + "/" + tokenId;
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        if (scopeAccess instanceof ImpersonatedScopeAccess) {
            ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) scopeAccess;
            if(impersonatedScopeAccess.isAccessTokenExpired(new DateTime())){
                throw new NotAuthorizedException("Impersonated token has expired.");
            }
            ScopeAccess impersonatedUserScopeAccess = scopeAccessService.getScopeAccessByAccessToken(impersonatedScopeAccess.getImpersonatingToken());
            if (impersonatedUserScopeAccess == null) {
                authorizationService.verifyIdentityAdminLevelAccess(defaultCloud20Service.getScopeAccessForValidToken(authToken));
                return validateImpersonatedTokenFromCloud(httpHeaders, impersonatedScopeAccess.getImpersonatingToken(), belongsTo, impersonatedScopeAccess);
            } else {
                return defaultCloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo);
            }
        }
        return defaultCloud20Service.validateToken(httpHeaders, authToken, tokenId, belongsTo);
    }

    @Override
    public ResponseBuilder revokeToken(HttpHeaders httpHeaders, String authToken) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ResponseBuilder revokeToken(HttpHeaders httpHeaders, String authToken, String userToken) {
        throw new NotImplementedException("Not implemented");
    }

    ResponseBuilder validateImpersonatedTokenFromCloud(HttpHeaders httpHeaders, String impersonatedCloudToken, String belongsTo, ImpersonatedScopeAccess impersonatedScopeAccess) {
        String gaXAuthToken = getXAuthTokenByPassword(config.getString("ga.username"), config.getString("ga.password")).getToken().getId();
        httpHeaders.getRequestHeaders().get(X_AUTH_TOKEN).set(0, gaXAuthToken);
        httpHeaders.getRequestHeaders().get(HttpHeaders.ACCEPT).set(0, MediaType.APPLICATION_XML);
        Response cloudValidateResponse = checkToken(httpHeaders, gaXAuthToken, impersonatedCloudToken, belongsTo).build();
        if (cloudValidateResponse.getStatus() != HttpServletResponse.SC_OK && cloudValidateResponse.getStatus() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION) {
            return Response.status(cloudValidateResponse.getStatus()).entity(cloudValidateResponse.getEntity()).header("response-source", "cloud-auth");
        }
        AuthenticateResponse validateResponse = (AuthenticateResponse) unmarshallResponse(cloudValidateResponse.getEntity().toString(), AuthenticateResponse.class);
        validateResponse.getToken().setId(impersonatedScopeAccess.getAccessTokenString());

        validateResponse.setUser(validateResponse.getUser());
        validateResponse.setToken(validateResponse.getToken());

        com.rackspace.idm.domain.entity.User impersonator = userService.getUserByScopeAccess(impersonatedScopeAccess);
        List<TenantRole> impRoles = tenantService.getGlobalRolesForUser(impersonator, null);
        UserForAuthenticateResponse userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse(impersonator, impRoles);

        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory raxAuthObjectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
        JAXBElement<UserForAuthenticateResponse> impersonatorJAXBElement = raxAuthObjectFactory.createImpersonator(userForAuthenticateResponse);
        validateResponse.getAny().add(impersonatorJAXBElement);

        return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(validateResponse).getValue());
    }


    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
             {

        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + TOKENS + "/" + tokenId;

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("belongsTo", belongsTo);
            request = appendQueryParams(request, params);

            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.checkToken(httpHeaders, authToken, tokenId, belongsTo);
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId)  {
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenId);

        if (isCloudAuthRoutingEnabled() && scopeAccess == null) {
            String request = getCloudAuthV20Url() + TOKENS + "/" + tokenId + "/endpoints";
            return cloudClient.get(request, httpHeaders);
        }
        if (scopeAccess instanceof ImpersonatedScopeAccess) {
            ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) scopeAccess;
            String impersonatedTokenId = impersonatedScopeAccess.getImpersonatingToken();
            ScopeAccess impersonatedUserScopeAccess = scopeAccessService.getScopeAccessByAccessToken(impersonatedTokenId);
            if (impersonatedUserScopeAccess == null) {
                String request = getCloudAuthV20Url() + TOKENS + "/" + impersonatedTokenId + "/endpoints";
                return cloudClient.get(request, httpHeaders);
            }
            return defaultCloud20Service.listEndpointsForToken(httpHeaders, authToken, impersonatedTokenId);
        }
        return defaultCloud20Service.listEndpointsForToken(httpHeaders, authToken, tokenId);
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "extensions";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listExtensions(httpHeaders);
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)  {

        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "extensions/" + alias;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getExtension(httpHeaders, alias);
    }

    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken, Integer marker, Integer limit)  {
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
        if (scopeAccess != null) {
            return defaultCloud20Service.listUsers(httpHeaders, authToken, marker, limit);
        }

        if (isCloudAuthRoutingEnabled()) {
            String request = getCloudAuthV20Url() + USERS;
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUsers(httpHeaders, authToken, marker, limit);
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/RAX-KSGRP";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUserGroups(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder getGroupById(HttpHeaders httpHeaders, String authToken, String groupId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + RAX_GRPADM_GROUPS + groupId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getGroupById(httpHeaders, authToken, groupId);
    }

    @Override
    public ResponseBuilder addGroup(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Group group) {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups";
            String body = marshallObjectToString(objectFactoryRAXGRP.createGroup(group));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addGroup(httpHeaders, uriInfo, authToken, group);
    }

    @Override
    public ResponseBuilder updateGroup(HttpHeaders httpHeaders, String authToken, String groupId, Group group) {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + RAX_GRPADM_GROUPS + groupId;
            String body = marshallObjectToString(objectFactoryRAXGRP.createGroup(group));
            return cloudClient.put(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateGroup(httpHeaders, authToken, groupId, group);
    }

    @Override
    public ResponseBuilder deleteGroup(HttpHeaders httpHeaders, String authToken, String groupId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + RAX_GRPADM_GROUPS + groupId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteGroup(httpHeaders, authToken, groupId);
    }

    @Override
    public ResponseBuilder addUserToGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + RAX_GRPADM_GROUPS + groupId + "/" + USERS + "/" + userId;
            return cloudClient.put(request, httpHeaders, null);
        }
        return defaultCloud20Service.addUserToGroup(httpHeaders, authToken, groupId, userId);
    }

    @Override
    public ResponseBuilder removeUserFromGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + RAX_GRPADM_GROUPS + groupId + "/" + USERS + "/" + userId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.removeUserFromGroup(httpHeaders, authToken, groupId, userId);
    }

    @Override
    public ResponseBuilder getUsersForGroup(HttpHeaders httpHeaders, String authToken, String groupId, String marker, Integer limit)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            // TODO: Implement routing to DefaultCloud20Service
            String request = getCloudAuthV20Url() + RAX_GRPADM_GROUPS + groupId + "/users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUsersForGroup(httpHeaders, authToken, groupId, marker, limit);
    }

    @Override
    public ResponseBuilder getGroup(HttpHeaders httpHeaders, String authToken, String groupName)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(NAME, groupName);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listGroups(httpHeaders, authToken, groupName, null, null);
    }

    @Override
    public ResponseBuilder impersonate(HttpHeaders httpHeaders, String authToken, ImpersonationRequest impersonationRequest)  {
        return null;
    }

    @Override
    public ResponseBuilder listDefaultRegionServices(String authToken) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder setDefaultRegionServices(String authToken, DefaultRegionServices defaultRegionServices) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder addDomain(String authToken, UriInfo uriInfo, Domain domain) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getDomain(String authToken, String domainId) {
       return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder updateDomain(String authToken, String domainId, Domain domain) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder deleteDomain(String authToken, String domainId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getDomainTenants(String authToken, String domainId, String enabled) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getUsersByDomainId(String authToken, String domainId, String enabled) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder addUserToDomain(String authToken, String domainId, String userId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getEndpointsByDomainId(String authToken, String domainId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder addTenantToDomain(String authToken, String domainId, String tenantId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder removeTenantFromDomain(String authToken, String domainId, String tenantId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getPoliciesForEndpointTemplate(String authToken, String endpointTemplateId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder updatePoliciesForEndpointTemplate(String authToken, String endpointTemplateId, Policies policies) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder addPolicyToEndpointTemplate(String authToken, String endpointTemplateId, String policyId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder deletePolicyToEndpointTemplate(String authToken, String endpointTemplateId, String policyId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getPolicies(String authToken) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder addPolicy(UriInfo uriInfo, String authToken, Policy policy) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getPolicy(String authToken, String policyId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder updatePolicy(String authToken, String policyId, Policy policy) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder deletePolicy(String authToken, String policyId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getAccessibleDomains(String authToken) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getAccessibleDomainsForUser(String authToken, String userId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getAccessibleDomainsEndpointsForUser(String authToken, String userId, String domainId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder updateCapabilities(String token, Capabilities capabilities, String type, String version) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());
    }

    @Override
    public ResponseBuilder getCapabilities(String token, String type, String version) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());
    }

    @Override
    public ResponseBuilder removeCapabilities(String token, String type, String version) {
        return exceptionHandler.exceptionResponse(new NotImplementedException());
    }

    public ResponseBuilder addRegion(UriInfo uriInfo, String authToken, Region region) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getRegion(String authToken, String name) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getRegions(String authToken) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder updateRegion(String authToken, String name, Region region) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder deleteRegion(String authToken, String name) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder addQuestion(UriInfo uriInfo, String authToken, Question question) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getQuestion(String authToken, String questionId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getQuestions(String authToken) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder updateQuestion(String authToken, String questionId, Question question) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder deleteQuestion(String authToken, String questionId) {
        return exceptionHandler.exceptionResponse(new NotImplementedException()); //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name)  {
        if (isCloudAuthRoutingEnabled() && !userService.userExistsByUsername(name)) {
            String request = getCloudAuthV20Url() + USERS;
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(NAME, name);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserByName(httpHeaders, authToken, name);
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserById(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/" + ROLES;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUserGlobalRoles(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders, String authToken, String userId,
                                                          String serviceId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/" + ROLES;
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUserGlobalRolesByServiceId(httpHeaders, authToken, userId, serviceId);
    }

    @Override
    public ResponseBuilder listGroups(HttpHeaders httpHeaders, String authToken, String marker, String groupName, Integer limit)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "RAX-GRPADM/groups";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(NAME, groupName);
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listGroups(httpHeaders, authToken, groupName, marker, limit);
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit)  {
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
        if (isCloudAuthRoutingEnabled() && scopeAccess == null) {
            String request = getCloudAuthV20Url() + TENANTS;
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listTenants(httpHeaders, authToken, marker, limit);
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name)  {
        com.rackspace.idm.domain.entity.Tenant tenant = tenantService.getTenantByName(name);
        if (isCloudAuthRoutingEnabled() && tenant == null) {
            String request = getCloudAuthV20Url() + TENANTS;
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(NAME, name);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getTenantByName(httpHeaders, authToken, name);
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId)  {
        com.rackspace.idm.domain.entity.Tenant tenant = tenantService.getTenant(tenantsId);
        if (isCloudAuthRoutingEnabled() && tenant == null) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantsId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getTenantById(httpHeaders, authToken, tenantsId);
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String userId, String body)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String xmlBody = body;
            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                xmlBody = convertCredentialToXML(body);
            }
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/OS-KSADM/credentials";
            return cloudClient.post(request, httpHeaders, xmlBody);
        }
        return defaultCloud20Service.addUserCredential(httpHeaders, uriInfo, authToken, userId, body);
    }

    String convertCredentialToXML(String body) {
        JAXBElement<? extends CredentialType> jaxbCreds = null;
        String xml = null;

        CredentialType creds = JSONReaderForCredentialType
                .checkAndGetCredentialsFromJSONString(body);

        if (creds instanceof PasswordCredentialsRequiredUsername) {
            PasswordCredentialsRequiredUsername userCreds = (PasswordCredentialsRequiredUsername) creds;
            jaxbCreds = objectFactory.createPasswordCredentials(userCreds);
        } else if (creds instanceof ApiKeyCredentials) {
            ApiKeyCredentials userCreds = (ApiKeyCredentials) creds;
            jaxbCreds = objectFactoryRAXKSKEY
                    .createApiKeyCredentials(userCreds);
        }

        xml = marshallObjectToString(jaxbCreds);

        return xml;
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit)
             {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/OS-KSADM/credentials";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listCredentials(httpHeaders, authToken, userId, marker, limit);
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                         String credentialType, PasswordCredentialsRequiredUsername creds) {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + OS_KSADM_CREDENTIALS + credentialType;
            String body = marshallObjectToString(objectFactory.createPasswordCredentials(creds));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateUserPasswordCredentials(httpHeaders, authToken, userId, credentialType, creds);
    }

    @Override
    public ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId, String credentialType,
                                                       ApiKeyCredentials creds) {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + OS_KSADM_CREDENTIALS + credentialType;
            String body = marshallObjectToString(objectFactoryRAXKSKEY.createApiKeyCredentials(creds));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateUserApiKeyCredentials(httpHeaders, authToken, userId, credentialType, creds);
    }

    @Override
    public ResponseBuilder resetUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) {
        if(!isUserInGAbyId(userId)) {
            throw new NotImplementedException();
        }
        return defaultCloud20Service.resetUserApiKeyCredentials(httpHeaders, authToken, userId, credentialType);
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
             {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + OS_KSADM_CREDENTIALS + credentialType;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserCredential(httpHeaders, authToken, userId, credentialType);
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
             {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + OS_KSADM_CREDENTIALS + credentialType;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteUserCredential(httpHeaders, authToken, userId, credentialType);
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                    String userId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/" + USERS + "/" + userId + "/" + ROLES;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listRolesForUserOnTenant(httpHeaders, authToken, tenantId, userId);
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, UserForCreate user)
            {

        ScopeAccess accessTokenByAuthHeader = scopeAccessService.getAccessTokenByAuthHeader(authToken);
        boolean isUserAdminInGA = false;

        validator20.validateUserForCreate(user);

        if (accessTokenByAuthHeader != null) {
            isUserAdminInGA = authorizationService.authorizeCloudUserAdmin(accessTokenByAuthHeader);
        }
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth() && !isUserAdminInGA) {
            String request = getCloudAuthV20Url() + USERS;
            String body = marshallObjectToString(objectFactory.createUser(user));
            if (user != null && userService.userExistsByUsername(user.getUsername())) {
                throw new DuplicateUsernameException(String.format("Username %s already exists", user.getUsername()));
            }
            return cloudClient.post(request, httpHeaders, body);
        }
        if (user != null && !StringUtils.isBlank(user.getUsername())) {
            MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();
            String gaUserUsername = config.getString("ga.username");
            String gaUserPassword = config.getString("ga.password");
            requestHeaders.add(org.apache.http.HttpHeaders.AUTHORIZATION, getBasicAuth(gaUserUsername, gaUserPassword));
            //search for user in US Cloud Auth
            String uri = getCloudAuthV11Url() + USERS + "/" + user.getUsername();
            ResponseBuilder cloudAuthUSResponse = cloudClient.get(uri, httpHeaders);
            int status = cloudAuthUSResponse.build().getStatus();
            if (status == HttpServletResponse.SC_OK) {
                throw new DuplicateUsernameException(String.format("Username %s already exists", user.getUsername()));
            }
            //search for user in UK Cloud Auth
            String ukUri = getCloudAuthUKV11Url() + USERS + "/" + user.getUsername();
            ResponseBuilder cloudAuthUKResponse = cloudClient.get(ukUri, httpHeaders);
            status = cloudAuthUKResponse.build().getStatus();
            if (status == HttpServletResponse.SC_OK) {
                throw new DuplicateUsernameException(String.format("Username %s already exists", user.getUsername()));
            }
        }
        return defaultCloud20Service.addUser(httpHeaders, uriInfo, authToken, user);
    }

    private String getBasicAuth(String username, String password) {
        String usernamePassword = (new StringBuffer(username).append(":").append(password)).toString();
        byte[] base = usernamePassword.getBytes();
        return (new StringBuffer("Basic ").append(Base64.encode(base))).toString();
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, UserForCreate user)
            {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId;
            String body = marshallObjectToString(objectFactory.createUser(user));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateUser(httpHeaders, authToken, userId, user);
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId)  {
        if (isCloudAuthRoutingEnabled()) {

            com.rackspace.idm.domain.entity.User user = userService.getSoftDeletedUser(userId);
            if(user == null){
                user = userService.getUserById(userId);
            }
            if (user != null) {
                if (userService.isMigratedUser(user)) {
                    User updateUser = new User();
                    updateUser.setUsername(user.getUsername());
                    updateUser.setEnabled(false);
                    httpHeaders.getRequestHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
                    String body = marshallObjectToString(objectFactory.createUser(updateUser));
                    ResponseBuilder response = cloudClient.post(getCloudAuthV20Url() + USERS + "/" + userId, httpHeaders, body);
                    if (response.build().getStatus() == 200) {
                        return defaultCloud20Service.deleteUser(httpHeaders, authToken, userId);
                    } else {
                        throw new BadRequestException("Could not delete user");
                    }
                }
            }else{
                return cloudClient.delete(getCloudAuthV20Url() + USERS + "/" + userId, httpHeaders);
            }
        }
        return defaultCloud20Service.deleteUser(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId)  {
        if (config.getBoolean("allowSoftDeleteDeletion")) {
            return defaultCloud20Service.deleteUserFromSoftDeleted(httpHeaders, authToken, userId);
        } else {
            throw new NotFoundException("Not found");
        }
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId, User user)
            {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/OS-KSADM/enabled";
            String body = marshallObjectToString(objectFactory.createUser(user));
            return cloudClient.put(request, httpHeaders, body);
        }
        return defaultCloud20Service.setUserEnabled(httpHeaders, authToken, userId, user);
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + ROLES_OS_KSADM + roleId;
            return cloudClient.put(request, httpHeaders, null);
        }
        return defaultCloud20Service.addUserRole(httpHeaders, authToken, userId, roleId);
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + ROLES_OS_KSADM + roleId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getUserRole(httpHeaders, authToken, userId, roleId);
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + ROLES_OS_KSADM + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteUserRole(httpHeaders, authToken, userId, roleId);
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, org.openstack.docs.identity.api.v2.Tenant tenant)
            {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + TENANTS;
            String body = marshallObjectToString(objectFactory.createTenant(tenant));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addTenant(httpHeaders, uriInfo, authToken, tenant);
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId, org.openstack.docs.identity.api.v2.Tenant tenant)
            {
        if (isCloudAuthRoutingEnabled() && !isTenantInGAbyId(tenantId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId;
            String body = marshallObjectToString(objectFactory
                    .createTenant(tenant));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateTenant(httpHeaders, authToken, tenantId, tenant);
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId)  {
        if (isCloudAuthRoutingEnabled() && !isTenantInGAbyId(tenantId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteTenant(httpHeaders, authToken, tenantId);
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker,
                                              Integer limit)  {
        if (isCloudAuthRoutingEnabled() && !isTenantInGAbyId(tenantId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/OS-KSADM/roles";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listRolesForTenant(httpHeaders, authToken, tenantId, marker, limit);
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String roleId, String marker, Integer limit)
             {
        if (isCloudAuthRoutingEnabled() && !isTenantInGAbyId(tenantId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("roleId", roleId);
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUsersWithRoleForTenant(httpHeaders, authToken, tenantId, roleId, marker, limit);
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit)
             {
        if (isCloudAuthRoutingEnabled() && !isTenantInGAbyId(tenantId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/users";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listUsersForTenant(httpHeaders, authToken, tenantId, marker, limit);
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId)
             {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/" + USERS + "/" + userId + ROLES_OS_KSADM + roleId;
            return cloudClient.put(request, httpHeaders, null);
        }
        return defaultCloud20Service.addRolesToUserOnTenant(httpHeaders, authToken, tenantId, userId, roleId);
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId)
             {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/" + USERS + "/" + userId + ROLES_OS_KSADM + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteRoleFromUserOnTenant(httpHeaders, authToken, tenantId, userId, roleId);
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId, String marker, Integer limit)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listRoles(httpHeaders, authToken, serviceId, marker, limit);
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role) {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles";
            String body = marshallObjectToString(objectFactory.createRole(role));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addRole(httpHeaders, uriInfo, authToken, role);
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getRole(httpHeaders, authToken, roleId);
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/roles/" + roleId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteRole(httpHeaders, authToken, roleId);
    }

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(MARKER, marker);
            params.put(LIMIT, limit);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listServices(httpHeaders, authToken, marker, limit);
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service) {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services";
            String body = marshallObjectToString(objectFactoryOSADMN.createService(service));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addService(httpHeaders, uriInfo, authToken, service);
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services/" + serviceId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getService(httpHeaders, authToken, serviceId);
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSADM/services/" + serviceId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteService(httpHeaders, authToken, serviceId);
    }

    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders, String authToken, String serviceId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates";
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("serviceId", serviceId);
            request = appendQueryParams(request, params);
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listEndpointTemplates(httpHeaders, authToken, serviceId);
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, EndpointTemplate endpoint)
            {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates";
            String body = marshallObjectToString(objectFactoryOSCATALOG.createEndpointTemplate(endpoint));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addEndpointTemplate(httpHeaders, uriInfo, authToken, endpoint);
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates/" + endpointTemplateId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getEndpointTemplate(httpHeaders, authToken, endpointTemplateId);
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + "OS-KSCATALOG/endpointTemplates/" + endpointTemplateId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteEndpointTemplate(httpHeaders, authToken, endpointTemplateId);
    }

    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders, String authToken, String tenantId)  {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/OS-KSCATALOG/endpoints";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.listEndpoints(httpHeaders, authToken, tenantId);
    }

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint)
            {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/OS-KSCATALOG/endpoints";
            String body = marshallObjectToString(objectFactoryOSCATALOG.createEndpointTemplate(endpoint));
            return cloudClient.post(request, httpHeaders, body);
        }
        return defaultCloud20Service.addEndpoint(httpHeaders, authToken, tenantId, endpoint);
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders, String authToken, String endpointId, String tenantId)
             {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId
                    + "/OS-KSCATALOG/endpoints/" + endpointId;
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getEndpoint(httpHeaders, authToken, endpointId, tenantId);
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders, String authToken, String endpointId, String tenantId)
             {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String request = getCloudAuthV20Url() + TENANTS + "/" + tenantId + "/OS-KSCATALOG/endpoints/" + endpointId;
            return cloudClient.delete(request, httpHeaders);
        }
        return defaultCloud20Service.deleteEndpoint(httpHeaders, authToken, endpointId, tenantId);
    }

    @Override
    public ResponseBuilder getSecretQA(HttpHeaders httpHeaders, String authToken, String userId)  {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/RAX-KSQA/secretqa";
            return cloudClient.get(request, httpHeaders);
        }
        return defaultCloud20Service.getSecretQA(httpHeaders, authToken, userId);
    }

    @Override
    public ResponseBuilder updateSecretQA(HttpHeaders httpHeaders, String authToken, String userId, SecretQA secrets)
            {
        if (isCloudAuthRoutingEnabled() && !isUserInGAbyId(userId)) {
            String request = getCloudAuthV20Url() + USERS + "/" + userId + "/RAX-KSQA/secretqa";
            String body = marshallObjectToString(objectFactorySECRETQA.createSecretQA(secrets));
            return cloudClient.put(request, httpHeaders, body);
        }
        return defaultCloud20Service.updateSecretQA(httpHeaders, authToken, userId, secrets);
    }

    public String appendQueryParams(String request, Map<String, Object> params) {
        String result = "";
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value != null) {
                if (result.length() == 0) {
                    result += "?";
                } else {
                    result += "&";
                }
                String encodeValue;
                try {
                    encodeValue = URLEncoder.encode(value.toString(), JSONConstants.UTF_8);
                } catch (Exception e) {
                    throw new BadRequestException("Unable to encode query params.", e);
                }
                result += key + "=" + encodeValue;
            }
        }
        return request + result;
    }

    //TODO change way we check for media type

    AuthenticateResponse unmarshallAuthenticateResponse(String entity) {
        try {
            if (entity.trim().startsWith("{")) {
                //TODO: get more than just the token
                AuthenticateResponse authenticateResponse = new AuthenticateResponse();
                authenticateResponse.setToken(JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(entity));
                return authenticateResponse;
            } else {
                JAXBContext jc = JAXBContext.newInstance(AuthenticateResponse.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                StreamSource xml = new StreamSource(new StringReader(entity));
                JAXBElement ob = unmarshaller.unmarshal(xml, AuthenticateResponse.class);
                return (AuthenticateResponse) ob.getValue();

            }
        } catch (Exception ex) {
            LOG.info("Unable to unmarshall AuthenticateResponse from cloud");
            throw new IdmException("unable to unmarshall cloud response",ex);
        }
    }

    Object unmarshallResponse(String entity, Class<?> objectClass) {
        try {
            if (entity.trim().startsWith("{")) {
                //TODO: get more than just the token
                JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();
                JSONJAXBContext context = new JSONJAXBContext(jsonConfiguration, "org.openstack.docs.identity.api.v2");
                JSONUnmarshaller jsonUnmarshaller = context.createJSONUnmarshaller();
                JAXBElement ob = jsonUnmarshaller.unmarshalJAXBElementFromJSON(new StringReader(entity), objectClass);
                return ob.getValue();
            } else {
                JAXBContext jc = JAXBContext.newInstance(objectClass);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                StreamSource xml = new StreamSource(new StringReader(entity));
                JAXBElement ob = unmarshaller.unmarshal(xml, objectClass);
                return ob.getValue();

            }
        } catch (Exception ex) {
            return null;
        }
    }

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    private String getCloudAuthV20Url() {
        return config.getString("cloudAuth20url");
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }

    private String getCloudAuthUKV11Url() {
        return config.getString("cloudAuthUK11url");
    }

    boolean isUserInGAbyId(String userId) {
        return userService.userExistsById(userId);
    }

    boolean isTenantInGAbyId(String tenantId) {
        return tenantService.getTenant(tenantId) != null;
    }

    String marshallObjectToString(Object jaxbObject) {
        StringWriter sw = new StringWriter();
        try {
            Marshaller marshaller = JAXBContextResolver.get().createMarshaller();
            marshaller.marshal(jaxbObject, sw);
        } catch (Exception e) {
            throw new IdmException("failed to marshall with error: " + e.getMessage(), e);
        }
        return sw.toString();
    }

    private boolean isGASourceOfTruth() {
        return config.getBoolean(GA_SOURCE_OF_TRUTH);
    }

    private boolean isCloudAuthRoutingEnabled() {
        return config.getBoolean(CLOUD_AUTH_ROUTING);
    }

    public void setCloudUserExtractor(CloudUserExtractor cloudUserExtractor) {
        this.cloudUserExtractor = cloudUserExtractor;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setDefaultCloud20Service(DefaultCloud20Service defaultCloud20Service) {
        this.defaultCloud20Service = defaultCloud20Service;
    }

    public void setObjectFactory(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public AuthenticateResponse getXAuthToken(String userName, String apiKey) {
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername(userName);
        apiKeyCredentials.setApiKey(apiKey);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setCredential(objectFactoryRAXKSKEY.createApiKeyCredentials(apiKeyCredentials));
        String body = marshallObjectToString(objectFactory.createAuth(authenticationRequest));
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
        Response authResponse = cloudClient.post(getCloudAuthV20Url() + TOKENS, headers, body).build();
        if (authResponse.getStatus() != HttpServletResponse.SC_OK && authResponse.getStatus() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION) {
            throw new ApiException(authResponse.getStatus(), "", "");
        }
        return (AuthenticateResponse) unmarshallResponse(authResponse.getEntity().toString(), AuthenticateResponse.class);
    }

    public AuthenticateResponse getXAuthTokenByPassword(String userName, String password) {
        PasswordCredentialsRequiredUsername passwordCredentials = new PasswordCredentialsRequiredUsername();
        passwordCredentials.setUsername(userName);
        passwordCredentials.setPassword(password);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setCredential(objectFactory.createPasswordCredentials(passwordCredentials));
        String body = marshallObjectToString(objectFactory.createAuth(authenticationRequest));
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
        Response authResponse = cloudClient.post(getCloudAuthV20Url() + TOKENS, headers, body).build();
        if (authResponse.getStatus() != HttpServletResponse.SC_OK && authResponse.getStatus() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION) {
            throw new ApiException(authResponse.getStatus(), "", "");
        }
        return (AuthenticateResponse) unmarshallResponse(authResponse.getEntity().toString(), AuthenticateResponse.class);
    }

    public ApiKeyCredentials getUserApiCredentials(String userId, String xAuthToken)  {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        headers.put(X_AUTH_TOKEN, xAuthToken);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
        Response credsResponse = cloudClient.get(getCloudAuthV20Url() + USERS + "/" + userId + "/OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials", headers).build();
        if (credsResponse.getStatus() != HttpServletResponse.SC_OK && credsResponse.getStatus() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION) {
            throw new ApiException(credsResponse.getStatus(), "", "");
        }
        return (ApiKeyCredentials) unmarshallResponse(credsResponse.getEntity().toString(), ApiKeyCredentials.class);
    }

    public User getCloudUserByName(String userName, String xAuthToken)  {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        headers.put(X_AUTH_TOKEN, xAuthToken);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
        Response userResponse = cloudClient.get(getCloudAuthV20Url() + USERS + "?name=" + userName, headers).build();
        if (userResponse.getStatus() != HttpServletResponse.SC_OK && userResponse.getStatus() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION) {
            throw new NotFoundException("User cannot be impersonated; User is not found.");
        }
        return (User) unmarshallResponse(userResponse.getEntity().toString(), User.class);
    }

    public RoleList getGlobalRolesForCloudUser(String userId, String xAuthToken)  {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        headers.put(X_AUTH_TOKEN, xAuthToken);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
        Response userResponse = cloudClient.get(getCloudAuthV20Url() + USERS + "/" + userId + "/" + ROLES, headers).build();
        if (userResponse.getStatus() != HttpServletResponse.SC_OK && userResponse.getStatus() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION) {
            throw new ApiException(userResponse.getStatus(), "", "");
        }
        return (RoleList) unmarshallResponse(userResponse.getEntity().toString(), RoleList.class);
    }

    public String impersonateUser(String userName, String impersonatorName, String impersonatorPassword) {
        String impersonatorXAuthToken = getXAuthTokenByPassword(impersonatorName, impersonatorPassword).getToken().getId();
        User user = getCloudUserByName(userName, impersonatorXAuthToken);
        if (!user.isEnabled()) {
            throw new ForbiddenException("User cannot be impersonated; User is not enabled");
        }
        RoleList globalRolesForCloudUser = getGlobalRolesForCloudUser(user.getId(), impersonatorXAuthToken);
        if (!isValidCloudImpersonatee(globalRolesForCloudUser)) {
            throw new BadRequestException("User cannot be impersonated; No valid impersonation roles");
        }
        String userApiKey = getUserApiCredentials(user.getId(), impersonatorXAuthToken).getApiKey();
        return getXAuthToken(userName, userApiKey).getToken().getId();
    }


    public boolean isValidCloudImpersonatee(RoleList userRoles) {
        for (Role role : userRoles.getRole()) {
            String name = role.getName();
            if (name.equals("identity:default") || name.equals("identity:user-admin")) {
                return true;
            }
        }
        return false;
    }

    public TenantService getTenantService() {
        return tenantService;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public UserConverterCloudV20 getUserConverterCloudV20() {
        return userConverterCloudV20;
    }


    public void setUserConverterCloudV20(UserConverterCloudV20 userConverterCloudV20) {
        this.userConverterCloudV20 = userConverterCloudV20;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }

    public void setObjectFactoryRAXKSKEY(com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory objectFactoryRAXKSKEY) {
        this.objectFactoryRAXKSKEY = objectFactoryRAXKSKEY;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void setValidator20(Validator20 validator20) {
        this.validator20 = validator20;
    }
}
