package com.rackspace.idm.api.converter;

import javax.xml.bind.JAXBElement;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordCredentials;

public class PasswordConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public PasswordConverter() {
    }

    public JAXBElement<com.rackspace.api.idm.v1.UserPassword> toJaxb(Password password) {
        com.rackspace.api.idm.v1.UserPassword userPassword = objectFactory.createUserPassword();
        
        userPassword.setPassword(password.getValue());

        return objectFactory.createUserPassword(userPassword);
    }
    
    public PasswordCredentials toPasswordCredentialsDO(UserPasswordCredentials passwordCredentials) {
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
