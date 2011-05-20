package com.rackspace.idm.api.converter;

import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.rackspace.idm.domain.entity.UserScopeAccess;
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
    
    public com.rackspace.idm.jaxb.Tokens toTokensJaxb(List<UserScopeAccess> scopeAccessList) {
        com.rackspace.idm.jaxb.Tokens jaxbTokens = of.createTokens();
        
        for(UserScopeAccess u : scopeAccessList) {
            com.rackspace.idm.jaxb.Token token = toTokenJaxb(u.getAccessTokenString(), u.getAccessTokenExp());
            jaxbTokens.getTokens().add(token);
        }
        return jaxbTokens;
    }
}
