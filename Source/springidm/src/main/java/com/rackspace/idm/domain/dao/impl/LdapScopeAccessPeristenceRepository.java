package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

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
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import com.unboundid.util.LDAPSDKRuntimeException;

public class LdapScopeAccessPeristenceRepository extends LdapRepository implements ScopeAccessObjectDao {

    public LdapScopeAccessPeristenceRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
    }

    @Override
    public ScopeAccessObject addScopeAccess(String parentUniqueId, ScopeAccessObject scopeAccess) {
        LDAPConnection conn = null;
        try {
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            conn = getAppConnPool().getConnection();
            persister.add(scopeAccess, conn, parentUniqueId);
            return (ScopeAccessObject) persister.get(scopeAccess, conn, parentUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public PermissionObject addPermissionToScopeAccess(String scopeAccessUniqueId, PermissionObject permission) {
        if (permission instanceof PermissionObject) {

            LDAPConnection conn = null;
            try {
                final PermissionObject po = permission;
                final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
                conn = getAppConnPool().getConnection();
                persister.add(po, conn, scopeAccessUniqueId);
                return persister.get(po, conn, scopeAccessUniqueId);
            } catch (final LDAPException e) {
                getLogger().error("Error adding permission", e);
            } finally {
                getAppConnPool().releaseConnection(conn);
            }
        }
        return null;
    }

    @Override
    public Boolean deleteScopeAccess(ScopeAccessObject scopeAccess) {
        LDAPConnection conn = null;
        try {
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            conn = getAppConnPool().getConnection();
            final LDAPResult result = persister.delete(scopeAccess, conn);
            return result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE;
        } catch (final LDAPPersistException e) {
            e.printStackTrace();
        } catch (final LDAPException e) {
            getLogger().error("Error deleting scope access", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean doesAccessTokenHavePermission(String accessToken, PermissionObject permission) {
        if (permission instanceof PermissionObject) {
            final PermissionObject po = permission;
            LDAPConnection conn = null;
            try {
                conn = getAppConnPool().getConnection();
                final ScopeAccessObject scopeAccess = getScopeAccessByAccessToken(accessToken);

                final PermissionObject searchForObject = LDAPPersister.getInstance(PermissionObject.class)
                .searchForObject(po, conn, scopeAccess.getUniqueId(), SearchScope.SUB);
                return searchForObject != null;
            } catch (final LDAPException e) {
                getLogger().error("Error checking permission", e);
            } finally {
                getAppConnPool().releaseConnection(conn);
            }
        }
        return false;
    }

    @Override
    public List<ScopeAccessObject> getScopeAccessesByParent(String parentUniqueId) {
        final List<ScopeAccessObject> list = new ArrayList<ScopeAccessObject>();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute("objectClass", "scopeAccess".getBytes())
            .build();
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.SUBORDINATE_SUBTREE, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope accesses by parent", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return list;
    }

    @Override
    public ScopeAccessObject getScopeAccessByAccessToken(String accessToken) {
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute("objectClass", "scopeAccess".getBytes())
            .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken.getBytes()).build();
            final SearchResult searchResult = conn.search("dc=rackspace,dc=com", SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by token", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken) {
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute("objectClass", "scopeAccess".getBytes())
            .addEqualAttribute(ATTR_REFRESH_TOKEN, refreshToken.getBytes()).build();
            final SearchResult searchResult = conn.search("dc=rackspace,dc=com", SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading refresh token", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public ScopeAccessObject getScopeAccessForParentByClientId(String parentUniqueId, String clientId) {
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute("objectClass", "scopeAccess".getBytes())
            .addEqualAttribute(ATTR_CLIENT_ID, clientId.getBytes()).build();
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.ONE, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by clientId", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    private ScopeAccessObject decodeScopeAccess(final SearchResultEntry searchResultEntry) throws LDAPPersistException {
        ScopeAccessObject object = null;
        if (searchResultEntry.getAttribute("objectClass").hasValue("userScopeAccess")) {
            object = LDAPPersister.getInstance(UserScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute("objectClass").hasValue("clientScopeAccess")) {
            object = LDAPPersister.getInstance(ClientScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute("objectClass").hasValue("passwordResetScopeAccess")) {
            object = LDAPPersister.getInstance(PasswordResetScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute("objectClass").hasValue("rackerScopeAccess")) {
            object = LDAPPersister.getInstance(RackerScopeAccessObject.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute("objectClass").hasValue("scopeAccess")) {
            object = LDAPPersister.getInstance(ScopeAccessObject.class).decode(searchResultEntry);
        }
        return object;
    }

    @Override
    public ScopeAccessObject getScopeAccessByUsernameAndClientId(String username, String clientId) {
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute("objectClass", "scopeAccess".getBytes())
            .addEqualAttribute(ATTR_UID, username.getBytes())
            .addEqualAttribute(ATTR_CLIENT_ID, clientId.getBytes()).build();
            final SearchResult searchResult = conn.search("dc=rackspace,dc=com", SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by username", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public Boolean removePermissionFromScopeAccess(PermissionObject permission) {
        if (permission instanceof PermissionObject) {

            LDAPConnection conn = null;
            try {
                final PermissionObject po = permission;
                final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
                conn = getAppConnPool().getConnection();
                final LDAPResult result = persister.delete(po, conn);
                return result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE;
            } catch (final LDAPException e) {
                getLogger().error("Error deleting permission", e);
            } finally {
                getAppConnPool().releaseConnection(conn);
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean updateScopeAccess(ScopeAccessObject scopeAccess) {
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            final LDAPResult result = persister.modify(scopeAccess, conn, null, true);
            return result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE;
        } catch (final LDAPException e) {
            getLogger().error("Error updating scope access", e);
        } catch (final LDAPSDKRuntimeException e) {
            //noop
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean updatePermissionForScopeAccess(PermissionObject permission) {
        LDAPConnection conn = null;
        try {
            final PermissionObject po = permission;
            final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
            conn = getAppConnPool().getConnection();
            final LDAPResult result = persister.modify(po, conn, null, true);
            return result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE;
        } catch (final LDAPException e) {
            getLogger().error("Error updating permission", e);
        } catch (final LDAPSDKRuntimeException e) {
            //noop
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return Boolean.FALSE;
    }

    @Override
    public PermissionObject getPermissionByParentAndPermissionId(String parentUniqueId, String permissionId) {
        LDAPConnection conn = null;
        try {
            final LDAPPersister<PermissionObject> persister = LDAPPersister.getInstance(PermissionObject.class);
            conn = getAppConnPool().getConnection();
            final PermissionObject po = new PermissionObject();
            po.setPermissionId(permissionId);
            return persister.get(po, conn, parentUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error reading permission by parent and permission", e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

}
