package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Password;

public class PasswordConverter {

    private final ObjectFactory of = new ObjectFactory();

    public PasswordConverter() {
    }

    public com.rackspace.api.idm.v1.UserPassword toJaxb(Password password) {
        com.rackspace.api.idm.v1.UserPassword userPassword = of.createUserPassword();

        userPassword.setPassword(password.getValue());

        return userPassword;
    }

}
