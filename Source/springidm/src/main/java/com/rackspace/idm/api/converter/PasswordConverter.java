package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordCredentials;

public class PasswordConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public PasswordConverter() {
    }

    public com.rackspace.api.idm.v1.PasswordCredentials toJaxb(Password password) {
        com.rackspace.api.idm.v1.PasswordCredentials userCredentials = objectFactory.createPasswordCredentials();
        com.rackspace.api.idm.v1.UserPassword userPassword = objectFactory.createUserPassword();
        
        userPassword.setPassword(password.getValue());
        userCredentials.setCurrentPassword(userPassword);

        return userCredentials;
    }
    
    public PasswordCredentials toPasswordCredentialsDO(com.rackspace.api.idm.v1.PasswordCredentials passwordCredentials) {
    	PasswordCredentials passwordCredentialsDO = new PasswordCredentials();
    	passwordCredentialsDO.setCurrentPassword(toPasswordDO(passwordCredentials.getCurrentPassword()));
    	passwordCredentialsDO.setNewPassword(toPasswordDO(passwordCredentials.getNewPassword()));
    	passwordCredentialsDO.setVerifyCurrentPassword(passwordCredentials.isVerifyCurrentPassword());

        return passwordCredentialsDO;
    }
    
    public Password toPasswordDO(com.rackspace.api.idm.v1.UserPassword password) {
    	Password passwordDO = new Password();
    	passwordDO.setValue(password.getPassword());
    	return passwordDO;
    }
}
