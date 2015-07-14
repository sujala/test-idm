package com.rackspace.idm.domain.entity;

import java.util.Date;

public interface KeyVersion {
    Integer getVersion();
    String getData();
    Date getCreated();
}
