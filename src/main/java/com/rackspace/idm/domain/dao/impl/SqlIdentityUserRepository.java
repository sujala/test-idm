package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.sql.dao.IdentityUserRepository;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
    public FederatedUser getFederatedUserByUsernameAndIdpId(String username, String idpId) {
        return fedUserDao.getUserByUsernameForIdentityProviderId(username, idpId);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String idpId) {
        return fedUserDao.getFederatedUsersByDomainIdAndIdentityProviderId(domainId, idpId);
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String idpId) {
        return fedUserDao.getFederatedUsersByDomainIdAndIdentityProviderIdCount(domainId, idpId);
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
    @Transactional
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

        if(enabled) {
            final Iterable<FederatedUser> federatedUsers = fedUserDao.getUsersByDomainId(domainId);
            if (federatedUsers != null) {
                for (FederatedUser federatedUser : federatedUsers) {
                    endUsers.add(federatedUser);
                }
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
    public int getUsersWithinRegionCount(String regionName) {
        throw new NotImplementedException("Not Implemented");
    }

    @Override
    public int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
