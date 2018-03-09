package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.User;

public interface EncryptionService {

    public void setUserEncryptionSaltAndVersion(User user);
    public void encryptUser(User user);
    public void decryptUser(User user);

    public void setUserEncryptionSaltAndVersion(FederatedUser user);
    public void encryptUser(FederatedUser user);
    public void decryptUser(FederatedUser user);

    public String getEncryptionVersionId();
}
