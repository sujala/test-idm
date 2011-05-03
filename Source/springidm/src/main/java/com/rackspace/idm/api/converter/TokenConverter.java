package com.rackspace.idm.api.converter;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.rackspace.idm.jaxb.ObjectFactory;

public class TokenConverter {
    private final ObjectFactory of = new ObjectFactory();

    public com.rackspace.idm.jaxb.Token toTokenJaxb(String tokenString, Date expiration) {
        com.rackspace.idm.jaxb.Token jaxbToken = of.createToken();

        Seconds diff = Seconds.secondsBetween(new DateTime(), new DateTime(expiration));
        int secs = diff.getSeconds();
        
        jaxbToken.setId(tokenString);
        
        if(expiration != null) {
            jaxbToken.setExpiresIn(secs);
        }

        return jaxbToken;
    }
}
