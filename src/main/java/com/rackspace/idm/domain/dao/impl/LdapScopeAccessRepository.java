package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.DaoGetEntityType;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.stereotype.Component;

@Component
public class LdapScopeAccessRepository extends LdapGenericRepository<ScopeAccess> implements ScopeAccessDao, DaoGetEntityType {

    @Override
    public String getBaseDn() {
        return SCOPE_ACCESS_BASE_DN;
    }

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_SCOPEACCESS;
    }

    @Override
    public String[] getSearchAttributes() {
        return ATTR_SCOPE_ACCESS_ATTRIBUTES;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ACCESS_TOKEN;
    }

    @Override
    public void addScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        addObject(addLdapContainer(object.getUniqueId(), LdapRepository.CONTAINER_TOKENS), scopeAccess);
    }

    @Override
    public void deleteScopeAccess(ScopeAccess scopeAccess) {
        deleteObject(scopeAccess);
    }

    @Override
    public void updateScopeAccess(ScopeAccess scopeAccess) {
        updateObject(scopeAccess);
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUser(BaseUser user, String impersonatingUsername) {
        return getObjects(searchFilterGetAllImpersonatedScopeAccessesForUser(impersonatingUsername), user.getUniqueId());
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUser(BaseUser user) {
        return getObjects(searchFilterGetAllImpersonatedScopeAccess(), user.getUniqueId());
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessForUser(BaseUser user, String impersonatingUsername) {
        return getMostRecentScopeAccess(user, searchFilterGetAllImpersonatedScopeAccessesForUser(impersonatingUsername));
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId) {
        return getMostRecentScopeAccess(object, searchFilterGetScopeAccessesByClientId(clientId));
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        return getObject(searchFilterGetScopeAccessByAccessToken(accessToken), SearchScope.SUB);
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessForUser(User user) {
        return getMostRecentScopeAccess(user, searchFilterGetScopeAccesses());
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByUserId(String userId) {
        return getObjects(searchFilterGetScopeAccessByUserId(userId));
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        return getObject(searchFilterGetScopeAccessByRefreshToken(refreshToken), SearchScope.SUB);
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccesses(UniqueId object) {
        return getObjects(searchFilterGetScopeAccesses(), object.getUniqueId());
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId) {
        return getObjects(searchFilterGetScopeAccessesByClientId(clientId));
    }

    @Override
    public String getClientIdForParent(ScopeAccess scopeAccess) {
        String parentDn = null;
        try {
            parentDn = scopeAccess.getLDAPEntry().getParentDN().getParentString();
        } catch (LDAPException e) {
            //noop
        }
        return parseDNForClientId(parentDn);
    }

    @Override
    public String getUserIdForParent(ScopeAccess scopeAccess) {
        return scopeAccess.getLDAPEntry().getAttributeValue(LdapRepository.ATTR_UID);
    }

    private String parseDNForClientId(String parentDn) {
        String clientId = null;
        try {
            if(parentDn != null) {
                String[] DN = parentDn.split(",");
                if(DN.length > 0) {
                    clientId = DN[0].split("=")[1];
                }
            }
        } catch (Exception e) {
            //noop
        }

        return clientId;
    }

    private ScopeAccess getMostRecentScopeAccess(UniqueId object, Filter filter) throws NotFoundException {
        Iterable<ScopeAccess> scopeAccessList = getObjects(filter, object.getUniqueId());

        if (!scopeAccessList.iterator().hasNext()) {
            return null;
        }

        ScopeAccess mostRecentScopeAccess = null;
        for (ScopeAccess scopeAccess : scopeAccessList) {
            if (mostRecentScopeAccess == null || mostRecentScopeAccess.getAccessTokenExp().before(scopeAccess.getAccessTokenExp())) {
                mostRecentScopeAccess = scopeAccess;
            }
        }

        return mostRecentScopeAccess;

    }

    private Filter searchFilterGetAllImpersonatedScopeAccess() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IMPERSONATEDSCOPEACCESS).build();
    }

    private Filter searchFilterGetAllImpersonatedScopeAccessesForUser(String impersonatingUsername) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IMPERSONATEDSCOPEACCESS)
                .addEqualAttribute(ATTR_IMPERSONATING_USERNAME, impersonatingUsername).build();
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

    @Override
    public Class getEntityType(SearchResultEntry entry) {
        //NOTE:!! order precedence is important. Base classes should be further down in the check list.
        if (entry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_USERSCOPEACCESS)) {
            return UserScopeAccess.class;
        }else if (entry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_CLIENTSCOPEACCESS)) {
            return ClientScopeAccess.class;
        } else if (entry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_PASSWORDRESETSCOPEACCESS)) {
            return PasswordResetScopeAccess.class;
        } else if (entry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_RACKERSCOPEACCESS)) {
            return RackerScopeAccess.class;
        } else if (entry.getAttribute(ATTR_OBJECT_CLASS).hasValue(OBJECTCLASS_IMPERSONATEDSCOPEACCESS)) {
            return ImpersonatedScopeAccess.class;
        } else {
            return ScopeAccess.class;
        }
    }
}
