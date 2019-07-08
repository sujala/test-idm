package com.rackspace.idm.domain.entity;

public enum DomainType {
    RACKSPACE_CLOUD_US("RACKSPACE_CLOUD_US"),
    RACKSPACE_CLOUD_UK("RACKSPACE_CLOUD_UK"),
    DEDICATED("DEDICATED"),
    DATAPIPE("DATAPIPE"),
    UNKNOWN("UNKNOWN");

    String name;

    DomainType(String name) {
        this.name = name;
    }


    public String getName() {
        return name.toUpperCase();
    }
}
