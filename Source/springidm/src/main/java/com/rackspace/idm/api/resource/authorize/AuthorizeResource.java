package com.rackspace.idm.api.resource.authorize;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.token.TokenResource;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;

@Component
public class AuthorizeResource {

    private final ClientService clientService;
    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    final private Logger logger = LoggerFactory.getLogger(TokenResource.class);
    
    private static final String UTF8 = "UTF-8";

    @Autowired(required = true)
    public AuthorizeResource(
        ScopeAccessService scopeAccessService, ClientService clientService,
        UserService userService) {
        this.scopeAccessService = scopeAccessService;
        this.clientService = clientService;
        this.userService = userService;
    }

    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML})
    public Response verifyCreds(@Context UriInfo uriInfo,
        @FormParam("response_type") String responseType,
        @FormParam("client_id") String clientId,
        @FormParam("redirect_uri") String redirectUri,
        @FormParam("scope") String scope,
        @FormParam("username") String username,
        @FormParam("password") String password) {

        UserAuthenticationResult uaResult = this.userService.authenticate(
            username, password);
        if (!uaResult.isAuthenticated()) {
            // TODO: How to handle incorrect credentials
            throw new IllegalStateException();
        }

        URI uri = UriBuilder.fromPath(uriInfo.getPath() + "/accept")
            .queryParam("response_type", responseType)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri).queryParam("scope", scope)
            .queryParam("username", username).build();

        return Response.status(302).location(uri).build();
    }

    @GET
    @Consumes({MediaType.TEXT_HTML})
    @Produces({MediaType.TEXT_HTML})
    public Response getAuthorizationCode(
        @QueryParam("response_type") String responseType,
        @QueryParam("client_id") String clientId,
        @QueryParam("redirect_uri") String redirectUri,
        @QueryParam("scope") String scope) {

        if (StringUtils.isBlank(redirectUri)) {
            String errMsg = "redirect_uri cannot be blank";
            logger.warn(errMsg);
            return Response.status(400).build();
        }

        if (StringUtils.isBlank(responseType) || StringUtils.isBlank(clientId)
            || StringUtils.isBlank(scope)) {
            URI uri = UriBuilder.fromPath(redirectUri)
                .queryParam("error", "invalid_request").build();
            return Response.status(302).location(uri).build();
        }

        String[] scopes = scope.split(" ");
        for (String s : scopes) {
            Client c = this.clientService.getClientByScope(s);
            if (c == null) {
                URI uri = UriBuilder.fromPath(redirectUri)
                    .queryParam("error", "invalid_scope").build();
                return Response.status(302).location(uri).build();
            }
        }

        Client client = this.clientService.getById(clientId);
        if (client == null || client.isDisabled()) {
            URI uri = UriBuilder.fromPath(redirectUri)
                .queryParam("error", "unauthorized_client").build();
            return Response.status(302).location(uri).build();
        }

        String htmlForm = "<html><head><title>Global Auth Login Page</title></head><body><form name='input' action='authorize' method='POST'>Username: <input type='text' name='username' /><br/>Password: <input type='password' name='password' /><br/><input type='submit' value='Login' /><input type='hidden' name='redirect_uri' value='%s'><input type='hidden' name='client_id' value='%s'><input type='hidden' name='response_type' value='%s'><input type='hidden' name='scope' value='%s'></form></body></html>";
        try {
            htmlForm = String.format(htmlForm, URLEncoder.encode(redirectUri, UTF8),
                URLEncoder.encode(clientId, UTF8), URLEncoder.encode(responseType, UTF8),
                URLEncoder.encode(scope, UTF8));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return Response.ok(htmlForm).build();
    }

    @Path("accept")
    @GET
    @Consumes({MediaType.TEXT_HTML})
    @Produces({MediaType.TEXT_HTML})
    public Response getUserAcceptance(
        @QueryParam("response_type") String responseType,
        @QueryParam("client_id") String clientId,
        @QueryParam("redirect_uri") String redirectUri,
        @QueryParam("scope") String scope,
        @QueryParam("username") String username) {

        Client client = this.clientService.getById(clientId);

        User user = this.userService.getUser(username);

        List<Client> clients = new ArrayList<Client>();
        String[] scopes = scope.split(" ");

        for (String s : scopes) {
            Client c = this.clientService.getClientByScope(s);
            if (c == null) {
                URI uri = UriBuilder.fromPath(redirectUri)
                    .queryParam("error", "invalid_scope").build();
                return Response.status(302).location(uri).build();
            }
            clients.add(c);
        }

        StringBuilder stuff = new StringBuilder();

        for (Client c : clients) {
            ScopeAccess sa = this.scopeAccessService
                .getDelegateScopeAccessForParentByClientId(user.getUniqueId(),
                    c.getClientId());
            if (sa == null) {
                URI uri = UriBuilder.fromPath(redirectUri)
                    .queryParam("error", "invalid_scope").build();
                return Response.status(302).location(uri).build();
            }
            stuff.append("<p>");
            stuff.append(c.getTitle());
            stuff.append(" - ");
            stuff.append(c.getDescription());
            stuff.append("</p>");
        }

        String htmlForm = "<html><head><title>Global Auth Login Page</title></head><body><form name='input' action='accept' method='POST'><p>%s</p><p>%s</p><p>wants access to</p><p>%s</p><br/><input name='accept' type='submit' value='Accept' /><input name='accept' type='submit' value='Deny' /><input type='hidden' name='redirect_uri' value='%s'><input type='hidden' name='client_id' value='%s'><input type='hidden' name='response_type' value='%s'><input type='hidden' name='scope' value='%s'><input type='hidden' name='username' value='%s'></form></body></html>";

        htmlForm = String.format(htmlForm, client.getTitle(),
            client.getDescription(), stuff.toString(), redirectUri, clientId,
            responseType, scope, username);
        return Response.ok(htmlForm).build();
    }

    @Path("accept")
    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML})
    public Response verifyUserAcceptance(
        @FormParam("response_type") String responseType,
        @FormParam("client_id") String clientId,
        @FormParam("redirect_uri") String redirectUri,
        @FormParam("scope") String scope,
        @FormParam("username") String username,
        @FormParam("accept") String accept) {
        
        if (!accept.equals("Accept")) {
            URI uri = UriBuilder.fromPath(redirectUri)
            .queryParam("error", "access_denied").build();
        return Response.status(302).location(uri).build();
        }

        String authCode = generateAuthCode();

        User user = this.userService.getUser(username);
        Client client = this.clientService.getById(clientId);

        DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) this.scopeAccessService
            .getDelegateScopeAccessForParentByClientId(user.getUniqueId(),
                client.getClientId());

        if (dcsa == null) {
            dcsa = new DelegatedClientScopeAccess();
            dcsa.setClientId(client.getClientId());
            dcsa.setClientRCN(client.getCustomerId());
            dcsa.setUsername(user.getUsername());
            dcsa.setUserRCN(user.getCustomerId());
            dcsa = (DelegatedClientScopeAccess) this.scopeAccessService
                .addDelegateScopeAccess(user.getUniqueId(), dcsa);
        }

        // TODO: Implement User Choice for how long token lasts
