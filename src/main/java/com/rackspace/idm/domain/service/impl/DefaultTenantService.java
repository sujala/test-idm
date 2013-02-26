package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.service.*;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultTenantService implements TenantService {

    public static final String GETTING_TENANT_ROLES = "Getting Tenant Roles";
    public static final String GOT_TENANT_ROLES = "Got {} Tenant Roles";

    @Autowired
    private Configuration config;

    @Autowired
    private DomainService domainService;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private TenantRoleDao tenantRoleDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void addTenant(Tenant tenant) {
        logger.info("Adding Tenant {}", tenant);
        if(tenant == null){
            throw new IllegalArgumentException("Tenant cannot be null");
        }
        Tenant exists = this.tenantDao.getTenant(tenant.getName());
        if (exists != null) {
            String errMsg = String.format("Tenant with name %s already exists", tenant.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
        this.tenantDao.addTenant(tenant);
        logger.info("Added Tenant {}", tenant);
    }

    @Override
    public void deleteTenant(String tenantId) {
        logger.info("Deleting Tenant {}", tenantId);
        
        // Delete all tenant roles for this tenant
        List<TenantRole> roles = this.tenantDao.getAllTenantRolesForTenant(tenantId);
        for (TenantRole role : roles) {
            if (role.getTenantIds().length == 1) {
                this.tenantDao.deleteTenantRole(role);
            } else {
                role.removeTenantId(tenantId);
                this.tenantDao.updateTenantRole(role);
            }
        }
        
        // Then delete the tenant
        this.tenantDao.deleteTenant(tenantId);
        
        logger.info("Added Tenant {}", tenantId);
    }

    @Override
    public Tenant getTenant(String tenantId) {
        logger.info("Getting Tenant {}", tenantId);
        Tenant tenant = this.tenantDao.getTenant(tenantId);
        logger.info("Got Tenant {}", tenant);
        return tenant;
    }

    @Override
    public Tenant checkAndGetTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId);

        if (tenant == null) {
            String errMsg = String.format("Tenant with id/name: '%s' was not found.", tenantId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenant;
    }

    @Override
    public Tenant getTenantByName(String name) {
        logger.info("Getting Tenant {}", name);
        Tenant tenant = this.tenantDao.getTenantByName(name);
        logger.info("Got Tenant {}", tenant);
        return tenant;
    }

    @Override
    public List<Tenant> getTenants() {
        logger.info("Getting Tenants");
        List<Tenant> tenants = this.tenantDao.getTenants();
        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public void updateTenant(Tenant tenant) {
        logger.info("Updating Tenant {}", tenant);
        this.tenantDao.updateTenant(tenant);
        logger.info("Updated Tenant {}", tenant);
    }

    @Override
    public TenantRole getTenantRoleForUserById(User user, String roleId) {
        return tenantRoleDao.getTenantRoleForUser(user, roleId);
    }

    @Override
    public boolean doesUserContainTenantRole(User user, String roleId) {
        TenantRole tenantRole = tenantRoleDao.getTenantRoleForUser(user, roleId);
        if (tenantRole == null) {
            return  false;
        } else {
            return true;
        }
    }

    @Override
    public TenantRole getTenantRoleForApplicationById(Application application, String id) {
        return tenantRoleDao.getTenantRoleForApplication(application, id);
    }

    @Override
    public List<Tenant> getTenantsForUserByTenantRoles(User user) {
        if (user == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants for Parent");
        List<Tenant> tenants = new ArrayList<Tenant>();
        List<String> tenantIds = new ArrayList<String>();
        List<TenantRole> tenantRoles = this.tenantRoleDao.getTenantRolesForUser(user);
        for (TenantRole role : tenantRoles) {
            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                for (String tenantId : role.getTenantIds()) {
                    if (!tenantIds.contains(tenantId)) {
                        tenantIds.add(tenantId);
                    }
                }
            }
        }
        for (String tenantId : tenantIds) {
            Tenant tenant = this.getTenant(tenantId);
            if (tenant != null && tenant.isEnabled()) {
                tenants.add(tenant);
            }
        }
        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa) {
        if (sa == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants for Parent");
        List<Tenant> tenants = new ArrayList<Tenant>();
        List<String> tenantIds = new ArrayList<String>();
        List<TenantRole> tenantRoles = this.tenantRoleDao.getTenantRolesForScopeAccess(sa);
        for (TenantRole role : tenantRoles) {
            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                for (String tenantId : role.getTenantIds()) {
                    if (!tenantIds.contains(tenantId)) {
                        tenantIds.add(tenantId);
                    }
                }
            }
        }
        for (String tenantId : tenantIds) {
            Tenant tenant = this.getTenant(tenantId);
            if (tenant != null && tenant.isEnabled()) {
                tenants.add(tenant);
            }
        }
        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public boolean hasTenantAccess(User user, String tenantId) {
        if(user ==null){
            return false;
        }
        if(StringUtils.isBlank(tenantId)){
            return false;
        }
        List<Tenant> tenantList = getTenantsForUserByTenantRoles(user);
        for(Tenant tenant : tenantList){
            if(tenant.getTenantId()!=null && tenant.getTenantId().equals(tenantId)){
                return true;
            }
            if(tenant.getName()!=null && tenant.getName().equals(tenantId)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteGlobalRole(TenantRole role) {
        logger.info("Deleting Global Role {}", role);
        this.tenantDao.deleteTenantRole(role);
        logger.info("Deleted Global Role {}", role);
    }

    @Override
    public List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            throw new IllegalStateException();
        }
        List<TenantRole> tenantRole = tenantRoleDao.getTenantRolesForScopeAccess(scopeAccess);
        getRoleDetails(tenantRole);
        return tenantRole;
    }

    @Override
    public void addTenantRoleToUser(User user, TenantRole role) {
        if (user == null || StringUtils.isBlank(user.getUniqueId()) || role == null) {
            throw new IllegalArgumentException(
                "User cannot be null and must have uniqueID; role cannot be null");
        }

        Application client = this.applicationService.getById(role.getClientId());
        if (client == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientRole cRole = this.applicationService.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        tenantRoleDao.addTenantRoleToUser(user, role);

        logger.info("Adding tenantRole {} to user {}", role, user);
    }

    @Override
    public void addCallerTenantRolesToUser(User caller, User user) {
        List<TenantRole> tenantRoles = this.getTenantRolesForUser(caller);
        for (TenantRole tenantRole : tenantRoles) {
            if (!tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.adminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.serviceAdminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userAdminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userRole"))) {
                TenantRole role = new TenantRole();
                role.setClientId(tenantRole.getClientId());
                role.setDescription(tenantRole.getDescription());
                role.setName(tenantRole.getName());
                role.setRoleRsId(tenantRole.getRoleRsId());
                role.setTenantIds(tenantRole.getTenantIds());
                tenantRole.setUserId(user.getId());
                this.addTenantRoleToUser(user, role);
            }
        }
    }

    @Override
    public void addTenantRoleToClient(Application client, TenantRole role) {
        if (client == null || StringUtils.isBlank(client.getUniqueId()) || role == null) {
            throw new IllegalArgumentException(
                "Client cannot be null and must have uniqueID; role cannot be null");
        }

        Application owner = this.applicationService.getById(role.getClientId());
        if (owner == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientRole cRole = this.applicationService.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        tenantRoleDao.addTenantRoleToApplication(client, role);

        logger.info("Added tenantRole {} to client {}", role, client);
    }

    @Override
    public void deleteTenantRoleForUser(User user, TenantRole role) {
        if (user == null || role == null) {
            throw new IllegalArgumentException();
        }
        tenantRoleDao.deleteTenantRoleForUser(user, role);
    }

    @Override
    public void deleteTenantRoleForApplication(Application application, TenantRole role) {
        if (application == null || role == null) {
            throw new IllegalStateException();
        }
        tenantRoleDao.deleteTenantRoleForApplication(application, role);
    }

    @Override
    public List<TenantRole> getGlobalRolesForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null.");
        }
        logger.debug("Getting Global Roles for user {}", user.getUsername());
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForUser(User user, String applicationId) {
        logger.debug("Getting Global Roles");
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user, applicationId);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForApplication(Application application) {
        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null.");
        }
        logger.debug("Getting Global Roles for application {}", application.getName());
        List<TenantRole> roles = this.tenantDao.getTenantRolesForApplication(application);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForApplication(Application application, String applicationId) {
        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null.");
        }
        logger.debug("Getting Global Roles for application {}", application.getName());
        List<TenantRole> roles = this.tenantDao.getTenantRolesForApplication(application, applicationId);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForUserOnTenant(User user, Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException(
                    "Tenant cannot be null.");
        }

        logger.debug(GETTING_TENANT_ROLES);
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user);
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role.containsTenantId(tenant.getTenantId())) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.setTenantIds(new String[]{tenant.getTenantId()});
                tenantRoles.add(newRole);
            }
        }
        logger.debug(GOT_TENANT_ROLES, roles.size());
        return tenantRoles;
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user) {
        logger.debug(GETTING_TENANT_ROLES);
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user);
        getRoleDetails(roles);
        return roles;
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId) {
        logger.debug(GETTING_TENANT_ROLES);
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user, applicationId, tenantId);
        getRoleDetails(roles);
        return roles;
    }

    private void getRoleDetails(List<TenantRole> roles) {
        for (TenantRole role : roles) {
            if (role != null) {
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
            }
        }
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(
            Application application, String applicationId, String tenantId) {
        logger.debug(GETTING_TENANT_ROLES);
        List<TenantRole> roles = this.tenantDao.getTenantRolesForApplication(application, applicationId, tenantId);

        return getTenantOnlyRoles(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForTenant(String tenantId) {

        List<TenantRole> roles = this.tenantDao
            .getAllTenantRolesForTenant(tenantId);

        List<String> roleIds = new ArrayList<String>();
        for (TenantRole role : roles) {
            if (!roleIds.contains(role.getRoleRsId())) {
                roleIds.add(role.getRoleRsId());
            }
        }

        List<TenantRole> returnedRoles = new ArrayList<TenantRole>();
        for (String roleId : roleIds) {
            ClientRole cRole = this.applicationService.getClientRoleById(roleId);
            if (cRole != null) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(cRole.getClientId());
                newRole.setDescription(cRole.getDescription());
                newRole.setName(cRole.getName());
                newRole.setRoleRsId(cRole.getId());
                newRole.setTenantIds(new String[]{tenantId});
                returnedRoles.add(newRole);
            }
        }
        return returnedRoles;
    }

    @Override
    public List<User> getUsersForTenant(String tenantId) {
        logger.debug("Getting Users for Tenant {}", tenantId);
        List<User> users = new ArrayList<User>();

        List<TenantRole> roles = this.tenantDao
            .getAllTenantRolesForTenant(tenantId);

        List<String> userIds = new ArrayList<String>();

        for (TenantRole role : roles) {
            if (!userIds.contains(role.getUserId())) {
                userIds.add(role.getUserId());
            }
        }

        for (String userId : userIds) {
            User user = this.userService.getUserById(userId);
            if (user != null && user.isEnabled()) {
                users.add(user);
            }
        }

        logger.debug("Got {} Users for Tenant {}", users.size(), tenantId);
        return users;
    }

    @Override
    public List<Tenant> getTenantsFromNameList(String[] tenants){
        List<Tenant> tenantList = new ArrayList<Tenant>();
        if(tenants == null) {
            return tenantList;
        }
        for (String tenantId : tenants) {
            Tenant tenant = this.tenantDao.getTenant(tenantId);
            if (tenant != null) {
                tenantList.add(tenant);
            }
        }
        return tenantList;
    }

    /**
     * get roles in this list that are non-tenant specific
     * @param roles
     */
    List<TenantRole> getGlobalRoles(List<TenantRole> roles) {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null
                && (role.getTenantIds() == null || role.getTenantIds().length == 0)) {
                ClientRole cRole = this.applicationService.getClientRoleById(role
                    .getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
                globalRoles.add(role);
            }
        }
        return globalRoles;
    }

    /**
     * get roles in this list that are tenant specific
     * @param roles
     */
    List<TenantRole> getTenantOnlyRoles(List<TenantRole> roles) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            // we only want to include roles on a tenant, and not global roles
            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.setDescription(role.getDescription());
                newRole.setTenantIds(role.getTenantIds());
                tenantRoles.add(newRole);
            }
        }

        return tenantRoles;
    }

    @Override
    public List<User> getUsersWithTenantRole(Tenant tenant, ClientRole cRole) {
        List<User> users = new ArrayList<User>();

        List<TenantRole> roles = this.tenantDao
            .getAllTenantRolesForTenantAndRole(tenant.getTenantId(),
                cRole.getId());

        List<String> userIds = new ArrayList<String>();

        for (TenantRole role : roles) {
            if (!userIds.contains(role.getUserId())) {
                userIds.add(role.getUserId());
            }
        }

        for (String userId : userIds) {
            User user = this.userService.getUserById(userId);
            if (user != null && user.isEnabled()) {
                users.add(user);
            }
        }

        logger.debug("Got {} Users for Tenant {}", users.size(),
            tenant.getTenantId());
        return users;

    }

    public boolean isTenantIdContainedInTenantRoles(String tenantId, List<TenantRole> roles){
        boolean truth = false;

        if (roles != null) {
            for (TenantRole role : roles) {
                if (role.containsTenantId(tenantId)) {
                    truth = true;
                }
            }
        }

        return truth;
    }

    @Override
    public List<Tenant> getTenantsByDomainId(String domainId) {
        Domain domain = domainService.getDomain(domainId);
        if(domain.getTenantIds() == null) {
            throw new NotFoundException("No tenants belong to this domain.");
        }
        List<Tenant> tenantList = new ArrayList<Tenant>();
        for (String tenantId : domain.getTenantIds()){
            Tenant tenant = getTenant(tenantId);
            if(tenant != null){
                tenantList.add(tenant);
            }

        }
        return tenantList;
    }

    @Override
    public List<TenantRole> getTenantRolesForClientRole(ClientRole role) {
        return tenantDao.getAllTenantRolesForClientRole(role);
    }

    @Override
    public void deleteTenantRole(TenantRole role) {
        tenantDao.deleteTenantRole(role);
    }

    @Override
    public TenantRole getTenantRoleForUser(User user, List<ClientRole> rolesForFilter) {
        return tenantRoleDao.getTenantRoleForUser(user, rolesForFilter);
    }

    @Override
    public PaginatorContext<String> getIdsForUsersWithTenantRole(String roleId, int offset, int limit) {
        return tenantDao.getIdsForUsersWithTenantRole(roleId, offset, limit);
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setDomainService(DomainService domainService) {
        this.domainService = domainService;
    }

	@Override
	public void setTenantDao(TenantDao tenantDao) {
		this.tenantDao = tenantDao;
	}

    @Override
    public void setTenantRoleDao(TenantRoleDao tenantRoleDao) {
        this.tenantRoleDao = tenantRoleDao;
    }
}
