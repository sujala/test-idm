package com.rackspace.idm.api.converter;

import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.jaxb.ObjectFactory;
import com.rackspace.idm.jaxb.Permissions;

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
    
    public com.rackspace.idm.jaxb.Token toTokenJaxb(String tokenString, Date expiration, Permissions permList) {
        com.rackspace.idm.jaxb.Token jaxbToken = of.createToken();

        Seconds diff = Seconds.secondsBetween(new DateTime(), new DateTime(expiration));
        int secs = diff.getSeconds();
        
        jaxbToken.setId(tokenString);
        
        if(expiration != null) {
            jaxbToken.setExpiresIn(secs);
        }
        
        if ( permList != null) {
            jaxbToken.setPermissionList(permList);
        }

        return jaxbToken;
    }
    
    public com.rackspace.idm.jaxb.Tokens toTokensJaxb(List<DelegatedClientScopeAccess> scopeAccessList) {
        com.rackspace.idm.jaxb.Tokens jaxbTokens = of.createTokens();
        
        for(DelegatedClientScopeAccess u : scopeAccessList) {
            com.rackspace.idm.jaxb.Token token = toTokenJaxb(u.getRefreshTokenString(), u.getRefreshTokenExp());
            jaxbTokens.getTokens().add(token);
        }
        return jaxbTokens;
    }
}
