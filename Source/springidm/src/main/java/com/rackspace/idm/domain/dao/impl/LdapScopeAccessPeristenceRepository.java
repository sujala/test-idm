package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.PermissionEntity;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import com.unboundid.ldap.sdk.persist.PersistedObjects;
import com.unboundid.util.LDAPSDKRuntimeException;

public class LdapScopeAccessPeristenceRepository extends LdapRepository implements ScopeAccessDao {

    public LdapScopeAccessPeristenceRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
    }

    @Override
    public ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        getLogger().debug("Adding ScopeAccess: {}", scopeAccess);
        LDAPConnection conn = null;
        Audit audit = Audit.log(scopeAccess).add();
        try {
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            conn = getAppConnPool().getConnection();
            try {
                persister.add(scopeAccess, conn, parentUniqueId);
            } catch (final LDAPException e) {
                if( e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS ) {
                    // noop
                } else {
                    throw e;
                }
            }
            audit.succeed();
            getLogger().debug("Added ScopeAccess: {}", scopeAccess);
            return (ScopeAccess) persister.get(scopeAccess, conn, parentUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public PermissionEntity grantPermission(String scopeAccessUniqueId, PermissionEntity permission) {
        getLogger().debug("Granting Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).add();
        try {
            final PermissionEntity po = new PermissionEntity();
            po.setClientId(permission.getClientId());
            po.setCustomerId(permission.getCustomerId());
            po.setPermissionId(permission.getPermissionId());
            po.setResourceGroup(permission.getResourceGroup());
            final LDAPPersister<PermissionEntity> persister = LDAPPersister.getInstance(PermissionEntity.class);
            conn = getAppConnPool().getConnection();
            try {
                persister.add(po, conn, scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if( e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS ) {
                    // noop
                } else {
                    throw e;
                }
            }
            getLogger().debug("Granted Permission: {}", permission);
            audit.succeed();
            return persister.get(po, conn, scopeAccessUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error granting permission", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public PermissionEntity definePermission(String scopeAccessUniqueId, PermissionEntity permission) {
        getLogger().debug("Defining Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<PermissionEntity> persister = LDAPPersister.getInstance(PermissionEntity.class);
            conn = getAppConnPool().getConnection();
            try {
                persister.add(permission, conn, scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if( e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS ) {
                    // noop
                } else {
                    throw e;
                }
            }
            getLogger().debug("Defined Permission: {}", permission);
            audit.succeed();
            return persister.get(permission, conn, scopeAccessUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error defining permission", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public Boolean deleteScopeAccess(ScopeAccess scopeAccess) {
        getLogger().debug("Deleting ScopeAccess: {}", scopeAccess);
        final String dn = scopeAccess.getUniqueId();
        final Audit audit = Audit.log(scopeAccess.getAuditContext()).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted ScopeAccess: {}", scopeAccess);
        return Boolean.TRUE;
    }

    @Override
    public Boolean doesAccessTokenHavePermission(String accessToken, PermissionEntity permission) {
        getLogger().debug("Checking Permission: {}", permission);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final ScopeAccess scopeAccess = getScopeAccessByAccessToken(accessToken);

            final PermissionEntity result = LDAPPersister.getInstance(PermissionEntity.class)
            .searchForObject(permission, conn, scopeAccess.getLDAPEntry().getParentDNString(), SearchScope.SUB);
            getLogger().debug("{} : {}", result == null ? "Found" : "Did not find", permission);
            return result != null;
        } catch (final LDAPException e) {
            getLogger().error("Error checking permission", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId) {
        getLogger().debug("Finding ScopeAccesses for: {}", parentUniqueId);
        final List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .build();
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope accesses by parent", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        getLogger().debug("Found {} ScopeAccess object(s) for: {}", list.size(), parentUniqueId);
        return list;
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        getLogger().debug("Find ScopeAccess by AccessToken: {}", accessToken);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken).build();
            final SearchResult searchResult = conn.search(BASE_DN, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug("Found {} ScopeAccess by AccessToken: {}", searchEntries.size(), accessToken);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading ScopeAccess by AccessToken: " + accessToken, e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        getLogger().debug("Find ScopeAccess by RefreshToken: {}", refreshToken);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .addEqualAttribute(ATTR_REFRESH_TOKEN, refreshToken).build();
            final SearchResult searchResult = conn.search(BASE_DN, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug("Found {} ScopeAccess object by RefreshToken: {}", searchEntries.size(), refreshToken);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading ScopeAccess by RefreshToken: " + refreshToken, e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public ScopeAccess getScopeAccessForParentByClientId(String parentUniqueId, String clientId) {
        getLogger().debug("Find ScopeAccess for Parent: {} by ClientId: {}", parentUniqueId, clientId);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug("Found {} ScopeAccess(s) for Parent: {} by ClientId: {}", new Object[] { searchEntries.size(), parentUniqueId, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by clientId", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    private ScopeAccess decodeScopeAccess(final SearchResultEntry searchResultEntry) throws LDAPPersistException {
        ScopeAccess object = null;
        if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_USERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(UserScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_CLIENTSCOPEACCESS)) {
            object = LDAPPersister.getInstance(ClientScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_PASSWORDRESETSCOPEACCESS)) {
            object = LDAPPersister.getInstance(PasswordResetScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_RACKERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(RackerScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_SCOPEACCESS)) {
            object = LDAPPersister.getInstance(ScopeAccess.class).decode(searchResultEntry);
        }
        return object;
    }

    @Override
    public ScopeAccess getScopeAccessByUsernameAndClientId(String username, String clientId) {
        getLogger().debug("Find ScopeAccess by Username: {} and ClientId: {}", username, clientId);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(BASE_DN, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug("Found {}  ScopeAccess(s) by Username: {} and ClientId: {}", new Object[] {searchEntries.size(), username, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by username", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public Boolean removePermissionFromScopeAccess(PermissionEntity permission) {
        getLogger().debug("Remove Permission: {}", permission);
        final String dn = permission.getUniqueId();
        final Audit audit = Audit.log(permission.getAuditContext()).delete();
        deleteEntryAndSubtree(dn, audit);
        getLogger().debug("Removed Permission: {}", permission);
        audit.succeed();
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateScopeAccess(ScopeAccess scopeAccess) {
        getLogger().debug("Updating ScopeAccess: {}", scopeAccess);
        LDAPConnection conn = null;
        Audit audit = Audit.log(scopeAccess);
        try {
            conn = getAppConnPool().getConnection();
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            List<Modification> modifications = persister.getModifications(scopeAccess, true, null);
            audit.modify(modifications);
            persister.modify(scopeAccess, conn, null, true);
            getLogger().debug("Updated ScopeAccess: {}", scopeAccess);
            audit.succeed();
            return Boolean.TRUE;
        } catch (final LDAPException e) {
            getLogger().error("Error updating scope access", e);
            audit.fail();
            throw new IllegalStateException(e);
        } catch (final LDAPSDKRuntimeException e) {
            //noop
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean updatePermissionForScopeAccess(PermissionEntity permission) {
        getLogger().debug("Updating Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).modify();
        try {
            final PermissionEntity po = permission;
            final LDAPPersister<PermissionEntity> persister = LDAPPersister.getInstance(PermissionEntity.class);
            conn = getAppConnPool().getConnection();
            final LDAPResult result = persister.modify(po, conn, null, true);
            getLogger().debug("Updated Permission: {}", permission);
            audit.succeed();
            return result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE;
        } catch (final Exception e) {
            getLogger().error("Error updating permission", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public List<PermissionEntity> getPermissionsByPermission(PermissionEntity permission) {
        return getPermissionsByParentAndPermissionId(BASE_DN, permission);
    }
    
    @Override
    public PermissionEntity getPermissionByParentAndPermissionId(String parentUniqueId, PermissionEntity permission) {
        getLogger().debug("Find Permission: {} by ParentId: {}", permission, parentUniqueId);
        final List<PermissionEntity> list = getPermissionsByParentAndPermissionId(parentUniqueId, permission);
        if(list.size() == 1) {
            getLogger().debug("Found 1 Permission: {} by ParentId: {}", permission, parentUniqueId);
            return list.get(0);
        }
        getLogger().debug("Found {} Permission: {} by ParentId: {} , returning NULL", new Object[] { list.size(), permission, parentUniqueId});
        return null;
    }

    @Override
    public List<PermissionEntity> getPermissionsByParentAndPermissionId(String parentUniqueId,
            PermissionEntity permission) {
        getLogger().debug("Find Permissions by ParentId: {} and Permission: {} ", parentUniqueId, permission);
        LDAPConnection conn = null;
        final List<PermissionEntity> list = new ArrayList<PermissionEntity>();
        try {
            final LDAPPersister<PermissionEntity> persister = LDAPPersister.getInstance(PermissionEntity.class);
            conn = getAppConnPool().getConnection();
            final PersistedObjects<PermissionEntity> objects = persister.search(permission,conn, parentUniqueId, SearchScope.SUB);
            while(true) {
                final PermissionEntity next = objects.next();
                if(next == null) {
                    break;
                }
                list.add(next);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading permission by parent and permission", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        getLogger().debug("Found {}  Permission(s) by ParentId: {} and Permission: {} ", new Object[] { list.size(), parentUniqueId, permission});
        return list;
    }

}
