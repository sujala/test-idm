package com.rackspace.idm.domain.security.encrypters.keyczar;

import java.util.Date;

public interface KeyVersion {
    Integer getVersion();
    String getData();
    Date getCreated();
}
