package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.exception.NotFoundException;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class LdapScopeAccessRepository extends LdapGenericRepository<ScopeAccess> implements ScopeAccessDao {

    public static final String FIND_SCOPE_ACCESS_FOR_PARENT_BY_CLIENT_ID = "Find ScopeAccess for Parent: {} by ClientId: {}";
    public static final String ERROR_READING_SCOPE_ACCESS_BY_CLIENT_ID = "Error reading scope access by clientId";

    public String getBaseDn() {
        return BASE_DN;
    }

    public String getLdapEntityClass() {
        return OBJECTCLASS_SCOPEACCESS;
    }

    public String[] getSearchAttributes() {
        return ATTR_SCOPE_ACCESS_ATTRIBUTES;
    }

    public String getSortAttribute() {
        return ATTR_ACCESS_TOKEN;
    }

    @Override
    public void addScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        addObject(object.getUniqueId(), scopeAccess);
    }

    @Override
    public void deleteScopeAccess(ScopeAccess scopeAccess) {
        deleteObject(scopeAccess);
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
    public List<ScopeAccess> getAllImpersonatedScopeAccessForParentByUser(String parentUniqueId, String username) {
        getLogger().debug("Find ScopeAccess for Parent: {} by impersonating username: {}", parentUniqueId, username);
        final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IMPERSONATEDSCOPEACCESS)
                .addEqualAttribute(ATTR_IMPERSONATING_USERNAME, username).build();

        return getMultipleImpersonatedScopeAccess(parentUniqueId, filter);
    }

    @Override
    public List<ScopeAccess> getAllImpersonatedScopeAccessForUser(User user) {
        final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IMPERSONATEDSCOPEACCESS).build();

        return getObjects(filter, user.getUniqueId());
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
            Filter filter = searchFilterGetScopeAccessesByClientId(clientId);

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
    public ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId) {
        return getMostRecentScopeAccess(getObjects(searchFilterGetScopeAccessesByClientId(clientId), object.getUniqueId()));
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
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        return getObjects(searchFilterGetScopeAccessByAccessToken(accessToken)).get(0);
    }

    @Override
    public ScopeAccess getScopeAccessByUserId(String userId) {
        return getObject(searchFilterGetScopeAccessByUserId(userId));
    }

    public List<ScopeAccess> getScopeAccessListByUserId(String userId) {
        return getObjects(searchFilterGetScopeAccessByUserId(userId));
    }

    @Override
    public DelegatedClientScopeAccess getDelegatedScopeAccessByAuthorizationCode(String authorizationCode) {
        return (DelegatedClientScopeAccess) getObject(searchFilterGetDelegatedScopeAccessByAuthorizationCode(authorizationCode));
    }

    private Filter searchFilterGetDelegatedScopeAccessByAuthorizationCode(String authorizationCode) {
        return new LdapSearchBuilder()
                    .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS)
                    .addEqualAttribute(ATTR_AUTH_CODE, authorizationCode).build();
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        return getObject(searchFilterGetScopeAccessByRefreshToken(refreshToken));
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
    public List<ScopeAccess> getScopeAccesses(UniqueId object) {
        return getObjects(searchFilterGetScopeAccesses(), object.getUniqueId());
    }

    @Override
    public List<ScopeAccess> getScopeAccessesByParentAndClientId(String parentUniqueId, String clientId) {
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
    public void updateScopeAccess(ScopeAccess scopeAccess) {
        updateObject(scopeAccess);
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

    private Filter searchFilterGetScopeAccesses() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS).build();
    }

    private Filter searchFilterGetScopeAccessByAccessToken(String accessToken) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken).build();
    }

    private Filter searchFilterGetScopeAccessByRefreshToken(String refreshToken) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_REFRESH_TOKEN, refreshToken).build();
    }

    private Filter searchFilterGetScopeAccessByUserId(String userId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ATTR_USER_RS_ID, userId).build();
    }

    private Filter searchFilterGetScopeAccessesByClientId(String clientId) {
        return Filter.createANDFilter(
                Filter.createORFilter(
                        Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENTSCOPEACCESS),
                        Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKERSCOPEACCESS),
                        Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_PASSWORDRESETSCOPEACCESS),
                        Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_USERSCOPEACCESS)
                ),
                Filter.createEqualityFilter(ATTR_CLIENT_ID, clientId)
        );
    }
}
