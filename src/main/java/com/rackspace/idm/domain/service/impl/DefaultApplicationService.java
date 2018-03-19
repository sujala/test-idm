package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.api.security.ImmutableTenantRole;
import com.rackspace.idm.domain.config.CacheConfiguration;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.HashHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DefaultApplicationService implements ApplicationService {

    @Autowired
    private ApplicationDao applicationDao;
    @Autowired
    private ApplicationRoleDao applicationRoleDao;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private Configuration config;
    @Autowired
    private RoleService roleService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void add(Application client) {
        logger.debug("Adding Client: {}", client);

        Application existingApplication = applicationDao.getApplicationByName(client.getName());
        if (existingApplication != null) {
            logger.warn("Couldn't add client {} because clientname already taken", client);
            throw new DuplicateException(String.format("Clientname %s already exists", client.getName()));
        }

        client.setClientId(HashHelper.makeSHA1Hash(client.getName()));
        client.setClientSecretObj(ClientSecret.newInstance(HashHelper.getRandomSha1()));

        applicationDao.addApplication(client);
        logger.debug("Added Client: {}", client);
    }

    @Override
    public void delete(String clientId) {
        logger.debug("Delete Client: {}", clientId);
        Application client = applicationDao.getApplicationByClientId(clientId);

        if (client == null) {
            String errMsg = String.format("Client with clientId %s not found.", clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        for (ClientRole role : applicationRoleDao.getClientRolesForApplication(client)) {
            this.deleteClientRole(role);
        }

        applicationDao.deleteApplication(client);
        logger.debug("Deleted Client: {}", clientId);
    }

    @Override
    public Application loadApplication(String applicationId) {
        Application client = applicationDao.getApplicationByClientId(applicationId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", applicationId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return client;
    }

    @Override
    public Application getById(String clientId) {
        return applicationDao.getApplicationByClientId(clientId);
    }

    @Override
    public Application checkAndGetApplication(String applicationId) {
        Application application = getById(applicationId);
        if (application == null) {
            String errMsg = String.format("Service %s not found", applicationId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return application;
    }

    @Override
    public Application checkAndGetApplicationByName(String name) {
        Application application = getByName(name);
        if (application == null) {
            String errMsg = String.format("Service %s not found", name);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return application;
    }

    @Override
    public Application getByName(String clientName) {
        return applicationDao.getApplicationByName(clientName);
    }

    @Override
    public Iterable<Application> getByType(String type) {
        return applicationDao.getApplicationByType(type);
    }

    @Override
    public ClientSecret resetClientSecret(Application client) {
        if (client == null) {
            throw new IllegalArgumentException();
        }
        logger.debug("Reseting Client secret ClientId: {}", client.getClientId());

        ClientSecret clientSecret;
        clientSecret = ClientSecret.newInstance(HashHelper.getRandomSha1());
        client.setClientSecretObj(clientSecret);
        applicationDao.updateApplication(client);
        logger.debug("Reset Client secret ClientId: {}", client.getClientId());
        return clientSecret;
    }

    @Override
    public void save(Application client) {
        applicationDao.updateApplication(client);
    }

    @Override
    public Applications getClientServices(Application client) {
        logger.debug("Finding Client Services for Client: {}", client.getClientId());
        if (client == null || client.getUniqueId() == null) {
            throw new IllegalArgumentException("Client cannot be null and must have uniqueID");
        }

        List<Application> clientList = new ArrayList<Application>();

        for (ScopeAccess service : scopeAccessService.getScopeAccessesForApplication(client)) {
            if (service != null) {
                clientList.add(this.getById(service.getClientId()));
            }
        }

        Applications clients = new Applications();
        clients.setClients(clientList);
        clients.setOffset(0);
        clients.setLimit(clientList.size());
        clients.setTotalRecords(clientList.size());

        logger.debug("Found {} Client Service(s) for Client: {}", clientList.size(), client.getClientId());
        return clients;
    }

    @Override
    public Application getApplicationByScopeAccess(ScopeAccess scopeAccess) {
        if(scopeAccess == null) {
            throw new IllegalArgumentException("ScopeAccess cannot be null");
        }
        String clientId = scopeAccessService.getClientIdForParent(scopeAccess);
        if(clientId == null) {
            String err = String.format("Application with clientId %s not found", clientId);
            logger.error(err);
            throw new NotFoundException(err);
        }
        return applicationDao.getApplicationByClientId(clientId);
    }

    @Override
    public void updateClient(Application client) {
        logger.info("Updating Client: {}", client);
        this.applicationDao.updateApplication(client);
        logger.info("Updated Client: {}", client);
    }

    @Override
    public void addClientRole(ClientRole role) {
        addClientRole(role, applicationRoleDao.getNextRoleId());
    }

    @Override
    public void addClientRole(ClientRole role, String roleId) {
        logger.info("Adding Client Role: {}", role);
        Application application = applicationDao.getApplicationByClientId(role.getClientId());
        if (application == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        ClientRole exists = applicationRoleDao.getClientRoleByApplicationAndName(application, role);
        if (exists != null) {
            String errMsg = String.format("Role with name %s already exists", role.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
        role.setId(roleId);
        applicationRoleDao.addClientRole(application, role);
        logger.info("Added Client Role: {}", role);
    }

    @Override
    public void deleteClientRole(ClientRole role) {
        logger.info("Delete Client Role: {}", role);
        
        for (TenantRole tenantRole : tenantService.getTenantRolesForClientRole(role)) {
            tenantService.deleteTenantRole(tenantRole);
        }
        
        this.applicationRoleDao.deleteClientRole(role);
        logger.info("Deleted Client Role: {}", role);
    }

    @Override
    public void updateClientRole(ClientRole role) {
        logger.info("Update Client Role: {}", role);
        this.applicationRoleDao.updateClientRole(role);
        logger.info("Udpated Client Role: {}", role);
    }

    @Override
    public Iterable<ClientRole> getClientRolesByClientId(String clientId) {
        logger.debug("Getting Client Roles for client: {}", clientId);
        Application application = applicationDao.getApplicationByClientId(clientId);
        if (application == null) {
            throw new NotFoundException(String.format("Client with id %s does not exit", clientId));
        }
        return this.applicationRoleDao.getClientRolesForApplication(application);
    }

    @Override
    public Iterable<ClientRole> getClientRolesByRoleType(RoleTypeEnum roleType) {
        if (roleType == null) {
            throw new IllegalArgumentException("Role type required.");
        }

        logger.debug("Getting Client Roles with role type: {}", roleType);

        return this.applicationRoleDao.getClientRolesWithRoleType(roleType);
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesByName(String roleName, int maxWeightAvailable, int offset, int limit) {
        if (StringUtils.isBlank(roleName)) {
            return null;
        }

        logger.debug("Getting ClientRole with name {} and weight {}", roleName, maxWeightAvailable);
        PaginatorContext<ClientRole> context = applicationRoleDao.getAvailableClientRolesByName(roleName, maxWeightAvailable, offset, limit);
        logger.debug("Got {} Client Roles", context.getTotalRecords());
        return context;
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable) {
        logger.debug("Getting all Client Roles page: {}");
        PaginatorContext<ClientRole> context = applicationRoleDao.getAvailableClientRolesPaged(applicationId, offset, limit, maxWeightAvailable);
        logger.debug("Got {} Client Roles", context.getTotalRecords());
        return context;
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable) {
        logger.debug("Getting all Client Roles page: {}");
        PaginatorContext<ClientRole> context = applicationRoleDao.getAvailableClientRolesPaged(offset, limit, maxWeightAvailable);
        logger.debug("Got {} Client Roles", context.getTotalRecords());
        return context;
    }
    
    @Override
    public ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName) {
        logger.debug("Getting Client Role {} for client {}", roleName, clientId);
        ClientRole role = this.applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName);
        logger.debug("Got Client Role {} for client {}", roleName, clientId);
        return role;
    }

    @Override
    public ClientRole getClientRoleById(String roleId) {
        logger.debug("Getting Client Role {}", roleId);
        ClientRole role = this.applicationRoleDao.getClientRole(roleId);
        logger.debug("Got Client Role {}", roleId);
        return role;
    }

    @Override
    @Cacheable(value = CacheConfiguration.CLIENT_ROLE_CACHE_BY_ID, unless="#result == null")
    public ImmutableClientRole getCachedClientRoleById(String roleId) {
        logger.trace("ClientRole cache miss. Retrieving client role with id {}", roleId);
        ClientRole role = this.applicationRoleDao.getClientRole(roleId);
        logCaching(role, roleId);

        return role != null ? new ImmutableClientRole(role) : null;
    }

    @Override
    @Cacheable(value = CacheConfiguration.CLIENT_ROLE_CACHE_BY_NAME, unless="#result == null")
    public ImmutableClientRole getCachedClientRoleByName(String name) {
        logger.trace("ClientRole cache miss. Retrieving client role with name {}", name);
        ClientRole role = this.applicationRoleDao.getRoleByName(name);
        logCaching(role, name);

        return role != null ? new ImmutableClientRole(role) : null;
    }

    private void logCaching(ClientRole role, String value) {
        if (role != null) {
            logger.trace("Retrieved Client Role {} for cache", value);
        } else {
            logger.trace("Client Role {} not found. Will not populate null result in cache", value);
        }
    }

    @Override
    public Iterable<Application> getOpenStackServices() {
        logger.debug("Getting Open Stack Services");
        return this.applicationDao.getOpenStackServices();
    }

    @Override
    public PaginatorContext<Application> getOpenStackServices(int offset, int limit) {
        logger.debug("Getting OpenStack Services");
        return this.applicationDao.getOpenStackServices(offset, limit);
    }

    @Override
    public ClientRole getUserIdentityRole(EndUser user) {
        List<ClientRole> result = new ArrayList<ClientRole>();

        logger.debug("getting identity:* role for user: {}", user);

        List<ClientRole> clientRoles = roleService.getIdentityAccessRoles();

        Set<String> tenantRoleIds = new HashSet<String>();

        if (clientRoles.size() > 0) {
            if (user instanceof ProvisionedUserDelegate) {
               Collection<TenantRole> tenantRoles =  CollectionUtils.collect(((ProvisionedUserDelegate) user).getDefaultDomainRoles(), new Transformer<ImmutableTenantRole, TenantRole>() {
                    @Override
                    public TenantRole transform(ImmutableTenantRole input) {
                        return input.asTenantRole();
                    }
                });
                for (TenantRole tenantRole : tenantRoles) {
                    tenantRoleIds.add(tenantRole.getRoleRsId());
                }
            } else {
                for (TenantRole tenantRole : tenantService.getTenantRolesForUserById(user, clientRoles)) {
                    tenantRoleIds.add(tenantRole.getRoleRsId());
                }
            }
        }

        for (ClientRole role : clientRoles) {
            if (tenantRoleIds.contains(role.getId())) {
                result.add(role);
            }
        }

        return getRoleWithLowestWeight(result);
    }

    private ClientRole getRoleWithLowestWeight(List<ClientRole> userIdentityRoles) {
        ClientRole result = null;

        if (userIdentityRoles.size() != 0) {
            for (ClientRole role : userIdentityRoles) {
                if (result == null || result.getRsWeight() > role.getRsWeight()) {
                    result = role;
                }
            }
        }
        return result;
    }
}
