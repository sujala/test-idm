package com.rackspace.idm.domain.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LdapApplicationRepository extends LdapRepository implements ApplicationDao {

    public static final String ENCRYPTION_ERROR = "encryption error";
    public static final String FOUND_CLIENT = "Found client - {}";

    @Autowired
    CryptHelper cryptHelper;

    @Override
    public void addClient(Application client) {
        getLogger().info("Adding client {}", client);

        if (client == null) {
            String errMsg = "Null instance of Client was passed in.";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Audit audit = Audit.log(client).add();

        Attribute[] attributes;
        try {
            attributes = getAddAttributesForClient(client);
            String clientDN = new LdapDnBuilder(APPLICATIONS_BASE_DN).addAttribute(ATTR_CLIENT_ID, client.getClientId()).build();
            client.setUniqueId(clientDN);
            addEntry(clientDN, attributes, audit);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        }

        // Now that it's in LDAP we'll set the password to the "existing" type
        client.setClientSecretObj(client.getClientSecretObj().toExisting());

        audit.succeed();

        getLogger().debug("Added client {}", client);
    }

    @Override
    public ClientAuthenticationResult authenticate(String clientId, String clientSecret) {
        BindResult result;
        Application client = getClientByClientId(clientId);

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
            getLogger().error("Bind operation on clientId " + clientId + " failed.", e);
            throw new IllegalStateException(e.getMessage(), e);
        }

        boolean isAuthenticated = ResultCode.SUCCESS.equals(result.getResultCode());
        getLogger().debug("Client {} authenticated == {}", isAuthenticated);
        return new ClientAuthenticationResult(client, isAuthenticated);
    }

    @Override
    public void deleteClient(Application client) {
        if (client != null) {
            getLogger().info("Deleting client {}", client.getClientId());

            Audit audit = Audit.log(client).delete();

            this.deleteEntryAndSubtree(client.getUniqueId(), audit);

            audit.succeed();
            getLogger().info("Deleted client {}", client.getClientId());
        }
    }

    @Override
    public List<Application> getAllClients() {
        getLogger().debug("Search all clients");

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION).build();

        List<Application> clients = new ArrayList<Application>();

        List<SearchResultEntry> entries = this.getMultipleEntries(APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter, ATTR_NAME);

        for (SearchResultEntry entry : entries) {
            clients.add(getClient(entry));
        }

        getLogger().debug("Found {} clients.", clients.size());

        return clients;
    }

    @Override
    public Application getClientByClientId(String clientId) {
        getLogger().debug("Doing search for clientId {}", clientId);

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            getLogger().info("Invalid clientId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CLIENT_ID, clientId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application client = getSingleClient(searchFilter);

        getLogger().debug(FOUND_CLIENT, client);

        return client;
    }

    @Override
    public Application getClientByClientname(String clientName) {
        getLogger().debug("Searching for client {}", clientName);

        if (StringUtils.isBlank(clientName)) {
            getLogger().error("Null or Empty client name parameter");
            throw new IllegalArgumentException("Null or Empty client name parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, clientName)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application client = getSingleClient(searchFilter);

        getLogger().debug(FOUND_CLIENT, client);

        return client;
    }

    @Override
    public Application getClientByCustomerIdAndClientId(String customerId, String clientId) {
        getLogger().debug("Doing search for clientId {}", clientId);

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            getLogger().info("Invalid clientId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CLIENT_ID, clientId)
                .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application client = getSingleClient(searchFilter);

        getLogger().debug(FOUND_CLIENT, client);

        return client;
    }

    @Override
    public Application getClientById(String id) {
        getLogger().debug("Doing search for id {}", id);

        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            throw new IllegalArgumentException("Null or Empty id parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application client = getSingleClient(searchFilter);

        getLogger().debug(FOUND_CLIENT, client);

        return client;
    }

    @Override
    public Application getClientByScope(String scope) {
        getLogger().debug("Doing search for Client with Scope {}", scope);

        if (StringUtils.isBlank(scope)) {
            getLogger().error("Null or Empty scope parameter");
            throw new IllegalArgumentException("Null or Empty scope parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_TOKEN_SCOPE, scope)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application client = getSingleClient(searchFilter);

        getLogger().debug(FOUND_CLIENT, client);

        return client;
    }

    @Override
    public Applications getClientsByCustomerId(String customerId, int offset,
                                               int limit) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                    "Null or Empty customerId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
                .addEqualAttribute(ATTR_OBJECT_CLASS,
                        OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Applications clients = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Found {} clients for customer {}",
                clients.getTotalRecords(), customerId);

        return clients;
    }

    @Override
    public Applications getAllClients(List<FilterParam> filters, int offset, int limit) {
        getLogger().debug("Getting all applications");

        LdapSearchBuilder searchBuilder = new LdapSearchBuilder();
        searchBuilder.addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION);

        if (filters != null) {
            for (FilterParam filter : filters) {
                // can only filter on application name for now
                if (filter.getParam() == FilterParamName.APPLICATION_NAME) {
                    searchBuilder.addEqualAttribute(ATTR_NAME, filter.getStrValue());
                }
            }
        }

        Filter searchFilter = searchBuilder.build();

        Applications applications = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Got {} applications", applications.getTotalRecords());

        return applications;
    }

    @Override
    public void updateClient(Application client) {
        getLogger().debug("Updating client {}", client);

        if (client == null || StringUtils.isBlank(client.getClientId())) {
            getLogger().error("Client instance is null or its clientId has no value");
            throw new IllegalArgumentException("Bad parameter: The Client instance either null or its clientName has no value.");
        }
        String clientId = client.getClientId();
        Application oldClient = getClientByClientId(clientId);

        if (oldClient == null) {
            getLogger().error("No record found for client {}", clientId);
            throw new IllegalArgumentException("There is no existing record for the given client instance.");
        }

        Audit audit = Audit.log(client);
        List<Modification> mods;
        try {
            mods = getModifications(oldClient, client);
            if (mods.size() < 1) {
                // No changes!
                return;
            }
            audit.modify(mods);

            updateEntry(oldClient.getUniqueId(), mods, audit);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        }

        audit.succeed();
        getLogger().debug("Updated client {}", client.getName());
    }

    @Override
    public List<Application> getAvailableScopes() {
        getLogger().debug("Search the scope accesses defined in the system.");

        Filter filter = new LdapSearchBuilder()
                .addPresenceAttribute(ATTR_TOKEN_SCOPE)
                .addEqualAttribute(ATTR_OBJECT_CLASS,
                        OBJECTCLASS_RACKSPACEAPPLICATION).build();

        List<Application> clients = new ArrayList<Application>();

        try {
            final SearchResult searchResult = getAppInterface().search(APPLICATIONS_BASE_DN,
                    SearchScope.SUB, filter);

            final List<SearchResultEntry> entries = searchResult.getSearchEntries();

            for (SearchResultEntry entry : entries) {
                clients.add(getClient(entry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scopeAccessList for clients.", e);
            throw new IllegalStateException(e.getMessage(), e);
        }

        getLogger().debug("Found the scope accesses defined in the system.");

        return clients;
    }

    Attribute[] getAddAttributesForClient(Application client) throws InvalidCipherTextException, GeneralSecurityException {
        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_CLIENT_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(client.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, client.getClientId()));
        }

        if (!StringUtils.isBlank(client.getOpenStackType())) {
            atts.add(new Attribute(ATTR_OPENSTACK_TYPE, client.getOpenStackType()));
        }

        if (!StringUtils.isBlank(client.getName())) {
            atts.add(new Attribute(ATTR_NAME, client.getName()));
        }

        if (!StringUtils.isBlank(client.getRCN())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, client.getRCN()));
        }

        if (!StringUtils.isBlank(client.getClientSecretObj().getValue())) {
            atts.add(new Attribute(ATTR_CLIENT_SECRET, client.getClientSecret()));
            atts.add(new Attribute(ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(client.getClientSecret())));
        }

        if (client.isEnabled() != null) {
            atts.add(new Attribute(ATTR_ENABLED, String.valueOf(client.isEnabled()).toUpperCase()));
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

        if (client.getUseForDefaultRegion() != null) {
            atts.add(new Attribute(ATTR_USE_FOR_DEFAULT_REGION, String.valueOf(client.getUseForDefaultRegion()).toUpperCase()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);
        getLogger().debug("Found {} attributes for client {}.", attributes.length, client);
        return attributes;
    }

    Application getClient(SearchResultEntry resultEntry) {
        Application client = new Application();
        client.setUniqueId(resultEntry.getDN());
        client.setClientId(resultEntry.getAttributeValue(ATTR_CLIENT_ID));

        try {
            String ecryptedPwd = cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_CLEAR_PASSWORD));
            ClientSecret secret = ClientSecret.existingInstance(ecryptedPwd);
            client.setClientSecretObj(secret);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        client.setName(resultEntry.getAttributeValue(ATTR_NAME));

        client.setRCN(resultEntry.getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));

        client.setOpenStackType(resultEntry.getAttributeValue(ATTR_OPENSTACK_TYPE));

        client.setEnabled(resultEntry.getAttributeValueAsBoolean(ATTR_ENABLED));

        client.setCallBackUrl(resultEntry.getAttributeValue(ATTR_CALLBACK_URL));
        client.setTitle(resultEntry.getAttributeValue(ATTR_TITLE));
        client.setDescription(resultEntry.getAttributeValue(ATTR_DESCRIPTION));
        client.setScope(resultEntry.getAttributeValue(ATTR_TOKEN_SCOPE));
        client.setUseForDefaultRegion(resultEntry.getAttributeValueAsBoolean(ATTR_USE_FOR_DEFAULT_REGION));

        getLogger().debug("Materialized Client object {}.", client);
        return client;
    }

    Applications getMultipleClients(Filter searchFilter, int offset, int limit) {

        int offsets = offset < 0 ? this.getLdapPagingOffsetDefault() : offset;
        int limits = limit <= 0 ? this.getLdapPagingLimitDefault() : limit;
        limits = limits > this.getLdapPagingLimitMax() ? this.getLdapPagingLimitMax() : limits;

        int contentCount = 0;

        List<Application> clientList = new ArrayList<Application>();

        List<SearchResultEntry> entries = this.getMultipleEntries(APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter, null);

        contentCount = entries.size();

        if (offsets < contentCount) {

            int toIndex = offsets + limits > contentCount ? contentCount : offsets + limits;
            int fromIndex = offsets;

            List<SearchResultEntry> subList = entries.subList(fromIndex, toIndex);

            for (SearchResultEntry entry : subList) {
                clientList.add(getClient(entry));
            }
        }

        getLogger().debug("Found {} clients.", clientList.size());

        Applications clients = new Applications();

        clients.setLimit(limits);
        clients.setOffset(offsets);
        clients.setTotalRecords(contentCount);
        clients.setClients(clientList);

        return clients;
    }

    Application getSingleClient(Filter searchFilter) {
        Application client = null;
        SearchResultEntry entry = this.getSingleEntry(APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter);

        if (entry != null) {
            client = getClient(entry);
        }

        getLogger().debug(FOUND_CLIENT, client);

        return client;
    }

    Application getSingleSoftDeletedClient(Filter searchFilter) {
        Application client = null;
        SearchResultEntry entry = this.getSingleEntry(SOFT_DELETED_APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter);

        if (entry != null) {
            client = getClient(entry);
        }

        getLogger().debug(FOUND_CLIENT, client);

        return client;
    }

    List<Modification> getModifications(Application cOld, Application cNew) throws InvalidCipherTextException, GeneralSecurityException {
        List<Modification> mods = new ArrayList<Modification>();

        checkForRCNModification(cOld, cNew, mods);
        checkForClientSecretModification(cNew, cryptHelper, mods);
        checkForEnabledStatusModification(cOld, cNew, mods);
        checkForTitleModification(cOld, cNew, mods);
        checkForDescriptionModification(cOld, cNew, mods);
        checkForScopeModification(cOld, cNew, mods);
        checkForCallBackUrlModification(cOld, cNew, mods);
        checkForUseForDefaultRegionModification(cOld,cNew,mods);

        getLogger().debug("Found {} modifications.", mods.size());

        return mods;
    }

    private void checkForCallBackUrlModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getCallBackUrl() != null) {
            if (StringUtils.isBlank(cNew.getCallBackUrl())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_CALLBACK_URL));
            } else if (!StringUtils.equals(cOld.getCallBackUrl(), cNew.getCallBackUrl())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_CALLBACK_URL, cNew.getCallBackUrl()));
            }
        }
    }

    private void checkForScopeModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getScope() != null) {
            if (StringUtils.isBlank(cNew.getScope())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_TOKEN_SCOPE));
            } else if (!StringUtils.equals(cOld.getScope(), cNew.getScope())) {
                mods.add(new Modification(ModificationType.REPLACE,
                        ATTR_TOKEN_SCOPE, cNew.getScope()));
            }
        }
    }

    private void checkForDescriptionModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getDescription() != null) {
            if (StringUtils.isBlank(cNew.getDescription())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_DESCRIPTION));
            } else if (!StringUtils.equals(cOld.getDescription(), cNew.getDescription())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_DESCRIPTION, cNew.getDescription()));
            }
        }
    }

    private void checkForTitleModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getTitle() != null) {
            if (StringUtils.isBlank(cNew.getTitle())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_TITLE));
            } else if (!StringUtils.equals(cOld.getTitle(), cNew.getTitle())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_TITLE, cNew.getTitle()));
            }
        }
    }

    private void checkForEnabledStatusModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.isEnabled() != null && !cNew.isEnabled().equals(cOld.isEnabled())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(cNew.isEnabled()).toUpperCase()));
        }
    }

    void checkForUseForDefaultRegionModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getUseForDefaultRegion() != null && !cNew.getUseForDefaultRegion().equals(cOld.getUseForDefaultRegion())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_USE_FOR_DEFAULT_REGION, String.valueOf(cNew.getUseForDefaultRegion()).toUpperCase()));
        }
    }

    private void checkForClientSecretModification(Application cNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        //TODO null pointer?
        if (cNew.getClientSecretObj() != null && cNew.getClientSecretObj().isNew()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_CLIENT_SECRET, cNew.getClientSecretObj().getValue()));
            mods.add(new Modification(ModificationType.REPLACE, ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(cNew.getClientSecretObj().getValue())));
        }
    }

    private void checkForRCNModification(Application cOld, Application cNew, List<Modification> mods) {
        if (cNew.getRCN() != null && !cNew.getRCN().equals(cOld.getRCN())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_CUSTOMER_NUMBER, cNew.getRCN()));
        }
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
        try {

            SearchResultEntry entry = getContainer(clientUniqueId, CONTAINER_TOKENS);
            if (entry == null) {
                addContainer(clientUniqueId, CONTAINER_TOKENS);
                entry = getContainer(clientUniqueId, CONTAINER_TOKENS);
            }

            final LDAPPersister<ClientRole> persister = LDAPPersister.getInstance(ClientRole.class);
            persister.add(role, getAppInterface(), entry.getDN());
            audit.succeed();
            getLogger().info("Added Client Role: {}", role);
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                String errMsg = String.format("Tenant %s already exists",
                        role.getName());
                getLogger().warn(errMsg);
                throw new DuplicateException(errMsg, e);
            }
            getLogger().error("Error adding client role object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
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
    public ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName) {
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
    public List<ClientRole> getAllClientRoles(List<FilterParam> filters) {
        getLogger().debug("Getting all roles");

        LdapSearchBuilder searchBuilder = new LdapSearchBuilder();
        searchBuilder.addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE);

        if (filters != null) {
            for (FilterParam filter : filters) {
                // can only filter on applicationiId and rolename for now
                if (filter.getParam() == FilterParamName.ROLE_NAME) {
                    searchBuilder.addEqualAttribute(ATTR_NAME, filter.getStrValue());
                } else if (filter.getParam() == FilterParamName.APPLICATION_ID) {
                    searchBuilder.addEqualAttribute(ATTR_CLIENT_ID, filter.getStrValue());
                }
            }
        }

        Filter searchFilter = searchBuilder.build();
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
    public List<ClientRole> getAllClientRoles() {

        getLogger().debug("Getting all client roles");
        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
                ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE).build();

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
        Audit audit = Audit.log(role);
        try {
            final LDAPPersister<ClientRole> persister = LDAPPersister.getInstance(ClientRole.class);
            List<Modification> modifications = persister.getModifications(role, true);
            if(modifications.size()<1){
                return;
            }
            audit.modify(modifications);
            persister.modify(role, getAppInterface(), null, true);
            getLogger().debug("Updated Client Role: {}", role);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating Client Role", e);
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    List<ClientRole> getMultipleClientRoles(String baseDN, Filter searchFilter) throws LDAPPersistException {
        List<SearchResultEntry> entries = this.getMultipleEntries(baseDN, SearchScope.SUB, searchFilter, null);

        List<ClientRole> roles = new ArrayList<ClientRole>();
        if(entries==null || entries.size()==0){
            return roles;
        }
        for (SearchResultEntry entry : entries) {
            roles.add(getClientRole(entry));
        }
        return roles;
    }

    ClientRole getSingleClientRole(String baseDN, Filter searchFilter)
            throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(baseDN, SearchScope.SUB,
                searchFilter);
        return getClientRole(entry);
    }

    ClientRole getClientRole(SearchResultEntry entry)
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
        return getNextId(NEXT_ROLE_ID);
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

    @Override
    public List<Application> getOpenStackServices() {
        final int limit = 400;

        Filter searchFilter = new LdapSearchBuilder()
                .addPresenceAttribute(ATTR_OPENSTACK_TYPE)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Applications clients = getMultipleClients(searchFilter, 0, limit);

        return clients.getClients();
    }

    @Override
    public void softDeleteApplication(Application application) {
        getLogger().info("SoftDeleting customer - {}", application.getRCN());
        try {
            String oldDn = application.getUniqueId();
            String newRdn = new LdapDnBuilder("").addAttribute(ATTR_CLIENT_ID, application.getClientId()).build();
            String newDn = new LdapDnBuilder(SOFT_DELETED_APPLICATIONS_BASE_DN).addAttribute(ATTR_CLIENT_ID, application.getClientId()).build();
            // Modify the Application
            getAppInterface().modifyDN(oldDn, newRdn, false, SOFT_DELETED_APPLICATIONS_BASE_DN);
            application.setUniqueId(newDn);
            // Disable the Application
            getAppInterface().modify(application.getUniqueId(), new Modification(
                    ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(false).toUpperCase()));
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting application", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("SoftDeleted application - {}", application.getRCN());
    }

    @Override
    public Application getSoftDeletedApplicationById(String id) {

        getLogger().debug("Doing search for id " + id);
        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            throw new IllegalArgumentException("Null or Empty id parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CLIENT_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application application = getSingleSoftDeletedClient(searchFilter);

        getLogger().debug("Found User - {}", application);

        return application;
    }

    @Override
    public Application getSoftDeletedClientByName(String clientName) {

        getLogger().debug("Doing search for application " + clientName);
        if (StringUtils.isBlank(clientName)) {
            getLogger().error("Null or Empty clientName parameter");
            throw new IllegalArgumentException(
                    "Null or Empty clientName parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, clientName)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEAPPLICATION)
                .build();

        Application application = getSingleSoftDeletedClient(searchFilter);

        getLogger().debug("Found Application - {}", application);

        return application;
    }

    @Override
    public void unSoftDeleteApplication(Application application) {
        getLogger().info("SoftDeleting user - {}", application);
        try {
            String oldDn = application.getUniqueId();
            String newRdn = new LdapDnBuilder("").addAttribute(ATTR_CLIENT_ID,
                    application.getClientId()).build();
            String newDn = new LdapDnBuilder(APPLICATIONS_BASE_DN)
                    .addAttribute(ATTR_CLIENT_ID, application.getClientId()).build();
            // Modify the User
            getAppInterface().modifyDN(oldDn, newRdn, false, APPLICATIONS_BASE_DN);
            application.setUniqueId(newDn);
            // Enabled the User
            getAppInterface().modify(application.getUniqueId(), new Modification(
                    ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(true).toUpperCase()));
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting application", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("SoftDeleted application - {}", application);
    }

    public void setCryptHelper(CryptHelper cryptHelper) {
        this.cryptHelper = cryptHelper;
    }
}
