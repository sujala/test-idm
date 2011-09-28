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
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;

public class LdapClientRepository extends LdapRepository implements ClientDao {

    public LdapClientRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    @Override
    public void addClient(Client client) {
        getLogger().info("Adding client {}", client);

        if (client == null) {
            String errMsg = "Null instance of Client was passed in.";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Audit audit = Audit.log(client).add();

        Attribute[] attributes = getAddAttributesForClient(client);

        String clientDN = new LdapDnBuilder(APPLICATIONS_BASE_DN).addAttribute(
            ATTR_CLIENT_ID, client.getClientId()).build();

        client.setUniqueId(clientDN);

        addEntry(clientDN, attributes, audit);

        audit.succeed();

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

        String groupDN = new LdapDnBuilder(clientUniqueId).addAttribute(
            ATTR_NAME, clientGroup.getName()).build();

        clientGroup.setUniqueId(groupDN);

        addEntry(groupDN, atts, audit);

        audit.succeed();

        getLogger().debug("Added clientGroup {}", clientGroup);
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
        mods.add(new Modification(ModificationType.ADD, ATTR_MEMBER_OF, group
            .getUniqueId()));

        Audit audit = Audit.log(group).modify(mods);

        try {
            getAppConnPool().modify(userUniqueId, mods);
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
            getLogger().debug("Client {} could not be found.", clientId);
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

        boolean isAuthenticated = ResultCode.SUCCESS.equals(result
            .getResultCode());
        getLogger().debug("Client {} authenticated == {}", isAuthenticated);
        return new ClientAuthenticationResult(client, isAuthenticated);
    }

    @Override
    public void deleteClient(Client client) {
        getLogger().info("Deleting client {}", client.getClientId());

        Audit audit = Audit.log(client).delete();

        this.deleteEntryAndSubtree(client.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted client {}", client.getClientId());
    }

    @Override
    public void deleteClientGroup(ClientGroup group) {
        getLogger().info("Deleting clientGroup {}", group.getName());
        Audit audit = Audit.log(group).delete();

        this.deleteEntryAndSubtree(group.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted clientGroup {}", group);
    }

    @Override
    public List<Client> getAllClients() {
        getLogger().debug("Search all clients");

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        List<Client> clients = new ArrayList<Client>();

        List<SearchResultEntry> entries = this.getMultipleEntries(
            APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter, ATTR_NAME);

        for (SearchResultEntry entry : entries) {
            clients.add(getClient(entry));
        }

        getLogger().debug("Found {} clients.", clients.size());

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
    public Client getClientById(String id) {
        getLogger().debug("Doing search for id {}", id);

        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            throw new IllegalArgumentException("Null or Empty id parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, id)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    @Override
    public Client getClientByScope(String scope) {
        getLogger().debug("Doing search for Client with Scope {}", scope);

        if (StringUtils.isBlank(scope)) {
            getLogger().error("Null or Empty scope parameter");
            throw new IllegalArgumentException("Null or Empty scope parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_TOKEN_SCOPE, scope)
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

        SearchResultEntry entry = this.getSingleEntry(APPLICATIONS_BASE_DN,
            SearchScope.SUB, searchFilter, ATTR_GROUP_SEARCH_ATTRIBUTES);

        if (entry != null) {
            group = getClientGroup(entry);
        }

        getLogger().debug("Found client group {}", group);

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

        String searchDN = client.getUniqueId();

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

        getLogger().debug("Found {} client groups.", groups.size());

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

        getLogger().debug("Found {} clients for customer {}",
            clients.getTotalRecords(), customerId);

        return clients;
    }

    @Override
    public boolean isUserInClientGroup(String username, String groupDN) {

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .addEqualAttribute(ATTR_MEMBER_OF, groupDN).build();

        SearchResultEntry entry = this.getSingleEntry(USERS_BASE_DN,
            SearchScope.ONE, searchFilter, ATTR_NO_ATTRIBUTES);

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
        mods.add(new Modification(ModificationType.DELETE, ATTR_MEMBER_OF,
            group.getUniqueId()));

        Audit audit = Audit.log(group).modify(mods);
        try {
            getAppConnPool().modify(userUniqueId, mods);
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
    public void setClientsLockedFlagByCustomerId(String customerId,
        boolean locked) {
        Clients clients = this.findFirst100ByCustomerIdAndLock(customerId,
            !locked);
        String msg = String.format(
            "Setting lock status to %s for %s clients under customer %s",
            locked, clients.getTotalRecords(), customerId);
        getLogger().debug(msg);
        if (clients.getClients() != null && clients.getClients().size() > 0) {
            for (Client client : clients.getClients()) {
                client.setLocked(locked);
                this.updateClient(client);
            }
        }
        if (clients.getTotalRecords() > 0) {
            getLogger().debug(
                "Attempting lookup of additional users under customer {}.",
                customerId);
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
    public List<Client> getAvailableScopes() {
        getLogger().debug("Search the scope accesses defined in the system.");

        Filter filter = new LdapSearchBuilder()
            .addPresenceAttribute(ATTR_TOKEN_SCOPE)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        LDAPConnection conn = null;

        List<Client> clients = new ArrayList<Client>();

        try {
            conn = getAppConnPool().getConnection();
            final SearchResult searchResult = conn.search(APPLICATIONS_BASE_DN,
                SearchScope.SUB, filter);

            final List<SearchResultEntry> entries = searchResult
                .getSearchEntries();

            for (SearchResultEntry entry : entries) {
                clients.add(getClient(entry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scopeAccessList for clients.", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }

        getLogger().debug("Found the scope accesses defined in the system.");

        return clients;
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
        getLogger().debug("Found {} add attributes for client group {}.",
            attributes.length, group);
        return attributes;
    }

    private Attribute[] getAddAttributesForClient(Client client) {
        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_CLIENT_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(client.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, client.getClientId()));
        }
        
        if (!StringUtils.isBlank(client.getOpenStackType())) {
            atts.add(new Attribute(ATTR_OPENSTACK_TYPE, client.getOpenStackType()));
        }

        if (!StringUtils.isBlank(client.getName())) {
            atts.add(new Attribute(ATTR_NAME, client.getName()));
        }

        if (!StringUtils.isBlank(client.getOrgInum())) {
            atts.add(new Attribute(ATTR_O, client.getOrgInum()));
        }

        if (!StringUtils.isBlank(client.getRCN())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, client
                .getRCN()));
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

        if (!StringUtils.isBlank(client.getTitle())) {
            atts.add(new Attribute(ATTR_TITLE, client.getTitle()));
        }

        if (!StringUtils.isBlank(client.getDescription())) {
            atts.add(new Attribute(ATTR_DESCRIPTION, client.getDescription()));
        }

        if (!StringUtils.isBlank(client.getScope())) {
            atts.add(new Attribute(ATTR_TOKEN_SCOPE, client.getScope()));
        }

        if (!StringUtils.isBlank(client.getCallBackUrl())) {
            atts.add(new Attribute(ATTR_CALLBACK_URL, client.getCallBackUrl()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);
        getLogger().debug("Found {} attributes for client {}.",
            attributes.length, client);
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

        client.setRCN(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        
        client.setOpenStackType(resultEntry.getAttributeValue(ATTR_OPENSTACK_TYPE));

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

        client.setCallBackUrl(resultEntry.getAttributeValue(ATTR_CALLBACK_URL));
        client.setTitle(resultEntry.getAttributeValue(ATTR_TITLE));
        client.setDescription(resultEntry.getAttributeValue(ATTR_DESCRIPTION));
        client.setScope(resultEntry.getAttributeValue(ATTR_TOKEN_SCOPE));

        getLogger().debug("Materialized Client object {}.", client);
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
        getLogger().debug("Materialized client group {}.", clientGroup);

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

        List<SearchResultEntry> entries = this.getMultipleEntries(
            APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter, ATTR_NAME);

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

        getLogger().debug("Found {} clients.", clientList.size());

        Clients clients = new Clients();

        clients.setLimit(limit);
        clients.setOffset(offset);
        clients.setTotalRecords(contentCount);
        clients.setClients(clientList);

        return clients;
    }

    private Client getSingleClient(Filter searchFilter) {
        Client client = null;
        SearchResultEntry entry = this.getSingleEntry(APPLICATIONS_BASE_DN,
            SearchScope.SUB, searchFilter);

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

        if (cNew.getTitle() != null) {
            if (StringUtils.isBlank(cNew.getTitle())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_TITLE));
            } else if (!StringUtils.equals(cOld.getTitle(), cNew.getTitle())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_TITLE,
                    cNew.getTitle()));
            }
        }

        if (cNew.getDescription() != null) {
            if (StringUtils.isBlank(cNew.getDescription())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_DESCRIPTION));
            } else if (!StringUtils.equals(cOld.getDescription(),
                cNew.getDescription())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_DESCRIPTION, cNew.getDescription()));
            }
        }

        if (cNew.getScope() != null) {
            if (StringUtils.isBlank(cNew.getScope())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_TOKEN_SCOPE));
            } else if (!StringUtils.equals(cOld.getScope(), cNew.getScope())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_TOKEN_SCOPE, cNew.getScope()));
            }
        }

        if (cNew.getCallBackUrl() != null) {
            if (StringUtils.isBlank(cNew.getCallBackUrl())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_CALLBACK_URL));
            } else if (!StringUtils.equals(cOld.getCallBackUrl(),
                cNew.getCallBackUrl())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_CALLBACK_URL, cNew.getCallBackUrl()));
            }
        }

        getLogger().debug("Found {} modifications.", mods.size());

        return mods;
    }

    @Override
    public void addClientRole(String clientUniqueId, ClientRole role) {
        if (StringUtils.isBlank(clientUniqueId)) {
            String errMsg = "clientUniqueId cannot be blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        if (role == null) {
            String errmsg = "Null instance of Client Role was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        getLogger().info("Adding Client Role: {}", role);
        Audit audit = Audit.log(role).add();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();

            SearchResultEntry entry = getContainer(conn, clientUniqueId,
                CONTAINER_ROLES);
            if (entry == null) {
                addContianer(conn, clientUniqueId, CONTAINER_ROLES);
                entry = getContainer(conn, clientUniqueId, CONTAINER_ROLES);
            }

            final LDAPPersister<ClientRole> persister = LDAPPersister
                .getInstance(ClientRole.class);
            persister.add(role, conn, entry.getDN());
            audit.succeed();
            getLogger().info("Added Client Role: {}", role);
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                String errMsg = String.format("Tenant %s already exists",
                    role.getName());
                getLogger().warn(errMsg);
                throw new DuplicateException(errMsg);
            }
            getLogger().error("Error adding client role object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public void deleteClientRole(ClientRole role) {
        if (role == null) {
            getLogger().error("Null or Empty Client Role parameter");
            throw new IllegalArgumentException(
                "Null or Empty Client Role parameter.");
        }
        getLogger().debug("Deleting ClientRole: {}", role);
        final String dn = role.getUniqueId();
        final Audit audit = Audit.log(role).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted ClientRole: {}", role);
        return;
    }

    @Override
    public ClientRole getClientRoleByClientIdAndRoleName(String clientId,
        String roleName) {
        if (StringUtils.isBlank(clientId)) {
            String errmsg = "ClientId cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        if (StringUtils.isBlank(roleName)) {
            String errmsg = "roleName cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Doing search for ClientRole " + roleName);

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_NAME, roleName)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
            .build();

        ClientRole role = null;

        try {
            role = getSingleClientRole(APPLICATIONS_BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting role object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found Client Role - {}", role);

        return role;
    }

    @Override
    public List<ClientRole> getClientRolesByClientId(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            String errmsg = "cilentId cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        getLogger().debug("Getting clientRoles for client {}", clientId);
        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
            .build();

        List<ClientRole> roles = new ArrayList<ClientRole>();
        try {
            roles = getMultipleClientRoles(APPLICATIONS_BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting client roles object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Client Roles", roles.size());

        return roles;
    }

    @Override
    public void updateClientRole(ClientRole role) {
        if (role == null || StringUtils.isBlank(role.getUniqueId())) {
            String errmsg = "Null instance of Client Role was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Updating Client Role: {}", role);
        LDAPConnection conn = null;
        Audit audit = Audit.log(role);
        try {
            conn = getAppConnPool().getConnection();
            final LDAPPersister<ClientRole> persister = LDAPPersister
                .getInstance(ClientRole.class);
            List<Modification> modifications = persister.getModifications(role,
                true);
            audit.modify(modifications);
            persister.modify(role, conn, null, true);
            getLogger().debug("Updated Client Role: {}", role);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating Client Role", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    private List<ClientRole> getMultipleClientRoles(String baseDN,
        Filter searchFilter) throws LDAPPersistException {
        List<SearchResultEntry> entries = this.getMultipleEntries(baseDN,
            SearchScope.SUB, searchFilter, ATTR_NAME);

        List<ClientRole> roles = new ArrayList<ClientRole>();
        for (SearchResultEntry entry : entries) {
            roles.add(getClientRole(entry));
        }
        return roles;
    }

    private ClientRole getSingleClientRole(String baseDN, Filter searchFilter)
        throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(baseDN, SearchScope.SUB,
            searchFilter);
        ClientRole role = getClientRole(entry);
        return role;
    }

    private ClientRole getClientRole(SearchResultEntry entry)
        throws LDAPPersistException {
        if (entry == null) {
            return null;
        }
        ClientRole role = null;
        role = LDAPPersister.getInstance(ClientRole.class).decode(entry);
        return role;
    }
    
    @Override
    public String getNextRoleId() {
        String roleId = null;
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            roleId = getNextId(conn, NEXT_ROLE_ID);
        } catch (LDAPException e) {
            getLogger().error("Error getting next roleId", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return roleId;
    }
    
    @Override
    public ClientRole getClientRoleById(String id) {
        if (StringUtils.isBlank(id)) {
            String errmsg = "id cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Doing search for ClientRole " + id);

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, id)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
            .build();

        ClientRole role = null;

        try {
            role = getSingleClientRole(APPLICATIONS_BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting role object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found Client Role - {}", role);

        return role;
    }
}
