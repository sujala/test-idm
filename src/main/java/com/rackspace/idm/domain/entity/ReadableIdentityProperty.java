package com.rackspace.idm.domain.entity;

public interface ReadableIdentityProperty {
    String getValueAsString();

    String getId();

    String getName();

    byte[] getValue();

    String getValueType();

    String getDescription();

    String getIdmVersion();

    boolean isSearchable();

    boolean isReloadable();
}
