package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.exceptions.DuplicateClientGroupException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;

public class LdapClientRepository extends LdapRepository implements ClientDao {
    private static final String[] ATTR_GROUP_SEARCH_ATTRIBUTES = {
        ATTR_RACKSPACE_CUSTOMER_NUMBER, ATTR_RACKSPACE_CUSTOMER_NUMBER,
        ATTR_NAME};

    public LdapClientRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    public void add(Client client, String customerUniqueId) {
        getLogger().info("Adding client {}", client);

        if (client == null) {
            String errMsg = "Null instance of Client was passed in.";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        
        Audit audit = Audit.log(client).add();

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

        LDAPResult result;

        String clientDN = new LdapDnBuilder(customerUniqueId)
            .addAttriubte(ATTR_INUM, client.getInum())
            .addAttriubte(ATTR_OU, OU_APPLICATIONS_NAME).build();

        client.setUniqueId(clientDN);

        try {
            result = getAppConnPool().add(clientDN, attributes);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error adding client {} - {}", client, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding client {} - {}",
                client.getClientId(), result.getResultCode());
            audit.fail(result.getResultCode().toString());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding client: %s - %s",
                client.getName(), result.getResultCode().toString()));
        }

        String clientPermissionsDN = new LdapDnBuilder(clientDN).addAttriubte(
            ATTR_OU, OU_PERMISSIONS_NAME).build();

        String clientGroupsDN = new LdapDnBuilder(clientDN).addAttriubte(
            ATTR_OU, OU_GROUPS_NAME).build();

        // Add ou=permissions under new client entry
        Attribute[] permissionAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, OU_PERMISSIONS_NAME)};

        try {
            result = getAppConnPool().add(clientPermissionsDN,
                permissionAttributes);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error adding client permission ou: {} - {}",
                client.getClientId(), ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            String errMsg = String
                .format(
                    "LDAP error encountered when adding client permissions ou: %s - %s",
                    client.getClientId(), result.getResultCode().toString());
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        // Add ou=groups under new client entry
        Attribute[] groupAttributes = {
            new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_OU_VALUES),
            new Attribute(ATTR_OU, OU_GROUPS_NAME)};

        try {
            result = getAppConnPool().add(clientGroupsDN, groupAttributes);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error adding client groups ou: {} - {}",
                client.getClientId(), ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            String errMsg = String.format(
                "LDAP error encountered when adding client groups ou: %s - %s",
                client.getClientId(), result.getResultCode().toString());
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        audit.succeed();

        getLogger().debug("Added client {}", client);
    }

    public void addClientGroup(ClientGroup clientGroup) {
        getLogger().info("Adding ClientGroup {}", clientGroup);

        Audit audit = Audit.log(clientGroup).add();

        if (clientGroup == null) {
            String errMsg = "Null instance of clientGroup was passed";
            audit.fail(errMsg);
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Client client = this.getClient(clientGroup.getCustomerId(),
            clientGroup.getClientId());

        if (client == null) {
            String errMsg = "Client Not Found";
            audit.fail(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientGroup group = this.getClientGroup(clientGroup.getCustomerId(),
            clientGroup.getClientId(), clientGroup.getName());

        if (group != null) {
            String errMsg = "Client Group already exists";
            audit.fail(errMsg);
            throw new DuplicateClientGroupException(errMsg);
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_CLIENT_GROUP_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(clientGroup.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, clientGroup
                .getCustomerId()));
        }
        if (!StringUtils.isBlank(clientGroup.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, clientGroup.getClientId()));
        }
        if (!StringUtils.isBlank(clientGroup.getName())) {
            atts.add(new Attribute(ATTR_NAME, clientGroup.getName()));
        }

        String groupDN = new LdapDnBuilder(client.getUniqueId())
            .addAttriubte(ATTR_NAME, clientGroup.getName())
            .addAttriubte(ATTR_OU, OU_GROUPS_NAME).build();

        clientGroup.setUniqueId(groupDN);

        LDAPResult result;

        try {
            result = getAppConnPool().add(groupDN, atts);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error adding client group {} - {}", clientGroup,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            getLogger().error("Error adding clientGroup {} - {}",
                clientGroup.getName(), result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding clientGroup: %s - %s",
                clientGroup.getName(), result.getResultCode().toString()));
        }

        audit.succeed();

        getLogger().debug("Added clientGroup {}", clientGroup);
    }

    public void addDefinedPermission(Permission permission) {
        getLogger().info("Adding Permission {}", permission);

        if (permission == null) {
            String errMsg = "Null instance of Permission was passed";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        
        Audit audit = Audit.log(permission).add();

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

        Client client = this.getClient(permission.getCustomerId(),
            permission.getClientId());

        if (client == null) {
            String errMsg = "Client Not Found";
            audit.fail(errMsg);
            throw new NotFoundException(errMsg);
        }

        String permissionDN = new LdapDnBuilder(client.getUniqueId())
            .addAttriubte(ATTR_NAME, permission.getPermissionId())
            .addAttriubte(ATTR_OU, OU_PERMISSIONS_NAME).build();

        permission.setUniqueId(permissionDN);

        LDAPResult result;

        try {
            result = getAppConnPool().add(permissionDN, atts);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error adding permission {} - {}", permission,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            getLogger().error("Error adding permission {} - {}",
                permission.getPermissionId(), result.getResultCode());
            throw new IllegalStateException(
                String.format(
                    "LDAP error encountered when adding permission: %s - %s",
                    permission.getPermissionId(), result.getResultCode()
                        .toString()));
        }

        audit.succeed();

        getLogger().debug("Added permission {}", permission);
    }

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

        LDAPResult result;
        Audit audit = Audit.log(group).modify(mods);

        try {
            result = getAppConnPool().modify(group.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding user to group {} - {}", group,
                ldapEx);

            if (ldapEx.getResultCode().equals(
                ResultCode.ATTRIBUTE_OR_VALUE_EXISTS)) {
                throw new DuplicateException("User already in group");
            }

            audit.fail(ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding user to group: %s - %s",
                group, result.getResultCode().toString()));
        }

        audit.succeed();

        getLogger().info("Added user {} to group {}", userUniqueId, group);
    }

    public ClientAuthenticationResult authenticate(String clientId,
        String clientSecret) {
        BindResult result;
        Client client = findByClientId(clientId);
        
        if (client == null) {
            return new ClientAuthenticationResult(null, false);
        }
        
        Audit audit = Audit.authClient(client);
        
        try {
            result = getBindConnPool().bind(client.getUniqueId(),
                clientSecret);
            audit.succeed();
        } catch (LDAPException e) {
            audit.fail(e.getMessage());
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                return new ClientAuthenticationResult(client, false);
            }
            getLogger()
            .error(
                "Bind operation on clientId " + clientId
                    + " failed.", e);
            throw new IllegalStateException(e);
        }

        return new ClientAuthenticationResult(client,
            ResultCode.SUCCESS.equals(result.getResultCode()));
    }

    public void delete(String clientId) {
        getLogger().info("Deleting client {}", clientId);
        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId paramter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        Client client = this.findByClientId(clientId);
        String clientDN = client.getUniqueId();

        LDAPResult result = null;
        Audit audit = Audit.log(client).delete();
        try {
            DeleteRequest request = new DeleteRequest(clientDN);
            request.addControl(new SubtreeDeleteRequestControl());
            result = getAppConnPool().delete(request);

        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Could not perform delete for client {} - {}",
                clientId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error deleting client {} - {}", clientId,
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting client: %s - %s",
                clientId, result.getResultCode().toString()));
        }

        audit.succeed();
        getLogger().info("Deleted client {}", clientId);
    }

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

        LDAPResult result = null;
        Audit audit = Audit.log(group).delete();
        try {
            result = getAppConnPool().delete(group.getUniqueId());
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            throw new IllegalStateException();
        }
        audit.succeed();
        getLogger().info("Deleted clientGroup {}", group);
    }

    public void deleteDefinedPermission(Permission permission) {
        getLogger().info("Deleting permission {}", permission);
        if (permission == null) {
            getLogger().error("Null or Empty permission paramter");
            throw new IllegalArgumentException(
                "Null or Empty permission parameter.");
        }

        LDAPResult result = null;
        Audit audit = Audit.log(permission).delete();
        try {
            result = getAppConnPool().delete(
                this.getDefinedPermissionByClientIdAndPermissionId(
                    permission.getClientId(), permission.getPermissionId())
                    .getUniqueId());
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error(
                "Could not perform delete for permission {} - {}", permission,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            getLogger().error("Error deleting permission {} - {}", permission,
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting permission: %s - %s",
                permission, result.getResultCode().toString()));
        }
        audit.succeed();
        getLogger().info("Deleted permission {}", permission);
    }

    public List<Client> findAll() {
        getLogger().debug("Search all clients");

        String searchFilter = new LdapSearchBuilder()
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

    public Client findByClientId(String clientId) {
        getLogger().debug("Doing search for clientId {}", clientId);

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    public Client getClient(String customerId, String clientId) {
        getLogger().debug("Doing search for clientId {}", clientId);

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    public Client findByClientname(String clientName) {
        getLogger().debug("Searching for client {}", clientName);

        if (StringUtils.isBlank(clientName)) {
            getLogger().error("Null or Empty client name parameter");
            throw new IllegalArgumentException(
                "Null or Empty client name parameter.");
        }

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, clientName)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    public ClientGroup findClientGroupByUniqueId(String uniqueId) {
        ClientGroup group = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTGROUP).build();

        try {
            searchResult = getAppConnPool().search(uniqueId, SearchScope.BASE,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            if (e.getObjectClassAttribute().hasValue(OBJECTCLASS_CLIENTGROUP)) {
                group = getClientGroup(e);
            }
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format(
                "More than one entry was found for client search - %s",
                uniqueId);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        getLogger().debug("Found Client Group - {}", group);

        return group;
    }

    public Client findByInum(String inum) {
        getLogger().debug("Doing search for Inum {}", inum);

        if (StringUtils.isBlank(inum)) {
            getLogger().error("Null or Empty Inum parameter");
            throw new IllegalArgumentException("Null or Empty Inum parameter.");
        }

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_INUM, inum)
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Client client = getSingleClient(searchFilter);

        getLogger().debug("Found client - {}", client);

        return client;
    }

    public Clients getByCustomerId(String customerId, int offset, int limit) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();
        Clients clients = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Found clients {} for customer {}", clients,
            customerId);

        return clients;
    }

    public ClientGroup getClientGroup(String customerId, String clientId,
        String groupName) {

        ClientGroup group = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, groupName)
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTGROUP)
            .build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                searchFilter, ATTR_GROUP_SEARCH_ATTRIBUTES);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for clientGroup {} - {}",
                groupName, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            group = getClientGroup(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for group {}",
                clientId);
            throw new IllegalStateException(
                "More than one entry was found for this group");
        }

        return group;
    }

    public List<ClientGroup> getClientGroupsByClientId(String clientId) {

        List<ClientGroup> groups = new ArrayList<ClientGroup>();

        Client client = this.findByClientId(clientId);

        if (client == null) {
            throw new NotFoundException();
        }

        String searchDN = "ou=groups," + client.getUniqueId();

        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTGROUP).build();

        try {
            searchResult = getAppConnPool().search(searchDN, SearchScope.ONE,
                searchFilter, ATTR_GROUP_SEARCH_ATTRIBUTES);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for clientId {} - {}", clientId,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                groups.add(getClientGroup(entry));
            }
        }
        return groups;
    }

    public Permission getDefinedPermissionByClientIdAndPermissionId(
        String clientId, String permissionId) {
        String clientDN = this.findByClientId(clientId).getUniqueId();

        String searchDN = "ou=permissions," + clientDN;

        Permission permission = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, permissionId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTPERMISSION)
            .build();

        try {
            searchResult = getAppConnPool().search(searchDN, SearchScope.ONE,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for permissionId {} - {}",
                clientId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            permission = getPermission(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error(
                "More than one entry was found for permissionId {}", clientId);
            throw new IllegalStateException(
                "More than one entry was found for this permissionId");
        }

        return permission;
    }

    public List<Permission> getDefinedPermissionsByClientId(String clientId) {
        String clientDN = this.findByClientId(clientId).getUniqueId();

        String searchDN = "ou=permissions," + clientDN;

        List<Permission> permissions = new ArrayList<Permission>();
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTPERMISSION).build();

        try {
            searchResult = getAppConnPool().search(searchDN, SearchScope.ONE,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for clientId {} - {}", clientId,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                permissions.add(getPermission(entry));
            }
        }
        return permissions;
    }

    public String getUnusedClientInum(String customerInum) {
        // TODO: We might may this call to the XDI server in the future.
        Client client = null;
        String inum = "";
        do {
            inum = customerInum + InumHelper.getRandomInum(1);
            client = findByInum(inum);
        } while (client != null);

        return inum;
    }

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

        LDAPResult result;
        Audit audit = Audit.log(group).modify(mods);
        try {
            result = getAppConnPool().modify(group.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            audit.fail(ldapEx.getMessage());
            getLogger().error("Error deleting user from group {} - {}", group,
                ldapEx);
            if (ldapEx.getResultCode().equals(ResultCode.NO_SUCH_ATTRIBUTE)) {
                throw new NotFoundException("User isn't in group");
            }
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail(result.getResultCode().toString());
            getLogger().error("Error removing user from group {} - {}", group,
                result.getResultCode());
            throw new IllegalStateException(
                String
                    .format(
                        "LDAP error encountered when removing user from group: %s - %s",
                        group, result.getResultCode().toString()));
        }
        audit.succeed();
        getLogger().info("Removed user {} from group {}", userUniqueId, group);
    }

    public void save(Client client) {
        getLogger().debug("Updating client {}", client);

        if (client == null || StringUtils.isBlank(client.getClientId())) {
            getLogger().error(
                "Client instance is null or its clientId has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The Client instance either null or its clientName has no value.");
        }
        String clientId = client.getClientId();
        Client oldClient = findByClientId(clientId);

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

        LDAPResult result = null;
        Audit audit = Audit.log(client).modify(mods);
        try {
            result = getAppConnPool().modify(oldClient.getUniqueId(),
                mods);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error updating client {} - {}", client, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error updating client {} - {}", clientId,
                result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating client: %s - %s",
                client.getName(), result.getResultCode().toString()));
        }

        audit.succeed();
        getLogger().debug("Updated client {}", client.getName());
    }

    public void setAllClientLocked(String customerId, boolean locked) {
        Clients clients = this.findFirst100ByCustomerIdAndLock(customerId,
            !locked);
        if (clients.getClients() != null && clients.getClients().size() > 0) {
            for (Client client : clients.getClients()) {
                client.setLocked(locked);
                this.save(client);
            }
        }
        if (clients.getTotalRecords() > 0) {
            this.setAllClientLocked(customerId, locked);
        }
    }

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

        LDAPResult result = null;
        Audit audit = Audit.log(oldPermission).modify();
        try {
            result = getAppConnPool().modify(oldPermission.getUniqueId(),
                getPermissionModifications(oldPermission, permission));
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error updating permission {} - {}", permission,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            audit.fail();
            getLogger().error("Error updating permission {} - {}", permission,
                result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating permission: %s - %s",
                permission, result.getResultCode().toString()));
        }
        audit.succeed();

        getLogger().debug("Updated permission {}", permission);
    }

    private Clients findFirst100ByCustomerIdAndLock(String customerId,
        boolean isLocked) {
        getLogger().debug("Doing search for customerId {}", customerId);

        int limit = 100;
        int offset = 0;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_LOCKED, String.valueOf(isLocked))
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEAPPLICATION).build();

        Clients clients = getMultipleClients(searchFilter, offset, limit);

        getLogger().debug("Found Users - {}", clients);

        return clients;
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
        return clientGroup;
    }

    private Clients getMultipleClients(String searchFilter, int offset,
        int limit) {

        ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(
            new SortKey(ATTR_NAME));

        offset = offset < 0 ? this.getLdapPagingOffsetDefault() : offset;
        limit = limit <= 0 ? this.getLdapPagingLimitDefault() : limit;
        limit = limit > this.getLdapPagingLimitMax() ? this
            .getLdapPagingLimitMax() : limit;

        // In the constructor below we're adding one to the offset because the
        // Rackspace API standard calls for a 0 based offset while LDAP uses a
        // 1 based offset.
        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(
            offset + 1, 0, limit - 1, 0, null);

        int contentCount = 0;

        List<Client> clientList = new ArrayList<Client>();
        SearchResult searchResult = null;
        try {

            SearchRequest request = new SearchRequest(BASE_DN, SearchScope.SUB,
                searchFilter);

            request.setControls(new Control[]{sortRequest, vlvRequest});
            searchResult = getAppConnPool().search(request);

            for (Control c : searchResult.getResponseControls()) {
                if (c instanceof VirtualListViewResponseControl) {
                    VirtualListViewResponseControl vlvResponse = (VirtualListViewResponseControl) c;
                    contentCount = vlvResponse.getContentCount();
                }
            }

        } catch (LDAPException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
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

    private Client getSingleClient(String searchFilter) {
        Client client = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            client = getClient(e);
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format(
                "More than one entry was found for client search - %s",
                searchFilter);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
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
