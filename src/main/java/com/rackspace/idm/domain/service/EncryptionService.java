package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.User;

public interface EncryptionService {

    public void setUserEncryptionSaltAndVersion(EndUser user);
    public void encryptUser(EndUser user);
    public void decryptUser(EndUser user);

    public String getEncryptionVersionId();
}
