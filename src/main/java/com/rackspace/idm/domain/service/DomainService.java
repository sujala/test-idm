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
     * Update domain's userAdmin DN to supplied user. The user supplied must be have the "identity:user-admin" role.
     *
     * @param user
     * @throws IllegalArgumentException If supplied user, user's uniqueId, user's domainId, or user's roles are null.
     * @throws IllegalArgumentException If supplied user is not a user-admin.
     */
    void updateDomainUserAdminDN(User user);

    /**
     * Remove the user admin DN set on domain. This method will remove the specified user from the "rsUserAdminDN"
     * attribute on domain if the user is currently set as the "rsUserAdminDN".
     *
     * @param user
     * @throws IllegalArgumentException If supplied user, user's domainId, or user's uniqueId is null;
     */
    void removeDomainUserAdminDN(User user);

    /**
     * Returns true if the domains share the same RCN. Domains are considered to share the same RCN if:
     * <ul>
     *     <li>The domain IDs are the same (case insensitive) - regardless of whether an RCN is actually set on the domain</li>
     *     <li>The domains contain the same case insensitive RCN</li>
     * </ul>
     *
     * Will always return false in following cases:
     * <ul>
     *     <li>At least one supplied domainId is null or blank</li>
     *     <li>At least one supplied domainId does not resolve to a domain</li>
     *     <li>If supplied domain IDs are distinct (case insenstive) and at least one domain does not contain an RCN</li>
     * </ul>
     *
     * @param domainId1
     * @param domainId2
     * @return
     */
    boolean doDomainsShareRcn(String domainId1, String domainId2);
}
