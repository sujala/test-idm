package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

public interface DomainService {

    void addDomain(Domain domain);
    Domain getDomain(String domainId);
    PaginatorContext<Domain> getDomains(int offset, int limit);
    PaginatorContext<Domain> getDomainsByRCN(String rcn, Integer marker, Integer limit);
    void updateDomain(Domain domain);
    void deleteDomain(String domainId);
    void addTenantToDomain(String tenantId, String domainId);
    void removeTenantFromDomain(String tenantId, String domainId);
    Iterable<User> getUsersByDomainId(String domainId);
    Iterable<User> getUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled);
    List<User> getDomainAdmins(String domainId);
    List<User> getDomainSuperAdmins(String domainId);
    List<User> getEnabledDomainAdmins(String domainId);
    Domain checkAndGetDomain(String domainId);
    String createNewDomain(String domainId);
    Iterable<Domain> getDomainsForTenants(List<Tenant> tenants);
    void expireAllTokenInDomain(String domainId);
    void deleteDomainPasswordPolicy(String domainId);

    /**
     * Return all the domains that have the specified RCN. Will return an empty list if the specified RCN is empty.
     *
     * @param rcn
     * @return
     */
    Iterable<Domain> findDomainsWithRcn(String rcn);

    /**
     * Generates a UUID for a new domain
     */
    String getDomainUUID();

    /**
     * Update domain's userAdmin DN by supplied user. The user supplied must be have the "identity:user-admin" role.
     *
     * @param user
     * @throws IllegalArgumentException If supplied user, user's uniqueId, user's domainId, or user's roles are null.
     * @throws IllegalArgumentException If supplied user is not a user-admin.
     */
    void updateDomainUserAdminDN(User user);

    /**
     * Delete the user admin DN set on domain.
     *
     * @param user
     * @throws IllegalArgumentException If supplied user, user's domainId, or user's uniqueId is null;
     */
    void deleteDomainUserAdminDN(User user);
}
