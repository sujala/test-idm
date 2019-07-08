package com.rackspace.idm.domain.entity;

public enum CloudRegion {US("US"), UK("UK");

    String name;

    CloudRegion(String name){
        this.name = name;
    }

    public String getName() {
        return name.toUpperCase();
    }
}
