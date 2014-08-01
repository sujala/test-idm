package com.rackspace.idm.domain.entity;

import java.util.HashSet;
import java.util.List;

/**
 * Represents a non-racker end user
 */
public interface EndUser extends BaseUser {
    String getRegion();
    String getEmail();
    String getUsername();
    String getDomainId();
    List<TenantRole> getRoles();
    String getCustomerId();
    String getId();
    HashSet<String> getRsGroupId();

    /*
    These were added to allow conversion for userById service
     */
    void setEmail(String email);
    void setRegion(String region);
    void setDomainId(String domainId);
}
