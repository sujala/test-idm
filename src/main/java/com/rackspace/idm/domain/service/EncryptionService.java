package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.User;

public interface EncryptionService {

    public void encryptUser(User user);
    public void decryptUser(User user);
    public void encryptApplication(Application application);
    public void decryptApplication(Application application);

    public String getEncryptionVersionId();
}
