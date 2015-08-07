package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.sql.dao.IdentityUserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

@SQLComponent
public class SqlIdentityUserRepository implements IdentityUserDao {

    @Autowired
    private UserDao userDao;

    @Autowired
    private FederatedUserDao fedUserDao;

    @Autowired
    private IdentityUserRepository identityUserRepository;

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
        final Set<EndUser> endUsers = new HashSet<EndUser>();

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

    @Override
    public Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        final Set<EndUser> endUsers = new HashSet<EndUser>();

        final Iterable<User> users = userDao.getUsersByDomainAndEnabledFlag(domainId, enabled);
        if (users != null) {
            for (EndUser endUser : users) {
                endUsers.add(endUser);
            }
        }

        final Iterable<FederatedUser> federatedUsers = fedUserDao.getUsersByDomainId(domainId);
        if (federatedUsers != null) {
            for (FederatedUser federatedUser : federatedUsers) {
                endUsers.add(federatedUser);
            }
        }

        return endUsers;
    }

    @Override
    public Iterable<Group> getGroupsForEndUser(String userId) {
        final Set<Group> groups = new HashSet<Group>();

        final Iterable<Group> userGroups = userDao.getGroupsForUser(userId);
        if (userGroups != null) {
            for (Group group : userGroups) {
                groups.add(group);
            }
        }

        if (groups.size() == 0) {
            final Iterable<Group> federatedGroups = fedUserDao.getGroupsForFederatedUser(userId);
            if (federatedGroups != null) {
                for (Group group : federatedGroups) {
                    groups.add(group);
                }
            }
        }

        return groups;
    }

    @Override
    public Iterable<EndUser> getEnabledEndUsersByGroupId(String groupId) {
        final Set<EndUser> endUsers = new HashSet<EndUser>();

        final Iterable<User> users = userDao.getEnabledUsersByGroupId(groupId);
        if (users != null) {
            for (EndUser endUser : users) {
                endUsers.add(endUser);
            }
        }

        final Iterable<FederatedUser> federatedUsers = fedUserDao.getFederatedUsersByGroupId(groupId);
        if (federatedUsers != null) {
            for (EndUser endUser : federatedUsers) {
                endUsers.add(endUser);
            }
        }

        return endUsers;
    }

    @Override
    public PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit) {
        return identityUserRepository.getEndUsersByDomainIdPaged(domainId, offset, limit);
    }

    @Override
    public PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit) {
        return identityUserRepository.getEnabledEndUsersPaged(offset, limit);
    }

}
