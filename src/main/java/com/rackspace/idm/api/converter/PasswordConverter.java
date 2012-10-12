package com.rackspace.idm.api.converter;

import org.springframework.stereotype.Component;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordCredentials;

import javax.xml.bind.JAXBElement;

@Component
public class PasswordConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public PasswordConverter() {
    }

    public JAXBElement<com.rackspace.api.idm.v1.UserPasswordCredentials> toJaxb(Password password) {
        com.rackspace.api.idm.v1.UserPasswordCredentials userPassword = objectFactory.createUserPasswordCredentials();
        com.rackspace.api.idm.v1.UserPassword currentPassword = objectFactory.createUserPassword();
        
        currentPassword.setPassword(password.getValue());
        userPassword.setCurrentPassword(currentPassword);
        
        return objectFactory.createPasswordCredentials(userPassword);
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
