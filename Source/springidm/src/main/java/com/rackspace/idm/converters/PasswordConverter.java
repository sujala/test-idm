package com.rackspace.idm.converters;

import com.rackspace.idm.entities.Password;
import com.rackspace.idm.jaxb.ObjectFactory;

public class PasswordConverter {

    protected ObjectFactory of = new ObjectFactory();

    public PasswordConverter() {
    }

    public com.rackspace.idm.jaxb.UserPassword toJaxb(Password password) {
        com.rackspace.idm.jaxb.UserPassword userPassword = new com.rackspace.idm.jaxb.UserPassword();

        userPassword.setPassword(password.getValue());

        return userPassword;
    }

}
