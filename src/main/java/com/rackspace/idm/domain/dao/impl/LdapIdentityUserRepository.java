package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@LDAPComponent
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
    private UserDao userDao;

    @Autowired
    private FederatedUserDao fedUserDao;

    @Autowired
    private GroupDao groupDao;

    @Override
    public String[] getSearchAttributes() {
        return ATTR_PROV_FED_USER_SEARCH_ATTRIBUTES_NO_PWD_HIS;
    }

    public User getProvisionedUserById(String userId) {
        return searchForUserById(userId, PROVISIONED_USER_CLASS_FILTERS, User.class);
    }

    public FederatedUser getFederatedUserById(String userId) {
        return searchForUserById(userId, FEDERATED_USER_CLASS_FILTERS, FederatedUser.class);
    }

    public FederatedUser getFederatedUserByUsernameAndIdpId(String username, String idpId) {
        return searchForUserByUsername(username, FEDERATED_USER_CLASS_FILTERS, FederatedUser.class, getBaseDnWithIdpName(idpId));
    }

    @Override
    public EndUser getEndUserById(String userId) {
        return searchForUserById(userId, ENDUSER_CLASS_FILTERS, EndUser.class);
    }

    @Override
    public User getProvisionedUserByIdWithPwdHis(String userId) {
        return searchForUserByIdWithPwdHis(userId, PROVISIONED_USER_CLASS_FILTERS, User.class);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String idpId) {
        return fedUserDao.getFederatedUsersByDomainIdAndIdentityProviderId(domainId, idpId);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByIdentityProviderId(String idpId) {
        return fedUserDao.getFederatedUsersByIdentityProviderId(idpId);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(List<String> approvedDomainIds, String idpId) {
        return fedUserDao.getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(approvedDomainIds, idpId);
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String idpId) {
        return fedUserDao.getFederatedUsersByDomainIdAndIdentityProviderIdCount(domainId, idpId);
    }

    @Override
    public int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String idpId) {
        return fedUserDao.getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(domainId, idpId);
    }

    @Override
    public Iterable<EndUser> getEndUsersByDomainId(String domainId) {
        return searchForUsersByDomainId(domainId, ENDUSER_CLASS_FILTERS, EndUser.class);
    }

    @Override
    public Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        return (Iterable) getObjects(searchFilterGetEnabledUsersByDomainIdAndEnabledFlag(domainId, enabled));
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

    @Override
    public int getUsersWithinRegionCount(String regionName) {
        return countUsersWithinRegion(regionName, ENDUSER_CLASS_FILTERS);
    }

    @Override
    public PaginatorContext<EndUser> getEndUsersInUserGroupPaged(UserGroup group, UserSearchCriteria userSearchCriteria) {
        return (PaginatorContext) getObjectsPaged(searchFilterGetUsersInUserGroup(group, ENDUSER_CLASS_FILTERS),
                userSearchCriteria.getPaginationRequest().getEffectiveMarker(),
                userSearchCriteria.getPaginationRequest().getEffectiveLimit());
    }

    @Override
    public Iterable<EndUser> getEndUsersInUserGroup(UserGroup group) {
        return (Iterable) getObjects(searchFilterGetUsersInUserGroup(group, ENDUSER_CLASS_FILTERS));
    }

    private <T extends BaseUser> T searchForUserById(String userId, List<Filter> userClassFilterList, Class<T> clazz) {
        return (T) getObject(searchFilterGetUserById(userId, userClassFilterList), SearchScope.SUB);
    }

    private <T extends BaseUser> T searchForUserByIdWithPwdHis(String userId, List<Filter> userClassFilterList, Class<T> clazz) {
        return (T) getObject(searchFilterGetUserById(userId, userClassFilterList), USERS_BASE_DN, SearchScope.SUB, LdapRepository.ATTR_USER_SEARCH_ATTRIBUTES);
    }

    private <T extends EndUser> T searchForUserByUsername(String username, List<Filter> userClassFilterList, Class<T> clazz, String baseDn) {
        return (T) getObject(searchFilterGetUserByUsername(username, userClassFilterList), baseDn, SearchScope.SUB);
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

    private int countUsersWithinRegion(String regionName, List<Filter> userClassFilterList) {
        return countObjects(searchFilterGetEndUsersByRegionName(regionName, userClassFilterList));
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
        List<Filter> idMatchFilters = new ArrayList<Filter>();
        if (userClassFilterList.contains(RACKER_USER_CLASS_FILTER)) {
            idMatchFilters.add(getRackerFilter(id));
        }

        if (userClassFilterList.contains(PROVISIONED_USER_CLASS_FILTER) || userClassFilterList.contains(FEDERATED_USER_CLASS_FILTER)) {
            idMatchFilters.add(getEndUserFilter(userClassFilterList, id));
        }

        if (CollectionUtils.isEmpty(idMatchFilters)) {
            throw new IllegalArgumentException("Must provide at least one userClassFilter");
        }

        return Filter.createORFilter(idMatchFilters);
    }

    private Filter getRackerFilter(String rackerId) {
        return Filter.createANDFilter(
                RACKER_USER_CLASS_FILTER,
                Filter.createEqualityFilter(ATTR_RACKER_ID, rackerId)
        );
    }

    private Filter getEndUserFilter(List<Filter> userClassFilterList, String userId) {
        List<Filter> endUserFilters = new ArrayList<Filter>();
        if (userClassFilterList.contains(PROVISIONED_USER_CLASS_FILTER)) {
            endUserFilters.add(PROVISIONED_USER_CLASS_FILTER);
        }
        if (userClassFilterList.contains(FEDERATED_USER_CLASS_FILTER)) {
            endUserFilters.add(FEDERATED_USER_CLASS_FILTER);
        }

        return Filter.createANDFilter(
                Filter.createORFilter(endUserFilters),
                Filter.createEqualityFilter(ATTR_ID, userId)
        );
    }

    private Filter searchFilterGetUserByUsername(String username, List<Filter> userClassFilterList) {
        return Filter.createANDFilter(
                Filter.createORFilter(userClassFilterList),
                Filter.createEqualityFilter(ATTR_UID, username)
        );
    }

    private Filter searchFilterGetUserByDomainId(String domainId, List<Filter> userClassFilterList) {
        return Filter.createANDFilter(
                Filter.createORFilter(userClassFilterList),
                Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId)
        );
    }

    private Filter searchFilterGetEnabledUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        //only query for federated users if you are searching for enabled users
        if(enabled) {
            return Filter.createORFilter(searchFilterGetFederatedUsersByDomainId(domainId), searchFilterGetUserByDomainIdAndEnabledFlag(domainId, enabled));
        } else {
            return searchFilterGetUserByDomainIdAndEnabledFlag(domainId, enabled);
        }
    }

    private Filter searchFilterGetUserByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        return Filter.createANDFilter(
                Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
                Filter.createANDFilter(PROVISIONED_USER_CLASS_FILTER),
                Filter.createEqualityFilter(ATTR_ENABLED, Boolean.toString(enabled).toUpperCase())
        );
    }

    private Filter searchFilterGetFederatedUsersByDomainId(String domainId) {
        return Filter.createANDFilter(
                Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
                Filter.createANDFilter(FEDERATED_USER_CLASS_FILTER)
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

    private Filter searchFilterGetEndUsersByRegionName(String regionName,  List<Filter> userClassFilterList) {
        return Filter.createANDFilter(
                Filter.createORFilter(userClassFilterList),
                Filter.createEqualityFilter(ATTR_REGION, regionName)
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

    private Filter searchFilterGetUsersInUserGroup(UserGroup userGroup, List<Filter> userClassFilterList) {
        return Filter.createANDFilter(
                Filter.createORFilter(userClassFilterList),
                Filter.createEqualityFilter(ATTR_USER_GROUP_DNS, userGroup.getUniqueId())
        );
    }

    private String getBaseDnWithIdpName(String idpName) {
        return String.format("ou=%s,ou=%s,%s", EXTERNAL_PROVIDERS_USER_CONTAINER_NAME, idpName, EXTERNAL_PROVIDERS_BASE_DN);
    }

    @Override
    public void doPreEncode(BaseUser object) {
        /*
         * For backwards compatibility, call the encode method on main user based repo
         */
        if (object instanceof User) {
            userDao.doPreEncode((User) object);
        } else if (object instanceof FederatedUser) {
            fedUserDao.doPreEncode((FederatedUser) object);
        }
    }

    @Override
    public void doPostEncode(BaseUser object) {
        /*
         * For backwards compatibility, call the encode method on main user based repo
         */
        if (object instanceof User) {
            userDao.doPostEncode((User) object);
        } else if (object instanceof FederatedUser) {
            fedUserDao.doPostEncode((FederatedUser) object);
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
    public void updateIdentityUser(BaseUser object) {
        if (object instanceof User) {
            userDao.updateUser((User) object);
        }
        else if (object instanceof FederatedUser) {
            fedUserDao.updateUser((FederatedUser) object);
        } else {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    @Override
    public void deleteIdentityUser(BaseUser baseUser) {
        if (baseUser instanceof User) {
            //delete regular provisioned user
            userDao.deleteUser((User) baseUser);
        }
        else if (baseUser instanceof FederatedUser) {
            //delete domain based federated user
            fedUserDao.deleteUser((FederatedUser) baseUser);
        } else {
            //the only other type of users are Rackers (Federated and Non-Federated) which are NOT persisted so no deletion necessary
            throw new UnsupportedOperationException("Not supported");
        }
    }

    @Override
    public void deleteObject(Filter searchFilter) {
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

}
