package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapClientRepository extends LdapRepository implements ClientDao {

    public LdapClientRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    @Override
    public void addClient(Client client, String customerUniqueId) {
        getLogger().info("Adding client {}", client);

        if (client == null) {
            String errMsg = "Null instance of Client was passed in.";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Audit audit = Audit.log(client).add();

        Attribute[] attributes = getAddAttributesForClient(client);

        String clientDN = new LdapDnBuilder(customerUniqueId)
            .addAttriubte(ATTR_INUM, client.getInum())
            .addAttriubte(ATTR_OU, OU_APPLICATIONS_NAME).build();

        client.setUniqueId(clientDN);

        LDAPConnection conn = getAppPoolConnection(audit);
        addEntry(conn, clientDN, attributes, audit);

        // Add ou=permissions under new client entry
        String clientPermissionsDN = new LdapDnBuilder(clientDN).addAttriubte(
            ATTR_OU, OU_PERMISSIONS_NAME).build();

        Attribute[] permissionAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, OU_PERMISSIONS_NAME)};

        addEntry(conn, clientPermissionsDN, permissionAttributes, audit);

        // Add ou=groups under new client entry
        String clientGroupsDN = new LdapDnBuilder(clientDN).addAttriubte(
            ATTR_OU, OU_GROUPS_NAME).build();

        Attribute[] groupAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, OU_GROUPS_NAME)};

        addEntry(conn, clientGroupsDN, groupAttributes, audit);

        audit.succeed();

        getAppConnPool().releaseConnection(conn);

        getLogger().debug("Added client {}", client);
    }

    @Override
    public void addClientGroup(ClientGroup clientGroup, String clientUniqueId) {
        getLogger().info("Adding ClientGroup {}", clientGroup);

        Audit audit = Audit.log(clientGroup).add();

        if (clientGroup == null) {
            String errMsg = "Null instance of clientGroup was passed";
            audit.fail(errMsg);
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        ClientGroup group = this.getClientGroup(clientGroup.getCustomerId(),
            clientGroup.getClientId(), clientGroup.getName());

        if (group != null) {
            String errMsg = "Client Group already exists";
            audit.fail(errMsg);
            throw new DuplicateClientGroupException(errMsg);
        }

        Attribute[] atts = getAddAttributesForClientGroup(clientGroup);

        String groupDN = new LdapDnBuilder(clientUniqueId)
            .addAttriubte(ATTR_NAME, clientGroup.getName())
            .addAttriubte(ATTR_OU, OU_GROUPS_NAME).build();

        clientGroup.setUniqueId(groupDN);

        addEntry(groupDN, atts, audit);

        audit.succeed();

        getLogger().debug("Added clientGroup {}", clientGroup);
    }

    @Override
    public void addDefinedPermission(Permission permission,
        String clientUniqueId) {
        getLogger().info("Adding Permission {}", permission);

        if (permission == null) {
            String errMsg = "Null instance of Permission was passed";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Audit audit = Audit.log(permission).add();

        Attribute[] atts = getAddAttributesForClientPermission(permission);

        String permissionDN = new LdapDnBuilder(clientUniqueId)
            .addAttriubte(ATTR_NAME, permission.getPermissionId())
            .addAttriubte(ATTR_OU, OU_PERMISSIONS_NAME).build();

        permission.setUniqueId(permissionDN);

        addEntry(permissionDN, atts, audit);

        audit.succeed();

        getLogger().debug("Added permission {}", permission);
    }

    @Override
    public void addUserToClientGroup(String userUniqueId, ClientGroup group) {

        getLogger().info("Adding user {} to {}", userUniqueId, group);

        if (StringUtils.isBlank(userUniqueId)) {
            String errMsg = "User uniqueId was blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (group == null || StringUtils.isBlank(group.getUniqueId())) {
            String errMsg = "Null group passed in or group uniqueId was blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.ADD, ATTR_MEMBER,
            userUniqueId));

        Audit audit = Audit.log(group).modify(mods);

        try {
            getAppConnPool().modify(group.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding user to group {} - {}", group,
                ldapEx);

            if (ldapEx.getResultCode().equals(
                ResultCode.ATTRIBUTE_OR_VALUE_EXISTS)) {
                audit.fail("User already in group");
                throw new DuplicateException("User already in group");
            }

            audit.fail(ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        audit.succeed();

        getLogger().info("Added user {} to group {}", userUniqueId, group);
    }

    @Override
    public ClientAuthenticationResult authenticate(String clientId,
        String clientSecret) {
        BindResult result;
        Client client = getClientByClientId(clientId);

        if (client == null) {
            return new ClientAuthenticationResult(null, false);
        }

        Audit audit = Audit.authClient(client);

        try {
            result = getBindConnPool().bind(client.getUniqueId(), clientSecret);
            audit.succeed();
        } catch (LDAPException e) {
            audit.fail(e.getMessage());
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                return new ClientAuthenticationResult(client, false);
            }
            getLogger().error(
                "Bind operation on clientId " + clientId + " failed.", e);
            throw new IllegalStateException(e);
        }

        return new ClientAuthenticationResult(client,
            ResultCode.SUCCESS.equals(result.getResultCode()));
    }

    @Override
    public void deleteClient(String clientId) {
        getLogger().info("Deleting client {}", clientId);
        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId paramter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        Client client = this.getClientByClientId(clientId);

        if (client == null) {
            String errorMsg = String.format("CLient %s not found", client);
            throw new NotFoundException(errorMsg);
        }

        Audit audit = Audit.log(client).delete();

        this.deleteEntryAndSubtree(client.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted client {}", clientId);
    }

    @Override
    public void deleteClientGroup(String customerId, String clientId,
        String name) {
        getLogger().info("Deleting clientGroup {}", name);

        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(name)) {
            throw new IllegalArgumentException();
        }

        ClientGroup group = this.getClientGroup(customerId, clientId, name);

        if (group == null) {
            throw new NotFoundException(
                String
                    .format(
                        "Client Group with Name %s, ClientId %s, and CustomerId %s not found",
                        customerId, clientId, name));
        }

        Audit audit = Audit.log(group).delete();

        this.deleteEntryAndSubtree(group.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted clientGroup {}", group);
    }

    @Override
    public void deleteDefinedPermission(Permission permission) {
        getLogger().info("Deleting permission {}", permission);
        if (permission == null) {
            getLogger().error("Null or Empty permission paramter");
            throw new IllegalArgumentException(
                "Null or Empty permission parameter.");
        }

        Audit audit = Audit.log(permission).delete();

        this.deleteEntryAndSubtree(permission.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted permission {}", permission);
    }

    @Override
    public List<Client> getAllClients() {
        getLogger().debug("Search all clients");

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        List<Client> clients = new ArrayList<Client>();
        SearchResult searchResult = null;
        try {
            SearchRequest request = new SearchRequest(BASE_DN, SearchScope.SUB,
                searchFilter);
            searchResult = getAppConnPool().search(request);
        } catch (LDAPException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                clients.add(getClient(entry));
            }
        }

        getLogger().debug("Found clients {}", clients);

        return clients;
    }

    @Override
    public Client getClientByClientId(String clientId) {
        getLogger().debug("Doing search for clientId {}", clientId);

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    @Override
    public Client getClientByClientname(String clientName) {
        getLogger().debug("Searching for client {}", clientName);

        if (StringUtils.isBlank(clientName)) {
            getLogger().error("Null or Empty client name parameter");
            throw new IllegalArgumentException(
                "Null or Empty client name parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, clientName)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    @Override
    public Client getClientByCustomerIdAndClientId(String customerId,
        String clientId) {
        getLogger().debug("Doing search for clientId {}", clientId);

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    @Override
    public Client getClientByInum(String inum) {
        getLogger().debug("Doing search for Inum {}", inum);

        if (StringUtils.isBlank(inum)) {
            getLogger().error("Null or Empty Inum parameter");
            throw new IllegalArgumentException("Null or Empty Inum parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_INUM, inum)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    @Override
    public ClientGroup getClientGroup(String customerId, String clientId,
        String groupName) {

        ClientGroup group = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, groupName)
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTGROUP)
            .build();

        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter, ATTR_GROUP_SEARCH_ATTRIBUTES);

        if (entry != null) {
            group = getClientGroup(entry);
        }

        return group;
    }

    @Override
    public ClientGroup getClientGroupByUniqueId(String uniqueId) {
        ClientGroup group = null;

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTGROUP).build();

        SearchResultEntry entry = this.getSingleEntry(uniqueId,
            SearchScope.BASE, searchFilter, ATTR_GROUP_SEARCH_ATTRIBUTES);

        if (entry != null) {
            group = getClientGroup(entry);
        }

        getLogger().debug("Found Client Group - {}", group);

        return group;
    }

    @Override
    public List<ClientGroup> getClientGroupsByClientId(String clientId) {

        List<ClientGroup> groups = new ArrayList<ClientGroup>();

        Client client = this.getClientByClientId(clientId);

        if (client == null) {
            throw new NotFoundException();
        }

        String searchDN = "ou=groups," + client.getUniqueId();

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTGROUP).build();

        List<SearchResultEntry> entries = this.getMultipleEntries(searchDN,
            SearchScope.ONE, searchFilter, ATTR_NAME,
            ATTR_GROUP_SEARCH_ATTRIBUTES);

        if (entries.size() > 0) {
            for (SearchResultEntry entry : entries) {
                groups.add(getClientGroup(entry));
            }
        }
        return groups;
    }

    @Override
    public Clients getClientsByCustomerId(String customerId, int offset,
        int limit) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Clients clients = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Found clients {} for customer {}", clients,
            customerId);

        return clients;
    }

    @Override
    public List<Client> getClientsThatHavePermission(Permission permission) {
        getLogger().debug("Doing search for clients that have permission {}",
            permission);

        if (permission == null) {
            String errMsg = "Null permission passed in";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_PERMISSION,
                permission.getPermissionLDAPserialization())
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        List<Client> clientList = new ArrayList<Client>();
        SearchResult searchResult = null;
        try {

            SearchRequest request = new SearchRequest(BASE_DN, SearchScope.SUB,
                searchFilter);

            searchResult = getAppConnPool().search(request);

        } catch (LDAPException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            clientList.add(getClient(entry));
        }

        getLogger().debug("Found Clients - {}", clientList);

        return clientList;
    }

    @Override
    public Permission getDefinedPermissionByClientIdAndPermissionId(
        String clientId, String permissionId) {
        String clientDN = this.getClientByClientId(clientId).getUniqueId();

        String searchDN = "ou=permissions," + clientDN;

        Permission permission = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, permissionId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTPERMISSION)
            .build();

        SearchResultEntry entry = this.getSingleEntry(searchDN,
            SearchScope.ONE, searchFilter);

        if (entry != null) {
            permission = getPermission(entry);
        }

        return permission;
    }

    @Override
    public List<Permission> getDefinedPermissionsByClientId(String clientId) {
        String clientDN = this.getClientByClientId(clientId).getUniqueId();

        String searchDN = "ou=permissions," + clientDN;

        List<Permission> permissions = new ArrayList<Permission>();

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTPERMISSION).build();

        List<SearchResultEntry> entries = this.getMultipleEntries(searchDN,
            SearchScope.ONE, searchFilter, ATTR_NAME);

        for (SearchResultEntry entry : entries) {
            permissions.add(getPermission(entry));
        }

        return permissions;
    }

    @Override
    public String getUnusedClientInum(String customerInum) {
        // TODO: We might may this call to the XDI server in the future.
        Client client = null;
        String inum = "";
        do {
            inum = customerInum + InumHelper.getRandomInum(1);
            client = getClientByInum(inum);
        } while (client != null);

        return inum;
    }

    @Override
    public void grantPermissionToClient(Permission permission, Client client) {

        getLogger().info("Adding permission {} to {}", permission, client);

        if (permission == null || StringUtils.isBlank(permission.getUniqueId())) {
            String errMsg = "Null permission passed in or permission uniqueId was blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (client == null || StringUtils.isBlank(client.getUniqueId())) {
            String errMsg = "Null client passed in or client uniqueId was blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.ADD, ATTR_PERMISSION,
            permission.getPermissionLDAPserialization()));

        LDAPResult result;
        Audit audit = Audit.log(client).modify(mods);

        try {
            result = getAppConnPool().modify(client.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding permission to client {} - {}",
                permission, ldapEx);

            if (ldapEx.getResultCode().equals(
                ResultCode.ATTRIBUTE_OR_VALUE_EXISTS)) {
                throw new DuplicateException("Client already has permission");
            }

            audit.fail(ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when adding permission to client: %s - %s",
                        permission, result.getResultCode().toString()));
        }

        audit.succeed();

        getLogger()
            .info("Added permission {} to client {}", permission, client);
    }

    @Override
    public boolean isUserInClientGroup(String username, String groupDN) {

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .addEqualAttribute(ATTR_MEMBER_OF, groupDN).build();

        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter, ATTR_NO_ATTRIBUTES);

        return entry != null;
    }

    @Override
    public void removeUserFromGroup(String userUniqueId, ClientGroup group) {
        getLogger().info("Removing user {} from {}", userUniqueId, group);

        if (StringUtils.isBlank(userUniqueId)) {
            getLogger().error("Null user passed in or user uniqueId was blank");
            throw new IllegalArgumentException(
                "Null user passed in or user uniqueId was blank");
        }

        if (group == null || StringUtils.isBlank(group.getUniqueId())) {
            getLogger().error(
                "Null group passed in or group uniqueId was blank");
            throw new IllegalArgumentException(
                "Null group passed in or group uniqueId was blank");
        }

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.DELETE, ATTR_MEMBER,
            userUniqueId));

        Audit audit = Audit.log(group).modify(mods);
        try {
            getAppConnPool().modify(group.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error deleting user from group {} - {}", group,
                ldapEx);
            if (ldapEx.getResultCode().equals(ResultCode.NO_SUCH_ATTRIBUTE)) {
                audit.fail("User isn't in group");
                throw new NotFoundException("User isn't in group");
            }
            throw new IllegalStateException(ldapEx);
        }

        audit.succeed();
        getLogger().info("Removed user {} from group {}", userUniqueId, group);
    }

    @Override
    public void revokePermissionFromClient(Permission permission, Client client) {
        getLogger().info("Revoking permission {} from client {}", permission,
            client);

        if (permission == null || StringUtils.isBlank(permission.getUniqueId())) {
            String errMsg = "Null permission passed in or permission uniqueId was blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (client == null || StringUtils.isBlank(client.getUniqueId())) {
            String errMsg = "Null client passed in or client uniqueId was blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.DELETE, ATTR_PERMISSION,
            permission.getPermissionLDAPserialization()));

        LDAPResult result;
        Audit audit = Audit.log(client).modify(mods);
        try {
            result = getAppConnPool().modify(client.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error revoking permission from client {} - {}",
                permission, ldapEx);
            if (ldapEx.getResultCode().equals(ResultCode.NO_SUCH_ATTRIBUTE)) {
                throw new NotFoundException("Client doesn't have permission");
            }
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            getLogger().error("Error revoking permission from client {} - {}",
                permission, result.getResultCode());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when revoking permission from client: %s - %s",
                        permission, result.getResultCode().toString()));
        }
        audit.succeed();
        getLogger().info("Revoked permission {} from client {}", permission,
            client);
    }

    @Override
    public void setClientsLockedFlagByCustomerId(String customerId,
        boolean locked) {
        Clients clients = this.findFirst100ByCustomerIdAndLock(customerId,
            !locked);
        if (clients.getClients() != null && clients.getClients().size() > 0) {
            for (Client client : clients.getClients()) {
                client.setLocked(locked);
                this.updateClient(client);
            }
        }
        if (clients.getTotalRecords() > 0) {
            this.setClientsLockedFlagByCustomerId(customerId, locked);
        }
    }

    @Override
    public void updateClient(Client client) {
        getLogger().debug("Updating client {}", client);

        if (client == null || StringUtils.isBlank(client.getClientId())) {
            getLogger().error(
                "Client instance is null or its clientId has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The Client instance either null or its clientName has no value.");
        }
        String clientId = client.getClientId();
        Client oldClient = getClientByClientId(clientId);

        if (oldClient == null) {
            getLogger().error("No record found for client {}", clientId);
            throw new IllegalArgumentException(
                "There is no exisiting record for the given client instance.");
        }

        List<Modification> mods = getModifications(oldClient, client);

        if (client.equals(oldClient) || mods.size() < 1) {
            // No changes!
            return;
        }

        Audit audit = Audit.log(client).modify(mods);

        updateEntry(oldClient.getUniqueId(), mods, audit);

        audit.succeed();
        getLogger().debug("Updated client {}", client.getName());
    }

    @Override
    public void updateClientGroup(ClientGroup group) {
        getLogger().debug("Updating client group {}", group);

        if (group == null || StringUtils.isBlank(group.getUniqueId())) {
            getLogger().error(
                "ClientGroup instance is null or its uniqueId is blank.");
            throw new IllegalArgumentException(
                "Bad parameter: The Client instance is null or its uniqueId is blank.");
        }

        ClientGroup oldGroup = this.getClientGroupByUniqueId(group
            .getUniqueId());

        if (group.getType().equalsIgnoreCase(oldGroup.getType())) {
            return;
        }

        List<Modification> mods = new ArrayList<Modification>();

        if (group.getType() != null && StringUtils.isBlank(group.getType())) {
            mods.add(new Modification(ModificationType.DELETE, ATTR_GROUP_TYPE));
        } else {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_GROUP_TYPE, group.getType()));
        }

        Audit audit = Audit.log(group).modify(mods);

        updateEntry(oldGroup.getUniqueId(), mods, audit);

        audit.succeed();
        getLogger().debug("Updated clientGroup {}", group.getName());
    }

    @Override
    public void updateDefinedPermission(Permission permission) {
        getLogger().debug("Updating permission {}", permission);
        if (permission == null || StringUtils.isBlank(permission.getClientId())) {
            getLogger().error(
                "Resouce instance is null or its clientId has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The Permission instance either null or its clientName has no value.");
        }
        Permission oldPermission = this
            .getDefinedPermissionByClientIdAndPermissionId(
                permission.getClientId(), permission.getPermissionId());

        if (oldPermission == null) {
            getLogger().error("No record found for permission {}", permission);
            throw new IllegalArgumentException(
                "There is no exisiting record for the given permission instance.");
        }

        if (permission.equals(oldPermission)) {
            // No changes!
            return;
        }

        List<Modification> mods = getPermissionModifications(oldPermission,
            permission);
        Audit audit = Audit.log(oldPermission).modify(mods);

        updateEntry(oldPermission.getUniqueId(), mods, audit);

        audit.succeed();

        getLogger().debug("Updated permission {}", permission);
    }

    private Clients findFirst100ByCustomerIdAndLock(String customerId,
        boolean isLocked) {
        getLogger().debug("Doing search for customerId {}", customerId);

        int limit = 100;
        int offset = 0;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_LOCKED, String.valueOf(isLocked))
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Clients clients = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Found Users - {}", clients);

        return clients;
    }

    private Attribute[] getAddAttributesForClientGroup(ClientGroup group) {
        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_CLIENT_GROUP_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(group.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, group
                .getCustomerId()));
        }
        if (!StringUtils.isBlank(group.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, group.getClientId()));
        }
        if (!StringUtils.isBlank(group.getName())) {
            atts.add(new Attribute(ATTR_NAME, group.getName()));
        }
        if (!StringUtils.isBlank(group.getType())) {
            atts.add(new Attribute(ATTR_GROUP_TYPE, group.getType()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        return attributes;
    }

    private Attribute[] getAddAttributesForClientPermission(
        Permission permission) {
        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_PERMISSION_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(permission.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, permission
                .getCustomerId()));
        }
        if (!StringUtils.isBlank(permission.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, permission.getClientId()));
        }
        if (!StringUtils.isBlank(permission.getPermissionId())) {
            atts.add(new Attribute(ATTR_NAME, permission.getPermissionId()));
        }
        if (!StringUtils.isBlank(permission.getValue())) {
            atts.add(new Attribute(ATTR_BLOB, permission.getValue()));
        }
        if (!StringUtils.isBlank(permission.getType())) {
            atts.add(new Attribute(ATTR_PERMISSION_TYPE, permission.getType()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        return attributes;
    }

    private Attribute[] getAddAttributesForClient(Client client) {
        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_CLIENT_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(client.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, client.getClientId()));
        }

        if (!StringUtils.isBlank(client.getName())) {
            atts.add(new Attribute(ATTR_NAME, client.getName()));
        }

        if (!StringUtils.isBlank(client.getIname())) {
            atts.add(new Attribute(ATTR_INAME, client.getIname()));
        }

        if (!StringUtils.isBlank(client.getInum())) {
            atts.add(new Attribute(ATTR_INUM, client.getInum()));
        }

        if (!StringUtils.isBlank(client.getOrgInum())) {
            atts.add(new Attribute(ATTR_O, client.getOrgInum()));
        }

        if (!StringUtils.isBlank(client.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, client
                .getCustomerId()));
        }

        if (client.getStatus() != null) {
            atts.add(new Attribute(ATTR_STATUS, client.getStatus().toString()));
        }

        if (!StringUtils.isBlank(client.getClientSecretObj().getValue())) {
            atts.add(new Attribute(ATTR_CLIENT_SECRET, client.getClientSecret()));
        }

        if (client.isLocked() != null) {
            atts.add(new Attribute(ATTR_LOCKED, String.valueOf(client
                .isLocked())));
        }

        if (client.isSoftDeleted() != null) {
            atts.add(new Attribute(ATTR_SOFT_DELETED, String.valueOf(client
                .isSoftDeleted())));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        return attributes;
    }

    private Client getClient(SearchResultEntry resultEntry) {
        Client client = new Client();
        client.setUniqueId(resultEntry.getDN());
        client.setClientId(resultEntry.getAttributeValue(ATTR_CLIENT_ID));
        ClientSecret secret = ClientSecret.existingInstance(resultEntry
            .getAttributeValue(ATTR_CLIENT_SECRET));
        client.setClientSecretObj(secret);
        client.setName(resultEntry.getAttributeValue(ATTR_NAME));
        client.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        client.setIname(resultEntry.getAttributeValue(ATTR_INAME));

        client.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));

        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            client.setStatus(Enum.valueOf(ClientStatus.class,
                statusStr.toUpperCase()));
        }

        String deleted = resultEntry.getAttributeValue(ATTR_SOFT_DELETED);
        if (deleted != null) {
            client.setSoftDeleted(resultEntry
                .getAttributeValueAsBoolean(ATTR_SOFT_DELETED));
        }

        String locked = resultEntry.getAttributeValue(ATTR_LOCKED);
        if (locked != null) {
            client.setLocked(resultEntry
                .getAttributeValueAsBoolean(ATTR_LOCKED));
        }

        String[] permissions = resultEntry.getAttributeValues(ATTR_PERMISSION);

        if (permissions != null && permissions.length > 0) {

            List<Permission> perms = new ArrayList<Permission>();

            for (String s : permissions) {
                String[] split = s.split(Permission.LDAP_SEPERATOR);

                if (split.length == 3) {
                    perms
                        .add(new Permission(split[0], split[1], split[2], null));
                }
            }
            client.setPermissions(perms);
        }

        return client;
    }

    private ClientGroup getClientGroup(SearchResultEntry resultEntry) {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId(resultEntry.getDN());
        clientGroup.setClientId(resultEntry.getAttributeValue(ATTR_CLIENT_ID));
        clientGroup.setName(resultEntry.getAttributeValue(ATTR_NAME));
        clientGroup.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        clientGroup.setType(resultEntry.getAttributeValue(ATTR_GROUP_TYPE));
        return clientGroup;
    }

    private Clients getMultipleClients(Filter searchFilter, int offset,
        int limit) {

        offset = offset < 0 ? this.getLdapPagingOffsetDefault() : offset;
        limit = limit <= 0 ? this.getLdapPagingLimitDefault() : limit;
        limit = limit > this.getLdapPagingLimitMax() ? this
            .getLdapPagingLimitMax() : limit;

        int contentCount = 0;

        List<Client> clientList = new ArrayList<Client>();

        List<SearchResultEntry> entries = this.getMultipleEntries(BASE_DN,
            SearchScope.SUB, searchFilter, ATTR_NAME);

        contentCount = entries.size();

        if (offset < contentCount) {

            int toIndex = offset + limit > contentCount ? contentCount : offset
                + limit;
            int fromIndex = offset;

            List<SearchResultEntry> subList = entries.subList(fromIndex,
                toIndex);

            for (SearchResultEntry entry : subList) {
                clientList.add(getClient(entry));
            }
        }

        getLogger().debug("Found clients {}", clientList);

        Clients clients = new Clients();

        clients.setLimit(limit);
        clients.setOffset(offset);
        clients.setTotalRecords(contentCount);
        clients.setClients(clientList);

        return clients;
    }

    private Permission getPermission(SearchResultEntry resultEntry) {
        Permission permission = new Permission();
        permission.setUniqueId(resultEntry.getDN());
        permission.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        permission.setClientId(resultEntry.getAttributeValue(ATTR_CLIENT_ID));
        permission.setPermissionId(resultEntry.getAttributeValue(ATTR_NAME));
        permission.setType(resultEntry.getAttributeValue(ATTR_PERMISSION_TYPE));
        permission.setValue(resultEntry.getAttributeValue(ATTR_BLOB));
        return permission;
    }

    private Client getSingleClient(Filter searchFilter) {
        Client client = null;
        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter);

        if (entry != null) {
            client = getClient(entry);
        }

        getLogger().debug("Found Client - {}", client);

        return client;
    }

    List<Modification> getModifications(Client cOld, Client cNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (cNew.getClientSecretObj().isNew()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_CLIENT_SECRET, cNew.getClientSecretObj().getValue()));
        }

        if (cNew.getIname() != null) {
            if (StringUtils.isBlank(cNew.getIname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_INAME));
            } else if (!StringUtils.equals(cOld.getIname(), cNew.getIname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_INAME,
                    cNew.getIname()));
            }
        }

        if (cNew.isLocked() != null && !cNew.isLocked().equals(cOld.isLocked())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED,
                String.valueOf(cNew.isLocked())));
        }

        if (cNew.getStatus() != null
            && !cOld.getStatus().equals(cNew.getStatus())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_STATUS,
                cNew.getStatus().toString()));
        }

        if (cNew.isSoftDeleted() != null
            && cNew.isSoftDeleted() != cOld.isSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_SOFT_DELETED, String.valueOf(cNew.isSoftDeleted())));
        }

        return mods;
    }

    List<Modification> getPermissionModifications(Permission rOld,
        Permission rNew) {
        List<Modification> mods = new ArrayList<Modification>();
        if (!StringUtils.equals(rOld.getValue(), rNew.getValue())) {
            if (!StringUtils.isBlank(rNew.getValue())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_BLOB,
                    rNew.getValue()));
            }
        }
        if (!StringUtils.equals(rOld.getType(), rNew.getType())) {
            if (!StringUtils.isBlank(rNew.getType())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_PERMISSION_TYPE, rNew.getType()));
            } else {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_PERMISSION_TYPE));
            }
        }
        return mods;
    }
}
