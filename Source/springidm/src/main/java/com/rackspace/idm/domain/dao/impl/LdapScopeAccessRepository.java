package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.StaticUtils;

public class LdapScopeAccessRepository extends LdapRepository implements
    ScopeAccessDao {

    public LdapScopeAccessRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    @Override
    public void addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        getLogger().info("Adding ScopeAccess {} to {}", scopeAccess,
            parentUniqueId);

        if (scopeAccess == null) {
            String errMsg = "Null instance of scopeAccess was passed";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Audit audit = Audit.log(scopeAccess).add();

        if (StringUtils.isBlank(parentUniqueId)) {
            String errMsg = "parentUnqiueId cannot be blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Attribute[] atts = getAddAttributesForScopeAccess(scopeAccess);

        String dn = new LdapDnBuilder(parentUniqueId).addAttriubte(
            ATTR_CLIENT_ID, scopeAccess.getClientId()).build();

        scopeAccess.setUniqueId(dn);

        addEntry(dn, atts, audit);

        audit.succeed();

        getLogger().info("Added ScopeAcces {} to {}", scopeAccess,
            parentUniqueId);
    }
    
    @Override
    public List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId) {
        getLogger().debug("Getting {} scope accesses",
            parentUniqueId);
        if (StringUtils.isBlank(parentUniqueId)) {
            getLogger().error("Null or Empty uniqueId paramter");
            throw new IllegalArgumentException(
                "Null or Empty uniqueId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .build();

        List<SearchResultEntry> entries = this.getMultipleEntries(parentUniqueId, SearchScope.ONE, searchFilter, ATTR_CLIENT_ID);

        List<ScopeAccess> scopes = new ArrayList<ScopeAccess>();
        
        for (SearchResultEntry entry : entries){
            scopes.add(this.getScopeAccess(entry));
        }

        getLogger().debug("Got scope accesses for {}", parentUniqueId);
        return scopes;
    }

    @Override
    public void addPermissionToScopeAccess(String scopeAccessUniqueId,
        Permission permission) {
        getLogger().info("Adding Permission {} to {}", permission,
            scopeAccessUniqueId);

        if (permission == null) {
            String errMsg = "Null instance of permission was passed";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Audit audit = Audit.log(permission).add();

        if (StringUtils.isBlank(scopeAccessUniqueId)) {
            String errMsg = "scopeAccessUniqueId cannot be blank";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Attribute[] atts = getAddAttributesForClientPermission(permission);

        String dn = new LdapDnBuilder(scopeAccessUniqueId).addAttriubte(
            ATTR_NAME, permission.getPermissionId()).build();

        permission.setUniqueId(dn);

        addEntry(dn, atts, audit);

        audit.succeed();

        getLogger().info("Added Permission {} to {}", permission,
            scopeAccessUniqueId);
    }

    @Override
    public void deleteScopeAccess(ScopeAccess scopeAccess) {
        getLogger().info("Deleting scopeAccess {}", scopeAccess);
        if (StringUtils.isBlank(scopeAccess.getUniqueId())) {
            getLogger().error("Null or Empty uniqueId paramter");
            throw new IllegalArgumentException(
                "Null or Empty uniqueId parameter.");
        }

        Audit audit = Audit.log(scopeAccess).delete();

        this.deleteEntryAndSubtree(scopeAccess.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted scopeAccess {}", scopeAccess);
    }

    @Override
    public boolean doesAccessTokenHavePermission(String accessToken,
        Permission permission) {
        getLogger().debug("Does accessToken {} have permission {}",
            accessToken, permission);
        if (StringUtils.isBlank(accessToken)) {
            getLogger().error("Token cannot be blank.");
            throw new IllegalArgumentException("Token cannot be blank.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .build();

        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter, ATTR_NO_ATTRIBUTES);

        if (entry == null) {
            String errMsg = String.format("AccessToken %s not found.",
                accessToken);
            getLogger().error(errMsg);
            throw new NotFoundException(errMsg);
        }

        String parentDn = null;
        try {
            parentDn = entry.getParentDNString();
        } catch (LDAPException e) {
            String errMsg = String.format(
                "AccessToken %s does not have parent.", accessToken);
            getLogger().error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, permission.getPermissionId())
            .addEqualAttribute(ATTR_CLIENT_ID, permission.getClientId())
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER,
                permission.getCustomerId()).build();

        boolean hasPermission = this.getSingleEntry(parentDn, SearchScope.SUB,
            searchFilter, ATTR_NO_ATTRIBUTES) != null;

        String message = String.format(
            "Does accessToken %s have permission %s - %s", accessToken,
            permission, hasPermission);
        getLogger().debug(message);

        return entry != null;
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        getLogger().debug("Getting ScopeAccess by accessToken {}", accessToken);
        if (StringUtils.isBlank(accessToken)) {
            getLogger().error("AccessToken cannot be blank.");
            throw new IllegalArgumentException("AccessToken cannot be blank.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .build();

        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter);

        ScopeAccess scopeAccess = null;
        if (entry != null) {
            scopeAccess = this.getScopeAccess(entry);
        }

        getLogger().debug("Got scope access {} for accessToken {}",
            scopeAccess, accessToken);
        return scopeAccess;
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        getLogger().debug("Getting ScopeAccess by refreshToken {}",
            refreshToken);
        if (StringUtils.isBlank(refreshToken)) {
            getLogger().error("RefreshToken cannot be blank.");
            throw new IllegalArgumentException("RefreshToken cannot be blank.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_REFRESH_TOKEN, refreshToken)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .build();

        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter);

        ScopeAccess scopeAccess = null;
        if (entry != null) {
            scopeAccess = this.getScopeAccess(entry);
        }

        getLogger().debug("Got scope access {} for refreshToken {}",
            scopeAccess, refreshToken);
        return scopeAccess;
    }

    @Override
    public ScopeAccess getScopeAccessForParentByClientId(String parentUniqueId,
        String clientId) {
        getLogger().debug("Getting {} scope access for {}", clientId,
            parentUniqueId);
        if (StringUtils.isBlank(parentUniqueId)
            || StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty uniqueId or clientId paramter");
            throw new IllegalArgumentException(
                "Null or Empty uniqueId or clientId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .build();

        SearchResultEntry entry = this.getSingleEntry(parentUniqueId,
            SearchScope.ONE, searchFilter);

        ScopeAccess scopeAccess = null;
        if (entry != null) {
            scopeAccess = this.getScopeAccess(entry);
        }

        getLogger().debug("Got scope access {} for {}", scopeAccess,
            parentUniqueId);
        return scopeAccess;
    }
    
    @Override
    public ScopeAccess getScopeAccessByUsernameAndClientId(String username,
        String clientId) {
        getLogger().debug("Getting {} scope access for {}", clientId,
            username);

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_CLIENT_ID, clientId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .build();

        SearchResultEntry entry = this.getSingleEntry(BASE_DN,
            SearchScope.SUB, searchFilter);

        ScopeAccess scopeAccess = null;
        if (entry != null) {
            scopeAccess = this.getScopeAccess(entry);
        }

        getLogger().debug("Got scope access {} for {}", scopeAccess,
            username);
        return scopeAccess;
    }

    @Override
    public void removePermissionFromScopeAccess(Permission permission) {
        getLogger().info("Deleting permission {}", permission);
        if (StringUtils.isBlank(permission.getUniqueId())) {
            getLogger().error("Null or Empty uniqueId paramter");
            throw new IllegalArgumentException(
                "Null or Empty uniqueId parameter.");
        }

        Audit audit = Audit.log(permission).delete();

        this.deleteEntryAndSubtree(permission.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted permission {}", permission);
    }

    @Override
    public void updateScopeAccess(ScopeAccess scopeAccess) {
        getLogger().info("Updating scopeAccess {}", scopeAccess);

        if (scopeAccess == null
            || StringUtils.isBlank(scopeAccess.getUniqueId())) {
            getLogger().error(
                "ScopeAccess instance is null or its uniqueId has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The ScopeAccess instance either null or its uniqueId has no value.");
        }

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS).build();

        SearchResultEntry entry = this.getSingleEntry(
            scopeAccess.getUniqueId(), SearchScope.BASE, searchFilter);

        ScopeAccess old = this.getScopeAccess(entry);

        List<Modification> mods = getScopeAccessModifications(old, scopeAccess);

        if (mods.size() == 0) {
            // No changes!
            return;
        }

        Audit audit = Audit.log(scopeAccess).modify(mods);

        updateEntry(old.getUniqueId(), mods, audit);

        audit.succeed();
        getLogger().info("Updated scopeAccess {}", scopeAccess);
    }

    private ScopeAccess getScopeAccess(SearchResultEntry resultEntry) {
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setUniqueId(resultEntry.getDN());
        scopeAccess.setAccessToken(resultEntry
            .getAttributeValue(ATTR_ACCESS_TOKEN));
        scopeAccess.setClientId(resultEntry.getAttributeValue(ATTR_CLIENT_ID));
        scopeAccess.setRefreshToken(resultEntry
            .getAttributeValue(ATTR_REFRESH_TOKEN));
        scopeAccess.setClientRCN(resultEntry.getAttributeValue(ATTR_CLIENT_RCN));
        scopeAccess.setUsername(resultEntry.getAttributeValue(ATTR_UID));
        scopeAccess.setUserRCN(resultEntry.getAttributeValue(ATTR_USER_RCN));

        Date accessExpiration = resultEntry
            .getAttributeValueAsDate(ATTR_ACCESS_TOKEN_EXP);
        if (accessExpiration != null) {
            scopeAccess
                .setAccessTokenExpiration(new DateTime(accessExpiration));
        }

        Date refreshExpiration = resultEntry
            .getAttributeValueAsDate(ATTR_REFRESH_TOKEN_EXP);
        if (refreshExpiration != null) {
            scopeAccess.setRefreshTokenExpiration(new DateTime(
                refreshExpiration));
        }

        return scopeAccess;
    }

    private Attribute[] getAddAttributesForScopeAccess(ScopeAccess scopeAccess) {
        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_SCOPE_ACCESS_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(scopeAccess.getClientId())) {
            atts.add(new Attribute(ATTR_CLIENT_ID, scopeAccess.getClientId()));
        }
        
        if (!StringUtils.isBlank(scopeAccess.getClientRCN())) {
            atts.add(new Attribute(ATTR_CLIENT_RCN, scopeAccess.getClientRCN()));
        }
        
        if (!StringUtils.isBlank(scopeAccess.getUsername())) {
            atts.add(new Attribute(ATTR_UID, scopeAccess.getUsername()));
        }
        
        if (!StringUtils.isBlank(scopeAccess.getUserRCN())) {
            atts.add(new Attribute(ATTR_USER_RCN, scopeAccess.getUserRCN()));
        }

        if (!StringUtils.isBlank(scopeAccess.getAccessToken())) {
            atts.add(new Attribute(ATTR_ACCESS_TOKEN, scopeAccess
                .getAccessToken()));
        }

        if (!StringUtils.isBlank(scopeAccess.getAccessToken())) {
            atts.add(new Attribute(ATTR_REFRESH_TOKEN, scopeAccess
                .getRefreshToken()));
        }

        if (scopeAccess.getAccessTokenExpiration() != null) {
            atts.add(new Attribute(ATTR_ACCESS_TOKEN_EXP, StaticUtils
                .encodeGeneralizedTime(scopeAccess.getAccessTokenExpiration()
                    .toDate())));
        }

        if (scopeAccess.getRefreshTokenExpiration() != null) {
            atts.add(new Attribute(ATTR_REFRESH_TOKEN_EXP, StaticUtils
                .encodeGeneralizedTime(scopeAccess.getRefreshTokenExpiration()
                    .toDate())));
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

    List<Modification> getScopeAccessModifications(ScopeAccess sOld,
        ScopeAccess sNew) {
        List<Modification> mods = new ArrayList<Modification>();

        if (sNew.getAccessToken() != null
            && !sOld.getAccessToken().equals(sNew.getAccessToken())) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_ACCESS_TOKEN, sNew.getAccessToken().toString()));
        }

        if (sNew.getRefreshToken() != null
            && !sOld.getAccessToken().equals(sNew.getRefreshToken())) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_REFRESH_TOKEN, sNew.getRefreshToken().toString()));
        }

        if (sNew.getAccessTokenExpiration() != null
            && !sNew.getAccessTokenExpiration().equals(
                sOld.getAccessTokenExpiration())) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_ACCESS_TOKEN_EXP, StaticUtils.encodeGeneralizedTime(sNew
                    .getAccessTokenExpiration().toDate())));
        }

        if (sNew.getRefreshTokenExpiration() != null
            && !sNew.getRefreshTokenExpiration().equals(
                sOld.getRefreshTokenExpiration())) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_ACCESS_TOKEN_EXP, StaticUtils.encodeGeneralizedTime(sNew
                    .getRefreshTokenExpiration().toDate())));
        }

        return mods;
    }
}