//        if (days > 0) {
//            dcsa.setRefreshTokenExp(current.plusDays(days).toDate());
//        } else {
//            dcsa.setRefreshTokenExp(current.plusYears(100).toDate());
//        }
        DateTime current = new DateTime();
        
        dcsa.setRefreshTokenExp(current.plusYears(100).toDate());

        dcsa.setAuthCode(authCode);
        dcsa.setAuthCodeExp(current.plusSeconds(20).toDate());
        this.scopeAccessService.updateScopeAccess(dcsa);

        List<Client> clients = new ArrayList<Client>();
        String[] scopes = scope.split(" ");

        for (String s : scopes) {
            Client c = this.clientService.getClientByScope(s);
            if (c != null) {
                clients.add(c);
            }
        }

        for (Client c : clients) {
            ScopeAccess sa = this.scopeAccessService
                .getDirectScopeAccessForParentByClientId(user.getUniqueId(),
                    c.getClientId());

            if (sa != null) {
                ScopeAccess newSa = new ScopeAccess();
                newSa.setClientId(sa.getClientId());
                newSa.setClientRCN(sa.getClientRCN());
                newSa = this.scopeAccessService.addDirectScopeAccess(
                    dcsa.getUniqueId(), newSa);

                Permission filter = new Permission();
                filter.setClientId(c.getClientId());
                List<Permission> perms = this.scopeAccessService
                    .getPermissionsForParent(newSa.getUniqueId(), filter);

                for (Permission p : perms) {
                    if (p instanceof GrantedPermission) {
                        GrantedPermission gp = (GrantedPermission) p;
                        DelegatedPermission perm = new DelegatedPermission();
                        perm.setClientId(gp.getClientId());
                        perm.setCustomerId(gp.getCustomerId());
                        perm.setPermissionId(gp.getPermissionId());
                        perm.setResourceGroups(gp.getResourceGroups());
                        this.scopeAccessService.delegatePermission(
                            sa.getUniqueId(), perm);
                    }

                }
            }
        }
        URI uri = UriBuilder.fromPath(redirectUri).queryParam("code", authCode)
            .build();
        return Response.status(302).location(uri).build();
    }

    // This call is just here for testing purposes
    // TODO: Remove before pushing to production
    @Path("callback")
    @GET
    @Consumes({MediaType.TEXT_HTML})
    @Produces({MediaType.TEXT_HTML})
    public Response getCallback(@QueryParam("error") String error,
        @QueryParam("code") String code) {

        String show = "";
        if (!StringUtils.isBlank(error)) {
            show = "Error: " + error;
        }
        if (!StringUtils.isBlank(code)) {
            show = "Code: " + code;
        }

        return Response.ok(show).build();
    }

    private String generateAuthCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
