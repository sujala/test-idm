package com.rackspace.idm.api.converter;

import java.util.Date;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.joda.time.DateTime;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;

public class TokenConverter {
    
    private final ObjectFactory of = new ObjectFactory();
    
    public TokenConverter() {
    }

    public com.rackspace.api.idm.v1.Token toTokenJaxb(String tokenString,
        Date expiration) {
    	if (tokenString == null) {
    		return null;
    	}
    	
        com.rackspace.api.idm.v1.Token jaxbToken = of.createToken();

        jaxbToken.setId(tokenString);

        try {
            if (expiration != null) {

                jaxbToken.setExpires(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                        new DateTime(expiration).toGregorianCalendar()));
            }

        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return jaxbToken;
    }

//    public com.rackspace.api.idm.v1.Token toTokenJaxb(String tokenString,
//        Date expiration, Permissions permList) {
//        com.rackspace.api.idm.v1.Token jaxbToken = of.createToken();
//
//        jaxbToken.setId(tokenString);
//
//        try {
//            if (expiration != null) {
//
//                jaxbToken.setExpires(DatatypeFactory.newInstance()
//                    .newXMLGregorianCalendar(
//                        new DateTime(expiration).toGregorianCalendar()));
//            }
//
//        } catch (DatatypeConfigurationException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        if (permList != null) {
//            jaxbToken.setPermissionList(permList);
//        }
//
//        return jaxbToken;
//    }

    public com.rackspace.api.idm.v1.DelegatedToken toDelegatedTokenJaxb(
        DelegatedClientScopeAccess token, List<Permission> perms) {
        com.rackspace.api.idm.v1.DelegatedToken jaxbToken = of
            .createDelegatedToken();

        jaxbToken.setId(token.getRefreshTokenString());
        jaxbToken.setClientId(token.getClientId());

        try {
            jaxbToken.setExpires(DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(
                    new DateTime(token.getRefreshTokenExp())
                        .toGregorianCalendar()));

        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return jaxbToken;
    }

    public com.rackspace.api.idm.v1.Tokens toTokensJaxb(
        List<DelegatedClientScopeAccess> scopeAccessList) {
        com.rackspace.api.idm.v1.Tokens jaxbTokens = of.createTokens();

        for (DelegatedClientScopeAccess u : scopeAccessList) {
            com.rackspace.api.idm.v1.Token token = toTokenJaxb(
                u.getRefreshTokenString(), u.getRefreshTokenExp());
            jaxbTokens.getTokens().add(token);
        }
        return jaxbTokens;
    }

    public com.rackspace.api.idm.v1.DelegatedTokens toDelegatedTokensJaxb(
        List<DelegatedClientScopeAccess> scopeAccessList) {
        com.rackspace.api.idm.v1.DelegatedTokens jaxbTokens = of
            .createDelegatedTokens();

        for (DelegatedClientScopeAccess u : scopeAccessList) {
            com.rackspace.api.idm.v1.DelegatedToken token = toDelegatedTokenJaxb(u, null);
            jaxbTokens.getDelegatedTokens().add(token);
        }
        return jaxbTokens;
    }
}
