package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
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

public class LdapScopeAccessPeristenceRepository extends LdapRepository implements ScopeAccessObjectDao {

    public LdapScopeAccessPeristenceRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
    }

    @Override
    public ScopeAccessObject addScopeAccess(String parentUniqueId, ScopeAccessObject scopeAccess) {
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
            return (ScopeAccessObject) persister.get(scopeAccess, conn, parentUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public PermissionObject grantPermission(String scopeAccessUniqueId, PermissionObject permission) {
        getLogger().debug("Granting Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).add();
        try {
            final PermissionObject po = new PermissionObject();
            po.setClientId(permission.getClientId());
            po.setCustomerId(permission.getCustomerId());
            po.setPermissionId(permission.getPermissionId());
            po.setResourceGroup(permission.getResourceGroup());
            final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
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
    public PermissionObject definePermission(String scopeAccessUniqueId, PermissionObject permission) {
        getLogger().debug("Defining Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
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
    public Boolean deleteScopeAccess(ScopeAccessObject scopeAccess) {
        getLogger().debug("Deleting ScopeAccess: {}", scopeAccess);
        final String dn = scopeAccess.getUniqueId();
        final Audit audit = Audit.log(scopeAccess.getAuditContext()).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted ScopeAccess: {}", scopeAccess);
        return Boolean.TRUE;
    }

    @Override
    public Boolean doesAccessTokenHavePermission(String accessToken, PermissionObject permission) {
        getLogger().debug("Checking Permission: {}", permission);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final ScopeAccessObject scopeAccess = getScopeAccessByAccessToken(accessToken);

            final PermissionObject result = LDAPPersister.getInstance(PermissionObject.class)
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
    public List<ScopeAccessObject> getScopeAccessesByParent(String parentUniqueId) {
        getLogger().debug("Finding ScopeAccesses for: {}", parentUniqueId);
        final List<ScopeAccessObject> list = new ArrayList<ScopeAccessObject>();
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
    public ScopeAccessObject getScopeAccessByAccessToken(String accessToken) {
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
    public ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken) {
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
    public ScopeAccessObject getScopeAccessForParentByClientId(String parentUniqueId, String clientId) {
        getLogger().debug("Find ScopeAccess for Parent: {} by ClientId: {}", parentUniqueId, clientId);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
            .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.ONE, filter);

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

    private ScopeAccessObject decodeScopeAccess(final SearchResultEntry searchResultEntry) throws LDAPPersistException {
        ScopeAccessObject object = null;
        if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_USERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(UserScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_CLIENTSCOPEACCESS)) {
            object = LDAPPersister.getInstance(ClientScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_PASSWORDRESETSCOPEACCESS)) {
            object = LDAPPersister.getInstance(PasswordResetScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_RACKERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(RackerScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_SCOPEACCESS)) {
            object = LDAPPersister.getInstance(ScopeAccessObject.class).decode(searchResultEntry);
        }
        return object;
    }

    @Override
    public ScopeAccessObject getScopeAccessByUsernameAndClientId(String username, String clientId) {
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
    public Boolean removePermissionFromScopeAccess(PermissionObject permission) {
        getLogger().debug("Remove Permission: {}", permission);
        final String dn = permission.getUniqueId();
        final Audit audit = Audit.log(permission.getAuditContext()).delete();
        deleteEntryAndSubtree(dn, audit);
        getLogger().debug("Removed Permission: {}", permission);
        audit.succeed();
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateScopeAccess(ScopeAccessObject scopeAccess) {
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
    public Boolean updatePermissionForScopeAccess(PermissionObject permission) {
        getLogger().debug("Updating Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).modify();
        try {
            final PermissionObject po = permission;
            final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
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
    public List<PermissionObject> getPermissionsByPermission(PermissionObject permission) {
        return getPermissionsByParentAndPermissionId(BASE_DN, permission);
    }
    
    @Override
    public PermissionObject getPermissionByParentAndPermissionId(String parentUniqueId, PermissionObject permission) {
        getLogger().debug("Find Permission: {} by ParentId: {}", permission, parentUniqueId);
        final List<PermissionObject> list = getPermissionsByParentAndPermissionId(parentUniqueId, permission);
        if(list.size() == 1) {
            getLogger().debug("Found 1 Permission: {} by ParentId: {}", permission, parentUniqueId);
            return list.get(0);
        }
        getLogger().debug("Found {} Permission: {} by ParentId: {} , returning NULL", new Object[] { list.size(), permission, parentUniqueId});
        return null;
    }

    @Override
    public List<PermissionObject> getPermissionsByParentAndPermissionId(String parentUniqueId,
            PermissionObject permission) {
        getLogger().debug("Find Permissions by ParentId: {} and Permission: {} ", parentUniqueId, permission);
        LDAPConnection conn = null;
        final List<PermissionObject> list = new ArrayList<PermissionObject>();
        try {
            final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
            conn = getAppConnPool().getConnection();
            final PersistedObjects<PermissionObject> objects = persister.search(permission,conn, parentUniqueId, SearchScope.SUB);
            while(true) {
                final PermissionObject next = objects.next();
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
