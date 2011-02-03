package com.rackspace.idm.converters;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.jaxb.IdmScopeType;
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

    public AccessToken toAccessTokenFromJaxb(
        com.rackspace.idm.jaxb.Token jaxbToken) {
        AccessToken tokenToReturn = new AccessToken();

        tokenToReturn.setTokenString(jaxbToken.getId());
        tokenToReturn.setExpiration(jaxbToken.getExpiresIn());
        if (IdmScopeType.SET_PASSWORD == jaxbToken.getIdmScope()) {
            tokenToReturn.setRestrictedToSetPassword();
        }

        jaxbToken.setIsTrusted(jaxbToken.isIsTrusted());

        return tokenToReturn;
    }
}
