package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.dao.RackerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

// FIXME: Improve the query logic here to make a merge between types of users (Federated and User).
@SQLComponent
public class SqlIdentityUserRepository implements IdentityUserDao {

    @Autowired
    private UserDao userDao;

    @Autowired
    private FederatedUserDao fedUserDao;

    @Autowired
    private RackerDao rackerDao;

    @Override
    public BaseUser getBaseUserById(String userId) {
        BaseUser baseUser = getEndUserById(userId);
        if (baseUser == null) {
            baseUser = rackerDao.getRackerByRackerId(userId);
        }
        return baseUser;
    }

    @Override
    public EndUser getEndUserById(String userId) {
        EndUser endUser = userDao.getUserById(userId);
        if (endUser == null) {
            endUser = fedUserDao.getUserById(userId);
        }
        return endUser;
    }

    @Override
    public User getProvisionedUserById(String userId) {
        return userDao.getUserById(userId);
    }

    @Override
    public FederatedUser getFederatedUserById(String userId) {
        return fedUserDao.getUserById(userId);
    }

    @Override
    public FederatedUser getFederatedUserByUsernameAndIdpName(String username, String idpName) {
        return fedUserDao.getUserByUsernameForIdentityProviderName(username, idpName);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderName(String domainId, String idpName) {
        return fedUserDao.getFederatedUsersByDomainIdAndIdentityProviderName(domainId, idpName);
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String idpName) {
        return fedUserDao.getFederatedUsersByDomainIdAndIdentityProviderNameCount(domainId, idpName);
    }

    @Override
    public Iterable<EndUser> getEndUsersByDomainId(String domainId) {
        final HashSet<EndUser> endUsers = new HashSet<EndUser>();

        final Iterable<User> users = userDao.getUsersByDomain(domainId);
        if (users != null) {
            for (EndUser endUser : users) {
                endUsers.add(endUser);
            }
        }

        final Iterable<FederatedUser> federatedUsers = fedUserDao.getUsersByDomainId(domainId);
        if (federatedUsers != null) {
            for (EndUser endUser : federatedUsers) {
                endUsers.add(endUser);
            }
        }

        return endUsers;
    }

    @Override
    public void updateIdentityUser(BaseUser baseUser) {
        if (baseUser instanceof User) {
            userDao.updateUser((User) baseUser);
        }
        else if (baseUser instanceof FederatedUser) {
            fedUserDao.updateUser((FederatedUser) baseUser);
        } else {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    /*** ----- Union MUST-FIX ----- ***/

    @Override
    public Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        final HashSet<EndUser> endUsers = new HashSet<EndUser>();

        final Iterable<User> users = userDao.getUsersByDomainAndEnabledFlag(domainId, enabled);
        if (users != null) {
            for (EndUser endUser : users) {
                endUsers.add(endUser);
            }
        }

         // FIXME: logic on FederatedDao

        return endUsers;
    }

    @Override
    public Iterable<Group> getGroupsForEndUser(String userId) {
        final HashSet<Group> groups = new HashSet<Group>();

        Iterable<Group> userGroups = userDao.getGroupsForUser(userId);
        if (userGroups != null) {
            for (Group group : userGroups) {
                groups.add(group);
            }
        }

        // FIXME: logic on FederatedDao

        return groups;
    }

    @Override
    public Iterable<EndUser> getEnabledEndUsersByGroupId(String groupId) {
        final HashSet<EndUser> endUsers = new HashSet<EndUser>();

        final Iterable<User> users = userDao.getEnabledUsersByGroupId(groupId);
        if (users != null) {
            for (EndUser endUser : users) {
                endUsers.add(endUser);
            }
        }

        // FIXME: logic on FederatedDao

        return endUsers;
    }

    /*** ----- Pagination MUST-FIX ----- **/

    @Override
    public PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit) {
        final PaginatorContext<EndUser> endUsers = new PaginatorContext<EndUser>();
        final List<EndUser> endUserList = new ArrayList<EndUser>();

        final PaginatorContext<User> users = userDao.getUsersByDomain(domainId, offset, limit);
        if (users != null) {
            endUsers.setLimit(users.getLimit());
            endUsers.setOffset(users.getOffset());
            endUsers.setTotalRecords(users.getTotalRecords());
            for (EndUser endUser : users.getValueList()) {
                endUserList.add(endUser);
            }
        }

        // FIXME: logic on FederatedDao

        endUsers.setValueList(endUserList);
        return endUsers;
    }

    @Override
    public PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit) {
        final PaginatorContext<EndUser> endUsers = new PaginatorContext<EndUser>();
        final List<EndUser> endUserList = new ArrayList<EndUser>();

        final PaginatorContext<User> users = userDao.getEnabledUsers(offset, limit);
        if (users != null) {
            endUsers.setLimit(users.getLimit());
            endUsers.setOffset(users.getOffset());
            endUsers.setTotalRecords(users.getTotalRecords());
            for (EndUser endUser : users.getValueList()) {
                endUserList.add(endUser);
            }
        }

        // FIXME: logic on FederatedDao

        endUsers.setValueList(endUserList);
        return endUsers;
    }

}
