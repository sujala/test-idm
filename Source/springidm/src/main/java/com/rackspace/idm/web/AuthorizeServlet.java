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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.UserDisabledException;

public class AuthorizeServlet extends HttpServlet {

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ClientService clientService;
    private UserService userService;
    private ScopeAccessService scopeAccessService;
    
    private static final String INVALID_REQUEST = "invalid_request";
    private static final String ACCESS_DENIED = "access_denied";
    private static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    private static final String INVALID_SCOPE = "invalid_scope";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String redirectUri = request.getParameter("redirect_uri");
        String responseType = request.getParameter("response_type");
        String clientId = request.getParameter("client_id");
        String scopeList = request.getParameter("scope");

        if (StringUtils.isBlank(redirectUri)) {
            String errMsg = "redirect_uri cannot be blank";
            logger.warn(errMsg);
            response.setStatus(400);
            return;
        }

        if (StringUtils.isBlank(responseType) || StringUtils.isBlank(clientId)
            || StringUtils.isBlank(scopeList)) {
            setErrorResponse(response, redirectUri, INVALID_REQUEST);
            return;
        }
        
        if (!responseType.equals("code")) {
            setErrorResponse(response, redirectUri, INVALID_REQUEST);
            return;
        }

        String[] scopes = scopeList.split(" ");
        for (String s : scopes) {
            Client c = getClientService().getClientByScope(s);
            if (c == null) {
                setErrorResponse(response, redirectUri, INVALID_SCOPE);
                return;
            }
        }

        Client client = getClientService().getById(clientId);
        if (client == null || client.isDisabled()) {
            setErrorResponse(response, redirectUri, UNAUTHORIZED_CLIENT);
            return;
        }

        request.getRequestDispatcher("/web/login.jsp").forward(request,
            response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String redirectUri = request.getParameter("redirect_uri");
        String responseType = request.getParameter("response_type");
        String clientId = request.getParameter("client_id");
        String scopeList = request.getParameter("scope");

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (StringUtils.isBlank(redirectUri)) {
            String errMsg = "redirect_uri cannot be blank";
            logger.warn(errMsg);
            response.setStatus(400);
            return;
        }

        if (StringUtils.isBlank(responseType) || StringUtils.isBlank(clientId)
            || StringUtils.isBlank(scopeList) || StringUtils.isBlank(username)
            || StringUtils.isBlank(password)) {
            setErrorResponse(response, redirectUri, INVALID_REQUEST);
            return;
        }
        
        if (!responseType.equals("code")) {
            setErrorResponse(response, redirectUri, INVALID_REQUEST);
            return;
        }

        List<Client> clients = new ArrayList<Client>();
        String[] scopes = scopeList.split(" ");
        for (String s : scopes) {
            Client c = getClientService().getClientByScope(s);
            if (c == null) {
                setErrorResponse(response, redirectUri, INVALID_SCOPE);
                return;
            }
            clients.add(c);
        }

        Client client = getClientService().getById(clientId);
        if (client == null || client.isDisabled()) {
            setErrorResponse(response, redirectUri, UNAUTHORIZED_CLIENT);
            return;
        }

        UserAuthenticationResult uaResult = null;
        try {
            uaResult = getUserService().authenticate(username, password);
        } catch (UserDisabledException ex) {
            setErrorResponse(response, redirectUri, ACCESS_DENIED);
            return;
        }

        if (!uaResult.isAuthenticated()) {
            request.setAttribute("error", "Incorrect Credentials");
            request.getRequestDispatcher("/web/login.jsp").forward(request,
                response);
            return;
        }

        for (Client c : clients) {
            ScopeAccess sa = getScopeAccessService()
                .getDirectScopeAccessForParentByClientId(
                    uaResult.getUser().getUniqueId(), c.getClientId());
            if (sa == null) {
                setErrorResponse(response, redirectUri, INVALID_SCOPE);
                return;
            }
        }

        User user = getUserService().getUser(username);
        String secureId = generateSecureId();
        user.setSecureId(secureId);
        getUserService().updateUser(user, false);

        request.setAttribute("requestingClient", client);
        request.setAttribute("scopes", clients);
        request.setAttribute("username", uaResult.getUser().getUsername());
        request.setAttribute("verification", secureId);

        request.getRequestDispatcher("/web/scope.jsp").forward(request,
            response);

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

    private String generateSecureId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private void setErrorResponse(HttpServletResponse response,
        String redirectUri, String errMsg) {
        URI uri = UriBuilder.fromPath(redirectUri)
            .queryParam("error", errMsg).build();
        response.setStatus(302);
        response.setHeader("Location", uri.toString());
        return;
    }
}
