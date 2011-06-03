package com.rackspace.idm.web;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;

public class AcceptServlet extends HttpServlet  {
    
    private ClientService clientService;
    private UserService userService;
    private ScopeAccessService scopeAccessService;
    private Configuration config;
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        
        String redirectUri = request.getParameter("redirect_uri");
        String clientId = request.getParameter("client_id");
        String scopeList = request.getParameter("scope");
        String accept = request.getParameter("accept");
        String days = request.getParameter("days");
        String username = request.getParameter("username");
        String verification = request.getParameter("verificaiton");
        
        if (StringUtils.isBlank(verification)) {
            response.setStatus(400);
            return;
        }

        User user = getUserService().getUserBySecureId(verification);
        
        if (user == null || !verification.equalsIgnoreCase(user.getSecureId()) || !username.equals(user.getUsername())) {
            response.setStatus(400);
            return;
        }
        
        //remove the secureId since its no longer needed
        user.setSecureId("");
        getUserService().updateUser(user, false);
        
        if (!accept.equals("Accept")) {
            URI uri = UriBuilder.fromPath(redirectUri)
            .queryParam("error", "access_denied").build();
        response.setStatus(302);
        response.setHeader("Location", uri.toString());
        return;
        }

        String authCode = generateAuthCode();

        Client client = getClientService().getById(clientId);

        DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) getScopeAccessService()
            .getDelegateScopeAccessForParentByClientId(user.getUniqueId(),
                client.getClientId());

        if (dcsa == null) {
            dcsa = new DelegatedClientScopeAccess();
            dcsa.setClientId(client.getClientId());
            dcsa.setClientRCN(client.getCustomerId());
            dcsa.setUsername(user.getUsername());
            dcsa.setUserRCN(user.getCustomerId());
            dcsa = (DelegatedClientScopeAccess) getScopeAccessService()
                .addDelegateScopeAccess(user.getUniqueId(), dcsa);
        }
        
        int numberOfDays = 0;

        try {
            numberOfDays = Integer.parseInt(days);
        } catch (NumberFormatException ex) {
            response.setStatus(400);
            return;
        }

        DateTime current = new DateTime();
        
        if (numberOfDays > 0) {
            dcsa.setRefreshTokenExp(current.plusDays(numberOfDays).toDate());
        } else {
            dcsa.setRefreshTokenExp(current.plusYears(100).toDate());
        }

        dcsa.setAuthCode(authCode);
        dcsa.setAuthCodeExp(current.plusSeconds(getAuthCodeExpirationSeconds()).toDate());
        getScopeAccessService().updateScopeAccess(dcsa);

        List<Client> clients = new ArrayList<Client>();
        String[] scopes = scopeList.split(" ");

        for (String s : scopes) {
            Client c = getClientService().getClientByScope(s);
            if (c != null) {
                clients.add(c);
            }
        }

        for (Client c : clients) {
            ScopeAccess sa = getScopeAccessService()
                .getDirectScopeAccessForParentByClientId(user.getUniqueId(),
                    c.getClientId());

            if (sa != null) {
                ScopeAccess newSa = new ScopeAccess();
                newSa.setClientId(sa.getClientId());
                newSa.setClientRCN(sa.getClientRCN());
                newSa = getScopeAccessService().addDirectScopeAccess(
                    dcsa.getUniqueId(), newSa);

                Permission filter = new Permission();
                filter.setClientId(c.getClientId());
                List<Permission> perms = getScopeAccessService()
                    .getPermissionsForParent(newSa.getUniqueId(), filter);

                for (Permission p : perms) {
                    if (p instanceof GrantedPermission) {
                        GrantedPermission gp = (GrantedPermission) p;
                        DelegatedPermission perm = new DelegatedPermission();
                        perm.setClientId(gp.getClientId());
                        perm.setCustomerId(gp.getCustomerId());
                        perm.setPermissionId(gp.getPermissionId());
                        perm.setResourceGroups(gp.getResourceGroups());
                        getScopeAccessService().delegatePermission(
                            sa.getUniqueId(), perm);
                    }
                }
            }
        }
        URI uri = UriBuilder.fromPath(redirectUri).queryParam("code", authCode)
            .build();
        response.setStatus(302);
        response.setHeader("Location", uri.toString());
        return;
    }

    private ClientService getClientService() {
        if (clientService == null) {
            WebApplicationContext context = WebApplicationContextUtils
                .getWebApplicationContext(getServletContext());
            clientService = context.getBean(ClientService.class);
        }
        return clientService;
    }

    private UserService getUserService() {
        if (userService == null) {
            WebApplicationContext context = WebApplicationContextUtils
                .getWebApplicationContext(getServletContext());
            userService = context.getBean(UserService.class);
        }
        return userService;
    }

    private ScopeAccessService getScopeAccessService() {
        if (scopeAccessService == null) {
            WebApplicationContext context = WebApplicationContextUtils
                .getWebApplicationContext(getServletContext());
            scopeAccessService = context.getBean(ScopeAccessService.class);
        }
        return scopeAccessService;
    }
    
    private String generateAuthCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private int getAuthCodeExpirationSeconds() {
        if (config == null) {
            WebApplicationContext context = WebApplicationContextUtils
            .getWebApplicationContext(getServletContext());
        config = context.getBean(Configuration.class);
        }
        return config.getInt("authcode.expiration.seconds", 20);
    }
}
