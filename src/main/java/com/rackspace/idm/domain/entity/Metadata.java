package com.rackspace.idm.domain.entity;

import java.util.Set;

/**
 * Represents metadata for directory
 */
public interface Metadata {
    public Set<String> getMetadata();
    public void setMetadata(Set<String> metadata);
}
