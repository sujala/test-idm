package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.User;

public interface EncryptionService {

    public void encryptUser(User user);
    public void decryptUser(User user);

    public String getEncryptionVersionId();
}
