package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapClientRepository extends LdapRepository implements ClientDao {

    private static final String ATTR_OBJECT_CLASS = "objectClass";
    private static final String[] ATTR_OBJECT_CLASS_VALUES = {"top",
        "rackspaceApplication"};
    private static final String[] ATTR_PERMISSION_OBJECT_CLASS_VALUES = {"top",
        "clientPermission"};
    private static final String ATTR_BLOB = "blob";
    private static final String ATTR_CLIENT_ID = "rackspaceApiKey";
    private static final String ATTR_CLIENT_SECRET = "userPassword";
    private static final String ATTR_DISPLAY_NAME = "displayName";
    private static final String ATTR_INAME = "iname";
    private static final String ATTR_INUM = "inum";
    private static final String ATTR_LOCKED = "locked";
    private static final String ATTR_NAME = "cn";
    private static final String ATTR_PERMISSION = "permission";
    private static final String ATTR_PERMISSION_TYPE = "permissionType";
    private static final String ATTR_RACKSPACE_CUSTOMER_NUMBER = "rackspaceCustomerNumber";
    private static final String ATTR_STATUS = "status";
    private static final String ATTR_SEE_ALSO = "seeAlso";
    private static final String ATTR_SOFT_DELETED = "softDeleted";
    private static final String ATTR_OWNER = "owner";

    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";

    private static final String BASE_DN = "o=rackspace,dc=rackspace,dc=com";

    private static final String CLIENT_ADD_DN_STRING = "inum=%s,ou=applications,o=%s,"
        + BASE_DN;
    private static final String CLIENT_FIND_ALL_STRING_NOT_DELETED = "(&(objectClass=rackspaceApplication)(softDeleted=FALSE))";
    private static final String CLIENT_FIND_BY_NAME_STRING_NOT_DELETED = "(&(objectClass=rackspaceApplication)(displayName=%s)(softDeleted=FALSE))";
    private static final String CLIENT_FIND_BY_INUM_STRING = "(&(objectClass=rackspaceApplication)(inum=%s))";
    private static final String CLIENT_FIND_BY_CLIENTID_STRING_NOT_DELETED = "(&(objectClass=rackspaceApplication)(rackspaceApiKey=%s)(softDeleted=FALSE))";
    private static final String CLIENT_FIND_BY_CUSTOMERID_STRING_NOT_DELETED = "(&(objectClass=rackspaceApplication)(rackspaceCustomerNumber=%s)(softDeleted=FALSE))";

    private static final String PERMISSION_FIND_BY_CLIENTID = "(objectClass=clientPermission)";
    private static final String PERMISSION_FIND_BY_ID = "(&(cn=%s)(objectClass=clientPermission))";

    public LdapClientRepository(LdapConnectionPools connPools, Logger logger) {
        super(connPools, logger);
    }

    public void addDefinedPermission(Permission permission) {
        getLogger().info("Adding Permission {}", permission);
        if (permission == null) {
            getLogger().error("Null instance of Permission was passed");
            throw new IllegalArgumentException(
                "Null instance of Permission was passed.");
        }

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

        String clientDN = this.getClientDnByClientId(permission.getClientId());

        String permissionDN = "cn=" + permission.getPermissionId() + ","
            + clientDN;

        permission.setUniqueId(permissionDN);

        LDAPResult result;

        try {
            result = getAppConnPool().add(permissionDN, atts);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding permission {} - {}", permission,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding permission {} - {}",
                permission.getPermissionId(), result.getResultCode());
            throw new IllegalStateException(
                String.format(
                    "LDAP error encountered when adding permission: %s - %s",
                    permission.getPermissionId(), result.getResultCode()
                        .toString()));
        }

        getLogger().debug("Added permission {}", permission);
    }

    public void add(Client client) {
        getLogger().info("Adding client {}", client);
        if (client == null) {
            getLogger().error("Null instance of Client was passed");
            throw new IllegalArgumentException(
                "Null instance of Client was passed.");
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(client.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, client.getClientId()));
        }

        if (!StringUtils.isBlank(client.getName())) {
            atts.add(new Attribute(ATTR_DISPLAY_NAME, client.getName()));
        }

        if (!StringUtils.isBlank(client.getIname())) {
            atts.add(new Attribute(ATTR_INAME, client.getIname()));
        }

        if (!StringUtils.isBlank(client.getInum())) {
            atts.add(new Attribute(ATTR_INUM, client.getInum()));
        }

        if (!StringUtils.isBlank(client.getOwner())) {
            atts.add(new Attribute(ATTR_OWNER, client.getOwner()));
        }

        if (!StringUtils.isBlank(client.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, client
                .getCustomerId()));
        }

        if (client.getStatus() != null) {
            atts.add(new Attribute(ATTR_STATUS, client.getStatus().toString()));
        }

        if (!StringUtils.isBlank(client.getSeeAlso())) {
            atts.add(new Attribute(ATTR_SEE_ALSO, client.getSeeAlso()));
        }

        if (!StringUtils.isBlank(client.getClientSecretObj().getValue())) {
            atts.add(new Attribute(ATTR_CLIENT_SECRET, client.getClientSecret()));
        }

        if (client.getIsLocked() != null) {
            atts.add(new Attribute(ATTR_LOCKED, String.valueOf(client
                .getIsLocked())));
        }

        if (client.getSoftDeleted() != null) {
            atts.add(new Attribute(GlobalConstants.ATTR_SOFT_DELETED, String
                .valueOf(client.getSoftDeleted())));
        }

        if (client.getPermissions() != null
            && client.getPermissions().size() > 0) {
            List<String> permissions = new ArrayList<String>();
            for (Permission permission : client.getPermissions()) {
                String p = permission.getPermissionLDAPserialization();
                if (p != null) {
                    permissions.add(p);
                }
            }
            String[] perms = permissions
                .toArray(new String[permissions.size()]);
            if (perms.length > 0) {
                atts.add(new Attribute(ATTR_PERMISSION, perms));
            }
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        LDAPResult result;

        String clientDN = String.format(CLIENT_ADD_DN_STRING, client.getInum(),
            client.getOwner().replace(GlobalConstants.INUM_PREFIX, ""));

        client.setUniqueId(clientDN);

        try {
            result = getAppConnPool().add(clientDN, attributes);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding client {} - {}", client, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding client {} - {}",
                client.getClientId(), result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding client: %s - %s",
                client.getName(), result.getResultCode().toString()));
        }

        getLogger().debug("Added client {}", client);
    }

    public void delete(String clientId) {
        getLogger().info("Deleting client {}", clientId);
        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId paramter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().delete(getClientDnByClientId(clientId));
        } catch (LDAPException ldapEx) {
            getLogger().error("Could not perform delete for client {} - {}",
                clientId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting client {} - {}", clientId,
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting client: %s - %s",
                clientId, result.getResultCode().toString()));
        }
        getLogger().info("Deleted client {}", clientId);
    }

    public void deleteDefinedPermission(Permission permission) {
        getLogger().info("Deleting permission {}", permission);
        if (permission == null) {
            getLogger().error("Null or Empty permission paramter");
            throw new IllegalArgumentException(
                "Null or Empty permission parameter.");
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().delete(
                this.getDefinedPermissionByClientIdAndPermissionId(
                    permission.getClientId(), permission.getPermissionId())
                    .getUniqueId());
        } catch (LDAPException ldapEx) {
            getLogger().error(
                "Could not perform delete for permission {} - {}", permission,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting permission {} - {}", permission,
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting permission: %s - %s",
                permission, result.getResultCode().toString()));
        }
        getLogger().info("Deleted permission {}", permission);
    }

    public List<Client> findAll() {
        getLogger().debug("Search all clients");
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                CLIENT_FIND_ALL_STRING_NOT_DELETED);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error(
                "Error searching for all clients under DN {} - {}", BASE_DN,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        List<Client> clients = new ArrayList<Client>();
        for (SearchResultEntry e : searchResult.getSearchEntries()) {
            Client client = getClient(e);
            clients.add(client);
        }

        getLogger().debug("Found {} clients under DN {}", clients.size(),
            BASE_DN);
        return clients;
    }

    public Client findByClientname(String clientName) {
        getLogger().debug("Searching for client {}", clientName);

        if (StringUtils.isBlank(clientName)) {
            getLogger().error("Null or Empty client name parameter");
            throw new IllegalArgumentException(
                "Null or Empty client name parameter.");
        }

        Client client = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                String.format(CLIENT_FIND_BY_NAME_STRING_NOT_DELETED,
                    clientName));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for client {} - {}", clientName,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            client = getClient(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for client {}",
                clientName);
            throw new IllegalStateException(
                "More than one entry was found for this client name");
        }

        getLogger().debug("Found client - {}", client);

        return client;
    }

    public Client findByClientId(String clientId) {
        getLogger().debug("Doing search for clientId {}", clientId);

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        Client client = null;
        SearchResult searchResult = getClientSearchResult(clientId);

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            client = getClient(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for clientId {}",
                clientId);
            throw new IllegalStateException(
                "More than one entry was found for this clientId");
        }

        getLogger().debug("Found client - {}", client);

        return client;
    }

    public Client findByInum(String inum) {
        getLogger().debug("Doing search for Inum {}", inum);

        if (StringUtils.isBlank(inum)) {
            getLogger().error("Null or Empty Inum parameter");
            throw new IllegalArgumentException("Null or Empty Inum parameter.");
        }

        Client client = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(CLIENT_FIND_BY_INUM_STRING, inum));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for Inum {} - {}", inum, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            client = getClient(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger()
                .error("More than one entry was found for Inum {}", inum);
            throw new IllegalStateException(
                "More than one entry was found for this Inum");
        }

        getLogger().debug("Found client - {}", client);

        return client;
    }

    public void save(Client client) {
        getLogger().debug("Updating client {}", client);
        if (client == null || StringUtils.isBlank(client.getClientId())) {
            getLogger().error(
                "Client instance is null or its clientId has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The Client instance either null or its clientName has no value.");
        }
        Client oldClient = findByClientId(client.getClientId());

        if (oldClient == null) {
            getLogger().error("No record found for client {}",
                client.getClientId());
            throw new IllegalArgumentException(
                "There is no exisiting record for the given client instance.");
        }

        List<Modification> mods = getModifications(oldClient, client);

        if (client.equals(oldClient) || mods.size() < 1) {
            // No changes!
            return;
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(
                getClientDnByClientId(client.getClientId()), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating client {} - {}", client, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating client {} - {}",
                client.getClientId(), result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating client: %s - %s",
                client.getName(), result.getResultCode().toString()));
        }

        getLogger().debug("Updated client {}", client.getName());
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
        try {
            result = getAppConnPool().modify(oldPermission.getUniqueId(),
                getPermissionModifications(oldPermission, permission));
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating permission {} - {}", permission,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating permission {} - {}", permission,
                result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating permission: %s - %s",
                permission, result.getResultCode().toString()));
        }

        getLogger().debug("Updated permission {}", permission);
    }

    public void setAllClientLocked(String customerId, boolean locked) {
        List<Client> clients = this.getByCustomerId(customerId);
        for (Client client : clients) {
            client.setIsLocked(locked);
            this.save(client);
        }
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

        if (cNew.getIsLocked() != null
            && cNew.getIsLocked() != cOld.getIsLocked()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED,
                String.valueOf(cNew.getIsLocked())));
        }

        if (cNew.getStatus() != null
            && !cOld.getStatus().equals(cNew.getStatus())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_STATUS,
                cNew.getStatus().toString()));
        }

        if (cNew.getSeeAlso() != null) {
            if (StringUtils.isBlank(cNew.getSeeAlso())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_SEE_ALSO));
            } else if (!StringUtils
                .equals(cOld.getSeeAlso(), cNew.getSeeAlso())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_SEE_ALSO, cNew.getSeeAlso()));
            }
        }

        if (cNew.getOwner() != null) {
            if (StringUtils.isBlank(cNew.getOwner())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_OWNER));
            } else if (!StringUtils.equals(cOld.getOwner(), cNew.getOwner())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_OWNER,
                    cNew.getOwner()));
            }
        }

        if (cNew.getSoftDeleted() != null
            && cNew.getSoftDeleted() != cOld.getSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE,
                GlobalConstants.ATTR_SOFT_DELETED, String.valueOf(cNew
                    .getSoftDeleted())));
        }

        if (cNew.getPermissions() != null
            && !cNew.getPermissions().equals(cOld.getPermissions())) {
            if (cNew.getPermissions().size() == 0) {
                // If the new list of permissions has zero permissions but the
                // old list of permissions is greater than 0. Then we need to
                // add
                // a modification to delete the permissions
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_PERMISSION));
            } else {
                // If the new list of permissions has 1 or more permissions then
                // we can just do a replace modification
                List<String> permissions = new ArrayList<String>();
                for (Permission permission : cNew.getPermissions()) {
                    String p = permission.getPermissionLDAPserialization();
                    if (p != null) {
                        permissions.add(p);
                    }
                }
                String[] perms = permissions.toArray(new String[permissions
                    .size()]);
                if (perms.length > 0) {
                    mods.add(new Modification(ModificationType.REPLACE,
                        ATTR_PERMISSION, perms));
                }
            }
        }

        return mods;
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

    private Client getClient(SearchResultEntry resultEntry) {
        Client client = new Client();
        client.setUniqueId(resultEntry.getDN());
        client.setClientId(resultEntry.getAttributeValue(ATTR_CLIENT_ID));
        ClientSecret secret = ClientSecret.existingInstance(resultEntry
            .getAttributeValue(ATTR_CLIENT_SECRET));
        client.setClientSecretObj(secret);
        client.setName(resultEntry.getAttributeValue(ATTR_DISPLAY_NAME));
        client.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        client.setIname(resultEntry.getAttributeValue(ATTR_INAME));

        client.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));

        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            client.setStatus(Enum.valueOf(ClientStatus.class,
                statusStr.toUpperCase()));
        }
        client.setSeeAlso(resultEntry.getAttributeValue(ATTR_SEE_ALSO));
        client.setOwner(resultEntry.getAttributeValue(ATTR_OWNER));

        String deleted = resultEntry.getAttributeValue(ATTR_SOFT_DELETED);
        if (deleted != null) {
            client.setSoftDeleted(resultEntry
                .getAttributeValueAsBoolean(ATTR_SOFT_DELETED));
        }

        String locked = resultEntry.getAttributeValue(ATTR_LOCKED);
        if (locked != null) {
            client.setIsLocked(resultEntry
                .getAttributeValueAsBoolean(ATTR_LOCKED));
        }

        String[] permissions = resultEntry.getAttributeValues(ATTR_PERMISSION);

        List<Permission> perms = new ArrayList<Permission>();

        if (permissions != null && permissions.length > 0) {
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

    public String getClientDnByClientId(String clientId) {
        String dn = null;
        SearchResult searchResult = getClientSearchResult(clientId);
        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            dn = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for clientId {}",
                clientId);
            throw new IllegalStateException(
                "More than one entry was found for this clientId");
        }
        return dn;
    }

    private SearchResult getClientSearchResult(String clientId) {
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                String.format(CLIENT_FIND_BY_CLIENTID_STRING_NOT_DELETED,
                    clientId));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for clientId {} - {}", clientId,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
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

    public boolean authenticate(String clientId, String clientSecret) {
        BindResult result = null;
        try {
            result = getBindConnPool().bind(getClientDnByClientId(clientId),
                clientSecret);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                return false;
            }
            getLogger().error("Error authenticating for clientId {} - {}",
                clientId, e);
            throw new IllegalStateException(CONNECT_ERROR_STRING, e);
        }

        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    public List<Client> getByCustomerId(String customerId) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        List<Client> clients = new ArrayList<Client>();
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                String.format(CLIENT_FIND_BY_CUSTOMERID_STRING_NOT_DELETED,
                    customerId));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for Clients CustomerId {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                clients.add(getClient(entry));
            }
        }

        getLogger().debug("Found clients {} for customer {}", clients,
            customerId);

        return clients;
    }

    public Permission getDefinedPermissionByClientIdAndPermissionId(
        String clientId, String permissionId) {
        String clientDN = this.findByClientId(clientId).getUniqueId();

        Permission permission = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(clientDN, SearchScope.ONE,
                String.format(PERMISSION_FIND_BY_ID, permissionId));
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

        List<Permission> permissions = new ArrayList<Permission>();
        SearchResult searchResult = null;

        String[] Attributes = {ATTR_NAME, ATTR_CLIENT_ID,
            ATTR_RACKSPACE_CUSTOMER_NUMBER};

        try {
            searchResult = getAppConnPool().search(clientDN, SearchScope.ONE,
                PERMISSION_FIND_BY_CLIENTID, Attributes);
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
}
