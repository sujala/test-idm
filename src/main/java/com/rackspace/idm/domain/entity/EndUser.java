package com.rackspace.idm.domain.entity;

import java.util.List;

/**
 * Represents a non-racker end user
 */
public interface EndUser extends BaseUser {
    String getRegion();
    String getEmail();
    String getUsername();
    List<TenantRole> getRoles();
    String getCustomerId();
    String getId();
}
