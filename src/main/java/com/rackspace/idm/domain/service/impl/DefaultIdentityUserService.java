package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultIdentityUserService implements IdentityUserService {

    @Autowired
    private IdentityUserDao identityUserRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityProviderDao identityProviderDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public EndUser getEndUserById(String userId) {
        return identityUserRepository.getEndUserById(userId);
    }

    @Override
    public EndUser checkAndGetEndUserById(String userId) {
        EndUser user = getEndUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public User getProvisionedUserById(String userId) {
        return identityUserRepository.getProvisionedUserById(userId);
    }

    public FederatedUser getFederatedUserByUsernameAndIdentityProviderId(String username, String providerId) {
        return identityUserRepository.getFederatedUserByUsernameAndIdpId(username, providerId);
    }

    @Override
    public FederatedUser checkAndGetFederatedUserByUsernameAndIdentityProviderName(String username, String providerId) {
        FederatedUser user = getFederatedUserByUsernameAndIdentityProviderId(username, providerId);

        if (user == null) {
            String errMsg = String.format("Federated user %s not found for IDP with id %s", username, providerId);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public FederatedUser checkAndGetFederatedUserByUsernameAndIdentityProviderUri(String username, String idpUri) {
        IdentityProvider idp = identityProviderDao.getIdentityProviderByUri(idpUri);

        FederatedUser user = null;
        if(idp != null) {
            user = getFederatedUserByUsernameAndIdentityProviderId(username, idp.getProviderId());
        }

        if (user == null) {
            String errMsg = String.format("Federated user %s not found for IDP with URI %s", username, idpUri);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public FederatedUser getFederatedUserById(String userId) {
        return identityUserRepository.getFederatedUserById(userId);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderName(String domainId, String idpName) {
        return identityUserRepository.getFederatedUsersByDomainIdAndIdentityProviderId(domainId, idpName);
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String idpName) {
        return identityUserRepository.getFederatedUsersByDomainIdAndIdentityProviderIdCount(domainId, idpName);
    }

    @Override
    public Iterable<EndUser> getEndUsersByDomainId(String domainId) {
        return identityUserRepository.getEndUsersByDomainId(domainId);
    }

    @Override
    public Iterable<User> getProvisionedUsersByDomainId(String domainId) {
        return userService.getUsersWithDomain(domainId);
    }

    public Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        return identityUserRepository.getEndUsersByDomainIdAndEnabledFlag(domainId, enabled);
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
    public Iterable<Group> getGroupsForEndUser(String userId) {
        return identityUserRepository.getGroupsForEndUser(userId);
    }

    @Override
    public EndUser checkAndGetUserById(String userId) {
        EndUser user = getEndUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    /**
     * Adds the group to the end user. After adding will send atom hopper event
     * @param groupId
     * @param endUserId
     *
     */
    @Override
    public void addGroupToEndUser(String groupId, String endUserId) {
        EndUser user = getEndUserById(endUserId);
        if (user != null && !user.getRsGroupId().contains(groupId)) {
            logger.debug("Adding groupId {} to user {}", groupId, endUserId);
            user.getRsGroupId().add(groupId);
            identityUserRepository.updateIdentityUser(user);
            logger.debug("Added groupId {} to user {}", groupId, endUserId);
        }
    }

    /**
     * Removes the group from the end user. After removing will send atom hopper event
     * @param groupId
     * @param endUserId
     *
     */
    @Override
    public void removeGroupFromEndUser(String groupId, String endUserId) {
        EndUser user = getEndUserById(endUserId);
        if (user != null && user.getRsGroupId().contains(groupId)) {
            logger.debug("Removing groupId {} from user {}", groupId, endUserId);
            user.getRsGroupId().remove(groupId);
            identityUserRepository.updateIdentityUser(user);
            logger.debug("Removed groupId {} from user {}", groupId, endUserId);
        }
    }

    @Override
    public Iterable<EndUser> getEnabledEndUsersByGroupId(String groupId) {
        logger.debug("Getting All Users: {} - {}", groupId);
        return identityUserRepository.getEnabledEndUsersByGroupId(groupId);
    }

    @Override
    public void deleteUser(BaseUser user) {
        if (user instanceof Racker) {
            //rackers are not persistent, so don't bother trying to delete
            return;
        }
        identityUserRepository.deleteIdentityUser(user);
    }

    @Override
    public int getUsersWithinRegionCount(String regionName) {
        return identityUserRepository.getUsersWithinRegionCount(regionName);
    }
}
