package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.FederatedIdentityService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.util.SamlResponseValidator;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class is responsible for handling identity operations related
 * to identities that do not exist locally in the directory store
 *
 */
@Component
public class DefaultFederatedIdentityService implements FederatedIdentityService {

    @Autowired
    SamlResponseValidator samlResponseValidator;

    @Autowired
    IdentityProviderDao identityProviderDao;

    @Autowired
    FederatedUserDao federatedUserDao;

    @Autowired
    ApplicationRoleDao roleDao;

    @Autowired
    ScopeAccessService scopeAccessService;

    @Autowired
    TenantService tenantService;

    @Autowired
    private Configuration config;

    @Override
    public AuthData generateAuthenticationInfo(Response response) {
        SamlResponseDecorator samlResponse = new SamlResponseDecorator(response);
        samlResponseValidator.validate(samlResponse);

        String domainId = samlResponse.getAttribute(SAMLConstants.ATTR_DOMAIN).get(0);
        IdentityProvider provider = identityProviderDao.getIdentityProviderByUri(samlResponse.getIdpUri());
        User user = getOrCreateUser(samlResponse.getUsername(), domainId, provider);
        scopeAccessService.deleteExpiredTokens(user);

        FederatedToken token = createFederatedToken(user, provider, samlResponse.getAttribute(SAMLConstants.ATTR_ROLES), domainId);
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForFederatedToken(token);

        return getAuthData(user, token, endpoints, tenantRoles);
    }

    @Override
    public AuthData getAuthenticationInfo(FederatedToken token) {
        IdentityProvider provider = identityProviderDao.getIdentityProviderByName(token.getIdpName());
        User user = getUser(token.getUsername(), provider);
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForFederatedToken(token);

        return getAuthData(user, token, null, tenantRoles);
    }

    private User getOrCreateUser(String username, String domain, IdentityProvider provider) {
        User user = getUser(username, provider);
        if (user == null) {
            user = createUser(username, domain, provider);
        }

        return user;
    }

    private User getUser(String username, IdentityProvider provider) {
        User user = federatedUserDao.getUserByUsername(username, provider.getName());
        if (user != null) {
            user.setFederated(true);
            user.setFederatedIdp(provider.getUri());
        }
        return user;
    }

    private User createUser(String username, String domainId, IdentityProvider provider) {
        User user = new User();
        user.setUsername(username);
        user.setFederated(true);
        user.setFederatedIdp(provider.getUri());
        user.setDomainId(domainId);
        federatedUserDao.addUser(user, provider.getName());

        return user;
    }

    private FederatedToken createFederatedToken(User user, IdentityProvider provider, List<String> roles, String domainId) {
        FederatedToken token = new FederatedToken();
        token.setUserRsId(user.getId());
        token.setAccessTokenString(generateToken());
        token.setAccessTokenExp(new DateTime().plusSeconds(getDefaultCloudAuthTokenExpirationSeconds()).toDate());
        token.setUsername(user.getUsername());
        token.setClientId(getCloudAuthClientId());
        token.setIdpName(provider.getName());
        scopeAccessService.addUserScopeAccess(user, token);

        List<TenantRole> tenantRoles = getTenantRoles(roles, domainId);
        tenantService.addTenantRolesToFederatedToken(token, tenantRoles);

        return token;
    }

    private AuthData getAuthData(User user, FederatedToken token, List<OpenstackEndpoint> endpoints, List<TenantRole> tenantRoles) {
        AuthData authData = new AuthData();
        authData.setToken(token);
        authData.setUser(user);
        authData.setEndpoints(endpoints);

        token.setRoles(tenantRoles);
        user.setRoles(tenantRoles);

        return authData;
    }

    private List<TenantRole> getTenantRoles(List<String> roles, String domainId) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();

        List<Tenant> tenants = tenantService.getTenantsByDomainId(domainId);

        for (String role : roles) {
            ClientRole roleObj = roleDao.getRoleByName(role);
            TenantRole tenantRole = new TenantRole();
            tenantRole.setRoleRsId(roleObj.getId());
            tenantRole.setClientId(roleObj.getClientId());
            tenantRole.setName(roleObj.getName());
            tenantRole.setDescription(roleObj.getDescription());

            for (Tenant tenant : tenants) {
                tenantRole.getTenantIds().add(tenant.getTenantId());
            }

            tenantRoles.add(tenantRole);
        }

        return tenantRoles;
    }

    String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    int getDefaultCloudAuthTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }

    String getCloudAuthClientId() {
        //ScopeAccesses require a client id. This is legacy stuff that needs to be removed.
        //In the meantime, we're hard coding the cloud auth client id in the scope access.
        return config.getString("cloudAuth.clientId");
    }
}
