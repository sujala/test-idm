package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.DaoGetEntityType;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LdapIdentityUserRepository extends LdapGenericRepository<BaseUser> implements IdentityUserDao, DaoGetEntityType {
    private static Filter PROVISIONED_USER_CLASS_FILTER = Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON);
    private static Filter FEDERATED_USER_CLASS_FILTER = Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACE_FEDERATED_PERSON);
    private static Filter RACKER_USER_CLASS_FILTER = Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKER);

    private static List<Filter> PROVISIONED_USER_CLASS_FILTERS = Arrays.asList(PROVISIONED_USER_CLASS_FILTER);
    private static List<Filter> FEDERATED_USER_CLASS_FILTERS = Arrays.asList(FEDERATED_USER_CLASS_FILTER);
    private static List<Filter> RACKER_USER_CLASS_FILTERS = Arrays.asList(RACKER_USER_CLASS_FILTER);

    private static List<Filter> ENDUSER_CLASS_FILTERS = Arrays.asList(PROVISIONED_USER_CLASS_FILTER, FEDERATED_USER_CLASS_FILTER);
    private static List<Filter> ALLUSER_CLASS_FILTERS = Arrays.asList(PROVISIONED_USER_CLASS_FILTER, FEDERATED_USER_CLASS_FILTER, RACKER_USER_CLASS_FILTER);

    @Autowired
    private LdapUserRepository userDao;

    @Autowired
    private LdapFederatedUserRepository fedUserDao;

    @Autowired
    private LdapGroupRepository groupDao;

    public User getProvisionedUserById(String userId) {
        return searchForUserById(userId, PROVISIONED_USER_CLASS_FILTERS, User.class);
    }

    public FederatedUser getFederatedUserById(String userId) {
        return searchForUserById(userId, FEDERATED_USER_CLASS_FILTERS, FederatedUser.class);
    }

    @Override
    public EndUser getEndUserById(String userId) {
        return searchForUserById(userId, ENDUSER_CLASS_FILTERS, EndUser.class);
    }

    @Override
    public Iterable<EndUser> getEndUsersByDomainId(String domainId) {
        return searchForUsersByDomainId(domainId, ENDUSER_CLASS_FILTERS, EndUser.class);
    }

    @Override
    public PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit) {
        return searchForUsersByDomainIdPaged(domainId, ENDUSER_CLASS_FILTERS, EndUser.class, offset, limit);
    }

    @Override
    public PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit) {
        return searchForEnabledUsersPaged(ENDUSER_CLASS_FILTERS, EndUser.class, offset, limit);
    }

    @Override
    public Iterable<Group> getGroupsForEndUser(String userId) {
        getLogger().debug("Inside getGroupsForUser {}", userId);

        List<Group> groups = new ArrayList<Group>();

        EndUser user = getEndUserById(userId);

        if(user != null){
            for (String groupId : user.getRsGroupId()) {
                groups.add(groupDao.getGroupById(groupId));
            }
        }
        return groups;
    }

    @Override
    public Iterable<EndUser> getEnabledEndUsersByGroupId(String groupId) {
        return (Iterable) getObjects(searchFilterGetEnabledEndUsersByGroupId(groupId));
    }

    private <T extends EndUser> T searchForUserById(String userId, List<Filter> userClassFilterList, Class<T> clazz) {
        return (T) getObject(searchFilterGetUserById(userId, userClassFilterList), SearchScope.SUB);
    }

    private <T extends EndUser> Iterable<T> searchForUsersByDomainId(String domainId, List<Filter> userClassFilterList, Class<T> clazz) {
        return (Iterable) getObjects(searchFilterGetUserByDomainId(domainId, userClassFilterList));
    }

    private <T extends EndUser> PaginatorContext<T> searchForUsersByDomainIdPaged(String domainId, List<Filter> userClassFilterList, Class<T> clazz, int offset, int limit) {
        return (PaginatorContext) getObjectsPaged(searchFilterGetUserByDomainId(domainId, userClassFilterList), offset, limit);
    }

    private <T extends EndUser> PaginatorContext<T> searchForEnabledUsersPaged(List<Filter> userClassFilterList, Class<T> clazz, int offset, int limit) {
        return (PaginatorContext) getObjectsPaged(searchFilterGetEnabledUsers(userClassFilterList), offset, limit);
    }

    @Override
    public String getBaseDn(){
        return IDENTITY_USER_BASE_DN;
    }

    @Override
    public Class getEntityType(SearchResultEntry entry) {
        //NOTE:!! order precedence is important. Base classes should be further down in the check list.
        Attribute objClass = entry.getAttribute(ATTR_OBJECT_CLASS);
        if (objClass.hasValue(OBJECTCLASS_RACKSPACEPERSON)) {
            return User.class;
        } else if (objClass.hasValue(OBJECTCLASS_RACKSPACE_FEDERATED_PERSON)) {
            return FederatedUser.class;
        } else if (objClass.hasValue(OBJECTCLASS_RACKER)) {
            return Racker.class;
        } else {
            throw new IllegalStateException("Unrecognized user entity type");
        }
    }

    private Filter searchFilterGetUserById(String id, List<Filter> userClassFilterList) {
        return Filter.createANDFilter(
                Filter.createORFilter(userClassFilterList),
                Filter.createEqualityFilter(ATTR_ID, id)
        );
    }

    private Filter searchFilterGetUserByDomainId(String domainId, List<Filter> userClassFilterList) {
        return Filter.createANDFilter(
                Filter.createORFilter(userClassFilterList),
                Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId)
        );
    }

    private Filter searchFilterGetEnabledEndUsersByGroupId(String groupId) {
        return Filter.createORFilter(searchFilterGetEnabledUserByGroupId(groupId), searchFilterGetFederatedUsersByGroupId(groupId));

    }

        private Filter searchFilterGetEnabledUserByGroupId(String groupId) {
        return Filter.createANDFilter(
                Filter.createEqualityFilter(ATTR_GROUP_ID, groupId),
                Filter.createANDFilter(PROVISIONED_USER_CLASS_FILTER),
                Filter.createEqualityFilter(ATTR_ENABLED, Boolean.toString(true).toUpperCase())
        );
    }

    private Filter searchFilterGetFederatedUsersByGroupId(String groupId) {
        return Filter.createANDFilter(
                Filter.createEqualityFilter(ATTR_GROUP_ID, groupId),
                Filter.createANDFilter(FEDERATED_USER_CLASS_FILTER)
                );
    }

    private Filter searchFilterGetEnabledUsers(List<Filter> userClassFilterList) {
        return Filter.createANDFilter(
                Filter.createORFilter(userClassFilterList),
                Filter.createORFilter(
                    Filter.createEqualityFilter(ATTR_ENABLED, Boolean.toString(true).toUpperCase()),
                    Filter.createNOTFilter(Filter.createPresenceFilter(ATTR_ENABLED))
                )
        );
    }

    @Override
    public void doPreEncode(BaseUser object) {
        /*
         * For backwards compatibility, call the encode method on main user based repo
         */
        if (object instanceof User) {
            userDao.doPreEncode((User) object);
        }
    }

    @Override
    public void doPostEncode(BaseUser object) {
        /*
         * For backwards compatibility, call the encode method on main user based repo
         */
        if (object instanceof User) {
            userDao.doPostEncode((User) object);
        }
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }


        @Override
    public void addObject(BaseUser object) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void addObject(String dn, BaseUser object) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void updateObjectAsIs(BaseUser object) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void updateObject(BaseUser object) {
        if (object instanceof User) {
            userDao.updateObject((User) object);
        }
        else if (object instanceof FederatedUser) {
            fedUserDao.updateObject((FederatedUser) object);
        } else {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    @Override
    public void deleteObject(Filter searchFilter) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void softDeleteObject(BaseUser object) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void unSoftDeleteObject(BaseUser object) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void deleteObject(BaseUser object) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getNextId() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getSoftDeletedBaseDn() {
        throw new UnsupportedOperationException("Not supported");
    }
}
