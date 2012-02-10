package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 1/31/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapGroupRepository extends LdapRepository implements GroupDao {

    public LdapGroupRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
    }

    @Override
    public List<Group> getGroups(String marker, Integer limit) {
        getLogger().debug("Getting groups");

        List<Group> groups = new ArrayList<Group>();
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLOUDGROUP).build();

        try {
            searchResult = getAppConnPool().search(GROUP_BASE_DN, SearchScope.ONE, searchFilter);
            getLogger().info("Got groups");
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for baseUrls - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                groups.add(getGroup(entry));
            }
        }

        return groups;
    }

    @Override
    public Group getGroupById(int groupId) {
        getLogger().debug("Get group by Id {}", groupId);
        Group group = null;
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, String.valueOf(groupId))
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLOUDGROUP).build();

        try {
            searchResult = getAppConnPool().search(GROUP_BASE_DN, SearchScope.ONE, searchFilter);
            getLogger().info("Got group by Id {}", groupId);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for group - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            group = getGroup(e);
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format("More than one entry was found for group - %s", groupId);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        } else{
            String errMsg = String.format("Group %s not found", groupId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);        
        }

        getLogger().debug("Get group by Id {}", groupId);
        return group;
    }

    @Override
    public Group getGroupByName(String groupName){
        getLogger().debug("Get group by Name {}", groupName);
        Group group = null;
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_GROUP_NAME,groupName).build();

        try{
            searchResult = getAppConnPool().search(GROUP_BASE_DN, SearchScope.ONE, searchFilter);
            getLogger().info("Got group by Name {}", groupName);
        } catch(LDAPSearchException ldapEx){
            getLogger().error("Error searching for group - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            group = getGroup(e);
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format("More than one entry was found for group - %s", groupName);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        getLogger().debug("Get group by Name {}", groupName);
        return group;
    }

    @Override
    public void addGroup(Group group) {

        if (group == null) {
            getLogger().error("Null instance of group was passed");
            throw new IllegalArgumentException("Null instance of group was passed.");
        }

        String groupDN = new LdapDnBuilder(GROUP_BASE_DN).addAttribute(ATTR_ID, group.getGroupId().toString()).build();

        group.setUniqueId(groupDN);

        Audit audit = Audit.log(group).add();

        Attribute[] attributes = null;

        try {
            attributes = getAddAttributes(group);
            addEntry(groupDN, attributes, audit);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        }
        audit.succeed();

        getLogger().debug("Added group {}", group);
    }

    @Override
    public void updateGroup(Group group) {
        Group oldGroup = getGroupById(group.getGroupId());
        getLogger().debug("Found existing group {}", oldGroup);

        Audit audit = Audit.log(group);

        try {
            List<Modification> mods = getModifications(oldGroup, group);
            audit.modify(mods);

            if (mods.size() < 1) {
                // No changes!
                return;
            }
            getAppConnPool().modify(oldGroup.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating group {} - {}", group.getName(), ldapEx);
            audit.fail("Error updating user");
            throw new IllegalStateException(ldapEx);
        }
        audit.succeed();
        getLogger().info("Updated group - {}", group);
    }

    @Override
    public void deleteGroup(int groupId) {
        getLogger().debug("Deleting group - {}", groupId);

        LDAPResult result = null;

        String baseUrlDN = new LdapDnBuilder(GROUP_BASE_DN).addAttribute(
                ATTR_ID, String.valueOf(groupId)).build();

        try {
            result = getAppConnPool().delete(String.format(baseUrlDN, groupId));
            getLogger().info("Deleted group - {}", groupId);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error deleting group {} - {}", groupId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting group {} - {}", groupId, result.getResultCode());
            throw new IllegalStateException(String.format("LDAP error encountered when deleting group: %s - %s",
                    groupId, result.getResultCode().toString()));
        }

        getLogger().debug("Deleted group - {}", groupId);
    }

    @Override
    public String getNextGroupId() {
        String groupId = null;
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            groupId = getNextId(conn, NEXT_GROUP_ID);
        } catch (LDAPException e) {
            getLogger().error("Error getting next groupId", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return groupId;
    }

    @Override
    public void addGroupToUser(int groupId, String userId) {
        getLogger().debug("Adding group {} to user {}", groupId, userId);
        Group group = this.getGroupById(groupId);
        if (group == null) {
            String errMsg = String.format("Group %s not found", groupId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        String userDN = new LdapDnBuilder(USERS_BASE_DN).addAttribute(ATTR_ID, userId).build();
        List<Group> oldGroups = this.getGroupsForUser(userId);

        List<String> groups = new ArrayList<String>();

        for (Group s : oldGroups) {
            groups.add(s.getGroupId().toString());
        }

        groups.add(group.getGroupId().toString());

        List<Modification> mods = new ArrayList<Modification>();

        String[] groupList = groups.toArray(new String[groups.size()]);

        mods.add(new Modification(ModificationType.REPLACE, ATTR_GROUP_ID, groupList));

        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(userDN, mods);
            getLogger().info("Added group {} to user {}", groupId, userId);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating user {} endpoints - {}", userId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating user {} groups - {}", groups, result.getResultCode());
            throw new IllegalArgumentException(
                    String.format("LDAP error encountered when updating user %s groups - %s", groups, result.getResultCode().toString()));
        }

        getLogger().debug("Adding groupId {} to user {}", groupId, userId);
    }

    @Override
    public void deleteGroupFromUser(int groupId, String userId) {
        getLogger().debug("Removing group {} from user {}", groupId, userId);
        Group group = this.getGroupById(groupId);
        if (group == null) {
            String errMsg = String.format("Group %s not found", groupId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        String userDN = new LdapDnBuilder(USERS_BASE_DN).addAttribute(ATTR_ID, userId).build();
        List<Group> oldGroups = this.getGroupsForUser(userId);

        List<String> groups = new ArrayList<String>();

        for (Group s : oldGroups) {
            if (!s.getGroupId().equals(groupId)) {
                groups.add(s.getGroupId().toString());
            }
        }

        List<Modification> mods = new ArrayList<Modification>();

        if (groups.size() < 1) {
            // If a user's last group has been removed we need to delete
            // the attribute from LDAP
            mods.add(new Modification(ModificationType.DELETE, ATTR_GROUP_ID));
        } else {
            // Else we'll just replace all the values for endpoints with the
            // reduced list.
            String[] atts = groups.toArray(new String[groups.size()]);
            mods.add(new Modification(ModificationType.REPLACE, ATTR_GROUP_ID, atts));
        }
        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(userDN, mods);
            getLogger().info("Removed group {} from user {}", groupId, userId);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating user {} groups - {}", userId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating user {} groups - {}", userId, result.getResultCode());
            throw new IllegalArgumentException(String.format(
                    "LDAP error encountered when updating user %s endpoints - %s", userId, result.getResultCode().toString()));
        }

        getLogger().debug("Removing groupId {} from user {}", groupId, userId);
    }

    @Override
    public List<Group> getGroupsForUser(String userId) {
        getLogger().debug("Inside getGroupsForUser {}", userId);
        List<Group> groups = new ArrayList<Group>();
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, userId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();

        try {
            searchResult = getAppConnPool().search(
                    BASE_DN,
                    SearchScope.SUB,
                    searchFilter,
                    new String[]{ATTR_GROUP_ID});
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for groups - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 0) {
            String errMsg = String.format("User %s not found.", userId);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        } else if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            String[] list = e.getAttributeValues(ATTR_GROUP_ID);
            if(list != null) {
                for(String id : list) {
                    groups.add(getGroupById(Integer.parseInt(id)));
                }
            }
        } else if (searchResult.getEntryCount() > 1) {
            String errMsg = String.format("More than one entry was found for user - %s", userId);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        getLogger().debug("Exiting getGroupsForUser {}", groups);
        return groups;
    }

    private Group getGroup(SearchResultEntry resultEntry) {
        getLogger().debug("Inside getCloudGroup");
        Group group = new Group();
        group.setUniqueId(resultEntry.getDN());
        group.setGroupId(Integer.parseInt(resultEntry.getAttributeValue(ATTR_ID)));
        group.setName(resultEntry.getAttributeValue(ATTR_GROUP_NAME));
        group.setDescription(resultEntry.getAttributeValue(ATTR_DESCRIPTION));
        getLogger().debug("Exiting getCloudGroup");
        return group;
    }

    private Attribute[] getAddAttributes(Group group)
        throws GeneralSecurityException, InvalidCipherTextException {
        CryptHelper cryptHelper = CryptHelper.getInstance();

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_GROUP_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(group.getGroupId().toString())) {
            atts.add(new Attribute(ATTR_ID, group.getGroupId().toString()));
        }

        if (!StringUtils.isBlank(group.getName())) {
            atts.add(new Attribute(ATTR_GROUP_NAME, group.getName()));
        }

        if (!StringUtils.isBlank(group.getDescription())) {
            atts.add(new Attribute(ATTR_DESCRIPTION, group.getDescription()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);
        getLogger().debug("Found {} attributes to add", attributes.length);
        return attributes;
    }

    List<Modification> getModifications(Group gOld, Group gNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (gNew.getName() != null && gNew.getName() != gOld.getName()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_GROUP_NAME, String.valueOf(gNew.getName())));
        }

        if (gNew.getDescription() != null && gNew.getDescription() != gOld.getDescription()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_DESCRIPTION, String.valueOf(gNew.getDescription())));
        }
        return mods;
    }

}
