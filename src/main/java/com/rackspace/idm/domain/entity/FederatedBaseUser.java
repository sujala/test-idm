package com.rackspace.idm.domain.entity;

public interface FederatedBaseUser extends BaseUser {
    String getFederatedIdpUri();
    void setId(String id);
}
