package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.RoleStatus;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapRoleRepository extends LdapRepository implements RoleDao {

    public LdapRoleRepository(LdapConnectionPools connPools,
        Configuration config, Logger logger) {
        super(connPools, config, logger);
    }

    public void add(Role role, String customerUniqueId) {
        getLogger().info("Adding role {}", role);
        if (role == null) {
            getLogger().error("Null instance of role was passed");
            throw new IllegalArgumentException(
                "Null instance of role was passed.");
        }

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_ROLE_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(role.getName())) {
            atts.add(new Attribute(ATTR_NAME, role.getName()));
        }
        if (!StringUtils.isBlank(role.getType())) {
            atts.add(new Attribute(ATTR_GROUP_TYPE, role.getType()));
        }
        if (!StringUtils.isBlank(role.getIname())) {
            atts.add(new Attribute(ATTR_INAME, role.getIname()));
        }
        if (!StringUtils.isBlank(role.getInum())) {
            atts.add(new Attribute(ATTR_INUM, role.getInum()));
        }
        if (!StringUtils.isBlank(role.getOrgInum())) {
            atts.add(new Attribute(ATTR_O, role.getOrgInum()));
        }
        if (!StringUtils.isBlank(role.getOwner())) {
            atts.add(new Attribute(ATTR_OWNER, role.getOwner()));
        }
        if (!StringUtils.isBlank(role.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, role
                .getCustomerId()));
        }
        if (role.getStatus() != null) {
            atts.add(new Attribute(ATTR_STATUS, role.getStatus().toString()));
        }
        if (!StringUtils.isBlank(role.getSeeAlso())) {
            atts.add(new Attribute(ATTR_SEE_ALSO, role.getSeeAlso()));
        }

        if (role.getPermissions() != null && role.getPermissions().size() > 0) {
            List<String> permissions = new ArrayList<String>();
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission.getValue());
            }
            String[] perms = permissions
                .toArray(new String[permissions.size()]);
            atts.add(new Attribute(ATTR_PERMISSION, perms));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        LDAPResult result;

        String roleDN = new LdapDnBuilder().setBaseDn(customerUniqueId)
            .addAttriubte(ATTR_INUM, role.getInum())
            .addAttriubte(ATTR_OU, "groups").build();

        role.setUniqueId(roleDN);

        try {
            result = getAppConnPool().add(roleDN, attributes);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding role {} - {}", role, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding role {} - {}", role.getName(),
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding role: %s - %s",
                role.getName(), result.getResultCode().toString()));
        }

        getLogger().debug("Added role {}", role);
    }

    public void addUserToRole(String userDN, String roleDN) {

        getLogger().info("Adding user {} to {}", userDN, roleDN);

        if (StringUtils.isBlank(userDN)) {
            getLogger().error("Blank userDN was passed");
            throw new IllegalArgumentException("Blank userDN was passed");
        }

        if (StringUtils.isBlank(roleDN)) {
            getLogger().error("Blank roleDN was passed");
            throw new IllegalArgumentException("Blank roleDN was passed");
        }

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.ADD, ATTR_MEMBER, userDN));

        LDAPResult result;
        try {
            result = getAppConnPool().modify(roleDN, mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding user to role {} - {}", roleDN,
                ldapEx);

            if (ldapEx.getResultCode().equals(
                ResultCode.ATTRIBUTE_OR_VALUE_EXISTS)) {
                throw new DuplicateException("User already has role");
            }
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding user to role: %s - %s",
                roleDN, result.getResultCode().toString()));
        }

        getLogger().info("Added user {} to role {}", userDN, roleDN);
    }

    public void delete(String name, String customerId) {
        // TODO Auto-generated method stub

    }

    public void deleteUserFromRole(String userDN, String roleDN) {
        getLogger().info("Deleting user {} to {}", userDN, roleDN);

        if (StringUtils.isBlank(userDN)) {
            getLogger().error("Blank userDN was passed");
            throw new IllegalArgumentException("Blank userDN was passed");
        }

        if (StringUtils.isBlank(roleDN)) {
            getLogger().error("Blank roleDN was passed");
            throw new IllegalArgumentException("Blank roleDN was passed");
        }

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.DELETE, ATTR_MEMBER, userDN));

        LDAPResult result;
        try {
            result = getAppConnPool().modify(roleDN, mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error deleting user from role {} - {}", roleDN,
                ldapEx);
            if (ldapEx.getResultCode().equals(ResultCode.NO_SUCH_ATTRIBUTE)) {
                throw new NotFoundException("User doesn't have role");
            }
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting user from role {} - {}", roleDN,
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting user from role: %s - %s",
                roleDN, result.getResultCode().toString()));
        }

        getLogger().info("Deleted user {} from role {}", userDN, roleDN);
    }

    public Role findByInum(String inum) {
        getLogger().debug("Doing search for inum " + inum);
        if (StringUtils.isBlank(inum)) {
            getLogger().error("Null or Empty inum parameter");
            throw new IllegalArgumentException("Null or Empty inum parameter.");
        }

        Role role = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_INUM, inum).build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for inum {} - {}", inum, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            role = getRole(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger()
                .error("More than one entry was found for inum {}", inum);
            throw new IllegalStateException(
                "More than one entry was found for this inum");
        }

        getLogger().debug("Found Role - {}", role);

        return role;
    }

    public Role findByRoleNameAndCustomerId(String roleName, String customerId) {
        getLogger().debug("Doing search for role {}:{} ", roleName, customerId);
        if (StringUtils.isBlank(roleName)) {
            getLogger().error("Null or Empty roleName parameter");
            throw new IllegalArgumentException(
                "Null or Empty roleName parameter.");
        }
        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        Role role = null;

        SearchResult searchResult = getRoleSearchResult(roleName, customerId);

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            role = getRole(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for role {}:{}",
                roleName, customerId);
            throw new IllegalStateException(
                "More than one entry was found for this role");
        }

        getLogger().debug("Found Role - {}", role);

        return role;
    }

    public void save(Role role) {
        // TODO Auto-generated method stub

    }

    private Role getRole(SearchResultEntry resultEntry) {

        Role role = new Role();

        role.setUniqueId(resultEntry.getDN());
        role.setName(resultEntry.getAttributeValue(ATTR_NAME));
        role.setCountry(resultEntry.getAttributeValue(ATTR_C));
        role.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        role.setIname(resultEntry.getAttributeValue(ATTR_INAME));
        role.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        role.setOrgInum(resultEntry.getAttributeValue(ATTR_O));
        role.setOwner(resultEntry.getAttributeValue(ATTR_OWNER));
        role.setSeeAlso(resultEntry.getAttributeValue(ATTR_SEE_ALSO));
        role.setType(resultEntry.getAttributeValue(ATTR_GROUP_TYPE));

        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            role.setStatus(Enum.valueOf(RoleStatus.class,
                statusStr.toUpperCase()));
        }

        return role;
    }

    private SearchResult getRoleSearchResult(String roleName, String customerId) {
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, roleName)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEGROUP)
            .build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for role {} = {}", roleName,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
    }

    public List<Role> findByCustomerId(String customerId) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        List<Role> roles = new ArrayList<Role>();
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEGROUP)
            .build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error(
                "Error searching for Roles for CustomerId {} - {}", customerId,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                roles.add(getRole(entry));
            }
        }

        getLogger().debug("Found roles {} for customer {}", roles, customerId);

        return roles;
    }

    public String getUnusedRoleInum(String customerInum) {
        if (StringUtils.isBlank(customerInum)) {
            getLogger().error("Null or empty customerInum value passesed in.");
            throw new IllegalArgumentException(
                "Null or empty customerInum value passesed in.");
        }

        Role role = null;
        String inum = "";
        do {
            inum = customerInum + InumHelper.getRandomInum(1);
            role = findByInum(inum);
        } while (role != null);

        return inum;
    }

    public Role findRoleByUniqueId(String uniqueId) {
        Role role = null;
        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEGROUP).build();

        try {
            searchResult = getAppConnPool().search(uniqueId, SearchScope.BASE,
                searchFilter);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            if (e.getObjectClassAttribute()
                .hasValue(OBJECTCLASS_RACKSPACEGROUP)) {
                role = getRole(e);
            }
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format(
                "More than one entry was found for client search - %s",
                uniqueId);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        getLogger().debug("Found Role - {}", role);

        return role;
    }
}
