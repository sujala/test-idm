package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import com.unboundid.util.LDAPSDKRuntimeException;
import org.apache.commons.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;

public class LdapScopeAccessPeristenceRepository extends LdapRepository
    implements ScopeAccessDao {

    public LdapScopeAccessPeristenceRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    @Override
    public ScopeAccess addDelegateScopeAccess(String parentUniqueId,
        ScopeAccess scopeAccess) {
        getLogger().info("Adding Delegate ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess).add();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            SearchResultEntry entry = getContainer(conn, parentUniqueId, CONTAINER_DELEGATE);
            if (entry == null) {
                addContianer(conn, parentUniqueId, CONTAINER_DELEGATE);
                entry = getContainer(conn, parentUniqueId, CONTAINER_DELEGATE);
            }
            audit.succeed();
            getLogger().info("Added Delegate ScopeAccess: {}", scopeAccess);
            return addScopeAccess(conn, entry.getDN(), scopeAccess);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public ScopeAccess addImpersonatedScopeAccess(String parentUniqueId, String clientId, ScopeAccess scopeAccess) {
        getLogger().info("Adding Impersonated ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess).add();
        LDAPConnection conn = null;

        String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME, clientId).build();
        try {
            conn = getAppConnPool().getConnection();
            SearchResultEntry entry = getContainer(conn, dn, CONTAINER_IMPERSONATED);
            if (entry == null) {
                addContianer(conn, dn, CONTAINER_IMPERSONATED);
                entry = getContainer(conn, dn, CONTAINER_IMPERSONATED);
            }
            audit.succeed();
            getLogger().info("Added Impersonated ScopeAccess: {}", scopeAccess);
            return addScopeAccess(conn, entry.getDN(), scopeAccess);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public ScopeAccess addDirectScopeAccess(String parentUniqueId,
        ScopeAccess scopeAccess) {
        getLogger().info("Adding Delegate ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess).add();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            SearchResultEntry entry = getContainer(conn, parentUniqueId, CONTAINER_DIRECT);
            if (entry == null) {
                addContianer(conn, parentUniqueId, CONTAINER_DIRECT);
                entry = getContainer(conn, parentUniqueId, CONTAINER_DIRECT);
            }
            audit.succeed();
            getLogger().info("Added Delegate ScopeAccess: {}", scopeAccess);
            return addScopeAccess(conn, entry.getDN(), scopeAccess);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public ScopeAccess addScopeAccess(String parentUniqueId,
        ScopeAccess scopeAccess) {
        getLogger().info("Adding Delegate ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess).add();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            getLogger().info("Added Delegate ScopeAccess: {}", scopeAccess);
            audit.succeed();
            return addScopeAccess(conn, parentUniqueId, scopeAccess);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public DefinedPermission definePermission(String scopeAccessUniqueId,
        DefinedPermission permission) {
        getLogger().debug("Defining Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<DefinedPermission> persister = LDAPPersister.getInstance(DefinedPermission.class);
            conn = getAppConnPool().getConnection();
            try {
                persister.add(permission, conn, scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
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
    public DelegatedPermission delegatePermission(String scopeAccessUniqueId,
        DelegatedPermission permission) {
        getLogger().debug("Delegating Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<DelegatedPermission> persister = LDAPPersister.getInstance(DelegatedPermission.class);
            conn = getAppConnPool().getConnection();
            try {
                persister.add(permission, conn, scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                    // noop
                } else {
                    throw e;
                }
            }
            getLogger().debug("Delegated Permission: {}", permission);
            audit.succeed();
            return persister.get(permission, conn, scopeAccessUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error delegating permission", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public boolean deleteScopeAccess(ScopeAccess scopeAccess) {
        getLogger().debug("Deleting ScopeAccess: {}", scopeAccess);
        final String dn = scopeAccess.getUniqueId();
        final Audit audit = Audit.log(scopeAccess.getAuditContext()).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted ScopeAccess: {}", scopeAccess);
        return true;
    }

    @Override
    public boolean doesAccessTokenHavePermission(ScopeAccess token,
        Permission permission) {
        getLogger().debug("Checking Permission: {}", permission);
        LDAPConnection conn = null;

        Permission perm = new Permission(permission.getCustomerId(), permission.getClientId(), permission.getPermissionId());
        try {
            String dn = token instanceof DelegatedClientScopeAccess ? token
                .getUniqueId() : token.getLDAPEntry().getParentDNString();

            conn = getAppConnPool().getConnection();

            final Permission result = LDAPPersister.getInstance(Permission.class)
                    .searchForObject(perm, conn, dn, SearchScope.SUB);
            getLogger().debug("{} : {}", result == null ? "Found" : "Did not find", perm);
            return result != null;
        } catch (final LDAPException e) {
            getLogger().error("Error checking permission", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public boolean doesParentHaveScopeAccess(String parentUniqueId,
        ScopeAccess scopeAccess) {
        LDAPConnection conn = null;

        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(scopeAccess.getClientId());

        try {
            conn = getAppConnPool().getConnection();

            final ScopeAccess result = LDAPPersister.getInstance(
                ScopeAccess.class).searchForObject(sa, conn, parentUniqueId, SearchScope.ONE);
            getLogger().debug("{} : {}",
                result == null ? "Found" : "Did not find", sa);
            return result != null;
        } catch (final LDAPException e) {
            getLogger().error("Error checking permission", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public ScopeAccess getDelegateScopeAccessForParentByClientId(
        String parentUniqueId, String clientId) {
        getLogger().debug("Find ScopeAccess for Parent: {} by ClientId: {}", parentUniqueId, clientId);
        LDAPConnection conn = null;

        String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME, CONTAINER_DELEGATE).build();

        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(dn, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug(
                "Found {} ScopeAccess(s) for Parent: {} by ClientId: {}",
                new Object[]{searchEntries.size(), parentUniqueId, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
                return null;
            }
            getLogger().error("Error reading scope access by clientId", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public ScopeAccess getDirectScopeAccessForParentByClientId(String parentUniqueId, String clientId) {
        getLogger().debug("Find ScopeAccess for Parent: {} by ClientId: {}", parentUniqueId, clientId);
        LDAPConnection conn = null;

        //String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME, CONTAINER_DIRECT).build();
        String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME, CONTAINER_DIRECT).addAttribute(ATTR_NAME, clientId).build();

        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(dn, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug(
                "Found {} ScopeAccess(s) for Parent: {} by ClientId: {}",
                new Object[]{searchEntries.size(), parentUniqueId, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
                return null;
            }
            getLogger().error("Error reading scope access by clientId", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public Permission getPermissionByParentAndPermission(String parentUniqueId, Permission permission) {
        getLogger().debug("Find Permission: {} by ParentId: {}", permission, parentUniqueId);
        final List<Permission> list = getPermissionsByParentAndPermission(parentUniqueId, permission);
        if (list.size() == 1) {
            getLogger().debug("Found 1 Permission: {} by ParentId: {}", permission, parentUniqueId);
            return list.get(0);
        }
        getLogger().debug(
            "Found {} Permission: {} by ParentId: {} , returning NULL",
            new Object[]{list.size(), permission, parentUniqueId});
        return null;
    }

    @Override
    public List<Permission> getPermissionsByParentAndPermission(
        String parentUniqueId, Permission permission) {
        getLogger().debug(
            "Find Permissions by ParentId: {} and Permission: {} ",
            parentUniqueId, permission);
        LDAPConnection conn = null;
        final List<Permission> list = new ArrayList<Permission>();
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = getFilterForPermission(permission);
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodePermission(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error(
                "Error reading permission by parent and permission", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        getLogger().debug(
            "Found {}  Permission(s) by ParentId: {} and Permission: {} ",
            new Object[]{list.size(), parentUniqueId, permission});
        return list;
    }

    @Override
    public List<Permission> getPermissionsByParent(String parentUniqueId) {
        return getPermissionsByParentAndPermission(parentUniqueId, null);
    }

    @Override
    public List<Permission> getPermissionsByPermission(Permission permission) {
        return getPermissionsByParentAndPermission(BASE_DN, permission);
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        getLogger().debug("Find ScopeAccess by AccessToken: {}", accessToken);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken).build();
            final SearchResult searchResult = conn.search(BASE_DN,
                SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult
                .getSearchEntries();
            getLogger().debug("Found {} ScopeAccess by AccessToken: {}",
                searchEntries.size(), accessToken);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error(
                "Error reading ScopeAccess by AccessToken: " + accessToken, e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public DelegatedClientScopeAccess getScopeAccessByAuthorizationCode(
        String authorizationCode) {
        getLogger().debug("Find ScopeAccess by Authorization Code: {}",
            authorizationCode);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS)
                .addEqualAttribute(ATTR_AUTH_CODE, authorizationCode).build();
            final SearchResult searchResult = conn.search(BASE_DN, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug("Found {} ScopeAccess by AccessToken: {}", searchEntries.size(), authorizationCode);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return (DelegatedClientScopeAccess) decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error(
                "Error reading ScopeAccess by Authorization Code: "
                    + authorizationCode, e);
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
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_REFRESH_TOKEN, refreshToken).build();
            final SearchResult searchResult = conn.search(BASE_DN,
                SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug(
                "Found {} ScopeAccess object by RefreshToken: {}",
                searchEntries.size(), refreshToken);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger()
                .error(
                        "Error reading ScopeAccess by RefreshToken: "
                                + refreshToken, e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return null;
    }

    @Override
    public ScopeAccess getScopeAccessByUsernameAndClientId(String username,
        String clientId) {
        getLogger().debug("Find ScopeAccess by Username: {} and ClientId: {}",
            username, clientId);
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(BASE_DN, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug(
                "Found {}  ScopeAccess(s) by Username: {} and ClientId: {}",
                new Object[]{searchEntries.size(), username, clientId});
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
    public List<DelegatedClientScopeAccess> getDelegatedClientScopeAccessByUsername(
        String username) {
        getLogger().debug("Find ScopeAccess by Username: {}", username);
        LDAPConnection conn = null;
        List<DelegatedClientScopeAccess> scopeAccessList = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS)
                .addEqualAttribute(ATTR_UID, username).build();
            final SearchResult searchResult = conn.search(BASE_DN, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug("Found {}  ScopeAccess(s) by Username: {}",
                new Object[]{searchEntries.size(), username});

            scopeAccessList = new ArrayList<DelegatedClientScopeAccess>();

            for (final SearchResultEntry searchResultEntry : searchEntries) {
                DelegatedClientScopeAccess scopeAccess = (DelegatedClientScopeAccess) decodeScopeAccess(searchResultEntry);
                scopeAccessList.add(scopeAccess);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by username", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return scopeAccessList;
    }

    @Override
    public List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId) {
        getLogger().debug("Finding ScopeAccesses for: {}", parentUniqueId);
        final List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(
                ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS).build();
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult
                .getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope accesses by parent", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        getLogger().debug("Found {} ScopeAccess object(s) for: {}",
            list.size(), parentUniqueId);
        return list;
    }

    @Override
    public ScopeAccess getScopeAccessByParentAndClientId(String parentUniqueId,
        String clientId) {
        getLogger().debug("Find ScopeAccess for Parent: {} by ClientId: {}",
            parentUniqueId, clientId);
        LDAPConnection conn = null;

        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(parentUniqueId,
                SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            getLogger().debug(
                "Found {} ScopeAccess(s) for Parent: {} by ClientId: {}",
                new Object[]{searchEntries.size(), parentUniqueId, clientId});
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

    @Override
    public List<ScopeAccess> getScopeAccessesByParentAndClientId(
        String parentUniqueId, String clientId) {
        getLogger().debug("Find ScopeAccess for Parent: {} by ClientId: {}",
            parentUniqueId, clientId);
        final List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        LDAPConnection conn = null;

        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();
            final SearchResult searchResult = conn.search(parentUniqueId, SearchScope.SUB, filter);

            final List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by clientId", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return list;
    }

    @Override
    public List<ScopeAccess> getDelegateScopeAccessesByParent(
        String parentUniqueId) {
        getLogger().debug("Finding ScopeAccesses for: {}", parentUniqueId);
        final List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        LDAPConnection conn = null;
        String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME, CONTAINER_DELEGATE).build();
        try {
            conn = getAppConnPool().getConnection();
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(
                ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS).build();
            final SearchResult searchResult = conn.search(dn, SearchScope.SUB,
                filter);

            final List<SearchResultEntry> searchEntries = searchResult
                .getSearchEntries();
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
                return list;
            }
            getLogger().error("Error reading scope accesses by parent", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        getLogger().debug("Found {} ScopeAccess object(s) for: {}",
            list.size(), parentUniqueId);
        return list;
    }

    @Override
    public GrantedPermission grantPermission(String scopeAccessUniqueId,
        GrantedPermission permission) {
        getLogger().debug("Granting Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<GrantedPermission> persister = LDAPPersister
                .getInstance(GrantedPermission.class);
            conn = getAppConnPool().getConnection();
            try {
                persister.add(permission, conn, scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                    // noop
                } else {
                    throw e;
                }
            }
            getLogger().debug("Granted Permission: {}", permission);
            audit.succeed();
            return persister.get(permission, conn, scopeAccessUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error granting permission", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public boolean removePermissionFromScopeAccess(Permission permission) {
        getLogger().debug("Remove Permission: {}", permission);
        final String dn = permission.getUniqueId();
        final Audit audit = Audit.log(permission.getAuditContext()).delete();
        deleteEntryAndSubtree(dn, audit);
        getLogger().debug("Removed Permission: {}", permission);
        audit.succeed();
        return true;
    }

    @Override
    public boolean updatePermissionForScopeAccess(Permission permission) {
        getLogger().debug("Updating Permission: {}", permission);
        LDAPConnection conn = null;
        Audit audit = Audit.log(permission).modify();
        try {
            final Permission po = permission;
            final LDAPPersister persister = LDAPPersister
                .getInstance(permission.getClass());
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
    public boolean updateScopeAccess(ScopeAccess scopeAccess) {
        getLogger().debug("Updating ScopeAccess: {}", scopeAccess);
        LDAPConnection conn = null;
        Audit audit = Audit.log(scopeAccess);
        try {
            conn = getAppConnPool().getConnection();
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            List<Modification> modifications = persister.getModifications(scopeAccess, true);
            audit.modify(modifications);
            persister.modify(scopeAccess, conn, null, true);
            getLogger().debug("Updated ScopeAccess: {}", scopeAccess);
            audit.succeed();
            return true;
        } catch (final LDAPException e) {
            getLogger().error("Error updating scope access", e);
            audit.fail();
            throw new IllegalStateException(e);
        } catch (final LDAPSDKRuntimeException e) {
            // noop
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
        return false;
    }

    private ScopeAccess addScopeAccess(LDAPConnection conn,
        String parentUniqueId, ScopeAccess scopeAccess) {
        getLogger().info("Adding ScopeAccess: {}", scopeAccess);
        try {
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            try {
                persister.add(scopeAccess, conn, parentUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                    // noop
                } else {
                    throw e;
                }
            }
            getLogger().info("Added ScopeAccess: {}", scopeAccess);
            return (ScopeAccess) persister.get(scopeAccess, conn, parentUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    private Permission decodePermission(
        final SearchResultEntry searchResultEntry) throws LDAPPersistException {
        Permission object = null;
        if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_DEFINEDPERMISSION)) {
            object = LDAPPersister.getInstance(DefinedPermission.class).decode(
                searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_GRANTEDPERMISSION)) {
            object = LDAPPersister.getInstance(GrantedPermission.class).decode(
                searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_DELEGATEDPERMISSION)) {
            object = LDAPPersister.getInstance(DelegatedPermission.class)
                .decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_PERMISSION)) {
            object = LDAPPersister.getInstance(Permission.class).decode(
                searchResultEntry);
        }
        return object;
    }

    private ScopeAccess decodeScopeAccess(
        final SearchResultEntry searchResultEntry) throws LDAPPersistException {
        ScopeAccess object = null;
        if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_USERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(UserScopeAccess.class).decode(
                searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_CLIENTSCOPEACCESS)) {
            object = LDAPPersister.getInstance(ClientScopeAccess.class).decode(
                searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_PASSWORDRESETSCOPEACCESS)) {
            object = LDAPPersister.getInstance(PasswordResetScopeAccess.class)
                .decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_RACKERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(RackerScopeAccess.class).decode(
                searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS)) {
            object = LDAPPersister
                .getInstance(DelegatedClientScopeAccess.class).decode(
                    searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(
            OBJECTCLASS_SCOPEACCESS)) {
            object = LDAPPersister.getInstance(ScopeAccess.class).decode(
                searchResultEntry);
        }
        return object;
    }

    private Filter getFilterForPermission(Permission permission) {

        try {
            LDAPPersister persister = LDAPPersister.getInstance(permission
                .getClass());
            return persister.getObjectHandler().createFilter(permission);
        } catch (Exception e) {
            return Filter.createEqualityFilter(ATTR_OBJECT_CLASS,
                OBJECTCLASS_PERMISSION);
        }

    }
}
