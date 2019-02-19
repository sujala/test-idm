package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.DN;

import java.util.List;
import java.util.Set;

/**
 * Represents a non-racker end user
 */
public interface EndUser extends BaseUser, PhonePinProtectedUser {
    String getRegion();
    String getEmail();
    String getUsername();
    String getDomainId();
    List<TenantRole> getRoles();
    Set<String> getRsGroupId();

    /*
    These were added to allow conversion for userById service
     */
    void setEmail(String email);
    void setRegion(String region);
    void setDomainId(String domainId);

    /**
     * This is required in order to calculate the effective roles for the end user based on user group membership
     * @return
     */
    Set<String> getUserGroupIds();
    Set<DN> getUserGroupDNs();

    /**
     * The "salesforce" contact Id maintained by core and linked for dedicated users.
     * @return
     */
    String getContactId();
}
