package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.domain.dao.FederatedTokenDao;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    @Autowired
    private FederatedTokenDao federatedTokenDao;

    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    FederatedUserDao federatedUserDao;

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
        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenant(tenantId)) {
            if (role.getTenantIds().size() == 1) {
                this.tenantRoleDao.deleteTenantRole(role);
            } else {
                role.getTenantIds().remove(tenantId);
                this.tenantRoleDao.updateTenantRole(role);
            }
        }
        
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
    public Iterable<Tenant> getTenants() {
        logger.info("Getting Tenants");
        return this.tenantDao.getTenants();
    }

    @Override
    public PaginatorContext<Tenant> getTenantsPaged(int offset, int limit) {
        logger.info("Getting Tenants Paged - offset {}, limit {}", offset, limit);
        PaginatorContext<Tenant> tenants = this.tenantDao.getTenantsPaged(offset, limit);
        logger.info("Got {} Tenants Paged of {} total", tenants.getValueList().size(), tenants.getTotalRecords());
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
    public boolean doesUserContainTenantRole(BaseUser user, String roleId) {
        TenantRole tenantRole = tenantRoleDao.getTenantRoleForUser(user, roleId);
        return tenantRole != null;
    }

    @Override
    public TenantRole checkAndGetTenantRoleForUserById(User user, String roleId) {
        TenantRole tenantRole = getTenantRoleForUserById(user, roleId);
        if(tenantRole == null) {
            String errMsg = String.format("Tenant Role %s not found for user %s", roleId, user.getId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenantRole;
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

        Iterable<TenantRole> tenantRoles = this.tenantRoleDao.getTenantRolesForUser(user);
        List<Tenant> tenants = getTenants(tenantRoles);

        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa) {
        if (sa == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants for Parent");

        Iterable<TenantRole> tenantRoles = this.tenantRoleDao.getTenantRolesForScopeAccess(sa);
        List<Tenant> tenants = getTenants(tenantRoles);

        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public List<Tenant> getTenantsForFederatedTokenByTenantRoles(FederatedToken token) {
        if (token == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants for Parent");

        Iterable<TenantRole> tenantRoles = this.tenantRoleDao.getTenantRolesForFederatedToken(token);
        List<Tenant> tenants = getTenants(tenantRoles);

        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    private List<Tenant> getTenants(Iterable<TenantRole> tenantRoles) {
        List<Tenant> tenants = new ArrayList<Tenant>();
        List<String> tenantIds = new ArrayList<String>();
        for (TenantRole role : tenantRoles) {
            if (role.getTenantIds() != null && role.getTenantIds().size() > 0) {
                for (String tenantId : role.getTenantIds()) {
                    if (!tenantIds.contains(tenantId)) {
                        Tenant tenant = this.getTenant(tenantId);
                        if (tenant != null && tenant.getEnabled()) {
                            tenants.add(tenant);
                            tenantIds.add(tenantId);
                        }
                    }
                }
            }
        }

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
        this.tenantRoleDao.deleteTenantRole(role);
        logger.info("Deleted Global Role {}", role);
    }

    private int getDefaultRsWeight() {
        return config.getInt("default.rsWeight");
    }

    @Override
    public void deleteRbacRolesForUser(User user) {
        logger.info("Deleting Product Roles for {}", user);
        Iterable<TenantRole> tenantRoles = tenantRoleDao.getTenantRolesForUser(user);

        for (TenantRole role : tenantRoles) {
            if (role != null) {
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                if (cRole != null && cRole.getRsWeight() == getDefaultRsWeight()) {
                    deleteTenantRoleForUser(user, role);
                }
            }
        }

        logger.info("Deleted Product Roles for {}", user);
    }

    @Override
    public List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            throw new IllegalStateException();
        }
        Iterable<TenantRole> tenantRole = tenantRoleDao.getTenantRolesForScopeAccess(scopeAccess);
        return getRoleDetails(tenantRole);
    }

    @Override
    public void addTenantRoleToUser(BaseUser user, TenantRole role) {
        if (user == null || StringUtils.isBlank(user.getUniqueId()) || role == null) {
            throw new IllegalArgumentException(
                "User cannot be null and must have uniqueID; role cannot be null");
        }

        validateTenantRole(role);

        tenantRoleDao.addTenantRoleToUser(user, role);

        if(user instanceof User){
            ClientRole cRole = this.applicationService.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
            atomHopperClient.asyncPost((User) user, AtomHopperConstants.ROLE);
            if (isUserAdmin((User) user) && cRole.getPropagate()) {
                //add the role to all sub-users
                for (User subUser : userService.getSubUsers((User) user)) {
                    try {
                        role.setLdapEntry(null);
                        tenantRoleDao.addTenantRoleToUser(subUser, role);
                        atomHopperClient.asyncPost(subUser, AtomHopperConstants.ROLE);
                    } catch (ClientConflictException ex) {
                        String msg = String.format("User %s already has tenantRole %s", subUser.getId(), role.getName());
                        logger.warn(msg);
                    }
                }

                //add role to all federated sub-users' tokens
                for(User subUser : federatedUserDao.getUsersByDomainId(user.getDomainId())) {
                    for(FederatedToken token : federatedTokenDao.getFederatedTokensByUserId(subUser.getId())) {
                        try {
                            role.setLdapEntry(null);
                            tenantRoleDao.addTenantRoleToFederatedToken(token, role);
                        } catch (ClientConflictException ex) {
                            String msg = String.format("Federated user %s already has tenantRole %s", subUser.getId(), role.getName());
                            logger.warn(msg);
                        }
                    }
                }
            }

        }
        logger.info("Adding tenantRole {} to user {}", role, user);
    }

    private boolean isUserAdmin(User user) {
        String roleName = config.getString("cloudAuth.userAdminRole");
        return hasRole(user, roleName);
    }

    private boolean hasRole(User user, String roleName) {
        for (TenantRole role : tenantRoleDao.getTenantRolesForUser(user)) {
            ClientRole cRole = applicationService.getClientRoleById(role.getRoleRsId());
            if (cRole.getName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addCallerTenantRolesToUser(User caller, User user) {
        List<TenantRole> tenantRoles = this.getTenantRolesForUser(caller);
        for (TenantRole tenantRole : tenantRoles) {
            if (!tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.adminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.serviceAdminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userAdminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userManagedRole"))
                    && tenantRole.getPropagate()
                    ) {
                TenantRole role = new TenantRole();
                role.setClientId(tenantRole.getClientId());
                role.setDescription(tenantRole.getDescription());
                role.setName(tenantRole.getName());
                role.setRoleRsId(tenantRole.getRoleRsId());
                role.setTenantIds(tenantRole.getTenantIds());
                role.setUserId(user.getId());
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

        validateTenantRole(role);

        tenantRoleDao.addTenantRoleToApplication(client, role);

        logger.info("Added tenantRole {} to client {}", role, client);
    }

    @Override
    public void addTenantRolesToFederatedToken(FederatedToken token, List<TenantRole> tenantRoles) {
        for (TenantRole tenantRole : tenantRoles) {
             addTenantRoleToFederatedToken(token, tenantRole);
        }

        logger.info("Added tenantRoles {} to federated token {}", tenantRoles, token);
    }

    @Override
    public void addTenantRoleToFederatedToken(FederatedToken token, TenantRole role) {
        if (token == null || StringUtils.isBlank(token.getUniqueId()) || role == null) {
            throw new IllegalArgumentException(
                    "Federated token cannot be null and must have uniqueID; role cannot be null");
        }

        validateTenantRole(role);

        tenantRoleDao.addTenantRoleToFederatedToken(token, role);

        logger.info("Added tenantRole {} to federated token {}", role, token);
    }

    @Override
    public void deleteTenantRoleForUser(User user, TenantRole role) {
        if (user == null || role == null) {
            throw new IllegalArgumentException();
        }

        ClientRole cRole = applicationService.getClientRoleById(role.getRoleRsId());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        tenantRoleDao.deleteTenantRoleForUser(user, role);
        atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE);

        if (isUserAdmin(user) && cRole.getPropagate()) {
            //remove propagating roles from sub-users
            for (User subUser : userService.getSubUsers(user)) {
                try {
                    role.setLdapEntry(null);
                    tenantRoleDao.deleteTenantRoleForUser(subUser, role);
                    atomHopperClient.asyncPost(subUser, AtomHopperConstants.ROLE);
                } catch (NotFoundException ex) {
                    String msg = String.format("User %s does not have tenantRole %s", subUser.getId(), role.getName());
                    logger.warn(msg);
                }
            }

            //remove propagating roles from federated users
            for(User subUser : federatedUserDao.getUsersByDomainId(user.getDomainId())) {
                for(FederatedToken token : federatedTokenDao.getFederatedTokensByUserId(subUser.getId())) {
                    try {
                        role.setLdapEntry(null);
                        tenantRoleDao.deleteTenantRoleForFederatedToken(token, role);
                    } catch (NotFoundException ex) {
                        String msg = String.format("Federated user %s does not have tenantRole %s", user.getId(), role.getName());
                        logger.warn(msg);
                    }
                }
            }

        }
    }

    @Override
    public void deleteTenantRoleForApplication(Application application, TenantRole role) {
        if (application == null || role == null) {
            throw new IllegalStateException();
        }
        tenantRoleDao.deleteTenantRoleForApplication(application, role);
    }

    @Override
    public List<TenantRole> getGlobalRolesForUser(BaseUser user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null.");
        }
        logger.debug("Getting Global Roles for user {}", user.getUniqueId());
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForUser(User user, String applicationId) {
        logger.debug("Getting Global Roles");
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user, applicationId);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForApplication(Application application) {
        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null.");
        }
        logger.debug("Getting Global Roles for application {}", application.getName());
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForApplication(application);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForApplication(Application application, String applicationId) {
        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null.");
        }
        logger.debug("Getting Global Roles for application {}", application.getName());
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForApplication(application, applicationId);
        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForUserOnTenant(User user, Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException(
                    "Tenant cannot be null.");
        }

        logger.debug(GETTING_TENANT_ROLES);
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : this.tenantRoleDao.getTenantRolesForUser(user)) {
            if (role.getTenantIds().contains(tenant.getTenantId())) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.getTenantIds().add(tenant.getTenantId());
                tenantRoles.add(newRole);
            }
        }
        logger.debug(GOT_TENANT_ROLES, tenantRoles.size());
        return tenantRoles;
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(BaseUser user) {
        logger.debug(GETTING_TENANT_ROLES);
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user);
        return getRoleDetails(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForFederatedToken(FederatedToken token) {
        logger.debug(GETTING_TENANT_ROLES);
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForFederatedToken(token);
        return getRoleDetails(roles);
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForFederatedTokenNoDetail(FederatedToken token) {
        logger.debug(GETTING_TENANT_ROLES);
        return this.tenantRoleDao.getTenantRolesForFederatedToken(token);
    }

    public Iterable<TenantRole> getTenantRolesForUserNoDetail(BaseUser user) {
        logger.debug(GETTING_TENANT_ROLES);
        return this.tenantRoleDao.getTenantRolesForUser(user);
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId) {
        logger.debug(GETTING_TENANT_ROLES);
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user, applicationId, tenantId);
        return getRoleDetails(roles);
    }

    private List<TenantRole> getRoleDetails(Iterable<TenantRole> roles) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
        if (role != null) {
            ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
            role.setName(cRole.getName());
            role.setDescription(cRole.getDescription());
            role.setPropagate(cRole.getPropagate());
            tenantRoles.add(role);
            }
        }
        return tenantRoles;
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(
            Application application, String applicationId, String tenantId) {
        logger.debug(GETTING_TENANT_ROLES);
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForApplication(application, applicationId, tenantId);

        return getTenantOnlyRoles(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForTenant(String tenantId) {

        List<TenantRole> roles = new ArrayList<TenantRole>();

        HashMap<String,ClientRole> clientRolesMap = new HashMap<String, ClientRole>();
        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenant(tenantId)){
            if(!clientRolesMap.containsKey(role.getRoleRsId())){
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                if(cRole != null){
                    clientRolesMap.put(role.getRoleRsId(),cRole);
                }
            }
            ClientRole clientRole = clientRolesMap.get(role.getRoleRsId());
            if (clientRole != null) {
                role.setDescription(clientRole.getDescription());
                role.setName(clientRole.getName());
            }
            roles.add(role);
        }

        return roles;
    }

    @Override
    public List<User> getUsersForTenant(String tenantId, int offset, int limit) {
        logger.debug("Getting Users for Tenant {}", tenantId);
        List<User> users = new ArrayList<User>();

        List<String> userIds = new ArrayList<String>();

        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenant(tenantId)) {
            if (!userIds.contains(role.getUserId())) {
                String userId = tenantRoleDao.getUserIdForParent(role);
                if(!StringUtils.isBlank(userId)){
                    userIds.add(userId);
                }
            }
        }

        for (String userId : userIds) {
            User user = this.userService.getUserById(userId);
            if (user != null && user.getEnabled()) {
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

    List<TenantRole> getGlobalRoles(Iterable<TenantRole> roles) {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null
                && (role.getTenantIds() == null || role.getTenantIds().size() == 0)) {
                ClientRole cRole = this.applicationService.getClientRoleById(role
                    .getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
                globalRoles.add(role);
            }
        }
        return globalRoles;
    }

    List<TenantRole> getTenantOnlyRoles(Iterable<TenantRole> roles) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            // we only want to include roles on a tenant, and not global roles
            if (role.getTenantIds() != null && role.getTenantIds().size() > 0) {
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
    public List<User> getUsersWithTenantRole(Tenant tenant, ClientRole cRole, int offset, int limit) {
        List<User> users = new ArrayList<User>();

        List<String> userIds = new ArrayList<String>();

        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant.getTenantId(), cRole.getId())) {
            if (!userIds.contains(role.getUserId())) {
                userIds.add(role.getUserId());
            }
        }

        for (String userId : userIds) {
            User user = this.userService.getUserById(userId);
            if (user != null && user.getEnabled()) {
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
                if (role.getTenantIds().contains(tenantId)) {
                    truth = true;
                }
            }
        }

        return truth;
    }

    @Override
    public List<Tenant> getTenantsByDomainId(String domainId) {
        //TODO: This should probably return an empty list as opposed to throwing an exception
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
    public Iterable<TenantRole> getTenantRolesForClientRole(ClientRole role) {
        return tenantRoleDao.getAllTenantRolesForClientRole(role);
    }

    @Override
    public void deleteTenantRole(TenantRole role) {
        tenantRoleDao.deleteTenantRole(role);
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUserById(User user, List<ClientRole> rolesForFilter) {
        return tenantRoleDao.getTenantRoleForUser(user, rolesForFilter);
    }

    @Override
    public List<String> getIdsForUsersWithTenantRole(String roleId, int sizeLimit) {
        return tenantRoleDao.getIdsForUsersWithTenantRole(roleId, sizeLimit);
    }

    @Override
    public void addUserIdToTenantRole(TenantRole tenantRole) {
        String userId = tenantRoleDao.getUserIdForParent(tenantRole);
        if (userId != null) {
            tenantRole.setUserId(userId);
            tenantRoleDao.updateTenantRole(tenantRole);
        }
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

	@Override
	public void setTenantDao(TenantDao tenantDao) {
		this.tenantDao = tenantDao;
	}

    @Override
    public void setTenantRoleDao(TenantRoleDao tenantRoleDao) {
        this.tenantRoleDao = tenantRoleDao;
    }

    private void validateTenantRole(TenantRole role) {
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
    }

    public String getMossoIdFromTenantRoles(List<TenantRole> roles) {

        Assert.notNull(roles);

        for (TenantRole role : roles) {
            if (role.getName().equals("compute:default") && !role.getTenantIds().isEmpty()) {
                return role.getTenantIds().iterator().next();
            }
        }

        for (TenantRole tenantRole : roles) {
            for (String tenantId : tenantRole.getTenantIds()) {
                if (tenantId.matches("\\d+")) {
                    return tenantId;
                }
            }
        }
        return null;
    }
}
