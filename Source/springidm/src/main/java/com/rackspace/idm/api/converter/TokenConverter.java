package com.rackspace.idm.api.converter;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.RefreshToken;
import com.rackspace.idm.jaxb.ObjectFactory;

public class TokenConverter {
    private ObjectFactory of = new ObjectFactory();

    public com.rackspace.idm.jaxb.Token toTokenJaxb(String tokenString, Date expiration) {
        com.rackspace.idm.jaxb.Token jaxbToken = of.createToken();

        Seconds diff = Seconds.secondsBetween(new DateTime(), new DateTime(expiration));
        int secs = diff.getSeconds();
        
        jaxbToken.setId(tokenString);
        jaxbToken.setExpiresIn(secs);

        return jaxbToken;
    }

    public AccessToken toAccessTokenFromJaxb(
        com.rackspace.idm.jaxb.Token JaxbToken) {
        AccessToken tokenToReturn = new AccessToken();

        tokenToReturn.setTokenString(JaxbToken.getId());
        tokenToReturn.setExpiration(JaxbToken.getExpiresIn());

        return tokenToReturn;
    }
}
