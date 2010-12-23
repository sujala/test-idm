package com.rackspace.idm.converters;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.jaxb.ObjectFactory;

public class TokenConverter {
    protected ObjectFactory of = new ObjectFactory();

    public com.rackspace.idm.jaxb.Token toAccessTokenJaxb(AccessToken token) {
        com.rackspace.idm.jaxb.Token jaxbToken = of.createToken();

        jaxbToken.setId(token.getTokenString());
        jaxbToken.setExpiresIn(token.getExpiration());

        return jaxbToken;
    }

    public com.rackspace.idm.jaxb.Token toRefreshTokenJaxb(RefreshToken token) {
        com.rackspace.idm.jaxb.Token jaxbToken = of.createToken();

        jaxbToken.setId(token.getTokenString());

        return jaxbToken;
    }

    public static AccessToken toAccessTokenFromJaxb(
        com.rackspace.idm.jaxb.Token JaxbToken) {
        AccessToken tokenToReturn = new AccessToken();

        tokenToReturn.setTokenString(JaxbToken.getId());
        tokenToReturn.setExpiration(JaxbToken.getExpiresIn());

        return tokenToReturn;
    }
}
