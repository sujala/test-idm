package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.exception.NotFoundException;
import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import com.unboundid.util.LDAPSDKRuntimeException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class LdapScopeAccessRepository extends LdapRepository implements ScopeAccessDao {

    public static final String FIND_SCOPE_ACCESS_FOR_PARENT_BY_CLIENT_ID = "Find ScopeAccess for Parent: {} by ClientId: {}";
    public static final String ERROR_READING_SCOPE_ACCESS_BY_CLIENT_ID = "Error reading scope access by clientId";

    @Override
    public ScopeAccess addDelegateScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        getLogger().info("Adding Delegate ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess).add();
        try{
            SearchResultEntry entry = getContainer( parentUniqueId, CONTAINER_TOKENS);
            if (entry == null) {
                addContainer( parentUniqueId, CONTAINER_TOKENS);
                entry = getContainer( parentUniqueId, CONTAINER_TOKENS);
            }

            audit.succeed();
            getLogger().debug("Added Delegate ScopeAccess: {}", scopeAccess);
            return addScopeAccess(entry.getDN(), scopeAccess);
        } catch (IllegalStateException e){
            getLogger().error("Error adding scope access object", e);
            audit.fail();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ScopeAccess addImpersonatedScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        getLogger().info("Adding Impersonated ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess).add();

        String dn = new LdapDnBuilder(parentUniqueId).build();
        try{
            SearchResultEntry entry = getContainer( dn, CONTAINER_TOKENS);
            if (entry == null) {
                addContainer( dn, CONTAINER_TOKENS);
                entry = getContainer( dn, CONTAINER_TOKENS);
            }
            audit.succeed();
            getLogger().debug("Added Impersonated ScopeAccess: {}", scopeAccess);
            return addScopeAccess(entry.getDN(), scopeAccess);
        } catch (final IllegalStateException e) {
            getLogger().error("Error adding scope access object", e);
            audit.fail();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ScopeAccess addDirectScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        getLogger().info("Adding Delegate ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess).add();
        try {
            SearchResultEntry entry = getContainer( parentUniqueId, LdapRepository.CONTAINER_TOKENS);
            if (entry == null) {
                addContainer(parentUniqueId, LdapRepository.CONTAINER_TOKENS);
                entry = getContainer( parentUniqueId, LdapRepository.CONTAINER_TOKENS);
            }

            audit.succeed();
            getLogger().debug("Added Delegate ScopeAccess: {}", scopeAccess);
            return addScopeAccess(entry.getDN(), scopeAccess);
        } catch (final IllegalStateException e) {
            getLogger().error("Error adding scope acccess object", e);
            audit.fail();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public DefinedPermission definePermission(String scopeAccessUniqueId,
        DefinedPermission permission) {
        getLogger().debug("Defining Permission: {}", permission);
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<DefinedPermission> persister = LDAPPersister.getInstance(DefinedPermission.class);
            try {
                persister.add(permission, getAppInterface(), scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() != ResultCode.ENTRY_ALREADY_EXISTS) {
                    throw e;
                }
            }
            getLogger().debug("Defined Permission: {}", permission);
            audit.succeed();
            return persister.get(permission, getAppInterface(), scopeAccessUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error defining permission", e);
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public DelegatedPermission delegatePermission(String scopeAccessUniqueId,
        DelegatedPermission permission) {
        getLogger().debug("Delegating Permission: {}", permission);
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<DelegatedPermission> persister = LDAPPersister.getInstance(DelegatedPermission.class);
            try {
                persister.add(permission, getAppInterface(), scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() != ResultCode.ENTRY_ALREADY_EXISTS) {
                    throw e;
                }
            }
            getLogger().debug("Delegated Permission: {}", permission);
            audit.succeed();
            return persister.get(permission, getAppInterface(), scopeAccessUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error delegating permission", e);
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteScopeAccess(ScopeAccess scopeAccess) {
        getLogger().info("Deleting ScopeAccess: {}", scopeAccess);
        final String dn = scopeAccess.getUniqueId();
        final Audit audit = Audit.log(scopeAccess.getAuditContext()).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted ScopeAccess: {}", scopeAccess);
        return true;
    }

    @Override
    public boolean doesAccessTokenHavePermission(ScopeAccess token, Permission permission) {
        getLogger().debug("Checking Permission: {}", permission);

        Permission perm = new Permission(permission.getCustomerId(), permission.getClientId(), permission.getPermissionId());
        try {
            String dn = token instanceof DelegatedClientScopeAccess ? token
                .getUniqueId() : token.getLDAPEntry().getParentDNString();

            final Permission result = LDAPPersister.getInstance(Permission.class)
                    .searchForObject(perm, getAppInterface(), dn, SearchScope.SUB);
            getLogger().debug("{} : {}", result != null ? "Found" : "Did not find", perm);
            return result != null;
        } catch (final LDAPException e) {
            getLogger().error("Error checking permission", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doesParentHaveScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(scopeAccess.getClientId());

        try {
            final ScopeAccess result = LDAPPersister.getInstance(
                ScopeAccess.class).searchForObject(sa, getAppInterface(), parentUniqueId, SearchScope.ONE);
            getLogger().debug("{} : {}",
                result == null ? "Found" : "Did not find", sa);
            return result != null;
        } catch (final LDAPException e) {
            getLogger().error("Error checking permission", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public ScopeAccess getDelegateScopeAccessForParentByClientId(String parentUniqueId, String clientId) {
        getLogger().debug(FIND_SCOPE_ACCESS_FOR_PARENT_BY_CLIENT_ID, parentUniqueId, clientId);

        String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME, CONTAINER_TOKENS).build();

        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(dn, SearchScope.SUB, filter);
            getLogger().debug(
                "Found {} ScopeAccess(s) for Parent: {} by ClientId: {}",
                new Object[]{searchEntries.size(), parentUniqueId, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            if (e.getResultCode() != ResultCode.NO_SUCH_OBJECT) {
                getLogger().error(ERROR_READING_SCOPE_ACCESS_BY_CLIENT_ID, e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public List<ScopeAccess> getAllImpersonatedScopeAccessForParentByUser(String parentUniqueId, String username) {
        getLogger().debug("Find ScopeAccess for Parent: {} by impersonating username: {}", parentUniqueId, username);
        final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IMPERSONATEDSCOPEACCESS)
                .addEqualAttribute(ATTR_IMPERSONATING_USERNAME, username).build();

        return getMultipleImpersonatedScopeAccess(parentUniqueId, filter);
    }

    @Override
    public List<ScopeAccess> getAllImpersonatedScopeAccessForParent(String parentUniqueId) {
        getLogger().debug("Finding impersonatedScopeAccess for Parent {}", parentUniqueId);
        final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IMPERSONATEDSCOPEACCESS).build();

        return getMultipleImpersonatedScopeAccess(parentUniqueId, filter);
    }

    private List<ScopeAccess> getMultipleImpersonatedScopeAccess(String searchDN, Filter filter) {
        String dn = new LdapDnBuilder(searchDN).addAttribute(ATTR_NAME, CONTAINER_TOKENS).build();
        List<ScopeAccess> scopeAccessList = new ArrayList<ScopeAccess>();

        try {
            final List<SearchResultEntry> searchEntries = getMultipleEntries(dn, SearchScope.SUB, filter);
            getLogger().debug("Found {} impersonatedScopeAccess for parent {}", new Object[]{searchEntries.size(), searchDN});
            for (final SearchResultEntry entry : searchEntries) {
                scopeAccessList.add(decodeScopeAccess(entry));
            }
        } catch (LDAPPersistException e) {
            if (e.getResultCode() != ResultCode.NO_SUCH_OBJECT) {
                getLogger().error(ERROR_READING_SCOPE_ACCESS_BY_CLIENT_ID, e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return scopeAccessList;
    }

    @Override
    public List<ScopeAccess> getDirectScopeAccessForParentByClientId(String parentUniqueId, String clientId) {
        getLogger().debug(FIND_SCOPE_ACCESS_FOR_PARENT_BY_CLIENT_ID, parentUniqueId, clientId);

        List<ScopeAccess> objectList = new ArrayList<ScopeAccess>();
        String dn = new LdapDnBuilder(parentUniqueId).build();

        try {
            final Filter filter = Filter.createANDFilter(
                    Filter.createORFilter(
                            Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTSCOPEACCESS),
                            Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKERSCOPEACCESS),
                            Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_PASSWORDRESETSCOPEACCESS),
                            Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_USERSCOPEACCESS)
                    ),
                    Filter.createEqualityFilter(ATTR_CLIENT_ID, clientId)
            );

            final List<SearchResultEntry> searchEntries = getMultipleEntries(dn, SearchScope.SUB, filter);
            getLogger().debug(
                "Found {} ScopeAccess(s) for Parent: {} by ClientId: {}",
                new Object[]{searchEntries.size(), parentUniqueId, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                objectList.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            if (e.getResultCode() != ResultCode.NO_SUCH_OBJECT) {
                getLogger().error(ERROR_READING_SCOPE_ACCESS_BY_CLIENT_ID, e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return objectList;
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessByParentForUser(String parentUniqueId, String username) {
        List<ScopeAccess> scopeAccessList = getAllImpersonatedScopeAccessForParentByUser(parentUniqueId, username);
        ScopeAccess scopeAccess;
        try {
            scopeAccess =  getMostRecentScopeAccess(scopeAccessList);
        } catch (NotFoundException ex) {
            scopeAccess = null;
        }
        return scopeAccess;
    }

    @Override
    public ScopeAccess getMostRecentDirectScopeAccessForParentByClientId(String parentUniqueId, String clientId) {
        List<ScopeAccess> scopeAccessList = getDirectScopeAccessForParentByClientId(parentUniqueId, clientId);
        return getMostRecentScopeAccess(scopeAccessList);
    }

    private ScopeAccess getMostRecentScopeAccess(List<ScopeAccess> scopeAccessList) throws NotFoundException {
        int mostRecentIndex = 0;

        if (scopeAccessList.size() == 0) {
            String errMsg = "Scope access not found.";
            getLogger().warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        for (int i = 0; i < scopeAccessList.size(); i++) {
            Date max = scopeAccessList.get(mostRecentIndex).getAccessTokenExp();
            Date current = scopeAccessList.get(i).getAccessTokenExp();
            if (max.before(current)) {
                mostRecentIndex = i;
            }
        }
        return scopeAccessList.get(mostRecentIndex);
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

        final List<Permission> list = new ArrayList<Permission>();

        try {
            final Filter filter = getFilterForPermission(permission);

            final List<SearchResultEntry> searchEntries = getMultipleEntries(parentUniqueId, SearchScope.SUB, filter);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodePermission(searchResultEntry));
            }

        } catch (final LDAPException e) {
            getLogger().error(
                "Error reading permission by parent and permission", e);
            throw new IllegalStateException(e.getMessage(), e);
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
        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(BASE_DN, SearchScope.SUB, filter, ATTR_SCOPE_ACCESS_ATTRIBUTES);
            if(searchEntries != null){
                getLogger().debug("Found {} ScopeAccess by AccessToken: {}", searchEntries.size(), accessToken);
                for (final SearchResultEntry searchResultEntry : searchEntries) {
                    return decodeScopeAccess(searchResultEntry);
                }
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading ScopeAccess by AccessToken: " + accessToken, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public ScopeAccess getScopeAccessByUserId(String userId) {
        getLogger().debug("Find ScopeAccess by user id: {}", userId);
        try {
            final Filter filter = new LdapSearchBuilder()
                    .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                    .addEqualAttribute(ATTR_USER_RS_ID, userId).build();
            final List<SearchResultEntry> searchEntries = getMultipleEntries(BASE_DN, SearchScope.SUB, filter);
            getLogger().debug("Found {} ScopeAccess by user id: {}", searchEntries.size(), userId);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading ScopeAccess by user id: " + userId, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    public List<ScopeAccess> getScopeAccessListByUserId(String userId) {
        getLogger().debug("Find ScopeAccess by user id: {}", userId);
        List<ScopeAccess> scopeAccessList = new ArrayList<ScopeAccess>();
        try {
            final Filter filter = new LdapSearchBuilder()
                    .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                    .addEqualAttribute(ATTR_USER_RS_ID, userId).build();
            final List<SearchResultEntry> searchEntries = getMultipleEntries(BASE_DN, SearchScope.SUB, filter);
            getLogger().debug("Found {} ScopeAccess by user id: {}", searchEntries.size(), userId);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                scopeAccessList.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading ScopeAccess by user id: " + userId, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return scopeAccessList;
    }

    @Override
    public DelegatedClientScopeAccess getScopeAccessByAuthorizationCode(String authorizationCode) {
        getLogger().debug("Find ScopeAccess by Authorization Code: {}",
            authorizationCode);
        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS)
                .addEqualAttribute(ATTR_AUTH_CODE, authorizationCode).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(BASE_DN, SearchScope.SUB, filter);
            getLogger().debug("Found {} ScopeAccess by AccessToken: {}", searchEntries.size(), authorizationCode);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return (DelegatedClientScopeAccess) decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error(
                "Error reading ScopeAccess by Authorization Code: "
                    + authorizationCode, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        getLogger().debug("Find ScopeAccess by RefreshToken: {}", refreshToken);
        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_REFRESH_TOKEN, refreshToken).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(BASE_DN,SearchScope.SUB, filter);
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
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public ScopeAccess getScopeAccessByUsernameAndClientId(String username, String clientId) {
        getLogger().debug("Find ScopeAccess by Username: {} and ClientId: {}", username, clientId);
        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(BASE_DN, SearchScope.SUB, filter);
            getLogger().debug(
                "Found {}  ScopeAccess(s) by Username: {} and ClientId: {}",
                new Object[]{searchEntries.size(), username, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by username", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<DelegatedClientScopeAccess> getDelegatedClientScopeAccessByUsername(String username) {
        getLogger().debug("Find ScopeAccess by Username: {}", username);
        List<DelegatedClientScopeAccess> scopeAccessList = null;
        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS)
                .addEqualAttribute(ATTR_UID, username).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(BASE_DN, SearchScope.SUB, filter);
            getLogger().debug("Found {}  ScopeAccess(s) by Username: {}",
                new Object[]{searchEntries.size(), username});

            scopeAccessList = new ArrayList<DelegatedClientScopeAccess>();

            for (final SearchResultEntry searchResultEntry : searchEntries) {
                DelegatedClientScopeAccess scopeAccess = (DelegatedClientScopeAccess) decodeScopeAccess(searchResultEntry);
                scopeAccessList.add(scopeAccess);
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope access by username", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return scopeAccessList;
    }

    @Override
    public List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId) {
        getLogger().debug("Finding ScopeAccesses for: {}", parentUniqueId);
        final List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        try {
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(
                ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(parentUniqueId, SearchScope.SUB, filter);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error("Error reading scope accesses by parent", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().debug("Found {} ScopeAccess object(s) for: {}",
            list.size(), parentUniqueId);
        return list;
    }

    @Override
    public ScopeAccess getScopeAccessByParentAndClientId(String parentUniqueId,
        String clientId) {
        getLogger().debug(FIND_SCOPE_ACCESS_FOR_PARENT_BY_CLIENT_ID,
            parentUniqueId, clientId);

        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(parentUniqueId,SearchScope.SUB, filter);
            getLogger().debug(
                "Found {} ScopeAccess(s) for Parent: {} by ClientId: {}",
                new Object[]{searchEntries.size(), parentUniqueId, clientId});
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                return decodeScopeAccess(searchResultEntry);
            }
        } catch (final LDAPException e) {
            getLogger().error(ERROR_READING_SCOPE_ACCESS_BY_CLIENT_ID, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<ScopeAccess> getScopeAccessesByParentAndClientId(
        String parentUniqueId, String clientId) {
        getLogger().debug(FIND_SCOPE_ACCESS_FOR_PARENT_BY_CLIENT_ID,
            parentUniqueId, clientId);
        final List<ScopeAccess> list = new ArrayList<ScopeAccess>();

        try {
            final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(parentUniqueId, SearchScope.SUB, filter);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            getLogger().error(ERROR_READING_SCOPE_ACCESS_BY_CLIENT_ID, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<ScopeAccess> getDelegateScopeAccessesByParent(
        String parentUniqueId) {
        getLogger().debug("Finding ScopeAccesses for: {}", parentUniqueId);
        final List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME, CONTAINER_TOKENS).build();
        try {
            final Filter filter = new LdapSearchBuilder().addEqualAttribute(
                ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS).build();

            final List<SearchResultEntry> searchEntries = getMultipleEntries(dn, SearchScope.SUB, filter);
            for (final SearchResultEntry searchResultEntry : searchEntries) {
                list.add(decodeScopeAccess(searchResultEntry));
            }
        } catch (final LDAPException e) {
            if (e.getResultCode() != ResultCode.NO_SUCH_OBJECT) {
                getLogger().error("Error reading scope accesses by parent", e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        getLogger().debug("Found {} ScopeAccess object(s) for: {}",
            list.size(), parentUniqueId);
        return list;
    }

    @Override
    public GrantedPermission grantPermission(String scopeAccessUniqueId,
        GrantedPermission permission) {
        getLogger().debug("Granting Permission: {}", permission);
        Audit audit = Audit.log(permission).add();
        try {
            final LDAPPersister<GrantedPermission> persister = LDAPPersister
                .getInstance(GrantedPermission.class);
            try {
                persister.add(permission, getAppInterface(), scopeAccessUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() != ResultCode.ENTRY_ALREADY_EXISTS) {
                    throw e;
                }
            }
            getLogger().debug("Granted Permission: {}", permission);
            audit.succeed();
            return persister.get(permission, getAppInterface(), scopeAccessUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error granting permission", e);
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
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
        Audit audit = Audit.log(permission).modify();
        try {
            final Permission po = permission;
            final LDAPPersister persister = LDAPPersister
                .getInstance(permission.getClass());
            final LDAPResult result = persister.modify(po, getAppInterface(), null, true);
            getLogger().debug("Updated Permission: {}", permission);
            audit.succeed();
            return result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE;
        } catch (final Exception e) {
            getLogger().error("Error updating permission", e);
            audit.fail();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean updateScopeAccess(ScopeAccess scopeAccess) {
        getLogger().debug("Updating ScopeAccess: {}", scopeAccess);
        Audit audit = Audit.log(scopeAccess);
        try {
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            List<Modification> modifications = persister.getModifications(scopeAccess, true);

            audit.modify(modifications);
            persister.modify(scopeAccess, getAppInterface(), null, true);
            getLogger().debug("Updated ScopeAccess: {}", scopeAccess);
            audit.succeed();
            return true;
        } catch (final LDAPException e) {
            getLogger().error("Error updating scope access", e);
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
        } catch (final LDAPSDKRuntimeException e) {
            // noop
        }
        return false;
    }

    @Override
    public void deleteScopeAccessByDn(String scopeAccessDn) {
        getLogger().debug("Deleting ScopeAccess: {}", scopeAccessDn);

        final Audit audit = Audit.log(scopeAccessDn).delete();
        deleteEntryAndSubtree(scopeAccessDn, audit);
        audit.succeed();
    }


    public ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess) {
        getLogger().info("Adding ScopeAccess: {}", scopeAccess);
        try {
            final LDAPPersister persister = LDAPPersister.getInstance(scopeAccess.getClass());
            try {
                persister.add(scopeAccess, getAppInterface(), parentUniqueId);
            } catch (final LDAPException e) {
                if (e.getResultCode() != ResultCode.ENTRY_ALREADY_EXISTS) {
                    throw e;
                }
            }
            getLogger().info("Added ScopeAccess: {}", scopeAccess);
            return (ScopeAccess) persister.get(scopeAccess, getAppInterface(), parentUniqueId);
        } catch (final LDAPException e) {
            getLogger().error("Error adding scope acccess object", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    Permission decodePermission(
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

    ScopeAccess decodeScopeAccess(
        final SearchResultEntry searchResultEntry) throws LDAPPersistException {
        ScopeAccess object = null;
        if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_USERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(UserScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_CLIENTSCOPEACCESS)) {
            object = LDAPPersister.getInstance(ClientScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_PASSWORDRESETSCOPEACCESS)) {
            object = LDAPPersister.getInstance(PasswordResetScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_RACKERSCOPEACCESS)) {
            object = LDAPPersister.getInstance(RackerScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS)) {
            object = LDAPPersister.getInstance(DelegatedClientScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_IMPERSONATEDSCOPEACCESS)) {
            object = LDAPPersister.getInstance(ImpersonatedScopeAccess.class).decode(searchResultEntry);
        } else if (searchResultEntry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_SCOPEACCESS)) {
            object = LDAPPersister.getInstance(ScopeAccess.class).decode(searchResultEntry);
        }
        return object;
    }

    Filter getFilterForPermission(Permission permission) {

        try {
            LDAPPersister persister = LDAPPersister.getInstance(permission.getClass());
            return persister.getObjectHandler().createFilter(permission);
        } catch (Exception e) {
            return Filter.createEqualityFilter(ATTR_OBJECT_CLASS,
                OBJECTCLASS_PERMISSION);
        }

    }
}
